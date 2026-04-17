package com.example.taskmanager.auth;

public class CurrentUser {

    private final Long id;
    private final String username;

    public CurrentUser(Long id, String username) {
        this.id = id;
        this.username = username;
    }

    public Long getId() {
        return id;
    }

    public String getUsername() {
        return username;
    }
}
