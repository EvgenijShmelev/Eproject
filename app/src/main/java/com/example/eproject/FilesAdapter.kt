package com.example.eproject

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import java.text.SimpleDateFormat
import java.util.*

interface FileActionListener {
    fun onFileClick(file: GroupFile)
    fun onFileDelete(file: GroupFile)
}

class FilesAdapter(
    private val listener: FileActionListener,
    private val canDelete: Boolean
) : RecyclerView.Adapter<FilesAdapter.FileViewHolder>() {

    private var files: List<GroupFile> = emptyList()

    fun submitList(newFiles: List<GroupFile>) {
        files = newFiles ?: emptyList()
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FileViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_file, parent, false)
        return FileViewHolder(view)
    }

    override fun onBindViewHolder(holder: FileViewHolder, position: Int) {
        holder.bind(files[position])
    }

    override fun getItemCount() = files.size

    inner class FileViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val fileName: TextView = itemView.findViewById(R.id.file_name)
        private val fileInfo: TextView = itemView.findViewById(R.id.file_info)
        private val btnDelete: ImageButton = itemView.findViewById(R.id.btn_delete_file)

        fun bind(file: GroupFile) {
            fileName.text = file.fileName

            val dateFormat = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault())
            val date = Date(file.uploadTime)
            fileInfo.text = "Загружено: ${dateFormat.format(date)}"

            btnDelete.visibility = if (canDelete) View.VISIBLE else View.GONE
            btnDelete.setOnClickListener { listener.onFileDelete(file) }

            itemView.setOnClickListener { listener.onFileClick(file) }
        }
    }
}