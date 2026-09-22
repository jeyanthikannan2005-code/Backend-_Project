package com.example.smartinvent.repository;

import com.example.smartinvent.entity.Review;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ReviewRepository extends JpaRepository<Review, Long> {
    List<Review> findAllByOrderByCreatedAtDesc();

    List<Review> findByProductIdOrderByCreatedAtDesc(Long productId);
}
