package com.matriz.usecases;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import com.matriz.access.AuthenticatedUser;
import com.matriz.shared.InvalidRequestException;
import com.matriz.tasks.Quadrant;
import com.matriz.tasks.TaskRecord;
import com.matriz.tasks.TaskStatus;
import com.matriz.views.ProjectTaskQueries;

/**
 * listProjectTasks (F04, F05, F22; US03, US09, US12): the List and the Matrix of a project. Whoever
 * can see the project can read its tasks, in any project state (R8); the filters are checked after
 * that (403 before 400).
 */
@Service
public class TaskListUseCases {

    private final TaskRules rules;
    private final ProjectTaskQueries queries;

    TaskListUseCases(TaskRules rules, ProjectTaskQueries queries) {
        this.rules = rules;
        this.queries = queries;
    }

    /**
     * The main tasks of the project with their subtasks inside (R18), open ones first and then the
     * closed ones, ordered as R23 says. {@code mine} keeps the tasks of the user (P25); {@code status}
     * and {@code quadrant} keep the tasks with that status or quadrant (R25); all of them combine.
     */
    @Transactional(readOnly = true, isolation = Isolation.REPEATABLE_READ) // one snapshot for all the reads
    public List<TaskView> listProjectTasks(AuthenticatedUser user, long projectId, String mine, String status,
            String quadrant) {
        rules.loadProject(user, projectId); // 404 and 403
        boolean onlyMine = parseMine(mine);
        TaskStatus taskStatus = status == null ? null : TaskStatus.fromLabel("status", status);
        Quadrant taskQuadrant = quadrant == null ? null : Quadrant.fromLabel("quadrant", quadrant);
        List<TaskRecord> tasks = queries.mainTasks(projectId, onlyMine ? user.id() : null, taskStatus, taskQuadrant);
        return rules.viewsOfMainTasks(tasks);
    }

    /** Read here, not by Spring, so that a bad value is a 400 that comes after the 403 (A28). */
    private static boolean parseMine(String mine) {
        if (mine == null || mine.equals("false")) {
            return false;
        }
        if (mine.equals("true")) {
            return true;
        }
        throw new InvalidRequestException("The filter is not valid.", "mine", "Must be true or false.");
    }
}
