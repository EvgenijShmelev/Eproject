package com.example.eproject

import android.view.Menu
import android.view.View
import androidx.appcompat.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.ImageButton
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import com.google.android.material.navigation.NavigationView

class GeneralMenu : AppCompatActivity() {
    private lateinit var dbHelper: DbHelper
    private lateinit var currentUserId: String
    private lateinit var selectedGroupText: TextView
    private var selectedGroup: Group? = null
    private lateinit var searchResultContainer: com.google.android.material.textfield.TextInputLayout
    private lateinit var searchResultText: TextView
    private lateinit var btnLeaveSearchedGroup: ImageButton
    private lateinit var btnMenu: ImageButton
    private lateinit var btnChat: ImageButton
    private lateinit var btnAddGroup: ImageButton
    private lateinit var btnSearchGroups: ImageButton
    private lateinit var searchGroupInput: EditText
    private lateinit var groupInfoText: TextView
    private lateinit var editInfoButton: ImageButton
    private var searchedGroup: Group? = null


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_general)

        dbHelper = DbHelper(this)
        // Получаем USER_LOGIN первым делом
        currentUserId = intent?.getStringExtra("USER_LOGIN") ?: run {
            Toast.makeText(this, "Ошибка авторизации", Toast.LENGTH_LONG).show()
            finish()
            return
        }

        // Инициализация UI элементов из activity_general.xml
        btnLeaveSearchedGroup = findViewById(R.id.btn_leave_searched_group)
        groupInfoText = findViewById(R.id.group_info_text)
        editInfoButton = findViewById(R.id.edit_info_button)
        selectedGroupText = findViewById(R.id.selected_group_text)
        searchResultContainer = findViewById(R.id.search_input_container)
        searchGroupInput = findViewById(R.id.search_group_input)
        btnSearchGroups = findViewById(R.id.btn_search_groups)
        btnMenu = findViewById(R.id.btn_menu)
        btnChat = findViewById(R.id.btn_chat)
        btnLeaveSearchedGroup.setImageResource(android.R.drawable.ic_menu_add)
        btnAddGroup = findViewById(R.id.btn_add_group)
        btnLeaveSearchedGroup = findViewById(R.id.btn_leave_searched_group)

        // Инициализация TextView для результатов поиска
        searchResultText = searchResultContainer.findViewById(R.id.search_result_text)

        // Инициализация кнопки настроек
        val btnSettings = findViewById<ImageButton>(R.id.btn_settings)
        btnSettings.setOnClickListener {
            showSettingsDialog()
        }

        // Инициализация кнопки редактирования
        editInfoButton.setOnClickListener {
            showEditInfoDialog()
        }

        // Настройка слушателей
        setupButtonListeners()
        setupSearchFunctionality()
        setupNavigationDrawer()
        setupGroupInfoView()

        Log.d("GeneralMenu", "Авторизован пользователь: $currentUserId")
    }

    private fun setupButtonListeners() {
        btnMenu.setOnClickListener {
            toggleDrawer()
        }

        btnChat.setOnClickListener {
            selectedGroup?.let { group ->
                try {
                    if (dbHelper.isUserInGroup(currentUserId, group.id)) {
                        val intent = Intent(this, GroupActivity::class.java).apply {
                            putExtra("GROUP_ID", group.id)
                            putExtra("USER_ID", currentUserId)
                            putExtra("GROUP_NAME", group.name)
                            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
                        }
                        startActivity(intent)
                    } else {
                        showToast("Вы не состоите в этой группе")
                    }
                } catch (e: Exception) {
                    Log.e("GroupError", "Transition error", e)
                    showToast("Ошибка открытия чата")
                }
            } ?: showToast("Сначала выберите группу")
        }

        btnAddGroup.setOnClickListener {
            showGroupCreationDialog()
        }
    }

    private fun setupGroupInfoView() {
        updateGroupInfoUI()

        editInfoButton.setOnClickListener {
            showEditInfoDialog()
        }
    }

    private fun updateGroupInfoUI() {
        selectedGroup?.let { group ->
            val description = dbHelper.getGroupDescription(group.id)
            groupInfoText.text = description.ifEmpty { "Информация группы не указана" }

            // Показываем кнопку редактирования только для главы группы
            val isHead = dbHelper.getUserRoleInGroup(currentUserId, group.id) == UserRole.HEAD
            editInfoButton.visibility = if (isHead) View.VISIBLE else View.GONE
        } ?: run {
            groupInfoText.text = "Выберите группу"
            editInfoButton.visibility = View.GONE
        }
    }

    private fun showSettingsDialog() {
        val intent = Intent(this, SettingsActivity::class.java).apply {
            putExtra("USER_LOGIN", currentUserId)
        }
        startActivity(intent)
    }

    private fun showEditInfoDialog() {
        selectedGroup?.let { group ->
            val builder = AlertDialog.Builder(this)
            builder.setTitle("Редактировать информацию группы: ${group.name}")

            val input = EditText(this).apply {
                setText(groupInfoText.text)
                hint = "Введите информацию о группе"
                setSingleLine(false)
                minLines = 3
                maxLines = 10
            }

            builder.setView(input)

            builder.setPositiveButton("Сохранить") { dialog, _ ->
                val newInfo = input.text.toString()
                if (dbHelper.saveGroupDescription(group.id, newInfo)) {
                    groupInfoText.text = newInfo
                    showToast("Информация сохранена")
                } else {
                    showToast("Ошибка сохранения")
                }
            }

            builder.setNegativeButton("Отмена", null)
            builder.show()
        } ?: showToast("Сначала выберите группу")
    }

    private fun updateSelectedGroupUI() {
        selectedGroup?.let { group ->
            selectedGroupText.text = "Группа: ${group.name}"
            selectedGroupText.visibility = View.VISIBLE
            selectedGroupText.setTextColor(ContextCompat.getColor(this, android.R.color.white))

            val description = dbHelper.getGroupDescription(group.id)
            groupInfoText.text = description.ifEmpty { "Информация не указана" }

            val isHead = dbHelper.getUserRoleInGroup(currentUserId, group.id) == UserRole.HEAD
            editInfoButton.visibility = if (isHead) View.VISIBLE else View.GONE
        } ?: run {
            selectedGroupText.text = "Группа не выбрана"
            selectedGroupText.visibility = View.VISIBLE
            groupInfoText.text = "Выберите группу"
            editInfoButton.visibility = View.GONE
        }
    }

    private fun setupSearchFunctionality() {
        btnSearchGroups.setOnClickListener {
            val query = searchGroupInput.text.toString().trim()
            if (query.isNotEmpty()) {
                searchGroupByName(query)
            } else {
                Toast.makeText(this, "Введите название группы", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun searchGroupByName(name: String) {
        val group = dbHelper.searchGroupExact(name)
        if (group != null) {
            searchedGroup = group

            // Проверяем, является ли пользователь участником группы
            val isMember = dbHelper.isUserInGroup(currentUserId, group.id)

            if (isMember) {
                // Если уже в группе - скрываем всё и показываем сообщение
                searchResultContainer.visibility = View.GONE
                btnLeaveSearchedGroup.visibility = View.GONE
                Toast.makeText(this, "Вы уже в этой группе", Toast.LENGTH_SHORT).show()
            } else {
                // Если не в группе - показываем кнопку "Добавить"
                searchResultText.text = "Найдена группа: ${group.name}"
                btnLeaveSearchedGroup.visibility = View.VISIBLE
                btnLeaveSearchedGroup.setImageResource(R.drawable.ic_add) // Установите свою иконку добавления
                btnLeaveSearchedGroup.contentDescription = "Добавить в группу"
                searchResultContainer.visibility = View.VISIBLE

                // Обработчик нажатия на кнопку добавления
                btnLeaveSearchedGroup.setOnClickListener {
                    // Добавляем пользователя в группу
                    val success = dbHelper.addGroupMember(GroupMember(
                        groupId = group.id,
                        userId = currentUserId,
                        role = UserRole.MEMBER
                    ))

                    if (success) {
                        // После добавления скрываем элементы и показываем сообщение
                        searchResultContainer.visibility = View.GONE
                        btnLeaveSearchedGroup.visibility = View.GONE
                        Toast.makeText(this, "Группа '${group.name}' добавлена", Toast.LENGTH_SHORT).show()

                        // Обновляем меню
                        updateNavigationMenu(findViewById<NavigationView>(R.id.nav_view).menu)
                    } else {
                        Toast.makeText(this, "Ошибка при добавлении в группу", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        } else {
            searchResultContainer.visibility = View.GONE
            btnLeaveSearchedGroup.visibility = View.GONE
            Toast.makeText(this, "Группа '$name' не найдена", Toast.LENGTH_SHORT).show()
        }
    }

    private fun setupNavigationDrawer() {
        val navView = findViewById<NavigationView>(R.id.nav_view)
        updateNavigationMenu(navView.menu)

        navView.setNavigationItemSelectedListener { menuItem ->
            try {
                if (menuItem.itemId >= 0) {
                    val groups = dbHelper.getGroupsForUser(currentUserId)
                    if (menuItem.itemId < groups.size) {
                        val group = groups[menuItem.itemId]
                        selectedGroup = group
                        // Обновляем TextView с названием группы
                        findViewById<TextView>(R.id.selected_group_text).text = "Группа: ${group.name}"
                        updateGroupInfoUI()
                    }
                }
            } catch (e: Exception) {
                Toast.makeText(this, "Ошибка: ${e.message}", Toast.LENGTH_SHORT).show()
            }
            findViewById<DrawerLayout>(R.id.drawer_layout).closeDrawer(GravityCompat.START)
            true
        }
    }


    private fun toggleDrawer() {
        val drawer = findViewById<DrawerLayout>(R.id.drawer_layout)
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START)
        } else {
            drawer.openDrawer(GravityCompat.START)
        }
    }

    private fun updateNavigationMenu(menu: Menu) {
        menu.clear()
        dbHelper.getGroupsForUser(currentUserId).forEachIndexed { index, group ->
            menu.add(Menu.NONE, index, Menu.NONE, group.name).apply {
                setIcon(android.R.drawable.ic_menu_info_details)
            }
        }
    }

    private fun showGroupCreationDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_create_group, null)
        val input = dialogView.findViewById<EditText>(R.id.group_name_input)

        AlertDialog.Builder(this)
            .setTitle("Создать группу")
            .setView(dialogView)
            .setPositiveButton("Создать") { _, _ ->
                val groupName = input.text.toString().trim()
                if (groupName.isNotEmpty()) {
                    createNewGroup(groupName)
                } else {
                    Toast.makeText(this, "Название группы не может быть пустым", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Отмена", null)
            .show()
    }



    private fun createNewGroup(groupName: String) {
        if (currentUserId.isBlank()) {
            Toast.makeText(this, "Ошибка: пользователь не идентифицирован", Toast.LENGTH_LONG).show()
            return
        }

        val groupId = System.currentTimeMillis().toString()
        val group = Group(
            id = groupId,
            name = groupName,
            creatorId = currentUserId,
            description = ""
        )

        val member = GroupMember(
            groupId = groupId,
            userId = currentUserId,
            role = UserRole.HEAD
        )

        val success = try {
            val groupCreated = dbHelper.createGroup(group)
            val memberAdded = dbHelper.addGroupMember(member)
            groupCreated && memberAdded
        } catch (e: Exception) {
            Log.e("GROUP_ERROR", "Критическая ошибка при создании группы", e)
            false
        }

        if (success) {
            updateNavigationMenu(findViewById<NavigationView>(R.id.nav_view).menu)
            Toast.makeText(this, "Группа '$groupName' создана", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(this, "Ошибка при создании группы. Проверьте логи", Toast.LENGTH_LONG).show()
        }
    }

    private fun leaveGroup(group: Group) {
        try {
            val success = dbHelper.removeGroupMember(group.id, currentUserId)
            if (success) {
                updateNavigationMenu(findViewById<NavigationView>(R.id.nav_view).menu)
                searchResultContainer.visibility = View.GONE
                Toast.makeText(this, "Вы вышли из группы '${group.name}'", Toast.LENGTH_SHORT).show()
                if (selectedGroup?.id == group.id) {
                    selectedGroup = null
                    updateSelectedGroupUI()
                }
            } else {
                Toast.makeText(this, "Ошибка при выходе из группы", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            Toast.makeText(this, "Ошибка: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    override fun onDestroy() {
        dbHelper.close()
        super.onDestroy()
    }
}