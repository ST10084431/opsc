package com.example.navdrawkotlin

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.navdrawkotlin.databinding.ActivityTasksBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.*

class Tasks : AppCompatActivity() {
    private lateinit var firestore: FirebaseFirestore
    private val currentUserEmail: String = FirebaseAuth.getInstance().currentUser?.email ?: ""
    private lateinit var binding: ActivityTasksBinding
    private val calendar: Calendar = Calendar.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityTasksBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Initialize Firebase Firestore
        firestore = FirebaseFirestore.getInstance()

        binding.buttonDueDate.setOnClickListener {
            showDatePickerDialog()
        }

        binding.buttonTime.setOnClickListener {
            showTimePickerDialog()
        }

        binding.buttonSave.setOnClickListener {
            val task = Task(
                name = binding.editTextTaskName.text.toString(),
                dueDate = binding.buttonDueDate.text.toString(),
                time = binding.buttonTime.text.toString(),
                hoursWorked = binding.editTextHoursWorked.text.toString().toInt(),
                priority = binding.seekBarPriority.progress,
                category = binding.editTextCategory.text.toString()
            )

            saveTask(task)
        }
    }

    private fun showDatePickerDialog() {
        val datePickerDialog = DatePickerDialog(this,
            DatePickerDialog.OnDateSetListener { _, year, month, dayOfMonth ->
                calendar.set(year, month, dayOfMonth)
                val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                val formattedDate = dateFormat.format(calendar.time)
                binding.buttonDueDate.text = formattedDate

            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        )
        datePickerDialog.show()
    }

    private fun showTimePickerDialog() {
        val timePickerDialog = TimePickerDialog(this,
            TimePickerDialog.OnTimeSetListener { _, hourOfDay, minute ->
                calendar.set(Calendar.HOUR_OF_DAY, hourOfDay)
                calendar.set(Calendar.MINUTE, minute)
                val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
                val formattedTime = timeFormat.format(calendar.time)
                binding.buttonTime.text = formattedTime

            },
            calendar.get(Calendar.HOUR_OF_DAY),
            calendar.get(Calendar.MINUTE),
            true
        )
        timePickerDialog.show()
    }

    private fun saveTask(task: Task) {
        // Save the task to Firebase Firestore
        val userTasksCollection = firestore.collection("users").document(currentUserEmail).collection("tasks")

        // Generate a document ID using the task name
        val taskId = task.name // Assuming `name` is the property containing the task name
        val taskDocument = userTasksCollection.document(taskId)

        taskDocument.set(task)
            .addOnSuccessListener {
                // Task saved successfully
                finish()
            }
            .addOnFailureListener { exception ->
                // Handle failure, e.g., show an error message
                val errorMessage = "Failed to save task: ${exception.message}"
                // Show the error message to the user, e.g., using a toast or dialog
                showToast(errorMessage)
            }
    }


    private fun showToast(message: String) {
        // Display a toast message with the provided error message
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }
}
