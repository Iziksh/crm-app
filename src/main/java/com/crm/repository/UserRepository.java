package com.crm.repository;

import com.crm.domain.entity.User;
import com.crm.domain.enums.UserStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByUsername(String username);
    Optional<User> findByEmail(String email);
    boolean existsByUsername(String username);
    boolean existsByEmail(String email);
    Page<User> findByUsernameContainingIgnoreCaseOrEmailContainingIgnoreCase(
            String username, String email, Pageable pageable);
    long countByUsernameContainingIgnoreCaseOrEmailContainingIgnoreCase(String username, String email);
    long countByRolesContaining(String role);

    Optional<User> findByEmailAndWorkspaceId(String email, Long workspaceId);
    List<User> findByWorkspaceId(Long workspaceId);

    @Query("SELECT COUNT(u) FROM User u JOIN u.roles r WHERE u.workspaceId = :wsId AND r = :role AND u.status = :status")
    long countByWorkspaceIdAndRoleAndStatus(@Param("wsId") Long workspaceId,
                                            @Param("role") String role,
                                            @Param("status") UserStatus status);
}
