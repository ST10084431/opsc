package com.example.navdrawkotlin.ui.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Spinner
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.example.navdrawkotlin.R
import com.example.navdrawkotlin.Task
import com.example.navdrawkotlin.databinding.FragmentHomeBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.model.FieldIndex

class HomeFragment : Fragment() {

    private lateinit var firestore: FirebaseFirestore
    private lateinit var comboBox: Spinner
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


        comboBox = binding.taskSpinner
        // Initialize the Firestore instance
        firestore = FirebaseFirestore.getInstance()

        // Provide the user's email here
        val currentUser = FirebaseAuth.getInstance().currentUser
        val userEmail = currentUser?.email

        // Fetch task names
        if (userEmail != null) {
            fetchTaskNames(userEmail)
        }

        return root
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


}
