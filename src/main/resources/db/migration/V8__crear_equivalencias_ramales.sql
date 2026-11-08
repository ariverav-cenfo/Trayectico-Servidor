-- SCRUM-91: equivalencias entre los ramales del CTP y los de ARESEP.
-- Esta migración solo crea la estructura. Los datos los carga el importador
-- de equivalencias, después de importar las rutas y las tarifas (ver README).

CREATE TABLE equivalencias_ramales (
                                       id                      BIGSERIAL,
                                       id_ramal_ctp            INTEGER       NOT NULL,
                                       codigo_ruta_aresep      VARCHAR       NOT NULL,
                                       codigo_ramal_aresep     VARCHAR       NOT NULL,
                                       tipo_coincidencia       VARCHAR       NOT NULL,
                                       puntaje                 NUMERIC(3, 2) NOT NULL,
                                       acepta_pago_electronico BOOLEAN,

                                       CONSTRAINT pk_equivalencias_ramales
                                           PRIMARY KEY (id),
                                       CONSTRAINT fk_equivalencias_ramales_ramales
                                           FOREIGN KEY (id_ramal_ctp) REFERENCES ramales (id_ctp),
                                       CONSTRAINT uq_equivalencias_ramales_id_ramal_ctp
                                           UNIQUE (id_ramal_ctp),
                                       CONSTRAINT ck_equivalencias_ramales_tipo_coincidencia
                                           CHECK (tipo_coincidencia IN ('NOMBRE_EXACTO', 'NOMBRE_AMBIGUO',
                                                                        'NOMBRE_PARECIDO', 'REVISAR', 'SIN_RUTA_ARESEP')),
                                       CONSTRAINT ck_equivalencias_ramales_puntaje
                                           CHECK (puntaje BETWEEN 0 AND 1)
);

COMMENT ON TABLE equivalencias_ramales IS
    'Equivalencia entre cada ramal del CTP y su ramal en ARESEP, generada por el cruce de SCRUM-36.';

COMMENT ON COLUMN equivalencias_ramales.tipo_coincidencia IS
    'Cómo se encontró la equivalencia. Acepta todos los tipos del CSV para que SCRUM-123 cargue después los confirmados a mano.';

COMMENT ON COLUMN equivalencias_ramales.puntaje IS
    'Similitud entre los nombres normalizados del CTP y de ARESEP, de 0 a 1.';

COMMENT ON COLUMN equivalencias_ramales.acepta_pago_electronico IS
    'Si el ramal acepta pago electrónico. NULL significa no verificado; se llena a mano desde el panel E6 (SCRUM-37).';

-- El método de pago ahora se guarda por ramal en equivalencias_ramales.acepta_pago_electronico.
ALTER TABLE tarifas_aresep DROP COLUMN metodo_pago;