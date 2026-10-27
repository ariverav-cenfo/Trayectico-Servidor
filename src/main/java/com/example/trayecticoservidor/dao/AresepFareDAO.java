package com.example.trayecticoservidor.dao;

import com.example.trayecticoservidor.model.AresepFare;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Repository
public class AresepFareDAO {
    private final JdbcTemplate jdbc;
    private static final String UPSERT = """
        INSERT INTO aresep_fare (route_code, route_name, branch_code, branch_name, fraction_code, fraction_name, average_trip_km, regular_fare, senior_fare, resolution, resolution_date, gazette_number, gazette_supplement, gazette_date, effective_date, case_number, operators, service_type, province)
        VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
        ON CONFLICT (fraction_code) DO UPDATE SET
        route_code = EXCLUDED.route_code,
        route_name = EXCLUDED.route_name,
        branch_code = EXCLUDED.branch_code,
        branch_name = EXCLUDED.branch_name,
        fraction_name = EXCLUDED.fraction_name,
        average_trip_km = EXCLUDED.average_trip_km,
        regular_fare = EXCLUDED.regular_fare,
        senior_fare = EXCLUDED.senior_fare,
        resolution = EXCLUDED.resolution,
        resolution_date = EXCLUDED.resolution_date,
        gazette_number = EXCLUDED.gazette_number,
        gazette_supplement = EXCLUDED.gazette_supplement,
        gazette_date = EXCLUDED.gazette_date,
        effective_date = EXCLUDED.effective_date,
        case_number = EXCLUDED.case_number,
        operators = EXCLUDED.operators,
        service_type = EXCLUDED.service_type,
        province = EXCLUDED.province,
        imported_at = CURRENT_TIMESTAMP
        WHERE EXCLUDED.effective_date >= aresep_fare.effective_date
        """;

    public AresepFareDAO(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    // A failed batch rolls back the entire import. Download and validation happen before this transaction.
    @Transactional
    public void upsertAll(List<AresepFare> fares) {
        jdbc.batchUpdate(UPSERT, fares, 250, (statement, fare) -> {
            statement.setObject(1, fare.routeCode());
            statement.setObject(2, fare.routeName());
            statement.setObject(3, fare.branchCode());
            statement.setObject(4, fare.branchName());
            statement.setObject(5, fare.fractionCode());
            statement.setObject(6, fare.fractionName());
            statement.setObject(7, fare.averageTripKm());
            statement.setObject(8, fare.regularFare());
            statement.setObject(9, fare.seniorFare());
            statement.setObject(10, fare.resolution());
            statement.setObject(11, fare.resolutionDate());
            statement.setObject(12, fare.gazetteNumber());
            statement.setObject(13, fare.gazetteSupplement());
            statement.setObject(14, fare.gazetteDate());
            statement.setObject(15, fare.effectiveDate());
            statement.setObject(16, fare.caseNumber());
            statement.setObject(17, fare.operators());
            statement.setObject(18, fare.serviceType());
            statement.setObject(19, fare.province());
        });
    }
}

