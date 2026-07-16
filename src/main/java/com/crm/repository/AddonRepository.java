package com.crm.repository;

import com.crm.domain.entity.Addon;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AddonRepository extends JpaRepository<Addon, Long> {
    List<Addon> findByAccount_IdOrderByNameAsc(Long accountId);
}
