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
                if (message.getContent().isEmpty()) {
                    holder.messageText.setVisibility(View.GONE);
                } else {
                    holder.messageText.setVisibility(View.VISIBLE);
                    markwon.setMarkdown(holder.messageText, message.getContent());
                }
            } else {
                // 消息已完成，隐藏进度条，显示 Markdown 内容
                holder.progressBar.setVisibility(View.GONE);
                holder.messageText.setVisibility(View.VISIBLE);
                markwon.setMarkdown(holder.messageText, message.getContent());
            }
        } else {
            // 用户消息使用普通文本显示
            holder.messageText.setText(message.getContent());
        }
    }

    @Override
    public int getItemCount() {
        return messages.size();
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