-- V5__
ALTER TABLE aresep_fare RENAME TO tarifas_aresep;

ALTER TABLE tarifas_aresep RENAME COLUMN route_code TO codigo_ruta;
ALTER TABLE tarifas_aresep RENAME COLUMN route_name TO nombre_ruta;
ALTER TABLE tarifas_aresep RENAME COLUMN branch_code TO codigo_ramal;
ALTER TABLE tarifas_aresep RENAME COLUMN branch_name TO nombre_ramal;
ALTER TABLE tarifas_aresep RENAME COLUMN fraction_code TO codigo_fraccion;
ALTER TABLE tarifas_aresep RENAME COLUMN fraction_name TO nombre_fraccion;
ALTER TABLE tarifas_aresep RENAME COLUMN average_trip_km TO kilometros_promedio_viaje;
ALTER TABLE tarifas_aresep RENAME COLUMN regular_fare TO tarifa_regular;
ALTER TABLE tarifas_aresep RENAME COLUMN senior_fare TO tarifa_adulto_mayor;
ALTER TABLE tarifas_aresep RENAME COLUMN resolution TO resolucion;
ALTER TABLE tarifas_aresep RENAME COLUMN resolution_date TO fecha_resolucion;
ALTER TABLE tarifas_aresep RENAME COLUMN gazette_number TO numero_gaceta;
ALTER TABLE tarifas_aresep RENAME COLUMN gazette_supplement TO suplemento_gaceta;
ALTER TABLE tarifas_aresep RENAME COLUMN gazette_date TO fecha_gaceta;
ALTER TABLE tarifas_aresep RENAME COLUMN effective_date TO fecha_vigencia;
ALTER TABLE tarifas_aresep RENAME COLUMN case_number TO numero_expediente;
ALTER TABLE tarifas_aresep RENAME COLUMN operators TO operadores;
ALTER TABLE tarifas_aresep RENAME COLUMN service_type TO tipo_servicio;
ALTER TABLE tarifas_aresep RENAME COLUMN province TO provincia;
ALTER TABLE tarifas_aresep RENAME COLUMN payment_method TO metodo_pago;
ALTER TABLE tarifas_aresep RENAME COLUMN imported_at TO fecha_importacion;

ALTER TABLE tarifas_aresep
    RENAME CONSTRAINT uq_aresep_fare_fraction
    TO uq_tarifas_aresep_codigo_fraccion;

ALTER TABLE tarifas_aresep
    RENAME CONSTRAINT ck_aresep_fare_regular_nonnegative
    TO ck_tarifas_aresep_tarifa_regular_no_negativa;

ALTER TABLE tarifas_aresep
    RENAME CONSTRAINT ck_aresep_fare_senior_nonnegative
    TO ck_tarifas_aresep_tarifa_adulto_mayor_no_negativa;

ALTER TABLE tarifas_aresep
    RENAME CONSTRAINT ck_aresep_fare_km_nonnegative
    TO ck_tarifas_aresep_kilometros_no_negativos;

ALTER INDEX idx_aresep_fare_route_branch
    RENAME TO idx_tarifas_aresep_ruta_ramal;

COMMENT ON TABLE tarifas_aresep IS
    'Últimas tarifas ARESEP importadas. El importador debe validar la unicidad del código de fracción antes de insertar o actualizar.';

COMMENT ON COLUMN tarifas_aresep.fecha_vigencia IS
    'Inicio de la vigencia tarifaria reportada por ARESEP; es distinta de la fecha de importación.';

COMMENT ON COLUMN tarifas_aresep.operadores IS
    'Lista original de operadores de ARESEP, incluidos los identificadores legales; se conserva sin dividir.';

COMMENT ON COLUMN tarifas_aresep.metodo_pago IS
    'Métodos de pago verificados manualmente, por ejemplo Efectivo y Tarjeta. NULL significa desconocido. El importador ARESEP no sobrescribe este campo.';

-- V2__
ALTER TABLE route RENAME TO rutas;
ALTER TABLE branch RENAME TO ramales;
ALTER TABLE displacement RENAME TO recorridos;
ALTER TABLE stop RENAME TO paradas;
ALTER TABLE stop_displacement RENAME TO paradas_recorridos;

ALTER SEQUENCE route_id_seq RENAME TO rutas_id_seq;
ALTER SEQUENCE branch_id_seq RENAME TO ramales_id_seq;
ALTER SEQUENCE displacement_id_seq RENAME TO recorridos_id_seq;
ALTER SEQUENCE stop_id_seq RENAME TO paradas_id_seq;
ALTER SEQUENCE stop_displacement_id_seq RENAME TO paradas_recorridos_id_seq;

ALTER TABLE rutas RENAME COLUMN ctp_code TO codigo_ctp;
ALTER TABLE rutas RENAME COLUMN ctp_id TO id_ctp;
ALTER TABLE rutas RENAME COLUMN name TO nombre;

ALTER TABLE ramales RENAME COLUMN route_id TO ruta_id;
ALTER TABLE ramales RENAME COLUMN ctp_code TO codigo_ctp;
ALTER TABLE ramales RENAME COLUMN ctp_id TO id_ctp;
ALTER TABLE ramales RENAME COLUMN description TO descripcion;

ALTER TABLE recorridos RENAME COLUMN branch_id TO ramal_id;
ALTER TABLE recorridos RENAME COLUMN ctp_code TO codigo_ctp;
ALTER TABLE recorridos RENAME COLUMN ctp_id TO id_ctp;
ALTER TABLE recorridos RENAME COLUMN direction TO direccion;
ALTER TABLE recorridos RENAME COLUMN length_m TO longitud_m;
ALTER TABLE recorridos RENAME COLUMN ctp_status TO estado_ctp;
ALTER TABLE recorridos RENAME COLUMN active TO activo;
ALTER TABLE recorridos RENAME COLUMN geom TO geometria;

ALTER TABLE paradas RENAME COLUMN ctp_code TO codigo_ctp;
ALTER TABLE paradas RENAME COLUMN ctp_id TO id_ctp;
ALTER TABLE paradas RENAME COLUMN description TO descripcion;
ALTER TABLE paradas RENAME COLUMN province TO provincia;
ALTER TABLE paradas RENAME COLUMN district TO distrito;
ALTER TABLE paradas RENAME COLUMN geom TO geometria;

ALTER TABLE paradas_recorridos RENAME COLUMN displacement_id TO recorrido_id;
ALTER TABLE paradas_recorridos RENAME COLUMN stop_id TO parada_id;
ALTER TABLE paradas_recorridos RENAME COLUMN stop_order TO orden_parada;

ALTER TABLE rutas RENAME CONSTRAINT route_pkey TO pk_rutas;
ALTER TABLE rutas RENAME CONSTRAINT route_ctp_code_key TO uq_rutas_codigo_ctp;

ALTER TABLE ramales RENAME CONSTRAINT branch_pkey TO pk_ramales;
ALTER TABLE ramales RENAME CONSTRAINT branch_route_id_fkey TO fk_ramales_rutas;
ALTER TABLE ramales RENAME CONSTRAINT branch_ctp_code_key TO uq_ramales_codigo_ctp;

ALTER TABLE recorridos RENAME CONSTRAINT displacement_pkey TO pk_recorridos;
ALTER TABLE recorridos RENAME CONSTRAINT displacement_branch_id_fkey TO fk_recorridos_ramales;
ALTER TABLE recorridos RENAME CONSTRAINT displacement_ctp_code_key TO uq_recorridos_codigo_ctp;

ALTER TABLE paradas RENAME CONSTRAINT stop_pkey TO pk_paradas;
ALTER TABLE paradas RENAME CONSTRAINT stop_ctp_code_key TO uq_paradas_codigo_ctp;

ALTER TABLE paradas_recorridos RENAME CONSTRAINT stop_displacement_pkey TO pk_paradas_recorridos;
ALTER TABLE paradas_recorridos RENAME CONSTRAINT stop_displacement_displacement_id_fkey TO fk_paradas_recorridos_recorridos;
ALTER TABLE paradas_recorridos RENAME CONSTRAINT stop_displacement_stop_id_fkey TO fk_paradas_recorridos_paradas;
ALTER TABLE paradas_recorridos RENAME CONSTRAINT uq_stop_displacement_order TO uq_paradas_recorridos_orden;

ALTER INDEX idx_stop_geom RENAME TO idx_paradas_geometria;
ALTER INDEX idx_displacement_geom RENAME TO idx_recorridos_geometria;
ALTER INDEX idx_displacement_active RENAME TO idx_recorridos_activo;

ALTER TABLE paradas
    ALTER COLUMN id_ctp SET NOT NULL,
    ALTER COLUMN geometria TYPE geometry(Point, 5367)
        USING geometria::geometry(Point, 5367);

ALTER TABLE recorridos
    ALTER COLUMN id_ctp SET NOT NULL,
    ALTER COLUMN geometria TYPE geometry(LineString, 5367)
        USING geometria::geometry(LineString, 5367);

ALTER TABLE paradas_recorridos ALTER COLUMN orden_parada SET NOT NULL;

ALTER TABLE rutas ALTER COLUMN id_ctp SET NOT NULL;

ALTER TABLE ramales ALTER COLUMN id_ctp SET NOT NULL;

ALTER TABLE rutas ADD CONSTRAINT uq_rutas_id_ctp UNIQUE (id_ctp);

ALTER TABLE ramales ADD CONSTRAINT uq_ramales_id_ctp UNIQUE (id_ctp);

ALTER TABLE recorridos ADD CONSTRAINT uq_recorridos_id_ctp UNIQUE (id_ctp);

ALTER TABLE paradas ADD CONSTRAINT uq_paradas_id_ctp UNIQUE (id_ctp);


CREATE TABLE vertices (
      id BIGSERIAL PRIMARY KEY,
      tipo VARCHAR NOT NULL,
      parada_id BIGINT NOT NULL REFERENCES paradas(id),
      recorrido_id BIGINT REFERENCES recorridos(id),
      orden_parada INTEGER,
      geometria geometry(Point, 5367),

      CONSTRAINT chk_vertices_tipo
          CHECK (tipo IN ('parada', 'en_bus')),

      CONSTRAINT chk_vertices_contexto
          CHECK (
              (tipo = 'parada'
                  AND recorrido_id IS NULL
                  AND orden_parada IS NULL)
                  OR
              (tipo = 'en_bus'
                  AND recorrido_id IS NOT NULL
                  AND orden_parada IS NOT NULL)
              )
);

CREATE UNIQUE INDEX uq_vertices_parada
    ON vertices (parada_id)
    WHERE tipo = 'parada';

CREATE UNIQUE INDEX uq_vertices_recorrido_orden
    ON vertices (recorrido_id, orden_parada)
    WHERE tipo = 'en_bus';

CREATE INDEX idx_vertices_recorrido
    ON vertices (recorrido_id);


CREATE TABLE aristas (
     id BIGSERIAL PRIMARY KEY,
     source BIGINT NOT NULL REFERENCES vertices(id),
     target BIGINT NOT NULL REFERENCES vertices(id),
     tipo VARCHAR NOT NULL,
     recorrido_id BIGINT REFERENCES recorridos(id),
     cost DOUBLE PRECISION NOT NULL,
     reverse_cost DOUBLE PRECISION NOT NULL,
     geometria geometry(LineString, 5367),

     CONSTRAINT chk_aristas_tipo
         CHECK (tipo IN ('abordar', 'viajar', 'descender', 'caminar')),

     CONSTRAINT chk_aristas_recorrido
         CHECK (
             (tipo = 'viajar' AND recorrido_id IS NOT NULL)
                 OR
             (tipo = 'caminar' AND recorrido_id IS NULL)
                 OR
             (tipo IN ('abordar', 'descender'))
             )
);

CREATE INDEX idx_aristas_source ON aristas (source);
CREATE INDEX idx_aristas_target ON aristas (target);

CREATE INDEX idx_aristas_recorrido
    ON aristas (recorrido_id);