package com.matriz.access;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import com.matriz.shared.ForbiddenException;

/**
 * Decides who can see what, from the role, project.manager_id and project_member
 * (docs/database.md section 6.1). The manager sees only their projects and a member only
 * those they belong to (R8, R19); nobody else has access, not even by direct link (S1).
 * Deleted projects are not accessible here (R9).
 */
@Service
public class ProjectPermissions {

    static final String NO_ACCESS = "You do not have access to this project.";

    private final JdbcTemplate jdbc;

    public ProjectPermissions(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public Optional<ProjectRole> roleIn(long userId, long projectId) {
        return jdbc.query("""
                SELECT CASE WHEN p.manager_id = ? THEN 'MANAGER' ELSE 'MEMBER' END
                FROM project p
                WHERE p.id = ? AND p.deleted_at IS NULL
                  AND (p.manager_id = ?
                       OR EXISTS (SELECT 1 FROM project_member m
                                  WHERE m.project_id = p.id AND m.user_id = ?))
                """, (rs, i) -> ProjectRole.valueOf(rs.getString(1)), userId, projectId, userId, userId)
                .stream().findFirst();
    }

    /** Refuses with 403 unless the user manages the project or is one of its members. */
    public ProjectRole requireAccess(AuthenticatedUser user, long projectId) {
        return roleIn(user.id(), projectId).orElseThrow(() -> new ForbiddenException(NO_ACCESS));
    }

    /** Identifiers of the not deleted projects the user manages or belongs to (R8, R25). */
    public Set<Long> accessibleProjectIds(long userId) {
        List<Long> ids = jdbc.queryForList("""
                SELECT id FROM project WHERE manager_id = ? AND deleted_at IS NULL
                UNION
                SELECT p.id FROM project p JOIN project_member m ON m.project_id = p.id
                WHERE m.user_id = ? AND p.deleted_at IS NULL
                """, Long.class, userId, userId);
        return new HashSet<>(ids);
    }
}
