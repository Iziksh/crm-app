package com.crm.repository;

import com.crm.domain.entity.Activity;
import com.crm.domain.enums.ActivityStatus;
import com.crm.domain.enums.ActivityType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;

@Repository
public interface ActivityRepository extends JpaRepository<Activity, Long>, JpaSpecificationExecutor<Activity> {
    List<Activity> findByType(ActivityType type);
    List<Activity> findByStatus(ActivityStatus status);
    List<Activity> findByAssignedTo_Id(Long userId);
    List<Activity> findByAccount_Id(Long accountId);
    List<Activity> findByContact_Id(Long contactId);
    long countByStatus(ActivityStatus status);

    @Query("SELECT a.status, COUNT(a) FROM Activity a GROUP BY a.status")
    List<Object[]> countGroupByStatus();

    @Query("SELECT a.status, COUNT(a) FROM Activity a WHERE a.workspace.id IN :ids GROUP BY a.status")
    List<Object[]> countGroupByStatusAndWorkspaceIds(@Param("ids") Collection<Long> ids);

    // T-02: Activities due on a specific date, not resolved
    @Query("SELECT a FROM Activity a WHERE a.dueDate = :dueDate AND a.status NOT IN :excluded AND a.assignedTo IS NOT NULL")
    List<Activity> findDueOn(@Param("dueDate") LocalDate dueDate, @Param("excluded") List<ActivityStatus> excluded);

    // T-03: Overdue activities
    @Query("SELECT a FROM Activity a WHERE a.dueDate < :today AND a.status NOT IN :excluded AND a.assignedTo IS NOT NULL")
    List<Activity> findOverdue(@Param("today") LocalDate today, @Param("excluded") List<ActivityStatus> excluded);

    // T-05: Meetings starting within a time window
    @Query("SELECT a FROM Activity a WHERE a.type = :type AND a.dueDate = :today AND a.status NOT IN :excluded AND a.assignedTo IS NOT NULL")
    List<Activity> findMeetingsOn(@Param("type") ActivityType type, @Param("today") LocalDate today, @Param("excluded") List<ActivityStatus> excluded);

    // Phase 17: Calendar view + iCal export
    List<Activity> findByDueDateBetweenAndTypeInOrderByDueDate(LocalDate from, LocalDate to, List<ActivityType> types);
}
