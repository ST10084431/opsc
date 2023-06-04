package com.example.navdrawkotlin

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log

import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import com.example.navdrawkotlin.R
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*
import java.util.concurrent.TimeUnit
import kotlin.collections.ArrayList

data class Time(val hours: Long, val minutes: Long, val seconds: Long, val milliseconds: Long)
class TimerCapture : AppCompatActivity() {
    private val lapTimestamps: ArrayList<Time> = ArrayList()


    private var startTime: Long = 0
    private lateinit var comboBox: Spinner
    private lateinit var timerTextView: TextView
    private lateinit var lapTextView: TextView
    private lateinit var firestore: FirebaseFirestore
    private var hoursWorked: Int = 0
    private val milestones: ArrayList<Int> = ArrayList()
    private var timer: Timer? = null
    private var elapsedTime: Long = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_timer_capture)

        // Initialize views
        comboBox = findViewById(R.id.comboBox)
        timerTextView = findViewById(R.id.timerTextView)
        lapTextView = findViewById(R.id.lapTextView)

        // Initialize Firestore
        firestore = FirebaseFirestore.getInstance()

        val currentUser = FirebaseAuth.getInstance().currentUser
        val userEmail = currentUser?.email

        if (userEmail != null) {
            // Set up the combo box with task names
            fetchTaskNames(userEmail)
        }

        val startButton: Button = findViewById(R.id.startButton)
        startButton.setOnClickListener {
            startTimer()
        }
        val stopButton: Button = findViewById(R.id.stopButton)
        stopButton.setOnClickListener {
            stopTimer()
        }

        val resetButton: Button = findViewById(R.id.resetButton)
        resetButton.setOnClickListener {
            resetTimer()
        }
        val lapButton: Button = findViewById(R.id.lapButton)
        lapButton.setOnClickListener {
            saveLapTimestamp()
        }
        val saveButton: Button = findViewById(R.id.BtnSave)
        saveButton.setOnClickListener {
            saveDataToFirestore { success, message ->
                if (success) {
                    // Show success message
                    Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
                    Log.d("SaveDataToFirestore", message) // Log success message to console
                } else {
                    // Show failure message
                    Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
                    Log.e("SaveDataToFirestore", message) // Log failure message to console
                }
            }
        }



        // Set up the combo box selection listener
        comboBox.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>?,
                view: View?,
                position: Int,
                id: Long
            ) {
                val selectedValue = parent?.getItemAtPosition(position).toString()

                // Update hours worked
                if (userEmail != null) {
                    updateHoursWorked(userEmail, selectedValue)
                }

                // Update milestones
                if (userEmail != null) {
                    updateMilestones(userEmail, selectedValue)
                }
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {
                // Do nothing
            }
        }

        // Start the timer

    }
    private fun stopTimer() {
        timer?.cancel()
        elapsedTime = System.currentTimeMillis() - startTime
        timerTextView.text = formatTime(elapsedTime)
    }
    private fun resetTimer() {
        timer?.cancel()
        elapsedTime = 0
        lapTimestamps.clear()
        timerTextView.text = formatTime(elapsedTime)
        lapTextView.text = ""
    }

    private fun saveLapTimestamp() {
        val currentTime = System.currentTimeMillis()
        val lapTime = formatTimeLong(currentTime - startTime)

        // Save lap time to the local array
        lapTimestamps.add(lapTime)
        updateLapTextView()
    }

    private fun saveDataToFirestore(callback: (Boolean, String) -> Unit) {
        // Start time of the timer
        // ... Perform some operations or wait for the timer to complete ...
        val currentTime: Long = System.currentTimeMillis() // Current time when you want to get the lap time
        val passedTime: Long = currentTime - startTime // Calculate the elapsed time in milliseconds

        val selectedTask = comboBox.selectedItem?.toString()
        val userEmail = FirebaseAuth.getInstance().currentUser?.email

        if (userEmail != null && selectedTask != null) {
            // Save work done
            val workDoneRef = firestore.collection("users")
                .document(userEmail)
                .collection("tasks")
                .document(selectedTask)

            workDoneRef.get()
                .addOnSuccessListener { documentSnapshot ->
                    val taskData = documentSnapshot.data
                    if (taskData != null) {
                        val currentHoursWorked = taskData["hoursWorked"] as? Number ?: 0.0
                        val currentHoursWorkedDouble = currentHoursWorked.toDouble()
                        val newHoursWorked = currentHoursWorkedDouble + passedTime / 3600000.0 // Convert milliseconds to hours

                        val updatedData = hashMapOf<String, Any>(
                            "hoursWorked" to newHoursWorked
                        )

                        workDoneRef.update(updatedData)
                            .addOnSuccessListener {
                                // Work done updated successfully

                                // Add a new entry to the work done history with the current date, time, and total hours worked
                                val workDoneHistoryRef = firestore.collection("users")
                                    .document(userEmail)
                                    .collection("tasks")
                                    .document(selectedTask)
                                    .collection("workDoneHistory")
                                    .document()

                                val workDoneEntry = hashMapOf(
                                    "date" to Timestamp.now(),
                                    "totalHoursWorked" to newHoursWorked,
                                    "taskName" to selectedTask
                                )

                                workDoneHistoryRef.set(workDoneEntry)
                                    .addOnSuccessListener {
                                        // Work done history updated successfully
                                        // Notify the callback
                                        callback.invoke(true, "Work done and history updated successfully")
                                    }
                                    .addOnFailureListener { exception ->
                                        // Handle Firestore update failure
                                        // Notify the callback
                                        callback.invoke(false, "Failed to update work done history: ${exception.message}")
                                    }
                            }
                            .addOnFailureListener { exception ->
                                // Handle Firestore update failure
                                // Notify the callback
                                callback.invoke(false, "Failed to update work done: ${exception.message}")
                            }
                    } else {
                        // Handle case where the task document doesn't exist
                        callback.invoke(false, "Task document doesn't exist")
                    }
                }
                .addOnFailureListener { exception ->
                    // Handle Firestore fetch failure
                    // Notify the callback
                    callback.invoke(false, "Failed to fetch work done: ${exception.message}")
                }
        }
        resetTimer()
    }





    private fun calculateTotalLapTime(): Long {
        var totalTime: Long = 0

        for (lapTime in lapTimestamps) {
            totalTime += lapTime.hours * 3600000L
            totalTime += lapTime.minutes * 60000L
            totalTime += lapTime.seconds * 1000L
            totalTime += lapTime.milliseconds
        }
        return totalTime
    }



    private fun updateLapTextView() {
        val lapText = StringBuilder()
        for (i in lapTimestamps.indices) {
            val lapTime = lapTimestamps[i]
            lapText.append("Lap ${i + 1}: ${formatTimeToString(lapTime)}\n")
        }
        lapTextView.text = lapText.toString()
    }


    private fun fetchTaskNames(userEmail: String) {
        // Fetch task names from Firestore for the specific user
        firestore.collection("users")
            .document(userEmail)
            .collection("tasks")
            .get()
            .addOnSuccessListener { querySnapshot ->
                val taskNames = querySnapshot.documents.mapNotNull { document ->
                    document.getString("name")
                }
                // Create an ArrayAdapter and set it as the adapter for the combo box
                val adapter = ArrayAdapter(
                    this@TimerCapture,
                    android.R.layout.simple_spinner_item,
                    taskNames
                )

                adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                comboBox.adapter = adapter
            }
            .addOnFailureListener { exception ->
                // Handle Firestore fetch failure
            }
    }

    private fun updateHoursWorked(userEmail: String, selectedValue: String?) {
        selectedValue?.toIntOrNull()?.let { value ->
            // Update Firestore document with hours worked
            val docRef = firestore.collection("users")
                .document(userEmail)

            docRef.get()
                .addOnSuccessListener { documentSnapshot ->
                    val currentHoursWorked = documentSnapshot.getLong("hours_worked")
                    currentHoursWorked?.let {
                        hoursWorked = it.toInt() + value
                    }

                    docRef.update("hours_worked", hoursWorked)
                        .addOnSuccessListener {
                            // Hours worked updated successfully
                        }
                        .addOnFailureListener { exception ->
                            // Handle Firestore update failure
                        }
                }
                .addOnFailureListener { exception ->
                    // Handle Firestore fetch failure
                }
        }
    }

    private fun updateMilestones(userEmail: String, selectedValue: String) {
        // Update Firestore document with milestones
        val docRef = firestore.collection("users")
            .document(userEmail)

        docRef.get()
            .addOnSuccessListener { documentSnapshot ->
                val currentMilestones = documentSnapshot.get("milestones") as? ArrayList<Int>
                currentMilestones?.let {
                    milestones.addAll(it)
                }
                selectedValue?.toIntOrNull()?.let { value ->
                    // Only add the value if it can be parsed as an integer
                    milestones.add(value)
                }


                docRef.update("milestones", milestones)
                    .addOnSuccessListener {
                        // Milestones updated successfully
                    }
                    .addOnFailureListener { exception ->
                        // Handle Firestore update failure
                    }
            }
            .addOnFailureListener { exception ->
                // Handle Firestore fetch failure
            }
    }

    private fun startTimer() {
        startTime = System.currentTimeMillis() - elapsedTime
        timer = Timer()
        timer?.scheduleAtFixedRate(object : TimerTask() {
            override fun run() {
                runOnUiThread {
                    val currentTime = System.currentTimeMillis()
                    timerTextView.text = formatTime(currentTime - startTime)
                }
            }
        }, 0, 100) // Update every 100 milliseconds
    }


    private fun formatTime(timeInMillis: Long): String {
        val hours = TimeUnit.MILLISECONDS.toHours(timeInMillis)
        val minutes = TimeUnit.MILLISECONDS.toMinutes(timeInMillis) % 60
        val seconds = TimeUnit.MILLISECONDS.toSeconds(timeInMillis) % 60
        val milliseconds = timeInMillis % 1000

        return String.format("%02d:%02d:%02d.%03d", hours, minutes, seconds, milliseconds)
    }
    fun formatTimeToString(time: Time): String {
        return String.format("%02d:%02d:%02d.%03d", time.hours, time.minutes, time.seconds, time.milliseconds)
    }

    private fun formatTimeLong(timeInMillis: Long): Time {
        val hours = TimeUnit.MILLISECONDS.toHours(timeInMillis)
        val minutes = TimeUnit.MILLISECONDS.toMinutes(timeInMillis) % 60
        val seconds = TimeUnit.MILLISECONDS.toSeconds(timeInMillis) % 60
        val milliseconds = timeInMillis % 1000

        return Time(hours, minutes, seconds, milliseconds)
    }
}






