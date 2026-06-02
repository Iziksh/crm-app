package com.crm.repository;

import com.crm.domain.entity.SavedSearch;
import com.crm.domain.enums.SavedSearchScope;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SavedSearchRepository extends JpaRepository<SavedSearch, Long> {
    List<SavedSearch> findByScope(SavedSearchScope scope);
    List<SavedSearch> findByOwner_IdOrOwnerIsNull(Long userId);
}
