package com.example.smartinvent.controller;

import com.example.smartinvent.entity.User;
import com.example.smartinvent.service.ProfileService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestController
@RequestMapping("/api/profile")
public class ProfileController {

    @Autowired
    private ProfileService profileService;

    @GetMapping
    public User getProfile(Authentication authentication) {
        return profileService.getProfile(authentication.getName());
    }

    @PutMapping
    public User updateProfile(@RequestBody User updated, Authentication authentication) {
        return profileService.updateProfile(authentication.getName(), updated);
    }

    @PutMapping("/change-password")
    public void changePassword(@RequestBody Map<String, String> body, Authentication authentication) {
        profileService.changePassword(authentication.getName(),
                body.get("currentPassword"), body.get("newPassword"));
    }
}
