package com.crm.repository;

import com.crm.domain.entity.Workspace;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface WorkspaceRepository extends JpaRepository<Workspace, Long> {
    List<Workspace> findByMembers_Id(Long userId);
    boolean existsByName(String name);
}
