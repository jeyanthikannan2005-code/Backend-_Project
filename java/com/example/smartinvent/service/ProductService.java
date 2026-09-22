package com.example.smartinvent.service;

import com.example.smartinvent.dto.ProductRequest;
import com.example.smartinvent.dto.ProductResponse;
import com.example.smartinvent.entity.Product;
import com.example.smartinvent.repository.ProductRepository;
import com.example.smartinvent.util.DateFilterUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

@Service
public class ProductService {
    @Autowired
    private ProductRepository productRepository;
    @Autowired
    private ExpiryService expiryService;

    public List<ProductResponse> getAll() {
        return search(null, null, null, null, null);
    }

    public ProductResponse getById(Long id) {
        return toResponse(findEntity(id));
    }

    public Product findEntity(Long id) {
        return productRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Product not found"));
    }

    public ProductResponse create(ProductRequest request) {
        Product product = new Product();
        applyRequest(product, request);
        productRepository.save(product);
        return toResponse(product);
    }

    public ProductResponse update(Long id, ProductRequest request) {
        Product product = findEntity(id);
        applyRequest(product, request);
        productRepository.save(product);
        return toResponse(product);
    }

    public void delete(Long id) {
        if (!productRepository.existsById(id))
            throw new RuntimeException("Product not found");
        productRepository.deleteById(id);
    }

    public List<ProductResponse> search(String keyword, String category, String productType,
            String period, String stockFilter) {
        return productRepository.findAll().stream()
                .filter(p -> keyword == null || keyword.isBlank()
                        || (p.getName() != null
                                && p.getName().toLowerCase(Locale.ROOT).contains(keyword.toLowerCase(Locale.ROOT))))
                .filter(p -> category == null || category.isBlank()
                        || (p.getCategory() != null && p.getCategory().equalsIgnoreCase(category)))
                .filter(p -> productType == null || productType.isBlank()
                        || normalizeType(p.getProductType()).equals(normalizeType(productType)))
                .filter(p -> DateFilterUtil.matches(p.getCreatedDate(), period))
                .filter(p -> stockFilter == null || stockFilter.isBlank()
                        || ("LOW".equalsIgnoreCase(stockFilter) && "LOW_STOCK".equals(expiryService.stockStatus(p))))
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    public List<ProductResponse> lowStock(String productType, String period) {
        return search(null, null, productType, period, "LOW");
    }

    public List<ProductResponse> nearExpiryOrExpired(String productType, String period) {
        return productRepository.findAll().stream()
                .filter(p -> productType == null || productType.isBlank()
                        || normalizeType(p.getProductType()).equals(normalizeType(productType)))
                .filter(p -> DateFilterUtil.matches(p.getExpiryDate(), period))
                .filter(p -> !"SAFE".equals(expiryService.expiryStatus(p)))
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    private void applyRequest(Product product, ProductRequest request) {
        product.setName(request.getName() == null ? "Unnamed product" : request.getName().trim());
        product.setCategory(request.getCategory());
        product.setProductType(normalizeType(request.getProductType()));
        product.setImageUrl(request.getImageUrl());
        product.setIngredients(request.getIngredients());
        if (request.getQuantity() != null)
            product.setQuantity(request.getQuantity());
        product.setUnit(request.getUnit());
        product.setPurchasePrice(request.getPurchasePrice());
        product.setSellingPrice(request.getSellingPrice());
        product.setSupplier(request.getSupplier());
        product.setManufactureDate(request.getManufactureDate());
        product.setExpiryDate(request.getExpiryDate());
        if (request.getReorderLevel() != null)
            product.setReorderLevel(request.getReorderLevel());
    }

    private String normalizeType(String value) {
        if (value == null || value.isBlank())
            return "SELLING_PRODUCT";
        String type = value.trim().toUpperCase().replace(' ', '_');
        return "RAW_MATERIAL".equals(type) || "RAW".equals(type) ? "RAW_MATERIAL" : "SELLING_PRODUCT";
    }

    private ProductResponse toResponse(Product product) {
        ProductResponse response = new ProductResponse();
        response.setId(product.getId());
        response.setName(product.getName());
        response.setCategory(product.getCategory());
        response.setProductType(product.getProductType());
        response.setImageUrl(product.getImageUrl());
        response.setIngredients(product.getIngredients());
        response.setCreatedDate(product.getCreatedDate());
        response.setQuantity(product.getQuantity());
        response.setUnit(product.getUnit());
        response.setPurchasePrice(product.getPurchasePrice());
        response.setSellingPrice(product.getSellingPrice());
        response.setSupplier(product.getSupplier());
        response.setManufactureDate(product.getManufactureDate());
        response.setExpiryDate(product.getExpiryDate());
        response.setReorderLevel(product.getReorderLevel());
        response.setTotalSold(product.getTotalSold());
        response.setStockStatus(expiryService.stockStatus(product));
        response.setLowStock("LOW_STOCK".equals(expiryService.stockStatus(product)));
        response.setExpiryStatus(expiryService.expiryStatus(product));
        response.setRemainingDays(expiryService.remainingDays(product));
        return response;
    }
}
