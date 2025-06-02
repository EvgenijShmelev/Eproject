package com.example.eproject

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

        val btnAddGroup = findViewById<Button>(R.id.btn_add_group)
        btnAddGroup.setOnClickListener {
            showGroupCreationDialog()
        }

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
    }

    private fun setupChatButton() {
        btnOpenChat.setOnClickListener {
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

    private fun setupSearchFunctionality() {
        val searchButton: Button = findViewById(R.id.btn_search_groups) // Добавьте эту кнопку в layout
        val searchInput: EditText = findViewById(R.id.search_group_input) // Добавьте это поле в layout

        searchButton.setOnClickListener {
            val query = searchInput.text.toString().trim()
            if (query.isNotEmpty()) {
                val filteredGroups = dbHelper.searchGroups(currentUserId, query)
                updateNavigationMenuWithSearchResults(filteredGroups)
            } else {
                updateNavigationMenu(findViewById<NavigationView>(R.id.nav_view).menu)
            }
        }
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
                        updateSelectedGroupUI()
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
        selectedGroup?.let { group ->
            selectedGroupText.text = "Группа: ${group.name}"
            groupDescriptionInput.setText(group.description)

            val isCreator = group.creatorId == currentUserId
            groupDescriptionInput.isEnabled = isCreator
            groupDescriptionInput.background = if (isCreator) {
                ContextCompat.getDrawable(this, R.drawable.bg_edittext_editable)
            } else {
                null
            }

            selectedGroupText.visibility = View.VISIBLE
            groupDescriptionInput.visibility = View.VISIBLE
            btnOpenChat.visibility = View.VISIBLE
        } ?: run {
            selectedGroupText.visibility = View.GONE
            groupDescriptionInput.visibility = View.GONE
            btnOpenChat.visibility = View.GONE
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
}