-- equivalencias_ramales apunta a ramales, así que se borra antes (SCRUM-92).
DELETE FROM equivalencias_ramales;
DELETE FROM tarifas_aresep WHERE codigo_ramal LIKE 'PBA-%';
DELETE FROM paradas_recorridos;
DELETE FROM recorridos;
DELETE FROM ramales;
DELETE FROM rutas;
DELETE FROM paradas;

INSERT INTO rutas (id, codigo_ctp, id_ctp, nombre) VALUES
                                                       (9901, '9901', 9901, NULL),                         -- sin nombre: ejercita el fallback al ramal
                                                       (9902, '9902', 9902, 'RUTA PRUEBA ESTE - OESTE');

INSERT INTO ramales (id, ruta_id, codigo_ctp, id_ctp, descripcion) VALUES
                                                                       (9901, 9901, '9901-A', 9901, 'RAMAL PRUEBA NORTE - SUR'),
                                                                       (9902, 9902, '9902-A', 9902, 'RAMAL PRUEBA ESTE - OESTE');

INSERT INTO paradas (codigo_ctp, descripcion, geometria) VALUES
                                                             ('PBA-99901-001', 'PARADA PRUEBA 1 ORIGEN',  ST_Transform(ST_SetSRID(ST_MakePoint(-84.1170, 9.9950), 4326), 5367)),
                                                             ('PBA-99901-002', 'PARADA PRUEBA 1 DESTINO', ST_Transform(ST_SetSRID(ST_MakePoint(-84.0987, 9.9347), 4326), 5367)),
                                                             ('PBA-99902-001', 'PARADA PRUEBA 2 ORIGEN',  ST_Transform(ST_SetSRID(ST_MakePoint(-84.0841, 9.9339), 4326), 5367)),
                                                             ('PBA-99902-002', 'PARADA PRUEBA 2 DESTINO', ST_Transform(ST_SetSRID(ST_MakePoint(-84.1061, 9.9332), 4326), 5367));

INSERT INTO recorridos (id, ramal_id, codigo_ctp, id_ctp, direccion, activo, estado_ctp, longitud_m, geometria) VALUES
                                                                                                                    (9901, 9901, '9901-A-1', 9901, '1-2', TRUE, 'activo', 11400,
                                                                                                                     ST_Transform(ST_SetSRID(ST_MakeLine(ARRAY[
                                                                                                                                                             ST_MakePoint(-84.1170, 9.9950),
                                                                                                                                                         ST_MakePoint(-84.1100, 9.9650),
                                                                                                                                                         ST_MakePoint(-84.0987, 9.9347)
                                                                                                                                                             ]), 4326), 5367)),
                                                                                                                    (9902, 9902, '9902-A-1', 9902, '1-2', TRUE, 'activo', 2400,
                                                                                                                     ST_Transform(ST_SetSRID(ST_MakeLine(ARRAY[
                                                                                                                                                             ST_MakePoint(-84.0841, 9.9339),
                                                                                                                                                         ST_MakePoint(-84.0950, 9.9335),
                                                                                                                                                         ST_MakePoint(-84.1061, 9.9332)
                                                                                                                                                             ]), 4326), 5367));

INSERT INTO paradas_recorridos (parada_id, recorrido_id, orden_parada) VALUES
                                                                           ((SELECT id FROM paradas WHERE codigo_ctp = 'PBA-99901-001'), 9901, 1),
                                                                           ((SELECT id FROM paradas WHERE codigo_ctp = 'PBA-99901-002'), 9901, 2),
                                                                           ((SELECT id FROM paradas WHERE codigo_ctp = 'PBA-99902-001'), 9902, 1),
                                                                           ((SELECT id FROM paradas WHERE codigo_ctp = 'PBA-99902-002'), 9902, 2);

-- SCRUM-92: el ramal 9901-A tiene tarifa de ARESEP (una sola fracción, tarifa exacta) y acepta pago electrónico.
-- El ramal 9902-A no tiene equivalencia: su tramo sale con "tarifa no disponible" y el viaje sin costo total.
INSERT INTO tarifas_aresep (codigo_ruta, nombre_ruta, codigo_ramal, nombre_ramal, codigo_fraccion, nombre_fraccion,
                            tarifa_regular, tarifa_adulto_mayor, fecha_vigencia) VALUES
    ('PBA-9901', 'RUTA PRUEBA', 'PBA-R9901', 'RAMAL PRUEBA NORTE - SUR', 'PBA-F9901', 'NORTE - SUR', 355, 0, DATE '2026-01-01');

INSERT INTO equivalencias_ramales (id_ramal_ctp, codigo_ruta_aresep, codigo_ramal_aresep, tipo_coincidencia, puntaje, acepta_pago_electronico) VALUES
    (9901, 'PBA-9901', 'PBA-R9901', 'NOMBRE_EXACTO', 1.00, TRUE);