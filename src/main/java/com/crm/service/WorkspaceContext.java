package com.crm.service;

import com.crm.domain.entity.Workspace;

import java.util.List;
import java.util.Optional;

public interface WorkspaceContext {
    Optional<Workspace> currentUserPrimaryWorkspace();
    List<Long> currentUserWorkspaceIds();
    boolean isAdmin();
}
