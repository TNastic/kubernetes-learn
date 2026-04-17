package com.example.taskmanager.service.impl;

import com.example.taskmanager.auth.CurrentUser;
import com.example.taskmanager.auth.CurrentUserHolder;
import com.example.taskmanager.common.ApiException;
import com.example.taskmanager.dto.auth.AuthRequest;
import com.example.taskmanager.dto.auth.AuthResponse;
import com.example.taskmanager.dto.auth.UserResponse;
import com.example.taskmanager.entity.UserAccount;
import com.example.taskmanager.mapper.UserMapper;
import com.example.taskmanager.service.AuthService;
import java.time.Duration;
import java.util.UUID;
import javax.servlet.http.HttpServletResponse;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthServiceImpl implements AuthService {

    private static final int MIN_PASSWORD_LENGTH = 6;
    private static final Duration TOKEN_TTL = Duration.ofDays(7);
    private static final String TOKEN_PREFIX = "auth:token:";

    private final UserMapper userMapper;
    private final StringRedisTemplate redisTemplate;
    private final PasswordEncoder passwordEncoder;

    public AuthServiceImpl(
            UserMapper userMapper,
            StringRedisTemplate redisTemplate,
            PasswordEncoder passwordEncoder) {
        this.userMapper = userMapper;
        this.redisTemplate = redisTemplate;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public AuthResponse register(AuthRequest request) {
        validateCredentials(request);
        UserAccount user = newUser(request);
        try {
            userMapper.insert(user);
        } catch (DuplicateKeyException error) {
            throw new ApiException(HttpServletResponse.SC_CONFLICT, "用户名已存在。");
        }
        return issueAuth(user);
    }

    @Override
    public AuthResponse login(AuthRequest request) {
        validateCredentials(request);
        UserAccount user = userMapper.findByUsername(normalize(request.getUsername()));
        if (user == null || !passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw new ApiException(HttpServletResponse.SC_UNAUTHORIZED, "用户名或密码错误。");
        }
        return issueAuth(user);
    }

    @Override
    public void logout(String token) {
        redisTemplate.delete(tokenKey(token));
    }

    @Override
    public UserAccount requireUser(String token) {
        String userId = redisTemplate.opsForValue().get(tokenKey(token));
        if (userId == null) {
            throw new ApiException(HttpServletResponse.SC_UNAUTHORIZED, "登录已失效。");
        }
        UserAccount user = userMapper.findById(Long.valueOf(userId));
        if (user == null) {
            throw new ApiException(HttpServletResponse.SC_UNAUTHORIZED, "用户不存在。");
        }
        return user;
    }

    @Override
    public UserResponse currentUser() {
        CurrentUser user = CurrentUserHolder.get();
        return new UserResponse(user.getId(), user.getUsername());
    }

    private AuthResponse issueAuth(UserAccount user) {
        String token = UUID.randomUUID().toString().replace("-", "");
        redisTemplate.opsForValue().set(tokenKey(token), String.valueOf(user.getId()), TOKEN_TTL);
        return new AuthResponse(token, new UserResponse(user.getId(), user.getUsername()));
    }

    private UserAccount newUser(AuthRequest request) {
        UserAccount user = new UserAccount();
        user.setUsername(normalize(request.getUsername()));
        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        return user;
    }

    private void validateCredentials(AuthRequest request) {
        if (request == null || isBlank(request.getUsername())) {
            throw new ApiException(HttpServletResponse.SC_BAD_REQUEST, "用户名不能为空。");
        }
        if (request.getPassword() == null || request.getPassword().length() < MIN_PASSWORD_LENGTH) {
            throw new ApiException(HttpServletResponse.SC_BAD_REQUEST, "密码至少 6 位。");
        }
    }

    private String normalize(String value) {
        return value.trim().toLowerCase();
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    private String tokenKey(String token) {
        return TOKEN_PREFIX + token;
    }
}
