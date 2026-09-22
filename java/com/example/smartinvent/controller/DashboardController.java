package com.example.smartinvent.controller;

import com.example.smartinvent.service.DashboardService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestController
@RequestMapping("/api/dashboard")
public class DashboardController {

    @Autowired
    private DashboardService dashboardService;

    @GetMapping
    public Map<String, Object> getSummary() {
        return dashboardService.getSummary();
    }

    // admin-dashboard.html and worker-dashboard.html each call their own
    // path, but both currently show the same underlying summary data -
    // worker-dashboard.js simply only reads the fields it needs from it.
    @GetMapping("/admin")
    public Map<String, Object> getAdminSummary() {
        return dashboardService.getSummary();
    }

    @GetMapping("/worker")
    public Map<String, Object> getWorkerSummary() {
        return dashboardService.getSummary();
    }
}
