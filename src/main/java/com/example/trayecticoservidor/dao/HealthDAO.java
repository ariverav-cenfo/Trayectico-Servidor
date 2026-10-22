package com.example.trayecticoservidor.dao;

import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class HealthDAO {

    private final JdbcTemplate jdbcTemplate;

    public HealthDAO(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public boolean isDatabaseConnected() {
        try {
            Integer result = jdbcTemplate.queryForObject("SELECT 1", Integer.class);
            return result != null && result == 1;
        } catch (DataAccessException e) {
            return false;
        }
    }

    public String getPostgisVersion(){
        return jdbcTemplate.queryForObject("SELECT postgis_version()", String.class);
    }

    public String getPgRoutingVersion() {
        return jdbcTemplate.queryForObject("SELECT * FROM pgr_version()", String.class);
    }

}