package com.matriz.usecases;

import java.time.Clock;
import java.time.DateTimeException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.matriz.access.AuthenticatedUser;
import com.matriz.access.ProjectPermissions;
import com.matriz.access.Role;
import com.matriz.access.UserDirectory;
import com.matriz.access.UserInfo;
import com.matriz.projects.NewProject;
import com.matriz.projects.ProjectCommands;
import com.matriz.projects.ProjectPhase;
import com.matriz.projects.ProjectQueries;
import com.matriz.projects.ProjectRecord;
import com.matriz.projects.ProjectStatus;
import com.matriz.projects.TeamRepository;
import com.matriz.shared.BusinessRuleException;
import com.matriz.shared.ForbiddenException;
import com.matriz.shared.InvalidRequestException;
import com.matriz.shared.NotFoundException;
import com.matriz.tasks.TaskAssignments;

/**
 * createProject and updateProject (F01, US01). Who is asking always comes from the session; the
 * manager of a new project is chosen by its creator (R19) and must be a project manager (M10).
 */
@Service
public class ProjectUseCases {

    static final String DEFAULT_TIMEZONE = "Europe/Lisbon"; // P26

    private final ProjectQueries queries;
    private final ProjectCommands commands;
    private final ProjectPermissions permissions;
    private final UserDirectory users;
    private final TeamRepository team;
    private final TaskAssignments assignments;
    private final Clock clock;

    public ProjectUseCases(ProjectQueries queries, ProjectCommands commands, ProjectPermissions permissions,
            UserDirectory users, TeamRepository team, TaskAssignments assignments, Clock clock) {
        this.queries = queries;
        this.commands = commands;
        this.permissions = permissions;
        this.users = users;
        this.team = team;
        this.assignments = assignments;
        this.clock = clock;
    }

    /** Only a project manager creates projects (F01, R19); checked before the body is looked at. */
    public void requireCanCreate(AuthenticatedUser creator) {
        if (creator.role() != Role.PROJECT_MANAGER) {
            throw new ForbiddenException("Only a project manager can create projects.");
        }
    }

    @Transactional
    public ProjectView createProject(AuthenticatedUser creator, NewProjectCommand c) {
        requireCanCreate(creator);
        ProjectPhase phase = ProjectPhase.fromLabel("phase", c.phase());
        ProjectStatus status = ProjectStatus.fromLabel("status", c.status());
        String timezone = c.timezone() == null ? DEFAULT_TIMEZONE : c.timezone();
        ZoneId zone = zone(timezone);
        UserInfo manager = requireManagerRole(c.managerId());
        checkDates(c.startDate(), c.startTime(), c.endDate(), c.endTime(), zone, true);

        long id = commands.insert(new NewProject(c.name(), c.description(), c.startDate(), c.startTime(),
                c.endDate(), c.endTime(), timezone, phase.label(), status.label(), manager.id()));
        return ProjectView.of(new ProjectRecord(id, c.name(), c.description(), c.startDate(), c.startTime(),
                c.endDate(), c.endTime(), timezone, phase.label(), status.label(), manager.id()), manager);
    }

    @Transactional
    public ProjectView updateProject(AuthenticatedUser user, long projectId, ProjectPatch patch) {
        queries.findById(projectId).orElseThrow(() -> new NotFoundException("The project was not found."));
        permissions.requireManager(user, projectId);
        // Read again with the row locked: two updates at the same time are done one after the other
        ProjectRecord current = queries.findByIdForUpdate(projectId)
                .orElseThrow(() -> new NotFoundException("The project was not found."));
        requireNotCompleted(current); // R28: nothing changes, and it is not reopened

        String name = patch.has("name") ? patch.name() : current.name();
        String description = patch.has("description") ? patch.description() : current.description();
        LocalDate startDate = patch.has("startDate") ? patch.startDate() : current.startDate();
        LocalTime startTime = patch.has("startTime") ? patch.startTime() : current.startTime();
        LocalDate endDate = patch.has("endDate") ? patch.endDate() : current.endDate();
        LocalTime endTime = patch.has("endTime") ? patch.endTime() : current.endTime();
        String timezone = patch.has("timezone") ? patch.timezone() : current.timezone();
        String phase = patch.has("phase") ? ProjectPhase.fromLabel("phase", patch.phase()).label() : current.phase();
        String status = patch.has("status") ? ProjectStatus.fromLabel("status", patch.status()).label()
                : current.status();
        long managerId = current.managerId();
        UserInfo manager = null;
        if (patch.has("managerId") && patch.managerId() != managerId) {
            manager = requireManagerRole(patch.managerId());
            managerId = manager.id();
        }
        boolean startChanged = !startDate.equals(current.startDate());
        checkDates(startDate, startTime, endDate, endTime, zone(timezone), startChanged); // R29: only when changed

        ProjectRecord updated = new ProjectRecord(projectId, name, description, startDate, startTime, endDate,
                endTime, timezone, phase, status, managerId);
        commands.update(updated);
        if (managerId != current.managerId()) {
            handOver(projectId, current.managerId(), managerId);
        }
        if (manager == null) {
            manager = users.findByIds(java.util.List.of(managerId)).get(managerId);
        }
        return ProjectView.of(updated, manager);
    }

    /**
     * The project changes manager (A12). The manager does not need a row in the team of his own
     * project (M5), so the new manager loses his member row if he had one; his tasks stay valid,
     * because a manager can be responsible for tasks of his project (R19, R21). The former manager
     * stops seeing the project, unless he has tasks in it: then he stays as a member, so that no
     * task is left with someone who cannot see it (R21).
     */
    private void handOver(long projectId, long formerManagerId, long newManagerId) {
        team.remove(projectId, newManagerId);
        if (assignments.hasTasks(projectId, formerManagerId) && !team.isMember(projectId, formerManagerId)) {
            team.add(projectId, formerManagerId);
        }
    }

    static void requireNotCompleted(ProjectRecord project) {
        if (ProjectStatus.fromLabel("status", project.status()) == ProjectStatus.COMPLETED) {
            throw new BusinessRuleException("PROJECT_COMPLETED", "A completed project cannot be changed or reopened.");
        }
    }

    private UserInfo requireManagerRole(long userId) {
        UserInfo user = users.findByIds(java.util.List.of(userId)).get(userId);
        if (user == null || !Role.PROJECT_MANAGER.label().equals(user.role())) {
            throw new BusinessRuleException("MANAGER_NOT_A_MANAGER", "The manager of a project must be a project manager.");
        }
        return user;
    }

    private static ZoneId zone(String timezone) {
        try {
            return ZoneId.of(timezone);
        } catch (DateTimeException ex) {
            throw new InvalidRequestException("The time zone is not valid.", "timezone",
                    "Must be a time zone like Europe/Lisbon.");
        }
    }

    /**
     * R16 and P23: the end instant is after the start instant; without an end time the whole end
     * day counts, and without a start time the project starts at 00:00 (P26). R29: the start date
     * is not in the past in the time zone of the project (today is accepted).
     */
    private void checkDates(LocalDate startDate, LocalTime startTime, LocalDate endDate, LocalTime endTime, ZoneId zone,
            boolean checkStartNotInPast) {
        LocalDateTime start = startDate.atTime(startTime == null ? LocalTime.MIDNIGHT : startTime);
        LocalDateTime end = endTime == null ? endDate.plusDays(1).atStartOfDay() : endDate.atTime(endTime);
        if (!end.isAfter(start)) {
            throw new BusinessRuleException("END_BEFORE_START", "The end date cannot be before the start date.");
        }
        if (checkStartNotInPast && startDate.isBefore(LocalDate.now(clock.withZone(zone)))) {
            throw new BusinessRuleException("START_DATE_IN_PAST", "The start date cannot be in the past.");
        }
    }
}
