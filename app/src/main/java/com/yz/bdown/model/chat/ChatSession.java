package com.yz.bdown.model.chat;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * 对话会话类
 * 用于管理单个对话会话，包含会话信息和消息历史
 */
public class ChatSession {
    private String id;              // 会话唯一标识
    private String name;            // 会话名称
    private String modelName;       // 使用的模型名称
    private Date createTime;        // 创建时间
    private Date lastUpdateTime;    // 最后更新时间
    private List<ChatMessage> messages; // 消息历史

    public ChatSession() {
        this.messages = new ArrayList<>();
        this.createTime = new Date();
        this.lastUpdateTime = new Date();
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
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

    public Date getCreateTime() {
        return createTime;
    }

    public void setCreateTime(Date createTime) {
        this.createTime = createTime;
    }

    public Date getLastUpdateTime() {
        return lastUpdateTime;
    }

    public void setLastUpdateTime(Date lastUpdateTime) {
        this.lastUpdateTime = lastUpdateTime;
    }

    public List<ChatMessage> getMessages() {
        return messages;
    }

    public void setMessages(List<ChatMessage> messages) {
        this.messages = messages;
    }

    public void addMessage(ChatMessage message) {
        this.messages.add(message);
        this.lastUpdateTime = new Date();
    }
} 