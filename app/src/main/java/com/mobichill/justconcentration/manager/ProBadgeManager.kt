package com.mobichill.justconcentration.manager

import android.util.Log
import com.mobichill.justconcentration.constants.Constants.PRO_BADGES.LOYALIST_BADGES_WITH_GOALS
import com.mobichill.justconcentration.constants.Constants.PRO_BADGES.PRO_SUPPORTER_BADGE_ID
import com.mobichill.justconcentration.repository.BadgeRepository
import com.mobichill.justconcentration.repository.FirestoreRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ProBadgeManager @Inject constructor(
    private val badgeRepository: BadgeRepository,
    private val firestoreRepository: FirestoreRepository
) {
    suspend fun checkAndUpdateLoyalistBadges() = withContext(Dispatchers.IO) {
        val isPro = firestoreRepository.isUserPro()
        val proStartDateMillis = firestoreRepository.getSubscriptionStartDateMillis()

        if (!isPro || proStartDateMillis == null || proStartDateMillis <= 0) {
            Log.d("BadgeManager", "User not Pro or start date invalid. Clearing loyalist progress.")
            // Clear progress if user is no longer Pro
            LOYALIST_BADGES_WITH_GOALS.keys.forEach { badgeId ->
                val badge = badgeRepository.getBadgeById(badgeId).firstOrNull()
                if (badge != null && !badge.isUnlocked && badge.progress != 0) {
                    updateBadgeProgress(badgeId, 0, badge.goal) // Reset progress
                }
            }
            return@withContext
        }

        val now = System.currentTimeMillis()
        val durationMillis = now - proStartDateMillis
        val currentDurationDays = TimeUnit.MILLISECONDS.toDays(durationMillis).toInt()

        // --- Update Progress & Check Unlocks ---
        var nextTargetGoal = -1 // Find the goal of the next badge to unlock
        var progressUpdatedForTarget = false

        // Iterate sorted by goal to find the next target correctly
        val sortedGoals = LOYALIST_BADGES_WITH_GOALS.entries.sortedBy { it.value }

        for ((badgeId, goalDays) in sortedGoals) {
            val badge = badgeRepository.getBadgeById(badgeId).firstOrNull()
            if (badge == null) {
                Log.w("BadgeManager", "Loyalist badge $badgeId not found in repository.")
                continue
            }

            if (!badge.isUnlocked) {
                // This is a potential target or needs progress update
                if (nextTargetGoal == -1) { // Found the first unlocked badge's goal
                    nextTargetGoal = goalDays
                }

                // Update progress for the current NEXT target badge
                if (goalDays == nextTargetGoal && !progressUpdatedForTarget) {
                    val newProgress = currentDurationDays.coerceAtMost(goalDays)
                    if (badge.progress != newProgress) {
                        Log.d(
                            "BadgeManager",
                            "Updating progress for $badgeId: $newProgress / $goalDays days"
                        )
                        // Directly update or use a helper if needed
                        updateBadgeProgress(badgeId, newProgress, goalDays)
                        progressUpdatedForTarget =
                            true // Only update progress for the *first* unachieved target
                    }
                }

                // Check for unlock condition
                if (currentDurationDays >= goalDays) {
                    Log.i("BadgeManager", "Unlocking loyalist badge $badgeId.")
                    // Use your existing update method to unlock
                    updateBadgeProgress(
                        badgeId,
                        goalDays,
                        goalDays
                    ) // This will set progress=goal and unlock
                }
            }
            // else: Badge is already unlocked, do nothing for this one
        }
    }

    suspend fun awardProSupporterBadge() = withContext(Dispatchers.IO) {
        Log.d("BadgeManager", "Attempting to award Pro Supporter badge.")
        // Use your existing update method. Goal is arbitrary (e.g., 1) since it's event-based.
        updateBadgeProgress(PRO_SUPPORTER_BADGE_ID, 1, 1)
    }


    /**
     * Private helper to update badge progress and state.
     * Replaces the previous 'update' function logic.
     * Assumes BadgeModel has goal field correctly set from definition.
     */
    private suspend fun updateBadgeProgress(badgeId: String, newProgress: Int, goal: Int) {
        val badge = badgeRepository.getBadgeById(badgeId).firstOrNull()
        if (badge == null) {
            Log.w("BadgeManager", "Cannot update progress for non-existent badge: $badgeId")
            return
        }

        // Only proceed if not already unlocked OR if progress needs resetting (newProgress=0)
        if (badge.isUnlocked && newProgress > 0) {
            // Log.v("BadgeManager", "Badge $badgeId already unlocked. Skipping progress update.")
            return
        }

        val actualGoal = if (badge.goal > 0) badge.goal else goal // Use defined goal or passed goal
        if (actualGoal <= 0) {
            Log.w("BadgeManager", "Badge $badgeId has invalid goal: $actualGoal. Cannot process.")
            return // Avoid division by zero or nonsensical progress
        }


        val progressToSet = newProgress.coerceAtMost(actualGoal)
        val shouldUnlock = newProgress >= actualGoal // Check unlock based on incoming progress

        // Check if an update is actually needed
        if (badge.progress == progressToSet && badge.isUnlocked == shouldUnlock) {
            // Log.v("BadgeManager", "No change needed for badge $badgeId.")
            return // No change
        }


        val unlockTimestamp = if (!badge.isUnlocked && shouldUnlock) {
            System.currentTimeMillis() // Set timestamp only when unlocking now
        } else {
            badge.unlockedAt // Keep existing timestamp if already unlocked (or null if not unlocking)
        }

        val updatedBadge = badge.copy(
            progress = progressToSet,
            isUnlocked = shouldUnlock,
            unlockedAt = unlockTimestamp,
            needsUpload = true // Mark that this needs to be synced
            // goal = actualGoal // Ensure goal is persisted if needed
        )

        Log.d(
            "BadgeManager",
            "Updating badge $badgeId: Progress=$progressToSet, Unlocked=$shouldUnlock"
        )
        badgeRepository.insertBadge(updatedBadge)
    }

    //Call badgeProgressManager.checkAndUpdateLoyalistBadges() from your ViewModel's init or data loading function (as discussed before).
    //Call badgeProgressManager.awardProSupporterBadge() from your Activity/Fragment/ViewModel right after confirming successful Pro activation (as discussed before).
}