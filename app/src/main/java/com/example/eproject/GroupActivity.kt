package com.example.eproject

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.view.View
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class GroupActivity : AppCompatActivity() {
    private lateinit var dbHelper: DbHelper
    private lateinit var groupId: String
    private lateinit var currentUserId: String
    private lateinit var adapter: MessageAdapter
    private lateinit var userRole: UserRole

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_group)

        dbHelper = DbHelper(this)
        groupId = intent.getStringExtra("GROUP_ID") ?: run {
            showToast("Ошибка: не указан ID группы")
            finish()
            return
        }
        currentUserId = intent.getStringExtra("USER_ID") ?: run {
            showToast("Ошибка авторизации")
            finish()
            return
        }

        val groupName = intent.getStringExtra("GROUP_NAME") ?: "Группа"
        findViewById<TextView>(R.id.group_name).text = groupName
        try {
            setupViews()
            loadMessages()
        } catch (e: Exception) {
            showToast("Ошибка при загрузке группы: ${e.localizedMessage}")
            finish()
        }
    }

    private fun setupRecyclerView() {
        val recyclerView = findViewById<RecyclerView>(R.id.messages_recycler)
        recyclerView.layoutManager = LinearLayoutManager(this).apply {
            stackFromEnd = true
        }
        adapter = MessageAdapter(mutableListOf())
        recyclerView.adapter = adapter
    }

    private fun setupViews() {
        // Получаем роль пользователя в группе
        userRole = getUserRoleInGroup()

        val group = dbHelper.getGroupsForUser(currentUserId).firstOrNull { it.id == groupId }

        if (group == null) {
            showToast("Группа не найдена")
            finish()
            return
        }

        findViewById<TextView>(R.id.group_name).text = group.name

        val recyclerView: RecyclerView = findViewById(R.id.messages_recycler)
        recyclerView.layoutManager = LinearLayoutManager(this)
        adapter = MessageAdapter(mutableListOf())
        recyclerView.adapter = adapter

        val sendButton: ImageButton = findViewById(R.id.send_button)
        val messageInput: EditText = findViewById(R.id.message_input)
        val filesButton: ImageButton = findViewById(R.id.btn_files)

        // Настройка кнопки для работы с файлами (исправленная версия)
        filesButton.setOnClickListener {
            startActivity(Intent(this, GroupFilesActivity::class.java).apply {
                putExtra("GROUP_ID", groupId)
                putExtra("USER_ID", currentUserId)
            }) // Добавлена закрывающая скобка для apply
        }

        // Если пользователь не участник группы, скрываем поле ввода и кнопку файлов
        if (userRole == UserRole.MEMBER || userRole == UserRole.HEAD) {
            sendButton.setOnClickListener {
                val messageText = messageInput.text.toString()
                if (messageText.isNotEmpty()) {
                    try {
                        val message = GroupMessage(
                            id = System.currentTimeMillis().toString(),
                            groupId = groupId,
                            senderId = currentUserId,
                            text = messageText,
                            timestamp = System.currentTimeMillis()
                        )

                        if (dbHelper.addMessage(message)) {
                            messageInput.text.clear()
                            loadMessages()
                        } else {
                            showToast("Ошибка при отправке сообщения")
                        }
                    } catch (e: Exception) {
                        showToast("Ошибка: ${e.message}")
                    }
                }
            }
        } else {
            messageInput.visibility = View.GONE
            sendButton.visibility = View.GONE
            filesButton.visibility = View.GONE
        }
    }


    private fun getUserRoleInGroup(): UserRole {
        return try {
            val role = dbHelper.getUserRoleInGroup(currentUserId, groupId)
            role ?: UserRole.MEMBER // По умолчанию считаем обычным участником
        } catch (e: Exception) {
            showToast("Ошибка при получении роли пользователя")
            UserRole.MEMBER
        }
    }

    private fun loadMessages() {
        try {
            val messages = dbHelper.getMessagesForGroup(groupId)
            adapter.updateMessages(messages)
            val recyclerView: RecyclerView = findViewById(R.id.messages_recycler)
            recyclerView.scrollToPosition(adapter.itemCount - 1)
        } catch (e: Exception) {
            showToast("Ошибка при загрузке сообщений")
        }
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
    }
}