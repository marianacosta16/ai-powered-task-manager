package com.matriz.usecases;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.TreeSet;

import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import com.matriz.access.UserDirectory;
import com.matriz.access.UserInfo;
import com.matriz.tasks.TaskQueries;
import com.matriz.tasks.TaskRecord;

/**
 * The only place that locks the rows of people and tasks (an ArchUnit test checks it). Rules that
 * involve several rows (R2 to R4, R11, R17, R21, R26, T13) take locks so that two requests about the
 * same rows are done one after the other. To avoid two requests waiting for each other in a circle
 * (a deadlock) the order is always the same: <b>the people first, by id, then the tasks, by id</b>.
 * A task is never locked before a person in the same transaction; this class fails fast if that happens.
 */
@Component
class Locks {

    private static final Object TASKS_ARE_LOCKED = new Object(); // marks the current transaction

    private final UserDirectory users;
    private final TaskQueries tasks;

    Locks(UserDirectory users, TaskQueries tasks) {
        this.users = users;
        this.tasks = tasks;
    }

    /** Locks one person and returns them, if they exist. */
    Optional<UserInfo> person(long id) {
        requirePeopleBeforeTasks();
        return users.findForUpdate(id);
    }

    /** Locks these people, by id. Unknown ids are ignored. */
    void people(Collection<Long> ids) {
        requirePeopleBeforeTasks();
        new TreeSet<>(ids).forEach(users::findForUpdate);
    }

    Optional<TaskRecord> task(long id) {
        taskLockTaken();
        return tasks.findByIdForUpdate(id);
    }

    /** The open tasks and subtasks of a person in a project, locked, by id (R17). */
    List<Long> openTasksOf(long projectId, long userId) {
        taskLockTaken();
        return tasks.lockOpenTaskIdsOf(projectId, userId);
    }

    private static void requirePeopleBeforeTasks() {
        if (TransactionSynchronizationManager.hasResource(TASKS_ARE_LOCKED)) {
            throw new IllegalStateException(
                    "Lock order: the people must be locked before the tasks (see docs/architecture.md).");
        }
    }

    private static void taskLockTaken() {
        if (TransactionSynchronizationManager.isSynchronizationActive()
                && !TransactionSynchronizationManager.hasResource(TASKS_ARE_LOCKED)) {
            TransactionSynchronizationManager.bindResource(TASKS_ARE_LOCKED, Boolean.TRUE);
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCompletion(int status) {
                    TransactionSynchronizationManager.unbindResourceIfPossible(TASKS_ARE_LOCKED);
                }
            });
        }
    }
}
