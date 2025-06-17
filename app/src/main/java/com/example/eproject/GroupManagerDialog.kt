package com.example.eproject

import android.app.AlertDialog
import android.app.Dialog
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.Toast
import androidx.fragment.app.DialogFragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.textfield.TextInputEditText

class GroupManageDialog : DialogFragment() {

    interface GroupActionListener {
        fun onGroupAdded(groupName: String)
        fun onGroupSelected(groupName: String)
    }

    private var listener: GroupActionListener? = null
    private lateinit var adapter: GroupAdapter
    private val groupNames = mutableListOf<String>()
    private val groups = mutableListOf<Group>()
    private val currentUserId = "current_user_id" // Замените на реальный ID пользователя

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val view = LayoutInflater.from(requireContext())
            .inflate(R.layout.dialog_group_manage, null)

        // Инициализация групп
        groupNames.addAll(listOf("Работа", "Друзья", "Семья"))
        updateGroupsList()

        adapter = GroupAdapter(
            onGroupClick = { group ->
                listener?.onGroupSelected(group.name)
                dismiss()
            },
            navHeaderView = null
        )

        view.findViewById<RecyclerView>(R.id.groups_recycler).apply {
            layoutManager = LinearLayoutManager(context)
            this.adapter = this@GroupManageDialog.adapter
        }

        view.findViewById<TextInputEditText>(R.id.search_input).addTextChangedListener(
            object : TextWatcher {
                override fun afterTextChanged(s: Editable?) {
                    filterGroups(s?.toString() ?: "")
                }
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            }
        )

        view.findViewById<Button>(R.id.add_button).setOnClickListener {
            val input = view.findViewById<TextInputEditText>(R.id.new_group_input)
            val groupName = input.text.toString().trim()

            if (groupName.isNotEmpty()) {
                addGroup(groupName)
                input.text?.clear()
            } else {
                Toast.makeText(context, "Введите название группы", Toast.LENGTH_SHORT).show()
            }
        }

        return AlertDialog.Builder(requireContext())
            .setView(view)
            .setTitle("Управление группами")
            .setNegativeButton("Закрыть") { dialog, _ -> dialog.dismiss() }
            .create()
    }

    private fun updateGroupsList() {
        groups.clear()
        groups.addAll(groupNames.map {
            Group(
                id = it.hashCode().toString(),
                name = it,
                creatorId = currentUserId // Добавлен creatorId
            )
        })
    }

    private fun filterGroups(query: String) {
        val filtered = if (query.isEmpty()) {
            groupNames
        } else {
            groupNames.filter { it.contains(query, ignoreCase = true) }
        }

        val filteredGroups = filtered.map {
            Group(
                id = it.hashCode().toString(),
                name = it,
                creatorId = currentUserId // Добавлен creatorId
            )
        }
        adapter.submitList(filteredGroups, emptyList(), emptyList())
    }

    private fun addGroup(name: String) {
        if (!groupNames.contains(name)) {
            groupNames.add(name)
            groups.add(
                Group(
                    id = name.hashCode().toString(),
                    name = name,
                    creatorId = currentUserId // Добавлен creatorId
                )
            )
            adapter.submitList(groups, emptyList(), emptyList())
            listener?.onGroupAdded(name)
        } else {
            Toast.makeText(context, "Группа уже существует", Toast.LENGTH_SHORT).show()
        }
    }

    fun setGroupActionListener(listener: GroupActionListener) {
        this.listener = listener
    }
}