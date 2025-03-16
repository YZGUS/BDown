package com.yz.bdown.fragment.deepseek;

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
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;
import com.yz.bdown.R;
import com.yz.bdown.adapter.ChatMessageAdapter;
import com.yz.bdown.model.ChatMessage;

import java.util.ArrayList;
import java.util.List;

/**
 * DeepSeek 交互 Fragment - 仅保留 UI 部分
 */
public class DeepSeekFragment extends Fragment {

    private static final String PREFS_NAME = "DeepSeekPrefs";
    private static final String KEY_API_KEY = "api_key";

    private EditText promptInput;
    private Button sendButton;
    private RecyclerView chatRecyclerView;
    private ProgressBar progressBar;
    private FloatingActionButton settingsButton;
    private Spinner modelSelector;
    private Handler mainHandler;
    
    private final String[] MODEL_OPTIONS = {"deepseek-chat", "deepseek-reasoner"};
    private String currentModel = MODEL_OPTIONS[0];
    
    private List<ChatMessage> messageHistory = new ArrayList<>();
    private ChatMessageAdapter chatAdapter;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_deepseek, container, false);
        
        // 初始化视图
        initViews(view);
        
        // 设置模型选择器
        setupModelSelector();
        
        // 设置事件监听
        setupListeners();
        
        return view;
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
        chatAdapter = new ChatMessageAdapter(requireContext(), messageHistory);
        chatRecyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        chatRecyclerView.setAdapter(chatAdapter);
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
            
            // UI 模拟响应 (实际逻辑已移除)
            simulateResponse();
        });
        
        settingsButton.setOnClickListener(v -> showApiKeyDialog());
    }

    /**
     * 模拟响应 - 替代实际 API 调用逻辑
     */
    private void simulateResponse() {
        progressBar.setVisibility(View.VISIBLE);
        
        // 添加一个空的助手消息，用于演示
        addMessage("assistant", "这是一个模拟响应。在实际实现中，这里会显示来自 DeepSeek API 的响应内容。");
        
        // 模拟完成后隐藏进度条
        mainHandler.postDelayed(() -> progressBar.setVisibility(View.GONE), 500);
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