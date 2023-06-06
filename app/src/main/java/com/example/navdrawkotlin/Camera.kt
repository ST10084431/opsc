package com.example.navdrawkotlin

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.Manifest
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.net.Uri
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.ImageView
import android.widget.Spinner
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContentProviderCompat.requireContext
import androidx.core.content.ContextCompat
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*

class Camera : AppCompatActivity() {
    private val CAMERA_PERMISSION_REQUEST_CODE = 123
    private val IMAGE_DIRECTORY_NAME = "MyApp"
    private lateinit var comboBox: Spinner
    private lateinit var takePhotoButton: Button
    private lateinit var imageView: ImageView
    private lateinit var firestore: FirebaseFirestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_camera)
        firestore = FirebaseFirestore.getInstance()
        comboBox = findViewById(R.id.taskSpinner2)
        takePhotoButton = findViewById(R.id.button2)
        imageView = findViewById(R.id.PhotoView)
        val currentUser = FirebaseAuth.getInstance().currentUser
        val  userEmail = currentUser?.email
        fetchTaskNames(userEmail.toString())
        takePhotoButton.setOnClickListener {
            checkCameraPermission()
        }

    }

    private fun checkCameraPermission() {
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.CAMERA
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.CAMERA),
                CAMERA_PERMISSION_REQUEST_CODE
            )
        } else {
            openCamera()
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
                comboBox.adapter = adapter






            }
            .addOnFailureListener { exception ->
                // Handle Firestore fetch failure
            }
    }
    private fun openCamera() {
        val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        if (intent.resolveActivity(packageManager) != null) {
            startActivityForResult(intent, CAMERA_PERMISSION_REQUEST_CODE)
        } else {
            Toast.makeText(this, "No camera app found", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == CAMERA_PERMISSION_REQUEST_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                openCamera()
            } else {
                Toast.makeText(this, "Camera permission denied", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == CAMERA_PERMISSION_REQUEST_CODE && resultCode == RESULT_OK) {
            val imageBitmap = data?.extras?.get("data") as Bitmap?
            imageBitmap?.let {

                saveImageToDirectory(this,it)
                imageView.setImageBitmap(it)

            }
        }
    }

    private fun saveImageToDirectory(context: Context, image: Bitmap) {
        val directory = getOutputDirectory(context) // Get the output directory where you want to save the image
        val fileName = comboBox.selectedItem?.toString()?.plus(".jpg") ?: "default.jpg"
        Log.d("ImageSaving", "$fileName")

        val file = File(directory, fileName)

        try {
            val fileOutputStream = FileOutputStream(file)
            image.compress(Bitmap.CompressFormat.JPEG, 100, fileOutputStream)
            fileOutputStream.flush()
            fileOutputStream.close()
            // Image saved successfully
            Log.d("ImageSaving", "Image saved successfully")
        } catch (e: IOException) {
            e.printStackTrace()
            // Error occurred while saving the image
            Log.e("ImageSaving", "Error occurred while saving the image: ${e.message}")
        }
    }

    private fun getOutputDirectory(context: Context): File {
        val mediaDir = context.externalMediaDirs.firstOrNull()?.let {
            File(it, context.resources.getString(R.string.app_name)).apply { mkdirs() }
        }
        val outputDir = if (mediaDir != null && mediaDir.exists())
            mediaDir else context.filesDir

        Log.d("ImageSaving", "Output directory: $outputDir")
        return outputDir
    }







}