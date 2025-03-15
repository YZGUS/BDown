package com.yz.bdown.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions;
import com.bumptech.glide.request.RequestOptions;
import com.yz.bdown.R;
import com.yz.bdown.model.BilibiliTvPart;
import com.yz.bdown.utils.GlideHelper;

import java.util.List;

public class BilibiliTvPartAdapter extends RecyclerView.Adapter<BilibiliTvPartAdapter.ViewHolder> {

    private Context context;
    private List<BilibiliTvPart> items;
    private OnItemClickListener onItemClickListener;
    private String coverUrl; // 封面URL

    public BilibiliTvPartAdapter(Context context, List<BilibiliTvPart> items) {
        this.context = context;
        this.items = items;
    }

    // 设置封面URL
    public void setCoverUrl(String coverUrl) {
        this.coverUrl = coverUrl;
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
        BilibiliTvPart item = items.get(position);
        
        // 设置标题
        holder.tvTitle.setText(item.getTitle());
        
        // 设置时长
        holder.tvDuration.setText(item.getFormatDuration());
        
        // 设置分P编号
        holder.tvPartNumber.setText("P" + (position + 1));
        
        // 加载封面图片
        if (coverUrl != null && !coverUrl.isEmpty()) {
            // 使用GlideHelper加载B站图片
            GlideHelper.loadBilibiliImage(context, coverUrl, holder.imgThumbnail);
        } else {
            // 加载默认图片
            holder.imgThumbnail.setImageResource(R.drawable.ic_no_login);
        }
        
        holder.itemView.setOnClickListener(v -> onItemClick(item));
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
        TextView tvPartNumber;
        ImageView imgThumbnail;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvTitle = itemView.findViewById(R.id.bilibili_tv_title);
            tvDuration = itemView.findViewById(R.id.bilibili_tv_duration);
            tvPartNumber = itemView.findViewById(R.id.bilibili_tv_part_number);
            imgThumbnail = itemView.findViewById(R.id.bilibili_tv_thumbnail);
        }
    }
}
