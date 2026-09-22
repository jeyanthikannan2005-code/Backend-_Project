package com.example.smartinvent.service;

import com.example.smartinvent.entity.Product;
import com.example.smartinvent.entity.Sale;
import com.example.smartinvent.repository.ActivityLogRepository;
import com.example.smartinvent.repository.ProductRepository;
import com.example.smartinvent.repository.SaleRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class DashboardService {
    @Autowired
    private ProductRepository productRepository;
    @Autowired
    private SaleRepository saleRepository;
    @Autowired
    private ActivityLogRepository activityLogRepository;
    @Autowired
    private ExpiryService expiryService;

    public Map<String, Object> getSummary() {
        List<Product> products = productRepository.findAll();
        List<Sale> sales = saleRepository.findAll();
        LocalDate today = LocalDate.now();
        int totalStock = products.stream().mapToInt(Product::getQuantity).sum();
        double todaySales = sales.stream().filter(sale -> today.equals(sale.getDate())).mapToDouble(this::saleValue)
                .sum();
        double monthlySales = sales.stream().filter(sale -> sale.getDate() != null
                && sale.getDate().getMonth() == today.getMonth() && sale.getDate().getYear() == today.getYear())
                .mapToDouble(this::saleValue).sum();
        long lowStockProducts = products.stream().filter(p -> "LOW_STOCK".equals(expiryService.stockStatus(p))).count();
        long nearExpiryProducts = products.stream().filter(p -> "NEAR_EXPIRY".equals(expiryService.expiryStatus(p)))
                .count();
        long expiredProducts = products.stream().filter(p -> "EXPIRED".equals(expiryService.expiryStatus(p))).count();

        List<Map<String, Object>> topSellingProducts = products.stream()
                .filter(product -> !"RAW_MATERIAL".equalsIgnoreCase(product.getProductType()))
                .sorted(Comparator
                        .comparingInt((Product product) -> product.getTotalSold() == null ? 0 : product.getTotalSold())
                        .reversed()
                        .thenComparing(Product::getName, String.CASE_INSENSITIVE_ORDER))
                .map(product -> {
                    Map<String, Object> card = new LinkedHashMap<>();
                    card.put("id", product.getId());
                    card.put("name", product.getName());
                    card.put("productType", product.getProductType());
                    card.put("imageUrl", product.getImageUrl());
                    card.put("sellingPrice", product.getSellingPrice());
                    card.put("quantity", product.getQuantity());
                    card.put("totalSold", product.getTotalSold());
                    return card;
                }).collect(Collectors.toList());

        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("totalProducts", products.size());
        summary.put("totalStock", totalStock);
        summary.put("todaySales", todaySales);
        summary.put("monthlySales", monthlySales);
        summary.put("lowStockProducts", lowStockProducts);
        summary.put("nearExpiryProducts", nearExpiryProducts);
        summary.put("expiredProducts", expiredProducts);
        summary.put("topSellingProducts", topSellingProducts);
        summary.put("recentActivity",
                activityLogRepository.findAllByOrderByDateDesc().stream().limit(5).collect(Collectors.toList()));
        return summary;
    }

    private double saleValue(Sale sale) {
        return sale.getGrandTotal() == null ? (sale.getTotalAmount() == null ? 0D : sale.getTotalAmount())
                : sale.getGrandTotal();
    }
}
