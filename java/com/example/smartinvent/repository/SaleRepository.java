package com.example.smartinvent.repository;

import com.example.smartinvent.entity.Sale;
import org.springframework.data.jpa.repository.JpaRepository;
import java.time.LocalDate;
import java.util.List;

public interface SaleRepository extends JpaRepository<Sale, Long> {
    List<Sale> findAllByOrderByDateDesc();

    List<Sale> findByDate(LocalDate date);
}
