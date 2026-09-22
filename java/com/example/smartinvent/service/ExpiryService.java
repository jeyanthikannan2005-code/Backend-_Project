package com.example.smartinvent.service;

import com.example.smartinvent.entity.Product;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

// Pure calculation helper - no database access here on purpose,
// so it's easy to read and reuse everywhere expiry status is needed
// (dashboard, expiry-management page, notifications, reports).
@Service
public class ExpiryService {

    public long remainingDays(Product product) {
        if (product.getExpiryDate() == null)
            return Long.MAX_VALUE;
        return ChronoUnit.DAYS.between(LocalDate.now(), product.getExpiryDate());
    }

    // Business rule (from the spec):
    // remaining <= 0 -> EXPIRED
    // remaining between 1 and 5 -> NEAR_EXPIRY
    // remaining > 5 -> SAFE
    public String expiryStatus(Product product) {
        long days = remainingDays(product);
        if (days <= 0)
            return "EXPIRED";
        if (days <= 5)
            return "NEAR_EXPIRY";
        return "SAFE";
    }

    public String stockStatus(Product product) {
        if (product.getReorderLevel() != null && product.getQuantity() <= product.getReorderLevel()) {
            return "LOW_STOCK";
        }
        return "IN_STOCK";
    }
}
