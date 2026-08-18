package com.example.smarttaskmanager

data class Task(
    val id: Int,
    var title: String,
    var category: String,
    var isCompleted: Boolean = false
)