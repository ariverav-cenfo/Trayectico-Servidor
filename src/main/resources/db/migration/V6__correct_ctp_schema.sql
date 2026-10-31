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