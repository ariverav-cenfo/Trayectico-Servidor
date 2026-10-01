package com.example.trayecticoservidor.DAO;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class HealthDAO {

    private final JdbcTemplate jdbcTemplate;

    public HealthDAO(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public boolean isDatabaseConnected() {
        Integer result = jdbcTemplate.queryForObject("SELECT 1", Integer.class);
        return result != null && result == 1;
    }
}