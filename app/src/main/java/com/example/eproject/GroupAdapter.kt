package com.example.eproject


import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class GroupAdapter(private val onGroupClick: (String) -> Unit) :
    RecyclerView.Adapter<GroupAdapter.GroupViewHolder>() {

    private var groups: List<String> = emptyList()

    fun submitList(newGroups: List<String>) {
        groups = newGroups ?: emptyList()
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): GroupViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_group, parent, false)
        return GroupViewHolder(view)
    }

    override fun onBindViewHolder(holder: GroupViewHolder, position: Int) {
        groups.getOrNull(position)?.let { groupName ->
            holder.bind(groupName)
        }
    }

    override fun getItemCount() = groups.size

    inner class GroupViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val groupName: TextView = itemView.findViewById(R.id.group_name)

        fun bind(name: String) {
            groupName.text = name
            itemView.setOnClickListener { onGroupClick(name) }
        }
    }
}