package com.example.smartinvent.service;

import com.example.smartinvent.entity.User;
import com.example.smartinvent.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.Map;
import java.util.LinkedHashMap;
import java.util.stream.Collectors;
import com.example.smartinvent.repository.SaleRepository;
import com.example.smartinvent.util.DateFilterUtil;

// Admin-only functionality (workers.html) - listing, adding, and
// deactivating worker accounts.
@Service
public class WorkerService {

    @Autowired
    private UserRepository userRepository;
    @Autowired
    private PasswordEncoder passwordEncoder;
    @Autowired
    private ActivityLogService activityLogService;
    @Autowired
    private SaleRepository saleRepository;

    public List<User> getAllWorkers() {
        return userRepository.findByRole("ROLE_WORKER");
    }

    public List<Map<String, Object>> getWorkerMetrics(String period) {
        return getAllWorkers().stream().map(worker -> {
            var sales = saleRepository.findAll().stream()
                    .filter(s -> worker.getUsername().equals(s.getSoldBy()))
                    .filter(s -> s.getSalesChannel() == null || "COUNTER".equalsIgnoreCase(s.getSalesChannel()))
                    .filter(s -> DateFilterUtil.matches(s.getDate(), period))
                    .collect(Collectors.toList());
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("username", worker.getUsername());
            row.put("name", worker.getName());
            row.put("billCount",
                    sales.stream().map(com.example.smartinvent.entity.Sale::getInvoiceNumber).distinct().count());
            row.put("amount",
                    sales.stream().mapToDouble(s -> s.getGrandTotal() == null ? 0D : s.getGrandTotal()).sum());
            row.put("lineItems", sales.size());
            return row;
        }).toList();
    }

    public User addWorker(User worker, String adminUsername) {
        worker.setRole("ROLE_WORKER");
        worker.setStatus("ACTIVE");
        worker.setPassword(passwordEncoder.encode(worker.getPassword()));
        userRepository.save(worker);
        activityLogService.log(adminUsername, "WORKER_ADDED", "Added worker " + worker.getUsername());
        return worker;
    }

    // We deactivate instead of deleting so past purchases/sales made by this
    // worker still make sense in reports and activity logs.
    public User deactivateWorker(Long id, String adminUsername) {
        User worker = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Worker not found"));
        worker.setStatus("INACTIVE");
        userRepository.save(worker);
        activityLogService.log(adminUsername, "WORKER_DEACTIVATED", "Deactivated worker " + worker.getUsername());
        return worker;
    }
}
