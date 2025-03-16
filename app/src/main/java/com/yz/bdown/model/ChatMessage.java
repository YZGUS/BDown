package com.yz.bdown.model;

/**
 * 聊天消息模型
 */
public class ChatMessage {
    private String role; // "user" 或 "assistant"
    private String content;

    public ChatMessage(String role, String content) {
        this.role = role;
        this.content = content;
    }

    public String getRole() {
        return role;
    }

    public String getContent() {
        return content;
    }
} 