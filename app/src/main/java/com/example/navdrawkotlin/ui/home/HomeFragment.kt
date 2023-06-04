package com.example.navdrawkotlin.ui.home

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Spinner
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.example.navdrawkotlin.Edit_Task
import com.example.navdrawkotlin.R
import com.example.navdrawkotlin.Task
import com.example.navdrawkotlin.TimerCapture
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

    private var _binding: FragmentHomeBinding? = null

    // This property is only valid between onCreateView and onDestroyView.
    private val binding get() = _binding!!

    override fun onCreateView(

        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val homeViewModel = ViewModelProvider(this).get(HomeViewModel::class.java)

        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        val root: View = binding.root

        val editButton = binding.editButton






        comboBox = binding.taskSpinner
        lineChart = binding.chart
        // Initialize the Firestore instance
        firestore = FirebaseFirestore.getInstance()

        // Provide the user's email here
        val currentUser = FirebaseAuth.getInstance().currentUser
        val userEmail = currentUser?.email


        binding.deleteButton.setOnClickListener {
            Log.d("DeleteTask", "Task started ")
            val selectedTask = comboBox.selectedItem as? String

            Log.d("DeleteTask", "$selectedTask ")

            selectedTask?.let {

                if (userEmail != null) {
                    Log.d("DeleteTask", "$comboBox.selectedItem ")
                    deleteTask(userEmail, comboBox.selectedItem as String)

                }
            }
        }
        Log.e("DeleteTask", "Task deletion failed: ${comboBox.selectedItem} ", )


        // Fetch task names
        if (userEmail != null) {
            fetchTaskNames(userEmail)
        }

        return root
    }
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.editButton.setOnClickListener {
            val selectedTaskName = comboBox.selectedItem as? String
            val intent = Intent(requireContext(), Edit_Task::class.java)
            intent.putExtra("taskName", selectedTaskName)
            startActivity(intent)
        }



        // Rest of your code
    }

    fun DataForGraph(task: Task) {
        Log.d("Firestore", "")
        val currentUser = FirebaseAuth.getInstance().currentUser
        val userEmail = currentUser?.email
        val db = FirebaseFirestore.getInstance()

        val selectedTaskName = comboBox.selectedItem as? String

        Log.d("Firestore", "$selectedTaskName    $userEmail ")
        val collectionRef = db.collection("users").document(userEmail.toString()).collection("tasks").document(
            task.name
        ).collection("workDoneHistory")
        collectionRef.get()
            .addOnSuccessListener { querySnapshot ->
                val workDoneList = ArrayList<Entry>()
                for (document in querySnapshot) {
                    val timestamp = document.getTimestamp("date")
                    val hoursWorked = document.getDouble("totalHoursWorked")?.toFloat()

                    val date = timestamp?.toDate()
                    val entry = Entry(date?.time?.toFloat() ?: 0f, hoursWorked ?: 0f)
                    workDoneList.add(entry)

                    val dateString = SimpleDateFormat("d MMMM yyyy 'at' HH:mm:ss 'UTC'Z", Locale.getDefault()).format(date)
                    Log.d("Firestore", "Date: $dateString, Hours Worked: $hoursWorked")
                }
                populateLineChart(workDoneList)
            }
            .addOnFailureListener { exception ->
                Log.e("Firestore", "Error retrieving work done history: $exception")
                // Handle any errors that occurred while fetching the data
            }
    }
    fun populateLineChart(workDoneList: List<Entry>) {
        Log.d("LineChart", "Work Done List: $workDoneList")

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
                    val dueDate = document.getString("dueDate") ?: ""
                    val time = document.getString("time") ?: ""
                    val hoursWorked = document.getLong("hoursWorked")?.toInt() ?: 0
                    val priority = document.getLong("priority")?.toInt() ?: 0
                    val category = document.getString("category") ?: ""
                    Task(taskName, dueDate, time, hoursWorked, priority, category)
                }

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
                }

                // Handle selection change event of the combobox
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
        }
    }
    override fun onResume() {
        super.onResume()

        // Provide the user's email here
        val currentUser = FirebaseAuth.getInstance().currentUser
        val userEmail = currentUser?.email

        // Fetch task names
        if (userEmail != null) {
            fetchTaskNames(userEmail)
        }

    }
    private fun deleteTask(userEmail: String, task: String) {
        // Delete the task document from Firestore
        Log.d("DeleteTask", "Deleting task: $task")

        firestore.collection("users")
            .document(userEmail)
            .collection("tasks")
            .document(task)
            .delete()
            .addOnSuccessListener {
                // Task deleted successfully
                Log.d("DeleteTask", "Task deleted successfully")
                Toast.makeText(requireContext(), "Task deleted successfully", Toast.LENGTH_SHORT).show()
                setTaskDetails(Task("", "", "", 0, 0, ""))
                fetchTaskNames(userEmail)
            }
            .addOnFailureListener { exception ->
                // Handle delete task failure
                Log.e("DeleteTask", "Task deletion failed: ${exception.message}", exception)
                Toast.makeText(requireContext(), "Task deletion failed", Toast.LENGTH_SHORT).show()
            }
    }




    // Rest of your code

}


