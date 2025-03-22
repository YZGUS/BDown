package com.yz.bdown.model.chat.db;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import com.yz.bdown.model.chat.ChatMessage;
import com.yz.bdown.model.chat.ChatSession;
import com.yz.bdown.model.chat.ChatSessionSummary;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

public class ChatDbHelper extends SQLiteOpenHelper {

    private static final String TAG = "ChatDbHelper";
    private static final String DATABASE_NAME = "chat_history.db";
    private static final int DATABASE_VERSION = 1;
    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());

    // 表名
    private static final String TABLE_SESSIONS = "sessions";
    private static final String TABLE_MESSAGES = "messages";

    // 会话表字段
    private static final String COL_SESSION_ID = "id";
    private static final String COL_SESSION_NAME = "name";
    private static final String COL_SESSION_MODEL = "model_name";
    private static final String COL_SESSION_CREATE_TIME = "create_time";
    private static final String COL_SESSION_UPDATE_TIME = "update_time";

    // 消息表字段
    private static final String COL_MESSAGE_ID = "id";
    private static final String COL_MESSAGE_SESSION_ID = "session_id";
    private static final String COL_MESSAGE_ROLE = "role";
    private static final String COL_MESSAGE_CONTENT = "content";
    private static final String COL_MESSAGE_REASONING = "reasoning";
    private static final String COL_MESSAGE_TIMESTAMP = "timestamp";

    // 创建会话表的SQL语句
    private static final String CREATE_SESSIONS_TABLE = "CREATE TABLE " + TABLE_SESSIONS + " (" +
            COL_SESSION_ID + " TEXT PRIMARY KEY," +
            COL_SESSION_NAME + " TEXT," +
            COL_SESSION_MODEL + " TEXT," +
            COL_SESSION_CREATE_TIME + " TEXT," +
            COL_SESSION_UPDATE_TIME + " TEXT" +
            ")";

    // 创建消息表的SQL语句
    private static final String CREATE_MESSAGES_TABLE = "CREATE TABLE " + TABLE_MESSAGES + " (" +
            COL_MESSAGE_ID + " TEXT PRIMARY KEY," +
            COL_MESSAGE_SESSION_ID + " TEXT," +
            COL_MESSAGE_ROLE + " TEXT," +
            COL_MESSAGE_CONTENT + " TEXT," +
            COL_MESSAGE_REASONING + " TEXT," +
            COL_MESSAGE_TIMESTAMP + " TEXT," +
            "FOREIGN KEY(" + COL_MESSAGE_SESSION_ID + ") REFERENCES " +
            TABLE_SESSIONS + "(" + COL_SESSION_ID + ")" +
            ")";

    private static ChatDbHelper instance;

    public static synchronized ChatDbHelper getInstance(Context context) {
        if (instance == null) {
            instance = new ChatDbHelper(context.getApplicationContext());
        }
        return instance;
    }

    private ChatDbHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(CREATE_SESSIONS_TABLE);
        db.execSQL(CREATE_MESSAGES_TABLE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // 这里处理数据库升级逻辑
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_MESSAGES);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_SESSIONS);
        onCreate(db);
    }

    /**
     * 插入新会话
     */
    public String insertSession(ChatSession session) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();

        // 生成唯一ID，如果没有的话
        if (session.getId() == null || session.getId().isEmpty()) {
            session.setId(UUID.randomUUID().toString());
        }

        // 如果没有名称，使用第一条用户消息作为名称
        if (session.getName() == null || session.getName().isEmpty()) {
            if (!session.getMessages().isEmpty()) {
                for (ChatMessage message : session.getMessages()) {
                    if (ChatMessage.ROLE_USER.equals(message.getRole())) {
                        String title = message.getContent();
                        if (title.length() > 30) {
                            title = title.substring(0, 27) + "...";
                        }
                        session.setName(title);
                        break;
                    }
                }
            }
            // 如果仍然没有名称，使用时间戳
            if (session.getName() == null || session.getName().isEmpty()) {
                session.setName("会话 " + new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(session.getCreateTime()));
            }
        }

        values.put(COL_SESSION_ID, session.getId());
        values.put(COL_SESSION_NAME, session.getName());
        values.put(COL_SESSION_MODEL, session.getModelName());
        values.put(COL_SESSION_CREATE_TIME, formatDate(session.getCreateTime()));
        values.put(COL_SESSION_UPDATE_TIME, formatDate(session.getLastUpdateTime()));

        long result = db.insert(TABLE_SESSIONS, null, values);
        if (result == -1) {
            Log.e(TAG, "Failed to insert session");
            return null;
        }

        // 插入该会话的所有消息
        for (ChatMessage message : session.getMessages()) {
            message.setSessionId(session.getId());
            insertMessage(message);
        }

        return session.getId();
    }

    /**
     * 插入新消息
     */
    public String insertMessage(ChatMessage message) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();

        // 生成唯一ID，如果没有的话
        if (message.getId() == null || message.getId().isEmpty()) {
            message.setId(UUID.randomUUID().toString());
        }

        values.put(COL_MESSAGE_ID, message.getId());
        values.put(COL_MESSAGE_SESSION_ID, message.getSessionId());
        values.put(COL_MESSAGE_ROLE, message.getRole());
        values.put(COL_MESSAGE_CONTENT, message.getContent());
        values.put(COL_MESSAGE_REASONING, message.getReasoning());
        values.put(COL_MESSAGE_TIMESTAMP, formatDate(message.getTimestamp()));

        long result = db.insert(TABLE_MESSAGES, null, values);
        if (result == -1) {
            Log.e(TAG, "Failed to insert message");
            return null;
        }

        // 更新会话的最后更新时间
        updateSessionLastUpdateTime(message.getSessionId(), message.getTimestamp());

        return message.getId();
    }

    /**
     * 更新会话的最后更新时间
     */
    private void updateSessionLastUpdateTime(String sessionId, Date lastUpdateTime) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COL_SESSION_UPDATE_TIME, formatDate(lastUpdateTime));
        db.update(TABLE_SESSIONS, values, COL_SESSION_ID + " = ?", new String[]{sessionId});
    }

    /**
     * 获取某个会话的所有消息
     */
    public List<ChatMessage> getMessagesForSession(String sessionId) {
        List<ChatMessage> messages = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();

        String query = "SELECT * FROM " + TABLE_MESSAGES + 
                " WHERE " + COL_MESSAGE_SESSION_ID + " = ?" + 
                " ORDER BY " + COL_MESSAGE_TIMESTAMP + " ASC";
        
        Cursor cursor = db.rawQuery(query, new String[]{sessionId});

        if (cursor.moveToFirst()) {
            do {
                String role = cursor.getString(cursor.getColumnIndexOrThrow(COL_MESSAGE_ROLE));
                String content = cursor.getString(cursor.getColumnIndexOrThrow(COL_MESSAGE_CONTENT));
                
                // 确保内容不为null
                if (content == null) {
                    content = "";
                }
                
                ChatMessage message = new ChatMessage(role, content);
                message.setId(cursor.getString(cursor.getColumnIndexOrThrow(COL_MESSAGE_ID)));
                message.setSessionId(cursor.getString(cursor.getColumnIndexOrThrow(COL_MESSAGE_SESSION_ID)));
                
                // 处理推理内容
                String reasoning = cursor.getString(cursor.getColumnIndexOrThrow(COL_MESSAGE_REASONING));
                message.setReasoning(reasoning != null ? reasoning : "");
                
                try {
                    String dateStr = cursor.getString(cursor.getColumnIndexOrThrow(COL_MESSAGE_TIMESTAMP));
                    message.setTimestamp(DATE_FORMAT.parse(dateStr));
                } catch (ParseException e) {
                    Log.e(TAG, "Error parsing date", e);
                    message.setTimestamp(new Date());
                }
                
                messages.add(message);
            } while (cursor.moveToNext());
        }
        cursor.close();
        return messages;
    }

    /**
     * 获取会话摘要列表
     */
    public List<ChatSessionSummary> getSessionSummaries() {
        List<ChatSessionSummary> summaries = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();

        String query = "SELECT s.*, COUNT(m.id) as message_count, " +
                "(SELECT content FROM " + TABLE_MESSAGES + 
                " WHERE session_id = s.id AND role = 'user' ORDER BY timestamp ASC LIMIT 1) as first_message " +
                "FROM " + TABLE_SESSIONS + " s " +
                "LEFT JOIN " + TABLE_MESSAGES + " m ON s.id = m.session_id " +
                "GROUP BY s.id " +
                "ORDER BY s.update_time DESC";
        
        Cursor cursor = db.rawQuery(query, null);

        if (cursor.moveToFirst()) {
            do {
                ChatSessionSummary summary = new ChatSessionSummary();
                summary.setSessionId(cursor.getString(cursor.getColumnIndexOrThrow(COL_SESSION_ID)));
                summary.setName(cursor.getString(cursor.getColumnIndexOrThrow(COL_SESSION_NAME)));
                summary.setModelName(cursor.getString(cursor.getColumnIndexOrThrow(COL_SESSION_MODEL)));
                summary.setMessageCount(cursor.getInt(cursor.getColumnIndexOrThrow("message_count")));
                
                try {
                    String dateStr = cursor.getString(cursor.getColumnIndexOrThrow(COL_SESSION_UPDATE_TIME));
                    summary.setLastUpdateTime(DATE_FORMAT.parse(dateStr));
                } catch (ParseException e) {
                    Log.e(TAG, "Error parsing date", e);
                    summary.setLastUpdateTime(new Date());
                }
                
                // 获取第一条用户消息作为预览
                String firstMessage = cursor.getString(cursor.getColumnIndexOrThrow("first_message"));
                if (firstMessage != null) {
                    if (firstMessage.length() > 50) {
                        firstMessage = firstMessage.substring(0, 47) + "...";
                    }
                    summary.setLastMessage(firstMessage);
                } else {
                    summary.setLastMessage("空会话");
                }
                
                summaries.add(summary);
            } while (cursor.moveToNext());
        }
        cursor.close();
        return summaries;
    }

    /**
     * 获取完整的聊天会话
     */
    public ChatSession getSession(String sessionId) {
        SQLiteDatabase db = this.getReadableDatabase();
        String query = "SELECT * FROM " + TABLE_SESSIONS + " WHERE " + COL_SESSION_ID + " = ?";
        Cursor cursor = db.rawQuery(query, new String[]{sessionId});
        
        ChatSession session = null;
        if (cursor.moveToFirst()) {
            session = new ChatSession();
            session.setId(cursor.getString(cursor.getColumnIndexOrThrow(COL_SESSION_ID)));
            session.setName(cursor.getString(cursor.getColumnIndexOrThrow(COL_SESSION_NAME)));
            session.setModelName(cursor.getString(cursor.getColumnIndexOrThrow(COL_SESSION_MODEL)));
            
            try {
                String createTimeStr = cursor.getString(cursor.getColumnIndexOrThrow(COL_SESSION_CREATE_TIME));
                session.setCreateTime(DATE_FORMAT.parse(createTimeStr));
                
                String updateTimeStr = cursor.getString(cursor.getColumnIndexOrThrow(COL_SESSION_UPDATE_TIME));
                session.setLastUpdateTime(DATE_FORMAT.parse(updateTimeStr));
            } catch (ParseException e) {
                Log.e(TAG, "Error parsing date", e);
                Date now = new Date();
                session.setCreateTime(now);
                session.setLastUpdateTime(now);
            }
            
            // 获取该会话的所有消息
            session.setMessages(getMessagesForSession(sessionId));
        }
        cursor.close();
        return session;
    }

    /**
     * 获取最近的一个会话（按最后更新时间排序）
     */
    public ChatSession getMostRecentSession() {
        SQLiteDatabase db = this.getReadableDatabase();
        String query = "SELECT " + COL_SESSION_ID + " FROM " + TABLE_SESSIONS +
                " ORDER BY " + COL_SESSION_UPDATE_TIME + " DESC LIMIT 1";
        Cursor cursor = db.rawQuery(query, null);
        
        ChatSession session = null;
        if (cursor.moveToFirst()) {
            String sessionId = cursor.getString(cursor.getColumnIndexOrThrow(COL_SESSION_ID));
            session = getSession(sessionId);
        }
        cursor.close();
        
        return session;
    }

    /**
     * 删除会话及其所有消息
     */
    public boolean deleteSession(String sessionId) {
        SQLiteDatabase db = this.getWritableDatabase();
        db.beginTransaction();
        try {
            // 首先删除所有关联的消息
            db.delete(TABLE_MESSAGES, COL_MESSAGE_SESSION_ID + " = ?", new String[]{sessionId});
            // 然后删除会话本身
            int result = db.delete(TABLE_SESSIONS, COL_SESSION_ID + " = ?", new String[]{sessionId});
            db.setTransactionSuccessful();
            return result > 0;
        } finally {
            db.endTransaction();
        }
    }

    /**
     * 格式化日期为字符串
     */
    private String formatDate(Date date) {
        return DATE_FORMAT.format(date);
    }
} 