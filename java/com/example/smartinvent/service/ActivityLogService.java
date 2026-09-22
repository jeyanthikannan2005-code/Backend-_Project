package com.example.smartinvent.service;

import com.example.smartinvent.entity.ActivityLog;
import com.example.smartinvent.repository.ActivityLogRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.example.smartinvent.util.DateFilterUtil;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class ActivityLogService {

    @Autowired
    private ActivityLogRepository activityLogRepository;

    // Called from other services whenever something worth remembering happens
    // (login, product added, bill created, etc.)
    public void log(String username, String action, String description) {
        activityLogRepository.save(new ActivityLog(username, action, description));
    }

    public List<ActivityLog> getAll() {
        return getAll(null, null);
    }

    public List<ActivityLog> getAll(String period, String action) {
        return activityLogRepository.findAllByOrderByDateDesc().stream()
                .filter(log -> DateFilterUtil.matches(log.getDate(), period))
                .filter(log -> action == null || action.isBlank() || action.equalsIgnoreCase(log.getAction()))
                .collect(Collectors.toList());
    }
}
