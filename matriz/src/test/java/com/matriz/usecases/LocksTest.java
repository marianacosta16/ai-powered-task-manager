package com.matriz.usecases;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.support.TransactionTemplate;

import com.matriz.testsupport.ApiIntegrationTest;

/** The lock order: the project first, then the people, then the tasks (docs/architecture.md). */
class LocksTest extends ApiIntegrationTest {

    @Autowired Locks locks;

    @Test
    void peopleCanBeLockedBeforeTasksButNotAfter() {
        long ana = userId("ana");
        long task = taskId("Design homepage");
        TransactionTemplate transaction = new TransactionTemplate(transactions);

        // the right order works
        transaction.executeWithoutResult(tx -> {
            locks.people(List.of(ana));
            assertThat(locks.task(task)).isPresent();
        });
        // the wrong order is refused at once, instead of waiting for a deadlock that may never show up in a test
        transaction.executeWithoutResult(tx -> {
            locks.task(task);
            assertThatThrownBy(() -> locks.people(List.of(ana))).isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("people must be locked before the tasks");
            assertThatThrownBy(() -> locks.person(ana)).isInstanceOf(IllegalStateException.class);
        });
        // the mark ends with the transaction: the next one may lock people again
        transaction.executeWithoutResult(tx -> assertThat(locks.person(ana)).isPresent());
    }

    @Test
    void theProjectIsLockedBeforeThePeopleAndTheTasks() {
        long ana = userId("ana");
        long task = taskId("Design homepage");
        long website = projectId("Website");
        TransactionTemplate transaction = new TransactionTemplate(transactions);

        // the right order works, with the project in either mode
        transaction.executeWithoutResult(tx -> {
            assertThat(locks.projectShared(website)).isPresent();
            locks.people(List.of(ana));
            assertThat(locks.task(task)).isPresent();
        });
        transaction.executeWithoutResult(tx -> assertThat(locks.project(website)).isPresent());
        // a project after a person, or after a task, is refused at once
        transaction.executeWithoutResult(tx -> {
            locks.person(ana);
            assertThatThrownBy(() -> locks.projectShared(website)).isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("project must be locked before");
            assertThatThrownBy(() -> locks.project(website)).isInstanceOf(IllegalStateException.class);
        });
        transaction.executeWithoutResult(tx -> {
            locks.task(task);
            assertThatThrownBy(() -> locks.projectShared(website)).isInstanceOf(IllegalStateException.class);
        });
    }

    @Test
    void aLockIsOnlyTakenInsideATransaction() {
        assertThatThrownBy(() -> locks.projectShared(projectId("Website"))).isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("inside a transaction");
        assertThatThrownBy(() -> locks.task(taskId("Design homepage"))).isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> locks.person(userId("ana"))).isInstanceOf(IllegalStateException.class);
    }

    @Test
    void peopleAreLockedByIdWhateverTheOrderTheyAreGiven() {
        // locks the three rows without failing, with unknown ids ignored
        new TransactionTemplate(transactions).executeWithoutResult(
                tx -> locks.people(List.of(userId("rui"), userId("ana"), 999999L, userId("ana"))));
    }
}
