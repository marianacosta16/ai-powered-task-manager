package com.matriz.projects;

import java.util.Arrays;

import com.matriz.shared.InvalidRequestException;

/** Phase of a project, set by the manager (P3). */
public enum ProjectPhase {
    INITIATION("Initiation"),
    PLANNING("Planning"),
    EXECUTION("Execution"),
    MONITORING("Monitoring"),
    EVALUATION("Evaluation");

    private final String label;

    ProjectPhase(String label) {
        this.label = label;
    }

    public String label() {
        return label;
    }

    public static ProjectPhase fromLabel(String field, String label) {
        return Arrays.stream(values()).filter(p -> p.label.equals(label)).findFirst().orElseThrow(
                () -> new InvalidRequestException("The phase is not valid.", field,
                        "Must be one of: Initiation, Planning, Execution, Monitoring, Evaluation."));
    }
}
