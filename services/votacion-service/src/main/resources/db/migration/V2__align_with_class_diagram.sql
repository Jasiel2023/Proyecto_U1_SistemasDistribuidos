ALTER TABLE votaciones RENAME TO elecciones;
ALTER TABLE elecciones RENAME COLUMN nombre TO titulo;

ALTER TABLE elecciones ADD COLUMN fecha DATE;
ALTER TABLE elecciones ADD COLUMN hora_inicio TIME;
ALTER TABLE elecciones ADD COLUMN hora_fin TIME;
ALTER TABLE elecciones ADD COLUMN esta_activa BOOLEAN;

UPDATE elecciones
SET fecha = fecha_inicio::DATE,
    hora_inicio = fecha_inicio::TIME,
    hora_fin = fecha_fin::TIME,
    esta_activa = (estado = 'ABIERTA');

ALTER TABLE elecciones ALTER COLUMN fecha SET NOT NULL;
ALTER TABLE elecciones ALTER COLUMN hora_inicio SET NOT NULL;
ALTER TABLE elecciones ALTER COLUMN esta_activa SET NOT NULL;
ALTER TABLE elecciones DROP COLUMN estado;
ALTER TABLE elecciones DROP COLUMN fecha_inicio;
ALTER TABLE elecciones DROP COLUMN fecha_fin;

ALTER TABLE opciones RENAME TO candidatos;
ALTER TABLE candidatos RENAME COLUMN votacion_id TO eleccion_id;
ALTER TABLE candidatos RENAME COLUMN activa TO activo;
ALTER TABLE candidatos ADD COLUMN partido_politico VARCHAR(150) NOT NULL DEFAULT 'Independiente';
ALTER TABLE candidatos ALTER COLUMN partido_politico DROP DEFAULT;

ALTER TABLE votos RENAME COLUMN votacion_id TO eleccion_id;
ALTER TABLE votos RENAME COLUMN opcion_id TO candidato_id;
ALTER TABLE votos RENAME COLUMN usuario_id TO id_usuario;
ALTER TABLE votos ADD COLUMN hora TIME;
UPDATE votos SET hora = emitido_en::TIME;
ALTER TABLE votos ALTER COLUMN hora SET NOT NULL;
