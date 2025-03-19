package com.yz.bdown.contents;

public enum DeepSeekModelEnum {

    V3("deepseek-chat", false),
    R1("deepseek-reasoner", true);

    private final String model; // 模型类型

    private final boolean stream; // 是否启动流式响应

    DeepSeekModelEnum(String model, boolean stream) {
        this.model = model;
        this.stream = stream;
    }

    public String getModel() {
        return model;
    }

    public boolean isStream() {
        return stream;
    }
}
