package com.example.smartinvent.repository;

import com.example.smartinvent.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

// Extending JpaRepository gives us save(), findById(), findAll(), deleteById(), etc.
// for FREE - we never write SQL for basic CRUD operations.
public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByUsername(String username);

    Optional<User> findByEmail(String email);

    boolean existsByUsername(String username);

    boolean existsByEmail(String email);

    List<User> findByRole(String role);

    boolean existsByRole(String role);
}