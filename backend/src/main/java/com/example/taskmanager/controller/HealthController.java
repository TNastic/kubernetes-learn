package com.example.taskmanager.controller;

import com.example.taskmanager.dto.DependencyStatusResponse;
import com.example.taskmanager.dto.HealthResponse;
import com.example.taskmanager.service.HealthService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/health")
public class HealthController {

    private final HealthService healthService;

    public HealthController(HealthService healthService) {
        this.healthService = healthService;
    }

    @GetMapping
    public HealthResponse health() {
        return healthService.health();
    }

    @GetMapping("/mysql")
    public DependencyStatusResponse mysql() {
        return healthService.mysql();
    }

    @GetMapping("/redis")
    public DependencyStatusResponse redis() {
        return healthService.redis();
    }

    @GetMapping("/dependencies")
    public HealthResponse dependencies() {
        return healthService.dependencies();
    }
}
