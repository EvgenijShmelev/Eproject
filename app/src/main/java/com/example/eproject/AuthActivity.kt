package com.example.eproject

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class AuthActivity : AppCompatActivity() {
    private lateinit var dbHelper: DbHelper

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_auth)
        dbHelper = DbHelper(this)

        val userLogin: EditText = findViewById(R.id.user_login_auth)
        val userPass: EditText = findViewById(R.id.user_pass_auth)
        val button: Button = findViewById(R.id.button_to_auth)
        val linkToReg: TextView = findViewById(R.id.link_to_reg)

        linkToReg.setOnClickListener {
            startActivity(Intent(this, MainActivity::class.java))
            finish()
        }

        button.setOnClickListener {
            val login = userLogin.text.toString().trim()
            val pass = userPass.text.toString().trim()

            when {
                login.isEmpty() || pass.isEmpty() -> {
                    showToast("Не все поля заполнены")
                }
                dbHelper.getUser(login, pass) -> {
                    showToast("Добро пожаловать, $login!")
                    startActivity(Intent(this, GeneralMenu::class.java).apply {
                        putExtra("USER_LOGIN", login)
                    })
                    finish()
                }
                else -> {
                    showToast("Неверные данные")
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