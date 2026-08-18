package com.example.smarttaskmanager

import android.graphics.Paint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class TaskAdapter(
    private var tasks: MutableList<Task>,
    private val onTaskStatusChanged: (Task) -> Unit,
    private val onDeleteClicked: (Task) -> Unit
) : RecyclerView.Adapter<TaskAdapter.TaskViewHolder>() {

    class TaskViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val cbCompleted: CheckBox = itemView.findViewById(R.id.cbCompleted)
        val tvTitle: TextView = itemView.findViewById(R.id.tvTitle)
        val tvCategory: TextView = itemView.findViewById(R.id.tvCategory)
        val btnDelete: ImageButton = itemView.findViewById(R.id.btnDelete)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TaskViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_task, parent, false)
        return TaskViewHolder(view)
    }

    override fun onBindViewHolder(holder: TaskViewHolder, position: Int) {
        val task = tasks[position]

        holder.tvTitle.text = task.title
        holder.tvCategory.text = task.category

        holder.cbCompleted.setOnCheckedChangeListener(null)
        holder.cbCompleted.isChecked = task.isCompleted

        if (task.isCompleted) {
            holder.tvTitle.paintFlags = holder.tvTitle.paintFlags or Paint.STRIKE_THRU_TEXT_FLAG
        } else {
            holder.tvTitle.paintFlags = holder.tvTitle.paintFlags and Paint.STRIKE_THRU_TEXT_FLAG.inv()
        }

        holder.cbCompleted.setOnCheckedChangeListener { _, isChecked ->
            task.isCompleted = isChecked
            onTaskStatusChanged(task)
            notifyItemChanged(position)
        }

        holder.btnDelete.setOnClickListener {
            onDeleteClicked(task)
        }
    }

    override fun getItemCount(): Int = tasks.size

    fun updateList(newList: List<Task>) {
        tasks = newList.toMutableList()
        notifyDataSetChanged()
    }
}