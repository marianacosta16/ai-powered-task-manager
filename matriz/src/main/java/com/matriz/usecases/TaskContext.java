package com.matriz.usecases;

import com.matriz.access.ProjectRole;
import com.matriz.projects.ProjectRecord;
import com.matriz.tasks.TaskRecord;

/** A task with its project and the role of the user in it. */
record TaskContext(TaskRecord task, ProjectRecord project, ProjectRole role) {
}
