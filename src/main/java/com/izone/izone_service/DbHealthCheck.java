package com.izone.izone_service;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class DbHealthCheck {

    private final JdbcTemplate jdbcTemplate;

    @PostConstruct
    public void checkDB() {
        try {
            Integer result = jdbcTemplate.queryForObject("SELECT 1", Integer.class);
            System.out.println("✅ DB CONNECT OK: " + result);
        } catch (Exception e) {
            System.out.println("❌ DB CONNECT FAIL");
            e.printStackTrace();
        }
    }
}