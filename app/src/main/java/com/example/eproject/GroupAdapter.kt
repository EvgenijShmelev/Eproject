package com.example.eproject

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class GroupAdapter(
    private val onGroupClick: (Group) -> Unit,
    private val navHeaderView: View? = null
) : RecyclerView.Adapter<GroupAdapter.GroupViewHolder>() {

    private var groups: List<Group> = emptyList()
    private var lastMessages: Map<String, GroupMessage> = emptyMap()
    private var fileCounts: Map<String, Int> = emptyMap()

    fun submitList(newGroups: List<Group>, messages: List<GroupMessage>, files: List<GroupFile>) {
        groups = newGroups
        lastMessages = messages.groupBy { it.groupId }
            .mapValues { it.value.maxByOrNull { m -> m.timestamp } }
            .filterValues { it != null }
            .mapValues { it.value!! } // Безопасно, так как мы уже отфильтровали null
        fileCounts = files.groupBy { it.groupId }.mapValues { it.value.size }
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): GroupViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_group, parent, false)
        return GroupViewHolder(view)
    }

    override fun onBindViewHolder(holder: GroupViewHolder, position: Int) {
        groups.getOrNull(position)?.let { group ->
            holder.bind(group)
        }
    }

    override fun getItemCount() = groups.size

    inner class GroupViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val groupName: TextView = itemView.findViewById(R.id.group_name)
        private val lastMessagePreview: TextView = itemView.findViewById(R.id.last_message_preview)
        private val filesCount: TextView = itemView.findViewById(R.id.files_count)

        fun bind(group: Group) {
            groupName.text = group.name

            // Показываем превью последнего сообщения или количество файлов
            lastMessages[group.id]?.let { message ->
                lastMessagePreview.text = message.text.take(20)
                lastMessagePreview.visibility = View.VISIBLE
                filesCount.visibility = View.GONE
            } ?: run {
                filesCount.text = "Файлов: ${fileCounts[group.id] ?: 0}"
                filesCount.visibility = View.VISIBLE
                lastMessagePreview.visibility = View.GONE
            }

            itemView.setOnClickListener {
                onGroupClick(group)
                updateNavHeader(group)
            }
        }

        private fun updateNavHeader(group: Group) {
            navHeaderView?.let { header ->
                header.findViewById<TextView>(R.id.textView).text = group.name
                val lastMsg = lastMessages[group.id]?.text?.take(15) ?: ""
                val filesCount = fileCounts[group.id] ?: 0
                header.findViewById<TextView>(R.id.textView2).text =
                    if (lastMsg.isNotEmpty()) "Сообщение: $lastMsg..."
                    else "Файлов: $filesCount"
            }
        }
    }
}