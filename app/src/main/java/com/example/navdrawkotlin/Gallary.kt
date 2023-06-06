package com.example.navdrawkotlin

import android.content.Context
import android.graphics.BitmapFactory
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.ImageView
import android.widget.Spinner
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.io.File



class Gallary : AppCompatActivity() {
    private lateinit var taskSpinner: Spinner
    private lateinit var photoImageView: ImageView

    private val imageList = mutableListOf<String>()
    private lateinit var firestore: FirebaseFirestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_gallary)
        firestore = FirebaseFirestore.getInstance()
        taskSpinner = findViewById(R.id.taskSpinner2)
        photoImageView = findViewById(R.id.PhotoView)
        val currentUser = FirebaseAuth.getInstance().currentUser
        val  userEmail = currentUser?.email
        fetchTaskNames(userEmail.toString())

        taskSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>?,
                view: View?,
                position: Int,
                id: Long
            ) {
                val selectedTask = taskSpinner.getItemAtPosition(position).toString()
                // Perform actions based on the selected task
                loadImageFromFile(selectedTask)
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {
                // No action needed when nothing is selected
            }
        }




    }


    private fun fetchTaskNames(userEmail: String) {
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

                // Create an ArrayAdapter and set it as the adapter for the combo box
                val adapter = ArrayAdapter(
                    this,
                    android.R.layout.simple_spinner_item,
                    tasks.map { it.name }
                )

                adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                taskSpinner.adapter = adapter






            }
            .addOnFailureListener { exception ->
              Log.d("user","faild")
            }
    }

    private fun loadImageFromFile(filePath: String) {
        val file = getOutputDirectory(this,filePath)

        if (file.exists()) {
            val bitmap = BitmapFactory.decodeFile(file.absolutePath)
            photoImageView.setImageBitmap(bitmap)
        } else {
            // File does not exist, handle error or display a placeholder image
        }
    }

    private fun getOutputDirectory(context: Context , filePath: String): File {
        val mediaDir = context.externalMediaDirs.firstOrNull()?.let {
            File(it, context.resources.getString(R.string.app_name)).apply { mkdirs() }
        }
        val outputDir = if (mediaDir != null && mediaDir.exists())
            mediaDir else context.filesDir


        val filePathWithDirectory = File(outputDir, "$filePath.jpg")
        Log.d("ImageSaving", "Output directory: $filePathWithDirectory")
        return filePathWithDirectory
    }
}