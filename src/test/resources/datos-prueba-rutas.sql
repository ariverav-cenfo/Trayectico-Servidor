-- Limpiar tablas en orden inverso a sus dependencias.
-- equivalencias_ramales va primero porque tiene llave foránea a ramales.
DELETE FROM equivalencias_ramales;
DELETE FROM tarifas_aresep;
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

-- SCRUM-116: tarifa de ARESEP y equivalencia solo para 0002-A (id_ctp 2).
-- 0002-B queda sin equivalencia, así que su tarifa debe salir en null.
INSERT INTO tarifas_aresep (codigo_ruta, nombre_ruta, codigo_ramal, nombre_ramal, codigo_fraccion,
                            nombre_fraccion, tarifa_regular, tarifa_adulto_mayor, fecha_vigencia)
VALUES ('02', 'SAN JOSE-SABANA-CEMENTERIO', 'R0012', 'SAN JOSE-SABANA-CEMENTERIO', 'T0012',
        'SAN JOSE-SABANA-CEMENTERIO', 240.00, 0.00, '2026-09-04');

INSERT INTO equivalencias_ramales (id_ramal_ctp, codigo_ruta_aresep, codigo_ramal_aresep, tipo_coincidencia, puntaje)
VALUES (2, '02', 'R0012', 'NOMBRE_EXACTO', 1.00);