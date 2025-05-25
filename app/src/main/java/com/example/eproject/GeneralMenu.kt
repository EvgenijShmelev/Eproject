package com.example.eproject

import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.drawerlayout.widget.DrawerLayout
import com.google.android.material.navigation.NavigationView

class GeneralMenu : AppCompatActivity() {
    private val userGroups = mutableListOf("Моя группа 1", "Рабочая группа", "Друзья")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_general)

        setSupportActionBar(findViewById(R.id.toolbar))
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setHomeAsUpIndicator(android.R.drawable.ic_menu_more)

        setupNavigationDrawer()
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

    private fun setupNavigationDrawer() {
        val drawerLayout = findViewById<DrawerLayout>(R.id.drawer_layout)
        val navView = findViewById<NavigationView>(R.id.nav_view)

        updateNavigationMenu(navView.menu)

        navView.setNavigationItemSelectedListener { menuItem ->
            val selectedGroup = userGroups[menuItem.itemId]
            Toast.makeText(this, "Выбрана: $selectedGroup", Toast.LENGTH_SHORT).show()
            drawerLayout.closeDrawers()
            true
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
        userGroups.add(groupName)
        updateNavigationMenu(findViewById<NavigationView>(R.id.nav_view).menu)
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