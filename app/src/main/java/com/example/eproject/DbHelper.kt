package com.example.eproject

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.util.Log

class DbHelper(context: Context) : SQLiteOpenHelper(
    context,
    DATABASE_NAME,
    null,
    DATABASE_VERSION
) {
    companion object {
        private const val TAG = "DbHelper"
        private const val DATABASE_NAME = "app_db.db"
        private const val DATABASE_VERSION = 4

        // Таблицы и колонки
        private const val TABLE_USERS = "users"
        private const val TABLE_GROUPS = "groups"
        private const val TABLE_GROUP_MEMBERS = "group_members"
        private const val TABLE_GROUP_MESSAGES = "group_messages"

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
    }

    override fun onCreate(db: SQLiteDatabase) {
        db.beginTransaction()
        try {
            // Создаем таблицы
            db.execSQL(
                """
            CREATE TABLE $TABLE_USERS (
                $COLUMN_LOGIN TEXT PRIMARY KEY,
                $COLUMN_EMAIL TEXT NOT NULL,
                $COLUMN_PASSWORD TEXT NOT NULL
            )"""
            )

            db.execSQL("""
            CREATE TABLE $TABLE_GROUPS (
                $COLUMN_ID TEXT PRIMARY KEY,
                $COLUMN_NAME TEXT NOT NULL,
                $COLUMN_CREATOR_ID TEXT NOT NULL,
                $COLUMN_DESCRIPTION TEXT DEFAULT ''
            )""")

            db.execSQL("""
            CREATE TABLE $TABLE_GROUP_MEMBERS (
                $COLUMN_GROUP_ID TEXT NOT NULL,
                $COLUMN_USER_ID TEXT NOT NULL,
                $COLUMN_ROLE TEXT NOT NULL,
                PRIMARY KEY ($COLUMN_GROUP_ID, $COLUMN_USER_ID)
            )""")

            db.execSQL(
                """
            CREATE TABLE $TABLE_GROUP_MESSAGES (
                $COLUMN_ID TEXT PRIMARY KEY,
                $COLUMN_GROUP_ID TEXT NOT NULL,
                $COLUMN_SENDER_ID TEXT NOT NULL,
                $COLUMN_TEXT TEXT NOT NULL,
                $COLUMN_TIMESTAMP INTEGER NOT NULL,
                FOREIGN KEY ($COLUMN_GROUP_ID) REFERENCES $TABLE_GROUPS($COLUMN_ID),
                FOREIGN KEY ($COLUMN_SENDER_ID) REFERENCES $TABLE_USERS($COLUMN_LOGIN)
            )"""
            )

            // Создаем индексы
            db.execSQL("CREATE INDEX IF NOT EXISTS idx_group_id ON $TABLE_GROUPS($COLUMN_ID)")
            db.execSQL("CREATE INDEX IF NOT EXISTS idx_group_description ON $TABLE_GROUPS($COLUMN_DESCRIPTION)")

            db.setTransactionSuccessful()

        } catch (e: Exception) {
            Log.e(TAG, "Error creating database tables", e)
            // В случае ошибки вызываем принудительное обновление БД
            forceDbUpgrade(db)
        } finally {
            db.endTransaction()
        }
    }

    // Новый метод для принудительного обновления БД
    private fun forceDbUpgrade(db: SQLiteDatabase) {
        try {
            // Удаляем все таблицы
            db.execSQL("DROP TABLE IF EXISTS $TABLE_GROUP_MESSAGES")
            db.execSQL("DROP TABLE IF EXISTS $TABLE_GROUP_MEMBERS")
            db.execSQL("DROP TABLE IF EXISTS $TABLE_GROUPS")
            db.execSQL("DROP TABLE IF EXISTS $TABLE_USERS")

            // Создаем заново
            onCreate(db)
            Log.w(TAG, "Database was recreated due to migration failure")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to recreate database", e)
        }
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        if (oldVersion < 4) {
            // Добавляем столбец description если его нет
            db.execSQL("ALTER TABLE $TABLE_GROUPS ADD COLUMN $COLUMN_DESCRIPTION TEXT DEFAULT ''")
            Log.i("DB_UPGRADE", "Добавлен столбец description в таблицу groups")
        }
    }

    fun isUserInGroup(userId: String, groupId: String): Boolean {
        val db = this.readableDatabase
        val cursor = db.query(
            "group_members",
            arrayOf("user_id"),
            "group_id = ? AND user_id = ?",
            arrayOf(groupId, userId),
            null, null, null
        )
        val result = cursor.count > 0
        cursor.close()
        return result
    }

    private fun recreateDatabase(db: SQLiteDatabase) {
        try {
            db.execSQL("DROP TABLE IF EXISTS $TABLE_GROUP_MESSAGES")
            db.execSQL("DROP TABLE IF EXISTS $TABLE_GROUP_MEMBERS")
            db.execSQL("DROP TABLE IF EXISTS $TABLE_GROUPS")
            db.execSQL("DROP TABLE IF EXISTS $TABLE_USERS")
            onCreate(db)
            Log.w(TAG, "Database recreated due to migration failure")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to recreate database", e)
        }
    }

    fun getGroupById(userId: String, groupId: String): Group? {
        return readableDatabase.rawQuery(
            """
            SELECT g.* FROM $TABLE_GROUPS g
            JOIN $TABLE_GROUP_MEMBERS m ON g.$COLUMN_ID = m.$COLUMN_GROUP_ID
            WHERE m.$COLUMN_USER_ID = ? AND g.$COLUMN_ID = ?
            """, arrayOf(userId, groupId)
        )
            .use { cursor ->
                if (cursor.moveToFirst()) {
                    Group(
                        id = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_ID)),
                        name = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_NAME)),
                        creatorId = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_CREATOR_ID)),
                        description = cursor.getString(
                            cursor.getColumnIndexOrThrow(
                                COLUMN_DESCRIPTION
                            )
                        ) ?: ""
                    )
                } else {
                    null
                }
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
    // Добавляем эти методы в класс DbHelper:

    fun getGroupsForUser(userId: String): List<Group> {
        return readableDatabase.rawQuery(
            """
        SELECT g.* FROM $TABLE_GROUPS g
        JOIN $TABLE_GROUP_MEMBERS m ON g.$COLUMN_ID = m.$COLUMN_GROUP_ID
        WHERE m.$COLUMN_USER_ID = ?
        """, arrayOf(userId))
            .use { cursor ->
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



    fun getMessagesForGroup(groupId: String): List<GroupMessage> {
        val cursor = readableDatabase.rawQuery(
            """
        SELECT * FROM $TABLE_GROUP_MESSAGES 
        WHERE $COLUMN_GROUP_ID = ?
        ORDER BY $COLUMN_TIMESTAMP ASC
        """, arrayOf(groupId)
        )

        return cursor.use {
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
    fun addUser(user: User): Boolean {
        val db = writableDatabase
        db.beginTransaction()
        return try {
            val values = ContentValues().apply {
                put(COLUMN_LOGIN, user.login)
                put(COLUMN_EMAIL, user.email)
                put(COLUMN_PASSWORD, user.password)
            }

            // Проверяем, не существует ли уже пользователь с таким логином
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

            Log.d("DB_DEBUG", "Пытаемся создать группу: ${group.name}")
            Log.d("DB_DEBUG", "Group values: $values")

            val result = db.insert(TABLE_GROUPS, null, values)
            if (result == -1L) {
                Log.e("DB_ERROR", "Ошибка при вставке группы в таблицу $TABLE_GROUPS")
                return false
            }

            db.setTransactionSuccessful()
            Log.d("DB_DEBUG", "Группа успешно создана с ID: ${group.id}")
            return true
        } catch (e: Exception) {
            Log.e("DB_ERROR", "Исключение при создании группы", e)
            return false
        } finally {
            db.endTransaction()
        }
    }

    fun addGroupMember(member: GroupMember): Boolean {
        val db = writableDatabase
        db.beginTransaction()
        try {
            val values = ContentValues().apply {
                put(COLUMN_GROUP_ID, member.groupId)
                put(COLUMN_USER_ID, member.userId)
                put(COLUMN_ROLE, member.role.name)
            }

            Log.d("DB_DEBUG", "Пытаемся добавить участника: ${member.userId} в группу ${member.groupId}")

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

    private fun isUserExists(login: String): Boolean {
        return readableDatabase.rawQuery(
            "SELECT 1 FROM $TABLE_USERS WHERE $COLUMN_LOGIN = ?",
            arrayOf(login)
        ).use { cursor ->
            cursor.count > 0
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
    fun removeGroupMember(groupId: String, userId: String): Boolean {
        return try {
            val db = this.writableDatabase
            val result = db.delete(
                "group_members",
                "group_id = ? AND user_id = ?",
                arrayOf(groupId, userId)
            ) > 0
            db.close()
            result
        } catch (e: Exception) {
            false
        }
    }

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

        fun debugDatabase() {
            Log.d("DB_DEBUG", "=== Проверка структуры БД ===")

            // Проверяем существование таблиц
            val tablesCursor = readableDatabase.rawQuery(
                "SELECT name FROM sqlite_master WHERE type='table'", null)

            Log.d("DB_DEBUG", "Таблицы в БД:")
            while (tablesCursor.moveToNext()) {
                Log.d("DB_DEBUG", tablesCursor.getString(0))
            }
            tablesCursor.close()

            // Проверяем структуру таблицы групп
            try {
                val groupsCursor = readableDatabase.rawQuery(
                    "SELECT * FROM $TABLE_GROUPS LIMIT 0", null)
                Log.d("DB_DEBUG", "Колонки в $TABLE_GROUPS:")
                groupsCursor.columnNames.forEach {
                    Log.d("DB_DEBUG", it)
                }
                groupsCursor.close()
            } catch (e: Exception) {
                Log.e("DB_ERROR", "Ошибка при проверке таблицы групп", e)
            }
        }
    }
}

