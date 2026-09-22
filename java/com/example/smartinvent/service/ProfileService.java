package com.example.smartinvent.service;

import com.example.smartinvent.entity.User;
import com.example.smartinvent.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class ProfileService {

    @Autowired
    private UserRepository userRepository;
    @Autowired
    private PasswordEncoder passwordEncoder;

    public User getProfile(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));
    }

    public User updateProfile(String username, User updated) {
        User user = getProfile(username);
        user.setName(updated.getName());
        user.setPhone(updated.getPhone());
        userRepository.save(user);
        return user;
    }

    public void changePassword(String username, String currentPassword, String newPassword) {
        User user = getProfile(username);
        if (!passwordEncoder.matches(currentPassword, user.getPassword())) {
            throw new RuntimeException("Current password is incorrect");
        }
        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);
    }
}
