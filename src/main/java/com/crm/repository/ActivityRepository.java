package com.crm.repository;

import com.crm.domain.entity.Activity;
import com.crm.domain.enums.ActivityStatus;
import com.crm.domain.enums.ActivityType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ActivityRepository extends JpaRepository<Activity, Long>, JpaSpecificationExecutor<Activity> {
    List<Activity> findByType(ActivityType type);
    List<Activity> findByStatus(ActivityStatus status);
    List<Activity> findByAssignedTo_Id(Long userId);
    List<Activity> findByAccount_Id(Long accountId);
    List<Activity> findByContact_Id(Long contactId);
    long countByStatus(ActivityStatus status);
}
