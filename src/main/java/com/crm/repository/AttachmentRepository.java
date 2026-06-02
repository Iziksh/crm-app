package com.crm.repository;

import com.crm.domain.entity.Attachment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AttachmentRepository extends JpaRepository<Attachment, Long> {
    List<Attachment> findByEntityTypeAndEntityId(String entityType, Long entityId);
}
