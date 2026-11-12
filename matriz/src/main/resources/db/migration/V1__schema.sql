-- Matriz v1 schema (docs/database.md). Rules that compare several rows or depend on
-- states (R2-R5, R11, R16, R17, R21, R22, R26-R29) are validated by the application.

CREATE TABLE app_user (
    id        BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    name      VARCHAR(100) NOT NULL,
    username  VARCHAR(50)  NOT NULL,
    password  VARCHAR(100) NOT NULL,            -- hashed, never plain text
    role      VARCHAR(20)  NOT NULL,
    CONSTRAINT uq_app_user_username UNIQUE (username),
    CONSTRAINT ck_app_user_role CHECK (role IN ('Team member', 'Project manager'))
);

CREATE TABLE project (
    id           BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    name         VARCHAR(100)  NOT NULL,        -- R14
    description  VARCHAR(2000),                 -- R14
    start_date   DATE          NOT NULL,        -- R16
    start_time   TIME,
    end_date     DATE          NOT NULL,        -- R16
    end_time     TIME,
    timezone     VARCHAR(50)   NOT NULL DEFAULT 'Europe/Lisbon',  -- P26
    phase        VARCHAR(20)   NOT NULL,
    status       VARCHAR(20)   NOT NULL,
    manager_id   BIGINT        NOT NULL REFERENCES app_user (id),  -- R19
    deleted_at   TIMESTAMPTZ,                   -- M4
    CONSTRAINT ck_project_phase CHECK (phase IN
        ('Initiation', 'Planning', 'Execution', 'Monitoring', 'Evaluation')),
    CONSTRAINT ck_project_status CHECK (status IN
        ('Not started', 'In progress', 'Suspended', 'Completed'))
);
CREATE INDEX ix_project_manager ON project (manager_id);

CREATE TABLE project_member (
    project_id  BIGINT NOT NULL REFERENCES project (id),
    user_id     BIGINT NOT NULL REFERENCES app_user (id),
    PRIMARY KEY (project_id, user_id)
);
CREATE INDEX ix_project_member_user ON project_member (user_id);

CREATE TABLE task (
    id               BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    project_id       BIGINT        NOT NULL REFERENCES project (id),
    parent_task_id   BIGINT        REFERENCES task (id),   -- M1: one level only
    original_task_id BIGINT        REFERENCES task (id),   -- R6
    title            VARCHAR(100)  NOT NULL,               -- R14
    description      VARCHAR(2000),                        -- R14
    status           VARCHAR(20)   NOT NULL,
    deadline         DATE          NOT NULL,               -- R1, M3
    urgency          SMALLINT      NOT NULL,
    importance       SMALLINT      NOT NULL,
    block_reason     VARCHAR(100),                         -- R6, M8
    deleted_at       TIMESTAMPTZ,                          -- R9, R20, M4
    CONSTRAINT ck_task_status CHECK (status IN
        ('To do', 'In progress', 'In review', 'Blocked', 'Done', 'Cancelled', 'Duplicate')),
    CONSTRAINT ck_task_urgency CHECK (urgency BETWEEN 1 AND 4),
    CONSTRAINT ck_task_importance CHECK (importance BETWEEN 1 AND 4),
    CONSTRAINT ck_task_not_own_parent CHECK (parent_task_id IS NULL OR parent_task_id <> id),
    CONSTRAINT ck_task_not_own_original CHECK (original_task_id IS NULL OR original_task_id <> id)
);
CREATE INDEX ix_task_project ON task (project_id);
CREATE INDEX ix_task_parent ON task (parent_task_id);

CREATE TABLE task_assignee (
    task_id  BIGINT NOT NULL REFERENCES task (id),
    user_id  BIGINT NOT NULL REFERENCES app_user (id),
    PRIMARY KEY (task_id, user_id)
);
CREATE INDEX ix_task_assignee_user ON task_assignee (user_id);

CREATE TABLE task_comment (
    id          BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    task_id     BIGINT         NOT NULL REFERENCES task (id),
    author_id   BIGINT         NOT NULL REFERENCES app_user (id),   -- R24
    content     VARCHAR(2000)  NOT NULL,                            -- R24
    created_at  TIMESTAMPTZ    NOT NULL DEFAULT now()
);
CREATE INDEX ix_task_comment_task ON task_comment (task_id);
