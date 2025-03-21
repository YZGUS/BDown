package com.yz.bdown.fragment.deepseek;

import static com.yz.bdown.contents.DeepSeekModelEnum.R1;
import static com.yz.bdown.contents.DeepSeekModelEnum.V3;

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
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.RadioButton;
import android.widget.RadioGroup;
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
import com.yz.bdown.utils.DeepSeekUtils;

import java.util.ArrayList;
import java.util.List;
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
    private Handler mainHandler;

    // 当前API调用，用于取消请求
    private AtomicBoolean isCancelled = new AtomicBoolean(false);

    private String currentModel = R1.getModel();

    private List<ChatMessage> messageHistory = new ArrayList<>();
    private ChatMessageAdapter chatAdapter;

    // Markwon 实例，用于渲染 Markdown
    private Markwon markwon;

    // 请求状态
    private boolean isRequestInProgress = false;
    private int consecutiveFailures = 0;
    private static final int MAX_FAILURES = 3;
    private int currentAssistantMessageIndex = -1;

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

        // 设置新增对话按钮点击事件
        newChatButton.setOnClickListener(v -> {
            AlertDialog.Builder builder = new AlertDialog.Builder(requireContext())
                    .setTitle("新增对话")
                    .setMessage("确定要开始新的对话吗？当前对话历史将被清空。")
                    .setPositiveButton("确定", (dialog, which) -> {
                        // 清空对话历史
                        messageHistory.clear();
                        chatAdapter.notifyDataSetChanged();
                        Toast.makeText(requireContext(), "已开始新对话", Toast.LENGTH_SHORT).show();
                    })
                    .setNegativeButton("取消", null);
            builder.show();
        });

        // 设置历史会话按钮点击事件
        historyDrawingButton.setOnClickListener(v -> {
            Toast.makeText(requireContext(), "历史会话功能将在后续版本中推出", Toast.LENGTH_SHORT).show();
        });
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

            // 格式化思考内容并设置为当前内容
            String formattedReasoning = formatReasoningContent(message.getReasoning());
            message.setContent(formattedReasoning);

            // 更新UI，不进行自动滚动
            mainHandler.post(() -> {
                chatAdapter.notifyItemChanged(currentAssistantMessageIndex);
            });
        }
    }

    /**
     * 格式化思考内容，确保在一个引用块内
     */
    private String formatReasoningContent(String reasoning) {
        if (reasoning == null || reasoning.isEmpty()) {
            return "";
        }

        // 将全部内容放在一个引用块中
        // 替换所有换行符为换行+>空格，确保多行内容在一个引用块内
        String formatted = reasoning
                .replaceAll("\n", "\n> ")
                .trim();

        // 添加引用块标记并确保最后有两个换行符
        return "> " + formatted + "\n\n";
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

            // 更新UI，不进行自动滚动
            mainHandler.post(() -> {
                chatAdapter.notifyItemChanged(currentAssistantMessageIndex);
            });
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

            // 更新UI，不进行自动滚动
            chatAdapter.notifyItemChanged(currentAssistantMessageIndex);

            // 添加日志记录
            Log.d(TAG, "回答完成: " + message.toString() +
                    " | 角色: " + message.getRole() +
                    " | 内容长度: " + message.getContent().length() +
                    " | 推理内容长度: " + message.getReasoning().length() +
                    " | 时间戳: " + message.getTimestamp());
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
            newChatButton.setEnabled(false);
            historyDrawingButton.setEnabled(false);
        } else {
            // 请求已完成或已取消
            sendButton.setText(R.string.send_button);
            sendButton.setBackgroundResource(R.drawable.send_button_background);
            promptInput.setEnabled(true);
            modelSelector.setEnabled(true);
            newChatButton.setEnabled(true);
            historyDrawingButton.setEnabled(true);
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