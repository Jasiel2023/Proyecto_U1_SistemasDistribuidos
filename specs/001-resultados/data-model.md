# Modelo de datos — Resultados

Base: `resultadosdb` (PostgreSQL). Propiedad exclusiva de Resultados. Sin claves foráneas ni joins hacia tablas de Votación, Usuarios o Autenticación. Los UUID externos son identificadores opacos.

## 1. Tipos y convenciones

- IDs: `UUID`.
- Conteos y revisión: `BIGINT` (`long`).
- Instantes: `TIMESTAMPTZ` y `OffsetDateTime` en UTC. `occurredAt` del evento se conserva como dato del mensaje; `ultima_actualizacion`/`procesado_en` son instantes de procesamiento.
- Esquema gobernado por Flyway (`V1__resultados.sql`); Hibernate `validate`.
- Lecturas de snapshot en transacción `REPEATABLE_READ` para coherencia resumen/conteos.

## 2. Tablas

### `resultado_resumen`

| Columna | Tipo | Restricciones |
|---|---|---|
| `eleccion_id` | `uuid` | PK |
| `total_votos` | `bigint` | NOT NULL, DEFAULT 0, CHECK >= 0 |
| `revision` | `bigint` | NOT NULL, DEFAULT 0, CHECK >= 0 |
| `ultima_actualizacion` | `timestamptz` | NOT NULL |

Representa una elección con al menos un voto procesado. No se crea por una consulta REST.

### `resultado_conteo`

| Columna | Tipo | Restricciones |
|---|---|---|
| `eleccion_id` | `uuid` | PK compuesta, FK a `resultado_resumen(eleccion_id)` |
| `candidato_id` | `uuid` | PK compuesta |
| `votos` | `bigint` | NOT NULL, DEFAULT 0, CHECK >= 0 |
| `ultima_actualizacion` | `timestamptz` | NOT NULL |

`PRIMARY KEY (eleccion_id, candidato_id)` garantiza un único conteo por candidato y elección.

### `voto_procesado`

| Columna | Tipo | Restricciones |
|---|---|---|
| `event_id` | `uuid` | PK |
| `voto_id` | `uuid` | NOT NULL |
| `eleccion_id` | `uuid` | NOT NULL |
| `candidato_id` | `uuid` | NOT NULL |
| `occurred_at` | `timestamptz` | NOT NULL |
| `procesado_en` | `timestamptz` | NOT NULL |

`UNIQUE (eleccion_id, voto_id)` implementa la unicidad del voto lógico. La PK implementa la deduplicación por mensaje. Ambas restricciones son la fuente de verdad de la idempotencia, no una comprobación previa de solo lectura.

### `evento_conflicto`

| Columna | Tipo | Restricciones |
|---|---|---|
| `id` | `bigserial` | PK |
| `event_id` | `uuid` | NOT NULL |
| `motivo` | `varchar(40)` | NOT NULL |
| `detalle` | `varchar(500)` | NOT NULL |
| `recibido_en` | `timestamptz` | NOT NULL, DEFAULT now() |

Auditoría de duplicados con payload incompatible. No guarda identidad de votante. No altera conteos.

## 3. Invariantes

1. Para cada fila de `resultado_resumen`: `total_votos = SUM(resultado_conteo.votos)` de esa elección.
2. Cada voto lógico produce como máximo un incremento en `resultado_resumen` y uno en `resultado_conteo`.
3. `revision` solo crece y aumenta exactamente una vez por evento nuevo confirmado.
4. Un evento procesado y sus dos incrementos viven en la misma transacción.
5. Un duplicado (por `eventId` o por voto lógico) no incrementa ni modifica conteos.
6. Un conflicto se registra sin sobrescribir el voto previamente aceptado.

## 4. Flujo transaccional de un evento

```text
BEGIN
  INSERT INTO voto_procesado (...) VALUES (...)
    ON CONFLICT DO NOTHING
  IF filas_insertadas = 1:
    INSERT INTO resultado_resumen (eleccion_id, total_votos, revision, ultima_actualizacion)
      VALUES (?, 1, 1, ?)
      ON CONFLICT (eleccion_id) DO UPDATE
        SET total_votos = resultado_resumen.total_votos + 1,
            revision = resultado_resumen.revision + 1,
            ultima_actualizacion = EXCLUDED.ultima_actualizacion
      RETURNING revision, total_votos, ultima_actualizacion
    INSERT INTO resultado_conteo (eleccion_id, candidato_id, votos, ultima_actualizacion)
      VALUES (?, ?, 1, ?)
      ON CONFLICT (eleccion_id, candidato_id) DO UPDATE
        SET votos = resultado_conteo.votos + 1,
            ultima_actualizacion = EXCLUDED.ultima_actualizacion
      RETURNING votos
  ELSE:
    consultar por event_id y por (eleccion_id, voto_id)
    si payload incompatible: INSERT evento_conflicto + log
  END IF
COMMIT (ACK después de este punto)
```

El upsert atómico evita read-modify-write sin protección. El bloqueo de la fila de resumen serializa las actualizaciones de una misma elección y evita deadlocks entre candidatos, porque todas las transacciones toman primero el resumen.

## 5. Reading model / snapshot

```text
ResultadoSnapshot:
  eleccionId: UUID
  totalVotos: long
  conteos: [{ candidatoId: UUID, votos: long }]
  revision: long
  ultimaActualizacion: OffsetDateTime | null
  disponible: boolean
```

- Sin fila de resumen: snapshot vacío, `disponible=false`, `revision=0`, `totalVotos=0`, `conteos=[]`, `ultimaActualizacion=null`.
- Con fila: se leen resumen y conteos en la misma transacción `REPEATABLE_READ`.
- `disponible` no afirma existencia de la elección en Votación.

## 6. Retención

- `voto_procesado` se conserva indefinidamente en el MVP para mantener la deduplicación ante reentregas tardías y reinicios.
- Una purga futura exigiría una ventana de retención documentada y coordinada con la política de reentrega de Votación; no se implementa.
- `evento_conflicto` puede crecer con duplicados conflictivos; se recomienda monitoreo, sin purga automática en el MVP.
