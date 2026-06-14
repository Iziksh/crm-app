package com.crm.service;

import com.crm.domain.entity.User;
import com.crm.domain.entity.Workspace;
import com.crm.repository.UserRepository;
import com.crm.repository.WorkspaceRepository;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Service
@Transactional(readOnly = true)
public class WorkspaceContextImpl implements WorkspaceContext {

    private final UserRepository userRepository;
    private final WorkspaceRepository workspaceRepository;

    public WorkspaceContextImpl(UserRepository userRepository, WorkspaceRepository workspaceRepository) {
        this.userRepository = userRepository;
        this.workspaceRepository = workspaceRepository;
    }

    @Override
    public Optional<Workspace> currentUserPrimaryWorkspace() {
        return currentUser().flatMap(user -> {
            if (user.getWorkspaceId() != null) {
                return workspaceRepository.findById(user.getWorkspaceId());
            }
            return workspaceRepository.findByMembers_Id(user.getId()).stream().findFirst();
        });
    }

    @Override
    public List<Long> currentUserWorkspaceIds() {
        return currentUser().map(user -> {
            Set<Long> ids = new LinkedHashSet<>();
            if (user.getWorkspaceId() != null) {
                ids.add(user.getWorkspaceId());
            }
            workspaceRepository.findByMembers_Id(user.getId())
                    .forEach(ws -> ids.add(ws.getId()));
            return new ArrayList<>(ids);
        }).orElseGet(ArrayList::new);
    }

    @Override
    public boolean isAdmin() {
        return currentUser()
                .map(user -> user.getRoles().contains("ROLE_ADMIN")
                        || user.getRoles().contains("ROLE_SUPER_ADMIN"))
                .orElse(false);
    }

    private Optional<User> currentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getPrincipal())) {
            return Optional.empty();
        }
        return userRepository.findByUsername(auth.getName());
    }
}
