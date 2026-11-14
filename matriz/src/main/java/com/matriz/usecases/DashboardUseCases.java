package com.matriz.usecases;

import java.time.Clock;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import com.matriz.access.AuthenticatedUser;
import com.matriz.access.ProjectPermissions;
import com.matriz.access.UserDirectory;
import com.matriz.access.UserInfo;
import com.matriz.projects.ProjectQueries;
import com.matriz.projects.ProjectRecord;
import com.matriz.projects.ProjectStatus;
import com.matriz.views.DashboardQueries;
import com.matriz.views.ProjectProgress;
import com.matriz.views.ProjectSchedule;
import com.matriz.views.StatusCounts;

/**
 * getDashboard (F07; US06, US13): the projects of the user that are not completed, with the number of main
 * tasks by status; the manager of a project also gets its progress and calculated status. The modules do not
 * call each other: this use case asks Access who may see what, Projects for the data and Views for the counts.
 */
@Service
public class DashboardUseCases {

    private final ProjectPermissions permissions;
    private final ProjectQueries projects;
    private final DashboardQueries queries;
    private final UserDirectory users;
    private final Clock clock;

    DashboardUseCases(ProjectPermissions permissions, ProjectQueries projects, DashboardQueries queries,
            UserDirectory users, Clock clock) {
        this.permissions = permissions;
        this.projects = projects;
        this.queries = queries;
        this.users = users;
        this.clock = clock;
    }

    /** By name, then by id, so the order does not change between two requests (A34). */
    @Transactional(readOnly = true, isolation = Isolation.REPEATABLE_READ) // the projects and the counts are one snapshot
    public List<DashboardProjectView> getDashboard(AuthenticatedUser user) {
        List<ProjectRecord> shown = projects.findAmong(permissions.accessibleProjectIds(user.id()), null).stream()
                .filter(p -> ProjectStatus.fromLabel("status", p.status()) != ProjectStatus.COMPLETED) // P27
                .sorted(Comparator.comparing(ProjectRecord::name, String.CASE_INSENSITIVE_ORDER)
                        .thenComparing(ProjectRecord::id))
                .toList();
        Map<Long, StatusCounts> counts = queries.countsByProject(shown.stream().map(ProjectRecord::id).toList());
        Map<Long, UserInfo> managers = users.findByIds(shown.stream().map(ProjectRecord::managerId).distinct().toList());
        return shown.stream().map(p -> new DashboardProjectView(
                new ProjectSummaryView(p.id(), p.name(), p.phase(), p.status(), managers.get(p.managerId())),
                counts.get(p.id()), p.managerId() == user.id() ? progress(p, counts.get(p.id())) : null)).toList();
    }

    private ProjectProgress progress(ProjectRecord p, StatusCounts counts) {
        ProjectSchedule schedule = new ProjectSchedule(p.startDate(), p.startTime(), p.endDate(), p.endTime(), p.timezone());
        boolean inProgress = ProjectStatus.fromLabel("status", p.status()) == ProjectStatus.IN_PROGRESS;
        return ProjectProgress.of(schedule, inProgress, counts, clock.instant());
    }
}
