package com.mobichill.justconcentration.helper

import android.content.Context
import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.mobichill.justconcentration.R
import com.mobichill.justconcentration.base.application.MyApp
import com.mobichill.justconcentration.model.ConcentrateSessionModel
import com.mobichill.justconcentration.model.TaskModel
import com.mobichill.justconcentration.model.UserModel
import com.mobichill.justconcentration.utils.Utils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

class FirestoreHelper private constructor() { // Private constructor to prevent instantiation
    companion object {
        private val TAG = FirestoreHelper::class.java.simpleName
        private var instance: FirestoreHelper? = null
        private val db: FirebaseFirestore by lazy { FirebaseFirestore.getInstance() }

        fun getInstance(): FirestoreHelper {
            if (instance == null) {
                instance = FirestoreHelper()
            }
            return instance!!
        }
    }

    // TASK HELPER
    fun saveTaskToFirestore(taskModel: TaskModel) {
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return

        val taskRef = db.collection("users").document(userId)
            .collection("tasks").document(taskModel.id)
        taskRef.set(taskModel)
    }

    fun updateTaskToFirestore(taskModel: TaskModel) {
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return

        val taskRef = db.collection("users").document(userId)
            .collection("tasks").document(taskModel.id)

        val taskUpdates = mapOf(
            "taskText" to taskModel.taskText,
            "alarmTimeMillis" to taskModel.alarmTimeMillis,
            "requestCode" to taskModel.requestCode
        )
        taskRef.update(taskUpdates)
    }

    suspend fun deleteTaskFromFirestore(taskModel: TaskModel) {
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return
        // Completely delete from Firestore
        db.collection("users").document(userId)
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
        db.collection("users").document(uid).get()
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
                MyApp.instance.userRepository.saveUserToRoom(
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

        db.collection("users").document(uid)
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

        db.collection("users").document(uid).set(newUser)
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
        db.collection("users").document(userId).set(user)
            .addOnSuccessListener {
                //Have to call these here because only when firestore success be the signup done
                //save userModel into db to load info offline
                val userModel = UserModel(userId, name, email, null, now, now)
                MyApp.instance.userRepository.saveUserToRoom(userModel)
                Utils.showToast(context, context.getString(R.string.signup_successful))
                Log.d(TAG, context.getString(R.string.signup_successful))
            }
            .addOnFailureListener {
                Utils.showToast(context, context.getString(R.string.signup_failed, it.message))
                Log.e(TAG, context.getString(R.string.signup_failed, it.message))
            }
    }

    fun fetchUserFromFirestore(uid: String) {
        db.collection("users").document(uid).get().addOnSuccessListener { document ->
            if (document.exists()) {
                val now = System.currentTimeMillis()
                val userId = document.getString("uid") ?: ""
                val name = document.getString("name") ?: ""
                val email = document.getString("email") ?: ""
                val createdAt = document.getLong("createdAt")
                val isSubscribed = document.getBoolean("subscriptionStatus") == true
                val startPro = document.getLong("subscriptionStartDate")
                val expiry = document.getLong("subscriptionExpiryDate")
                val user =
                    UserModel(
                        userId, name, email, null, createdAt, now, isSubscribed, startPro, expiry
                    )
                //save room
                MyApp.instance.userRepository.saveUserToRoom(user)
                Log.d(TAG, "UserInfo fetched successful")
            }
        }
            .addOnFailureListener {
                Log.e(TAG, "Failed to fetch user: ${it.message}")
            }
    }

    suspend fun isUserPro(): Boolean =
        withContext(Dispatchers.IO) { // Use IO dispatcher for network/db
            val currentUser = FirebaseAuth.getInstance().currentUser
            if (currentUser == null) {
                Log.w(TAG, "isUserPro check failed: No authenticated user.")
                return@withContext false // Not logged in, definitely not Pro
            }
            val userId = currentUser.uid

            try {
                val documentRef = db.collection("users").document(userId)
                val documentSnapshot = documentRef.get().await() // Suspend until fetch completes

                if (!documentSnapshot.exists()) {
                    Log.w(TAG, "isUserPro check failed: User document for $userId does not exist.")
                    return@withContext false // User document doesn't exist
                }

                val status = documentSnapshot.getBoolean("subscriptionStatus")
                val expiryDate = documentSnapshot.getLong("subscriptionExpiryDate")

                if (status == null || expiryDate == null)
                    return@withContext false // Essential fields missing

                val isProStatus = status == true
                val isActive = expiryDate > System.currentTimeMillis() // Compare Timestamps

                Log.d(
                    TAG,
                    "isUserPro check for $userId: Status='$status'(isPro=$isProStatus), Expiry=$expiryDate, IsActive=$isActive"
                )
                return@withContext isProStatus && isActive

            } catch (e: Exception) {
                Log.e(TAG, "Error checking user pro status for $userId in Firestore", e)
                return@withContext false
            }
        }

    suspend fun getSubscriptionStartDateMillis(): Long? = withContext(Dispatchers.IO) {
        val currentUser = FirebaseAuth.getInstance().currentUser
        if (currentUser == null) {
            Log.w(TAG, "getSubscriptionStartDateMillis failed: No authenticated user.")
            return@withContext null
        }
        val userId = currentUser.uid

        try {
            val documentRef = db.collection("users").document(userId)
            val documentSnapshot = documentRef.get().await()

            if (!documentSnapshot.exists()) {
                Log.w(
                    TAG,
                    "getSubscriptionStartDateMillis failed: User document for $userId does not exist."
                )
                return@withContext null
            }

            // --- First, re-verify if user is currently considered Pro ---
            // (Avoid returning a start date if they aren't actually Pro right now)
            val status = documentSnapshot.getBoolean("subscriptionStatus")
            val expiryDate = documentSnapshot.getLong("subscriptionExpiryDate")
            if (status == null || expiryDate == null) {
                Log.w(
                    TAG,
                    "getSubscriptionStartDateMillis prerequisite check failed: Missing status or expiry for user $userId."
                )
                return@withContext null
            }
            val isProStatus = status == true
            val isActive = expiryDate > System.currentTimeMillis()
            if (!isProStatus || !isActive) {
                Log.d(
                    TAG,
                    "getSubscriptionStartDateMillis failed: User $userId is not currently considered Pro (Status: $status, Active: $isActive)."
                )
                return@withContext null // Not currently Pro, so start date isn't relevant
            }
            // --- Verification complete, proceed to get start date ---

            val startDate = documentSnapshot.getLong("subscriptionStartDate")

            if (startDate == null) {
                Log.w(
                    TAG,
                    "getSubscriptionStartDateMillis failed: subscriptionStartDate field missing for Pro user $userId."
                )
                return@withContext null
            }

            return@withContext startDate

        } catch (e: Exception) {
            Log.e(TAG, "Error getting subscription start date for $userId from Firestore", e)
            return@withContext null // Return null on error
        }
    }
    // --- Optional: Add Caching ---
    // You could add functions here to cache/retrieve status from UserDao/SharedPreferences
    // to avoid hitting Firestore every single time.
    // Example:
    // suspend fun getCachedUserStatus(): LocalUserEntity? { return userDao.getCurrentUser() }
    // suspend fun refreshUserStatusFromFirestore() { /* Fetch from Firestore, update Room/Prefs */ }

    // CONCENTRATE SESSION HELPER
    fun addConcentrateSessionToFirestore(session: ConcentrateSessionModel) {
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return
        db.collection("users")
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