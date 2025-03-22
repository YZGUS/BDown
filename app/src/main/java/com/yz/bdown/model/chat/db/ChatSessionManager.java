package com.yz.bdown.model.chat.db;

import android.content.Context;
import android.util.Log;

import com.yz.bdown.model.chat.ChatHistory;
import com.yz.bdown.model.chat.ChatMessage;
import com.yz.bdown.model.chat.ChatSession;
import com.yz.bdown.model.chat.ChatSessionSummary;

import java.util.List;
import java.util.UUID;

/**
 * 聊天会话管理器
 * 用于提供与UI层交互的接口，管理聊天会话的持久化、加载和更新
 */
public class ChatSessionManager {
    private static final String TAG = "ChatSessionManager";
    private static ChatSessionManager instance;
    private ChatDbHelper dbHelper;
    private ChatSession currentSession;
    private String currentSessionId;

    private ChatSessionManager(Context context) {
        dbHelper = ChatDbHelper.getInstance(context);
    }

    public static synchronized ChatSessionManager getInstance(Context context) {
        if (instance == null) {
            instance = new ChatSessionManager(context.getApplicationContext());
        }
        return instance;
    }

    /**
     * 初始化，加载最近的会话或创建新会话
     * @return 当前会话
     */
    public ChatSession initialize() {
        if (currentSession == null) {
            // 尝试加载最近的会话
            currentSession = dbHelper.getMostRecentSession();
            if (currentSession == null) {
                // 如果没有会话，创建一个新的
                currentSession = createNewSession("DeepSeek Chat");
            }
            currentSessionId = currentSession.getId();
        }
        return currentSession;
    }

    /**
     * 获取当前会话的所有消息
     * @return 当前会话的消息列表
     */
    public List<ChatMessage> getCurrentMessages() {
        initialize();
        return currentSession.getMessages();
    }

    /**
     * 创建并切换到新会话
     * @param modelName 模型名称
     * @return 新创建的会话
     */
    public ChatSession createNewSession(String modelName) {
        // 如果当前有会话，先保存
        if (currentSession != null) {
            saveCurrentSession();
        }

        // 创建新会话
        currentSession = new ChatSession();
        currentSession.setId(UUID.randomUUID().toString());
        currentSession.setModelName(modelName);
        currentSessionId = currentSession.getId();

        // 将新会话插入数据库
        dbHelper.insertSession(currentSession);

        return currentSession;
    }

    /**
     * 添加消息到当前会话
     * @param message 要添加的消息
     * @return 消息ID
     */
    public String addMessage(ChatMessage message) {
        initialize();
        
        // 设置会话ID
        message.setSessionId(currentSessionId);
        
        // 添加到内存中的会话
        currentSession.addMessage(message);
        
        // 添加到数据库
        return dbHelper.insertMessage(message);
    }

    /**
     * 保存当前会话
     */
    public void saveCurrentSession() {
        if (currentSession != null) {
            // 更新数据库中的会话信息
            dbHelper.insertSession(currentSession);
        }
    }

    /**
     * 切换到指定的会话
     * @param sessionId 会话ID
     * @return 切换后的会话
     */
    public ChatSession switchSession(String sessionId) {
        // 保存当前会话
        if (currentSession != null) {
            saveCurrentSession();
        }

        // 加载新会话
        currentSession = dbHelper.getSession(sessionId);
        if (currentSession != null) {
            currentSessionId = sessionId;
            return currentSession;
        } else {
            Log.e(TAG, "Failed to switch to session: " + sessionId);
            return null;
        }
    }

    /**
     * 获取所有会话的摘要信息
     * @return 会话摘要列表
     */
    public List<ChatSessionSummary> getAllSessionSummaries() {
        return dbHelper.getSessionSummaries();
    }

    /**
     * 获取当前会话
     * @return 当前会话
     */
    public ChatSession getCurrentSession() {
        return initialize();
    }

    /**
     * 删除指定的会话
     * @param sessionId 要删除的会话ID
     * @return 是否删除成功
     */
    public boolean deleteSession(String sessionId) {
        // 如果要删除的是当前会话，需要先切换到其他会话
        if (sessionId.equals(currentSessionId)) {
            // 查找所有会话摘要
            List<ChatSessionSummary> summaries = dbHelper.getSessionSummaries();
            String nextSessionId = null;
            
            // 寻找另一个可以切换的会话
            for (ChatSessionSummary summary : summaries) {
                if (!summary.getSessionId().equals(sessionId)) {
                    nextSessionId = summary.getSessionId();
                    break;
                }
            }
            
            // 如果找到了可切换的会话，先切换
            if (nextSessionId != null) {
                switchSession(nextSessionId);
            } else {
                // 如果没有其他会话，创建一个新的
                createNewSession("DeepSeek Chat");
            }
        }
        
        // 删除会话
        return dbHelper.deleteSession(sessionId);
    }
} 