package com.matriz.usecases;

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.stereotype.Service;

import com.matriz.access.AuthenticatedUser;
import com.matriz.access.ProjectPermissions;
import com.matriz.access.UserDirectory;
import com.matriz.access.UserInfo;
import com.matriz.projects.ProjectQueries;
import com.matriz.projects.ProjectRecord;
import com.matriz.shared.NotFoundException;

/**
 * listProjects and getProject (read only). The modules do not call each other: this use case asks
 * Access who may see what and Projects for the data (docs/architecture.md, section 5).
 */
@Service
public class ProjectReadUseCases {

    private final ProjectPermissions permissions;
    private final ProjectQueries projects;
    private final UserDirectory users;

    public ProjectReadUseCases(ProjectPermissions permissions, ProjectQueries projects, UserDirectory users) {
        this.permissions = permissions;
        this.projects = projects;
        this.users = users;
    }

    /** Only the projects of the user in the session (R8, R25). */
    public List<ProjectSummaryView> listProjects(AuthenticatedUser user, String search) {
        Set<Long> accessible = permissions.accessibleProjectIds(user.id());
        List<ProjectRecord> found = projects.findAmong(accessible, search);
        Map<Long, UserInfo> managers = users.findByIds(found.stream().map(ProjectRecord::managerId).distinct().toList());
        return found.stream()
                .map(p -> new ProjectSummaryView(p.id(), p.name(), p.phase(), p.status(), managers.get(p.managerId())))
                .toList();
    }

    /** 404 if it does not exist or was deleted; 403 if the user is neither its manager nor a member (S1). */
    public ProjectView getProject(AuthenticatedUser user, long projectId) {
        ProjectRecord p = projects.findById(projectId)
                .orElseThrow(() -> new NotFoundException("The project was not found."));
        permissions.requireAccess(user, projectId);
        UserInfo manager = users.findByIds(List.of(p.managerId())).get(p.managerId());
        return ProjectView.of(p, manager);
    }
}
