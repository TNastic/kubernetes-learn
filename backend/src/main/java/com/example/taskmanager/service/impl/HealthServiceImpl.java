package com.example.taskmanager.service.impl;

import com.example.taskmanager.dto.DependencyStatusResponse;
import com.example.taskmanager.dto.HealthResponse;
import com.example.taskmanager.mapper.HealthMapper;
import com.example.taskmanager.service.HealthService;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.stereotype.Service;

@Service
public class HealthServiceImpl implements HealthService {

    private static final String STATUS_UP = "UP";

    private final HealthMapper healthMapper;
    private final RedisConnectionFactory redisConnectionFactory;

    public HealthServiceImpl(
            HealthMapper healthMapper,
            RedisConnectionFactory redisConnectionFactory) {
        this.healthMapper = healthMapper;
        this.redisConnectionFactory = redisConnectionFactory;
    }

    @Override
    public HealthResponse health() {
        return new HealthResponse(STATUS_UP, Collections.<DependencyStatusResponse>emptyList());
    }

    @Override
    public DependencyStatusResponse mysql() {
        Integer result = healthMapper.selectOne();
        return new DependencyStatusResponse("mysql", STATUS_UP, "SELECT " + result);
    }

    @Override
    public DependencyStatusResponse redis() {
        RedisConnection connection = redisConnectionFactory.getConnection();
        try {
            String result = connection.ping();
            return new DependencyStatusResponse("redis", STATUS_UP, result);
        } finally {
            connection.close();
        }
    }

    @Override
    public HealthResponse dependencies() {
        List<DependencyStatusResponse> statuses = Arrays.asList(mysql(), redis());
        return new HealthResponse(STATUS_UP, statuses);
    }
}
