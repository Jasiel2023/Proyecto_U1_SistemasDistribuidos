CREATE TABLE resultado_resumen (
    eleccion_id UUID PRIMARY KEY,
    total_votos BIGINT NOT NULL DEFAULT 0,
    revision BIGINT NOT NULL DEFAULT 0,
    ultima_actualizacion TIMESTAMPTZ NOT NULL,
    CONSTRAINT ck_resultado_resumen_total CHECK (total_votos >= 0),
    CONSTRAINT ck_resultado_resumen_revision CHECK (revision >= 0)
);

CREATE TABLE resultado_conteo (
    eleccion_id UUID NOT NULL,
    candidato_id UUID NOT NULL,
    votos BIGINT NOT NULL DEFAULT 0,
    ultima_actualizacion TIMESTAMPTZ NOT NULL,
    CONSTRAINT pk_resultado_conteo PRIMARY KEY (eleccion_id, candidato_id),
    CONSTRAINT fk_resultado_conteo_resumen FOREIGN KEY (eleccion_id)
        REFERENCES resultado_resumen (eleccion_id),
    CONSTRAINT ck_resultado_conteo_votos CHECK (votos >= 0)
);

CREATE TABLE voto_procesado (
    event_id UUID PRIMARY KEY,
    voto_id UUID NOT NULL,
    eleccion_id UUID NOT NULL,
    candidato_id UUID NOT NULL,
    occurred_at TIMESTAMPTZ NOT NULL,
    procesado_en TIMESTAMPTZ NOT NULL,
    CONSTRAINT uq_voto_procesado_logico UNIQUE (eleccion_id, voto_id)
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
