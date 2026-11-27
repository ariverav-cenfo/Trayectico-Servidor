DELETE FROM recorridos WHERE id IN (9901, 9902, 9903);
DELETE FROM ramales WHERE id IN (9901, 9902);
DELETE FROM rutas WHERE id IN (9901, 9902);

INSERT INTO rutas (id, codigo_ctp, id_ctp, nombre) VALUES
                                                       (9901, '9901', 9901, NULL),
                                                       (9902, '9902', 9902, 'RUTA PRUEBA ESTE - OESTE');

INSERT INTO ramales (id, ruta_id, codigo_ctp, id_ctp, descripcion) VALUES
                                                                       (9901, 9901, '9901-A', 9901, 'RAMAL PRUEBA NORTE - SUR'),
                                                                       (9902, 9902, '9902-A', 9902, 'RAMAL PRUEBA ESTE - OESTE');

INSERT INTO recorridos (id, ramal_id, codigo_ctp, id_ctp, direccion, activo, estado_ctp, longitud_m, geometria) VALUES
                                                                                                                    (9901, 9901, '9901-A-1', 9901, '1-2', TRUE, 'false', 11400,
                                                                                                                     ST_Transform(ST_SetSRID(ST_MakeLine(ARRAY[
                                                                                                                         ST_MakePoint(-84.1170, 9.9950),
                                                                                                                         ST_MakePoint(-84.1100, 9.9650),
                                                                                                                         ST_MakePoint(-84.0987, 9.9347)
                                                                                                                         ]), 4326), 5367)),
                                                                                                                    (9903, 9901, '9901-A-2', 9903, '2-1', TRUE, 'false', 11500,
                                                                                                                     ST_Transform(ST_SetSRID(ST_MakeLine(ARRAY[
                                                                                                                         ST_MakePoint(-84.0987, 9.9347),
                                                                                                                         ST_MakePoint(-84.1100, 9.9650),
                                                                                                                         ST_MakePoint(-84.1170, 9.9950)
                                                                                                                         ]), 4326), 5367)),
                                                                                                                    (9902, 9902, '9902-A-1', 9902, '1-2', TRUE, 'activo', 2400,
                                                                                                                     ST_Transform(ST_SetSRID(ST_MakeLine(ARRAY[
                                                                                                                         ST_MakePoint(-84.0841, 9.9339),
                                                                                                                         ST_MakePoint(-84.0950, 9.9335),
                                                                                                                         ST_MakePoint(-84.1061, 9.9332)
                                                                                                                         ]), 4326), 5367));
