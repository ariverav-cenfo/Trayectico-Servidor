CREATE TABLE route (
   id BIGSERIAL PRIMARY KEY,
   ctp_code VARCHAR UNIQUE,
   ctp_id INTEGER,
   name TEXT
);

CREATE TABLE branch (
   id BIGSERIAL PRIMARY KEY,
   route_id BIGINT NOT NULL REFERENCES route(id),
   ctp_code VARCHAR UNIQUE,
   ctp_id INTEGER,
   description TEXT
);

CREATE TABLE displacement (
   id BIGSERIAL PRIMARY KEY,
   branch_id BIGINT NOT NULL REFERENCES branch(id),
   ctp_code VARCHAR UNIQUE,
   ctp_id INTEGER,
   direction VARCHAR,
   length_m INTEGER,
   ctp_status VARCHAR,
   active BOOLEAN DEFAULT TRUE,
   geom geometry(LineString, 5367)
);

CREATE TABLE stop (
   id BIGSERIAL PRIMARY KEY,
   ctp_code VARCHAR UNIQUE,
   ctp_id INTEGER,
   description TEXT,
   province VARCHAR,
   canton VARCHAR,
   district VARCHAR,
   geom geometry(Point, 5367)
);

CREATE TABLE stop_displacement (
   id BIGSERIAL PRIMARY KEY,
   displacement_id BIGINT NOT NULL REFERENCES displacement(id),
   stop_id BIGINT NOT NULL REFERENCES stop(id),
   stop_order INTEGER,
   CONSTRAINT uq_stop_displacement_order
       UNIQUE (displacement_id, stop_order)
);

CREATE INDEX idx_stop_geom
    ON stop USING GIST (geom);

CREATE INDEX idx_displacement_geom
    ON displacement USING GIST (geom);

CREATE INDEX idx_displacement_active
    ON displacement (active);