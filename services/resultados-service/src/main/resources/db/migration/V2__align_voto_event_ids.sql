DROP TABLE IF EXISTS evento_conflicto, voto_procesado, resultado_conteo, resultado_resumen CASCADE;

CREATE TABLE resultado_resumen (
    eleccion_id BIGINT PRIMARY KEY,
    total_votos BIGINT NOT NULL DEFAULT 0,
    revision BIGINT NOT NULL DEFAULT 0,
    ultima_actualizacion TIMESTAMPTZ NOT NULL
);

CREATE TABLE resultado_conteo (
    eleccion_id BIGINT NOT NULL REFERENCES resultado_resumen (eleccion_id),
    candidato_id BIGINT NOT NULL,
    votos BIGINT NOT NULL DEFAULT 0,
    ultima_actualizacion TIMESTAMPTZ NOT NULL,
    PRIMARY KEY (eleccion_id, candidato_id)
);

CREATE TABLE voto_procesado (
    event_id UUID PRIMARY KEY,
    voto_id BIGINT NOT NULL,
    eleccion_id BIGINT NOT NULL,
    candidato_id BIGINT NOT NULL,
    occurred_at TIMESTAMPTZ NOT NULL,
    procesado_en TIMESTAMPTZ NOT NULL,
    UNIQUE (eleccion_id, voto_id)
);

CREATE INDEX idx_voto_procesado_eleccion ON voto_procesado (eleccion_id);

CREATE TABLE evento_conflicto (
    id BIGSERIAL PRIMARY KEY,
    event_id UUID NOT NULL,
    motivo VARCHAR(40) NOT NULL,
    detalle VARCHAR(500) NOT NULL,
    recibido_en TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_evento_conflicto_event_id ON evento_conflicto (event_id);
