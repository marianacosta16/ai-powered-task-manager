package com.matriz.access;

/** Global role of a user (M10): a user is either a team member or a project manager. */
public enum Role {
    TEAM_MEMBER("Team member"),
    PROJECT_MANAGER("Project manager");

    private final String label;

    Role(String label) {
        this.label = label;
    }

    /** Value used by the interface and the contract. */
    public String label() {
        return label;
    }

    public static Role fromLabel(String label) {
        for (Role r : values()) {
            if (r.label.equals(label)) {
                return r;
            }
        }
        throw new IllegalArgumentException("Unknown role: " + label);
    }
}
