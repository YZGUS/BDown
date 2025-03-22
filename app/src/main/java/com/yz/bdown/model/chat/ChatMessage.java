package com.yz.bdown.model.chat;

import java.util.Date;

/**
 * 聊天消息类
 * 用于存储单条对话消息
 */
public class ChatMessage {
    public static final String ROLE_USER = "user";
    public static final String ROLE_ASSISTANT = "assistant";
    private String id;          // 消息唯一标识
    private String role;        // 消息角色：user 或 assistant
    private String content;     // 消息内容
    private String reasoning;   // 思考/推理内容
    private Date timestamp;     // 消息时间戳
    private String sessionId;   // 所属会话ID
    private boolean inProgress; // 是否正在处理中

    public ChatMessage(String role, String content) {
        this.role = role;
        this.content = content;
        this.reasoning = "";    // 初始化推理内容为空字符串
        this.timestamp = new Date();
        this.inProgress = false;
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

    public String getReasoning() {
        return reasoning;
    }

    public void setReasoning(String reasoning) {
        this.reasoning = reasoning;
    }

    /**
     * 追加推理内容
     *
     * @param newReasoning 要追加的推理内容
     */
    public void appendReasoning(String newReasoning) {
        if (newReasoning != null && !newReasoning.isEmpty()) {
            this.reasoning += newReasoning;
        }
    }

    /**
     * 获取格式化后的推理内容，用于在UI中展示
     *
     * @return 格式化为Markdown块引用的推理内容
     */
    public String getFormattedReasoning() {
        if (reasoning == null || reasoning.isEmpty()) {
            return "";
        }

        // 将思考内容格式化为单个引用块
        // 替换所有换行符为换行+>空格，确保多行内容在一个引用块内
        String formatted = reasoning
                .replaceAll("\n", "\n> ")
                .trim();

        // 添加引用块标记并确保最后有两个换行符
        return "> " + formatted + "\n\n";
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

    public boolean isInProgress() {
        return inProgress;
    }

    public void setInProgress(boolean inProgress) {
        this.inProgress = inProgress;
    }
} 