package com.yz.bdown.model.chat;

import java.util.ArrayList;
import java.util.List;

/**
 * 对话历史存储类
 * 用于管理所有对话会话
 */
public class ChatHistory {

    private List<ChatSession> sessions;     // 所有对话会话
    private ChatSession currentSession;      // 当前活动会话

    public ChatHistory() {
        this.sessions = new ArrayList<>();
    }

    public List<ChatSession> getSessions() {
        return sessions;
    }

    public void setSessions(List<ChatSession> sessions) {
        this.sessions = sessions;
    }

    public ChatSession getCurrentSession() {
        return currentSession;
    }

    public void setCurrentSession(ChatSession currentSession) {
        this.currentSession = currentSession;
    }

    public void addSession(ChatSession session) {
        this.sessions.add(session);
    }

    public void removeSession(ChatSession session) {
        this.sessions.remove(session);
    }
} 