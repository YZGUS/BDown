package com.yz.bdown.utils;

import com.yz.bdown.callback.DeepSeekStreamCallback;
import com.yz.bdown.contents.DeepSeekModelEnum;
import com.yz.bdown.model.chat.ChatMessage;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * DeepSeek API测试类
 * 使用线程池版本的DeepSeekUtils进行测试
 */
public class DeepSeekUtilsTest {

    private final String API_KEY = "xxxxx"; // 需要替换为实际的API密钥
    private List<ChatMessage> messageHistory;
    private CountDownLatch latch;
    private DeepSeekStreamCallback callback;

    @Before
    public void setUp() {
        // 初始化聊天历史
        messageHistory = new ArrayList<>();
        callback = new DeepSeekStreamCallback() {
            @Override
            public void onMessage(String reasoningContent, String content) {
                if (content != null) {
                    System.out.println("收到消息: " + content);
                }
                if (reasoningContent != null) {
                    System.out.println("推理内容: " + reasoningContent);
                }
            }

            @Override
            public void onComplete(String fullContent, String fullReasoningContent) {
                System.out.println("请求完成");
                System.out.println("完整内容: " + fullContent);
                System.out.println("完整推理内容: " + fullReasoningContent);
                latch.countDown(); // 释放锁，表示请求完成
            }

            @Override
            public void onError(String errMsg) {
                System.err.println("请求错误: " + errMsg);
                latch.countDown(); // 错误也视为完成
            }
        };
    }

    @After
    public void tearDown() {
        // 关闭线程池
        DeepSeekUtils.shutdown();
    }

    /**
     * 测试非流式API请求
     * 使用deepseek-chat模型发送普通请求
     */
    @Test
    public void testNormalRequest() throws InterruptedException {
        System.out.println("\n======= 开始测试普通请求 =======");

        latch = new CountDownLatch(1);

        // 发送非流式请求
        DeepSeekUtils.sendChatRequestStream(
                API_KEY,
                DeepSeekModelEnum.V3, // 使用非流式模型
                messageHistory,
                callback
        );

        // 等待请求完成
        boolean completed = latch.await(120, TimeUnit.SECONDS);
        System.out.println("请求是否在超时前完成: " + completed);
        System.out.println("======= 普通请求测试结束 =======\n");
    }

    /**
     * 测试流式API请求
     * 使用deepseek-reasoner模型发送流式请求
     */
    @Test
    public void testStreamRequest() throws InterruptedException {
        System.out.println("\n======= 开始测试流式请求 =======");

        latch = new CountDownLatch(1);

        // 发送流式请求
        DeepSeekUtils.sendChatRequestStream(
                API_KEY,
                DeepSeekModelEnum.R1, // 使用流式模型
                messageHistory,
                callback
        );

        // 等待请求完成
        boolean completed = latch.await(120, TimeUnit.SECONDS);
        System.out.println("请求是否在超时前完成: " + completed);
        System.out.println("======= 流式请求测试结束 =======\n");
    }
}