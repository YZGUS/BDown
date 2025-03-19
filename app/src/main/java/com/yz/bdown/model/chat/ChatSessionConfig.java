package com.yz.bdown.model.chat;

/**
 * 对话会话配置类
 * 用于存储会话的配置信息
 */
public class ChatSessionConfig {
    private String modelName;           // 模型名称
    private int maxContextLength;       // 最大上下文长度
    private float temperature;          // 温度参数
    private int maxTokens;             // 最大生成token数
    private boolean stream;             // 是否启用流式输出

    public ChatSessionConfig() {
        // 设置默认值
        this.maxContextLength = 2048;
        this.temperature = 0.7f;
        this.maxTokens = 1000;
        this.stream = true;
    }

    public String getModelName() {
        return modelName;
    }

    public void setModelName(String modelName) {
        this.modelName = modelName;
    }

    public int getMaxContextLength() {
        return maxContextLength;
    }

    public void setMaxContextLength(int maxContextLength) {
        this.maxContextLength = maxContextLength;
    }

    public float getTemperature() {
        return temperature;
    }

    public void setTemperature(float temperature) {
        this.temperature = temperature;
    }

    public int getMaxTokens() {
        return maxTokens;
    }

    public void setMaxTokens(int maxTokens) {
        this.maxTokens = maxTokens;
    }

    public boolean isStream() {
        return stream;
    }

    public void setStream(boolean stream) {
        this.stream = stream;
    }
} 