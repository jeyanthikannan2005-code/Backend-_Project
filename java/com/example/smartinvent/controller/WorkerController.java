package com.example.smartinvent.controller;

import com.example.smartinvent.entity.User;
import com.example.smartinvent.service.WorkerService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;

// SecurityConfig already restricts "/api/workers/**" to ROLE_ADMIN only
@RestController
@RequestMapping("/api/workers")
public class WorkerController {

    @Autowired
    private WorkerService workerService;

    @GetMapping
    public List<User> getAll() {
        return workerService.getAllWorkers();
    }

    @GetMapping("/metrics")
    public List<Map<String, Object>> metrics(@RequestParam(required = false) String period) {
        return workerService.getWorkerMetrics(period);
    }

    @PostMapping
    public User add(@RequestBody User worker, Authentication authentication) {
        return workerService.addWorker(worker, authentication.getName());
    }

    @PutMapping("/{id}/deactivate")
    public User deactivate(@PathVariable Long id, Authentication authentication) {
        return workerService.deactivateWorker(id, authentication.getName());
    }
}
