CREATE TABLE usuario (
    id SERIAL PRIMARY KEY,
    id_cuenta INT NOT NULL,
    cedula VARCHAR(13) NOT NULL,
    correo VARCHAR(150) NOT NULL,
    nickname VARCHAR(60) NOT NULL,
    nombre VARCHAR(100) NOT NULL,
    apellido VARCHAR(100) NOT NULL,
    rol VARCHAR(20) NOT NULL DEFAULT 'VOTANTE',
    CONSTRAINT uk_usuario_id_cuenta UNIQUE (id_cuenta),
    CONSTRAINT uk_usuario_cedula UNIQUE (cedula),
    CONSTRAINT uk_usuario_correo UNIQUE (correo),
    CONSTRAINT uk_usuario_nickname UNIQUE (nickname)
);