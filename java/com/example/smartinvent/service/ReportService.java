package com.example.smartinvent.service;

import com.example.smartinvent.entity.Product;
import com.example.smartinvent.entity.Purchase;
import com.example.smartinvent.entity.Sale;
import com.example.smartinvent.repository.ProductRepository;
import com.example.smartinvent.repository.PurchaseRepository;
import com.example.smartinvent.repository.SaleRepository;
import com.example.smartinvent.util.DateFilterUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;

@Service
public class ReportService {
        @Autowired
        private ProductRepository productRepository;
        @Autowired
        private SaleRepository saleRepository;
        @Autowired
        private PurchaseRepository purchaseRepository;
        @Autowired
        private ExpiryService expiryService;

        public Map<String, Object> getReport() {
                return getReport(null, null);
        }

        public Map<String, Object> getReport(String period, String productType) {
                // Bug fix: normalizeType() always collapses a null/blank/"ALL" filter down to
                // "SELLING_PRODUCT" (it only recognises RAW_MATERIAL vs SELLING_PRODUCT), so
                // the
                // old code below could never actually match "ALL" and silently filtered every
                // raw-material purchase out of the report whenever the "All product types"
                // filter
                // was selected (the default). That made the "Raw materials used / purchased"
                // chart
                // stay stuck at zero even right after a purchase was recorded. Use a dedicated
                // filter-normalizer that keeps "ALL" as "ALL" instead.
                String requestedType = normalizeFilterType(productType);
                Predicate<Product> typeMatches = product -> "ALL".equals(requestedType)
                                || normalizeType(product.getProductType()).equals(requestedType);

                List<Product> allProducts = productRepository.findAll();
                List<Product> products = allProducts.stream().filter(typeMatches).collect(Collectors.toList());
                List<Sale> sales = saleRepository.findAll().stream()
                                .filter(sale -> DateFilterUtil.matches(sale.getDate(), period))
                                .filter(sale -> sale.getProduct() != null)
                                .filter(sale -> "SELLING_PRODUCT"
                                                .equals(normalizeType(sale.getProduct().getProductType())))
                                .filter(sale -> typeMatches.test(sale.getProduct()))
                                .collect(Collectors.toList());
                List<Purchase> purchases = purchaseRepository.findAll().stream()
                                .filter(purchase -> DateFilterUtil.matches(purchase.getDate(), period))
                                .filter(purchase -> purchase.getProduct() != null)
                                .filter(purchase -> typeMatches.test(purchase.getProduct()))
                                .collect(Collectors.toList());

                double totalSales = sales.stream()
                                .mapToDouble(sale -> valueOrZero(sale.getGrandTotal(), sale.getTotalAmount())).sum();
                double totalPurchases = purchases.stream()
                                .mapToDouble(purchase -> valueOrZero(purchase.getTotalAmount(), 0D)).sum();
                Map<Long, Integer> periodSold = sales.stream()
                                .collect(Collectors.groupingBy(sale -> sale.getProduct().getId(), Collectors.summingInt(
                                                sale -> sale.getQuantity() == null ? 0 : sale.getQuantity())));

                List<Product> sellingProducts = products.stream()
                                .filter(product -> "SELLING_PRODUCT".equals(normalizeType(product.getProductType())))
                                .collect(Collectors.toList());
                List<Product> lowStock = products.stream().filter(p -> "LOW_STOCK".equals(expiryService.stockStatus(p)))
                                .collect(Collectors.toList());
                List<Product> expired = products.stream().filter(p -> "EXPIRED".equals(expiryService.expiryStatus(p)))
                                .collect(Collectors.toList());
                List<Product> nearExpiry = products.stream()
                                .filter(p -> "NEAR_EXPIRY".equals(expiryService.expiryStatus(p)))
                                .collect(Collectors.toList());
                List<Product> mostSold = sellingProducts.stream()
                                .sorted(Comparator.comparingInt((Product p) -> periodSold.getOrDefault(p.getId(), 0))
                                                .reversed())
                                .collect(Collectors.toList());

                Map<String, Object> report = new LinkedHashMap<>();
                report.put("period", period == null || period.isBlank() ? "ALL" : period.toUpperCase());
                report.put("productType", requestedType);
                report.put("totalSales", totalSales);
                report.put("totalPurchases", totalPurchases);
                report.put("totalExpenses", totalPurchases);
                report.put("netProfit", totalSales - totalPurchases);
                report.put("totalOrders", sales.size());
                report.put("mostSoldProducts", mostSold);
                report.put("lowStockProducts", lowStock);
                report.put("expiredProducts", expired);
                report.put("nearExpiryProducts", nearExpiry);
                report.put("demandPrediction", predictDemand(sellingProducts, periodSold));
                Map<String, Integer> rawMaterialUsage = purchases.stream()
                                .filter(p -> "RAW_MATERIAL".equals(normalizeType(p.getProduct().getProductType())))
                                .collect(Collectors.groupingBy(p -> p.getProduct().getName(), LinkedHashMap::new,
                                                Collectors.summingInt(
                                                                p -> p.getQuantity() == null ? 0 : p.getQuantity())));
                report.put("rawMaterialUsage", rawMaterialUsage);
                return report;
        }

        public List<Map<String, Object>> predictDemand(List<Product> products) {
                Map<Long, Integer> totals = products.stream()
                                .collect(Collectors.toMap(Product::getId, Product::getTotalSold, (a, b) -> a));
                return predictDemand(products, totals);
        }

        private List<Map<String, Object>> predictDemand(List<Product> products, Map<Long, Integer> soldTotals) {
                List<Product> sorted = products.stream()
                                .filter(product -> "SELLING_PRODUCT".equals(normalizeType(product.getProductType())))
                                .sorted(Comparator.comparingInt((Product p) -> soldTotals.getOrDefault(p.getId(), 0))
                                                .reversed())
                                .collect(Collectors.toList());
                int total = sorted.size();
                List<Map<String, Object>> result = new ArrayList<>();
                for (int i = 0; i < total; i++) {
                        Product product = sorted.get(i);
                        String demand = i < Math.ceil(total / 3.0) ? "HIGH_DEMAND"
                                        : i < Math.ceil(2 * total / 3.0) ? "NORMAL_DEMAND" : "LOW_DEMAND";
                        int sold = soldTotals.getOrDefault(product.getId(), 0);
                        Map<String, Object> row = new LinkedHashMap<>();
                        row.put("productId", product.getId());
                        row.put("productName", product.getName());
                        row.put("productType", "SELLING_PRODUCT");
                        row.put("totalSold", sold);
                        row.put("demand", demand);
                        row.put("suggestion", "HIGH_DEMAND".equals(demand)
                                        ? "Consider increasing stock of " + product.getName() + "."
                                        : "NORMAL_DEMAND".equals(demand)
                                                        ? "Stock level for " + product.getName() + " looks adequate."
                                                        : "Consider reducing future orders of " + product.getName()
                                                                        + ".");
                        result.add(row);
                }
                return result;
        }

        private String normalizeType(String value) {
                if (value == null || value.isBlank())
                        return "SELLING_PRODUCT";
                String type = value.trim().toUpperCase().replace(' ', '_');
                return "RAW_MATERIAL".equals(type) || "RAW".equals(type) ? "RAW_MATERIAL" : "SELLING_PRODUCT";
        }

        // Used only for the incoming "productType" report filter, where a missing value
        // or
        // "ALL" genuinely means "don't filter" - unlike normalizeType() above, which is
        // used
        // to classify an actual product and must default to SELLING_PRODUCT.
        private String normalizeFilterType(String value) {
                if (value == null || value.isBlank())
                        return "ALL";
                String type = value.trim().toUpperCase().replace(' ', '_');
                if ("ALL".equals(type))
                        return "ALL";
                return "RAW_MATERIAL".equals(type) || "RAW".equals(type) ? "RAW_MATERIAL" : "SELLING_PRODUCT";
        }

        private double valueOrZero(Double preferred, Double fallback) {
                return preferred == null ? (fallback == null ? 0D : fallback) : preferred;
        }
}
