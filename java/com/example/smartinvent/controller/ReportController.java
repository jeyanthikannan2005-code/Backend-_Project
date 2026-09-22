package com.example.smartinvent.controller;

import com.example.smartinvent.service.ReportService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestController
@RequestMapping("/api/reports")
public class ReportController {

    @Autowired
    private ReportService reportService;

    @GetMapping
    public Map<String, Object> getReport(
            @RequestParam(required = false) String period,
            @RequestParam(required = false) String productType) {
        return reportService.getReport(period, productType);
    }
}
