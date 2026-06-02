package com.crm.repository;

import com.crm.domain.entity.ActivityNote;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ActivityNoteRepository extends JpaRepository<ActivityNote, Long> {
    List<ActivityNote> findByActivity_IdOrderByCreatedAtAsc(Long activityId);
}
