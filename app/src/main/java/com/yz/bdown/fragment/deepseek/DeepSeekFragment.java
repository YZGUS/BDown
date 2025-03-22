package com.yz.bdown.fragment.deepseek;

import static com.yz.bdown.contents.DeepSeekModelEnum.R1;
import static com.yz.bdown.contents.DeepSeekModelEnum.V3;
import static com.yz.bdown.model.chat.ChatMessage.ROLE_ASSISTANT;
import static com.yz.bdown.model.chat.ChatMessage.ROLE_USER;

import android.app.AlertDialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.Toast;

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
import com.yz.bdown.model.chat.ChatScenarioEnum;
import com.yz.bdown.utils.DeepSeekUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;

import io.noties.markwon.Markwon;
import io.noties.markwon.core.CorePlugin;
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
    private static final String KEY_SCENARIO = "scenario";

    private EditText promptInput;
    private Button sendButton;
    private RecyclerView chatRecyclerView;
    private ProgressBar progressBar;
    private FloatingActionButton settingsButton;
    private FloatingActionButton newChatButton;
    private FloatingActionButton historyDrawingButton;
    private RadioGroup modelSelector;
    private RadioButton v3RadioButton;
    private RadioButton r1RadioButton;
    private Spinner scenarioSpinner;
    private Handler mainHandler;

    // 当前API调用，用于取消请求
    private AtomicBoolean isCancelled = new AtomicBoolean(false);

    private String currentModel = R1.getModel();
    private ChatScenarioEnum currentScenario = ChatScenarioEnum.DATA_ANALYSIS;

    private List<ChatMessage> messageHistory = new ArrayList<>();
    private ChatMessageAdapter chatAdapter;

    // Markwon 实例，用于渲染 Markdown
    private Markwon markwon;

    // 请求状态
    private boolean isRequestInProgress = false;
    private int consecutiveFailures = 0;
    private static final int MAX_FAILURES = 3;
    private int curAssistantMessageIndex = -1;

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

        // 设置场景选择器
        setupScenarioSelector();

        // 设置事件监听
        setupListeners();

        // 设置双击复制功能
        setupCopyOnDoubleTap();

        return view;
    }

    /**
     * 初始化 Markwon 库
     */
    private void initMarkwon() {
        // 配置 Markwon 实例，使用多个插件以支持丰富的 Markdown 功能
        markwon = Markwon.builder(requireContext())
                .usePlugin(CorePlugin.create())
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
        newChatButton = view.findViewById(R.id.new_chat_button);
        historyDrawingButton = view.findViewById(R.id.history_drawing_button);
        modelSelector = view.findViewById(R.id.model_selector);
        v3RadioButton = view.findViewById(R.id.model_v3);
        r1RadioButton = view.findViewById(R.id.model_r1);
        scenarioSpinner = view.findViewById(R.id.scenario_selector);
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
        // 加载保存的模型选择
        String savedModel = getSelectedModel();

        // 根据保存的模型设置选中状态
        if (R1.getModel().equals(savedModel)) {
            r1RadioButton.setChecked(true);
            currentModel = R1.getModel();
        } else {
            v3RadioButton.setChecked(true);
            currentModel = V3.getModel();
        }

        // 设置RadioGroup监听器
        modelSelector.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.model_v3) {
                currentModel = V3.getModel();
            } else if (checkedId == R.id.model_r1) {
                currentModel = R1.getModel();
            }
            saveSelectedModel(currentModel);
        });
    }

    /**
     * 设置场景选择器
     */
    private void setupScenarioSelector() {
        // 创建适配器
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                requireContext(),
                android.R.layout.simple_spinner_item,
                ChatScenarioEnum.getAllDisplayNames()
        );
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        scenarioSpinner.setAdapter(adapter);

        // 设置默认选择
        String savedScenario = getSelectedScenario();
        currentScenario = ChatScenarioEnum.fromDisplayName(savedScenario);

        // 设置选择项
        for (int i = 0; i < adapter.getCount(); i++) {
            if (Objects.equals(adapter.getItem(i), currentScenario.getDisplayName())) {
                scenarioSpinner.setSelection(i);
                break;
            }
        }

        // 设置选择监听器
        scenarioSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String selected = parent.getItemAtPosition(position).toString();
                currentScenario = ChatScenarioEnum.fromDisplayName(selected);
                saveSelectedScenario(selected);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                // 不做任何处理
            }
        });
    }

    /**
     * 设置双击复制功能
     */
    private void setupCopyOnDoubleTap() {
        chatAdapter.setOnItemDoubleClickListener(message -> {
            if (message != null && !TextUtils.isEmpty(message.getContent())) {
                copyToClipboard(message.getContent());
                Toast.makeText(requireContext(), "文本已复制到剪贴板", Toast.LENGTH_SHORT).show();
            }
        });
    }

    /**
     * 复制文本到剪贴板
     */
    private void copyToClipboard(String text) {
        ClipboardManager clipboard = (ClipboardManager) requireContext().getSystemService(Context.CLIPBOARD_SERVICE);
        ClipData clip = ClipData.newPlainText("聊天消息", text);
        clipboard.setPrimaryClip(clip);
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
                startRequest();
            }
        });

        settingsButton.setOnClickListener(v -> showApiKeyDialog());
        newChatButton.setOnClickListener(v -> {
            AlertDialog.Builder builder = new AlertDialog.Builder(requireContext())
                    .setTitle("新增对话")
                    .setMessage("确定要开始新的对话吗？当前对话历史将被清空。")
                    .setPositiveButton("确定", (dialog, which) -> {
                        messageHistory.clear();
                        chatAdapter.notifyDataSetChanged();
                        Toast.makeText(requireContext(), "已开始新对话", Toast.LENGTH_SHORT).show();
                    })
                    .setNegativeButton("取消", null);
            builder.show();
        });
        historyDrawingButton.setOnClickListener(v -> {
            Toast.makeText(requireContext(), "历史会话功能将在后续版本中推出", Toast.LENGTH_SHORT).show();
        });
    }

    /**
     * 开始请求
     */
    private void startRequest() {
        String inputMessage = promptInput.getText().toString().trim();
        if (TextUtils.isEmpty(inputMessage)) {
            Toast.makeText(requireContext(), "请输入提示内容", Toast.LENGTH_SHORT).show();
            return;
        }
        promptInput.setText("");

        // 添加消息到聊天记录
        addMessagesToChat(inputMessage);

        // 更新UI状态
        setUiState(false);

        // 发送API请求
        DeepSeekModelEnum model = R1.getModel().equals(currentModel) ? R1 : V3;
        DeepSeekUtils.sendChatRequestStream(
                getApiKey(),
                model,
                getMessagesToSend(model),
                currentScenario.getTemperature(),
                createCallbackHandler()
        );
    }

    /**
     * 添加消息到聊天记录
     */
    private void addMessagesToChat(String userMessage) {
        // 添加用户消息
        messageHistory.add(new ChatMessage(ROLE_USER, userMessage));
        chatAdapter.notifyItemInserted(messageHistory.size() - 1);
        curAssistantMessageIndex = messageHistory.size(); // 索引从 0 开始

        // 添加助手消息（等待接收内容）
        ChatMessage assistantMessage = new ChatMessage(ROLE_ASSISTANT, "");
        assistantMessage.setInProgress(true);

        messageHistory.add(assistantMessage);
        chatAdapter.notifyItemInserted(messageHistory.size() - 1);
        chatRecyclerView.smoothScrollToPosition(messageHistory.size() - 1);
    }

    /**
     * 设置UI状态
     */
    private void setUiState(boolean enabled) {
        promptInput.setEnabled(enabled);
        sendButton.setEnabled(enabled);
        if (!enabled) {
            sendButton.setText(R.string.cancel_button);
            sendButton.setBackgroundResource(R.drawable.cancel_button_background);
        } else {
            sendButton.setText(R.string.send_button);
            sendButton.setBackgroundResource(R.drawable.send_button_background);
        }
    }

    /**
     * 获取要发送的消息列表（已控制上下文长度）
     */
    private List<ChatMessage> getMessagesToSend(DeepSeekModelEnum model) {
        // 获取截止到当前用户消息的所有消息(不包括当前助手消息)
        List<ChatMessage> messages = new ArrayList<>(messageHistory.subList(0, messageHistory.size() - 1));
        if (messages.size() <= 1) {
            return new ArrayList<>(messages); // 如果只有一条消息，无需裁剪
        }

        int totalLength = 0;
        List<ChatMessage> messagesToKeep = new ArrayList<>();
        for (int i = messages.size() - 1; i >= 0; i--) {
            ChatMessage message = messages.get(i);
            int messageLength = message.getContent().length();
            if (totalLength + messageLength <= model.getMaxContextLength()) {
                messagesToKeep.add(0, message);
                totalLength += messageLength;
            } else {
                break;
            }
        }

        if (messagesToKeep.size() < messages.size()) {
            int removed = messages.size() - messagesToKeep.size();
            Log.d(TAG, "裁剪历史消息: 移除了 " + removed + " 条较早的消息，以适应上下文长度限制");
        }
        return messagesToKeep;
    }

    /**
     * 创建回调处理器
     */
    private DeepSeekStreamCallback createCallbackHandler() {
        return new DeepSeekStreamCallback() {
            @Override
            public void onMessage(String reasoningContent, String messageContent) {
                if (reasoningContent != null) {
                    updateAssistantReasoningContent(reasoningContent);
                }
                if (messageContent != null) {
                    updateAssistantMessageContent(messageContent);
                }
            }

            @Override
            public void onComplete(String fullMessageContent, String fullReasoningContent) {
                handleRequestSuccess(fullMessageContent, fullReasoningContent);
            }

            @Override
            public void onError(String errorMsg) {
                handleRequestError(errorMsg);
            }
        };
    }

    /**
     * 更新助手消息的思考内容
     */
    private void updateAssistantReasoningContent(String newReasoning) {
        if (curAssistantMessageIndex >= 0 && curAssistantMessageIndex < messageHistory.size()) {
            ChatMessage message = messageHistory.get(curAssistantMessageIndex);

            // 只更新思考内容，不影响主要内容
            message.appendReasoning(newReasoning);

            // 更新UI
            mainHandler.post(() -> {
                chatAdapter.notifyItemChanged(curAssistantMessageIndex);
            });
        }
    }

    /**
     * 更新助手消息内容（流式响应时使用）
     */
    private void updateAssistantMessageContent(String newContent) {
        if (curAssistantMessageIndex >= 0 && curAssistantMessageIndex < messageHistory.size()) {
            ChatMessage message = messageHistory.get(curAssistantMessageIndex);

            // 追加内容
            String currentContent = message.getContent();
            message.setContent(currentContent + newContent);

            // 更新UI，不进行自动滚动
            mainHandler.post(() -> chatAdapter.notifyItemChanged(curAssistantMessageIndex));
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
        if (curAssistantMessageIndex >= 0 && curAssistantMessageIndex < messageHistory.size()) {
            ChatMessage message = messageHistory.get(curAssistantMessageIndex);
            message.setContent("*请求已取消*");
            message.setInProgress(false);
            chatAdapter.notifyItemChanged(curAssistantMessageIndex);
        }

        curAssistantMessageIndex = -1;
    }

    /**
     * 请求成功处理
     */
    private void handleRequestSuccess(String content, String reasoning) {
        finishRequest();
        consecutiveFailures = 0;

        // 更新当前消息内容
        updateAssistantFinalMessage(content, reasoning);

        // 添加日志记录
        logMessageCompletion(content, reasoning);
    }

    /**
     * 更新助手最终消息
     */
    private void updateAssistantFinalMessage(String content, String reasoning) {
        if (curAssistantMessageIndex >= 0 && curAssistantMessageIndex < messageHistory.size()) {
            ChatMessage message = messageHistory.get(curAssistantMessageIndex);
            message.setContent(content);
            message.setReasoning(reasoning);
            message.setInProgress(false);

            // 更新UI
            chatAdapter.notifyItemChanged(curAssistantMessageIndex);
        }

        curAssistantMessageIndex = -1;
    }

    /**
     * 结束请求状态
     */
    private void finishRequest() {
        isRequestInProgress = false;
        updateUIForRequestInProgress(false);
    }

    /**
     * 记录消息完成日志
     */
    private void logMessageCompletion(String content, String reasoning) {
        if (curAssistantMessageIndex >= 0 && curAssistantMessageIndex < messageHistory.size()) {
            ChatMessage message = messageHistory.get(curAssistantMessageIndex);
            Log.d(TAG, "回答完成: " + message.toString() +
                    " | 角色: " + message.getRole() +
                    " | 内容长度: " + content.length() +
                    " | 推理内容长度: " + (reasoning != null ? reasoning.length() : 0) +
                    " | 时间戳: " + message.getTimestamp() +
                    " | 场景: " + currentScenario.getDisplayName() +
                    " | 温度: " + currentScenario.getTemperature());
        }
    }

    /**
     * 请求失败处理
     */
    private void handleRequestError(String errorMessage) {
        finishRequest();
        incrementFailureCount();

        // 更新当前消息内容为错误信息
        updateAssistantErrorMessage(errorMessage);

        // 处理连续失败情况
        handleConsecutiveFailures();
    }

    /**
     * 更新助手错误消息
     */
    private void updateAssistantErrorMessage(String errorMessage) {
        if (curAssistantMessageIndex >= 0 && curAssistantMessageIndex < messageHistory.size()) {
            ChatMessage message = messageHistory.get(curAssistantMessageIndex);
            message.setContent("**错误：** " + errorMessage);
            message.setInProgress(false);
            chatAdapter.notifyItemChanged(curAssistantMessageIndex);
        }
    }

    /**
     * 增加失败计数
     */
    private void incrementFailureCount() {
        consecutiveFailures++;
    }

    /**
     * 处理连续失败
     */
    private void handleConsecutiveFailures() {
        // 如果连续失败次数达到阈值，从上下文中移除本次问题
        if (consecutiveFailures >= MAX_FAILURES) {
            // 移除最后两条消息（用户问题和失败回答）
            removeLastMessages();
            showError("连续请求失败，已从上下文中移除本次问题");
            consecutiveFailures = 0;
        }

        curAssistantMessageIndex = -1;
    }

    /**
     * 移除最后的消息
     */
    private void removeLastMessages() {
        if (messageHistory.size() >= 2) {
            messageHistory.remove(messageHistory.size() - 1);
            messageHistory.remove(messageHistory.size() - 1);
            chatAdapter.notifyDataSetChanged();
        }
    }

    /**
     * 更新 UI 以反映请求状态
     */
    private void updateUIForRequestInProgress(boolean inProgress) {
        setUiState(!inProgress);

        // 其他UI组件的状态更新
        modelSelector.setEnabled(!inProgress);
        scenarioSpinner.setEnabled(!inProgress);
        newChatButton.setEnabled(!inProgress);
        historyDrawingButton.setEnabled(!inProgress);
    }

    /**
     * 获取保存的API密钥
     */
    private String getApiKey() {
        SharedPreferences prefs = requireActivity().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        return prefs.getString(KEY_API_KEY, "");
    }

    /**
     * 获取保存的场景选择
     */
    private String getSelectedScenario() {
        SharedPreferences prefs = requireActivity().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        return prefs.getString(KEY_SCENARIO, ChatScenarioEnum.DATA_ANALYSIS.getDisplayName());
    }

    /**
     * 保存场景选择
     */
    private void saveSelectedScenario(String scenarioName) {
        SharedPreferences prefs = requireActivity().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        prefs.edit().putString(KEY_SCENARIO, scenarioName).apply();
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
    private int addMessageToChatList(ChatMessage message) {
        messageHistory.add(message);
        chatAdapter.notifyItemInserted(messageHistory.size() - 1);
        // 滚动到最新消息
        return messageHistory.size() - 1;
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
        return prefs.getString("selected_model", DeepSeekModelEnum.V3.getModel());
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