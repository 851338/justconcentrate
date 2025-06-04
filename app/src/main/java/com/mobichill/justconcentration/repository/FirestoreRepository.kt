package com.mobichill.justconcentration.repository

import android.content.Context
import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.ktx.snapshots
import com.mobichill.justconcentration.R
import com.mobichill.justconcentration.helper.FirestoreHelper
import com.mobichill.justconcentration.model.ConcentrateSessionModel
import com.mobichill.justconcentration.model.SubscriptionDetails
import com.mobichill.justconcentration.model.TaskModel
import com.mobichill.justconcentration.model.UserModel
import com.mobichill.justconcentration.utils.Utils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FirestoreRepository @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth,
    private val userRepository: UserRepository
) {

    companion object {
        private val TAG = FirestoreHelper::class.java.simpleName
    }

    // TASK HELPER
    fun saveTaskToFirestore(taskModel: TaskModel) {
        val userId = auth.currentUser?.uid ?: return

        val taskRef = firestore.collection("users").document(userId)
            .collection("tasks").document(taskModel.id)
        taskRef.set(taskModel)
    }

    fun updateTaskToFirestore(taskModel: TaskModel) {
        val userId = auth.currentUser?.uid ?: return

        val taskRef = firestore.collection("users").document(userId)
            .collection("tasks").document(taskModel.id)

        val taskUpdates = mapOf(
            "taskText" to taskModel.taskText,
            "alarmTimeMillis" to taskModel.alarmTimeMillis,
            "requestCode" to taskModel.requestCode
        )
        taskRef.update(taskUpdates)
    }

    suspend fun deleteTaskFromFirestore(taskModel: TaskModel) {
        val userId = auth.currentUser?.uid ?: return
        // Completely delete from Firestore
        firestore.collection("users").document(userId)
            .collection("tasks").document(taskModel.id)
            .delete().await()
    }

    // USER HELPER

    // Main function SignInWithGoogle
    fun checkIfUserExistsAndSave(
        uid: String,
        name: String?,
        email: String?,
        photoUrl: String?
    ) {
        var createdAt = 0L
        firestore.collection("users").document(uid).get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    val isSubscribed = document.getBoolean("subscriptionStatus")
                    val expiry = document.getLong("subscriptionExpiryDate")
                    val startPro = document.getLong("subscriptionStartDate")
                    createdAt = document.getLong("createdAt") ?: 0L
                    // User exists -> Update last login time & save
                    updateGoogleUserToFirestore(
                        uid, name, email, photoUrl, isSubscribed, startPro, expiry
                    )
                } else {
                    // First time login -> Create new user
                    addNewUserByGoogle(uid, name, email, photoUrl)
                }
                //Have to call these here because only when firestore success be the signIn done
                val lastLogin = System.currentTimeMillis()
                userRepository.saveUserToRoom(
                    UserModel(uid, name, email, photoUrl, createdAt, lastLogin)
                )
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "checkIfUserExistsAndSave: ", e)
            }
    }

    private fun updateGoogleUserToFirestore(
        uid: String,
        name: String?,
        email: String?,
        photoUrl: String?,
        isSubscribed: Boolean?,
        startPro: Long?,
        expiry: Long?
    ) {
        val userMap = hashMapOf(
            "name" to name,
            "email" to email,
            "profilePic" to photoUrl,
            "lastLogin" to System.currentTimeMillis(),
            "subscriptionStatus" to isSubscribed,
            "subscriptionStartDate" to startPro,
            "subscriptionExpiryDate" to expiry
        )

        firestore.collection("users").document(uid)
            .set(userMap, SetOptions.merge()) // Merges new data without overwriting old fields
            .addOnSuccessListener {
                Log.d(TAG, "User updated successfully!")
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "updateGoogleUserToFirestore: ", e)
            }
    }

    private fun addNewUserByGoogle(uid: String, name: String?, email: String?, photoUrl: String?) {
        val newUser = hashMapOf(
            "name" to name,
            "email" to email,
            "profilePic" to photoUrl,
            "createdAt" to System.currentTimeMillis(),
            "lastLogin" to System.currentTimeMillis(),
            "subscriptionStatus" to false,
            "subscriptionStartDate" to null,
            "subscriptionExpiryDate" to null
        )

        firestore.collection("users").document(uid).set(newUser)
            .addOnSuccessListener {
                Log.d(TAG, "User created successfully!")
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "addNewUserByGoogle: ", e)
            }
    }

    fun addNewUserBySigningUp(
        context: Context,
        userId: String,
        email: String,
        name: String,
    ) {
        val now = System.currentTimeMillis()
        val user = hashMapOf(
            "uid" to userId,
            "email" to email,
            "name" to name,
            "createdAt" to now,
            "lastLogin" to now,
            "subscriptionStatus" to false,
            "subscriptionStartDate" to null,
            "subscriptionExpiryDate" to null
        )
        firestore.collection("users").document(userId).set(user)
            .addOnSuccessListener {
                //Have to call these here because only when firestore success be the signup done
                //save userModel into firestore to load info offline
                val userModel = UserModel(userId, name, email, null, now, now)
                userRepository.saveUserToRoom(userModel)
                Utils.showToast(context, context.getString(R.string.signup_successful))
                Log.d(TAG, context.getString(R.string.signup_successful))
            }
            .addOnFailureListener {
                Utils.showToast(context, context.getString(R.string.signup_failed, it.message))
                Log.e(TAG, context.getString(R.string.signup_failed, it.message))
            }
    }

    fun fetchUserFromFirestore(uid: String) {
        firestore.collection("users").document(uid).get().addOnSuccessListener { document ->
            if (document.exists()) {
                val now = System.currentTimeMillis()
                val userId = document.getString("uid") ?: ""
                val name = document.getString("name") ?: ""
                val email = document.getString("email") ?: ""
                val createdAt = document.getLong("createdAt")

                val isSubscribed = document.getBoolean("subscriptionStatus") == true // Read status from root

                // --- Read subscription details from the nested map ---
                val subscriptionDetailsMap = document.get("subscriptionDetails") as? Map<String, Any>

                // --- Create the strongly typed SubscriptionDetails object ---
                val subscriptionDetails = if (subscriptionDetailsMap != null) {
                    try {
                        SubscriptionDetails(
                            productId = subscriptionDetailsMap["productId"] as? String,
                            purchaseToken = subscriptionDetailsMap["purchaseToken"] as? String,
                            expiryDateMillis = subscriptionDetailsMap["expiryDateMillis"] as? Long,
                            startDateMillis = subscriptionDetailsMap["startDateMillis"] as? Long // Read start date
                            // ... cast other fields you store ...
                        )
                    } catch (e: Exception) {
                        // Handle potential casting errors if data types in Firestore are unexpected
                        Log.e(TAG, "Error mapping subscriptionDetails map to data class for user $userId", e)
                        null // Set subscriptionDetails to null if mapping fails
                    }
                } else {
                    null // subscriptionDetails map does not exist
                }


                val user =
                    UserModel(
                        uid = userId, // Use named arguments for clarity
                        name = name,
                        email = email,
                        profilePic = null,
                        createdAt = createdAt,
                        lastLogin = now,
                        subscriptionStatus = isSubscribed,
                        // Pass the created SubscriptionDetails object
                        subscriptionDetails = subscriptionDetails
                    )

                // Save to Room
                userRepository.saveUserToRoom(user)
                Log.d(TAG, "UserInfo fetched successful for user: $userId")
            } else {
                Log.d(TAG, "User document does not exist for user: $uid")
            }
        }
            .addOnFailureListener {
                Log.e(TAG, "Failed to fetch user $uid: ${it.message}", it)
            }
    }

    // SUBSCRIPTION HELPER
    suspend fun isUserPro(): Boolean =
        withContext(Dispatchers.IO) {
            val currentUser = auth.currentUser
            if (currentUser == null) {
                Log.w(TAG, "isUserPro check failed: No authenticated user.")
                return@withContext false
            }
            val userId = currentUser.uid

            try {
                val documentRef = firestore.collection("users").document(userId)
                val documentSnapshot = documentRef.get().await()

                if (!documentSnapshot.exists()) {
                    Log.w(TAG, "isUserPro check failed: User document for $userId does not exist.")
                    return@withContext false
                }

                val status = documentSnapshot.getBoolean("subscriptionStatus")

                // --- Read the nested map and create the data class ---
                val subscriptionDetailsMap = documentSnapshot.get("subscriptionDetails") as? Map<String, Any?>
                val subscriptionDetails = if (subscriptionDetailsMap != null) {
                    SubscriptionDetails(
                        productId = subscriptionDetailsMap["productId"] as? String,
                        purchaseToken = subscriptionDetailsMap["purchaseToken"] as? String,
                        expiryDateMillis = subscriptionDetailsMap["expiryDateMillis"] as? Long,
                        startDateMillis = subscriptionDetailsMap["startDateMillis"] as? Long
                    )
                } else {
                    null
                }


                // Now check against the properties of the data class
                if (status == null || subscriptionDetails?.expiryDateMillis == null) { // Check expiryDateMillis from the data class
                    Log.w(TAG, "isUserPro check failed: Essential fields missing or null for $userId.")
                    return@withContext false
                }

                val isProStatus = status == true
                val isActive = subscriptionDetails.expiryDateMillis!! > System.currentTimeMillis() // Check expiryDateMillis from the data class

                Log.d(
                    TAG,
                    "isUserPro check for $userId: Status='$status'(isPro=$isProStatus), Expiry=${subscriptionDetails.expiryDateMillis}, IsActive=$isActive"
                )
                return@withContext isProStatus && isActive

            } catch (e: Exception) {
                Log.e(TAG, "Error checking user pro status for $userId in Firestore", e)
                return@withContext false
            }
        }

    suspend fun getSubscriptionStartDateMillis(): Long? = withContext(Dispatchers.IO) {
        val currentUser = auth.currentUser
        if (currentUser == null) {
            Log.w(TAG, "getSubscriptionStartDateMillis failed: No authenticated user.")
            return@withContext null
        }
        val userId = currentUser.uid

        try {
            val documentRef = firestore.collection("users").document(userId)
            val documentSnapshot = documentRef.get().await()

            if (!documentSnapshot.exists()) {
                Log.w(
                    TAG,
                    "getSubscriptionStartDateMillis failed: User document for $userId does not exist."
                )
                return@withContext null
            }

            // --- First, re-verify if user is currently considered Pro based on the latest data ---
            // This prerequisite check is still important: we only want to return a start date
            // if the user is currently believed to be Pro and active.

            val status = documentSnapshot.getBoolean("subscriptionStatus")

            // Read the expiry date from the correct nested path
            val expiryDate = documentSnapshot.get("subscriptionDetails.expiryDateMillis") as? Long

            // We need both status and a valid expiry date to determine current "Pro" status for this check
            if (status == null || expiryDate == null) {
                Log.w(
                    TAG,
                    "getSubscriptionStartDateMillis prerequisite check failed: Missing status or expiryDateMillis in subscriptionDetails for user $userId."
                )
                return@withContext null
            }

            val isProStatus = status == true
            val isActive = expiryDate > System.currentTimeMillis()

            if (!isProStatus || !isActive) {
                Log.d(
                    TAG,
                    "getSubscriptionStartDateMillis failed: User $userId is not currently considered Pro (Status: $status, Active: $isActive based on expiryDateMillis). Not returning start date."
                )
                return@withContext null // Not currently Pro, so returning a start date isn't relevant
            }
            // --- Prerequisite verification complete ---

            // --- Get the start date from the correct nested path ---
            val startDateMillis =
                documentSnapshot.get("subscriptionDetails.startDateMillis") as? Long // Correct field name

            if (startDateMillis == null) {
                Log.w(
                    TAG,
                    "getSubscriptionStartDateMillis failed: startDateMillis field missing or null in subscriptionDetails for Pro user $userId." // Correct field name in log
                )
                return@withContext null
            }

            Log.d(TAG, "Retrieved subscription start dateMillis for user $userId: $startDateMillis") // Log the retrieved value

            return@withContext startDateMillis // Return the retrieved value

        } catch (e: Exception) {
            Log.e(TAG, "Error getting subscription startDateMillis for $userId from Firestore", e) // Correct field name in log
            return@withContext null // Return null on error
        }
    }

    fun getUserModelFlow(userId: String): Flow<UserModel?> {
        if (userId.isBlank()) {
            Log.e(TAG, "getUserModelFlow: userId is blank, returning empty flow.")
            return kotlinx.coroutines.flow.flowOf(null) // Return a flow that immediately emits null and completes
        }
        return firestore.collection("users").document(userId)
            .snapshots() // Use the snapshots extension to get Flow<DocumentSnapshot>
            .map { snapshot ->
                val userModel = snapshot.toObject(UserModel::class.java)
                Log.d(TAG, "UserModel flow updated for $userId: $userModel")
                userModel // Emit the UserModel (or null if document doesn't exist)
            }
            .catch { e ->
                Log.e(TAG, "Error getting UserModel flow for $userId", e)
                emit(null) // Emit null on error so the flow doesn't crash
            }
            .flowOn(Dispatchers.IO) // Run Firestore operations on the IO dispatcher
    }
    // --- Optional: Add Caching ---
    // You could add functions here to cache/retrieve status from UserDao/SharedPreferences
    // to avoid hitting Firestore every single time.
    // Example:
    // suspend fun getCachedUserStatus(): LocalUserEntity? { return userDao.getCurrentUser() }
    // suspend fun refreshUserStatusFromFirestore() { /* Fetch from Firestore, update Room/Prefs */ }

    // CONCENTRATE SESSION HELPER
    fun addConcentrateSessionToFirestore(session: ConcentrateSessionModel) {
        val userId = auth.currentUser?.uid ?: return
        firestore.collection("users")
            .document(userId)
            .collection("focus_sessions")
            .document(session.id)
            .set(session)
            .addOnSuccessListener {
                Log.d(TAG, "UserInfo fetched successful")
            }
            .addOnFailureListener {
                Log.e(TAG, "Failed to push Concentrate Session: ${it.message}")
            }
    }
}