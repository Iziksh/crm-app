package com.crm.repository;

import com.crm.domain.entity.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {
    Page<Product> findByNameContainingIgnoreCaseAndActive(String name, boolean active, Pageable pageable);
    long countByNameContainingIgnoreCaseAndActive(String name, boolean active);
    Page<Product> findByActive(boolean active, Pageable pageable);
    long countByActive(boolean active);
    Optional<Product> findBySku(String sku);
}
