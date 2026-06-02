package com.crm.service;

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
        ws.setDescription(request.description());
        userRepository.findByUsername(createdByUsername).ifPresent(user -> {
            ws.setCreatedBy(user);
            ws.getMembers().add(user);
        });
        return WorkspaceResponse.from(workspaceRepository.save(ws));
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
        userRepository.findById(userId).ifPresentOrElse(
                user -> { if (!ws.getMembers().contains(user)) ws.getMembers().add(user); },
                () -> { throw new ResourceNotFoundException("User", "id", userId); }
        );
        return WorkspaceResponse.from(workspaceRepository.save(ws));
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
}
