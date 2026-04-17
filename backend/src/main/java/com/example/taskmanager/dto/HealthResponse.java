package com.example.taskmanager.dto;

import java.time.Instant;
import java.util.Collections;
import java.util.List;

public class HealthResponse {

    private final String status;
    private final Instant checkedAt;
    private final List<DependencyStatusResponse> dependencies;

    public HealthResponse(String status, List<DependencyStatusResponse> dependencies) {
        this.status = status;
        this.checkedAt = Instant.now();
        this.dependencies = Collections.unmodifiableList(dependencies);
    }

    public String getStatus() {
        return status;
    }

    public Instant getCheckedAt() {
        return checkedAt;
    }

    public List<DependencyStatusResponse> getDependencies() {
        return dependencies;
    }
}
