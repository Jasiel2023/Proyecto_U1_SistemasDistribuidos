import { Client } from '@stomp/stompjs'
import { execFileSync } from 'node:child_process'
import { mkdir, readFile, writeFile } from 'node:fs/promises'
import { fileURLToPath } from 'node:url'
import path from 'node:path'

const gateway = process.env.GATEWAY_URL ?? 'http://localhost:8080'
const websocketUrl = process.env.RESULTADOS_WS_URL ?? 'ws://localhost:8083/ws'
const password = process.env.VOTER_TEST_PASSWORD ?? '123456'
const evidenceDir = fileURLToPath(new URL('../../artifacts/integration/', import.meta.url))
const delay = (ms) => new Promise((resolve) => setTimeout(resolve, ms))

async function request(route, { method = 'GET', token, body } = {}) {
  const response = await fetch(`${gateway}${route}`, {
    method,
    headers: {
      ...(body === undefined ? {} : { 'Content-Type': 'application/json' }),
      ...(token ? { Authorization: `Bearer ${token}` } : {}),
    },
    ...(body === undefined ? {} : { body: JSON.stringify(body) }),
  })
  const text = await response.text()
  let parsed = text
  try { parsed = text ? JSON.parse(text) : null } catch {}
  return { status: response.status, body: parsed }
}

function requireStatus(result, statuses, action) {
  if (!statuses.includes(result.status)) {
    throw new Error(`${action}: HTTP ${result.status} ${JSON.stringify(result.body)}`)
  }
}

async function writeEvidence(name, record) {
  await mkdir(evidenceDir, { recursive: true })
  const file = path.join(evidenceDir, name)
  await writeFile(file, `${JSON.stringify(record, null, 2)}\n`, 'utf8')
  return file
}

function persistedVotes(eleccionId) {
  const sql = `SELECT count(*) FROM votos WHERE eleccion_id = '${eleccionId}'`
  const count = execFileSync('docker', [
    'exec', 'votacion-postgres-votacion', 'psql', '-U', 'admin', '-d', 'votaciondb', '-tAc', sql,
  ], { encoding: 'utf8', timeout: 15000 }).trim()
  return { count: Number(count), sql }
}

async function ensureVoter({ nickname, nombre, apellido, correo, cedula }) {
  let priorCreation = null
  try {
    const previous = JSON.parse(await readFile(path.join(evidenceDir, 'alta-perfiles-catalogo.json'), 'utf8'))
    priorCreation = [...(previous.voters ?? []), ...(previous.additionalVoters ?? [])]
      .find((voter) => voter.nickname === nickname) ?? null
  } catch {}
  const priorLogin = await request('/api/v1/auth/login', {
    method: 'POST', body: { nickname, clave: password },
  })
  if (priorLogin.status === 200 && priorLogin.body?.token) {
    const profile = await request('/api/v1/auth/me', { token: priorLogin.body.token })
    const userProfile = profile.status === 200
      ? await request(`/api/v1/usuarios/cuenta/${profile.body.userId}`)
      : null
    if (userProfile?.status === 200) {
      return {
        nickname,
        token: priorLogin.body.token,
        authProfile: profile.body,
        userProfile: userProfile.body,
        created: Boolean(priorCreation?.created && priorCreation.authProfile?.userId === profile.body.userId),
        createResponse: priorCreation?.authProfile?.userId === profile.body.userId ? priorCreation.createResponse : null,
        loginStatus: priorLogin.status,
      }
    }
  }

  const createResponse = await request('/api/v1/usuarios', {
    method: 'POST',
    body: { clave: password, cedula, correo, nickname, nombre, apellido, rol: 'VOTANTE' },
  })
  requireStatus(createResponse, [201, 409], `crear votante ${nickname}`)

  const login = await request('/api/v1/auth/login', {
    method: 'POST', body: { nickname, clave: password },
  })
  requireStatus(login, [200], `login ${nickname}`)
  const authProfile = await request('/api/v1/auth/me', { token: login.body.token })
  requireStatus(authProfile, [200], `perfil de cuenta ${nickname}`)
  const userProfile = await request(`/api/v1/usuarios/cuenta/${authProfile.body.userId}`, { token: login.body.token })
  requireStatus(userProfile, [200], `perfil de usuario ${nickname}`)
  if (userProfile.body.rol !== 'VOTANTE') {
    throw new Error(`El usuario ${nickname} tiene rol ${userProfile.body.rol}, no VOTANTE`)
  }
  return {
    nickname,
    token: login.body.token,
    authProfile: authProfile.body,
    userProfile: userProfile.body,
    created: createResponse.status === 201,
    createResponse,
    loginStatus: login.status,
  }
}

async function getResultEventually(eleccionId, expectedTotal) {
  for (let attempt = 0; attempt < 80; attempt += 1) {
    const result = await request(`/api/resultados/elecciones/${eleccionId}`)
    if (result.status === 200 && result.body?.totalVotos === expectedTotal) return result
    await delay(250)
  }
  return request(`/api/resultados/elecciones/${eleccionId}`)
}

async function rabbitTrace(electionId) {
  const trace = { queues: null, votePublisher: [], resultConsumer: [] }
  try {
    const response = await fetch('http://localhost:15672/api/queues/%2F', {
      headers: { Authorization: `Basic ${Buffer.from('guest:guest').toString('base64')}` },
    })
    if (!response.ok) throw new Error(`RabbitMQ Management HTTP ${response.status}`)
    const queues = await response.json()
    trace.queues = queues
      .filter((queue) => ['cuentas.crear', 'cuentas.creadas', 'votos.procesados'].includes(queue.name))
      .map(({ name, messages, consumers }) => ({ name, messages, consumers }))
  } catch (error) {
    trace.queues = `No disponible: ${error.message}`
  }
  for (const [service, key] of [['votacion-service', 'votePublisher'], ['resultados-service', 'resultConsumer']]) {
    try {
      const name = execFileSync('docker', [
        'ps', '--filter', `label=com.docker.compose.service=${service}`, '--format', '{{.Names}}',
      ], { encoding: 'utf8', timeout: 10000 }).trim().split(/\r?\n/)[0]
      if (name) {
        const logs = execFileSync('docker', ['logs', '--since', '10m', name], {
          encoding: 'utf8', timeout: 15000,
        })
        trace[key] = logs.split(/\r?\n/)
          .filter((line) => /voto\.emitido|eventId=.*eleccionId/.test(line))
          .filter((line) => !electionId || line.includes(`eleccionId=${electionId}`))
          .slice(-120)
      }
    } catch (error) {
      trace[key] = [`No disponible: ${error.message}`]
    }
  }
  return trace
}

const evidence = {
  setup: { verdict: 'PENDING', startedAt: new Date().toISOString() },
  doubleVote: { verdict: 'PENDING' },
  consistency: { verdict: 'PENDING' },
}
let traceElectionId = null

try {
  const are = await ensureVoter({
    nickname: 'are', nombre: 'Arelys', apellido: 'Ajila',
    correo: 'are.integration@example.test', cedula: '1720000001',
  })
  const leo = await ensureVoter({
    nickname: 'leo', nombre: 'Leonardo', apellido: 'Votante',
    correo: 'leo.integration@example.test', cedula: '1720000002',
  })
  const additionalVoters = []
  for (let batch = 0; batch < 98; batch += 10) {
    const batchEnd = Math.min(batch + 10, 98)
    const voters = await Promise.all(Array.from({ length: batchEnd - batch }, (_, offset) => {
      const index = batch + offset
      const serial = String(index).padStart(3, '0')
      return ensureVoter({
        nickname: `v${serial}`,
        nombre: `Votante ${serial}`,
        apellido: 'Prueba',
        correo: `votante.${serial}@integration.example.test`,
        cedula: String(1730000000 + index),
      })
    }))
    additionalVoters.push(...voters)
  }

  async function createOpenElection(suffix) {
    const response = await request('/api/elecciones', {
      method: 'POST', body: {
        titulo: `Prueba ${suffix} ${new Date().toISOString()}`,
        descripcion: `Elección temporal de ${suffix} para pruebas de integración local`,
        candidatos: [
          { nombre: 'Candidata A', partido: 'Partido A' },
          { nombre: 'Candidato B', partido: 'Partido B' },
        ],
      },
    })
    requireStatus(response, [201], `crear elección abierta ${suffix}`)
    return response
  }

  const createElection = await createOpenElection('doble voto')
  const election = createElection.body
  traceElectionId = election.id
  const catalog = await request('/api/elecciones')
  requireStatus(catalog, [200], 'consultar catálogo')
  const listedElection = catalog.body.find((entry) => entry.id === election.id)
  if (!listedElection || listedElection.estado !== 'OPEN' || listedElection.candidatos.length !== 2) {
    throw new Error('La elección abierta con dos candidatos no apareció correctamente en el catálogo')
  }

  evidence.setup = {
    verdict: 'PASS',
    commandToRepeat: 'cd frontend; node scripts/integration/run-vote-integration.mjs',
    expectedVoterCount: 100,
    voters: [are, leo].map(({ nickname, created, loginStatus, authProfile, userProfile, createResponse }) => ({
      nickname, created, loginStatus, authProfile, userProfile, createResponse,
    })),
    additionalVoters: additionalVoters.map(({ nickname, created, loginStatus, authProfile, userProfile, createResponse }) => ({
      nickname, created, loginStatus, authProfile, userProfile, createResponse,
    })),
    election: { createStatus: createElection.status, ...election },
    catalogStatus: catalog.status,
    catalogEntry: listedElection,
    testedAt: new Date().toISOString(),
  }

  const beforeDouble = persistedVotes(election.id)
  const doubleResponses = await Promise.all(Array.from({ length: 100 }, () => request('/api/votos', {
    method: 'POST',
    token: are.token,
    body: {
      eleccionId: election.id,
      candidatoId: election.candidatos[0].id,
      votanteId: are.authProfile.userId,
    },
  })))
  const accepted = doubleResponses.filter((response) => response.status === 201 && response.body?.aceptado)
  const rejected = doubleResponses.filter((response) => response.status === 409 && response.body?.codigo === 'VOTO_DUPLICADO')
  const afterDouble = await getResultEventually(election.id, 1)
  const afterDoublePersisted = persistedVotes(election.id)
  if (accepted.length !== 1 || rejected.length !== 99) {
    throw new Error(`Doble voto esperado 1 aceptado/99 rechazados; real ${accepted.length}/${rejected.length}`)
  }
  if (afterDouble.status !== 200 || afterDouble.body.totalVotos !== 1 || afterDoublePersisted.count !== 1) {
    throw new Error(`REST después del doble voto no muestra total 1: ${JSON.stringify(afterDouble)}`)
  }
  evidence.doubleVote = {
    verdict: 'PASS',
    commandToRepeat: 'cd frontend; node scripts/integration/run-vote-integration.mjs',
    electionId: election.id,
    voter: 'are',
    concurrentRequests: doubleResponses.length,
    accepted: accepted.length,
    rejectedAsDuplicate: rejected.length,
    responses: doubleResponses.map(({ status, body }) => ({ status, body })),
    persistedVotesBefore: beforeDouble.count,
    persistedVotesAfter: afterDoublePersisted.count,
    persistenceQuery: afterDoublePersisted.sql,
    restSnapshot: afterDouble.body,
    testedAt: new Date().toISOString(),
  }

  const createConsistencyElection = await createOpenElection('consistencia 100 votos')
  const consistencyElection = createConsistencyElection.body
  traceElectionId = consistencyElection.id
  const consistencyCatalog = await request('/api/elecciones')
  requireStatus(consistencyCatalog, [200], 'consultar catálogo antes de consistencia')
  const consistencyListed = consistencyCatalog.body.find((entry) => entry.id === consistencyElection.id)
  if (!consistencyListed || consistencyListed.candidatos.length !== 2 || consistencyListed.estado !== 'OPEN') {
    throw new Error('La elección de consistencia no aparece abierta con dos candidatos en el catálogo')
  }
  evidence.setup.consistencyElection = { createStatus: createConsistencyElection.status, ...consistencyElection }

  const consistencyVoters = [are, leo, ...additionalVoters]
  if (consistencyVoters.length !== 100) throw new Error(`Se esperaban 100 votantes; hay ${consistencyVoters.length}`)

  const stomp = new Client({ brokerURL: websocketUrl, reconnectDelay: 0, debug: () => {} })
  await new Promise((resolve, reject) => {
    const timeout = setTimeout(() => reject(new Error('Timeout conectando a WebSocket STOMP')), 15000)
    stomp.onConnect = () => { clearTimeout(timeout); resolve() }
    stomp.onStompError = (frame) => { clearTimeout(timeout); reject(new Error(frame.headers.message ?? frame.body)) }
    stomp.onWebSocketError = () => { clearTimeout(timeout); reject(new Error('Error de transporte WebSocket')) }
    stomp.activate()
  })
  const websocketSnapshots = []
  let resolveSnapshot
  const snapshotPromise = new Promise((resolve) => { resolveSnapshot = resolve })
  stomp.subscribe(`/topic/resultados/elecciones/${consistencyElection.id}`, (message) => {
    const snapshot = JSON.parse(message.body)
    if (snapshot.eleccionId === consistencyElection.id) {
      websocketSnapshots.push(snapshot)
      if (snapshot.totalVotos === consistencyVoters.length) resolveSnapshot(snapshot)
    }
  })
  await delay(500)
  const consistencyVoteResponses = await Promise.all(consistencyVoters.map((voter, index) => request('/api/votos', {
    method: 'POST',
    token: voter.token,
    body: {
      eleccionId: consistencyElection.id,
      candidatoId: consistencyElection.candidatos[index % consistencyElection.candidatos.length].id,
      votanteId: voter.authProfile.userId,
    },
  })))
  const consistencyAccepted = consistencyVoteResponses.filter((response) => response.status === 201 && response.body?.aceptado)
  if (consistencyAccepted.length !== 100) {
    throw new Error(`Votos válidos esperados 100; aceptados ${consistencyAccepted.length}`)
  }
  const wsSnapshot = await Promise.race([
    snapshotPromise,
    delay(45000).then(() => { throw new Error('Timeout esperando snapshot STOMP total=100') }),
  ])
  const restAfter = await getResultEventually(consistencyElection.id, 100)
  const finalPersisted = persistedVotes(consistencyElection.id)
  requireStatus(restAfter, [200], 'consultar resultado REST actualizado')
  const equal = wsSnapshot.eleccionId === restAfter.body.eleccionId
    && wsSnapshot.totalVotos === restAfter.body.totalVotos
    && wsSnapshot.revision === restAfter.body.revision
  if (!equal || restAfter.body.totalVotos !== 100 || restAfter.body.revision !== 100 || finalPersisted.count !== 100) {
    throw new Error(`REST y WS difieren o no alcanzan 100: REST=${JSON.stringify(restAfter.body)} WS=${JSON.stringify(wsSnapshot)} DB=${finalPersisted.count}`)
  }
  await stomp.deactivate()
  evidence.consistency = {
    verdict: 'PASS',
    commandToRepeat: 'cd frontend; node scripts/integration/run-vote-integration.mjs',
    websocketEndpoint: websocketUrl,
    restEndpoint: `${gateway}/api/resultados/elecciones/${consistencyElection.id}`,
    electionId: consistencyElection.id,
    consistencyElection: { createStatus: createConsistencyElection.status, ...consistencyElection },
    voterCount: consistencyVoters.length,
    voteRequests: consistencyVoteResponses.length,
    acceptedVotes: consistencyAccepted.length,
    voteResponses: consistencyVoteResponses.map(({ status, body }) => ({ status, body })),
    rest: restAfter.body,
    websocket: wsSnapshot,
    websocketSnapshotsReceived: websocketSnapshots.length,
    persistedVoteCount: finalPersisted.count,
    persistenceQuery: finalPersisted.sql,
    matchedFields: ['eleccionId', 'totalVotos', 'revision'],
    rabbitmqTrace: await rabbitTrace(consistencyElection.id),
    testedAt: new Date().toISOString(),
  }
} catch (error) {
  const failure = { message: error.message, testedAt: new Date().toISOString() }
  for (const section of Object.keys(evidence)) {
    if (evidence[section].verdict === 'PENDING') evidence[section] = { verdict: 'FAIL', ...failure }
  }
  evidence.consistency.rabbitmqTrace ??= await rabbitTrace(traceElectionId)
} finally {
  evidence.completedAt = new Date().toISOString()
  await writeEvidence('alta-perfiles-catalogo.json', evidence.setup)
  await writeEvidence('doble-voto.json', evidence.doubleVote)
  await writeEvidence('consistencia-rest-websocket.json', evidence.consistency)
  console.log(JSON.stringify({ evidenceDir, ...evidence }, null, 2))
}

if ([evidence.setup, evidence.doubleVote, evidence.consistency].some((section) => section.verdict !== 'PASS')) {
  process.exitCode = 1
}
