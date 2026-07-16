package com.crm.service;

import com.crm.domain.entity.User;
import com.crm.domain.entity.Workspace;
import com.crm.dto.request.WorkspaceRequest;
import com.crm.dto.response.WorkspaceResponse;
import com.crm.exception.ResourceNotFoundException;
import com.crm.repository.UserRepository;
import com.crm.repository.WorkspaceRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class WorkspaceService {

    private final WorkspaceRepository workspaceRepository;
    private final UserRepository userRepository;

    public WorkspaceService(WorkspaceRepository workspaceRepository, UserRepository userRepository) {
        this.workspaceRepository = workspaceRepository;
        this.userRepository = userRepository;
    }

    public WorkspaceResponse create(WorkspaceRequest request, String createdByUsername) {
        Workspace ws = new Workspace();
        ws.setName(request.name());
        ws.setSlug(generateUniqueSlug(request.name()));
        ws.setDescription(request.description());

        User creator = userRepository.findByUsername(createdByUsername).orElse(null);
        if (creator != null) {
            ws.setCreatedBy(creator);
            ws.getMembers().add(creator);
        }

        Workspace saved = workspaceRepository.save(ws);
        if (creator != null) assignPrimaryWorkspaceIfMissing(creator, saved.getId());
        return WorkspaceResponse.from(saved);
    }

    private String generateUniqueSlug(String name) {
        String base = Workspace.slugFrom(name);
        String slug = base;
        int n = 1;
        while (workspaceRepository.existsBySlug(slug)) {
            slug = base + n++;
        }
        return slug;
    }

    @Transactional(readOnly = true)
    public List<WorkspaceResponse> findAll() {
        return workspaceRepository.findAll().stream().map(WorkspaceResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public List<WorkspaceResponse> findForUser(Long userId) {
        return workspaceRepository.findByMembers_Id(userId).stream().map(WorkspaceResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public WorkspaceResponse findById(Long id) {
        return WorkspaceResponse.from(getOrThrow(id));
    }

    public WorkspaceResponse addMember(Long workspaceId, Long userId) {
        Workspace ws = getOrThrow(workspaceId);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));
        if (!ws.getMembers().contains(user)) ws.getMembers().add(user);
        Workspace saved = workspaceRepository.save(ws);
        assignPrimaryWorkspaceIfMissing(user, workspaceId);
        return WorkspaceResponse.from(saved);
    }

    public WorkspaceResponse removeMember(Long workspaceId, Long userId) {
        Workspace ws = getOrThrow(workspaceId);
        ws.getMembers().removeIf(u -> u.getId().equals(userId));
        return WorkspaceResponse.from(workspaceRepository.save(ws));
    }

    public void delete(Long id) {
        workspaceRepository.delete(getOrThrow(id));
    }

    private Workspace getOrThrow(Long id) {
        return workspaceRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Workspace", "id", id));
    }

    /**
     * Keeps User.workspaceId (the scalar field workspace-scoped admin queries filter on) consistent
     * with Workspace.members (many-to-many) membership. Only fills a currently-null value — never
     * reassigns a user who already has a primary workspace — so this can never override an explicit
     * choice or change behavior for anyone the system already scopes correctly.
     */
    private void assignPrimaryWorkspaceIfMissing(User user, Long workspaceId) {
        if (user.getWorkspaceId() == null) {
            user.setWorkspaceId(workspaceId);
            userRepository.save(user);
        }
    }
}
