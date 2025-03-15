package com.yz.bdown.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.yz.bdown.R;
import com.yz.bdown.model.BilibiliTvPart;

import java.util.List;

public class BilibiliTvPartAdapter extends RecyclerView.Adapter<BilibiliTvPartAdapter.ViewHolder> {

    private Context context;
    private List<BilibiliTvPart> items;
    private OnItemClickListener onItemClickListener;

    public BilibiliTvPartAdapter(Context context, List<BilibiliTvPart> items) {
        this.context = context;
        this.items = items;
    }

    public void setOnItemClickListener(OnItemClickListener onItemClickListener) {
        this.onItemClickListener = onItemClickListener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.bilibili_tv_part, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.tvTitle.setText(items.get(position).getTitle());
        holder.tvDuration.setText(items.get(position).getFormatDuration());
        holder.itemView.setOnClickListener(v -> onItemClick(items.get(position)));
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    // 点击事件回调接口
    public interface OnItemClickListener {
        void onItemClick(BilibiliTvPart item);
    }

    // 处理点击事件
    private void onItemClick(BilibiliTvPart item) {
        if (onItemClickListener != null) {
            onItemClickListener.onItemClick(item);
        }
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {

        TextView tvTitle;
        TextView tvDuration;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvTitle = itemView.findViewById(R.id.bilibili_tv_title);
            tvDuration = itemView.findViewById(R.id.bilibili_tv_duration);
        }
    }
}
