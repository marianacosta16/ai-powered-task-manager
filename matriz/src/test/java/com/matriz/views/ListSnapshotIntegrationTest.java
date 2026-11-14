package com.matriz.views;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doAnswer;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import com.matriz.tasks.CommentQueries;
import com.matriz.testsupport.ApiIntegrationTest;

/**
 * A page of a list and its total are read in one snapshot: a read-only REPEATABLE_READ transaction
 * (the review of PR 7). The queries are watched to see in which transaction they run.
 */
class ListSnapshotIntegrationTest extends ApiIntegrationTest {

    private static final int REPEATABLE_READ = java.sql.Connection.TRANSACTION_REPEATABLE_READ;

    @MockitoSpyBean ProjectTaskQueries taskQueries;
    @MockitoSpyBean CommentQueries commentQueries;

    @Test
    void theListOfTasksIsReadInOneReadOnlySnapshot() {
        List<String> seen = new ArrayList<>();
        doAnswer(call -> {
            seen.add(describe());
            return call.callRealMethod();
        }).when(taskQueries).mainTasks(anyLong(), any(), any(), any(), any());

        get("/projects/" + projectId("Website") + "/tasks", sessionOf("ana"));

        assertThat(seen).containsExactly("read-only, REPEATABLE_READ");
    }

    @Test
    void theListOfCommentsIsReadInOneReadOnlySnapshot() {
        List<String> seen = new ArrayList<>();
        doAnswer(call -> {
            seen.add(describe());
            return call.callRealMethod();
        }).when(commentQueries).listByTask(anyLong(), any());

        get("/tasks/" + taskId("Design homepage") + "/comments", sessionOf("ana"));

        assertThat(seen).containsExactly("read-only, REPEATABLE_READ");
    }

    private static String describe() {
        if (!TransactionSynchronizationManager.isActualTransactionActive()) {
            return "no transaction";
        }
        Integer level = TransactionSynchronizationManager.getCurrentTransactionIsolationLevel();
        return (TransactionSynchronizationManager.isCurrentTransactionReadOnly() ? "read-only" : "read-write") + ", "
                + (level != null && level == REPEATABLE_READ ? "REPEATABLE_READ" : "level " + level);
    }
}
