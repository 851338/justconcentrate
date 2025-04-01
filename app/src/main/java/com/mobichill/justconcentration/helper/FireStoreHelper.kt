package com.mobichill.justconcentration.helper

import android.content.Context
import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.mobichill.justconcentration.R
import com.mobichill.justconcentration.model.TaskModel
import com.mobichill.justconcentration.model.UserModel
import com.mobichill.justconcentration.repository.RoomRepository
import com.mobichill.justconcentration.util.Utils
import kotlinx.coroutines.tasks.await


class FireStoreHelper private constructor() { // Private constructor to prevent instantiation
    private val TAG = javaClass.canonicalName

    companion object {
        private var instance: FireStoreHelper? = null
        private val db: FirebaseFirestore by lazy { FirebaseFirestore.getInstance() }

        fun getInstance(): FireStoreHelper {
            if (instance == null) {
                instance = FireStoreHelper()
            }
            return instance!!
        }

        fun getDatabase(): FirebaseFirestore {
            return db
        }
    }

    fun saveTaskToFireStore(taskModel: TaskModel, onComplete: (Boolean) -> Unit) {
        val userId = FirebaseAuth.getInstance().currentUser?.uid
        if (userId == null) {
            onComplete(false)
            return
        }
        val taskRef = db.collection("users").document(userId)
            .collection("tasks").document(taskModel.id)
        taskRef.set(taskModel)
            .addOnSuccessListener {
                onComplete(true)
            }
            .addOnFailureListener {
                onComplete(false)
            }
    }

    fun getAllTasksFromFireStore(onComplete: (List<TaskModel>) -> Unit) {
        val userId = FirebaseAuth.getInstance().currentUser?.uid
        if (userId == null) {
            onComplete(emptyList())  // User not logged in
            return
        }

        val tasksRef = db.collection("users").document(userId).collection("tasks")

        tasksRef.get()
            .addOnSuccessListener { result ->
                val tasks = result.documents.mapNotNull { it.toObject(TaskModel::class.java) }
                onComplete(tasks)  // Successfully retrieved tasks
            }
            .addOnFailureListener {
                onComplete(emptyList()) // Fetching failed
            }
    }

    suspend fun getAllTasksFromFireStore(): List<TaskModel> {
        return try {
            val snapshot = db.collection("tasks").get().await()
            snapshot.documents.mapNotNull { doc ->
                doc.toObject(TaskModel::class.java)
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun updateTaskToFireStore(taskModel: TaskModel, onComplete: (Boolean) -> Unit) {

        val userId = FirebaseAuth.getInstance().currentUser?.uid
        if (userId == null) {
            onComplete(false)  // User not logged in
            return
        }
        val taskRef = db.collection("users").document(userId)
            .collection("tasks").document(taskModel.id)

        val taskUpdates = mapOf(
            "taskText" to taskModel.taskText,
            "alarmTimeMillis" to taskModel.alarmTimeMillis,
            "requestCode" to taskModel.requestCode
        )

        taskRef.update(taskUpdates)
            .addOnSuccessListener {
                onComplete(true)
            }
            .addOnFailureListener { exception ->
                onComplete(false)
            }
    }

    //Main function
    fun checkIfUserExistsAndSave(
        context: Context,
        uid: String,
        name: String?,
        email: String?,
        photoUrl: String?
    ) {
        db.collection("users").document(uid).get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    // User exists -> Update last login time & save
                    saveGoogleUserToFireStore(uid, name, email, photoUrl)
                } else {
                    // First time login -> Create new user
                    addNewUserByGoogle(uid, name, email, photoUrl)
                }
                //Have to call these here because only when firestore success be the signin done
                val lastLogin = System.currentTimeMillis()
                RoomRepository(RoomHelper.getInstance(context))
                    .saveUserToRoom(
                        UserModel(uid, name, email, photoUrl, 0L, lastLogin)
                    )

                //Save username to sharedPreference
                Utils.saveUserNameAfterLogin(context, username = name ?: "Unknown")
                // Store login state
                Utils.saveLoginState(context, true)
            }
    }

    //Update
    private fun saveGoogleUserToFireStore(
        uid: String,
        name: String?,
        email: String?,
        photoUrl: String?
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
                Log.e(TAG, e.message.toString())
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
                Log.e(TAG, e.message.toString())
            }
    }

    fun addNewUserBySigningUp(
        context: Context,
        userId: String,
        username: String,
        hashedPassword: String
    ) {
        val now = System.currentTimeMillis()
        val user = hashMapOf(
            "uid" to userId,
            "username" to username,
            "password" to hashedPassword,
            "createdAt" to now,
            "lastLogin" to now,
            "subscription" to false
        )
        val db = FirebaseFirestore.getInstance()
        db.collection("users").document(userId).set(user)
            .addOnSuccessListener {
                //Have to call these here because only when firestore success be the signup done
                //save userModel into db to load info offline
                val userModel = UserModel(userId, username, "", null, now, now)
                RoomRepository(RoomHelper.getInstance(context))
                    .saveUserToRoom(userModel)

                //Save username to sharedPreference
                Utils.saveUserNameAfterLogin(context, username)
                //update login state
                Utils.saveLoginState(context, true)

                Utils.showToast(context, context.getString(R.string.signup_successful))
                Log.d(TAG, context.getString(R.string.signup_successful))
            }
            .addOnFailureListener {
                Utils.showToast(context, context.getString(R.string.signup_failed, it.message))
                Log.e(TAG, context.getString(R.string.signup_failed, it.message))
            }
    }
}