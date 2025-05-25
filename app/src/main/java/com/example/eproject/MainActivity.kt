package com.example.eproject

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {
    private lateinit var dbHelper: DbHelper

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        dbHelper = DbHelper(this)

        val userLogin: EditText = findViewById(R.id.user_login)
        val userEmail: EditText = findViewById(R.id.user_email)
        val userPass: EditText = findViewById(R.id.user_pass)
        val button: Button = findViewById(R.id.button_reg)
        val linkToAuth: TextView = findViewById(R.id.link_to_auth)

        linkToAuth.setOnClickListener {
            startActivity(Intent(this, AuthActivity::class.java))
            finish()
        }

        button.setOnClickListener {
            val login = userLogin.text.toString().trim()
            val email = userEmail.text.toString().trim()
            val pass = userPass.text.toString().trim()

            when {
                login.isEmpty() || email.isEmpty() || pass.isEmpty() -> {
                    showToast("Не все поля заполнены")
                }
                else -> {
                    val user = User(login, email, pass)
                    if (dbHelper.addUser(user)) {
                        showToast("Пользователь $login зарегистрирован")
                        userLogin.text.clear()
                        userEmail.text.clear()
                        userPass.text.clear()
                        startActivity(Intent(this, AuthActivity::class.java))
                        finish()
                    } else {
                        showToast("Ошибка регистрации")
                    }
                }
            }
        }
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
    }

    override fun onDestroy() {
        dbHelper.close()
        super.onDestroy()
    }
}