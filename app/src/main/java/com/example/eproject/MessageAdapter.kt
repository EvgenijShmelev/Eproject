package com.example.eproject

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class MessageAdapter(private val messages: MutableList<GroupMessage>) :
    RecyclerView.Adapter<MessageAdapter.MessageViewHolder>() {

    // Объявляем ViewHolder перед его использованием
    class MessageViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val senderText: TextView = itemView.findViewById(R.id.sender_text)
        private val messageText: TextView = itemView.findViewById(R.id.message_text)

        fun bind(message: GroupMessage) {
            senderText.text = message.senderId
            messageText.text = message.text
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MessageViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val view = inflater.inflate(R.layout.item_message, parent, false)
        return MessageViewHolder(view)
    }

    override fun onBindViewHolder(holder: MessageViewHolder, position: Int) {
        holder.bind(messages[position])
    }

    override fun getItemCount() = messages.size

    fun updateMessages(newMessages: List<GroupMessage>) {
        messages.clear()
        messages.addAll(newMessages)
        notifyDataSetChanged()
    }
}