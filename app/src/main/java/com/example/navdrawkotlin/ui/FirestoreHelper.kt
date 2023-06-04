package com.example.navdrawkotlin.ui
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.FirebaseFirestore

object FirestoreHelper {
    private const val TASKS_COLLECTION = "tasks"
    private const val USERS_COLLECTION = "users"

    private val firestore: FirebaseFirestore by lazy { FirebaseFirestore.getInstance() }

    fun getUserTasksCollection(userEmail: String): CollectionReference {
        return firestore.collection(USERS_COLLECTION).document(userEmail).collection(TASKS_COLLECTION)
    }
}
