-- T13 / R30: each task has a version that goes up every time its data is changed, so a save made
-- from an old version can be refused (HTTP 409) instead of overwriting someone else's change.
ALTER TABLE task ADD COLUMN version INTEGER NOT NULL DEFAULT 1;
ALTER TABLE task ADD CONSTRAINT ck_task_version CHECK (version >= 1);
