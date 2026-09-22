package com.example.smartinvent.repository;

import com.example.smartinvent.entity.Product;
import org.springframework.data.jpa.repository.JpaRepository;
// import org.springframework.data.jpa.repository.Query;
// import org.springframework.data.jpa.repository.*;
import java.time.LocalDate;
import java.util.List;

public interface ProductRepository extends JpaRepository<Product, Long> {

    List<Product> findByCategory(String category);

    List<Product> findByNameContainingIgnoreCase(String name);

    java.util.Optional<Product> findByNameIgnoreCase(String name);

    // quantity <= reorderLevel -> low stock. We fetch all and filter in the
    // service layer for simplicity (fine for a student-project data size).
    List<Product> findByExpiryDateLessThanEqual(LocalDate date);
    // @Query ("SELECT p From Product p ORDER BY p.quantity DESC ");
    // List<Product> findAllOrderByQuantityDesc();
}
