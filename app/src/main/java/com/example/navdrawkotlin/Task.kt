package com.example.navdrawkotlin

data class Task(
    val name: String = "",
    val dueDate: String = "",
    val time: String = "",
    val hoursWorked: Int = 0,
    val priority: Int = 0,
    val category: String = ""
)
