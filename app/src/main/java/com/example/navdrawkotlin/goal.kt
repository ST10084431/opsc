package com.example.navdrawkotlin

import android.content.ContentValues.TAG
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.*
import androidx.core.content.ContentProviderCompat.requireContext
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class goal : AppCompatActivity() {
    private lateinit var firestore: FirebaseFirestore
    private lateinit var comboBox: Spinner
    private lateinit var save_button: Button
    private lateinit var Max: EditText
    private lateinit var Min: EditText

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_goal)
        comboBox = findViewById(R.id.taskSpinner)
        Max = findViewById(R.id.max_goal_edit_text)
        Min =   findViewById(R.id.min_goal_edit_text)


        firestore = FirebaseFirestore.getInstance() // Initialize the 'firestore' property
        val currentUser = FirebaseAuth.getInstance().currentUser
        val userEmail = currentUser?.email
        if (userEmail != null) {
            fetchTaskNames(userEmail) { taskNames ->
                // Update the ComboBox with the task names
                val adapter = ArrayAdapter(
                    comboBox.context,
                    android.R.layout.simple_spinner_item,
                    taskNames
                )
                adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                comboBox.adapter = adapter
            }
            save_button= findViewById(R.id.save_button)

            save_button.setOnClickListener {
                saveGoalsToFirestore(Min , Max)
            }

        }


    }



    private fun fetchTaskNames(userEmail: String, callback: (List<String>) -> Unit) {
        // Fetch task names from Firestore for the specific user
        firestore.collection("users")
            .document(userEmail)
            .collection("tasks")
            .get()
            .addOnSuccessListener { querySnapshot ->
                val tasks = querySnapshot.documents.mapNotNull { document ->
                    val taskName = document.getString("name") ?: ""
                    val startDate = document.getString("startDate") ?: ""
                    val dueDate = document.getString("dueDate") ?: ""
                    val time = document.getString("time") ?: ""
                    val hoursNeeded = document.getLong("hoursNeeded")?.toInt() ?: 0
                    val hoursWorked = document.getLong("hoursWorked")?.toInt() ?: 0
                    val priority = document.getLong("priority")?.toInt() ?: 0
                    val category = document.getString("category") ?: ""
                    Task(
                        taskName,
                        startDate,
                        dueDate,
                        time,
                        hoursNeeded,
                        hoursWorked,
                        priority,
                        category
                    )
                }.sortedBy { it.dueDate } // Sort the tasks by due date

                val taskNames = tasks.map { it.name }
                callback(taskNames)
            }
    }


    // Function to save minimum and maximum goals to Firestore
    fun saveGoalsToFirestore(minGoalEditText: EditText, maxGoalEditText: EditText) {
        val currentUser = FirebaseAuth.getInstance().currentUser
        val userEmail = currentUser?.email
        val selectedTask = comboBox.selectedItem.toString()
        val minGoalInput = minGoalEditText.text.toString().toDoubleOrNull()
        val maxGoalInput = maxGoalEditText.text.toString().toDoubleOrNull()

        // Check if the inputs are valid numbers
        if (minGoalInput != null && maxGoalInput != null) {
            val minGoal = minGoalInput
            val maxGoal = maxGoalInput

            val goalRef = firestore
                .collection("users")
                .document(userEmail.toString())
                .collection("tasks")
                .document(selectedTask)
                .collection("Goal")
                .document()

            val goalsData = hashMapOf(
                "minGoal" to minGoal,
                "maxGoal" to maxGoal
            )

            goalRef
                .set(goalsData)
                .addOnSuccessListener {
                    Log.d(TAG, "Minimum and maximum goals saved successfully!")
                }
                .addOnFailureListener { e ->
                    Log.e(TAG, "Error saving goals", e)
                }
        } else {
            // Handle invalid input or display an error message
            Log.e(TAG, "Invalid input for minimum or maximum goal")
        }
    }



}