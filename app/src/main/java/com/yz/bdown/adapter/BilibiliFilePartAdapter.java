package com.yz.bdown.adapter;

import static android.content.Intent.ACTION_SEND;
import static android.view.View.GONE;
import static android.view.View.VISIBLE;
import static androidx.core.content.ContextCompat.startActivity;
import static androidx.recyclerview.widget.RecyclerView.NO_POSITION;
import static com.google.android.material.snackbar.BaseTransientBottomBar.LENGTH_LONG;
import static com.google.android.material.snackbar.BaseTransientBottomBar.LENGTH_SHORT;
import static com.yz.bdown.utils.AudioConverterUtils.convertM4sToMp3;
import static com.yz.bdown.utils.FileUtils.getFileUri;
import static com.yz.bdown.utils.FileUtils.getFolder;
import static com.yz.bdown.utils.FileUtils.toFile;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.media3.common.MediaItem;
import androidx.media3.exoplayer.ExoPlayer;
import androidx.media3.ui.PlayerView;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.snackbar.Snackbar;
import com.yz.bdown.R;
import com.yz.bdown.model.FileItem;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class BilibiliFilePartAdapter extends RecyclerView.Adapter<BilibiliFilePartAdapter.FileViewHolder> {

    private static final String TAG = "BilibiliFilePartAdapter";
    private static final String BILIBILI_FOLDER = "bilibiliDown";
    private List<FileItem> fileList;
    private int currentPlayingPosition = -1; // 当前正在播放的视频位置
    private List<FileViewHolder> activeViewHolders = new ArrayList<>(); // 追踪所有活跃的 ViewHolder

    public BilibiliFilePartAdapter(List<FileItem> fileList) {
        this.fileList = fileList;
    }

    @NonNull
    @Override
    public FileViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.bilibili_file_part, parent, false);
        FileViewHolder holder = new FileViewHolder(view);
        activeViewHolders.add(holder);
        return holder;
    }

    @Override
    public void onBindViewHolder(@NonNull FileViewHolder holder, int position) {
        FileItem fileItem = fileList.get(position);
        holder.fileName.setText(fileItem.getFileName());
        holder.fileSize.setText(fileItem.getFileSize());
        holder.shareButton.setOnClickListener(v -> share(fileItem, v));
        holder.musicButton.setOnClickListener(v -> transformMp3(fileItem, v));
        holder.itemView.setOnClickListener(v -> onClick(holder));
        if (position != currentPlayingPosition) {
            holder.releasePlayer();
            holder.video.setVisibility(GONE);
            return;
        }

        if (fileItem.getFileType().equals("MP4")) {
            holder.video.setVisibility(VISIBLE);
            initializePlayer(holder, fileItem.getPreviewUri());
        }
    }

    private void share(FileItem fileItem, View view) {
        Context context = view.getContext();
        Uri fileUri = getFileUri(context, getFolder("bilibiliDown"), fileItem.getFileName());
        if (fileUri != null) {
            Intent shareIntent = new Intent(ACTION_SEND);
            shareIntent.setType(getMimeType(fileItem.getFileType()));
            shareIntent.putExtra(Intent.EXTRA_STREAM, fileUri);
            shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            startActivity(context, Intent.createChooser(shareIntent, "分享到"), null);
        } else {
            Snackbar.make(view, "文件不存在", LENGTH_LONG).show();
        }
    }

    // 获取文件的 MIME 类型
    private String getMimeType(String fileType) {
        switch (fileType) {
            case "MP4":
                return "video/mp4";
            case "IMAGE":
                return "image/*";
            default:
                return "*/*";
        }
    }

    private void transformMp3(FileItem fileItem, View v) {
        String fileName = fileItem.getFileName();
        String fileNamePrefix = fileName.substring(0, fileName.lastIndexOf('.'));
        String m4sPath = toFile(fileNamePrefix + "_audio.m4s", BILIBILI_FOLDER).getAbsolutePath();
        String mp3Path = toFile(fileNamePrefix + ".mp3", BILIBILI_FOLDER).getAbsolutePath();
        CompletableFuture
                .supplyAsync(() -> convertM4sToMp3(m4sPath, mp3Path))
                .thenAccept(result -> {
                    final String tip = fileName + (result ? " 转换成功" : " 转换失败");
                    Snackbar.make(v, tip, LENGTH_SHORT).show();
                })
                .exceptionally(throwable -> {
                    Snackbar.make(v, "音频转换异常", LENGTH_SHORT).show();
                    Log.e(TAG, "音频转换异常", throwable);
                    return null;
                });
    }

    private void onClick(FileViewHolder holder) {
        int clickPosition = holder.getBindingAdapterPosition();
        if (clickPosition == NO_POSITION) return;

        if (!fileList.get(clickPosition).getFileType().equals("MP4")) {
            return;
        }

        int previousPosition = currentPlayingPosition;
        currentPlayingPosition = (currentPlayingPosition == clickPosition) ? -1 : clickPosition;
        if (previousPosition != -1) {
            notifyItemChanged(previousPosition);
        }

        if (currentPlayingPosition != -1) {
            notifyItemChanged(currentPlayingPosition);
        }
    }

    private void initializePlayer(FileViewHolder holder, String videoUri) {
        holder.releasePlayer(); // 确保释放旧实例
        holder.player = new ExoPlayer.Builder(holder.video.getContext()).build();
        holder.video.setPlayer(holder.player);

        MediaItem mediaItem = MediaItem.fromUri(videoUri);
        holder.player.setMediaItem(mediaItem);
        holder.player.prepare();
        holder.player.play();
    }

    @Override
    public int getItemCount() {
        return fileList.size();
    }

    @Override
    public void onViewRecycled(@NonNull FileViewHolder holder) {
        super.onViewRecycled(holder);
        activeViewHolders.remove(holder);
        holder.releasePlayer(); // 视图被回收时释放播放器
    }

    public void releasePlayer() {
        for (FileViewHolder holder : activeViewHolders) {
            holder.releasePlayer();
        }
        activeViewHolders.clear();
    }

    public static class FileViewHolder extends RecyclerView.ViewHolder {
        TextView fileName, fileSize;
        ImageButton shareButton;
        ImageButton musicButton;
        PlayerView video;
        ExoPlayer player; // 每个 ViewHolder 持有自己的播放器实例

        public FileViewHolder(@NonNull View itemView) {
            super(itemView);
            fileName = itemView.findViewById(R.id.file_name);
            fileSize = itemView.findViewById(R.id.file_size);
            shareButton = itemView.findViewById(R.id.share_button);
            musicButton = itemView.findViewById(R.id.musci_button);
            video = itemView.findViewById(R.id.preview_video);
        }

        // 释放播放器的方法
        public void releasePlayer() {
            if (player != null) {
                player.stop();
                player.release();
                player = null;
                video.setPlayer(null); // 解绑 PlayerView
            }
        }
    }
}
