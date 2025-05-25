package com.example.eproject

import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import com.google.android.material.navigation.NavigationView

class GeneralMenu : AppCompatActivity() {
    private val userGroups = mutableListOf("Моя группа 1", "Рабочая группа", "Друзья")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_general)

        setSupportActionBar(findViewById(R.id.toolbar))
        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            setHomeAsUpIndicator(android.R.drawable.ic_menu_more)
        }

        setupNavigationDrawer()
        setupButtonListener()
    }

    private fun setupButtonListener() {
        findViewById<android.widget.Button>(R.id.btn_open_groups).setOnClickListener {
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

    private fun setupNavigationDrawer() {
        val navView = findViewById<NavigationView>(R.id.nav_view)
        updateNavigationMenu(navView.menu)

        navView.setNavigationItemSelectedListener { menuItem ->
            if (menuItem.itemId >= 0 && menuItem.itemId < userGroups.size) {
                val selectedGroup = userGroups[menuItem.itemId]
                Toast.makeText(this, "Выбрана: $selectedGroup", Toast.LENGTH_SHORT).show()
            }
            findViewById<DrawerLayout>(R.id.drawer_layout).closeDrawer(GravityCompat.START)
            true
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.main_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.add_group -> {
                showGroupDialog()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun showGroupDialog() {
        GroupManageDialog().apply {
            setGroupActionListener(object : GroupManageDialog.GroupActionListener {
                override fun onGroupAdded(groupName: String) {
                    addNewGroup(groupName)
                    Toast.makeText(this@GeneralMenu, "Добавлено: $groupName", Toast.LENGTH_SHORT).show()
                }
                override fun onGroupSelected(groupName: String) {
                    Toast.makeText(this@GeneralMenu, "Выбрано: $groupName", Toast.LENGTH_SHORT).show()
                }
            })
        }.show(supportFragmentManager, "GroupManageDialog")
    }

    private fun addNewGroup(groupName: String) {
        if (groupName.isNotBlank() && !userGroups.contains(groupName)) {
            userGroups.add(groupName)
            val navView = findViewById<NavigationView>(R.id.nav_view)
            updateNavigationMenu(navView.menu)
        }
    }

    private fun updateNavigationMenu(menu: Menu) {
        menu.clear()
        userGroups.forEachIndexed { index, groupName ->
            menu.add(Menu.NONE, index, Menu.NONE, groupName).apply {
                icon = getDrawable(android.R.drawable.ic_menu_info_details)
            }
        }
    }
}