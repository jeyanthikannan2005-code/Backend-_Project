package com.example.smartinvent.controller;

import com.example.smartinvent.dto.PurchaseRequest;
import com.example.smartinvent.entity.Purchase;
import com.example.smartinvent.service.PurchaseService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/purchases")
public class PurchaseController {

    @Autowired
    private PurchaseService purchaseService;

    @GetMapping
    public List<Purchase> getAll(
            @RequestParam(required = false) String period,
            @RequestParam(required = false) String productType) {
        return purchaseService.getAll(period, productType);
    }

    @PostMapping
    public Purchase create(@RequestBody PurchaseRequest request, Authentication authentication) {
        return purchaseService.create(request, authentication.getName());
    }
}
