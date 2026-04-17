package com.example.taskmanager.service;

import com.example.taskmanager.dto.DependencyStatusResponse;
import com.example.taskmanager.dto.HealthResponse;

public interface HealthService {

    HealthResponse health();

    DependencyStatusResponse mysql();

    DependencyStatusResponse redis();

    HealthResponse dependencies();
}
