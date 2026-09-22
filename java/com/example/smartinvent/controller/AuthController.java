package com.example.smartinvent.controller;

import com.example.smartinvent.dto.*;
import com.example.smartinvent.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

// All endpoints here are PUBLIC (see SecurityConfig: "/api/auth/**" is permitAll)
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    @Autowired
    private AuthService authService;

    @PostMapping("/register")
    public ResponseEntity<MessageResponse> register(@Valid @RequestBody RegisterRequest request) {
        return ResponseEntity.ok(authService.register(request));
    }

    @PostMapping("/admin/login")
    public ResponseEntity<LoginResponse> adminLogin(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.login(request, "ROLE_ADMIN"));
    }

    @PostMapping("/worker/login")
    public ResponseEntity<LoginResponse> workerLogin(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.login(request, "ROLE_WORKER"));
    }

    @PostMapping("/customer/login")
    public ResponseEntity<LoginResponse> customerLogin(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.login(request, "ROLE_CUSTOMER"));
    }

}
