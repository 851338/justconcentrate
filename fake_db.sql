-- Drop tables in reverse order of dependency (if any)
DROP TABLE IF EXISTS tasks;
DROP TABLE IF EXISTS badges;
DROP TABLE IF EXISTS focus_sessions;

-- ============================================================================
-- Table: focus_sessions (from ConcentrateSessionModel) - UNCHANGED
-- ============================================================================
CREATE TABLE focus_sessions (
    id TEXT PRIMARY KEY NOT NULL,
    goal TEXT NOT NULL,
    startTime INTEGER NOT NULL,
    endTime INTEGER NOT NULL,
    date TEXT NOT NULL,
    durationMinutes INTEGER NOT NULL,
    wasCompleted INTEGER NOT NULL,
    serverLastUpdatedMillis INTEGER,
    isSynced INTEGER NOT NULL DEFAULT 0,
    needsUpload INTEGER NOT NULL DEFAULT 1
);

-- ============================================================================
-- Table: badges (from BadgeModel) - Structure UNCHANGED
-- ============================================================================
CREATE TABLE badges (
    id TEXT PRIMARY KEY NOT NULL,
    name TEXT NOT NULL,
    description TEXT NOT NULL,
    iconResId INTEGER NOT NULL,               -- Placeholder integer
    progress INTEGER NOT NULL DEFAULT 0,
    isUnlocked INTEGER NOT NULL DEFAULT 0,
    unlockedAt INTEGER,
    goal INTEGER NOT NULL,
    serverLastUpdatedMillis INTEGER,
    isSynced INTEGER NOT NULL DEFAULT 0,
    needsUpload INTEGER NOT NULL DEFAULT 0    -- Defaulting to 0 as this is the base list
);

-- ============================================================================
-- Table: tasks (from TaskModel) - UNCHANGED
-- ============================================================================
CREATE TABLE tasks (
    id TEXT PRIMARY KEY NOT NULL,
    taskText TEXT NOT NULL,
    alarmTimeMillis INTEGER NOT NULL,
    requestCode INTEGER NOT NULL,
    completed INTEGER NOT NULL DEFAULT 0,
    alarmSoundUri TEXT NOT NULL,
    deletedAt INTEGER,
    createdAt INTEGER NOT NULL,
    isSynced INTEGER NOT NULL DEFAULT 0,
    completedAt INTEGER,
    serverLastUpdatedMillis INTEGER,
    needsUpload INTEGER NOT NULL DEFAULT 1,
    isDeletedLocally INTEGER NOT NULL DEFAULT 0
);


-- ============================================================================
-- Insert Sample Data for focus_sessions - UNCHANGED
-- ============================================================================
INSERT INTO focus_sessions (id, goal, startTime, endTime, date, durationMinutes, wasCompleted, serverLastUpdatedMillis, isSynced, needsUpload) VALUES
('session-uuid-1', 'Study Android Room', 1698350400000, 1698353100000, '2023-10-26', 45, 1, 1698360000000, 1, 0),
('session-uuid-2', 'Work on UI Design', 1698436800000, 1698438600000, '2023-10-27', 30, 1, NULL, 0, 1),
('session-uuid-3', 'Plan project tasks', 1698440400000, 0, '2023-10-27', 0, 0, NULL, 0, 1),
('session-uuid-4', 'Read documentation', 1698250400000, 1698252200000, '2023-10-25', 30, 1, 1698260000000, 1, 0);

-- ============================================================================
-- Insert Badge Data based on the provided Kotlin list - **UPDATED**
-- Using placeholder integers for iconResId (e.g., 2130000001...)
-- Assuming initial state: progress=0, isUnlocked=0 (false), unlockedAt=NULL, synced=0, needsUpload=0
-- ============================================================================
INSERT INTO badges (id, name, description, iconResId, progress, isUnlocked, unlockedAt, goal, serverLastUpdatedMillis, isSynced, needsUpload) VALUES
-- Task-related badges
('task_1', 'First Step', 'Complete your first task', 2130000001, 0, 0, NULL, 1, NULL, 0, 0),
('task_10', 'Getting Things Done', 'Complete 10 tasks', 2130000002, 0, 0, NULL, 10, NULL, 0, 0),
('task_100', 'Task Master', 'Complete 100 tasks', 2130000003, 0, 0, NULL, 100, NULL, 0, 0),
('today_star', 'Today''s star', 'Complete all today''s tasks (At least 3 tasks)', 2130000004, 0, 0, NULL, 0, NULL, 0, 0), -- Goal 0 for event-based
('task_streak', 'Consistency Champ', 'Complete tasks 7 days in a row', 2130000005, 0, 0, NULL, 7, NULL, 0, 0),
-- Focus session badges
('focus_1', 'Focus Initiated', 'Finish your first focus session', 2130000006, 0, 0, NULL, 1, NULL, 0, 0),
('focus_5', 'Focus Apprentice', 'Finish 5 focus sessions', 2130000007, 0, 0, NULL, 5, NULL, 0, 0),
('focus_hour_5', '5-Hour Club', 'Focus for 5 hours total', 2130000008, 0, 0, NULL, 5, NULL, 0, 0), -- Goal is 5 (hours)
('focus_hour_25', 'Deep Worker', 'Focus for 25 hours total', 2130000009, 0, 0, NULL, 25, NULL, 0, 0),-- Goal is 25 (hours)
('focus_streak_3', 'Laser Loop', 'Focus 3 days in a row', 2130000010, 0, 0, NULL, 3, NULL, 0, 0),
-- Stats / Streak / General badges
('streak_3', 'On Fire', '3-day activity streak', 2130000011, 0, 0, NULL, 3, NULL, 0, 0),
('streak_7', 'Weekly Warrior', '7-day streak', 2130000012, 0, 0, NULL, 7, NULL, 0, 0),
('early_bird', 'Early Bird', 'Complete a task before 7 AM', 2130000013, 0, 0, NULL, 0, NULL, 0, 0), -- Goal 0 for event-based
('night_owl', 'Night Owl', 'Focus session after midnight', 2130000014, 0, 0, NULL, 0, NULL, 0, 0),  -- Goal 0 for event-based
('comeback', 'Comeback Kid', 'Return after 3+ days inactive', 2130000015, 0, 0, NULL, 0, NULL, 0, 0); -- Goal 0 for event-based

-- NOTE: You might want to manually set some badges to 'unlocked' (isUnlocked=1, unlockedAt=timestamp)
-- and add some progress for testing purposes. E.g.:
-- UPDATE badges SET progress = 5, isSynced = 0, needsUpload = 1 WHERE id = 'task_10';
-- UPDATE badges SET isUnlocked = 1, unlockedAt = 1698252200000, progress = 1, isSynced = 1, needsUpload = 0 WHERE id = 'task_1';


-- ============================================================================
-- Insert Sample Data for tasks - UNCHANGED
-- ============================================================================
INSERT INTO tasks (id, taskText, alarmTimeMillis, requestCode, completed, alarmSoundUri, deletedAt, createdAt, isSynced, completedAt, serverLastUpdatedMillis, needsUpload, isDeletedLocally) VALUES
('task-uuid-1', 'Buy groceries', 0, 1001, 0, '', NULL, 1698300000000, 0, NULL, NULL, 1, 0),
('task-uuid-2', 'Submit report', 1698436800000, 1002, 0, 'content://media/internal/audio/media/12', NULL, 1698310000000, 1, NULL, 1698320000000, 0, 0),
('task-uuid-3', 'Call insurance company', 0, 1003, 1, '', NULL, 1698320000000, 1, 1698350400000, 1698360000000, 0, 0),
('task-uuid-4', 'Plan weekend trip', 0, 1004, 0, '', 1698436800000, 1698330000000, 0, NULL, NULL, 1, 1),
('task-uuid-5', 'Water plants', 1698609600000, 1005, 0, '', NULL, 1698400000000, 0, NULL, NULL, 1, 0);
