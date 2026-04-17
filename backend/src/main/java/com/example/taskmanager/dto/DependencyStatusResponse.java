package com.example.taskmanager.dto;

public class DependencyStatusResponse {

    private final String name;
    private final String status;
    private final String detail;

    public DependencyStatusResponse(String name, String status, String detail) {
        this.name = name;
        this.status = status;
        this.detail = detail;
    }

    public String getName() {
        return name;
    }

    public String getStatus() {
        return status;
    }

    public String getDetail() {
        return detail;
    }
}
