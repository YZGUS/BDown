package com.yz.bdown.utils;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import org.apache.commons.lang3.StringUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.ResponseBody;
import okio.BufferedSource;

/**
 * DeepSeek API 工具类
 * 封装对 DeepSeek API 的调用，支持流式输出
 */
public class DeepSeekUtils {

    private static final String TAG = "DeepSeekUtils";
    private static final String API_URL = "https://api.deepseek.com/chat/completions";
    private static final MediaType JSON = MediaType.parse("application/json; charset=utf-8");

    // 增加超时时间以支持流式输出
    private static final OkHttpClient CLIENT = new OkHttpClient.Builder()
            .connectTimeout(120, TimeUnit.SECONDS)
            .readTimeout(120, TimeUnit.SECONDS)
            .writeTimeout(120, TimeUnit.SECONDS)
            .build();

    private static final Handler mainHandler = new Handler(Looper.getMainLooper());

    /**
     * 发送聊天请求（支持流式输出和多轮对话）
     *
     * @param apiKey         DeepSeek API 密钥
     * @param modelName      模型名称
     * @param userPrompt     用户输入的提示
     * @param messageHistory 消息历史记录
     * @param streamCallback 流式回调
     */
    public static void sendChatRequestStream(String apiKey, String modelName, String userPrompt,
                                             List<ChatMessage> messageHistory,
                                             DeepSeekStreamCallback streamCallback) {
        try {
            JSONObject jsonBody = buildRequestBody(modelName, userPrompt, messageHistory, true);

            RequestBody body = RequestBody.create(jsonBody.toString(), JSON);
            Request request = new Request.Builder()
                    .url(API_URL)
                    .addHeader("Content-Type", "application/json")
                    .addHeader("Authorization", "Bearer " + apiKey)
                    .post(body)
                    .build();

            // 通知开始
            if (streamCallback != null) {
                mainHandler.post(streamCallback::onStart);
            }

            // 使用异步请求处理流式响应
            CLIENT.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    if (streamCallback != null) {
                        mainHandler.post(() -> streamCallback.onError("请求失败: " + e.getMessage()));
                    }
                }

                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    if (!response.isSuccessful()) {
                        String errorBody = response.body() != null ? response.body().string() : "无响应内容";
                        if (streamCallback != null) {
                            mainHandler.post(() -> streamCallback.onError("请求失败 (" + response.code() + "): " + errorBody));
                        }
                        return;
                    }

                    ResponseBody responseBody = response.body();
                    if (responseBody == null) {
                        if (streamCallback != null) {
                            mainHandler.post(() -> streamCallback.onError("响应体为空"));
                        }
                        return;
                    }

                    // 处理流式响应
                    processStreamResponse(responseBody, streamCallback);
                }
            });
        } catch (Exception e) {
            Log.e(TAG, "发送请求错误", e);
            if (streamCallback != null) {
                mainHandler.post(() -> streamCallback.onError("发送请求错误: " + e.getMessage()));
            }
        }
    }

    /**
     * 处理流式响应
     */
    private static void processStreamResponse(ResponseBody responseBody, DeepSeekStreamCallback callback) {
        StringBuilder fullContent = new StringBuilder();

        try {
            BufferedSource source = responseBody.source();
            while (!source.exhausted()) {
                // 读取一行数据（流式响应的每个数据块）
                String line = source.readUtf8Line();
                if (line == null) {
                    break;
                }

                if (line.isEmpty()) {
                    continue;
                }

                // 如果行以 "data: " 开头，处理 JSON 数据
                if (line.startsWith("data: ")) {
                    String data = line.substring(6);

                    // 检查是否是完成的标记
                    if ("[DONE]".equals(data)) {
                        if (callback != null) {
                            String finalContent = fullContent.toString();
                            mainHandler.post(() -> callback.onComplete(finalContent));
                        }
                        continue;
                    }

                    try {
                        JSONObject jsonObject = new JSONObject(data);
                        JSONArray choices = jsonObject.getJSONArray("choices");
                        JSONObject choice = choices.getJSONObject(0);
                        JSONObject delta = choice.getJSONObject("delta");

                        if (delta.has("content")) {
                            String content = delta.getString("content");
                            if (StringUtils.isBlank(content)) {
                                continue;
                            }

                            fullContent.append(content);
                            if (callback != null) {
                                final String currentContent = fullContent.toString();
                                mainHandler.post(() -> callback.onToken(content, currentContent));
                            }
                        } else if (delta.has("reasoning_content")) {
                            String content = delta.getString("reasoning_content");
                            if (StringUtils.isBlank(content)) {
                                continue;
                            }

                            fullContent.append(content);
                            if (callback != null) {
                                final String currentContent = fullContent.toString();
                                mainHandler.post(() -> callback.onToken(content, currentContent));
                            }
                        }
                    } catch (JSONException e) {
                        Log.e(TAG, "JSON解析错误", e);
                    }
                }
            }
        } catch (IOException e) {
            Log.e(TAG, "读取流式响应错误", e);
            if (callback != null) {
                mainHandler.post(() -> callback.onError("读取响应错误: " + e.getMessage()));
            }
        } finally {
            responseBody.close();
            if (fullContent.length() > 0 && callback != null) {
                String finalContent = fullContent.toString();
                mainHandler.post(() -> callback.onComplete(finalContent));
            }
        }
    }

    /**
     * 构建请求体
     */
    private static JSONObject buildRequestBody(String modelName, String userPrompt,
                                               List<ChatMessage> messageHistory, boolean stream)
            throws JSONException {
        JSONObject jsonBody = new JSONObject();
        jsonBody.put("model", modelName);
        jsonBody.put("stream", stream);

        JSONArray messagesArray = new JSONArray();
        // 添加历史消息
        if (messageHistory != null && !messageHistory.isEmpty()) {
            for (ChatMessage message : messageHistory) {
                JSONObject msgObj = new JSONObject();
                msgObj.put("role", message.getRole());
                msgObj.put("content", message.getContent());
                messagesArray.put(msgObj);
            }
        }

        // 添加当前用户消息
        JSONObject userMessage = new JSONObject();
        userMessage.put("role", "user");
        userMessage.put("content", userPrompt);
        messagesArray.put(userMessage);

        jsonBody.put("messages", messagesArray);

        return jsonBody;
    }

    /**
     * 聊天消息类
     */
    public static class ChatMessage {
        private String role;
        private String content;

        public ChatMessage(String role, String content) {
            this.role = role;
            this.content = content;
        }

        public String getRole() {
            return role;
        }

        public String getContent() {
            return content;
        }
    }

    /**
     * DeepSeek 流式回调接口
     */
    public interface DeepSeekStreamCallback {
        /**
         * 开始流式请求
         */
        void onStart();

        /**
         * 接收到新的单个 token
         *
         * @param token          新 token 文本
         * @param currentContent 当前累积的完整内容
         */
        void onToken(String token, String currentContent);

        /**
         * 流式请求完成
         *
         * @param fullContent 完整的响应内容
         */
        void onComplete(String fullContent);

        /**
         * 请求失败
         *
         * @param errorMessage 错误信息
         */
        void onError(String errorMessage);
    }
} 