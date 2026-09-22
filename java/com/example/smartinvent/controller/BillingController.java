package com.example.smartinvent.controller;

import com.example.smartinvent.dto.SaleRequest;
import com.example.smartinvent.dto.PaymentReceiptRequest;
import com.example.smartinvent.entity.Sale;
import com.example.smartinvent.service.BillingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/billing")
public class BillingController {
    @Autowired
    private BillingService billingService;

    @GetMapping
    public List<Sale> getAll(@RequestParam(required = false) String period,
            @RequestParam(required = false) String productType, @RequestParam(required = false) String paymentMethod,
            @RequestParam(required = false) String from, @RequestParam(required = false) String to,
            Authentication authentication) {
        if (authentication != null
                && authentication.getAuthorities().stream().anyMatch(a -> "ROLE_CUSTOMER".equals(a.getAuthority())))
            return billingService.getCustomerOrders(authentication.getName());
        return billingService.getAll(period, productType, paymentMethod, from, to);
    }

    @GetMapping("/online")
    public List<Sale> onlineOrders(@RequestParam(required = false) String period,
            @RequestParam(required = false) String paymentMethod, Authentication authentication) {
        if (authentication == null || authentication.getAuthorities().stream()
                .noneMatch(a -> "ROLE_ADMIN".equals(a.getAuthority()) || "ROLE_WORKER".equals(a.getAuthority())))
            throw new RuntimeException("Staff access required");
        return billingService.getOnlineOrders(period, paymentMethod);
    }

    @PostMapping
    public List<Sale> createBill(@RequestBody SaleRequest request, Authentication authentication) {
        boolean customer = authentication.getAuthorities().stream()
                .anyMatch(a -> "ROLE_CUSTOMER".equals(a.getAuthority()));
        return billingService.createBill(request, authentication.getName(), customer ? "ONLINE" : "COUNTER");
    }

    @PutMapping("/{invoice}/status")
    public List<Sale> updateStatus(@PathVariable String invoice, @RequestParam String status,
            Authentication authentication) {
        if (authentication == null || authentication.getAuthorities().stream()
                .noneMatch(a -> "ROLE_ADMIN".equals(a.getAuthority()) || "ROLE_WORKER".equals(a.getAuthority())))
            throw new RuntimeException("Staff access required");
        return billingService.updateOrderStatus(invoice, status);
    }

    @PutMapping("/{invoice}/payment")
    public List<Sale> recordPayment(@PathVariable String invoice, @RequestBody PaymentReceiptRequest request,
            Authentication authentication) {
        if (authentication == null || authentication.getAuthorities().stream()
                .noneMatch(a -> "ROLE_ADMIN".equals(a.getAuthority()) || "ROLE_WORKER".equals(a.getAuthority())))
            throw new RuntimeException("Staff access required");
        return billingService.recordDeliveredPayment(invoice, request.getAmountReceived(),
                request.getPaymentReference());
    }

    @PostMapping("/{invoice}/cancel")
    public List<Sale> cancel(@PathVariable String invoice, @RequestParam String reason, Authentication authentication) {
        if (authentication == null
                || authentication.getAuthorities().stream().noneMatch(a -> "ROLE_CUSTOMER".equals(a.getAuthority())))
            throw new RuntimeException("Customer access required");
        return billingService.cancelCustomerOrder(invoice, authentication.getName(), reason);
    }
}
