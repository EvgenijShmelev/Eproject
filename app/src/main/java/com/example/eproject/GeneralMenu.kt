package com.example.eproject

import android.widget.ImageButton
import android.widget.LinearLayout
import android.view.LayoutInflater
import androidx.appcompat.app.AlertDialog
import android.text.Editable
import android.text.TextWatcher
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Button
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
    private lateinit var groupDescriptionInput: EditText
    private lateinit var btnOpenChat: Button
    private var selectedGroup: Group? = null
    private lateinit var searchResultContainer: LinearLayout
    private lateinit var searchResultText: TextView
    private lateinit var btnAddSearchedGroup: Button
    private var searchedGroup: Group? = null
    private lateinit var btnLeaveSearchedGroup: ImageButton
    private lateinit var btnGroupFiles: Button



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

        // Инициализация кнопки настроек (добавлено)
        val btnSettings = findViewById<ImageButton>(R.id.btn_settings)
        btnSettings.setOnClickListener {
            // Обработка нажатия на кнопку настроек
            showSettingsDialog()
        }

        val btnAddGroup = findViewById<Button>(R.id.btn_add_group)
        btnAddGroup.setOnClickListener {
            showGroupCreationDialog()
        }

        btnGroupFiles = findViewById(R.id.btn_group_files)
        btnGroupFiles.setOnClickListener {
            selectedGroup?.let { group ->
                startActivity(Intent(this, GroupFilesActivity::class.java).apply {
                    putExtra("GROUP_ID", group.id)
                    putExtra("USER_ID", currentUserId)
                })
            } ?: showToast("Сначала выберите группу")
        }
        btnAddSearchedGroup = findViewById(R.id.btn_add_searched_group)
        btnLeaveSearchedGroup = findViewById(R.id.btn_leave_searched_group)
        btnAddSearchedGroup = findViewById(R.id.btn_add_searched_group)
        searchResultContainer = findViewById(R.id.search_result_container)
        searchResultText = findViewById(R.id.search_result_text)

        Log.d("GeneralMenu", "Авторизован пользователь: $currentUserId")

        // Инициализация DbHelper
        dbHelper = DbHelper(this)

        // Инициализация UI элементов
        selectedGroupText = findViewById(R.id.selected_group_text)
        groupDescriptionInput = findViewById(R.id.group_description_input)
        btnOpenChat = findViewById(R.id.btn_open_chat)

        setupDescriptionInputListener()
        setupSearchFunctionality()
        setupNavigationDrawer()
        setupChatButton()
        setupButtonListener()
        searchResultContainer = findViewById(R.id.search_result_container)
        searchResultText = findViewById(R.id.search_result_text)
        btnAddSearchedGroup = findViewById(R.id.btn_add_searched_group)
        btnLeaveSearchedGroup.setBackgroundResource(R.drawable.ic_exit_group)

        btnLeaveSearchedGroup.setOnClickListener {
            selectedGroup?.let { group ->
                leaveGroup(group)
                selectedGroup = null // Сбрасываем выбранную группу
                updateSelectedGroupUI() // Обновляем UI
            }
        }
    }

    // Добавьте эту функцию для обработки настроек
    private fun showSettingsDialog() {
        // Реализация диалога настроек
        AlertDialog.Builder(this)
            .setTitle("Настройки")
            .setMessage("Здесь будут настройки приложения")
            .setPositiveButton("OK") { dialog, _ -> dialog.dismiss() }
            .create()
            .show()
    }

    private fun setupChatButton() {
        findViewById<Button>(R.id.btn_open_chat).setOnClickListener {
            selectedGroup?.let { group ->
                startActivity(Intent(this, GroupActivity::class.java).apply {
                    putExtra("GROUP_ID", group.id)
                    putExtra("USER_ID", currentUserId)
                })
            } ?: showToast("Сначала выберите группу")
        }
    }
    private fun setupDescriptionInputListener() {
        groupDescriptionInput.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                selectedGroup?.let { group ->
                    if (group.creatorId == currentUserId) {
                        val newDescription = s.toString()
                        dbHelper.saveGroupDescription(group.id, newDescription)
                    }
                }
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })
    }


    private fun updateNavigationMenuWithSearchResults(groups: List<Group>) {
        val navView = findViewById<NavigationView>(R.id.nav_view)
        val menu = navView.menu
        menu.clear()

        groups.forEachIndexed { index, group ->
            menu.add(Menu.NONE, index, Menu.NONE, group.name).apply {
                setIcon(android.R.drawable.ic_menu_info_details)
            }
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
                        val groupId = groups[menuItem.itemId].id
                        selectedGroup = dbHelper.getGroupById(currentUserId, groupId)
                        updateSelectedGroupUI() // Это обновит видимость кнопки
                    }
                }
            } catch (e: Exception) {
                Toast.makeText(this, "Ошибка: ${e.message}", Toast.LENGTH_SHORT).show()
            }
            findViewById<DrawerLayout>(R.id.drawer_layout).closeDrawer(GravityCompat.START)
            true
        }
    }



    private fun updateSelectedGroupUI() {
        val selectedGroupContainer = findViewById<LinearLayout>(R.id.selected_group_container)
        Log.d("UI_DEBUG", "Обновление UI. Группа: ${selectedGroup?.name ?: "null"}, isMember: ${selectedGroup?.let { dbHelper.isUserInGroup(currentUserId, it.id) } ?: false}")

        selectedGroup?.let { group ->
            selectedGroupContainer.visibility = View.VISIBLE // Показываем контейнер
            selectedGroupText.text = "Группа: ${group.name}"
            groupDescriptionInput.setText(group.description)

            val isCreator = group.creatorId == currentUserId
            groupDescriptionInput.isEnabled = isCreator
            groupDescriptionInput.background = if (isCreator) {
                ContextCompat.getDrawable(this, R.drawable.bg_edittext_editable)
            } else {
                null
            }

            selectedGroup?.let { group ->
                // Показываем кнопки только для участников группы
                val isMember = dbHelper.isUserInGroup(currentUserId, group.id)
                btnGroupFiles.visibility = if (isMember) View.VISIBLE else View.GONE
                btnOpenChat.visibility = if (isMember) View.VISIBLE else View.GONE
            } ?: run {
                btnGroupFiles.visibility = View.GONE
                btnOpenChat.visibility = View.GONE
            }
            btnLeaveSearchedGroup.visibility = if (dbHelper.isUserInGroup(currentUserId, group.id)) View.VISIBLE else View.GONE

        } ?: run {
            selectedGroupContainer.visibility = View.GONE // Скрываем если группа не выбрана
            btnLeaveSearchedGroup.visibility = View.GONE
        }
    }

    private fun setupButtonListener() {
        val btnOpenGroups: Button = findViewById(R.id.btn_open_groups)
        btnOpenGroups.setOnClickListener {
            toggleDrawer()
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

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        selectedGroup?.let {
            outState.putString("SELECTED_GROUP_ID", it.id)
        }
    }

    override fun onRestoreInstanceState(savedInstanceState: Bundle) {
        super.onRestoreInstanceState(savedInstanceState)
        savedInstanceState.getString("SELECTED_GROUP_ID")?.let { groupId ->
            selectedGroup = dbHelper.getGroupById(currentUserId, groupId) // Используем оптимизированный метод
            updateSelectedGroupUI()
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
        Log.d("GROUP_DEBUG", "Начало создания группы: $groupName")
        Log.d("GROUP_DEBUG", "Текущий пользователь: $currentUserId")

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

        Log.d("GROUP_DEBUG", "Создаем группу: $group")
        Log.d("GROUP_DEBUG", "Создаем участника: $member")

        val success = try {
            val groupCreated = dbHelper.createGroup(group)
            val memberAdded = dbHelper.addGroupMember(member)

            if (!groupCreated) {
                Log.e("GROUP_ERROR", "Не удалось создать группу в БД")
            }
            if (!memberAdded) {
                Log.e("GROUP_ERROR", "Не удалось добавить участника в БД")
            }

            groupCreated && memberAdded
        } catch (e: Exception) {
            Log.e("GROUP_ERROR", "Критическая ошибка при создании группы", e)
            false
        }

        if (success) {
            Log.d("GROUP_DEBUG", "Группа успешно создана!")
            updateNavigationMenu(findViewById<NavigationView>(R.id.nav_view).menu)
            Toast.makeText(this, "Группа '$groupName' создана", Toast.LENGTH_SHORT).show()
        } else {
            Log.e("GROUP_ERROR", "Общая ошибка при создании группы")
            Toast.makeText(this, "Ошибка при создании группы. Проверьте логи", Toast.LENGTH_LONG).show()
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.main_menu, menu)
        return true
    }

    private fun isGroupExists(groupName: String): Boolean {
        return dbHelper.getGroupsForUser(currentUserId).any { it.name == groupName }
    }

    private fun createGroupIfNotExists(groupName: String) {
        if (isGroupExists(groupName)) {
            Toast.makeText(this, "Группа с таким названием уже существует", Toast.LENGTH_SHORT).show()
            return
        }
    }



    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.add_group -> {
                showGroupCreationDialog()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun showGroupDialog() {
        GroupManageDialog().apply {
            setGroupActionListener(object : GroupManageDialog.GroupActionListener {
                override fun onGroupAdded(groupName: String) {
                    try {
                        val groupId = System.currentTimeMillis().toString()
                        val group = Group(groupId, groupName, currentUserId)
                        val member = GroupMember(groupId, currentUserId, UserRole.HEAD)

                        if (dbHelper.createGroup(group) && dbHelper.addGroupMember(member)) {
                            updateNavigationMenu(findViewById<NavigationView>(R.id.nav_view).menu)
                            showToast("Группа '$groupName' создана")
                        } else {
                            showToast("Ошибка при создании группы")
                        }
                    } catch (e: Exception) {
                        showToast("Ошибка: ${e.message}")
                    }
                }

                override fun onGroupSelected(groupName: String) {
                    // Not used
                }
            })
        }.show(supportFragmentManager, "GroupManageDialog")
    }

    private fun checkDatabaseTables() {
        val cursor = dbHelper.readableDatabase.rawQuery(
            "SELECT name FROM sqlite_master WHERE type='table'", null)
        Log.d("DB_TABLES", "Доступные таблицы:")
        while (cursor.moveToNext()) {
            Log.d("DB_TABLES", cursor.getString(0))
        }
        cursor.close()
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }
    override fun onDestroy() {
        dbHelper.close()
        super.onDestroy()
    }
    private fun setupSearchFunctionality() {
        val searchInput = findViewById<EditText>(R.id.search_group_input)
        val searchButton = findViewById<Button>(R.id.btn_search_groups)

        searchButton.setOnClickListener {
            val query = searchInput.text.toString().trim()
            if (query.isNotEmpty()) {
                searchGroupByName(query)
            } else {
                Toast.makeText(this, "Введите название группы", Toast.LENGTH_SHORT).show()
            }
        }

        btnAddSearchedGroup.setOnClickListener {
            searchedGroup?.let { group ->
                addUserToGroup(group)
            } ?: run {
                Toast.makeText(this, "Группа не выбрана", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun leaveGroup(group: Group) {
        Log.d("DEBUG", "Попытка выхода из группы ${group.id}")
        try {
            val success = dbHelper.removeGroupMember(group.id, currentUserId)
            Log.d("DEBUG", "Результат выхода: $success")

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
            Log.e("DEBUG", "Ошибка при выходе из группы", e)
            Toast.makeText(this, "Ошибка: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun searchGroupByName(name: String) {
        val group = dbHelper.searchGroupExact(name)
        if (group != null) {
            searchedGroup = group
            searchResultText.text = group.name
            searchResultContainer.visibility = View.VISIBLE

            val isMember = dbHelper.isUserInGroup(currentUserId, group.id)
            Log.d("SEARCH_DEBUG", "User is member: $isMember")

            btnAddSearchedGroup.visibility = if (isMember) View.GONE else View.VISIBLE
            btnLeaveSearchedGroup.visibility = if (isMember) View.VISIBLE else View.GONE

        } else {
            searchResultContainer.visibility = View.GONE
            Toast.makeText(this, "Группа '$name' не найдена", Toast.LENGTH_SHORT).show()
        }
    }

    private fun addUserToGroup(group: Group) {
        val member = GroupMember(
            groupId = group.id,
            userId = currentUserId,
            role = UserRole.MEMBER
        )

        if (dbHelper.addGroupMember(member)) {
            updateNavigationMenu(findViewById<NavigationView>(R.id.nav_view).menu)
            searchResultContainer.visibility = View.GONE
            Toast.makeText(this, "Вы добавлены в группу '${group.name}'", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(this, "Ошибка добавления в группу", Toast.LENGTH_SHORT).show()
        }
    }
}