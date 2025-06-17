package com.example.eproject


import com.example.eproject.UserRole
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.ktx.toObject
import java.util.concurrent.atomic.AtomicBoolean
import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.util.Log
import com.google.firebase.firestore.FieldValue
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await



class DbHelper(context: Context) : SQLiteOpenHelper(
    context,
    DATABASE_NAME,
    null,
    DATABASE_VERSION
) {
    private val firestore = FirebaseFirestore.getInstance()
    private val storage = FirebaseStorage.getInstance()
    private val syncInProgress = AtomicBoolean(false)

    companion object {
        private const val FIRESTORE_USERS = "users"
        private const val FIRESTORE_GROUPS = "groups"
        private const val FIRESTORE_MEMBERS = "members"
        private const val FIRESTORE_MESSAGES = "messages"
        private const val FIRESTORE_FILES_METADATA = "files_metadata"

        private const val TAG = "DbHelper"
        private const val DATABASE_NAME = "app_db.db"
        private const val DATABASE_VERSION = 5

        // Таблицы и колонки
        private const val TABLE_USERS = "users"
        private const val TABLE_GROUPS = "groups"
        private const val TABLE_GROUP_MEMBERS = "group_members"
        private const val TABLE_GROUP_MESSAGES = "group_messages"
        private const val TABLE_FILES = "group_files"

        private const val COLUMN_ID = "id"
        private const val COLUMN_LOGIN = "login"
        private const val COLUMN_EMAIL = "email"
        private const val COLUMN_PASSWORD = "password"
        private const val COLUMN_NAME = "name"
        private const val COLUMN_CREATOR_ID = "creator_id"
        private const val COLUMN_GROUP_ID = "group_id"
        private const val COLUMN_USER_ID = "user_id"
        private const val COLUMN_ROLE = "role"
        private const val COLUMN_SENDER_ID = "sender_id"
        private const val COLUMN_TEXT = "text"
        private const val COLUMN_TIMESTAMP = "timestamp"
        private const val COLUMN_DESCRIPTION = "description"
        private const val COLUMN_FILE_ID = "file_id"
        private const val COLUMN_FILE_NAME = "file_name"
        private const val COLUMN_FILE_PATH = "file_path"
        private const val COLUMN_UPLOADER_ID = "uploader_id"
        private const val COLUMN_UPLOAD_TIME = "upload_time"
    }

    override fun onCreate(db: SQLiteDatabase) {
        db.beginTransaction()
        try {
            db.execSQL(
                """
                CREATE TABLE $TABLE_USERS (
                    $COLUMN_LOGIN TEXT PRIMARY KEY,
                    $COLUMN_EMAIL TEXT NOT NULL,
                    $COLUMN_PASSWORD TEXT NOT NULL
                )
                """
            )

            db.execSQL(
                """
                CREATE TABLE $TABLE_GROUPS (
                    $COLUMN_ID TEXT PRIMARY KEY,
                    $COLUMN_NAME TEXT NOT NULL,
                    $COLUMN_CREATOR_ID TEXT NOT NULL,
                    $COLUMN_DESCRIPTION TEXT DEFAULT ''
                )
                """
            )

            db.execSQL(
                """
                CREATE TABLE $TABLE_GROUP_MEMBERS (
                    $COLUMN_GROUP_ID TEXT NOT NULL,
                    $COLUMN_USER_ID TEXT NOT NULL,
                    $COLUMN_ROLE TEXT NOT NULL,
                    PRIMARY KEY ($COLUMN_GROUP_ID, $COLUMN_USER_ID),
                    FOREIGN KEY ($COLUMN_GROUP_ID) REFERENCES $TABLE_GROUPS($COLUMN_ID)
                )
                """
            )

            db.execSQL(
                """
                CREATE TABLE $TABLE_GROUP_MESSAGES (
                    $COLUMN_ID TEXT PRIMARY KEY,
                    $COLUMN_GROUP_ID TEXT NOT NULL,
                    $COLUMN_SENDER_ID TEXT NOT NULL,
                    $COLUMN_TEXT TEXT NOT NULL,
                    $COLUMN_TIMESTAMP INTEGER NOT NULL,
                    FOREIGN KEY ($COLUMN_GROUP_ID) REFERENCES $TABLE_GROUPS($COLUMN_ID)
                )
                """
            )

            db.execSQL(
                """
                CREATE TABLE $TABLE_FILES (
                    $COLUMN_FILE_ID TEXT PRIMARY KEY,
                    $COLUMN_GROUP_ID TEXT NOT NULL,
                    $COLUMN_FILE_NAME TEXT NOT NULL,
                    $COLUMN_FILE_PATH TEXT NOT NULL,
                    $COLUMN_UPLOADER_ID TEXT NOT NULL,
                    $COLUMN_UPLOAD_TIME INTEGER NOT NULL,
                    FOREIGN KEY ($COLUMN_GROUP_ID) REFERENCES $TABLE_GROUPS($COLUMN_ID)
                )
                """
            )

            // Создаем индексы
            db.execSQL("CREATE INDEX IF NOT EXISTS idx_group_id ON $TABLE_GROUPS($COLUMN_ID)")
            db.execSQL("CREATE INDEX IF NOT EXISTS idx_group_description ON $TABLE_GROUPS($COLUMN_DESCRIPTION)")

            db.setTransactionSuccessful()
        } catch (e: Exception) {
            Log.e(TAG, "Error creating database tables", e)
            forceDbUpgrade(db)
        } finally {
            db.endTransaction()
        }
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        if (oldVersion < 5) {
            db.execSQL("DROP TABLE IF EXISTS $TABLE_FILES")
            db.execSQL(
                """
                CREATE TABLE $TABLE_FILES (
                    $COLUMN_FILE_ID TEXT PRIMARY KEY,
                    $COLUMN_GROUP_ID TEXT NOT NULL,
                    $COLUMN_FILE_NAME TEXT NOT NULL,
                    $COLUMN_FILE_PATH TEXT NOT NULL,
                    $COLUMN_UPLOADER_ID TEXT NOT NULL,
                    $COLUMN_UPLOAD_TIME INTEGER NOT NULL,
                    FOREIGN KEY ($COLUMN_GROUP_ID) REFERENCES $TABLE_GROUPS($COLUMN_ID)
                )
                """
            )
        }
    }

    private fun forceDbUpgrade(db: SQLiteDatabase) {
        try {
            db.execSQL("DROP TABLE IF EXISTS $TABLE_GROUP_MESSAGES")
            db.execSQL("DROP TABLE IF EXISTS $TABLE_GROUP_MEMBERS")
            db.execSQL("DROP TABLE IF EXISTS $TABLE_GROUPS")
            db.execSQL("DROP TABLE IF EXISTS $TABLE_USERS")
            db.execSQL("DROP TABLE IF EXISTS $TABLE_FILES")
            onCreate(db)
            Log.w(TAG, "Database was recreated due to migration failure")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to recreate database", e)
        }
    }

    // Методы для работы с пользователями
    fun addUser(user: User): Boolean {
        val db = writableDatabase
        db.beginTransaction()
        return try {
            val values = ContentValues().apply {
                put(COLUMN_LOGIN, user.login)
                put(COLUMN_EMAIL, user.email)
                put(COLUMN_PASSWORD, user.password)
            }

            if (isUserExists(user.login)) {
                Log.w(TAG, "User with login ${user.login} already exists")
                false
            } else {
                val result = db.insert(TABLE_USERS, null, values) != -1L
                db.setTransactionSuccessful()
                result
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error adding user", e)
            false
        } finally {
            db.endTransaction()
        }
    }

    fun getUser(login: String, password: String): User? {
        return readableDatabase.rawQuery(
            "SELECT * FROM $TABLE_USERS WHERE $COLUMN_LOGIN = ? AND $COLUMN_PASSWORD = ?",
            arrayOf(login, password)
        ).use { cursor ->
            if (cursor.moveToFirst()) {
                User(
                    login = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_LOGIN)),
                    email = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_EMAIL)),
                    password = "" // Не возвращаем пароль
                )
            } else {
                null
            }
        }
    }

    private fun isUserExists(login: String): Boolean {
        return readableDatabase.rawQuery(
            "SELECT 1 FROM $TABLE_USERS WHERE $COLUMN_LOGIN = ?",
            arrayOf(login)
        ).use { cursor ->
            cursor.count > 0
        }
    }

    // Методы для работы с группами
    fun createGroup(group: Group): Boolean {
        val db = writableDatabase
        db.beginTransaction()
        try {
            val values = ContentValues().apply {
                put(COLUMN_ID, group.id)
                put(COLUMN_NAME, group.name)
                put(COLUMN_CREATOR_ID, group.creatorId)
                put(COLUMN_DESCRIPTION, group.description)
            }

            val result = db.insert(TABLE_GROUPS, null, values)
            if (result == -1L) {
                Log.e("DB_ERROR", "Ошибка при вставке группы в таблицу $TABLE_GROUPS")
                return false
            }

            db.setTransactionSuccessful()
            return true
        } catch (e: Exception) {
            Log.e("DB_ERROR", "Исключение при создании группы", e)
            return false
        } finally {
            db.endTransaction()
        }
    }

    fun getGroupById(userId: String, groupId: String): Group? {
        return readableDatabase.rawQuery(
            """
            SELECT g.* FROM $TABLE_GROUPS g
            JOIN $TABLE_GROUP_MEMBERS m ON g.$COLUMN_ID = m.$COLUMN_GROUP_ID
            WHERE m.$COLUMN_USER_ID = ? AND g.$COLUMN_ID = ?
            """, arrayOf(userId, groupId)
        ).use { cursor ->
            if (cursor.moveToFirst()) {
                Group(
                    id = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_ID)),
                    name = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_NAME)),
                    creatorId = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_CREATOR_ID)),
                    description = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_DESCRIPTION)) ?: ""
                )
            } else {
                null
            }
        }
    }

    fun searchGroups(userId: String, query: String): List<Group> {
        return readableDatabase.rawQuery(
            """
            SELECT g.* FROM $TABLE_GROUPS g
            JOIN $TABLE_GROUP_MEMBERS m ON g.$COLUMN_ID = m.$COLUMN_GROUP_ID
            WHERE m.$COLUMN_USER_ID = ? AND g.$COLUMN_NAME LIKE ?
            """, arrayOf(userId, "%$query%")
        ).use { cursor ->
            val groups = mutableListOf<Group>()
            while (cursor.moveToNext()) {
                groups.add(
                    Group(
                        id = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_ID)),
                        name = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_NAME)),
                        creatorId = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_CREATOR_ID)),
                        description = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_DESCRIPTION)) ?: ""
                    )
                )
            }
            groups
        }
    }

    fun searchGroupExact(name: String): Group? {
        return readableDatabase.rawQuery(
            "SELECT * FROM $TABLE_GROUPS WHERE $COLUMN_NAME = ?",
            arrayOf(name)
        ).use { cursor ->
            if (cursor.moveToFirst()) {
                Group(
                    id = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_ID)),
                    name = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_NAME)),
                    creatorId = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_CREATOR_ID)),
                    description = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_DESCRIPTION)) ?: ""
                )
            } else {
                null
            }
        }
    }

    fun getGroupsForUser (userId: String): List<Group> {
        return readableDatabase.rawQuery(
            """
        SELECT g.* FROM $TABLE_GROUPS g
        JOIN $TABLE_GROUP_MEMBERS m ON g.$COLUMN_ID = m.$COLUMN_GROUP_ID
        WHERE m.$COLUMN_USER_ID = ?
        """, arrayOf(userId)
        ).use { cursor -> // Исправлено: use теперь применяется к cursor
            val groups = mutableListOf<Group>()
            while (cursor.moveToNext()) {
                groups.add(
                    Group(
                        id = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_ID)),
                        name = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_NAME)),
                        creatorId = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_CREATOR_ID)),
                        description = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_DESCRIPTION)) ?: ""
                    )
                )
            }
            groups
        }
    }


    fun saveGroupDescription(groupId: String, description: String): Boolean {
        val db = writableDatabase
        db.beginTransaction()
        return try {
            val values = ContentValues().apply {
                put(COLUMN_DESCRIPTION, description)
            }
            val result = db.update(
                TABLE_GROUPS,
                values,
                "$COLUMN_ID = ?",
                arrayOf(groupId)
            ) > 0
            db.setTransactionSuccessful()
            result
        } catch (e: Exception) {
            Log.e(TAG, "Error saving group description", e)
            false
        } finally {
            db.endTransaction()
        }
    }

    fun getGroupDescription(groupId: String): String {
        return readableDatabase.rawQuery(
            "SELECT $COLUMN_DESCRIPTION FROM $TABLE_GROUPS WHERE $COLUMN_ID = ?",
            arrayOf(groupId)
        ).use { cursor ->
            if (cursor.moveToFirst()) cursor.getString(0) ?: "" else ""
        }
    }

    // Методы для работы с участниками групп
    fun addGroupMember(member: GroupMember): Boolean {
        val db = writableDatabase
        db.beginTransaction()
        try {
            val values = ContentValues().apply {
                put(COLUMN_GROUP_ID, member.groupId)
                put(COLUMN_USER_ID, member.userId)
                put(COLUMN_ROLE, member.role.toString())
            }

            val result = db.insert(TABLE_GROUP_MEMBERS, null, values)
            if (result == -1L) {
                Log.e("DB_ERROR", "Ошибка при вставке участника в таблицу $TABLE_GROUP_MEMBERS")
                return false
            }

            db.setTransactionSuccessful()
            return true
        } catch (e: Exception) {
            Log.e("DB_ERROR", "Исключение при добавлении участника", e)
            return false
        } finally {
            db.endTransaction()
        }
    }

    fun isUserInGroup(userId: String, groupId: String): Boolean {
        val db = this.readableDatabase
        val cursor = db.query(
            TABLE_GROUP_MEMBERS,
            arrayOf(COLUMN_USER_ID),
            "$COLUMN_GROUP_ID = ? AND $COLUMN_USER_ID = ?",
            arrayOf(groupId, userId),
            null, null, null
        )
        val result = cursor.count > 0
        cursor.close()
        return result
    }

    fun getUserRoleInGroup(userId: String, groupId: String): UserRole? {
        return readableDatabase.rawQuery(
            "SELECT $COLUMN_ROLE FROM $TABLE_GROUP_MEMBERS WHERE $COLUMN_USER_ID = ? AND $COLUMN_GROUP_ID = ?",
            arrayOf(userId, groupId)
        ).use { cursor ->
            if (cursor.moveToFirst()) {
                UserRole.valueOf(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_ROLE)))
            } else {
                null
            }
        }
    }

    fun removeGroupMember(groupId: String, userId: String): Boolean {
        return try {
            val db = this.writableDatabase
            val result = db.delete(
                TABLE_GROUP_MEMBERS,
                "$COLUMN_GROUP_ID = ? AND $COLUMN_USER_ID = ?",
                arrayOf(groupId, userId)
            ) > 0
            result
        } catch (e: Exception) {
            false
        }
    }

    // Методы для работы с сообщениями
    fun addMessage(message: GroupMessage): Boolean {
        val db = writableDatabase
        db.beginTransaction()
        return try {
            val values = ContentValues().apply {
                put(COLUMN_ID, message.id)
                put(COLUMN_GROUP_ID, message.groupId)
                put(COLUMN_SENDER_ID, message.senderId)
                put(COLUMN_TEXT, message.text)
                put(COLUMN_TIMESTAMP, message.timestamp)
            }

            val result = db.insert(TABLE_GROUP_MESSAGES, null, values) != -1L
            db.setTransactionSuccessful()
            result
        } catch (e: Exception) {
            Log.e(TAG, "Error adding message", e)
            false
        } finally {
            db.endTransaction()
        }
    }

    fun getMessagesForGroup(groupId: String): List<GroupMessage> {
        return try {
            readableDatabase.rawQuery(
                """SELECT * FROM $TABLE_GROUP_MESSAGES 
               WHERE $COLUMN_GROUP_ID = ? 
               ORDER BY $COLUMN_TIMESTAMP ASC""",
                arrayOf(groupId)
            ).use { cursor ->
                mutableListOf<GroupMessage>().apply {
                    while (cursor.moveToNext()) {
                        add(GroupMessage(
                            id = cursor.getString(0),
                            groupId = cursor.getString(1),
                            senderId = cursor.getString(2),
                            text = cursor.getString(3),
                            timestamp = cursor.getLong(4)
                        ))
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error loading messages", e)
            emptyList()
        }
    }

    fun getLastMessagesForUserGroups(userId: String): List<GroupMessage> {
        return readableDatabase.rawQuery(
            """
            SELECT m.* FROM $TABLE_GROUP_MESSAGES m
            JOIN $TABLE_GROUP_MEMBERS mem ON m.$COLUMN_GROUP_ID = mem.$COLUMN_GROUP_ID
            WHERE mem.$COLUMN_USER_ID = ?
            ORDER BY m.$COLUMN_TIMESTAMP DESC
            """, arrayOf(userId)
        ).use { cursor ->
            val messages = mutableListOf<GroupMessage>()
            while (cursor.moveToNext()) {
                messages.add(
                    GroupMessage(
                        id = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_ID)),
                        groupId = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_GROUP_ID)),
                        senderId = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_SENDER_ID)),
                        text = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_TEXT)),
                        timestamp = cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_TIMESTAMP))
                    )
                )
            }
            messages
        }
    }

    // Методы для работы с файлами
    fun addFile(file: GroupFile): Boolean {
        val db = writableDatabase
        db.beginTransaction()
        return try {
            val values = ContentValues().apply {
                put(COLUMN_FILE_ID, file.id)
                put(COLUMN_GROUP_ID, file.groupId)
                put(COLUMN_FILE_NAME, file.fileName)
                put(COLUMN_FILE_PATH, file.filePath)
                put(COLUMN_UPLOADER_ID, file.uploaderId)
                put(COLUMN_UPLOAD_TIME, file.uploadTime)
            }

            val result = db.insert(TABLE_FILES, null, values) != -1L
            db.setTransactionSuccessful()
            result
        } catch (e: Exception) {
            Log.e(TAG, "Error adding file", e)
            false
        } finally {
            db.endTransaction()
        }
    }

    fun getGroupFiles(groupId: String): List<GroupFile> {
        return readableDatabase.rawQuery(
            "SELECT * FROM $TABLE_FILES WHERE $COLUMN_GROUP_ID = ? ORDER BY $COLUMN_UPLOAD_TIME DESC",
            arrayOf(groupId)
        ).use { cursor ->
            val files = mutableListOf<GroupFile>()
            while (cursor.moveToNext()) {
                files.add(
                    GroupFile(
                        id = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_FILE_ID)),
                        groupId = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_GROUP_ID)),
                        fileName = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_FILE_NAME)),
                        filePath = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_FILE_PATH)),
                        uploaderId = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_UPLOADER_ID)),
                        uploadTime = cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_UPLOAD_TIME))
                    )
                )
            }
            files
        }
    }

    fun deleteFile(fileId: String): Boolean {
        val db = writableDatabase
        db.beginTransaction()
        return try {
            val result = db.delete(
                TABLE_FILES,
                "$COLUMN_FILE_ID = ?",
                arrayOf(fileId)
            ) > 0
            db.setTransactionSuccessful()
            result
        } catch (e: Exception) {
            Log.e(TAG, "Error deleting file", e)
            false
        } finally {
            db.endTransaction()
        }
    }

    fun getFilesCountForUserGroups(userId: String): Map<String, Int> {
        return readableDatabase.rawQuery(
            """
            SELECT f.$COLUMN_GROUP_ID, COUNT(*) as count FROM $TABLE_FILES f
            JOIN $TABLE_GROUP_MEMBERS m ON f.$COLUMN_GROUP_ID = m.$COLUMN_GROUP_ID
            WHERE m.$COLUMN_USER_ID = ?
            GROUP BY f.$COLUMN_GROUP_ID
            """,
            arrayOf(userId))
            .use { cursor ->
                val result = mutableMapOf<String, Int>()
                while (cursor.moveToNext()) {
                    result[cursor.getString(0)] = cursor.getInt(1)
                }
                result
            }
    }

    // Методы для отладки
    fun debugDatabase() {
        Log.d("DB_DEBUG", "=== Проверка структуры БД ===")

        // Проверяем существование таблиц
        val tablesCursor = readableDatabase.rawQuery(
            "SELECT name FROM sqlite_master WHERE type='table'", null
        )

        Log.d("DB_DEBUG", "Таблицы в БД:")
        while (tablesCursor.moveToNext()) {
            Log.d("DB_DEBUG", tablesCursor.getString(0))
        }
        tablesCursor.close()

        // Проверяем структуру таблицы групп
        try {
            val groupsCursor = readableDatabase.rawQuery(
                "SELECT * FROM $TABLE_GROUPS LIMIT 0", null
            )
            Log.d("DB_DEBUG", "Колонки в $TABLE_GROUPS:")
            groupsCursor.columnNames.forEach {
                Log.d("DB_DEBUG", it)
            }
            groupsCursor.close()
        } catch (e: Exception) {
            Log.e("DB_ERROR", "Ошибка при проверке таблицы групп", e)
        }
    }

    fun updateUserEmail(login: String, newEmail: String): Boolean {
        val db = writableDatabase
        db.beginTransaction()
        return try {
            val values = ContentValues().apply {
                put(COLUMN_EMAIL, newEmail)
            }
            val result = db.update(
                TABLE_USERS,
                values,
                "$COLUMN_LOGIN = ?",
                arrayOf(login)
            ) > 0
            db.setTransactionSuccessful()
            result
        } catch (e: Exception) {
            Log.e(TAG, "Error updating user email", e)
            false
        } finally {
            db.endTransaction()
        }
    }

    fun updateUserLogin(oldLogin: String, newLogin: String, password: String): Boolean {
        val db = writableDatabase
        db.beginTransaction()
        return try {
            // Проверяем пароль
            val currentUser = getUser(oldLogin, password)
            if (currentUser == null) {
                return false
            }

            // Проверяем, не занят ли новый логин
            if (isUserExists(newLogin)) {
                return false
            }

            val values = ContentValues().apply {
                put(COLUMN_LOGIN, newLogin)
            }
            val result = db.update(
                TABLE_USERS,
                values,
                "$COLUMN_LOGIN = ?",
                arrayOf(oldLogin)
            ) > 0
            db.setTransactionSuccessful()
            result
        } catch (e: Exception) {
            Log.e(TAG, "Error updating user login", e)
            false
        } finally {
            db.endTransaction()
        }
    }

    // Методы для синхронизации с Firebase
    fun syncWithFirebase(userId: String) {
        if (syncInProgress.get()) return
        syncInProgress.set(true)

        CoroutineScope(Dispatchers.IO).launch {
            try {
                syncGroups(userId)
                syncGroupMembers(userId)
                syncMessages(userId)
                syncFiles(userId)
                Log.d(TAG, "Синхронизация завершена успешно")
            } catch (e: Exception) {
                Log.e(TAG, "Ошибка синхронизации", e)
            } finally {
                syncInProgress.set(false)
            }
        }
    }

    private suspend fun syncGroups(userId: String) {
        try {
            val firestoreGroups = firestore.collection(FIRESTORE_GROUPS)
                .whereArrayContains("members", userId)
                .get()
                .await()

            val db = writableDatabase
            db.beginTransaction()
            try {
                db.execSQL("DELETE FROM $TABLE_GROUPS WHERE $COLUMN_ID IN " +
                        "(SELECT $COLUMN_GROUP_ID FROM $TABLE_GROUP_MEMBERS WHERE $COLUMN_USER_ID = ?)",
                    arrayOf(userId))

                for (doc in firestoreGroups) {
                    val group = doc.toObject<Group>()
                    val values = ContentValues().apply {
                        put(COLUMN_ID, group.id)
                        put(COLUMN_NAME, group.name)
                        put(COLUMN_CREATOR_ID, group.creatorId)
                        put(COLUMN_DESCRIPTION, group.description ?: "")
                    }
                    db.insert(TABLE_GROUPS, null, values)
                }
                db.setTransactionSuccessful()
            } finally {
                db.endTransaction()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Ошибка синхронизации групп", e)
        }
    }

    private suspend fun syncGroupMembers(userId: String) {
        try {
            val userGroups = getGroupsForUser(userId).map { it.id }

            val db = writableDatabase
            db.beginTransaction()
            try {
                for (groupId in userGroups) {
                    db.delete(TABLE_GROUP_MEMBERS, "$COLUMN_GROUP_ID = ?", arrayOf(groupId))

                    val members = firestore.collection(FIRESTORE_GROUPS)
                        .document(groupId)
                        .collection(FIRESTORE_MEMBERS)
                        .get()
                        .await()

                    for (doc in members) {
                        val member = doc.toObject<GroupMember>()
                        val values = ContentValues().apply {
                            put(COLUMN_GROUP_ID, member.groupId)
                            put(COLUMN_USER_ID, member.userId)
                            put(COLUMN_ROLE, member.role.toString())
                        }
                        db.insert(TABLE_GROUP_MEMBERS, null, values)
                    }
                }
                db.setTransactionSuccessful()
            } finally {
                db.endTransaction()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Ошибка синхронизации участников групп", e)
        }
    }

    private suspend fun syncMessages(userId: String) {
        try {
            val userGroups = getGroupsForUser(userId).map { it.id }

            val db = writableDatabase
            db.beginTransaction()
            try {
                for (groupId in userGroups) {
                    db.delete(TABLE_GROUP_MESSAGES, "$COLUMN_GROUP_ID = ?", arrayOf(groupId))

                    val messages = firestore.collection(FIRESTORE_GROUPS)
                        .document(groupId)
                        .collection(FIRESTORE_MESSAGES)
                        .orderBy("timestamp")
                        .get()
                        .await()

                    for (doc in messages) {
                        val message = doc.toObject<GroupMessage>()
                        val values = ContentValues().apply {
                            put(COLUMN_ID, message.id)
                            put(COLUMN_GROUP_ID, message.groupId)
                            put(COLUMN_SENDER_ID, message.senderId)
                            put(COLUMN_TEXT, message.text)
                            put(COLUMN_TIMESTAMP, message.timestamp)
                        }
                        db.insert(TABLE_GROUP_MESSAGES, null, values)
                    }
                }
                db.setTransactionSuccessful()
            } finally {
                db.endTransaction()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Ошибка синхронизации сообщений", e)
        }
    }

    private suspend fun syncFiles(userId: String) {
        try {
            val userGroups = getGroupsForUser(userId).map { it.id }

            val db = writableDatabase
            db.beginTransaction()
            try {
                for (groupId in userGroups) {
                    db.delete(TABLE_FILES, "$COLUMN_GROUP_ID = ?", arrayOf(groupId))

                    val files = firestore.collection(FIRESTORE_GROUPS)
                        .document(groupId)
                        .collection(FIRESTORE_FILES_METADATA)
                        .orderBy("uploadTime", com.google.firebase.firestore.Query.Direction.DESCENDING)
                        .get()
                        .await()

                    for (doc in files) {
                        val file = doc.toObject<GroupFile>()
                        val values = ContentValues().apply {
                            put(COLUMN_FILE_ID, file.id)
                            put(COLUMN_GROUP_ID, file.groupId)
                            put(COLUMN_FILE_NAME, file.fileName)
                            put(COLUMN_FILE_PATH, file.filePath)
                            put(COLUMN_UPLOADER_ID, file.uploaderId)
                            put(COLUMN_UPLOAD_TIME, file.uploadTime)
                        }
                        db.insert(TABLE_FILES, null, values)
                    }
                }
                db.setTransactionSuccessful()
            } finally {
                db.endTransaction()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Ошибка синхронизации файлов", e)
        }
    }

    // Методы для синхронизированных операций
    fun addMessageWithSync(message: GroupMessage): Boolean {
        val dbResult = addMessage(message)
        if (dbResult) {
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    firestore.collection(FIRESTORE_GROUPS)
                        .document(message.groupId)
                        .collection(FIRESTORE_MESSAGES)
                        .document(message.id)
                        .set(message)
                        .await()
                } catch (e: Exception) {
                    Log.e(TAG, "Ошибка при синхронизации сообщения", e)
                }
            }
        }
        return dbResult
    }

    fun createGroupWithSync(group: Group, creatorId: String): Boolean {
        val dbResult = createGroup(group)
        if (dbResult) {
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    firestore.collection(FIRESTORE_GROUPS)
                        .document(group.id)
                        .set(mapOf(
                            "groupId" to group.id,
                            "userId" to creatorId,
                            "role" to UserRole.ADMIN.name
                        ))
                        .await()

                    firestore.collection(FIRESTORE_GROUPS)
                        .document(group.id)
                        .collection(FIRESTORE_MEMBERS)
                        .document(creatorId)
                        .set(mapOf(
                            "groupId" to group.id,
                            "userId" to creatorId,
                            "role" to UserRole.ADMIN.name)
                        )
                        .await()
                } catch (e: Exception) {
                    Log.e(TAG, "Ошибка при синхронизации группы", e)
                }
            }
        }
        return dbResult
    }

    fun addGroupMemberWithSync(member: GroupMember): Boolean {
        val dbResult = addGroupMember(member)
        if (dbResult) {
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    firestore.collection(FIRESTORE_GROUPS)
                        .document(member.groupId)
                        .collection(FIRESTORE_MEMBERS)
                        .document(member.userId)
                        .set(mapOf(
                            "groupId" to member.groupId,
                            "userId" to member.userId,
                            "role" to UserRole.ADMIN.name
                        ))
                        .await()
                } catch (e: Exception) {
                    Log.e(TAG, "Ошибка при синхронизации участника группы", e)
                }
            }
        }
        return dbResult
    }

    fun addFileWithSync(file: GroupFile): Boolean {
        val dbResult = addFile(file)
        if (dbResult) {
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    firestore.collection(FIRESTORE_GROUPS)
                        .document(file.groupId)
                        .collection(FIRESTORE_FILES_METADATA)
                        .document(file.id)
                        .set(file)
                        .await()
                } catch (e: Exception) {
                    Log.e(TAG, "Ошибка при синхронизации файла", e)
                }
            }
        }
        return dbResult
    }
}