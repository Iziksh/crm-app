package com.crm.repository;

import com.crm.domain.entity.AccountGroup;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

@Repository
public interface AccountGroupRepository extends JpaRepository<AccountGroup, Long>, JpaSpecificationExecutor<AccountGroup> {
    List<AccountGroup> findByNameContainingIgnoreCase(String name);
    Page<AccountGroup> findByNameContainingIgnoreCase(String name, Pageable pageable);
    long countByNameContainingIgnoreCase(String name);
    boolean existsByName(String name);
}
