#!/usr/bin/env node
import { createHash } from 'node:crypto';
import { pathToFileURL } from 'node:url';

export function generarEventos({ eleccion, candidatos = 3, votos = 10, duplicados = 0, seed = eleccion }) {
  const idsCandidatos = resolverCandidatos(candidatos, seed);
  const eventos = [];
  for (let i = 0; i < votos; i++) {
    eventos.push({
      eventId: idReproducible(`${seed}:evento:${i}`),
      schemaVersion: 1,
      votoId: idReproducible(`${seed}:voto:${i}`),
      eleccionId: eleccion,
      candidatoId: idsCandidatos[i % idsCandidatos.length],
      occurredAt: new Date(Date.UTC(2026, 0, 1, 0, 0, i)).toISOString()
    });
  }
  for (let i = 0; i < duplicados; i++) {
    eventos.push(structuredClone(eventos[0]));
  }
  return { eventos, candidatos: idsCandidatos };
}

export async function publicarEventos(eventos, {
  url = 'http://localhost:15672',
  exchange = 'votacion.exchange',
  routingKey = 'voto.emitido',
  user = 'guest',
  password = 'guest',
  allowRemote = false
} = {}) {
  const base = new URL(url);
  if (!['localhost', '127.0.0.1', '::1'].includes(base.hostname) && !allowRemote) {
    throw new Error('Host remoto bloqueado. Use --allow-remote solo en entornos de desarrollo autorizados.');
  }
  const endpoint = new URL(
    `/api/exchanges/${encodeURIComponent('/')}/${encodeURIComponent(exchange)}/publish`,
    base
  );
  const auth = 'Basic ' + Buffer.from(`${user}:${password}`).toString('base64');
  for (const evento of eventos) {
    const respuesta = await fetch(endpoint, {
      method: 'POST',
      headers: { Authorization: auth, 'Content-Type': 'application/json' },
      body: JSON.stringify({
        properties: { contentType: 'application/json', contentEncoding: 'UTF-8' },
        routing_key: routingKey,
        payload: JSON.stringify(evento),
        payload_encoding: 'string'
      })
    });
    if (!respuesta.ok) {
      throw new Error(`RabbitMQ respondió ${respuesta.status}: ${await respuesta.text()}`);
    }
    const cuerpo = await respuesta.json();
    if (!cuerpo.routed) {
      throw new Error(`Evento no enrutado (${evento.eventId}). Verifique exchange/cola.`);
    }
  }
  return eventos.length;
}

export async function publicarFixtures(opciones) {
  if (!opciones.eleccion) throw new Error('Falta --eleccion.');
  const { eventos, candidatos } = generarEventos({
    eleccion: opciones.eleccion,
    candidatos: opciones.candidatos ?? 3,
    votos: opciones.votos ?? 10,
    duplicados: opciones.duplicados ?? 0,
    seed: opciones.seed ?? opciones.eleccion
  });
  await publicarEventos(eventos, opciones);
  return { eventos, candidatos };
}

function main() {
  const args = parseArgs(process.argv.slice(2));
  if (args.help || !args.eleccion) {
    console.log(`Uso: node scripts/resultados/publicar-fixtures.mjs --eleccion <uuid> [opciones]

Opciones:
  --candidatos <n|uuid,uuid,...>  Cantidad o lista de candidatos (por defecto 3).
  --votos <n>                     Votos a publicar (por defecto 10).
  --url <url>                     API de gestión RabbitMQ (http://localhost:15672).
  --exchange <nombre>             Exchange (votacion.exchange).
  --routing-key <clave>           Routing key (voto.emitido).
  --user <usuario>                Usuario RabbitMQ de desarrollo (guest).
  --password <clave>              Clave RabbitMQ de desarrollo (guest).
  --seed <texto>                  Semilla para IDs reproducibles (demo).
  --duplicados <n>                Republica el primer evento n veces para probar idempotencia.
  --allow-remote                  Permite un host remoto distinto de localhost.

Publica VotoEmitidoEvent v1 por RabbitMQ. No usa endpoints REST de escritura.`);
    process.exit(args.help ? 0 : 1);
  }

  const votos = numeroPositivo(args.votos ?? '10', '--votos', 1);
  const duplicados = numeroPositivo(args.duplicados ?? '0', '--duplicados', 0);
  const cantidadCandidatos = Number(args.candidatos ?? '3');
  const candidatos = Number.isInteger(cantidadCandidatos)
    ? cantidadCandidatos
    : String(args.candidatos).split(',').map((item) => item.trim()).filter(Boolean);
  publicarFixtures({
    eleccion: args.eleccion,
    candidatos,
    votos,
    duplicados,
    seed: args.seed ?? args.eleccion,
    url: args.url,
    exchange: args.exchange,
    routingKey: args['routing-key'],
    user: args.user,
    password: args.password,
    allowRemote: Boolean(args['allow-remote'])
  }).then(({ eventos, candidatos: ids }) => {
    console.log(`Publicados ${eventos.length} eventos (${duplicados} duplicados) en ${args.exchange ?? 'votacion.exchange'}/${args['routing-key'] ?? 'voto.emitido'}.`);
    console.log(`Elección: ${args.eleccion}`);
    console.log(`Candidatos: ${ids.join(', ')}`);
    console.log(`Evento base: ${eventos[0].eventId}; voto base: ${eventos[0].votoId}`);
  }).catch((error) => {
    console.error(error.message);
    process.exit(1);
  });
}

function parseArgs(argv) {
  const resultado = {};
  for (let i = 0; i < argv.length; i++) {
    const actual = argv[i];
    if (!actual.startsWith('--')) continue;
    const clave = actual.slice(2);
    const siguiente = argv[i + 1];
    if (siguiente === undefined || siguiente.startsWith('--')) {
      resultado[clave] = true;
    } else {
      resultado[clave] = siguiente;
      i++;
    }
  }
  return resultado;
}

function resolverCandidatos(valor, semilla) {
  if (typeof valor === 'number' || /^\d+$/.test(String(valor))) {
    const cantidad = numeroPositivo(String(valor), '--candidatos', 1);
    return Array.from({ length: cantidad },
      (_, i) => idReproducible(`${semilla}:candidato:${i}`));
  }
  const lista = Array.isArray(valor)
    ? valor
    : String(valor).split(',').map((item) => item.trim()).filter(Boolean);
  if (lista.length === 0 || lista.some((item) => !esUuid(item))) {
    throw new Error('--candidatos debe ser un entero positivo o una lista de UUID separados por coma.');
  }
  return lista;
}

function idReproducible(texto) {
  if (esUuid(texto)) return texto;
  const hex = createHash('sha256').update(texto).digest('hex');
  const variante = ((parseInt(hex[16], 16) & 0x3) | 0x8).toString(16);
  return `${hex.slice(0, 8)}-${hex.slice(8, 12)}-4${hex.slice(13, 16)}-${variante}${hex.slice(17, 20)}-${hex.slice(20, 32)}`;
}

function esUuid(valor) {
  return /^[0-9a-f]{8}-[0-9a-f]{4}-[1-5][0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$/i.test(valor);
}

function numeroPositivo(valor, opcion, minimo) {
  const numero = Number(valor);
  if (!Number.isInteger(numero) || numero < minimo) {
    throw new Error(`${opcion} debe ser un entero >= ${minimo}.`);
  }
  return numero;
}

if (import.meta.url === pathToFileURL(process.argv[1]).href) {
  main();
}
