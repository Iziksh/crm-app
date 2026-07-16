package com.crm.billing.repository;

import com.crm.billing.entity.DocumentLineItem;
import com.crm.billing.enums.DocumentOwnerType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DocumentLineItemRepository extends JpaRepository<DocumentLineItem, Long> {

    List<DocumentLineItem> findByOwnerTypeAndOwnerIdOrderBySortOrderAsc(DocumentOwnerType ownerType, Long ownerId);

    void deleteByOwnerTypeAndOwnerId(DocumentOwnerType ownerType, Long ownerId);
}
