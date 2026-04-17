package com.example.taskmanager.controller;

import com.example.taskmanager.dto.auth.AuthRequest;
import com.example.taskmanager.dto.auth.AuthResponse;
import com.example.taskmanager.dto.auth.UserResponse;
import com.example.taskmanager.service.AuthService;
import javax.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private static final String BEARER_PREFIX = "Bearer ";

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    public AuthResponse register(@RequestBody AuthRequest request) {
        return authService.register(request);
    }

    @PostMapping("/login")
    public AuthResponse login(@RequestBody AuthRequest request) {
        return authService.login(request);
    }

    @PostMapping("/logout")
    public void logout(@RequestHeader("Authorization") String authorization) {
        authService.logout(authorization.substring(BEARER_PREFIX.length()));
    }

    @GetMapping("/me")
    public UserResponse me(HttpServletRequest request) {
        return authService.currentUser();
    }
}
