ALTER TABLE stop
ALTER COLUMN geom TYPE geometry(Point, 5367)
    USING geom::geometry(Point, 5367);

ALTER TABLE displacement
ALTER COLUMN geom TYPE geometry(LineString, 5367)
    USING geom::geometry(LineString, 5367);

ALTER TABLE stop_displacement
    ALTER COLUMN stop_order SET NOT NULL;

ALTER TABLE route
    ADD CONSTRAINT uq_route_ctp_id UNIQUE (ctp_id);

ALTER TABLE branch
    ADD CONSTRAINT uq_branch_ctp_id UNIQUE (ctp_id);

ALTER TABLE displacement
    ADD CONSTRAINT uq_displacement_ctp_id UNIQUE (ctp_id);

ALTER TABLE stop
    ADD CONSTRAINT uq_stop_ctp_id UNIQUE (ctp_id);

CREATE TABLE vertex (
    id BIGSERIAL PRIMARY KEY,
    type VARCHAR NOT NULL,
    stop_id BIGINT REFERENCES stop(id),
    displacement_id BIGINT REFERENCES displacement(id),
    stop_order INTEGER NOT NULL,
    geom GEOMETRY(Point, 5367),

    CONSTRAINT chk_vertex_type
        CHECK (type IN ('stop', 'on_bus')),

    CONSTRAINT chk_vertex_context
        CHECK (
            (type = 'stop' AND displacement_id IS NULL)
                OR
            (type = 'on_bus' AND displacement_id IS NOT NULL)
            )
);

CREATE TABLE edge (
   id BIGSERIAL PRIMARY KEY,
   source BIGINT NOT NULL REFERENCES vertex(id),
   target BIGINT NOT NULL REFERENCES vertex(id),
   type VARCHAR NOT NULL,
   displacement_id BIGINT REFERENCES displacement(id),
   cost DOUBLE PRECISION NOT NULL,
   reverse_cost DOUBLE PRECISION NOT NULL,
   geom geometry(LineString, 5367),

   CONSTRAINT chk_edge_type
      CHECK (type IN ('board', 'ride', 'alight', 'walk'))
);

CREATE INDEX idx_edge_source ON edge (source);
CREATE INDEX idx_edge_target ON edge (target);