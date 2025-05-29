package com.example.eproject

import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import com.google.android.material.navigation.NavigationView

class GeneralMenu : AppCompatActivity() {
    private lateinit var dbHelper: DbHelper
    private lateinit var currentUserId: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_general)

        dbHelper = DbHelper(this)
        currentUserId = intent.getStringExtra("USER_LOGIN") ?: run {
            finish()
            return
        }

        setSupportActionBar(findViewById(R.id.toolbar))
        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            setHomeAsUpIndicator(android.R.drawable.ic_menu_more)
        }

        setupNavigationDrawer()
        setupButtonListener()
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

    private fun setupNavigationDrawer() {
        val navView = findViewById<NavigationView>(R.id.nav_view)
        updateNavigationMenu(navView.menu)

        navView.setNavigationItemSelectedListener { menuItem ->
            try {
                if (menuItem.itemId >= 0) {
                    val groups = dbHelper.getGroupsForUser(currentUserId)
                    if (menuItem.itemId < groups.size) {
                        val selectedGroup = groups[menuItem.itemId]
                        startActivity(Intent(this, GroupActivity::class.java).apply {
                            putExtra("GROUP_ID", selectedGroup.id)
                            putExtra("USER_ID", currentUserId)
                        })
                    } else {
                        Toast.makeText(this, "Группа не найдена", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                Toast.makeText(this, "Ошибка: ${e.message}", Toast.LENGTH_SHORT).show()
            }
            findViewById<DrawerLayout>(R.id.drawer_layout).closeDrawer(GravityCompat.START)
            true
        }
    }

    private fun updateNavigationMenu(menu: Menu) {
        menu.clear()
        dbHelper.getGroupsForUser(currentUserId).forEachIndexed { index, group ->
            menu.add(Menu.NONE, index, Menu.NONE, group.name).apply {
                icon = getDrawable(android.R.drawable.ic_menu_info_details)
            }
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
                    val groupId = System.currentTimeMillis().toString()
                    val group = Group(groupId, groupName, currentUserId)

                    if (dbHelper.createGroup(group) &&
                        dbHelper.addGroupMember(GroupMember(groupId, currentUserId, UserRole.HEAD))) {
                        updateNavigationMenu(findViewById<NavigationView>(R.id.nav_view).menu)
                        Toast.makeText(this@GeneralMenu, "Группа создана", Toast.LENGTH_SHORT).show()
                    }
                }

                override fun onGroupSelected(groupName: String) {
                    // Not used in this implementation
                }
            })
        }.show(supportFragmentManager, "GroupManageDialog")
    }
}