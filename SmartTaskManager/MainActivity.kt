package com.example.smarttaskmanager

import android.content.Context
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SearchView
import androidx.appcompat.widget.Toolbar
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.snackbar.Snackbar
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class MainActivity : AppCompatActivity() {

    private val allTasks = mutableListOf<Task>()
    private val displayedTasks = mutableListOf<Task>()
    private lateinit var adapter: TaskAdapter
    private lateinit var toolbar: Toolbar
    private lateinit var rvTasks: RecyclerView

    private var currentFilter = "ALL" // Options: "ALL", "PENDING", "COMPLETED"
    private var searchQuery = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)

        loadTasksFromPreferences()

        if (allTasks.isEmpty() && isFirstRun()) {
            allTasks.add(Task(1, "Review Android Lecture", "Study", false))
            allTasks.add(Task(2, "Submit Internship Assignment", "Work", true))
            allTasks.add(Task(3, "Build Smart Task App", "Coding", false))
            saveTasksToPreferences()
        }

        rvTasks = findViewById(R.id.rvTasks)
        adapter = TaskAdapter(
            tasks = displayedTasks,
            onTaskStatusChanged = {
                saveTasksToPreferences()
                updateToolbarSubtitle()
            },
            onDeleteClicked = { task ->
                deleteTaskWithUndo(task)
            }
        )

        rvTasks.layoutManager = LinearLayoutManager(this)
        rvTasks.adapter = adapter

        setupSwipeToDelete()

        val etTaskTitle = findViewById<EditText>(R.id.etTaskTitle)
        val btnAdd = findViewById<Button>(R.id.btnAdd)

        btnAdd.setOnClickListener {
            val title = etTaskTitle.text.toString().trim()
            if (title.isNotEmpty()) {
                val newTask = Task(
                    id = System.currentTimeMillis().toInt(),
                    title = title,
                    category = "General",
                    isCompleted = false
                )
                allTasks.add(0, newTask)
                saveTasksToPreferences()
                etTaskTitle.text.clear()
                applyFilterAndSearch()
            } else {
                Toast.makeText(this, "Task title cannot be empty", Toast.LENGTH_SHORT).show()
            }
        }

        applyFilterAndSearch()
    }


    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.main_menu, menu)

        val searchItem = menu?.findItem(R.id.action_search)
        val searchView = searchItem?.actionView as? SearchView

        searchView?.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean = false

            override fun onQueryTextChange(newText: String?): Boolean {
                searchQuery = newText.orEmpty()
                applyFilterAndSearch()
                return true
            }
        })

        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.filter_all -> {
                currentFilter = "ALL"
                applyFilterAndSearch()
                true
            }
            R.id.filter_pending -> {
                currentFilter = "PENDING"
                applyFilterAndSearch()
                true
            }
            R.id.filter_completed -> {
                currentFilter = "COMPLETED"
                applyFilterAndSearch()
                true
            }
            R.id.action_clear_completed -> {
                val removedCount = allTasks.count { it.isCompleted }
                if (removedCount > 0) {
                    allTasks.removeAll { it.isCompleted }
                    saveTasksToPreferences()
                    applyFilterAndSearch()
                    Toast.makeText(this, "Cleared $removedCount completed task(s)", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this, "No completed tasks to clear", Toast.LENGTH_SHORT).show()
                }
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }


    private fun applyFilterAndSearch() {
        var filteredList = when (currentFilter) {
            "PENDING" -> allTasks.filter { !it.isCompleted }
            "COMPLETED" -> allTasks.filter { it.isCompleted }
            else -> allTasks
        }

        if (searchQuery.isNotEmpty()) {
            filteredList = filteredList.filter {
                it.title.contains(searchQuery, ignoreCase = true) ||
                        it.category.contains(searchQuery, ignoreCase = true)
            }
        }

        displayedTasks.clear()
        displayedTasks.addAll(filteredList)
        adapter.updateList(displayedTasks)
        updateToolbarSubtitle()
    }

    private fun updateToolbarSubtitle() {
        val pendingCount = allTasks.count { !it.isCompleted }
        toolbar.subtitle = "$pendingCount pending task(s)"
    }

    private fun setupSwipeToDelete() {
        val swipeHandler = object : ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT) {
            override fun onMove(
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                target: RecyclerView.ViewHolder
            ): Boolean = false

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                val position = viewHolder.adapterPosition
                if (position != RecyclerView.NO_POSITION && position < displayedTasks.size) {
                    val taskToDelete = displayedTasks[position]
                    deleteTaskWithUndo(taskToDelete)
                }
            }
        }

        val itemTouchHelper = ItemTouchHelper(swipeHandler)
        itemTouchHelper.attachToRecyclerView(rvTasks)
    }

    private fun deleteTaskWithUndo(task: Task) {
        val originalIndex = allTasks.indexOf(task)
        allTasks.remove(task)
        saveTasksToPreferences()
        applyFilterAndSearch()

        Snackbar.make(rvTasks, "Task deleted", Snackbar.LENGTH_LONG)
            .setAction("UNDO") {
                if (originalIndex != -1 && originalIndex <= allTasks.size) {
                    allTasks.add(originalIndex, task)
                } else {
                    allTasks.add(task)
                }
                saveTasksToPreferences()
                applyFilterAndSearch()
            }
            .show()
    }


    private fun saveTasksToPreferences() {
        val sharedPreferences = getSharedPreferences("TaskManagerPrefs", Context.MODE_PRIVATE)
        val editor = sharedPreferences.edit()
        val gson = Gson()
        val json = gson.toJson(allTasks)
        editor.putString("key_tasks_list", json)
        editor.apply()
    }

    private fun loadTasksFromPreferences() {
        val sharedPreferences = getSharedPreferences("TaskManagerPrefs", Context.MODE_PRIVATE)
        val gson = Gson()
        val json = sharedPreferences.getString("key_tasks_list", null)

        if (!json.isNullOrEmpty()) {
            val type = object : TypeToken<MutableList<Task>>() {}.type
            val loadedList: MutableList<Task>? = gson.fromJson(json, type)
            if (loadedList != null) {
                allTasks.clear()
                allTasks.addAll(loadedList)
            }
        }
    }

    private fun isFirstRun(): Boolean {
        val prefs = getSharedPreferences("TaskManagerPrefs", Context.MODE_PRIVATE)
        val isFirst = prefs.getBoolean("key_is_first_run", true)
        if (isFirst) {
            prefs.edit().putBoolean("key_is_first_run", false).apply()
        }
        return isFirst
    }
}