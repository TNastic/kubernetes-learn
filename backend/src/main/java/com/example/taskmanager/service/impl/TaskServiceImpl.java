package com.example.taskmanager.service.impl;

import com.example.taskmanager.auth.CurrentUser;
import com.example.taskmanager.auth.CurrentUserHolder;
import com.example.taskmanager.common.ApiException;
import com.example.taskmanager.dto.task.TaskRequest;
import com.example.taskmanager.dto.task.TaskResponse;
import com.example.taskmanager.entity.TaskItem;
import com.example.taskmanager.mapper.TaskMapper;
import com.example.taskmanager.service.TaskService;
import java.util.ArrayList;
import java.util.List;
import javax.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Service;

@Service
public class TaskServiceImpl implements TaskService {

    private static final int TITLE_MAX_LENGTH = 120;
    private static final String STATUS_TODO = "TODO";
    private static final String STATUS_DONE = "DONE";

    private final TaskMapper taskMapper;

    public TaskServiceImpl(TaskMapper taskMapper) {
        this.taskMapper = taskMapper;
    }

    @Override
    public List<TaskResponse> list(String status) {
        String normalizedStatus = normalizeStatusFilter(status);
        List<TaskItem> tasks = taskMapper.findByUser(currentUserId(), normalizedStatus);
        List<TaskResponse> responses = new ArrayList<TaskResponse>();
        for (TaskItem task : tasks) {
            responses.add(toResponse(task));
        }
        return responses;
    }

    @Override
    public TaskResponse create(TaskRequest request) {
        TaskItem task = newTask(request);
        taskMapper.insert(task);
        return toResponse(taskMapper.findOne(task.getId(), currentUserId()));
    }

    @Override
    public TaskResponse update(Long id, TaskRequest request) {
        TaskItem existing = requireTask(id);
        TaskItem updated = updatedTask(existing, request);
        taskMapper.update(updated);
        return toResponse(taskMapper.findOne(id, currentUserId()));
    }

    @Override
    public void delete(Long id) {
        int deleted = taskMapper.delete(id, currentUserId());
        if (deleted == 0) {
            throw new ApiException(HttpServletResponse.SC_NOT_FOUND, "任务不存在。");
        }
    }

    private TaskItem newTask(TaskRequest request) {
        validateTask(request);
        TaskItem task = new TaskItem();
        task.setUserId(currentUserId());
        task.setTitle(request.getTitle().trim());
        task.setDescription(trimToEmpty(request.getDescription()));
        task.setStatus(normalizeStatus(request.getStatus()));
        return task;
    }

    private TaskItem updatedTask(TaskItem existing, TaskRequest request) {
        validateTask(request);
        existing.setTitle(request.getTitle().trim());
        existing.setDescription(trimToEmpty(request.getDescription()));
        existing.setStatus(normalizeStatus(request.getStatus()));
        return existing;
    }

    private void validateTask(TaskRequest request) {
        if (request == null || isBlank(request.getTitle())) {
            throw new ApiException(HttpServletResponse.SC_BAD_REQUEST, "任务标题不能为空。");
        }
        if (request.getTitle().trim().length() > TITLE_MAX_LENGTH) {
            throw new ApiException(HttpServletResponse.SC_BAD_REQUEST, "任务标题不能超过 120 字。");
        }
        normalizeStatus(request.getStatus());
    }

    private TaskItem requireTask(Long id) {
        TaskItem task = taskMapper.findOne(id, currentUserId());
        if (task == null) {
            throw new ApiException(HttpServletResponse.SC_NOT_FOUND, "任务不存在。");
        }
        return task;
    }

    private TaskResponse toResponse(TaskItem task) {
        return new TaskResponse(
                task.getId(),
                task.getTitle(),
                task.getDescription(),
                task.getStatus(),
                task.getCreatedAt(),
                task.getUpdatedAt());
    }

    private String normalizeStatusFilter(String status) {
        return isBlank(status) || "ALL".equalsIgnoreCase(status) ? null : normalizeStatus(status);
    }

    private String normalizeStatus(String status) {
        if (isBlank(status)) {
            return STATUS_TODO;
        }
        String normalized = status.trim().toUpperCase();
        if (STATUS_TODO.equals(normalized) || STATUS_DONE.equals(normalized)) {
            return normalized;
        }
        throw new ApiException(HttpServletResponse.SC_BAD_REQUEST, "任务状态只能是 TODO 或 DONE。");
    }

    private Long currentUserId() {
        CurrentUser user = CurrentUserHolder.get();
        return user.getId();
    }

    private String trimToEmpty(String value) {
        return value == null ? "" : value.trim();
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
