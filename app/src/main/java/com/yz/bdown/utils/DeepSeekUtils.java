package com.yz.bdown.utils;

import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static java.util.concurrent.TimeUnit.SECONDS;
import static okhttp3.RequestBody.create;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.yz.bdown.callback.DeepSeekStreamCallback;
import com.yz.bdown.contents.DeepSeekModelEnum;
import com.yz.bdown.model.chat.ChatMessage;

import org.apache.commons.lang3.StringUtils;

import java.io.IOException;
import java.util.List;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
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
            .connectTimeout(120, SECONDS)
            .readTimeout(120, SECONDS)
            .writeTimeout(120, SECONDS)
            .build();

    private static final Handler mainHandler = new Handler(Looper.getMainLooper());

    public static void sendChatRequestStream(String apiKey,
                                             DeepSeekModelEnum model,
                                             List<ChatMessage> messageHistory,
                                             DeepSeekStreamCallback streamCallback) {
        sendChatRequestStream(apiKey, model, messageHistory, 1.0, streamCallback);
    }

    public static void sendChatRequestStream(String apiKey,
                                             DeepSeekModelEnum model,
                                             List<ChatMessage> messageHistory,
                                             double temperature,
                                             DeepSeekStreamCallback streamCallback) {
        try {
            Request request = new Request.Builder()
                    .url(API_URL)
                    .addHeader("Content-Type", "application/json")
                    .addHeader("Authorization", "Bearer " + apiKey)
                    .post(create(buildReqBody(model, messageHistory, temperature), JSON))
                    .build();
            CLIENT.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    Log.e(TAG, "发送请求错误", e);
                    mainHandler.post(() -> streamCallback.onError("请求失败: " + e.getMessage()));
                }

                @Override
                public void onResponse(Call call, Response response) {
                    try {
                        onSuccess(model.isStream(), streamCallback, response);
                    } catch (Throwable t) {
                        Log.e(TAG, "发送请求错误", t);
                        mainHandler.post(() -> streamCallback.onError("处理响应错误: " + t.getMessage()));
                    }
                }
            });
        } catch (Throwable t) {
            Log.e(TAG, "发送请求错误", t);
            mainHandler.post(() -> streamCallback.onError("创建请求错误: " + t.getMessage()));
        }
    }

    private static String buildReqBody(DeepSeekModelEnum model, List<ChatMessage> messageHistory) {
        return buildReqBody(model, messageHistory, 1.0);
    }

    private static String buildReqBody(DeepSeekModelEnum model, List<ChatMessage> messageHistory, double temperature) {
        JSONObject jsonBody = new JSONObject();
        jsonBody.put("model", model.getModel());
        jsonBody.put("stream", model.isStream());
        jsonBody.put("temperature", temperature);

        JSONArray messagesArray = new JSONArray();
        messageHistory.forEach(m -> messagesArray.add(toMessage(m.getRole(), m.getContent())));
        jsonBody.put("messages", messagesArray);
        return jsonBody.toString();
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
            if (stream) {
                mainHandler.post(() -> streamCallback.onError("请求失败 (" + response.code() + "): " + errorBody));
            }
            return;
        }

        ResponseBody responseBody = response.body();
        if (responseBody == null) {
            if (stream) {
                mainHandler.post(() -> streamCallback.onError("响应体为空"));
            }
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
                mainHandler.post(() -> callback.onError("响应内容为空"));
                return;
            }

            String content = JSONObject.parseObject(responseStr)
                    .getJSONArray("choices")
                    .getJSONObject(0)
                    .getJSONObject("message")
                    .getString("content");
            if (isNotBlank(content)) {
                mainHandler.post(() -> callback.onComplete(content, null));
            } else {
                mainHandler.post(() -> callback.onError("解析响应内容失败，内容为空"));
            }
        } catch (Exception e) {
            Log.e(TAG, "处理普通响应错误", e);
            mainHandler.post(() -> callback.onError("处理响应错误: " + e.getMessage()));
        }
    }

    /**
     * 处理流式响应
     */
    private static void processStreamResponse(ResponseBody responseBody, DeepSeekStreamCallback callback) {
        final StringBuilder think = new StringBuilder(), result = new StringBuilder();
        try (responseBody) {
            String line;
            BufferedSource source = responseBody.source();
            while ((line = source.readUtf8Line()) != null) {
                if (!line.startsWith("data: ")) {
                    continue;
                }

                String data = line.substring(6);
                if ("[DONE]".equals(data)) {
                    mainHandler.post(() -> callback.onComplete(
                            result.toString(),
                            think.toString()
                    ));
                    return;
                }
                processStreamData(data, think, result, callback);
            }
        } catch (IOException e) {
            Log.e(TAG, "读取流式响应错误", e);
            if (callback != null) {
                mainHandler.post(() -> callback.onError("读取响应错误: " + e.getMessage()));
            }
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
            JSONObject delta = JSONObject.parseObject(data)
                    .getJSONArray("choices")
                    .getJSONObject(0)
                    .getJSONObject("delta");
            String reasoning = delta.getString("reasoning_content");
            if (isNotBlank(reasoning)) {
                reasoningContent.append(reasoning);
                mainHandler.post(() -> callback.onMessage(reasoning, null));
            }

            String content = delta.getString("content");
            if (isNotBlank(content)) {
                resultContent.append(content);
                mainHandler.post(() -> callback.onMessage(null, content));
            }
        } catch (Exception e) {
            Log.e(TAG, "解析流式数据错误", e);
        }
    }
}