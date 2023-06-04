package com.example.navdrawkotlin

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.text.Editable
import android.util.Log
import android.widget.Button
import android.widget.EditText
import com.example.navdrawkotlin.databinding.ActivityEditTaskBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.*

class Edit_Task : AppCompatActivity() {
    private lateinit var binding: ActivityEditTaskBinding
    private lateinit var firestore: FirebaseFirestore

    private val calendar: Calendar = Calendar.getInstance()
    private lateinit var editTimeButton: Button
    private lateinit var editDateButton: Button
    private lateinit var saveButton: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_edit_task)

        editTimeButton = findViewById(R.id.editTimeButton)
        editDateButton = findViewById(R.id.editDueDateButton)
        saveButton = findViewById(R.id.saveButton)
        val taskName = intent.getStringExtra("taskName")
        Log.d("EditTask", "Task Name: $taskName")
        editTimeButton.setOnClickListener {
            showTimePickerDialog()
        }

        editDateButton.setOnClickListener {
            showDatePickerDialog()
        }
        val nameEditText = findViewById<EditText>(R.id.editNameEditText)
        val hoursWorkedEditText = findViewById<EditText>(R.id.editHoursWorkedEditText)
        val categoryEditText = findViewById<EditText>(R.id.editCategoryEditText)
        val priorityEditText = findViewById<EditText>(R.id.editPriorityEditText)

        saveButton.setOnClickListener {
            val task = Task(
                name = nameEditText.text.toString(),
                dueDate =  editDateButton.text.toString(),
                time = editDateButton.text.toString(),
                hoursWorked = hoursWorkedEditText.text.toString().toInt(),
                priority = priorityEditText.text.toString().toInt(),
                category = categoryEditText.text.toString()

                    // nameEditText.text = Editable.Factory.getInstance().newEditable(task.name)
                //        hoursWorkedEditText.text = Editable.Factory.getInstance().newEditable(task.hoursWorked.toString())
                //        categoryEditText.text = Editable.Factory.getInstance().newEditable(task.category)
                //        priorityEditText.text = Editable.Factory.getInstance().newEditable(task.priority.toString())
                //        editTimeButton.text =Editable.Factory.getInstance().newEditable(task.time)
                //        editDateButton.text=Editable.Factory.getInstance().newEditable(task.dueDate)
            )

            saveTask(task)
        }
        firestore = FirebaseFirestore.getInstance()

        // Fetch the task details from Firestore
        if (taskName != null) {
            fetchTaskDetails(taskName)
        }

    }

    private fun showDatePickerDialog() {
        val datePickerDialog = DatePickerDialog(
            this,
            DatePickerDialog.OnDateSetListener { _, year, month, dayOfMonth ->
                calendar.set(year, month, dayOfMonth)
                val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                val formattedDate = dateFormat.format(calendar.time)
                editDateButton.text = formattedDate
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        )
        datePickerDialog.show()
    }
    private fun fetchTaskDetails(taskName: String) {
        // Fetch the task details from Firestore
        val currentUser = FirebaseAuth.getInstance().currentUser
        val userEmail = currentUser?.email

        Log.e("EditTask", "userEmail $userEmail")
        Log.e("EditTask", "TaskName $taskName")
        if (userEmail != null) {
            firestore.collection("users")
                .document(userEmail)
                .collection("tasks")
                .document(taskName)
                .get()
                .addOnSuccessListener { documentSnapshot ->
                    if (documentSnapshot.exists()) {
                        val task = documentSnapshot.toObject(Task::class.java)
                        if (task != null) {
                            setTaskDetails(task)
                            Log.e("EditTask", "i worked ")
                        }
                    } else {
                        Log.e("EditTask", "Task not found in Firestore")
                    }
                }
                .addOnFailureListener { exception ->
                    Log.e("EditTask", "Error fetching task details: ${exception.message}", exception)
                }
        }
    }



    private fun setTaskDetails(task: Task) {
        val nameEditText = findViewById<EditText>(R.id.editNameEditText)
        val hoursWorkedEditText = findViewById<EditText>(R.id.editHoursWorkedEditText)
        val categoryEditText = findViewById<EditText>(R.id.editCategoryEditText)
        val priorityEditText = findViewById<EditText>(R.id.editPriorityEditText)

        nameEditText.text = Editable.Factory.getInstance().newEditable(task.name)
        hoursWorkedEditText.text = Editable.Factory.getInstance().newEditable(task.hoursWorked.toString())
        categoryEditText.text = Editable.Factory.getInstance().newEditable(task.category)
        priorityEditText.text = Editable.Factory.getInstance().newEditable(task.priority.toString())
        editTimeButton.text =Editable.Factory.getInstance().newEditable(task.time)
        editDateButton.text=Editable.Factory.getInstance().newEditable(task.dueDate)

    }

    private fun saveTask(task: Task) {
        // Save the task to Firebase Firestore
        val currentUser = FirebaseAuth.getInstance().currentUser
        val userEmail = currentUser?.email
        val userTasksCollection = firestore.collection("users").document(userEmail.toString()).collection("tasks")

        // Generate a document ID using the task name
        val taskId = task.name // Assuming `name` is the property containing the task name
        val taskDocument = userTasksCollection.document(taskId)

        taskDocument.update(
            "category", task.category,
            "dueDate", task.dueDate,
            "hoursWorked", task.hoursWorked,
            "priority", task.priority,
            "time", task.time
        )
            .addOnSuccessListener {
                // Task updated successfully
                finish()
            }
            .addOnFailureListener { exception ->
                // Handle failure, e.g., show an error message
                val errorMessage = "Failed to update task: ${exception.message}"
                // Show the error message to the user, e.g., using a toast or dialog
            }
    }





    private fun showTimePickerDialog() {
        val calendar = Calendar.getInstance()
        val currentHour = calendar.get(Calendar.HOUR_OF_DAY)
        val currentMinute = calendar.get(Calendar.MINUTE)

        val timePickerDialog = TimePickerDialog(
            this,
            TimePickerDialog.OnTimeSetListener { _, hourOfDay, minute ->
                calendar.set(Calendar.HOUR_OF_DAY, hourOfDay)
                calendar.set(Calendar.MINUTE, minute)
                val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
                val formattedTime = timeFormat.format(calendar.time)
                editTimeButton.text = formattedTime // Update the button text with selected time
            },
            currentHour,
            currentMinute,
            false
        )
        timePickerDialog.show()
    }
}
