package com.yz.bdown.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.yz.bdown.R;
import com.yz.bdown.model.chat.ChatMessage;

import java.util.List;

import io.noties.markwon.Markwon;

/**
 * 聊天消息适配器
 * 用于显示AI对话的消息列表
 */
public class ChatMessageAdapter extends RecyclerView.Adapter<ChatMessageAdapter.MessageViewHolder> {

    private static final int VIEW_TYPE_USER = 1;
    private static final int VIEW_TYPE_ASSISTANT = 2;

    private Context context;
    private List<ChatMessage> messages;
    private Markwon markwon;

    // 双击监听接口
    public interface OnItemDoubleClickListener {
        void onItemDoubleClick(ChatMessage message);
    }

    private OnItemDoubleClickListener doubleClickListener;

    // 设置双击监听
    public void setOnItemDoubleClickListener(OnItemDoubleClickListener listener) {
        this.doubleClickListener = listener;
    }

    public ChatMessageAdapter(Context context, List<ChatMessage> messages, Markwon markwon) {
        this.context = context;
        this.messages = messages;
        this.markwon = markwon;
    }

    @Override
    public int getItemViewType(int position) {
        ChatMessage message = messages.get(position);
        return "user".equals(message.getRole()) ? VIEW_TYPE_USER : VIEW_TYPE_ASSISTANT;
    }

    @NonNull
    @Override
    public MessageViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view;
        if (viewType == VIEW_TYPE_USER) {
            view = LayoutInflater.from(context).inflate(R.layout.item_chat_message_user, parent, false);
        } else {
            view = LayoutInflater.from(context).inflate(R.layout.item_chat_message_assistant, parent, false);
        }
        return new MessageViewHolder(view, viewType);
    }

    @Override
    public void onBindViewHolder(@NonNull MessageViewHolder holder, int position) {
        ChatMessage message = messages.get(position);

        if (holder.viewType == VIEW_TYPE_ASSISTANT) {
            // 助手消息使用 Markdown 渲染
            if (message.isInProgress()) {
                // 如果消息正在处理中，显示进度条
                holder.progressBar.setVisibility(View.VISIBLE);
                if (message.getContent().isEmpty() && message.getReasoning().isEmpty()) {
                    holder.messageText.setVisibility(View.GONE);
                } else {
                    holder.messageText.setVisibility(View.VISIBLE);

                    // 如果有推理内容，则将推理内容格式化为引用块并显示
                    String displayContent = message.getContent();
                    if (!message.getReasoning().isEmpty()) {
                        String formattedReasoning = message.getFormattedReasoning();

                        // 如果主内容不为空，先显示推理内容，然后显示主内容
                        if (!displayContent.isEmpty()) {
                            displayContent = formattedReasoning + displayContent;
                        } else {
                            displayContent = formattedReasoning;
                        }
                    }

                    markwon.setMarkdown(holder.messageText, displayContent);
                }
            } else {
                // 消息已完成，隐藏进度条，显示 Markdown 内容
                holder.progressBar.setVisibility(View.GONE);
                holder.messageText.setVisibility(View.VISIBLE);

                // 组合推理内容和主内容
                String displayContent = message.getContent();
                if (!message.getReasoning().isEmpty()) {
                    String formattedReasoning = message.getFormattedReasoning();
                    // 只有在主内容不为空的情况下，才添加推理内容
                    if (!displayContent.isEmpty()) {
                        displayContent = formattedReasoning + displayContent;
                    }
                }

                markwon.setMarkdown(holder.messageText, displayContent);
            }
        } else {
            // 用户消息使用普通文本显示
            holder.messageText.setText(message.getContent());
        }

        // 设置双击监听
        if (doubleClickListener != null) {
            holder.itemView.setOnClickListener(new DoubleClickListener() {
                @Override
                public void onDoubleClick(View v) {
                    doubleClickListener.onItemDoubleClick(message);
                }
            });
        }
    }

    @Override
    public int getItemCount() {
        return messages.size();
    }

    /**
     * 双击检测类
     */
    public abstract class DoubleClickListener implements View.OnClickListener {
        private static final long DOUBLE_CLICK_TIME_DELTA = 300; // 双击时间间隔（毫秒）
        private long lastClickTime = 0;

        @Override
        public void onClick(View v) {
            long clickTime = System.currentTimeMillis();
            if (clickTime - lastClickTime < DOUBLE_CLICK_TIME_DELTA) {
                onDoubleClick(v);
                lastClickTime = 0;
            } else {
                lastClickTime = clickTime;
            }
        }

        public abstract void onDoubleClick(View v);
    }

    static class MessageViewHolder extends RecyclerView.ViewHolder {
        TextView messageText;
        ProgressBar progressBar;
        int viewType;

        MessageViewHolder(View itemView, int viewType) {
            super(itemView);
            this.viewType = viewType;
            messageText = itemView.findViewById(R.id.message_text);

            // 只有助手消息布局中有进度条
            if (viewType == VIEW_TYPE_ASSISTANT) {
                progressBar = itemView.findViewById(R.id.message_progress);
            }
        }
    }
} 