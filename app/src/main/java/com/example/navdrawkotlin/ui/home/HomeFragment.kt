package com.example.navdrawkotlin.ui.home

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Spinner
import android.widget.Toast
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.example.navdrawkotlin.Edit_Task

import com.example.navdrawkotlin.NotificationReceiver
import com.example.navdrawkotlin.R
import com.example.navdrawkotlin.Task
import com.example.navdrawkotlin.databinding.FragmentHomeBinding
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.AxisBase
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.components.YAxis
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.Locale
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.formatter.ValueFormatter
import com.github.mikephil.charting.utils.ColorTemplate
import java.util.Date


class HomeFragment : Fragment() {

    private lateinit var firestore: FirebaseFirestore
    private lateinit var comboBox: Spinner
    private lateinit var lineChart: LineChart

    private lateinit var notificationManager: NotificationManager
    private val notificationChannelId = "UpcomingTaskNotificationChannel"
    private val notificationChannelName = "Upcoming Task Notification"
    private val notificationChannelDescription =
        "Channel for displaying upcoming task notifications"
    private val notificationId = 1001

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val homeViewModel = ViewModelProvider(this).get(HomeViewModel::class.java)
        showTestNotification()
        // Initialize the Firestore instance
        firestore = FirebaseFirestore.getInstance()

        // Initialize the notification manager
        notificationManager =
            requireContext().getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // Register the broadcast receiver
        val notificationReceiver = NotificationReceiver()
        val intentFilter = IntentFilter().apply {
            addAction("UPCOMING_TASK_NOTIFICATION")
        }
        requireContext().registerReceiver(notificationReceiver, intentFilter)

        comboBox = binding.taskSpinner
        lineChart = binding.chart

        val currentUser = FirebaseAuth.getInstance().currentUser
        val userEmail = currentUser?.email

        binding.deleteButton.setOnClickListener {
            val selectedTask = comboBox.selectedItem as? String

            selectedTask?.let {
                if (userEmail != null) {
                    deleteTask(userEmail, selectedTask)
                }
            }
        }

        binding.editButton.setOnClickListener {
            val selectedTaskName = comboBox.selectedItem as? String
            val intent = Intent(requireContext(), Edit_Task::class.java)
            intent.putExtra("taskName", selectedTaskName)
            startActivity(intent)
        }

        // Fetch task names
        if (userEmail != null) {
            fetchTaskNames(userEmail)
        }
    }

    private fun showTestNotification() {
        val channelId = "TestNotificationChannel"
        val notificationId = 123

        // Create a notification channel (required for Android Oreo and above)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Test Notification",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                enableLights(true)
                lightColor = Color.GREEN
                enableVibration(true)
            }

            // Register the channel with the system
            val notificationManager =
                requireContext().getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }

        // Check for permission
        val notificationPermission = android.Manifest.permission.VIBRATE
        val hasPermission = PackageManager.PERMISSION_GRANTED ==
                ContextCompat.checkSelfPermission(requireContext(), notificationPermission)

        if (hasPermission) {
            // Build the notification
            val notificationBuilder = NotificationCompat.Builder(requireContext(), channelId)
                .setSmallIcon(R.drawable.timewize)
                .setContentTitle("Test Notification")
                .setContentText("This is a test notification")
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setAutoCancel(true)

            // Show the notification
            val notificationManager = NotificationManagerCompat.from(requireContext())
            notificationManager.notify(notificationId, notificationBuilder.build())
        } else {
            // Handle the case when the permission is not granted
            // You can request the permission here or show an error message to the user
        }
    }




    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
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
                    requireContext(),
                    android.R.layout.simple_spinner_item,
                    tasks.map { it.name }
                )

                adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                comboBox.adapter = adapter

                // Set the initial task details on the UI
                if (tasks.isNotEmpty()) {
                    setTaskDetails(tasks[0])
                    DataForGraph(tasks[0])
                    displayUpcomingTask(tasks[0])
                }

                // Handle selection change event of the combo box
                comboBox.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                    override fun onItemSelected(
                        parent: AdapterView<*>?,
                        view: View?,
                        position: Int,
                        id: Long
                    ) {
                        val selectedTask = tasks[position]
                        setTaskDetails(selectedTask)
                        DataForGraph(selectedTask)
                    }

                    override fun onNothingSelected(parent: AdapterView<*>?) {
                        // Do nothing
                    }
                }
            }
            .addOnFailureListener { exception ->
                // Handle Firestore fetch failure
            }
    }

    private fun setTaskDetails(task: Task) {
        binding.apply {
            nameTextView.text = "Name: ${task.name}"
            hoursWorkedTextView.text = "Hours Worked: ${task.hoursWorked}"
            categoryTextView.text = "Category: ${task.category}"
            dueDateTextView.text = "Due Date: ${task.dueDate}"
            timeTextView.text = "Time: ${task.time}"
            priorityTextView.text = "Priority: ${task.priority}"
            hoursNeededTextView.text = "Hours needed: ${task.hoursNeeded}"
            startDateTextView.text ="Start Date: ${task.startDate}"
            hoursRemaining.text ="Start Date: ${task.hoursNeeded - task.hoursWorked}"
        }
    }

    private fun displayUpcomingTask(task: Task) {
        // Display the soonest upcoming task in the UI
        binding.upcomingTaskName.text = "Upcoming Task: ${task.name}"
        binding.upcomingTaskDueDate.text = "Due Date: ${task.dueDate}"
    }

    private fun DataForGraph(task: Task) {
        val currentUser = FirebaseAuth.getInstance().currentUser
        val userEmail = currentUser?.email
        val db = FirebaseFirestore.getInstance()

        val selectedTaskName = comboBox.selectedItem as? String

        val collectionRef = db.collection("users")
            .document(userEmail.toString())
            .collection("tasks")
            .document(task.name)
            .collection("workDoneHistory")

        collectionRef.get()
            .addOnSuccessListener { querySnapshot ->
                val workDoneList = ArrayList<Entry>()
                for (document in querySnapshot) {
                    val timestamp = document.getTimestamp("date")
                    val hoursWorked = document.getDouble("totalHoursWorked")?.toFloat()

                    val date = timestamp?.toDate()
                    val entry = Entry(date?.time?.toFloat() ?: 0f, hoursWorked ?: 0f)
                    workDoneList.add(entry)

                    val dateString =
                        SimpleDateFormat(
                            "d MMMM yyyy 'at' HH:mm:ss 'UTC'Z",
                            Locale.getDefault()
                        ).format(date)
                    Log.d("Firestore", "Date: $dateString, Hours Worked: $hoursWorked")
                }
                populateLineChart(workDoneList)
            }
            .addOnFailureListener { exception ->
                Log.e("Firestore", "Error retrieving work done history: $exception")
                // Handle any errors that occurred while fetching the data
            }
    }

    private fun populateLineChart(workDoneList: List<Entry>) {
        val sortedList = workDoneList.sortedBy { it.x } // Sort the list based on x-values (dates)

        val xAxis: XAxis = lineChart.xAxis
        xAxis.position = XAxis.XAxisPosition.BOTTOM
        xAxis.granularity = 1f // Set the distance between points on the X-axis
        xAxis.valueFormatter = DateAxisFormatter(sortedList) // Custom formatter for date axis

        val yAxis: YAxis = lineChart.axisLeft
        yAxis.valueFormatter = HoursAxisFormatter() // Custom formatter for hours axis
        yAxis.setLabelCount(5, true) // Set the label count for the Y-axis

        lineChart.axisRight.isEnabled = false

        val dataSet = LineDataSet(sortedList, "Hours Worked")
        dataSet.setDrawValues(false)
        dataSet.setDrawFilled(true)
        dataSet.setDrawCircles(true)
        dataSet.lineWidth = 2f
        dataSet.circleRadius = 4f
        dataSet.setCircleColor(ColorTemplate.MATERIAL_COLORS[0])
        dataSet.color = ColorTemplate.MATERIAL_COLORS[0]
        dataSet.fillColor = ColorTemplate.MATERIAL_COLORS[0]

        val lineData = LineData(dataSet)
        lineChart.data = lineData

        lineChart.notifyDataSetChanged() // Notify the chart that the data has changed
        lineChart.invalidate() // Refresh the chart
    }

    // Custom formatter for X-axis (Date)
    class DateAxisFormatter(private val workDoneList: List<Entry>) : ValueFormatter() {
        private val dateFormat = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())

        override fun getAxisLabel(value: Float, axis: AxisBase?): String {
            val index = value.toInt()
            if (index >= 0 && index < workDoneList.size) {
                val entry = workDoneList[index]
                val date = Date(entry.x.toLong())
                return dateFormat.format(date)
            }
            return ""
        }
    }

    // Custom formatter for Y-axis (Hours)
    class HoursAxisFormatter : ValueFormatter() {
        override fun getAxisLabel(value: Float, axis: AxisBase?): String {
            return value.toInt().toString()
        }
    }

    private fun deleteTask(userEmail: String, task: String) {
        // Delete the task document from Firestore
        firestore.collection("users")
            .document(userEmail)
            .collection("tasks")
            .document(task)
            .delete()
            .addOnSuccessListener {
                // Task deleted successfully
                Toast.makeText(requireContext(), "Task deleted successfully", Toast.LENGTH_SHORT)
                    .show()
                setTaskDetails(Task("", "","", "",0, 0, 0, ""))
                fetchTaskNames(userEmail)
            }
            .addOnFailureListener { exception ->
                // Handle delete task failure
                Log.e("DeleteTask", "Task deletion failed: ${exception.message}", exception)
                Toast.makeText(requireContext(), "Task deletion failed", Toast.LENGTH_SHORT).show()
            }
    }
}


