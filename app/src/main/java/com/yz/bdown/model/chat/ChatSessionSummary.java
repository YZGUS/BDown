package com.yz.bdown.model.chat;

import java.util.Date;

/**
 * 对话会话摘要类
 * 用于在会话列表中显示会话概要信息
 */
public class ChatSessionSummary {
    private String sessionId;           // 会话ID
    private String name;                // 会话名称
    private String modelName;           // 使用的模型
    private int messageCount;           // 消息数量
    private Date lastUpdateTime;        // 最后更新时间
    private String lastMessage;         // 最后一条消息预览

    public String getSessionId() {
        return sessionId;
    }

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getModelName() {
        return modelName;
    }

    public void setModelName(String modelName) {
        this.modelName = modelName;
    }

    public int getMessageCount() {
        return messageCount;
    }

    public void setMessageCount(int messageCount) {
        this.messageCount = messageCount;
    }

    public Date getLastUpdateTime() {
        return lastUpdateTime;
    }

    public void setLastUpdateTime(Date lastUpdateTime) {
        this.lastUpdateTime = lastUpdateTime;
    }

    public String getLastMessage() {
        return lastMessage;
    }

    public void setLastMessage(String lastMessage) {
        this.lastMessage = lastMessage;
    }
} 