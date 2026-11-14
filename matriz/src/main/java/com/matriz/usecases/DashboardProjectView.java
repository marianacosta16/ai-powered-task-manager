package com.matriz.usecases;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.matriz.views.ProjectProgress;
import com.matriz.views.StatusCounts;

/** DashboardProject of the contract. The progress is only there for the manager of the project (US06). */
public record DashboardProjectView(ProjectSummaryView project, StatusCounts counts,
        @JsonInclude(JsonInclude.Include.NON_NULL) ProjectProgress progress) {
}
