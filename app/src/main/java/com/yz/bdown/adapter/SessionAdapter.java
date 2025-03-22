package com.yz.bdown.adapter;

import android.content.Context;
import android.text.format.DateUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.yz.bdown.R;
import com.yz.bdown.model.chat.ChatSessionSummary;

import java.util.Date;
import java.util.List;

public class SessionAdapter extends RecyclerView.Adapter<SessionAdapter.SessionViewHolder> {

    private List<ChatSessionSummary> sessions;
    private Context context;
    private OnSessionClickListener sessionClickListener;
    private OnDeleteSessionClickListener deleteClickListener;

    public SessionAdapter(Context context, List<ChatSessionSummary> sessions) {
        this.context = context;
        this.sessions = sessions;
    }

    @NonNull
    @Override
    public SessionViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_chat_session, parent, false);
        return new SessionViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull SessionViewHolder holder, int position) {
        ChatSessionSummary session = sessions.get(position);
        
        // 设置会话标题
        holder.titleTextView.setText(session.getName());
        
        // 设置会话预览
        holder.previewTextView.setText(session.getLastMessage());
        
        // 设置会话信息（消息数量和时间）
        String timeAgo = getTimeAgo(session.getLastUpdateTime());
        String info = session.getMessageCount() + "条消息 · " + timeAgo;
        holder.infoTextView.setText(info);
        
        // 设置点击事件
        holder.itemView.setOnClickListener(v -> {
            if (sessionClickListener != null) {
                sessionClickListener.onSessionClick(session.getSessionId());
            }
        });
        
        // 设置删除按钮点击事件
        holder.deleteButton.setOnClickListener(v -> {
            if (deleteClickListener != null) {
                deleteClickListener.onDeleteSessionClick(session.getSessionId());
            }
        });
    }

    @Override
    public int getItemCount() {
        return sessions.size();
    }

    /**
     * 更新会话列表数据
     */
    public void updateSessions(List<ChatSessionSummary> newSessions) {
        this.sessions = newSessions;
        notifyDataSetChanged();
    }

    /**
     * 获取相对时间描述
     */
    private String getTimeAgo(Date date) {
        if (date == null) {
            return "未知时间";
        }
        
        long time = date.getTime();
        long now = System.currentTimeMillis();
        
        CharSequence relativeTime = DateUtils.getRelativeTimeSpanString(
                time, now, DateUtils.MINUTE_IN_MILLIS,
                DateUtils.FORMAT_ABBREV_RELATIVE);
        
        return relativeTime.toString();
    }

    /**
     * 会话点击监听接口
     */
    public interface OnSessionClickListener {
        void onSessionClick(String sessionId);
    }

    /**
     * 删除会话点击监听接口
     */
    public interface OnDeleteSessionClickListener {
        void onDeleteSessionClick(String sessionId);
    }

    /**
     * 设置会话点击监听器
     */
    public void setOnSessionClickListener(OnSessionClickListener listener) {
        this.sessionClickListener = listener;
    }

    /**
     * 设置删除会话点击监听器
     */
    public void setOnDeleteSessionClickListener(OnDeleteSessionClickListener listener) {
        this.deleteClickListener = listener;
    }

    /**
     * 会话视图持有者
     */
    static class SessionViewHolder extends RecyclerView.ViewHolder {
        TextView titleTextView;
        TextView previewTextView;
        TextView infoTextView;
        ImageButton deleteButton;

        public SessionViewHolder(@NonNull View itemView) {
            super(itemView);
            titleTextView = itemView.findViewById(R.id.session_title);
            previewTextView = itemView.findViewById(R.id.session_preview);
            infoTextView = itemView.findViewById(R.id.session_info);
            deleteButton = itemView.findViewById(R.id.delete_session_button);
        }
    }
} 