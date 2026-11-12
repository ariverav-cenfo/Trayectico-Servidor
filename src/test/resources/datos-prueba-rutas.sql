-- Limpiar tablas en orden inverso a sus dependencias
DELETE FROM paradas_recorridos;
DELETE FROM recorridos;
DELETE FROM ramales;
DELETE FROM rutas;

-- Insertar ruta principal
-- (Se cambia el nombre a NULL para validar que la ruta herede el nombre del primer ramal)
INSERT INTO rutas (id, codigo_ctp, id_ctp, nombre)
VALUES (1, '0002', 1, NULL);

-- Insertar ramales asociados a la ruta (Nombres reales indicados en el JSON)
INSERT INTO ramales (id, ruta_id, codigo_ctp, id_ctp, descripcion)
VALUES (2, 1, '0002-B', 1, 'SAN JOSÉ - B° LA PITAHAYA');

INSERT INTO ramales (id, ruta_id, codigo_ctp, id_ctp, descripcion)
VALUES (1, 1, '0002-A', 2, 'SAN JOSÉ - SABANA CEMENTERIO');

-- Insertar recorridos con el formato real (direccion '1-2', códigos tipo '0002-A-1')
INSERT INTO recorridos (id, ramal_id, codigo_ctp, id_ctp, direccion, activo)
VALUES (1, 1, '0002-A-1', 1, '1-2', true);

INSERT INTO recorridos (id, ramal_id, codigo_ctp, id_ctp, direccion, activo)
VALUES (2, 1, '0002-A-2', 2, '1-2', true);

INSERT INTO recorridos (id, ramal_id, codigo_ctp, id_ctp, direccion, activo)
VALUES (3, 2, '0002-B-1', 3, '1-2', true);