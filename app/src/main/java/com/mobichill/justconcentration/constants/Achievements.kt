package com.mobichill.justconcentration.constants

import com.mobichill.justconcentration.model.BadgeModel

object Achievements {
    val allBadges = listOf(
        // Task-related badges
        BadgeModel(
            id = "task_1",
            name = "First Step",
            description = "Complete your first task",
            criteria = "",
            iconName = "ic_task_first",
            isUnlocked = false,
            progress = 0,
            goal = 1,
            isPro = false
        ),
        BadgeModel(
            id = "task_10",
            name = "Getting Things Done",
            description = "Complete 10 tasks",
            criteria = "",
            iconName = "ic_task_10",
            isUnlocked = false,
            progress = 0,
            goal = 10,
            isPro = false
        ),
        BadgeModel(
            id = "task_100",
            name = "Task Master",
            description = "Complete 100 tasks",
            criteria = "",
            iconName = "ic_task_100",
            isUnlocked = false,
            progress = 0,
            goal = 100,
            isPro = false
        ),
        BadgeModel(
            id = "today_star",
            name = "Today's star",
            description = "Complete all today's tasks",
            criteria = "Complete at least 3 tasks in a single day to earn this achievement.",
            iconName = "ic_today_star",
            isUnlocked = false,
            progress = 0,
            goal = 0,
            isPro = false
        ),
        BadgeModel(
            id = "task_streak",
            name = "Consistency Champ",
            description = "Complete tasks 7 days in a row",
            criteria = "",
            iconName = "ic_task_streak",
            isUnlocked = false,
            progress = 0,
            goal = 7,
            isPro = false
        ),

        // 🎯 Focus session badges
        BadgeModel(
            id = "focus_1",
            name = "Focus Initiated",
            description = "Finish your first focus session",
            criteria = "",
            iconName = "ic_focus_1",
            isUnlocked = false,
            progress = 0,
            goal = 1,
            isPro = false
        ),
        BadgeModel(
            id = "focus_5",
            name = "Focus Apprentice",
            description = "Finish 5 focus sessions",
            criteria = "",
            iconName = "ic_focus_5",
            isUnlocked = false,
            progress = 0,
            goal = 5,
            isPro = false
        ),
        BadgeModel(
            id = "focus_hour_5",
            name = "5-Hour Club",
            description = "Focus for 5 hours total",
            criteria = "",
            iconName = "ic_focus_hour5",
            isUnlocked = false,
            progress = 0,
            goal = 5,
            isPro = false
        ),
        BadgeModel(
            id = "focus_hour_25",
            name = "Deep Worker",
            description = "Focus for 25 hours total",
            iconName = "ic_focus_hour25",
            isUnlocked = false,
            progress = 0,
            goal = 25,
            isPro = false
        ),
        BadgeModel(
            id = "focus_streak_3",
            name = "Laser Loop",
            description = "Focus 3 days in a row",
            criteria = "",
            iconName = "ic_focus_streak3",
            isUnlocked = false,
            progress = 0,
            goal = 3,
            isPro = false
        ),

        // 📊 Stats / Streak / General badges
        BadgeModel(
            id = "streak_3",
            name = "On Fire",
            description = "3-day activity streak",
            criteria = "",
            iconName = "ic_streak_3",
            isUnlocked = false,
            progress = 0,
            goal = 3,
            isPro = false
        ),
        BadgeModel(
            id = "streak_7",
            name = "Weekly Warrior",
            description = "7-day streak",
            criteria = "",
            iconName = "ic_streak_7",
            isUnlocked = false,
            progress = 0,
            goal = 7,
            isPro = false
        ),
        BadgeModel(
            id = "early_bird",
            name = "Early Bird",
            description = "Complete a task before 7 AM",
            criteria = "Rise early and complete one task before 7 AM to unlock this badge.",
            iconName = "ic_early_bird",
            isUnlocked = false,
            progress = 0,
            goal = 0,
            isPro = false
        ),
        BadgeModel(
            id = "night_owl",
            name = "Night Owl",
            description = "Complete a task at night",
            criteria = "Burn the midnight oil—complete a task after 10 PM to unlock this badge.",
            iconName = "ic_night_owl",
            isUnlocked = false,
            progress = 0,
            goal = 0,
            isPro = false
        ),
        BadgeModel(
            id = "comeback",
            name = "Comeback Kid",
            description = "Return after 3+ days inactive",
            criteria = "Return after being inactive for 3 or more days",
            iconName = "ic_comeback",
            isUnlocked = false,
            progress = 0,
            goal = 0,
            isPro = false
        ),

        // Pro badges for subscriber
        BadgeModel(
            id = "pro_loyalist_3",
            name = "3-Month Loyalist",
            description = "Be a Pro user for 3 months",
            criteria = "Stay subscribed to Pro for 3 consecutive months",
            iconName = "ic_loyalist_3",
            isUnlocked = false,
            progress = 0,
            goal = 0,
            isPro = true
        ),
        BadgeModel(
            id = "pro_loyalist_6",
            name = "6-Month Loyalist",
            description = "Be a Pro user for 6 months",
            criteria = "Stay subscribed to Pro for 6 consecutive months",
            iconName = "ic_loyalist_6",
            isUnlocked = false,
            progress = 0,
            goal = 0,
            isPro = true
        ),
        BadgeModel(
            id = "pro_loyalist_12",
            name = "12-Month Loyalist",
            description = "Be a Pro user for 12 months",
            criteria = "Stay subscribed to Pro for 12 consecutive months",
            iconName = "ic_loyalist_12",
            isUnlocked = false,
            progress = 0,
            goal = 0,
            isPro = true
        ),
        BadgeModel(
            id = "pro_supporter",
            name = "First Time Pro",
            description = "Be a Pro user for the first time",
            criteria = "Subscribe to Pro for the first time",
            iconName = "ic_first_pro",
            isUnlocked = false,
            progress = 0,
            goal = 0,
            isPro = true
        )
    )
}