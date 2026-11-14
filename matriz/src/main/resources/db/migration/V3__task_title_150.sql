-- R14, A46: the title of a task or subtask has up to 150 characters (it was 100), to follow the example of the exercise.
-- The column only gets wider, so no data changes and V1 is left as it was applied.
ALTER TABLE task ALTER COLUMN title TYPE VARCHAR(150);
