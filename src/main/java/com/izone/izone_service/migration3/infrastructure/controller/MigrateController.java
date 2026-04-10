package com.izone.izone_service.migration3.infrastructure.controller;

import com.izone.izone_service.migration3.application.service.MigrationOrchestrator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller dieu khien qua trinh migration du lieu tu V1 sang V2.
 * Endpoint: POST /api/migration/v1/start
 */
@Slf4j
@RestController
@RequiredArgsConstructor
public class MigrateController {

    private final MigrationOrchestrator migrationOrchestrator;

    @PostMapping("/api/migrate")
    public String startMigration() {
        migrationOrchestrator.migrate();
        return "Migration started";
    }
}