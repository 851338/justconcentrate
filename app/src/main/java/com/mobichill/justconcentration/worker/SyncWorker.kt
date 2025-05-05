package com.mobichill.justconcentration.worker

import android.content.Context
import android.util.Log
import androidx.room.withTransaction
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.WriteBatch
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.mobichill.justconcentration.base.database.MyRoomDatabase
import com.mobichill.justconcentration.model.BadgeModel
import com.mobichill.justconcentration.model.ConcentrateSessionModel
import com.mobichill.justconcentration.model.TaskModel
import com.mobichill.justconcentration.repository.BadgeRepository
import com.mobichill.justconcentration.repository.ConcentrateSessionRepository
import com.mobichill.justconcentration.repository.TaskRepository
import com.mobichill.justconcentration.utils.SharedPreferencesUtils
import com.mobichill.justconcentration.utils.Utils.isNetworkAvailable
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.tasks.await

class SyncWorker(
    appContext: Context, workerParams: WorkerParameters,
    private val taskRepository: TaskRepository,
    private val sessionRepository: ConcentrateSessionRepository,
    private val badgeRepository: BadgeRepository,
    private val myRoomDatabase: MyRoomDatabase
) :
    CoroutineWorker(appContext, workerParams) {
    private val TAG = javaClass.simpleName
    private val firestore = Firebase.firestore
    private val sfUtils: SharedPreferencesUtils by lazy {
        SharedPreferencesUtils(appContext)
    }

    override suspend fun doWork(): Result {
        Log.d(TAG, "SyncWorker started.")
        if (!isNetworkAvailable(applicationContext))
            return Result.retry()
        val userId = FirebaseAuth.getInstance().currentUser?.uid
            ?: return Result.failure()
        try {
            // --- UPLOAD PHASE ---
            val uploadBatch = firestore.batch()
            var batchHasUploads = false
            val uploadedTaskIds = mutableListOf<String>()
            val deletedTaskIds = mutableListOf<String>()
            val uploadedSessionIds = mutableListOf<String>()
            val uploadedBadgeIds = mutableListOf<String>()

            // Add upload operations for each entity type to the SAME batch
            batchHasUploads = prepareTaskUploads(
                userId,
                uploadBatch,
                uploadedTaskIds,
                deletedTaskIds
            ) || batchHasUploads

            batchHasUploads =
                prepareSessionUploads(userId, uploadBatch, uploadedSessionIds) || batchHasUploads

            batchHasUploads =
                prepareBadgeUploads(userId, uploadBatch, uploadedBadgeIds) || batchHasUploads

            if (batchHasUploads) {
                Log.d("SyncWorker", "Committing FireStore upload batch...")
                uploadBatch.commit().await()
                Log.d("SyncWorker", "Upload batch committed successfully.")
                // Update local status after successful commit
                updateLocalStatusAfterUpload(
                    uploadedTaskIds = uploadedTaskIds,
                    deletedTaskIds = deletedTaskIds,
                    uploadedSessionIds = uploadedSessionIds,
                    uploadedBadgeIds = uploadedBadgeIds
                )
            } else {
                Log.d("SyncWorker", "No local changes to upload.")
            }

            // --- DOWNLOAD PHASE ---
            val lastSyncTime = sfUtils.getLastSyncTimestamp()
            val changesToApplyLocally = mutableListOf<suspend () -> Unit>() // List of DB operations

            // Fetch and prepare download operations for each entity type
            prepareTaskDownloads(userId, lastSyncTime, changesToApplyLocally)
            prepareSessionDownloads(userId, lastSyncTime, changesToApplyLocally)
            prepareBadgeDownloads(userId, lastSyncTime, changesToApplyLocally)

            if (changesToApplyLocally.isNotEmpty()) {
                Log.d("SyncWorker", "Applying downloaded changes to Room...")
                // Apply all changes in one transaction
                myRoomDatabase.withTransaction {
                    changesToApplyLocally.forEach { operation -> operation() }
                }
                Log.d("SyncWorker", "Room updated with downloaded changes.")
            } else {
                Log.d("SyncWorker", "No relevant remote changes found.")
            }
            // --- FINALIZE ---
            sfUtils.saveLastSyncTimestamp(Timestamp.now()) // Update after successful sync cycle
            Log.d("SyncWorker", "Combined sync finished successfully.")
            return Result.success()
        } catch (e: Exception) {
            Log.e("SyncWorker", "Sync error", e)
            return Result.retry()
        }
    }

    private suspend fun prepareTaskUploads(
        userId: String,
        batch: WriteBatch,
        uploadedIds: MutableList<String>,
        deletedIds: MutableList<String>
    ): Boolean {
        var operationsAdded = false
        val firestorePath = firestore.collection("users").document(userId).collection("tasks")

        try {
            val tasksToUpload = taskRepository.getUnsyncedActiveTasks()
            if (tasksToUpload.isNotEmpty()) {
                Log.d("SyncWorker", "Upload: Found ${tasksToUpload.size} new/modified tasks.")
                tasksToUpload.forEach { task ->
                    val docRef =
                        firestorePath.document(task.id) // Use client-generated UUID as doc ID
                    val taskData = task.toFireStoreMap() // Convert model to Map
                    // CRUCIAL: Add/Overwrite with server timestamp for reliable conflict resolution
                    taskData["lastModified"] = FieldValue.serverTimestamp()
                    // Use set with merge to handle partial updates if needed, or just set if map is complete
                    batch.set(docRef, taskData, SetOptions.merge())
                    operationsAdded = true
                    uploadedIds.add(task.id)
                    Log.v("SyncWorker", "Upload: Added set operation for Task ID: ${task.id}")
                }
            }

            val tasksToDelete = taskRepository.getUnsyncedDeletedTasks()
            if (tasksToDelete.isNotEmpty()) {
                Log.d("SyncWorker", "Upload: Found ${tasksToDelete.size} locally deleted tasks.")
                tasksToDelete.forEach { task ->
                    val docRef = firestorePath.document(task.id)
                    batch.delete(docRef) // Add delete operation to the batch
                    operationsAdded = true
                    deletedIds.add(task.id)
                    Log.v("SyncWorker", "Upload: Added delete operation for Task ID: ${task.id}")
                }
            }
        } catch (e: Exception) {
            Log.e("SyncWorker", "Upload: Error preparing task uploads", e)
            throw e
        }
        return operationsAdded
    }

    private suspend fun prepareSessionUploads(
        userId: String,
        batch: WriteBatch,
        uploadedIds: MutableList<String>
    ): Boolean {
        var operationsAdded = false
        // Path: users/{userId}/sessions/{sessionId} (Example path)
        val firestorePath = firestore.collection("users").document(userId).collection("sessions")
        val TAG = "SyncWorker_UploadSessions"

        try {
            // Upload New/Modified Sessions
            val sessionsToUpload = sessionRepository.getSessionsNeedingUpload()
            if (sessionsToUpload.isNotEmpty()) {
                Log.d(TAG, "Found ${sessionsToUpload.size} new/modified sessions to upload.")
                sessionsToUpload.forEach { session ->
                    val docRef = firestorePath.document(session.id)
                    val sessionData = session.toFireStoreMap()
                    sessionData["lastUpdated"] = FieldValue.serverTimestamp()
                    batch.set(docRef, sessionData, SetOptions.merge())
                    operationsAdded = true
                    uploadedIds.add(session.id)
                    Log.v(TAG, "Added set operation for Session ID: ${session.id}")
                }
            }

        } catch (e: Exception) {
            Log.e(TAG, "Error preparing session uploads", e)
            throw e // Propagate error
        }
        return operationsAdded
    }

    private suspend fun prepareBadgeUploads(
        userId: String,
        batch: WriteBatch,
        uploadedIds: MutableList<String>
    ): Boolean {
        var operationsAdded = false
        // Path: users/{userId}/badgeProgress/{badgeId} (Example path)
        val firestorePath =
            firestore.collection("users").document(userId).collection("badgeProgress")
        val TAG = "SyncWorker_UploadBadges"

        try {
            // Get badges with local changes needing upload
            val badgesToUpload =
                badgeRepository.getBadgesNeedingUpload() // Query: WHERE needsUpload = true
            if (badgesToUpload.isNotEmpty()) {
                Log.d(TAG, "Found ${badgesToUpload.size} modified badges to upload.")
                badgesToUpload.forEach { badge ->
                    val docRef = firestorePath.document(badge.id) // Use badge definition ID
                    val badgeData = badge.toFireStoreMap() // Get progress data map
                    badgeData["lastUpdated"] = FieldValue.serverTimestamp() // Add server timestamp
                    // SetOptions.merge() is good here to only update progress fields
                    batch.set(docRef, badgeData, SetOptions.merge())
                    operationsAdded = true
                    uploadedIds.add(badge.id)
                    Log.v(TAG, "Added set operation for Badge ID: ${badge.id}")
                }
            }
            // Deletion usually doesn't apply to badges unless resetting progress

        } catch (e: Exception) {
            Log.e(TAG, "Error preparing badge uploads", e)
            throw e // Propagate error
        }
        return operationsAdded
    }

    private suspend fun prepareSessionDownloads(
        userId: String,
        lastSyncTime: Timestamp,
        changesToApplyLocally: MutableList<suspend () -> Unit>
    ) {
        val firestorePath = firestore.collection("users").document(userId).collection("sessions")
        val TAG = "SyncWorker_DownloadSessions"

        try {
            val query = firestorePath.whereGreaterThan("lastUpdated", lastSyncTime)
            val snapshot = query.get().await()
            Log.d(TAG, "Fetched ${snapshot.size()} potential session changes from Firestore.")

            if (snapshot.isEmpty) return

            snapshot.documents.forEach { doc ->
                val docId = doc.id
                val remoteData = doc.data

                if (remoteData == null || remoteData["lastUpdated"] !is Timestamp) {
                    Log.w(
                        TAG,
                        "Skipping Session ID $docId - null data or missing/invalid 'lastUpdated'."
                    )
                    return@forEach
                }

                // Handle soft delete if applicable (check 'deletedAt' or 'isDeleted')
                // ...

                val remoteSession =
                    ConcentrateSessionModel.fromFireStoreMap(docId, remoteData) ?: return@forEach
                val localSession = sessionRepository.getSessionById(docId)
                if (doc.data == null) {
                    Log.w(TAG, "Skipping session download for ID $docId - data was null.")
                    return@forEach // Skip if data is missing for some reason
                }
                if (localSession == null) {
                    Log.d(TAG, "Session ${doc.id} not found locally. Inserting.")
                    val newSession = ConcentrateSessionModel.fromFireStoreMap(doc.id, doc.data!!)
                    if (newSession == null) return@forEach
                    sessionRepository.addConcentrateSessionToRoom(newSession)
                } else {
                    val localServerTime = localSession.serverLastUpdatedMillis ?: 0L
                    val remoteServerTime = remoteSession.serverLastUpdatedMillis ?: Long.MAX_VALUE

                    if (remoteServerTime > localServerTime) {
                        Log.d(TAG, "Remote session newer (ID: $docId). Preparing update.")
                        changesToApplyLocally.add { sessionRepository.upsertSession(remoteSession) }
                    } else {
                        Log.d(
                            TAG,
                            "Local session same or newer (ID: $docId). Skipping remote update."
                        )
                        // Optional state correction
                        if (remoteServerTime == localServerTime && !localSession.isSynced) {
                            Log.w(
                                TAG,
                                "Correcting local sync state for matching timestamp session (ID: $docId)"
                            )
                            changesToApplyLocally.add {
                                sessionRepository.markSessionAsSyncedById(
                                    docId,
                                    remoteServerTime
                                )
                            }
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error preparing session downloads", e)
            throw e // Propagate error
        }
    }

    private suspend fun prepareBadgeDownloads(
        userId: String,
        lastSyncTime: Timestamp,
        changesToApplyLocally: MutableList<suspend () -> Unit>
    ) {
        val firestorePath =
            firestore.collection("users").document(userId).collection("badgeProgress")
        val TAG = "SyncWorker_DownloadBadges"

        try {
            val query = firestorePath.whereGreaterThan("lastUpdated", lastSyncTime)
            val snapshot = query.get().await()
            Log.d(TAG, "Fetched ${snapshot.size()} potential badge changes from Firestore.")

            if (snapshot.isEmpty) return

            snapshot.documents.forEach { doc ->
                val docId = doc.id
                val remoteData = doc.data

                if (remoteData == null || remoteData["lastUpdated"] !is Timestamp) {
                    Log.w(
                        TAG,
                        "Skipping Badge ID $docId - null data or missing/invalid 'lastUpdated'."
                    )
                    return@forEach
                }

                // Convert FireStore data to local model
                val remoteBadge = BadgeModel.fromFireStoreMap(docId, remoteData) ?: return@forEach

                // Get local version (make sure local DB is pre-populated with badge definitions)
                val localBadge = badgeRepository.getBadgeById(docId)
                // Compare timestamps
                val localServerTime = localBadge.first().serverLastUpdatedMillis ?: 0L
                val remoteServerTime = remoteBadge.serverLastUpdatedMillis ?: Long.MAX_VALUE

                if (remoteServerTime > localServerTime) {
                    Log.d(TAG, "Remote badge newer (ID: $docId). Preparing update.")
                    // Merge remote progress data into local definition state (optional but safer)
                    val badgeToSave = localBadge.first().copy( // Keep local definition fields
                        progress = remoteBadge.progress,
                        isUnlocked = remoteBadge.isUnlocked,
                        unlockedAt = remoteBadge.unlockedAt,
                        serverLastUpdatedMillis = remoteBadge.serverLastUpdatedMillis,
                        isSynced = true,
                        needsUpload = false
                    )
                    changesToApplyLocally.add { badgeRepository.upsertBadge(badgeToSave) }
                } else {
                    Log.d(TAG, "Local badge same or newer (ID: $docId). Skipping remote update.")
                    // Optional state correction
                    if (remoteServerTime == localServerTime && !localBadge.first().isSynced) {
                        Log.w(
                            TAG,
                            "Correcting local sync state for matching timestamp badge (ID: $docId)"
                        )
                        changesToApplyLocally.add {
                            badgeRepository.markBadgeAsSyncedById(
                                docId,
                                remoteServerTime
                            )
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error preparing badge downloads", e)
            throw e // Propagate error
        }
    }

    private suspend fun prepareTaskDownloads(
        userId: String,
        lastSyncTime: Timestamp,
        changesToApplyLocally: MutableList<suspend () -> Unit>
    ) {
        val firestorePath = firestore.collection("users").document(userId).collection("tasks")
        val TAG = "SyncWorker_DownloadTasks" // Specific tag

        try {
            // Query FireStore for documents modified after the last sync timestamp
            val query = firestorePath.whereGreaterThan("lastModified", lastSyncTime)
            val snapshot = query.get().await()
            Log.d(TAG, "Fetched ${snapshot.size()} potential task changes from Firestore.")

            if (snapshot.isEmpty) {
                return // No changes found
            }

            snapshot.documents.forEach { doc ->
                val docId = doc.id
                val remoteData = doc.data

                // Basic validation
                if (remoteData == null) {
                    Log.w(TAG, "Skipping Task ID $docId - null data.")
                    return@forEach // continue to next document in loop
                }
                // Ensure server timestamp exists (critical for comparison)
                if (remoteData["lastModified"] !is Timestamp) {
                    Log.w(
                        TAG,
                        "Skipping Task ID $docId - missing or invalid 'lastModified' Firestore timestamp."
                    )
                    return@forEach
                }

//                 Check for soft delete flag (Adapt if using soft delete)
                val isRemoteDeleted =
                    remoteData["deletedAt"] != null // Example: if deletedAt is set, it's soft-deleted
                if (isRemoteDeleted) {
                    Log.d(TAG, "Remote task soft-deleted (ID: $docId). Preparing local delete.")
                    changesToApplyLocally.add { taskRepository.deleteTaskPermanentlyById(docId) }
                    return@forEach
                }

                // Convert Firestore data to local Room model using the companion object function
                val remoteTask = TaskModel.fromFireStoreMap(docId, remoteData)

                if (remoteTask == null)
                    return@forEach

                // --- Conflict Resolution (Last Write Wins based on Server Timestamp) ---
                val localTask = taskRepository.getTaskById(docId).firstOrNull() // Fetch local version

                if (localTask == null) {
                    Log.d(TAG, "Task does not exist locally (ID: $docId). Preparing insert.")
                    changesToApplyLocally.add { taskRepository.upsertTask(remoteTask) }
                } else {
                    // Local exists -> Compare server timestamps (stored locally vs new from server)
                    val localServerTime =
                        localTask.serverLastUpdatedMillis ?: 0L // Use 0 if never synced before
                    val remoteServerTime = remoteTask.serverLastUpdatedMillis
                        ?: Long.MAX_VALUE // Should not be null here based on earlier check

                    if (remoteServerTime > localServerTime) {
                        // Remote is newer -> Update local version
                        Log.d(
                            TAG,
                            "Remote task newer (ID: $docId, RemoteTime: $remoteServerTime > LocalTime: $localServerTime). Preparing update."
                        )
                        // remoteTask already has isSynced=true, needsUpload=false set by fromFirestoreMap
                        changesToApplyLocally.add { taskRepository.upsertTask(remoteTask) }
                    } else {
                        // Local is same age or newer (or remote time is somehow missing)
                        // This could happen if a local change was uploaded but the download check runs before the upload confirmation updated local state fully.
                        Log.d(
                            TAG,
                            "Local task same or newer (ID: $docId, RemoteTime: $remoteServerTime <= LocalTime: $localServerTime). Skipping remote update."
                        )
                        // Optional: Ensure local state is correct if timestamps match
                        if (remoteServerTime == localServerTime && !localTask.isSynced) {
                            Log.w(
                                TAG,
                                "Correcting local sync state for matching timestamp task (ID: $docId)"
                            )
                            // Prepare lambda to update ONLY sync flags, not overwrite other data
                            changesToApplyLocally.add {
                                taskRepository.markTaskAsSyncedById(
                                    docId,
                                    remoteServerTime
                                )
                            }
                        }
                    }
                }


                // --- End Conflict Resolution ---
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error preparing task downloads", e)
            // Let the main doWork catch block handle retry/failure for the whole sync
            throw e // Re-throw to signal failure in the download phase if needed
        }
    }

    private suspend fun updateLocalStatusAfterUpload(
        uploadedTaskIds: List<String>,
        deletedTaskIds: List<String>,
        uploadedSessionIds: List<String>,
        uploadedBadgeIds: List<String>
    ) {
        val TAG = "SyncWorker_LocalUpdate"
        if (uploadedTaskIds.isEmpty() && deletedTaskIds.isEmpty() &&
            uploadedSessionIds.isEmpty() &&
            uploadedBadgeIds.isEmpty()
        ) {
            Log.d(TAG, "No local status updates needed after upload.")
            return
        }

        Log.d(TAG, "Starting local status update transaction after successful upload...")
        try {
            myRoomDatabase.withTransaction {
                // --- Update Tasks ---
                if (uploadedTaskIds.isNotEmpty()) {
                    Log.d(TAG, "Marking ${uploadedTaskIds.size} tasks as synced.")
                    // You need a DAO method for this, see below
                    taskRepository.markTasksAsSyncedAfterUpload(uploadedTaskIds)
                }
                if (deletedTaskIds.isNotEmpty()) {
                    Log.d(TAG, "Permanently deleting ${deletedTaskIds.size} tasks locally.")
                    taskRepository.deleteTasksPermanentlyByIds(deletedTaskIds)
                }

                // --- Update Sessions ---
                if (uploadedSessionIds.isNotEmpty()) {
                    Log.d(TAG, "Marking ${uploadedSessionIds.size} sessions as synced.")
                    sessionRepository.markSessionsAsSyncedAfterUpload(uploadedSessionIds)
                }

                // --- Update Badges ---
                if (uploadedBadgeIds.isNotEmpty()) {
                    Log.d(TAG, "Marking ${uploadedBadgeIds.size} badges as synced.")
                    badgeRepository.markBadgesAsSyncedAfterUpload(uploadedBadgeIds)
                }
                // Add other entities if necessary...
            }
            Log.d(TAG, "Local status update transaction finished successfully.")
        } catch (e: Exception) {
            Log.e(TAG, "Error during local status update transaction", e)
            // Important: If this fails, the local state is inconsistent with Firestore.
            // The next sync *should* ideally correct it, but it's an error state.
            // Rethrow might cause the worker to retry, potentially re-uploading? Careful.
            // Or just log and hope the next sync download fixes discrepancies.
            throw e // Or handle more gracefully depending on desired retry behavior
        }
    }
} //TODO change lastModified into lastUpdated