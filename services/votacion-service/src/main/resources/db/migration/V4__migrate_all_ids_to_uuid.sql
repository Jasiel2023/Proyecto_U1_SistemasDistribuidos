DROP TABLE IF EXISTS votos, candidatos, opciones, elecciones, votaciones CASCADE;

CREATE TABLE elecciones (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    titulo VARCHAR(150) NOT NULL,
    fecha DATE NOT NULL,
    hora_inicio TIME NOT NULL,
    hora_fin TIME,
    esta_activa BOOLEAN NOT NULL,
    CONSTRAINT ck_elecciones_periodo CHECK (hora_fin IS NULL OR hora_fin > hora_inicio)
);

CREATE TABLE candidatos (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    eleccion_id UUID NOT NULL REFERENCES elecciones (id),
    nombre VARCHAR(150) NOT NULL,
    apellido VARCHAR(150) NOT NULL,
    partido_politico VARCHAR(150) NOT NULL,
    activo BOOLEAN NOT NULL DEFAULT TRUE,
    UNIQUE (id, eleccion_id)
);

CREATE TABLE votos (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    eleccion_id UUID NOT NULL,
    candidato_id UUID NOT NULL,
    id_usuario UUID NOT NULL,
    hora TIME NOT NULL,
    emitido_en TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_votos_eleccion FOREIGN KEY (eleccion_id) REFERENCES elecciones (id),
    CONSTRAINT fk_votos_candidato_eleccion FOREIGN KEY (candidato_id, eleccion_id)
        REFERENCES candidatos (id, eleccion_id),
    CONSTRAINT uq_votos_usuario_por_eleccion UNIQUE (eleccion_id, id_usuario)
);

CREATE INDEX idx_candidatos_eleccion_id ON candidatos (eleccion_id);
CREATE INDEX idx_votos_eleccion_id ON votos (eleccion_id);
CREATE INDEX idx_votos_candidato_id ON votos (candidato_id);
CREATE INDEX idx_votos_usuario_id ON votos (id_usuario);
