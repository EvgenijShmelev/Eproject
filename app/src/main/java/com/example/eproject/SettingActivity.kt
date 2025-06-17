package com.example.eproject

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class SettingsActivity : AppCompatActivity() {
    private lateinit var dbHelper: DbHelper
    private lateinit var currentLogin: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        dbHelper = DbHelper(this)
        currentLogin = intent.getStringExtra("USER_LOGIN") ?: run {
            Toast.makeText(this, "Ошибка: пользователь не идентифицирован", Toast.LENGTH_LONG).show()
            finish()
            return
        }

        val btnChangeEmail = findViewById<Button>(R.id.btn_change_email)
        val btnChangeLogin = findViewById<Button>(R.id.btn_change_login)
        val etNewEmail = findViewById<EditText>(R.id.et_new_email)
        val etNewLogin = findViewById<EditText>(R.id.et_new_login)
        val etPassword = findViewById<EditText>(R.id.et_password)

        btnChangeEmail.setOnClickListener {
            val newEmail = etNewEmail.text.toString().trim()
            if (newEmail.isEmpty()) {
                Toast.makeText(this, "Введите новый email", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (dbHelper.updateUserEmail(currentLogin, newEmail)) {
                Toast.makeText(this, "Email успешно изменен", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "Ошибка при изменении email", Toast.LENGTH_SHORT).show()
            }
        }

        btnChangeLogin.setOnClickListener {
            val newLogin = etNewLogin.text.toString().trim()
            val password = etPassword.text.toString().trim()

            if (newLogin.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Заполните все поля", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (dbHelper.updateUserLogin(currentLogin, newLogin, password)) {
                Toast.makeText(this, "Логин успешно изменен", Toast.LENGTH_SHORT).show()
                // Обновляем текущий логин
                currentLogin = newLogin
            } else {
                Toast.makeText(this, "Ошибка при изменении логина. Проверьте пароль или попробуйте другой логин", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onDestroy() {
        dbHelper.close()
        super.onDestroy()
    }
}