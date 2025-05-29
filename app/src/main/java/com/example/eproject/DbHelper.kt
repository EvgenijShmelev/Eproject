package com.example.eproject

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
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
        private const val DATABASE_VERSION = 2
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
    }

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE $TABLE_USERS (
                $COLUMN_LOGIN TEXT PRIMARY KEY,
                $COLUMN_EMAIL TEXT NOT NULL,
                $COLUMN_PASSWORD TEXT NOT NULL
            )
        """.trimIndent()
        )

        db.execSQL(
            """
            CREATE TABLE $TABLE_GROUPS (
                $COLUMN_ID TEXT PRIMARY KEY,
                $COLUMN_NAME TEXT NOT NULL,
                $COLUMN_CREATOR_ID TEXT NOT NULL,
                FOREIGN KEY ($COLUMN_CREATOR_ID) REFERENCES $TABLE_USERS($COLUMN_LOGIN)
            )
        """.trimIndent()
        )

        db.execSQL(
            """
            CREATE TABLE $TABLE_GROUP_MEMBERS (
                $COLUMN_GROUP_ID TEXT NOT NULL,
                $COLUMN_USER_ID TEXT NOT NULL,
                $COLUMN_ROLE TEXT NOT NULL,
                PRIMARY KEY ($COLUMN_GROUP_ID, $COLUMN_USER_ID),
                FOREIGN KEY ($COLUMN_GROUP_ID) REFERENCES $TABLE_GROUPS($COLUMN_ID),
                FOREIGN KEY ($COLUMN_USER_ID) REFERENCES $TABLE_USERS($COLUMN_LOGIN)
            )
        """.trimIndent()
        )

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
            )
        """.trimIndent()
        )
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS $TABLE_GROUP_MESSAGES")
        db.execSQL("DROP TABLE IF EXISTS $TABLE_GROUP_MEMBERS")
        db.execSQL("DROP TABLE IF EXISTS $TABLE_GROUPS")
        db.execSQL("DROP TABLE IF EXISTS $TABLE_USERS")
        onCreate(db)
    }

    fun addUser (user: User): Boolean {
        val db = writableDatabase
        val values = ContentValues().apply {
            put(COLUMN_LOGIN, user.login)
            put(COLUMN_EMAIL, user.email)
            put(COLUMN_PASSWORD, user.password)
        }
        return db.insert(TABLE_USERS, null, values) != -1L
    }

    fun getUser (login: String, password: String): Boolean {
        val db = readableDatabase
        val cursor = db.query(
            TABLE_USERS,
            arrayOf(COLUMN_LOGIN),
            "$COLUMN_LOGIN = ? AND $COLUMN_PASSWORD = ?",
            arrayOf(login, password),
            null, null, null
        )
        val exists = cursor.count > 0
        cursor.close()
        return exists
    }

    fun createGroup(group: Group): Boolean {
        val db = writableDatabase
        return try {
            val values = ContentValues().apply {
                put(COLUMN_ID, group.id)
                put(COLUMN_NAME, group.name)
                put(COLUMN_CREATOR_ID, group.creatorId)
            }
            db.insert(TABLE_GROUPS, null, values) != -1L
        } catch (e: Exception) {
            Log.e(TAG, "Error creating group", e)
            false
        }
    }

    fun addGroupMember(member: GroupMember): Boolean {
        val db = writableDatabase
        return try {
            val values = ContentValues().apply {
                put(COLUMN_GROUP_ID, member.groupId)
                put(COLUMN_USER_ID, member.userId)
                put(COLUMN_ROLE, member.role.name)
            }
            db.insert(TABLE_GROUP_MEMBERS, null, values) != -1L
        } catch (e: Exception) {
            Log.e(TAG, "Error adding group member", e)
            false
        }
    }

    fun getGroupsForUser (userId: String): List<Group> {
        val db = readableDatabase
        val groups = mutableListOf<Group>()
        var cursor: Cursor? = null

        try {
            cursor = db.rawQuery(
                """
                SELECT g.* FROM $TABLE_GROUPS g
                JOIN $TABLE_GROUP_MEMBERS m ON g.$COLUMN_ID = m.$COLUMN_GROUP_ID
                WHERE m.$COLUMN_USER_ID = ?
                """, arrayOf(userId)
            )

            while (cursor.moveToNext()) {
                try {
                    groups.add(
                        Group(
                            id = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_ID)),
                            name = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_NAME)),
                            creatorId = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_CREATOR_ID))
                        )
                    )
                } catch (e: Exception) {
                    Log.e(TAG, "Error parsing group data", e)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error getting groups for user", e)
        } finally {
            cursor?.close()
        }
        return groups
    }

    fun addMessage(message: GroupMessage): Boolean {
        val db = writableDatabase
        val values = ContentValues().apply {
            put(COLUMN_ID, message.id)
            put(COLUMN_GROUP_ID, message.groupId)
            put(COLUMN_SENDER_ID, message.senderId)
            put(COLUMN_TEXT, message.text)
            put(COLUMN_TIMESTAMP, message.timestamp)
        }
        return db.insert(TABLE_GROUP_MESSAGES, null, values) != -1L
    }
    fun getUserRoleInGroup(userId: String, groupId: String): UserRole {
        val db = readableDatabase
        var cursor: Cursor? = null
        return try {
            cursor = db.query(
                TABLE_GROUP_MEMBERS,
                arrayOf(COLUMN_ROLE),
                "$COLUMN_USER_ID = ? AND $COLUMN_GROUP_ID = ?",
                arrayOf(userId, groupId),
                null, null, null
            )

            if (cursor.moveToFirst()) {
                val roleStr = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_ROLE))
                UserRole.valueOf(roleStr)
            } else {
                UserRole.MEMBER // или другое значение по умолчанию
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error getting user role", e)
            UserRole.MEMBER
        } finally {
            cursor?.close()
        }
    }

    fun getMessagesForGroup(groupId: String): List<GroupMessage> {
        val db = readableDatabase
        val messages = mutableListOf<GroupMessage>()
        var cursor: Cursor? = null

        try {
            cursor = db.query(
                TABLE_GROUP_MESSAGES,
                null,
                "$COLUMN_GROUP_ID = ?",
                arrayOf(groupId),
                null, null,
                "$COLUMN_TIMESTAMP ASC"
            )

            while (cursor.moveToNext()) {
                try {
                    messages.add(
                        GroupMessage(
                            id = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_ID)),
                            groupId = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_GROUP_ID)),
                            senderId = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_SENDER_ID)),
                            text = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_TEXT)),
                            timestamp = cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_TIMESTAMP))
                        )
                    )
                } catch (e: Exception) {
                    Log.e(TAG, "Error parsing message data", e)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error getting messages for group", e)
        } finally {
            cursor?.close()
        }
        return messages
    }
}
