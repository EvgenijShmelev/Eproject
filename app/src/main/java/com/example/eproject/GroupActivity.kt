package com.example.eproject

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class GroupActivity : AppCompatActivity() {
    private lateinit var dbHelper: DbHelper
    private lateinit var groupId: String
    private lateinit var currentUserId: String
    private lateinit var adapter: MessageAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_group)

        dbHelper = DbHelper(this)
        groupId = intent.getStringExtra("GROUP_ID") ?: run {
            finish()
            return
        }
        currentUserId = intent.getStringExtra("USER_ID") ?: run {
            finish()
            return
        }

        setupViews()
        loadMessages()
    }

    private fun setupViews() {
        val group = dbHelper.getGroupsForUser(currentUserId).firstOrNull { it.id == groupId }
        findViewById<TextView>(R.id.group_name).text = group?.name ?: "Группа"

        val recyclerView: RecyclerView = findViewById(R.id.messages_recycler)
        recyclerView.layoutManager = LinearLayoutManager(this)
        adapter = MessageAdapter(mutableListOf())
        recyclerView.adapter = adapter

        val sendButton: Button = findViewById(R.id.send_button)
        val messageInput: EditText = findViewById(R.id.message_input)

        sendButton.setOnClickListener {
            val messageText = messageInput.text.toString()
            if (messageText.isNotEmpty()) {
                val message = GroupMessage(
                    id = System.currentTimeMillis().toString(),
                    groupId = groupId,
                    senderId = currentUserId,
                    text = messageText,
                    timestamp = System.currentTimeMillis() // Добавлен timestamp
                )

                if (dbHelper.addMessage(message)) {
                    messageInput.text.clear()
                    loadMessages()
                }
            }
        }
    }

    private fun loadMessages() {
        val messages = dbHelper.getMessagesForGroup(groupId)
        adapter.updateMessages(messages)
        val recyclerView: RecyclerView = findViewById(R.id.messages_recycler)
        recyclerView.scrollToPosition(adapter.itemCount - 1)
    }
}

class MessageAdapter(private val messages: MutableList<GroupMessage>) :
    RecyclerView.Adapter<MessageAdapter.MessageViewHolder>() {

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