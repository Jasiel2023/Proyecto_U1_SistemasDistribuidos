CREATE TABLE IF NOT EXISTS elecciones (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    titulo VARCHAR(150) NOT NULL,
    fecha DATE NOT NULL DEFAULT CURRENT_DATE,
    hora_inicio TIME NOT NULL DEFAULT CURRENT_TIME,
    hora_fin TIME,
    esta_activa BOOLEAN NOT NULL DEFAULT TRUE
);

ALTER TABLE elecciones
    ADD COLUMN IF NOT EXISTS descripcion VARCHAR(1000) NOT NULL DEFAULT '';

CREATE TABLE IF NOT EXISTS candidatos (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    eleccion_id UUID NOT NULL REFERENCES elecciones(id),
    nombre VARCHAR(150) NOT NULL,
    apellido VARCHAR(150) NOT NULL DEFAULT '',
    partido_politico VARCHAR(150) NOT NULL DEFAULT '',
    activo BOOLEAN NOT NULL DEFAULT TRUE,
    CONSTRAINT uq_candidatos_id_eleccion UNIQUE (id, eleccion_id)
);

ALTER TABLE candidatos
    ADD COLUMN IF NOT EXISTS apellido VARCHAR(150) NOT NULL DEFAULT '',
    ADD COLUMN IF NOT EXISTS partido_politico VARCHAR(150) NOT NULL DEFAULT '',
    ADD COLUMN IF NOT EXISTS activo BOOLEAN NOT NULL DEFAULT TRUE;

CREATE TABLE IF NOT EXISTS votos (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    eleccion_id UUID NOT NULL REFERENCES elecciones(id),
    candidato_id UUID NOT NULL,
    id_usuario UUID NOT NULL,
    hora TIME NOT NULL DEFAULT CURRENT_TIME,
    emitido_en TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    votante_id INTEGER,
    CONSTRAINT uq_votos_usuario_por_eleccion UNIQUE (eleccion_id, id_usuario),
    CONSTRAINT fk_votos_candidato_eleccion FOREIGN KEY (candidato_id, eleccion_id)
        REFERENCES candidatos(id, eleccion_id)
);

ALTER TABLE votos
    ADD COLUMN IF NOT EXISTS votante_id INTEGER;

CREATE UNIQUE INDEX IF NOT EXISTS uq_votos_eleccion_votante
    ON votos (eleccion_id, votante_id)
    WHERE votante_id IS NOT NULL;

CREATE INDEX IF NOT EXISTS idx_votos_eleccion ON votos (eleccion_id);
CREATE INDEX IF NOT EXISTS idx_candidatos_eleccion ON candidatos (eleccion_id);
