package com.crm.repository;

import com.crm.domain.entity.Contact;
import com.crm.domain.enums.ContactStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface ContactRepository extends JpaRepository<Contact, Long>, JpaSpecificationExecutor<Contact> {
    Optional<Contact> findByEmail(String email);
    boolean existsByEmail(String email);
    List<Contact> findByAccount_Id(Long accountId);
    List<Contact> findByStatus(ContactStatus status);
    Page<Contact> findByFirstNameContainingIgnoreCaseOrLastNameContainingIgnoreCase(
            String firstName, String lastName, Pageable pageable);
    long countByFirstNameContainingIgnoreCaseOrLastNameContainingIgnoreCase(String firstName, String lastName);

    // Workspace-scoped queries (for non-admin users)
    Page<Contact> findByWorkspace_IdIn(Collection<Long> workspaceIds, Pageable pageable);
    long countByWorkspace_IdIn(Collection<Long> workspaceIds);

    @Query("""
        SELECT c FROM Contact c WHERE c.workspace.id IN :ids
        AND (LOWER(c.firstName) LIKE LOWER(CONCAT('%',:q,'%'))
          OR LOWER(c.lastName)  LIKE LOWER(CONCAT('%',:q,'%')))
        """)
    Page<Contact> searchByWorkspaceIds(@Param("q") String q,
                                       @Param("ids") Collection<Long> ids,
                                       Pageable pageable);

    @Query("""
        SELECT COUNT(c) FROM Contact c WHERE c.workspace.id IN :ids
        AND (LOWER(c.firstName) LIKE LOWER(CONCAT('%',:q,'%'))
          OR LOWER(c.lastName)  LIKE LOWER(CONCAT('%',:q,'%')))
        """)
    long countSearchByWorkspaceIds(@Param("q") String q, @Param("ids") Collection<Long> ids);

    @Query("""
        SELECT c FROM Contact c WHERE c.workspace.id IN :ids
        AND (LOWER(c.firstName) LIKE LOWER(CONCAT('%',:q,'%'))
          OR LOWER(c.lastName)  LIKE LOWER(CONCAT('%',:q,'%')))
        """)
    List<Contact> searchAllByWorkspaceIds(@Param("q") String q,
                                          @Param("ids") Collection<Long> ids,
                                          Pageable pageable);

    @Query("SELECT c FROM Contact c WHERE c.workspace.id IN :ids")
    List<Contact> findAllByWorkspaceIds(@Param("ids") Collection<Long> ids);
}
