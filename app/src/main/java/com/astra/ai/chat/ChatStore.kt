
package com.astra.ai.chat

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

class ChatStore(context: Context) :
    SQLiteOpenHelper(context, "astra_conversations.db", null, 1) {

    override fun onCreate(db: SQLiteDatabase) {

        db.execSQL("""
            CREATE TABLE conversations (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                title TEXT NOT NULL,
                created_at INTEGER NOT NULL,
                updated_at INTEGER NOT NULL
            )
        """)

        db.execSQL("""
            CREATE TABLE messages (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                conversation_id INTEGER NOT NULL,
                role TEXT NOT NULL,
                content TEXT NOT NULL,
                timestamp INTEGER NOT NULL,
                FOREIGN KEY(conversation_id)
                REFERENCES conversations(id)
                ON DELETE CASCADE
            )
        """)
    }

    override fun onUpgrade(
        db: SQLiteDatabase,
        oldVersion: Int,
        newVersion: Int
    ) {}

    fun createConversation(
        title: String = "Nueva conversación"
    ): Long {

        val now = System.currentTimeMillis()

        val values = ContentValues().apply {
            put("title", title)
            put("created_at", now)
            put("updated_at", now)
        }

        return writableDatabase.insert(
            "conversations", null, values
        )
    }

    fun getLatestConversation(): Long {

        readableDatabase.rawQuery(
            "SELECT id FROM conversations ORDER BY updated_at DESC LIMIT 1",
            null
        ).use { cursor ->

            if (cursor.moveToFirst()) {
                return cursor.getLong(0)
            }
        }

        return createConversation()
    }

    fun getConversations(): List<ChatConversation> {

        val result = mutableListOf<ChatConversation>()

        readableDatabase.rawQuery(
            """
            SELECT id, title, created_at, updated_at
            FROM conversations
            ORDER BY updated_at DESC
            """,
            null
        ).use { cursor ->

            while (cursor.moveToNext()) {
                result.add(
                    ChatConversation(
                        id = cursor.getLong(0),
                        title = cursor.getString(1),
                        createdAt = cursor.getLong(2),
                        updatedAt = cursor.getLong(3)
                    )
                )
            }
        }

        return result
    }

    fun addMessage(
        conversationId: Long,
        role: String,
        content: String
    ): Long {

        val now = System.currentTimeMillis()

        val values = ContentValues().apply {
            put("conversation_id", conversationId)
            put("role", role)
            put("content", content)
            put("timestamp", now)
        }

        val id = writableDatabase.insert(
            "messages", null, values
        )

        val update = ContentValues().apply {
            put("updated_at", now)

            if (role == "user") {
                put("title", content.take(45))
            }
        }

        writableDatabase.update(
            "conversations",
            update,
            "id = ?",
            arrayOf(conversationId.toString())
        )

        return id
    }

    fun getMessages(conversationId: Long): List<ChatMessage> {

        val result = mutableListOf<ChatMessage>()

        readableDatabase.rawQuery(
            """
            SELECT id, conversation_id, role, content, timestamp
            FROM messages
            WHERE conversation_id = ?
            ORDER BY timestamp ASC, id ASC
            """,
            arrayOf(conversationId.toString())
        ).use { cursor ->

            while (cursor.moveToNext()) {
                result.add(
                    ChatMessage(
                        id = cursor.getLong(0),
                        conversationId = cursor.getLong(1),
                        role = cursor.getString(2),
                        content = cursor.getString(3),
                        timestamp = cursor.getLong(4)
                    )
                )
            }
        }

        return result
    }

    fun deleteConversation(id: Long) {
        writableDatabase.delete(
            "messages",
            "conversation_id = ?",
            arrayOf(id.toString())
        )

        writableDatabase.delete(
            "conversations",
            "id = ?",
            arrayOf(id.toString())
        )
    }

    fun clearAll() {
        writableDatabase.delete("messages", null, null)
        writableDatabase.delete("conversations", null, null)
    }
    }
    
