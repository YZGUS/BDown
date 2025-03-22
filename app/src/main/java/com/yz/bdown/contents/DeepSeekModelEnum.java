package com.yz.bdown.contents;

public enum DeepSeekModelEnum {

    V3("deepseek-chat", false, 64 * 1024, 0, 8 * 1024),
    R1("deepseek-reasoner", true, 64 * 1024, 32 * 1024, 8 * 1024);

    private final String model; // 模型类型
    private final boolean stream; // 是否启动流式响应
    private final int maxContextLength; // 最大上下文长度（字符数）
    private final int maxReasoningLength; // 最大思维链长度（字符数）
    private final int maxOutputLength; // 最大输出长度（字符数）

    DeepSeekModelEnum(String model, boolean stream, int maxContextLength, int maxReasoningLength, int maxOutputLength) {
        this.model = model;
        this.stream = stream;
        this.maxContextLength = maxContextLength;
        this.maxReasoningLength = maxReasoningLength;
        this.maxOutputLength = maxOutputLength;
    }

    public String getModel() {
        return model;
    }

    public boolean isStream() {
        return stream;
    }
    
    public int getMaxContextLength() {
        return maxContextLength;
    }
    
    public int getMaxReasoningLength() {
        return maxReasoningLength;
    }
    
    public int getMaxOutputLength() {
        return maxOutputLength;
    }
}
