package com.yz.bdown.model.chat;

import java.util.Date;

/**
 * 聊天消息类
 * 用于存储单条对话消息
 */
public class ChatMessage {
    private String id;          // 消息唯一标识
    private String role;        // 消息角色：user 或 assistant
    private String content;     // 消息内容
    private Date timestamp;     // 消息时间戳
    private String sessionId;   // 所属会话ID

    public ChatMessage(String role, String content) {
        this.role = role;
        this.content = content;
        this.timestamp = new Date();
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public Date getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Date timestamp) {
        this.timestamp = timestamp;
    }

    public String getSessionId() {
        return sessionId;
    }

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }
} 