package com.example.smartinvent.config;

import com.example.smartinvent.entity.Product;
import com.example.smartinvent.entity.User;
import com.example.smartinvent.repository.ProductRepository;
import com.example.smartinvent.repository.UserRepository;

import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDate;

@Configuration
public class DataInitializer {

        private static final String DEFAULT_INGREDIENTS = "Fresh fruit pulp, sugar, water, natural pectin, citric acid (acidity regulator). "
                        + "No artificial colours or preservatives added.";

        @Bean
        CommandLineRunner createDefaultAdmin(UserRepository userRepository, PasswordEncoder passwordEncoder) {
                return args -> {
                        String adminUsername = "Jeyanthi";
                        if (userRepository.existsByUsername(adminUsername)) {
                                return;
                        }

                        User admin = new User();
                        admin.setName("Jeyanthi");
                        admin.setEmail("jeyanthi@smartstock.com");
                        admin.setPhone("");
                        admin.setUsername(adminUsername);
                        admin.setPassword(passwordEncoder.encode("jeya@2005"));
                        admin.setRole("ROLE_ADMIN");
                        admin.setStatus("ACTIVE");
                        userRepository.save(admin);
                };
        }

        @Bean
        CommandLineRunner seedStarterCatalog(ProductRepository productRepository) {
                return args -> {
                        if (productRepository.count() > 0) {
                                return;
                        }

                        productRepository.save(sellingProduct("Strawberry Jam", "Jams", "jar", 100, 170, 30, 10,
                                        LocalDate.now().plusMonths(9), "images/jam-strawberry.jpg"));
                        productRepository.save(sellingProduct("Mixed Fruit Jam", "Jams", "jar", 90, 150, 60, 15,
                                        LocalDate.now().plusMonths(8), "images/jam-mixed-fruit.jpg"));
                        productRepository.save(sellingProduct("Orange Marmalade", "Jams", "jar", 88, 155, 35, 10,
                                        LocalDate.now().plusMonths(7), "images/jam-mango.jpg"));
                        productRepository.save(sellingProduct("Blueberry Jam", "Jams", "jar", 120, 210, 22, 8,
                                        LocalDate.now().plusMonths(8), "images/jam-blueberry.jpg"));
                        productRepository.save(sellingProduct("Pineapple Jam", "Jams", "jar", 90, 155, 25, 10,
                                        LocalDate.now().plusMonths(7), "images/jam-pineapple.jpg"));
                        productRepository.save(sellingProduct("Jackfruit Jam", "Jams", "jar", 95, 165, 18, 8,
                                        LocalDate.now().plusMonths(6), "images/jam-mixed-fruit.jpg"));

                        productRepository.save(rawMaterial("Sugar", "Raw materials", "kg", 42, 500, 80,
                                        LocalDate.now().plusMonths(12)));
                        productRepository.save(rawMaterial("Glass Jars (250g)", "Raw materials", "pcs", 12, 200, 40,
                                        null));
                };
        }

        private Product sellingProduct(String name, String category, String unit, double purchasePrice,
                        double sellingPrice, int quantity, int reorderLevel, LocalDate expiryDate, String imageUrl) {
                Product product = new Product();
                product.setName(name);
                product.setCategory(category);
                product.setProductType("SELLING_PRODUCT");
                product.setImageUrl(imageUrl);
                product.setUnit(unit);
                product.setPurchasePrice(purchasePrice);
                product.setSellingPrice(sellingPrice);
                product.setQuantity(quantity);
                product.setReorderLevel(reorderLevel);
                product.setExpiryDate(expiryDate);
                product.setSupplier("In-house production");
                product.setIngredients(DEFAULT_INGREDIENTS);
                return product;
        }

        private Product rawMaterial(String name, String category, String unit, double purchasePrice,
                        int quantity, int reorderLevel, LocalDate expiryDate) {
                Product product = new Product();
                product.setName(name);
                product.setCategory(category);
                product.setProductType("RAW_MATERIAL");
                product.setImageUrl("images/product-placeholder.png");
                product.setUnit(unit);
                product.setPurchasePrice(purchasePrice);
                product.setSellingPrice(0D);
                product.setQuantity(quantity);
                product.setReorderLevel(reorderLevel);
                product.setExpiryDate(expiryDate);
                product.setSupplier("Local supplier");
                return product;
        }
}
