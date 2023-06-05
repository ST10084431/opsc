package com.example.navdrawkotlin

data class Task(
    val name: String = "",
    val startDate: String = "",
    val dueDate: String = "",
    val time: String = "",
    val hoursNeeded: Int = 0,
    val hoursWorked: Int = 0,
    val priority: Int = 0,
    val category: String = ""

)
