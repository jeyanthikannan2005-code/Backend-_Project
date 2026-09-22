package com.example.smartinvent.controller;

import com.example.smartinvent.dto.ProductRequest;
import com.example.smartinvent.dto.ProductResponse;
import com.example.smartinvent.service.ProductService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import java.util.List;

// Everything here requires a valid JWT (see SecurityConfig: "/api/**" -> authenticated())
@RestController
@RequestMapping("/api/products")
public class ProductController {

    @Autowired
    private ProductService productService;

    @GetMapping
    public List<ProductResponse> getAll(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String productType,
            @RequestParam(required = false) String period,
            @RequestParam(required = false) String stockFilter) {
        if (keyword != null || category != null || productType != null || period != null || stockFilter != null) {
            return productService.search(keyword, category, productType, period, stockFilter);
        }
        return productService.getAll();
    }

    @GetMapping("/store")
    public List<ProductResponse> publicStore() {
        return productService.search(null, null, "SELLING_PRODUCT", null, null);
    }

    @GetMapping("/{id}")
    public ProductResponse getById(@PathVariable Long id) {
        return productService.getById(id);
    }

    @PostMapping
    public ProductResponse create(@RequestBody ProductRequest request) {
        return productService.create(request);
    }

    @PutMapping("/{id}")
    public ProductResponse update(@PathVariable Long id, @RequestBody ProductRequest request) {
        return productService.update(id, request);
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable Long id) {
        productService.delete(id);
    }

    // inventory.html's search box + category filter call this directly.
    @GetMapping("/search")
    public List<ProductResponse> search(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String productType,
            @RequestParam(required = false) String period,
            @RequestParam(required = false) String stockFilter) {
        return productService.search(keyword, category, productType, period, stockFilter);
    }

    @GetMapping("/low-stock")
    public List<ProductResponse> lowStock(
            @RequestParam(required = false) String productType,
            @RequestParam(required = false) String period) {
        return productService.lowStock(productType, period);
    }

    @GetMapping("/expiry")
    public List<ProductResponse> expiring(
            @RequestParam(required = false) String productType,
            @RequestParam(required = false) String period) {
        return productService.nearExpiryOrExpired(productType, period);
    }
}
