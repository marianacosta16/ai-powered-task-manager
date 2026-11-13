package com.matriz.projects;

import com.matriz.shared.InvalidRequestException;
import com.matriz.shared.Labelled;

/** Phase of a project, set by the manager (P3). */
public enum ProjectPhase implements Labelled {
    INITIATION("Initiation"),
    PLANNING("Planning"),
    EXECUTION("Execution"),
    MONITORING("Monitoring"),
    EVALUATION("Evaluation");

    private final String label;

    ProjectPhase(String label) {
        this.label = label;
    }

    @Override
    public String label() {
        return label;
    }

    public static ProjectPhase fromLabel(String field, String label) {
        return Labelled.find(ProjectPhase.class, label).orElseThrow(
                () -> new InvalidRequestException("The phase is not valid.", field,
                        "Must be one of: Initiation, Planning, Execution, Monitoring, Evaluation."));
    }
}
