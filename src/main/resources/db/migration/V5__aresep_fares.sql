-- Latest imported tariff for each ARESEP fraction; amounts are in Costa Rican colones.
-- ARESEP codes are preserved as text and do not identify CTP records directly.
CREATE TABLE aresep_fare (
   id BIGSERIAL PRIMARY KEY,
   route_code VARCHAR NOT NULL,
   route_name TEXT NOT NULL,
   branch_code VARCHAR NOT NULL,
   branch_name TEXT NOT NULL,
   fraction_code VARCHAR NOT NULL,
   fraction_name TEXT NOT NULL,
   average_trip_km NUMERIC(12, 2),
   regular_fare NUMERIC(12, 2) NOT NULL,
   senior_fare NUMERIC(12, 2) NOT NULL,
   resolution TEXT,
   resolution_date DATE,
   gazette_number VARCHAR,
   gazette_supplement VARCHAR,
   gazette_date DATE,
   effective_date DATE NOT NULL,
   case_number TEXT,
   operators TEXT,
   service_type TEXT,
   province TEXT,
   payment_method TEXT,
   imported_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
   CONSTRAINT uq_aresep_fare_fraction UNIQUE (fraction_code),
   CONSTRAINT ck_aresep_fare_regular_nonnegative CHECK (regular_fare >= 0),
   CONSTRAINT ck_aresep_fare_senior_nonnegative CHECK (senior_fare >= 0),
   CONSTRAINT ck_aresep_fare_km_nonnegative CHECK (average_trip_km >= 0)
);

CREATE INDEX idx_aresep_fare_route_branch
    ON aresep_fare (route_code, branch_code);

COMMENT ON TABLE aresep_fare IS
    'Latest imported ARESEP tariffs. Importer must validate fraction-code uniqueness before upserting.';

COMMENT ON COLUMN aresep_fare.effective_date IS
    'Start of tariff validity reported by ARESEP; distinct from the import time.';

COMMENT ON COLUMN aresep_fare.operators IS
    'Original ARESEP operator list, including legal identifiers; preserved without splitting.';

COMMENT ON COLUMN aresep_fare.payment_method IS
    'Manually verified payment methods, for example Efectivo, Tarjeta. NULL means unknown. Not overwritten by the ARESEP importer.';
