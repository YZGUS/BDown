package com.yz.bdown.model.chat;

/**
 * 聊天场景枚举类
 * 不同场景对应不同的temperature值
 */
public enum ChatScenarioEnum {
    
    CODE_MATH("代码生成/数学解题", 0.0f),
    DATA_ANALYSIS("数据抽取/分析", 1.0f),
    GENERAL_CONVERSATION("通用对话", 1.3f),
    TRANSLATION("翻译", 1.3f),
    CREATIVE_WRITING("创意类写作/诗歌创作", 1.5f);
    
    private final String displayName; // 显示名称
    private final float temperature; // 温度值
    
    ChatScenarioEnum(String displayName, float temperature) {
        this.displayName = displayName;
        this.temperature = temperature;
    }
    
    public String getDisplayName() {
        return displayName;
    }
    
    public float getTemperature() {
        return temperature;
    }
    
    /**
     * 获取所有场景的显示名称数组
     */
    public static String[] getAllDisplayNames() {
        ChatScenarioEnum[] values = values();
        String[] displayNames = new String[values.length];
        for (int i = 0; i < values.length; i++) {
            displayNames[i] = values[i].getDisplayName();
        }
        return displayNames;
    }
    
    /**
     * 根据显示名称获取对应的枚举值
     */
    public static ChatScenarioEnum fromDisplayName(String displayName) {
        for (ChatScenarioEnum scenario : values()) {
            if (scenario.getDisplayName().equals(displayName)) {
                return scenario;
            }
        }
        // 默认返回数据抽取/分析
        return DATA_ANALYSIS;
    }
} 