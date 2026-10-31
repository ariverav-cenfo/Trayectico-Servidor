-- Limpiar tablas en orden inverso a sus dependencias
DELETE FROM stop_displacement;
DELETE FROM displacement;
DELETE FROM branch;
DELETE FROM route;

-- Insertar ruta principal
-- (Se cambia el nombre a NULL para validar que la ruta herede el nombre del primer ramal)
INSERT INTO route (id, ctp_code, name)
VALUES (1, '0002', NULL);

-- Insertar ramales asociados a la ruta (Nombres reales indicados en el JSON)
INSERT INTO branch (id, route_id, ctp_code, description)
VALUES (1, 1, '0002-A', 'SAN JOSÉ - SABANA CEMENTERIO');

INSERT INTO branch (id, route_id, ctp_code, description)
VALUES (2, 1, '0002-B', 'SAN JOSÉ - B LA PITAHAYA');

-- Insertar desplazamientos con el formato real (direction '1-2', códigos tipo '0002-A-1')
INSERT INTO displacement (id, branch_id, ctp_code, direction, active)
VALUES (1, 1, '0002-A-1', '1-2', true);

INSERT INTO displacement (id, branch_id, ctp_code, direction, active)
VALUES (2, 1, '0002-A-2', '1-2', true);

INSERT INTO displacement (id, branch_id, ctp_code, direction, active)
VALUES (3, 2, '0002-B-1', '1-2', true);