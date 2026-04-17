package com.example.taskmanager.common;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ApiException.class)
    public ResponseEntity<ErrorResponse> handleApiException(ApiException error) {
        HttpStatus status = HttpStatus.valueOf(error.getStatus());
        return new ResponseEntity<ErrorResponse>(new ErrorResponse(error.getMessage()), status);
    }
}
