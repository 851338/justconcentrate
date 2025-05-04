package com.mobichill.justconcentration.constants

import com.mobichill.justconcentration.model.BadgeModel

object Achievements {
    val allBadges = listOf(
        // Task-related badges
        BadgeModel(
            id = "task_1",
            name = "First Step",
            description = "Complete your first task",
            iconName = "ic_task_first",
            isUnlocked = false,
            progress = 0,
            goal = 1
        ),
        BadgeModel(
            id = "task_10",
            name = "Getting Things Done",
            description = "Complete 10 tasks",
            iconName = "ic_task_10",
            isUnlocked = false,
            progress = 0,
            goal = 10
        ),
        BadgeModel(
            id = "task_100",
            name = "Task Master",
            description = "Complete 100 tasks",
            iconName = "ic_task_100",
            isUnlocked = false,
            progress = 0,
            goal = 100
        ),
        BadgeModel(
            id = "today_star",
            name = "Today's star",
            description = "Complete all today's tasks (At least 3 tasks)",
            iconName = "ic_today_star",
            isUnlocked = false,
            progress = 0,
            goal = 0 // no fixed goal, event-based
        ),
        BadgeModel(
            id = "task_streak",
            name = "Consistency Champ",
            description = "Complete tasks 7 days in a row",
            iconName = "ic_task_streak",
            isUnlocked = false,
            progress = 0,
            goal = 7
        ),

        // 🎯 Focus session badges
        BadgeModel(
            id = "focus_1",
            name = "Focus Initiated",
            description = "Finish your first focus session",
            iconName = "ic_focus_1",
            isUnlocked = false,
            progress = 0,
            goal = 1
        ),
        BadgeModel(
            id = "focus_5",
            name = "Focus Apprentice",
            description = "Finish 5 focus sessions",
            iconName = "ic_focus_5",
            isUnlocked = false,
            progress = 0,
            goal = 5
        ),
        BadgeModel(
            id = "focus_hour_5",
            name = "5-Hour Club",
            description = "Focus for 5 hours total",
            iconName = "ic_focus_hour5",
            isUnlocked = false,
            progress = 0,
            goal = 5
        ),
        BadgeModel(
            id = "focus_hour_25",
            name = "Deep Worker",
            description = "Focus for 25 hours total",
            iconName = "ic_focus_hour25",
            isUnlocked = false,
            progress = 0,
            goal = 25
        ),
        BadgeModel(
            id = "focus_streak_3",
            name = "Laser Loop",
            description = "Focus 3 days in a row",
            iconName = "ic_focus_streak3",
            isUnlocked = false,
            progress = 0,
            goal = 3
        ),

        // 📊 Stats / Streak / General badges
        BadgeModel(
            id = "streak_3",
            name = "On Fire",
            description = "3-day activity streak",
            iconName = "ic_streak_3",
            isUnlocked = false,
            progress = 0,
            goal = 3
        ),
        BadgeModel(
            id = "streak_7",
            name = "Weekly Warrior",
            description = "7-day streak",
            iconName = "ic_streak_7",
            isUnlocked = false,
            progress = 0,
            goal = 7
        ),
        BadgeModel(
            id = "early_bird",
            name = "Early Bird",
            description = "Complete a task before 7 AM",
            iconName = "ic_early_bird",
            isUnlocked = false,
            progress = 0,
            goal = 0 // event-based
        ),
        BadgeModel(
            id = "night_owl",
            name = "Night Owl",
            description = "Focus session after midnight",
            iconName = "ic_night_owl",
            isUnlocked = false,
            progress = 0,
            goal = 0 // event-based
        ),
        BadgeModel(
            id = "comeback",
            name = "Comeback Kid",
            description = "Return after 3+ days inactive",
            iconName = "ic_comeback",
            isUnlocked = false,
            progress = 0,
            goal = 0 // event-based
        )
    )

}