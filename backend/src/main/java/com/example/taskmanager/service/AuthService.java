package com.example.taskmanager.service;

import com.example.taskmanager.dto.auth.AuthRequest;
import com.example.taskmanager.dto.auth.AuthResponse;
import com.example.taskmanager.dto.auth.UserResponse;
import com.example.taskmanager.entity.UserAccount;

public interface AuthService {

    AuthResponse register(AuthRequest request);

    AuthResponse login(AuthRequest request);

    void logout(String token);

    UserAccount requireUser(String token);

    UserResponse currentUser();
}
