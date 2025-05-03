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
import kotlinx.coroutines.tasks.await

class FireStoreHelper private constructor() { // Private constructor to prevent instantiation
    private val TAG = javaClass.simpleName

    companion object {
        private var instance: FireStoreHelper? = null
        private val db: FirebaseFirestore by lazy { FirebaseFirestore.getInstance() }

        fun getInstance(): FireStoreHelper {
            if (instance == null) {
                instance = FireStoreHelper()
            }
            return instance!!
        }
    }

    fun saveTaskToFireStore(taskModel: TaskModel) {
        val userId = FirebaseAuth.getInstance().currentUser?.uid
        if (userId == null)
            return

        val taskRef = db.collection("users").document(userId)
            .collection("tasks").document(taskModel.id)
        taskRef.set(taskModel)
    }

    fun updateTaskToFireStore(taskModel: TaskModel) {

        val userId = FirebaseAuth.getInstance().currentUser?.uid
        if (userId == null)
            return

        val taskRef = db.collection("users").document(userId)
            .collection("tasks").document(taskModel.id)

        val taskUpdates = mapOf(
            "taskText" to taskModel.taskText,
            "alarmTimeMillis" to taskModel.alarmTimeMillis,
            "requestCode" to taskModel.requestCode
        )
        taskRef.update(taskUpdates)
    }

    suspend fun deleteTaskFromFireStore(taskModel: TaskModel) {
        val userId = FirebaseAuth.getInstance().currentUser?.uid
        if (userId == null) {
            return
        }
        // Completely delete from FireStore
        db.collection("users").document(userId)
            .collection("tasks").document(taskModel.id)
            .delete().await()
    }

    //Main function SignInWithGoogle
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
                    createdAt = document.getLong("createdAt") ?: 0L
                    // User exists -> Update last login time & save
                    updateGoogleUserToFireStore(uid, name, email, photoUrl)
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

    //Update
    private fun updateGoogleUserToFireStore(
        uid: String,
        name: String?,
        email: String?,
        photoUrl: String?,
    ) {
        val userMap = hashMapOf(
            "name" to name,
            "email" to email,
            "profilePic" to photoUrl,
            "lastLogin" to System.currentTimeMillis()  // Track last login time
        )

        db.collection("users").document(uid)
            .set(userMap, SetOptions.merge()) // Merges new data without overwriting old fields
            .addOnSuccessListener {
                Log.d(TAG, "User updated successfully!")
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "updateGoogleUserToFireStore: ", e)
            }
    }

    //Create
    private fun addNewUserByGoogle(uid: String, name: String?, email: String?, photoUrl: String?) {
        val newUser = hashMapOf(
            "name" to name,
            "email" to email,
            "profilePic" to photoUrl,
            "createdAt" to System.currentTimeMillis(),
            "lastLogin" to System.currentTimeMillis(),
            "subscription" to false
        )

        //Add new user
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
            "subscription" to false
        )
        val db = FirebaseFirestore.getInstance()
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

    fun fetchUserFromFireStore(uid: String) {
        FirebaseFirestore.getInstance().collection("users").document(uid)
            .get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    val now = System.currentTimeMillis()
                    val userId = document.getString("uid") ?: ""
                    val name = document.getString("name") ?: ""
                    val email = document.getString("email") ?: ""
                    val createdAt = document.getLong("createdAt")
                    val user = UserModel(userId, name, email, null, createdAt, now)
                    //save room
                    MyApp.instance.userRepository.saveUserToRoom(user)
                    Log.d(TAG, "UserInfo fetched successful")
                }
            }
            .addOnFailureListener {
                Log.e(TAG, "Failed to fetch user: ${it.message}")
            }
    }

    fun addConcentrateSessionToFireStore(session: ConcentrateSessionModel) {
        val userId = FirebaseAuth.getInstance().currentUser?.uid
        if (userId == null) {
            return
        }
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