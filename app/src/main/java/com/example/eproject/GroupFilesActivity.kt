package com.example.eproject

import android.app.AlertDialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.provider.OpenableColumns
import android.view.View
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import java.io.File
import java.io.FileOutputStream
import java.util.*

class GroupFilesActivity : AppCompatActivity(), FileActionListener {

    private lateinit var dbHelper: DbHelper
    private lateinit var groupId: String
    private lateinit var currentUserId: String
    private lateinit var userRole: UserRole
    private lateinit var adapter: FilesAdapter
    private lateinit var btnAddFile: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_group_files)

        dbHelper = DbHelper(this)
        groupId = intent.getStringExtra("GROUP_ID") ?: run {
            showToast("Ошибка: не указан ID группы")
            finish()
            return
        }
        currentUserId = intent.getStringExtra("USER_ID") ?: run {
            showToast("Ошибка: не указан ID пользователя")
            finish()
            return
        }

        userRole = dbHelper.getUserRoleInGroup(currentUserId, groupId) ?: UserRole.MEMBER

        setupViews()
        loadFiles()
    }

    private fun setupViews() {
        btnAddFile = findViewById(R.id.btn_add_file)
        btnAddFile.setOnClickListener {
            val intent = Intent(Intent.ACTION_GET_CONTENT).apply {
                type = "*/*"
                addCategory(Intent.CATEGORY_OPENABLE)
            }
            startActivityForResult(intent, FILE_PICK_REQUEST)
        }

        // Скрываем кнопку добавления, если пользователь не участник группы
        if (userRole == UserRole.GUEST) {
            btnAddFile.visibility = View.GONE
        }

        val recyclerView: RecyclerView = findViewById(R.id.files_recycler)
        recyclerView.layoutManager = LinearLayoutManager(this)
        adapter = FilesAdapter(this, userRole == UserRole.HEAD)
        recyclerView.adapter = adapter
    }

    private fun loadFiles() {
        val files = dbHelper.getGroupFiles(groupId)
        adapter.submitList(files)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == FILE_PICK_REQUEST && resultCode == RESULT_OK) {
            data?.data?.let { uri ->
                uploadFile(uri)
            }
        }
    }

    private fun uploadFile(uri: Uri) {
        try {
            val cursor = contentResolver.query(uri, null, null, null, null)
            cursor?.use {
                if (it.moveToFirst()) {
                    val displayName = it.getString(it.getColumnIndexOrThrow(OpenableColumns.DISPLAY_NAME))

                    val filesDir = File(getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS), "group_files")
                    if (!filesDir.exists()) {
                        filesDir.mkdirs()
                    }

                    val file = File(filesDir, "${UUID.randomUUID()}_$displayName")
                    contentResolver.openInputStream(uri)?.use { inputStream ->
                        FileOutputStream(file).use { outputStream ->
                            inputStream.copyTo(outputStream)
                        }
                    }

                    val groupFile = GroupFile(
                        id = UUID.randomUUID().toString(),
                        groupId = groupId,
                        fileName = displayName,
                        filePath = file.absolutePath,
                        uploaderId = currentUserId,
                        uploadTime = System.currentTimeMillis()
                    )

                    if (dbHelper.addFile(groupFile)) {
                        showToast("Файл загружен")
                        loadFiles()
                    } else {
                        showToast("Ошибка при сохранении файла")
                        file.delete()
                    }
                }
            }
        } catch (e: Exception) {
            showToast("Ошибка при загрузке файла: ${e.message}")
            e.printStackTrace()
        }
    }

    override fun onFileClick(file: GroupFile) {
        val fileUri = FileProvider.getUriForFile(
            this,
            "${packageName}.fileprovider",
            File(file.filePath)
        )

        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(fileUri, getMimeType(file.fileName))
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        try {
            startActivity(intent)
        } catch (e: Exception) {
            showToast("Не удалось открыть файл")
        }
    }

    override fun onFileDelete(file: GroupFile) {
        AlertDialog.Builder(this)
            .setTitle("Удаление файла")
            .setMessage("Вы уверены, что хотите удалить файл ${file.fileName}?")
            .setPositiveButton("Удалить") { _, _ ->
                if (File(file.filePath).delete() && dbHelper.deleteFile(file.id)) {
                    showToast("Файл удален")
                    loadFiles()
                } else {
                    showToast("Ошибка при удалении файла")
                }
            }
            .setNegativeButton("Отмена", null)
            .show()
    }

    private fun getMimeType(fileName: String): String {
        return when {
            fileName.endsWith(".doc") || fileName.endsWith(".docx") -> "application/msword"
            fileName.endsWith(".pdf") -> "application/pdf"
            fileName.endsWith(".txt") -> "text/plain"
            else -> "*/*"
        }
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    companion object {
        private const val FILE_PICK_REQUEST = 1001
    }
}