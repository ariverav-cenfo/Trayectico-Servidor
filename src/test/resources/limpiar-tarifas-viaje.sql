-- Limpia lo que datos-prueba-viaje.sql agrega para SCRUM-92, para no ensuciar otras pruebas.
DELETE FROM equivalencias_ramales WHERE id_ramal_ctp = 9901;
DELETE FROM tarifas_aresep WHERE codigo_ramal LIKE 'PBA-%';