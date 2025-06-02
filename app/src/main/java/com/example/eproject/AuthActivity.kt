package com.example.eproject

import android.widget.TextView
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class AuthActivity : AppCompatActivity() {

    private lateinit var dbHelper: DbHelper

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_auth)

        // Инициализация базы данных
        dbHelper = DbHelper(this)

        val loginInput = findViewById<EditText>(R.id.user_login_auth)
        val passInput = findViewById<EditText>(R.id.user_pass_auth)
        val authButton = findViewById<Button>(R.id.button_to_auth)
        val regLink = findViewById<TextView>(R.id.link_to_reg)

        authButton.setOnClickListener {
            val login = loginInput.text.toString().trim()
            val pass = passInput.text.toString().trim()

            if (login.isEmpty() || pass.isEmpty()) {
                showToast("Заполните все поля")
                return@setOnClickListener
            }

            try {
                val user = dbHelper.getUser(login, pass)
                if (user != null) {
                    // Успешная авторизация
                    startActivity(Intent(this, GeneralMenu::class.java).apply {
                        putExtra("USER_LOGIN", user.login)
                    })
                    finish()
                } else {
                    showToast("Неверные данные")
                }
            } catch (e: Exception) {
                showToast("Ошибка базы данных: ${e.message}")
                e.printStackTrace()
            }
        }

        regLink.setOnClickListener {
            startActivity(Intent(this, MainActivity::class.java))
            finish()
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