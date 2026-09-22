package com.example.smartinvent.exception;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;

// Catches errors thrown anywhere in our services/controllers and turns them
// into a clean JSON response like { "error": "message" } instead of an ugly
// stack trace, so the frontend JavaScript can read error.message easily.
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<?> handleRuntimeException(RuntimeException ex) {
        return ResponseEntity.badRequest().body(Map.of("error", ex.getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<?> handleValidationException(MethodArgumentNotValidException ex) {
        String message = ex.getBindingResult().getFieldErrors().isEmpty()
                ? "Invalid input"
                : ex.getBindingResult().getFieldErrors().get(0).getField() + " is invalid";
        return ResponseEntity.badRequest().body(Map.of("error", message));
    }
}
