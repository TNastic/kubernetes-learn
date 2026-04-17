package com.example.taskmanager.auth;

import com.example.taskmanager.common.ApiException;
import com.example.taskmanager.entity.UserAccount;
import com.example.taskmanager.service.AuthService;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class AuthInterceptor implements HandlerInterceptor {

    private static final String AUTHORIZATION = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";

    private final AuthService authService;

    public AuthInterceptor(AuthService authService) {
        this.authService = authService;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        UserAccount user = authService.requireUser(extractToken(request));
        CurrentUserHolder.set(new CurrentUser(user.getId(), user.getUsername()));
        return true;
    }

    @Override
    public void afterCompletion(
            HttpServletRequest request,
            HttpServletResponse response,
            Object handler,
            Exception ex) {
        CurrentUserHolder.clear();
    }

    private String extractToken(HttpServletRequest request) {
        String header = request.getHeader(AUTHORIZATION);
        if (header == null || !header.startsWith(BEARER_PREFIX)) {
            throw new ApiException(HttpServletResponse.SC_UNAUTHORIZED, "请先登录。");
        }
        return header.substring(BEARER_PREFIX.length());
    }
}
