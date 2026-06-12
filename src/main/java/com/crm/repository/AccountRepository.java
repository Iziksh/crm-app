package com.crm.repository;

import com.crm.domain.entity.Account;
import com.crm.domain.enums.AccountType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface AccountRepository extends JpaRepository<Account, Long>, JpaSpecificationExecutor<Account> {
    Optional<Account> findByEmail(String email);
    List<Account> findByType(AccountType type);
    List<Account> findByNameContainingIgnoreCase(String name);
    Page<Account> findByNameContainingIgnoreCase(String name, Pageable pageable);
    long countByNameContainingIgnoreCase(String name);
    boolean existsByEmail(String email);

    // Workspace-scoped queries (for non-admin users)
    Page<Account> findByWorkspace_IdIn(Collection<Long> workspaceIds, Pageable pageable);
    long countByWorkspace_IdIn(Collection<Long> workspaceIds);

    @Query("SELECT a FROM Account a WHERE a.workspace.id IN :ids AND LOWER(a.name) LIKE LOWER(CONCAT('%',:q,'%'))")
    Page<Account> searchByWorkspaceIds(@Param("q") String q,
                                       @Param("ids") Collection<Long> ids,
                                       Pageable pageable);

    @Query("SELECT COUNT(a) FROM Account a WHERE a.workspace.id IN :ids AND LOWER(a.name) LIKE LOWER(CONCAT('%',:q,'%'))")
    long countSearchByWorkspaceIds(@Param("q") String q, @Param("ids") Collection<Long> ids);

    @Query("SELECT a FROM Account a WHERE a.workspace.id IN :ids AND LOWER(a.name) LIKE LOWER(CONCAT('%',:q,'%'))")
    List<Account> searchAllByWorkspaceIds(@Param("q") String q, @Param("ids") Collection<Long> ids);

    @Query("SELECT a FROM Account a WHERE a.workspace.id IN :ids")
    List<Account> findAllByWorkspaceIds(@Param("ids") Collection<Long> ids);

    // A-01: Dormant accounts with no recent activities, leads, or opportunities
    @Query("""
        SELECT a FROM Account a
        WHERE NOT EXISTS (
            SELECT 1 FROM Activity act WHERE act.account = a AND act.createdAt >= :cutoff
        )
        AND NOT EXISTS (
            SELECT 1 FROM Lead l WHERE l.account = a AND l.updatedAt >= :cutoff
        )
        AND NOT EXISTS (
            SELECT 1 FROM Opportunity o WHERE o.account = a AND o.updatedAt >= :cutoff
        )
        """)
    List<Account> findDormant(@Param("cutoff") LocalDateTime cutoff);
}
