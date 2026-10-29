-- Limpiar tablas en orden inverso a sus dependencias
DELETE FROM stop_displacement;
DELETE FROM displacement;
DELETE FROM branch;
DELETE FROM route;

-- Insertar ruta principal
INSERT INTO route (id, ctp_code, name)
VALUES (1, '0002', 'San José - Sabana - Estadio');

-- Insertar ramal asociado a la ruta
INSERT INTO branch (id, route_id, ctp_code, description)
VALUES (1, 1, '0002-A', 'Por Sabana Sur');

-- Insertar desplazamientos (Ida y Vuelta) para el ramal
INSERT INTO displacement (id, branch_id, ctp_code, direction, active)
VALUES (1, 1, 'DESP-001', 'Ida', true);

INSERT INTO displacement (id, branch_id, ctp_code, direction, active)
VALUES (2, 1, 'DESP-002', 'Vuelta', true);