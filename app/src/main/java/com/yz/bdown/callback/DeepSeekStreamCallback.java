package com.yz.bdown.callback;

/**
 * DeepSeek 流式回调接口
 */
public interface DeepSeekStreamCallback {

    /**
     * 接收到新的单个 message
     *
     * @param content          新的内容文本（可能为null）
     * @param reasoningContent 新的推理内容（可能为null）
     */
    void onMessage(String content, String reasoningContent);

    /**
     * 流式请求完成
     *
     * @param fullContent          完整的响应内容
     * @param fullReasoningContent 完整的推理内容（仅deepseek-reasoner模型有值）
     */
    void onComplete(String fullContent, String fullReasoningContent);

    /**
     * 请求失败
     *
     * @param errMsg 错误信息
     */
    void onError(String errMsg);
}