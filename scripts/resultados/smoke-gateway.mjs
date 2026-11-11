#!/usr/bin/env node
import { randomUUID } from 'node:crypto';
import { publicarFixtures } from './publicar-fixtures.mjs';

const args = parseArgs(process.argv.slice(2));
const base = args.base ?? 'http://localhost:3000';
const eleccion = args.eleccion ?? randomUUID();
const votos = Number(args.votos ?? 6);
const duplicados = Number(args.duplicados ?? 2);
const timeoutMs = Number(args.timeout ?? 20000);

const clientes = [];
try {
  clientes.push(await conectar(base, eleccion));
  clientes.push(await conectar(base, eleccion));
  await sleep(700);

  await publicarFixtures({
    eleccion,
    candidatos: 2,
    votos,
    duplicados,
    url: args.rabbit ?? 'http://localhost:15672'
  });

  await esperarHasta(() => clientes.every((cliente) => cliente.mensajes.some((m) => m.totalVotos === votos)), timeoutMs);
  const snapshots = clientes.map((cliente) => cliente.mensajes.filter((m) => m.totalVotos === votos).at(-1));
  for (const snapshot of snapshots) {
    if (snapshot.eleccionId !== eleccion || snapshot.totalVotos !== votos) {
      throw new Error(`Snapshot WebSocket inesperado: ${JSON.stringify(snapshot)}`);
    }
  }

  const respuesta = await fetch(`${base}/api/resultados/elecciones/${eleccion}`);
  if (!respuesta.ok) throw new Error(`REST respondió ${respuesta.status}`);
  const rest = await respuesta.json();
  if (rest.totalVotos !== votos || rest.revision !== votos) {
    throw new Error(`REST inesperado: ${JSON.stringify(rest)}`);
  }

  console.log('SMOKE OK');
  console.log(`Base: ${base}`);
  console.log(`Elección: ${eleccion}`);
  console.log(`Total REST: ${rest.totalVotos}; revisión: ${rest.revision}`);
  console.log(`Clientes WebSocket: ${clientes.length}; total recibido: ${snapshots.map((s) => s.totalVotos).join(', ')}`);
  console.log(`Duplicados publicados sin incremento: ${duplicados}`);
} catch (error) {
  console.error(`SMOKE FAIL: ${error.message}`);
  process.exitCode = 1;
} finally {
  for (const cliente of clientes) {
    try {
      cliente.ws.close();
    } catch {
      // sin acción
    }
  }
}

function conectar(baseUrl, eleccionId) {
  const wsUrl = baseUrl.replace(/^http/, 'ws') + '/ws';
  return new Promise((resolve, reject) => {
    const ws = new WebSocket(wsUrl);
    const mensajes = [];
    const timer = setTimeout(() => reject(new Error(`Timeout conectando a ${wsUrl}`)), timeoutMs);
    ws.onopen = () => ws.send('CONNECT\naccept-version:1.2\nheart-beat:0,0\n\n\0');
    ws.onerror = () => {
      clearTimeout(timer);
      reject(new Error(`Error WebSocket en ${wsUrl}`));
    };
    ws.onmessage = (evento) => {
      const data = evento.data.toString();
      const comando = data.split('\n')[0];
      if (comando === 'CONNECTED') {
        clearTimeout(timer);
        ws.send(`SUBSCRIBE\nid:sub-0\ndestination:/topic/resultados/elecciones/${eleccionId}\n\n\0`);
        resolve({ ws, mensajes });
      } else if (comando === 'MESSAGE') {
        const cuerpo = data.split('\n\n')[1]?.replace(/\0/g, '');
        if (cuerpo) mensajes.push(JSON.parse(cuerpo));
      }
    };
  });
}

async function esperarHasta(condicion, limiteMs) {
  const inicio = Date.now();
  while (!condicion()) {
    if (Date.now() - inicio > limiteMs) throw new Error('Timeout esperando notificaciones WebSocket.');
    await sleep(100);
  }
}

function sleep(ms) {
  return new Promise((resolve) => setTimeout(resolve, ms));
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
