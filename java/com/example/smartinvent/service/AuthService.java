package com.example.smartinvent.service;

import com.example.smartinvent.dto.LoginRequest;
import com.example.smartinvent.dto.LoginResponse;
import com.example.smartinvent.dto.MessageResponse;
import com.example.smartinvent.dto.RegisterRequest;
import com.example.smartinvent.entity.User;
import com.example.smartinvent.repository.UserRepository;
import com.example.smartinvent.security.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

        @Autowired
        private UserRepository userRepository;

        @Autowired
        private PasswordEncoder passwordEncoder;

        @Autowired
        private JwtUtil jwtUtil;

        @Autowired
        private ActivityLogService activityLogService;

        // Used by register.html.
        // Everyone who self-registers becomes ROLE_WORKER.
        // Admin accounts are created directly in the database
        // or by another admin.
        public MessageResponse register(RegisterRequest request) {

                if (userRepository.existsByUsername(request.getUsername())) {
                        throw new RuntimeException("Username already taken");
                }

                if (userRepository.existsByEmail(request.getEmail())) {
                        throw new RuntimeException("Email already registered");
                }

                User user = new User();

                user.setName(request.getFullName());
                user.setEmail(request.getEmail());
                user.setPhone(request.getPhone());
                user.setUsername(request.getUsername());

                // Never store the plain password.
                // Always hash it using BCrypt.
                user.setPassword(passwordEncoder.encode(request.getPassword()));

                String requestedRole = "WORKER".equalsIgnoreCase(request.getRole()) ? "ROLE_WORKER" : "ROLE_CUSTOMER";
                user.setRole(requestedRole);
                user.setStatus("ACTIVE");

                userRepository.save(user);

                activityLogService.log(
                                user.getUsername(),
                                "REGISTER",
                                "New worker account created");

                return new MessageResponse(
                                "Registration successful. You can now log in.");
        }

        // Shared by both admin-login.html and worker-login.html.
        // expectedRole is ROLE_ADMIN or ROLE_WORKER.
        public LoginResponse login(
                        LoginRequest request,
                        String expectedRole) {

                User user = userRepository.findByUsername(request.getUsername())
                                .orElseThrow(() -> new RuntimeException("Invalid username or password"));

                if (!passwordEncoder.matches(
                                request.getPassword(),
                                user.getPassword())) {
                        throw new RuntimeException("Invalid username or password");
                }

                if (!user.getRole().equals(expectedRole)) {
                        throw new RuntimeException(
                                        "This account cannot log in from this page");
                }

                if (!"ACTIVE".equals(user.getStatus())) {
                        throw new RuntimeException(
                                        "This account has been deactivated");
                }

                String token = jwtUtil.generateToken(
                                user.getUsername(),
                                user.getRole());

                activityLogService.log(
                                user.getUsername(),
                                "LOGIN",
                                user.getRole() + " logged in");

                return new LoginResponse(
                                token,
                                user.getUsername(),
                                user.getRole(),
                                user.getName());
        }
}
