package com.yz.bdown.fragment.deepseek;

import static com.yz.bdown.contents.DeepSeekModelEnum.R1;
import static com.yz.bdown.contents.DeepSeekModelEnum.V3;

import android.app.AlertDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Spinner;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;
import com.yz.bdown.R;
import com.yz.bdown.adapter.ChatMessageAdapter;
import com.yz.bdown.callback.DeepSeekStreamCallback;
import com.yz.bdown.contents.DeepSeekModelEnum;
import com.yz.bdown.model.chat.ChatMessage;
import com.yz.bdown.utils.DeepSeekUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import io.noties.markwon.Markwon;
import io.noties.markwon.ext.strikethrough.StrikethroughPlugin;
import io.noties.markwon.ext.tables.TablePlugin;
import io.noties.markwon.ext.tasklist.TaskListPlugin;
import io.noties.markwon.html.HtmlPlugin;
import io.noties.markwon.image.glide.GlideImagesPlugin;

/**
 * DeepSeek 交互 Fragment
 */
public class DeepSeekFragment extends Fragment {

    private static final String PREFS_NAME = "DeepSeekPrefs";
    private static final String KEY_API_KEY = "api_key";
    private static final String TAG = "DeepSeekFragment";

    private EditText promptInput;
    private Button sendButton;
    private RecyclerView chatRecyclerView;
    private ProgressBar progressBar;
    private FloatingActionButton settingsButton;
    private Spinner modelSelector;
    private Handler mainHandler;

    // 当前API调用，用于取消请求
    private okhttp3.Call currentCall;
    private AtomicBoolean isCancelled = new AtomicBoolean(false);

    private final String[] MODEL_OPTIONS = {"deepseek-chat", "deepseek-reasoner"};
    private String currentModel = MODEL_OPTIONS[0];

    private List<ChatMessage> messageHistory = new ArrayList<>();
    private ChatMessageAdapter chatAdapter;

    // Markwon 实例，用于渲染 Markdown
    private Markwon markwon;

    // 请求状态
    private boolean isRequestInProgress = false;
    private int consecutiveFailures = 0;
    private static final int MAX_FAILURES = 3;
    private int currentAssistantMessageIndex = -1;

    // 在 DeepSeekFragment 类中添加成员变量用于累积当前提问的思考内容
    private StringBuilder currentReasoningContent = new StringBuilder();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_deepseek, container, false);

        // 初始化 Markwon
        initMarkwon();

        // 初始化视图
        initViews(view);

        // 设置模型选择器
        setupModelSelector();

        // 设置事件监听
        setupListeners();

        return view;
    }

    /**
     * 初始化 Markwon 库
     */
    private void initMarkwon() {
        // 配置 Markwon 实例，使用多个插件以支持丰富的 Markdown 功能
        markwon = Markwon.builder(requireContext())
                .usePlugin(StrikethroughPlugin.create())
                .usePlugin(TablePlugin.create(requireContext()))
                .usePlugin(TaskListPlugin.create(requireContext()))
                .usePlugin(HtmlPlugin.create())
                .usePlugin(GlideImagesPlugin.create(requireContext()))
                .build();
    }

    /**
     * 初始化视图组件
     */
    private void initViews(View view) {
        promptInput = view.findViewById(R.id.deepseek_prompt);
        sendButton = view.findViewById(R.id.deepseek_send_button);
        chatRecyclerView = view.findViewById(R.id.deepseek_chat_recycler_view);
        progressBar = view.findViewById(R.id.deepseek_progress);
        settingsButton = view.findViewById(R.id.deepseek_settings_button);
        modelSelector = view.findViewById(R.id.model_selector);
        mainHandler = new Handler(Looper.getMainLooper());

        // 设置RecyclerView
        chatAdapter = new ChatMessageAdapter(requireContext(), messageHistory, markwon);
        chatRecyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        chatRecyclerView.setAdapter(chatAdapter);

        // 隐藏最初的进度条，放在回答旁边的进度条会通过 ChatMessageAdapter 显示
        progressBar.setVisibility(View.GONE);
    }

    /**
     * 设置模型选择器
     */
    private void setupModelSelector() {
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                requireContext(),
                android.R.layout.simple_spinner_item,
                MODEL_OPTIONS
        );
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        modelSelector.setAdapter(adapter);

        // 加载保存的模型选择
        String savedModel = getSelectedModel();
        for (int i = 0; i < MODEL_OPTIONS.length; i++) {
            if (MODEL_OPTIONS[i].equals(savedModel)) {
                modelSelector.setSelection(i);
                currentModel = savedModel;
                break;
            }
        }

        modelSelector.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                currentModel = MODEL_OPTIONS[position];
                saveSelectedModel(currentModel);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                // 什么也不做
            }
        });
    }

    /**
     * 设置事件监听器
     */
    private void setupListeners() {
        sendButton.setOnClickListener(v -> {
            if (isRequestInProgress) {
                // 如果请求正在进行中，则中止请求
                cancelRequest();
            } else {
                // 否则，发送新请求
                String prompt = promptInput.getText().toString().trim();

                if (TextUtils.isEmpty(prompt)) {
                    showError("请输入提示内容");
                    return;
                }

                String apiKey = getApiKey();
                if (TextUtils.isEmpty(apiKey)) {
                    showApiKeyDialog();
                    return;
                }

                // 添加用户消息到聊天记录
                addMessage("user", prompt);

                // 清空输入框
                promptInput.setText("");

                // 开始请求
                startRequest(prompt);
            }
        });

        settingsButton.setOnClickListener(v -> showApiKeyDialog());
    }

    /**
     * 开始请求
     */
    private void startRequest(String userPrompt) {
        isRequestInProgress = true;
        isCancelled.set(false);
        updateUIForRequestInProgress(true);

        // 添加一个空的助手消息，并记录索引，用于更新显示
        currentAssistantMessageIndex = messageHistory.size();
        ChatMessage assistantMessage = new ChatMessage("assistant", "");
        assistantMessage.setInProgress(true);
        // 确保初始化reasoning为空
        assistantMessage.setReasoning("");

        messageHistory.add(assistantMessage);
        chatAdapter.notifyItemInserted(messageHistory.size() - 1);
        chatRecyclerView.smoothScrollToPosition(messageHistory.size() - 1);

        // 确定使用哪个模型
        DeepSeekModelEnum model = R1.getModel().equals(currentModel) ? R1 : V3;

        // 调用API
        String apiKey = getApiKey();

        // 使用DeepSeekUtils发送请求
        DeepSeekUtils.sendChatRequestStream(apiKey, model,
                messageHistory.subList(0, messageHistory.size() - 1), // 不包括刚添加的空消息
                new DeepSeekStreamCallback() {
                    @Override
                    public void onMessage(String reasoningContent, String content) {
                        if (isCancelled.get()) return;

                        // 处理思考过程 (reasoningContent)
                        if (reasoningContent != null && !reasoningContent.isEmpty()) {
                            // 使用新方法更新思考内容
                            updateAssistantReasoningContent(reasoningContent);
                        }

                        // 处理响应内容 (content)
                        if (content != null) {
                            // 更新界面显示的内容
                            updateAssistantMessageContent(content);
                        }
                    }

                    @Override
                    public void onComplete(String fullContent, String fullReasoningContent) {
                        if (isCancelled.get()) return;

                        // 请求成功完成
                        handleRequestSuccess(fullContent);
                    }

                    @Override
                    public void onError(String errMsg) {
                        if (isCancelled.get()) return;

                        // 请求失败
                        handleRequestFailure(errMsg);
                    }
                });
    }

    /**
     * 更新助手消息的思考内容
     */
    private void updateAssistantReasoningContent(String newReasoning) {
        if (currentAssistantMessageIndex >= 0 && currentAssistantMessageIndex < messageHistory.size()) {
            ChatMessage message = messageHistory.get(currentAssistantMessageIndex);

            // 追加新的思考内容
            message.appendReasoning(newReasoning);
            message.setContent(message.getFormattedReasoning());

            // 更新UI
            mainHandler.post(() -> chatAdapter.notifyItemChanged(currentAssistantMessageIndex));
        }
    }

    /**
     * 更新助手消息内容（流式响应时使用）
     */
    private void updateAssistantMessageContent(String newContent) {
        if (currentAssistantMessageIndex >= 0 && currentAssistantMessageIndex < messageHistory.size()) {
            ChatMessage message = messageHistory.get(currentAssistantMessageIndex);

            // 追加内容
            String currentContent = message.getContent();
            message.setContent(currentContent + newContent);

            mainHandler.post(() -> chatAdapter.notifyItemChanged(currentAssistantMessageIndex));
        }
    }

    /**
     * 取消请求
     */
    private void cancelRequest() {
        // 标记已取消
        isCancelled.set(true);

        // 结束请求状态
        isRequestInProgress = false;
        updateUIForRequestInProgress(false);

        // 如果有当前助手消息，将其内容更新为 "已取消"
        if (currentAssistantMessageIndex >= 0 && currentAssistantMessageIndex < messageHistory.size()) {
            ChatMessage message = messageHistory.get(currentAssistantMessageIndex);
            message.setContent("*请求已取消*");
            message.setInProgress(false);
            chatAdapter.notifyItemChanged(currentAssistantMessageIndex);
        }

        currentAssistantMessageIndex = -1;
    }

    /**
     * 请求失败处理
     */
    private void handleRequestFailure(String errorMessage) {
        isRequestInProgress = false;
        updateUIForRequestInProgress(false);

        consecutiveFailures++;

        // 更新当前消息内容为错误信息
        if (currentAssistantMessageIndex >= 0 && currentAssistantMessageIndex < messageHistory.size()) {
            ChatMessage message = messageHistory.get(currentAssistantMessageIndex);
            message.setContent("**错误：** " + errorMessage);
            message.setInProgress(false);
            chatAdapter.notifyItemChanged(currentAssistantMessageIndex);
        }

        // 如果连续失败次数达到阈值，从上下文中移除本次问题
        if (consecutiveFailures >= MAX_FAILURES) {
            // 实际实现中，这里会从上下文中移除最近的用户问题
            showError("连续请求失败，已从上下文中移除本次问题");
            // 移除最后两条消息（用户问题和失败回答）
            if (messageHistory.size() >= 2) {
                messageHistory.remove(messageHistory.size() - 1);
                messageHistory.remove(messageHistory.size() - 1);
                chatAdapter.notifyDataSetChanged();
            }
            consecutiveFailures = 0;
        }

        currentAssistantMessageIndex = -1;
    }

    /**
     * 请求成功处理
     */
    private void handleRequestSuccess(String content) {
        isRequestInProgress = false;
        updateUIForRequestInProgress(false);
        consecutiveFailures = 0;

        // 更新当前消息内容
        if (currentAssistantMessageIndex >= 0 && currentAssistantMessageIndex < messageHistory.size()) {
            ChatMessage message = messageHistory.get(currentAssistantMessageIndex);
            message.setContent(content);
            message.setInProgress(false);
            chatAdapter.notifyItemChanged(currentAssistantMessageIndex);
        }

        currentAssistantMessageIndex = -1;
    }

    /**
     * 更新 UI 以反映请求状态
     */
    private void updateUIForRequestInProgress(boolean inProgress) {
        if (inProgress) {
            // 请求进行中
            sendButton.setText(R.string.cancel_button);
            sendButton.setBackgroundResource(R.drawable.cancel_button_background);
            promptInput.setEnabled(false);
            modelSelector.setEnabled(false);
        } else {
            // 请求已完成或已取消
            sendButton.setText(R.string.send_button);
            sendButton.setBackgroundResource(R.drawable.send_button_background);
            promptInput.setEnabled(true);
            modelSelector.setEnabled(true);
        }
    }

    /**
     * 获取保存的API密钥
     */
    private String getApiKey() {
        SharedPreferences prefs = requireActivity().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        return prefs.getString(KEY_API_KEY, "");
    }

    /**
     * 显示API密钥配置对话框
     */
    private void showApiKeyDialog() {
        View dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_api_key, null);
        EditText apiKeyInput = dialogView.findViewById(R.id.dialog_api_key_input);

        // 设置已保存的API密钥（如果有）
        String savedApiKey = getApiKey();
        if (!TextUtils.isEmpty(savedApiKey)) {
            apiKeyInput.setText(savedApiKey);
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext())
                .setTitle("配置 DeepSeek API 密钥")
                .setView(dialogView)
                .setPositiveButton("保存", (dialog, which) -> {
                    String apiKey = apiKeyInput.getText().toString().trim();
                    if (!TextUtils.isEmpty(apiKey)) {
                        saveApiKey(apiKey);
                        Snackbar.make(requireView(), "API 密钥已保存", Snackbar.LENGTH_SHORT).show();
                    } else {
                        showError("API 密钥不能为空");
                    }
                })
                .setNegativeButton("取消", null)
                .setNeutralButton("重置", (dialog, which) -> {
                    // 显示确认对话框
                    new AlertDialog.Builder(requireContext())
                            .setTitle("确认重置")
                            .setMessage("确定要重置 API 密钥吗？这将删除您当前保存的密钥。")
                            .setPositiveButton("确定", (dialogInterface, i) -> {
                                resetApiKey();
                                Snackbar.make(requireView(), "API 密钥已重置", Snackbar.LENGTH_SHORT).show();
                            })
                            .setNegativeButton("取消", null)
                            .show();
                });

        builder.show();
    }

    /**
     * 添加消息到聊天记录
     */
    private void addMessage(String role, String content) {
        ChatMessage message = new ChatMessage(role, content);
        message.setInProgress(isRequestInProgress && "assistant".equals(role));
        messageHistory.add(message);
        chatAdapter.notifyItemInserted(messageHistory.size() - 1);
        // 滚动到最新消息
        chatRecyclerView.smoothScrollToPosition(messageHistory.size() - 1);
    }

    /**
     * 保存API密钥
     */
    private void saveApiKey(String apiKey) {
        SharedPreferences prefs = requireActivity().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        prefs.edit().putString(KEY_API_KEY, apiKey).apply();
    }

    /**
     * 重置API密钥
     */
    private void resetApiKey() {
        SharedPreferences prefs = requireActivity().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        prefs.edit().remove(KEY_API_KEY).apply();
    }

    /**
     * 获取保存的模型选择
     */
    private String getSelectedModel() {
        SharedPreferences prefs = requireActivity().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        return prefs.getString("selected_model", MODEL_OPTIONS[0]);
    }

    /**
     * 保存模型选择
     */
    private void saveSelectedModel(String model) {
        SharedPreferences prefs = requireActivity().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        prefs.edit().putString("selected_model", model).apply();
    }

    /**
     * 显示错误信息
     */
    private void showError(String message) {
        Snackbar.make(requireView(), message, Snackbar.LENGTH_SHORT).show();
    }
} 