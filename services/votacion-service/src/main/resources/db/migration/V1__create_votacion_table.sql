CREATE TABLE votacion (
    id SERIAL PRIMARY KEY,
    eleccion_id UUID NOT NULL,
    candidato_id UUID NOT NULL,
    votante_id INTEGER NOT NULL,
    fecha_voto TIMESTAMPTZ NOT NULL DEFAULT now(),
    ip_origen VARCHAR(45),
    dispositivo VARCHAR(100),
    CONSTRAINT uk_votacion_votante_eleccion UNIQUE (eleccion_id, votante_id)
);

CREATE INDEX idx_votacion_eleccion ON votacion (eleccion_id);
CREATE INDEX idx_votacion_candidato ON votacion (candidato_id);
CREATE INDEX idx_votacion_votante ON votacion (votante_id);
