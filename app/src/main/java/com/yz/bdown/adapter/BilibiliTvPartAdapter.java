package com.yz.bdown.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.yz.bdown.R;
import com.yz.bdown.model.BilibiliTvPart;
import com.yz.bdown.utils.GlideUtils;

import java.util.List;

/**
 * B站视频分P适配器
 * 用于显示B站视频的分P列表
 */
public class BilibiliTvPartAdapter extends RecyclerView.Adapter<BilibiliTvPartAdapter.ViewHolder> {

    private final Context context;
    private final List<BilibiliTvPart> items;
    private OnItemClickListener onItemClickListener;
    private String coverUrl; // 视频封面URL

    /**
     * 构造函数
     *
     * @param context 上下文
     * @param items   视频分P列表
     */
    public BilibiliTvPartAdapter(Context context, List<BilibiliTvPart> items) {
        this.context = context;
        this.items = items;
    }

    /**
     * 设置封面URL
     *
     * @param coverUrl 封面图片URL
     */
    public void setCoverUrl(String coverUrl) {
        this.coverUrl = coverUrl;
    }

    /**
     * 设置项目点击监听器
     *
     * @param onItemClickListener 点击回调接口
     */
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
        loadThumbnail(holder.imgThumbnail);

        // 设置点击监听
        holder.itemView.setOnClickListener(v -> onItemClick(item));
    }

    /**
     * 加载视频缩略图
     *
     * @param imageView 图片视图
     */
    private void loadThumbnail(ImageView imageView) {
        if (coverUrl != null && !coverUrl.isEmpty()) {
            // 使用GlideUtils加载B站图片
            GlideUtils.loadBilibiliImage(context, coverUrl, imageView);
        } else {
            // 加载默认图片
            imageView.setImageResource(R.drawable.ic_no_login);
        }
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    /**
     * 项目点击事件回调接口
     */
    public interface OnItemClickListener {
        void onItemClick(BilibiliTvPart item);
    }

    /**
     * 处理项目点击事件
     *
     * @param item 被点击的项目
     */
    private void onItemClick(BilibiliTvPart item) {
        if (onItemClickListener != null) {
            onItemClickListener.onItemClick(item);
        }
    }

    /**
     * 视图持有者内部类
     */
    public static class ViewHolder extends RecyclerView.ViewHolder {
        final TextView tvTitle;
        final TextView tvDuration;
        final TextView tvPartNumber;
        final ImageView imgThumbnail;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvTitle = itemView.findViewById(R.id.bilibili_tv_title);
            tvDuration = itemView.findViewById(R.id.bilibili_tv_duration);
            tvPartNumber = itemView.findViewById(R.id.bilibili_tv_part_number);
            imgThumbnail = itemView.findViewById(R.id.bilibili_tv_thumbnail);
        }
    }
}
