package com.example.smartinvent.service;

import com.example.smartinvent.dto.PurchaseRequest;
import com.example.smartinvent.entity.Product;
import com.example.smartinvent.entity.Purchase;
import com.example.smartinvent.repository.ProductRepository;
import com.example.smartinvent.repository.PurchaseRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.example.smartinvent.util.DateFilterUtil;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class PurchaseService {

    @Autowired
    private PurchaseRepository purchaseRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private ActivityLogService activityLogService;

    public List<Purchase> getAll() {
        return getAll(null, null);
    }

    public List<Purchase> getAll(String period, String productType) {
        return purchaseRepository.findAllByOrderByDateDesc().stream()
                .filter(p -> DateFilterUtil.matches(p.getDate(), period))
                .filter(p -> productType == null || productType.isBlank()
                        || (p.getProduct() != null && p.getProduct().getProductType().equalsIgnoreCase(productType)))
                .collect(Collectors.toList());
    }

    // Business rule:
    // Existing Product -> Old Stock + Purchased Quantity = New Stock
    // New Product -> Create Product with Purchased Quantity as initial stock
    public Purchase create(PurchaseRequest request, String username) {

        // Procurement is intentionally limited to ingredients, packaging and
        // other raw materials. Selling products are made in-house and must not
        // enter stock through this endpoint, even if a client sends a forged
        // productType value.
        request.setProductType("RAW_MATERIAL");

        // Check product name
        if (request.getProductName() == null ||
                request.getProductName().isBlank()) {

            throw new RuntimeException("Product name is required");
        }

        // Check quantity
        if (request.getQuantity() == null ||
                request.getQuantity() <= 0) {

            throw new RuntimeException("Quantity must be greater than 0");
        }

        // Check purchase price
        if (request.getPurchasePrice() == null ||
                request.getPurchasePrice() < 0) {

            throw new RuntimeException("Purchase price must be valid");
        }

        String productName = request.getProductName().trim();

        /*
         * Try to find an existing product using its name.
         */
        Product product = productRepository
                .findByNameIgnoreCase(productName)
                .orElse(null);

        int oldStock = 0;

        /*
         * CASE 1:
         * Product already exists.
         */
        if (product != null) {

            oldStock = product.getQuantity();

            int newStock = oldStock + request.getQuantity();

            product.setQuantity(newStock);

            product.setPurchasePrice(request.getPurchasePrice());

            if (request.getSupplier() != null &&
                    !request.getSupplier().isBlank()) {
                product.setSupplier(request.getSupplier().trim());
            }
            if (request.getProductType() != null && !request.getProductType().isBlank()) {
                product.setProductType("RAW_MATERIAL".equalsIgnoreCase(request.getProductType())
                        ? "RAW_MATERIAL"
                        : "SELLING_PRODUCT");
            }
            productRepository.save(product);
        }

        /*
         * CASE 2:
         * Product does not exist.
         * Create a new product automatically.
         */
        else {

            product = new Product();

            product.setName(productName);

            // Purchased quantity becomes initial stock
            product.setQuantity(request.getQuantity());

            product.setPurchasePrice(request.getPurchasePrice());

            /*
             * For now, use purchase price as initial selling price.
             * You can edit the selling price later from Inventory.
             */
            product.setSellingPrice(request.getPurchasePrice());

            product.setSupplier(request.getSupplier());

            // Default values for a newly created product
            product.setProductType("RAW_MATERIAL".equalsIgnoreCase(request.getProductType())
                    ? "RAW_MATERIAL"
                    : "SELLING_PRODUCT");
            product.setCategory(product.getProductType().equals("RAW_MATERIAL") ? "Raw Material" : "General");
            product.setUnit("unit");
            product.setReorderLevel(10);
            product.setTotalSold(0);

            productRepository.save(product);
        }

        /*
         * Save purchase history.
         */
        Purchase purchase = new Purchase();

        purchase.setProduct(product);
        purchase.setSupplier(request.getSupplier());
        purchase.setQuantity(request.getQuantity());
        purchase.setPurchasePrice(request.getPurchasePrice());

        purchase.setTotalAmount(
                request.getQuantity() * request.getPurchasePrice());

        purchaseRepository.save(purchase);

        /*
         * Activity log.
         */
        int newStock = product.getQuantity();

        activityLogService.log(
                username,
                "PURCHASE_ADDED",
                "Purchased " +
                        request.getQuantity() +
                        " x " +
                        product.getName() +
                        " (stock " +
                        oldStock +
                        " -> " +
                        newStock +
                        ")");

        return purchase;
    }
}
