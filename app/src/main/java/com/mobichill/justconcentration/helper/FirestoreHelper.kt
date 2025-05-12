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

}