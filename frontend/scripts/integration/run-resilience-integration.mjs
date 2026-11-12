import { execFileSync } from 'node:child_process'
import { mkdir, readFile, writeFile } from 'node:fs/promises'
import { fileURLToPath } from 'node:url'
import path from 'node:path'
import { performance } from 'node:perf_hooks'

const scriptDir = path.dirname(fileURLToPath(import.meta.url))
const repoRoot = path.resolve(scriptDir, '../../..')
const evidenceDir = path.resolve(scriptDir, '../../artifacts/resilience')
const base = process.env.GATEWAY_URL ?? 'http://localhost:8080'
const password = process.env.VOTER_TEST_PASSWORD ?? '123456'
const delay = (ms) => new Promise((resolve) => setTimeout(resolve, ms))

async function probe(url, { method = 'GET', body } = {}) {
  const startedAt = new Date().toISOString()
  try {
    const response = await fetch(url, {
      method,
      headers: body === undefined ? {} : { 'Content-Type': 'application/json' },
      ...(body === undefined ? {} : { body: JSON.stringify(body) }),
      signal: AbortSignal.timeout(5000),
    })
    const text = await response.text()
    let parsed = text
    try { parsed = text ? JSON.parse(text) : null } catch {}
    return { url, method, status: response.status, body: parsed, startedAt }
  } catch (error) {
    return { url, method, status: null, error: error.message, startedAt }
  }
}

function composeContainer(service) {
  const id = execFileSync('docker', ['compose', '-f', 'compose.full.yml', 'ps', '-q', service], {
    cwd: repoRoot, encoding: 'utf8', timeout: 15000,
  }).trim()
  if (!id) throw new Error(`Compose no encontró un contenedor activo para ${service}`)
  return id
}

function docker(action, container) {
  return execFileSync('docker', [action, container], {
    cwd: repoRoot, encoding: 'utf8', timeout: 30000,
  }).trim()
}

async function waitForRecovery(probeFn, timeoutMs = 90000) {
  const deadline = Date.now() + timeoutMs
  let last
  while (Date.now() < deadline) {
    last = await probeFn()
    if (last.status === 200) return last
    await delay(1000)
  }
  return last
}

async function writeEvidence(fileName, result) {
  await mkdir(evidenceDir, { recursive: true })
  const file = path.join(evidenceDir, fileName)
  await writeFile(file, `${JSON.stringify(result, null, 2)}\n`, 'utf8')
  return file
}

let electionId = process.env.FAILURE_ELECTION_ID
if (!electionId) {
  try {
    const consistency = JSON.parse(await readFile(
      path.resolve(scriptDir, '../../artifacts/integration/consistencia-rest-websocket.json'), 'utf8'))
    electionId = consistency.electionId
  } catch {
    throw new Error('Define FAILURE_ELECTION_ID o ejecuta primero las pruebas funcionales de integración')
  }
}

const catalogUrl = `${base}/api/elecciones`
const voterProfileUrl = `${base}/api/v1/usuarios/cuenta/3`
const electionResultUrl = `${base}/api/resultados/elecciones/${electionId}`
const checks = [
  {
    service: 'autenticacion-service',
    containerService: 'autenticacion-service',
    direct: () => probe('http://localhost:8081/actuator/health'),
    business: () => probe('http://localhost:8081/api/v1/auth/login', {
      method: 'POST', body: { nickname: 'are', clave: password },
    }),
    public: () => probe(`${base}/api/v1/auth/login`, {
      method: 'POST', body: { nickname: 'are', clave: password },
    }),
  },
  {
    service: 'usuario-service',
    containerService: 'usuario-service',
    direct: () => probe('http://localhost:8084/actuator/health'),
    business: () => probe('http://localhost:8084/api/v1/usuarios/cuenta/3'),
    public: () => probe(voterProfileUrl),
  },
  {
    service: 'votacion-service',
    containerService: 'votacion-service',
    direct: () => probe('http://localhost:8082/actuator/health'),
    business: () => probe('http://localhost:8082/api/elecciones'),
    public: () => probe(catalogUrl),
  },
  {
    service: 'resultados-service',
    containerService: 'resultados-service',
    direct: () => probe('http://localhost:8083/actuator/health'),
    business: () => probe(`http://localhost:8083/api/resultados/elecciones/${electionId}`),
    public: () => probe(electionResultUrl),
  },
  {
    service: 'api-gateway',
    containerService: 'api-gateway',
    direct: () => probe(`${base}/actuator/health`),
    business: null,
    public: () => probe(catalogUrl),
  },
  {
    service: 'frontend',
    containerService: 'frontend',
    direct: () => probe('http://localhost:3000/'),
    business: () => probe('http://localhost:3000/'),
    public: () => probe('http://localhost:3000/'),
  },
]

const results = []
for (const check of checks) {
  const container = composeContainer(check.containerService)
  const result = {
    service: check.service,
    containerId: container.slice(0, 12),
    electionId,
    baseline: null,
    baselineBusiness: null,
    stopCommand: 'docker stop <container-id>',
    afterStopDirect: null,
    afterStopPublic: null,
    otherServicesDuringOutage: [],
    startCommand: 'docker start <container-id>',
    recoveryTimeMs: null,
    dockerStartCommandMs: null,
    recoveredDirect: null,
    recoveredBusiness: null,
    recoveredPublic: null,
    verdict: 'PENDING',
  }
  let stopped = false
  try {
    result.baseline = await check.direct()
    if (result.baseline.status !== 200) throw new Error(`Baseline HTTP ${result.baseline.status}`)
    result.baselineBusiness = check.business ? await check.business() : null
    if (result.baselineBusiness && result.baselineBusiness.status !== 200) {
      throw new Error(`Baseline funcional HTTP ${result.baselineBusiness.status}`)
    }
    const stopRequestedAt = performance.now()
    docker('stop', container)
    stopped = true
    result.stopDurationMs = Math.round(performance.now() - stopRequestedAt)
    const stoppedAt = performance.now()
    await delay(1200)
    result.afterStopDirect = await check.direct()
    result.afterStopPublic = await check.public()
    result.downDetectionMs = Math.round(performance.now() - stoppedAt)
    if (result.afterStopDirect.status === 200) {
      throw new Error(`El endpoint del servicio siguió disponible tras detener ${check.service}`)
    }
    const others = checks.filter((other) => other.service !== check.service)
    result.otherServicesDuringOutage = await Promise.all(others.map(async (other) => {
      const direct = await other.direct()
      const business = other.business ? await other.business() : null
      return {
        service: other.service,
        directHealthStatus: direct.status,
        businessStatus: business?.status ?? null,
        passed: direct.status === 200 && (!business || business.status === 200),
      }
    }))
    const failedOthers = result.otherServicesDuringOutage.filter((other) => !other.passed)
    if (failedOthers.length) {
      throw new Error(`Servicios no afectados que fallaron: ${failedOthers.map((other) => other.service).join(', ')}`)
    }
  } catch (error) {
    result.failure = error.message
  } finally {
    try {
      const startRequestedAt = performance.now()
      docker('start', container)
      result.dockerStartCommandMs = Math.round(performance.now() - startRequestedAt)
      result.recoveredDirect = await waitForRecovery(check.direct)
      result.recoveryTimeMs = Math.round(performance.now() - startRequestedAt)
      result.recoveredBusiness = check.business ? await waitForRecovery(check.business) : null
      result.recoveredPublic = await waitForRecovery(check.public)
      const recovered = result.recoveredDirect?.status === 200
        && (!result.recoveredBusiness || result.recoveredBusiness.status === 200)
        && result.recoveredPublic?.status === 200
      if (recovered && !result.failure) result.verdict = 'PASS'
      else if (recovered) result.verdict = 'FAIL'
      else {
        result.verdict = 'FAIL'
        result.failure ??= `No recuperó: direct=${result.recoveredDirect?.status}, public=${result.recoveredPublic?.status}`
      }
    } catch (error) {
      result.verdict = 'FAIL'
      result.failure ??= `Error durante recuperación: ${error.message}`
    }
  }
  result.completedAt = new Date().toISOString()
  result.evidenceFile = await writeEvidence(`caida-recuperacion-${check.service}.json`, result)
  results.push(result)
  const outage = result.afterStopDirect?.status ?? 'sin respuesta HTTP'
  const otherCount = result.otherServicesDuringOutage.filter((other) => other.passed).length
  console.log(`${result.service}: ${result.verdict} (down=${outage}, recovery=${result.recoveryTimeMs ?? 'n/a'}ms, other services=${otherCount}/${result.otherServicesDuringOutage.length})`)
}

const summary = {
  verdict: results.every((result) => result.verdict === 'PASS') ? 'PASS' : 'FAIL',
  strategy: 'docker stop/start individual; bases de datos y RabbitMQ permanecen activos',
  electionId,
  services: results.map(({ service, verdict, afterStopDirect, recoveredDirect, recoveredPublic, evidenceFile, failure }) => ({
    service, verdict,
    outageStatus: afterStopDirect?.status ?? 'sin respuesta HTTP',
    outageError: afterStopDirect?.error ?? null,
    otherServicesStayedUp: results.find((result) => result.service === service)
      ?.otherServicesDuringOutage?.every((other) => other.passed) ?? false,
    recoveryTimeMs: results.find((result) => result.service === service)?.recoveryTimeMs ?? null,
    recoveryDirectStatus: recoveredDirect?.status ?? null,
    recoveryPublicStatus: recoveredPublic?.status ?? null,
    evidenceFile, ...(failure ? { failure } : {}),
  })),
  completedAt: new Date().toISOString(),
}
await writeEvidence('resumen-caidas-recuperacion.json', summary)
console.log(JSON.stringify(summary, null, 2))
if (summary.verdict !== 'PASS') process.exitCode = 1
