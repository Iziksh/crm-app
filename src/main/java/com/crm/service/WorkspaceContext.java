package com.crm.service;

import com.crm.domain.entity.Workspace;
import com.crm.repository.UserRepository;
import com.crm.repository.WorkspaceRepository;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class WorkspaceContext {

    private final UserRepository userRepository;
    private final WorkspaceRepository workspaceRepository;

    public WorkspaceContext(UserRepository userRepository, WorkspaceRepository workspaceRepository) {
        this.userRepository = userRepository;
        this.workspaceRepository = workspaceRepository;
    }

    // Checks stored roles + workspace assignment. A user with a workspaceId is always
    // workspace-scoped — only unassigned ROLE_ADMIN / ROLE_SUPER_ADMIN accounts are global.
    public boolean isAdmin() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || "anonymousUser".equals(auth.getPrincipal())) return false;
        return userRepository.findByUsername(auth.getName())
                .map(u -> u.getWorkspaceId() == null &&
                        (u.getRoles().contains("ROLE_ADMIN") || u.getRoles().contains("ROLE_SUPER_ADMIN")))
                .orElse(false);
    }

    @Transactional(readOnly = true)
    public List<Long> currentUserWorkspaceIds() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getPrincipal())) {
            return List.of();
        }
        return userRepository.findByUsername(auth.getName())
                .map(u -> workspaceRepository.findByMembers_Id(u.getId())
                        .stream().map(Workspace::getId).toList())
                .orElse(List.of());
    }

    public String currentWorkspaceSlug() {
        if (isAdmin()) return "all";
        return currentUserPrimaryWorkspace()
                .map(Workspace::getSlug)
                .orElse("all");
    }

    @Transactional(readOnly = true)
    public Optional<Workspace> currentUserPrimaryWorkspace() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getPrincipal())) {
            return Optional.empty();
        }
        return userRepository.findByUsername(auth.getName())
                .flatMap(u -> workspaceRepository.findByMembers_Id(u.getId()).stream().findFirst());
    }
}