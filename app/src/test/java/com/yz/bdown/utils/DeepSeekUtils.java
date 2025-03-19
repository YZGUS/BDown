package com.yz.bdown.utils;

import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static java.util.concurrent.TimeUnit.SECONDS;
import static okhttp3.RequestBody.create;

import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.yz.bdown.callback.DeepSeekStreamCallback;
import com.yz.bdown.contents.DeepSeekModelEnum;
import com.yz.bdown.model.chat.ChatMessage;

import org.apache.commons.lang3.StringUtils;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;
import okio.BufferedSource;

/**
 * DeepSeek API 工具类 - 测试版本
 * 使用Java线程池替代Android Handler
 */
public class DeepSeekUtils {

    private static final String TAG = "DeepSeekUtils";
    private static final String API_URL = "https://api.deepseek.com/chat/completions";
    private static final MediaType JSON = MediaType.parse("application/json; charset=utf-8");

    // 增加超时时间以支持流式输出
    private static final OkHttpClient CLIENT = new OkHttpClient.Builder()
            .connectTimeout(120, SECONDS)
            .readTimeout(120, SECONDS)
            .writeTimeout(120, SECONDS)
            .build();

    // 使用Java线程池替代Android Handler
    private static final ExecutorService executor = Executors.newSingleThreadExecutor();

    public static void sendChatRequestStream(String apiKey,
                                             DeepSeekModelEnum model,
                                             String userPrompt,
                                             List<ChatMessage> messageHistory,
                                             DeepSeekStreamCallback streamCallback) {
        try {
            System.out.println("开始发送请求: 模型 = " + model.getModel() + ", 流式 = " + model.isStream());
            
            Request request = new Request.Builder()
                    .url(API_URL)
                    .addHeader("Content-Type", "application/json")
                    .addHeader("Authorization", "Bearer " + apiKey)
                    .post(create(buildReqBody(model, userPrompt, messageHistory), JSON))
                    .build();
            
            CLIENT.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    System.err.println("发送请求错误: " + e.getMessage());
                    executor.execute(() -> streamCallback.onError("请求失败: " + e.getMessage()));
                }

                @Override
                public void onResponse(Call call, Response response) {
                    try {
                        onSuccess(model.isStream(), streamCallback, response);
                    } catch (Throwable t) {
                        System.err.println("处理响应错误: " + t.getMessage());
                        executor.execute(() -> streamCallback.onError("处理响应错误: " + t.getMessage()));
                    }
                }
            });
        } catch (Throwable t) {
            System.err.println("创建请求错误: " + t.getMessage());
            executor.execute(() -> streamCallback.onError("创建请求错误: " + t.getMessage()));
        }
    }

    private static String buildReqBody(DeepSeekModelEnum model, String userPrompt, List<ChatMessage> messageHistory) {
        JSONObject jsonBody = new JSONObject();
        jsonBody.put("model", model.getModel());
        jsonBody.put("stream", model.isStream());

        JSONArray messagesArray = new JSONArray();
        if (messageHistory != null && !messageHistory.isEmpty()) {
            messageHistory.forEach(m -> messagesArray.add(toMessage(m.getRole(), m.getContent())));
        }
        messagesArray.add(toMessage("user", userPrompt));
        jsonBody.put("messages", messagesArray);
        
        String body = jsonBody.toString();
        System.out.println("请求体: " + body);
        return body;
    }

    private static JSONObject toMessage(String role, String content) {
        JSONObject msgObj = new JSONObject();
        msgObj.put("role", role);
        msgObj.put("content", content);
        return msgObj;
    }

    private static void onSuccess(boolean stream, DeepSeekStreamCallback streamCallback, Response response) throws Throwable {
        if (!response.isSuccessful()) {
            String errorBody = response.body() != null ? response.body().string() : "无响应内容";
            System.err.println("API返回错误: " + response.code() + " - " + errorBody);
            executor.execute(() -> streamCallback.onError("请求失败 (" + response.code() + "): " + errorBody));
            return;
        }

        ResponseBody responseBody = response.body();
        if (responseBody == null) {
            System.err.println("API返回空响应体");
            executor.execute(() -> streamCallback.onError("响应体为空"));
            return;
        }

        if (stream) {
            processStreamResponse(responseBody, streamCallback);
        } else {
            processNormalResponse(responseBody, streamCallback);
        }
    }

    private static void processNormalResponse(ResponseBody responseBody, DeepSeekStreamCallback callback) {
        try (responseBody) {
            String responseStr = responseBody.string();
            if (StringUtils.isBlank(responseStr)) {
                System.err.println("API返回空内容");
                executor.execute(() -> callback.onError("响应内容为空"));
                return;
            }

            System.out.println("收到非流式响应: " + responseStr);
            
            String content = JSONObject.parseObject(responseStr)
                    .getJSONArray("choices")
                    .getJSONObject(0)
                    .getJSONObject("message")
                    .getString("content");
                    
            if (isNotBlank(content)) {
                System.out.println("解析得到内容: " + content);
                executor.execute(() -> callback.onComplete(content, null));
            } else {
                System.err.println("解析响应内容失败，内容为空");
                executor.execute(() -> callback.onError("解析响应内容失败，内容为空"));
            }
        } catch (Exception e) {
            System.err.println("处理普通响应错误: " + e.getMessage());
            executor.execute(() -> callback.onError("处理响应错误: " + e.getMessage()));
        }
    }

    /**
     * 处理流式响应
     */
    private static void processStreamResponse(ResponseBody responseBody, DeepSeekStreamCallback callback) {
        final StringBuilder reasoningContent = new StringBuilder();
        final StringBuilder resultContent = new StringBuilder();
        
        try (responseBody) {
            System.out.println("开始处理流式响应...");
            
            String line;
            BufferedSource source = responseBody.source();
            while ((line = source.readUtf8Line()) != null) {
                // 跳过空行和非数据行
                if (line.isEmpty() || !line.startsWith("data: ")) {
                    continue;
                }

                String data = line.substring(6);
                if ("[DONE]".equals(data)) {
                    System.out.println("收到流式响应结束标记");
                    String finalResult = resultContent.toString();
                    String finalReasoning = reasoningContent.toString();
                    
                    executor.execute(() -> callback.onComplete(finalResult, finalReasoning));
                    return;
                }
                
                processStreamData(data, reasoningContent, resultContent, callback);
            }
        } catch (IOException e) {
            System.err.println("读取流式响应错误: " + e.getMessage());
            executor.execute(() -> callback.onError("读取响应错误: " + e.getMessage()));
        }
    }

    /**
     * 处理流式数据
     */
    private static void processStreamData(String data,
                                          StringBuilder reasoningContent,
                                          StringBuilder resultContent,
                                          DeepSeekStreamCallback callback) {
        try {
            System.out.println("收到流式数据: " + data);
            
            JSONObject delta = JSONObject.parseObject(data)
                    .getJSONArray("choices")
                    .getJSONObject(0)
                    .getJSONObject("delta");
                    
            String reasoning = delta.getString("reasoning_content");
            if (isNotBlank(reasoning)) {
                reasoningContent.append(reasoning);
                System.out.println("流式推理内容: " + reasoning);
                executor.execute(() -> callback.onMessage(reasoning, null));
            }

            String content = delta.getString("content");
            if (isNotBlank(content)) {
                resultContent.append(content);
                System.out.println("流式内容: " + content);
                executor.execute(() -> callback.onMessage(null, content));
            }
        } catch (Exception e) {
            System.err.println("解析流式数据错误: " + e.getMessage());
        }
    }
    
    /**
     * 关闭执行器
     * 在测试完成后应调用此方法
     */
    public static void shutdown() {
        executor.shutdown();
        try {
            if (!executor.awaitTermination(5, SECONDS)) {
                executor.shutdownNow();
            }
        } catch (InterruptedException e) {
            executor.shutdownNow();
        }
    }
} 