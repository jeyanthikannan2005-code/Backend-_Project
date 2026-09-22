package com.example.smartinvent.controller;

import com.example.smartinvent.entity.ActivityLog;
import com.example.smartinvent.service.ActivityLogService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/activity-logs")
public class ActivityLogController {

    @Autowired
    private ActivityLogService activityLogService;

    @GetMapping
    public List<ActivityLog> getAll(
            @RequestParam(required = false) String period,
            @RequestParam(required = false) String action) {
        return activityLogService.getAll(period, action);
    }
}
