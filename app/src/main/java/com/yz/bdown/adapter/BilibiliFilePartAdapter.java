package com.yz.bdown.adapter;

import static android.content.Intent.ACTION_SEND;
import static androidx.core.content.ContextCompat.startActivity;
import static com.google.android.material.snackbar.BaseTransientBottomBar.LENGTH_LONG;
import static com.google.android.material.snackbar.BaseTransientBottomBar.LENGTH_SHORT;
import static com.yz.bdown.utils.AudioConverterUtils.convertM4sToMp3;
import static com.yz.bdown.utils.FileUtils.getFileUri;
import static com.yz.bdown.utils.FileUtils.getFolder;
import static com.yz.bdown.utils.FileUtils.toFile;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.snackbar.Snackbar;
import com.yz.bdown.R;
import com.yz.bdown.model.FileItem;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class BilibiliFilePartAdapter extends RecyclerView.Adapter<BilibiliFilePartAdapter.FileViewHolder> implements Filterable {

    private static final String TAG = "BilibiliFilePartAdapter";
    private static final String BILIBILI_FOLDER = "bilibiliDown";
    private List<FileItem> fileList;
    private List<FileItem> fileListFull; // 用于搜索过滤
    private int currentPlayingPosition = -1; // 当前正在播放的视频位置
    private List<FileViewHolder> activeViewHolders = new ArrayList<>(); // 追踪所有活跃的 ViewHolder
    private OnFileItemClickListener onFileItemClickListener;
    private Map<String, Bitmap> thumbnailCache = new HashMap<>(); // 缩略图缓存
    private ExecutorService executorService = Executors.newFixedThreadPool(3); // 线程池用于加载缩略图
    private Handler mainHandler = new Handler(Looper.getMainLooper());

    public interface OnFileItemClickListener {
        void onFileItemClick(FileItem fileItem);
    }

    public void setOnFileItemClickListener(OnFileItemClickListener listener) {
        this.onFileItemClickListener = listener;
    }

    public BilibiliFilePartAdapter(List<FileItem> fileList) {
        this.fileList = fileList;
        this.fileListFull = new ArrayList<>(fileList);
    }

    @NonNull
    @Override
    public FileViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.bilibili_file_card, parent, false);
        FileViewHolder holder = new FileViewHolder(view);
        activeViewHolders.add(holder);
        return holder;
    }

    @Override
    public void onBindViewHolder(@NonNull FileViewHolder holder, int position) {
        FileItem fileItem = fileList.get(position);
        
        // 设置文件名和大小
        holder.fileName.setText(fileItem.getFileName());
        holder.fileSize.setText(fileItem.getFileSize());
        
        // 设置文件日期（如果有）
        if (holder.fileDate != null && fileItem.getLastModified() != null) {
            holder.fileDate.setText(fileItem.getLastModified());
        }
        
        // 加载视频缩略图
        loadVideoThumbnail(fileItem, holder.fileThumbnail);
        
        // 设置分享和音乐按钮点击事件
        holder.shareButton.setOnClickListener(v -> share(fileItem, v));
        holder.musicButton.setOnClickListener(v -> transformMp3(fileItem, v));
        
        // 设置整个卡片的点击事件
        holder.itemView.setOnClickListener(v -> {
            if (onFileItemClickListener != null) {
                onFileItemClickListener.onFileItemClick(fileItem);
            }
        });
    }

    /**
     * 加载视频缩略图
     */
    private void loadVideoThumbnail(FileItem fileItem, ImageView imageView) {
        // 先检查缓存中是否已有缩略图
        if (thumbnailCache.containsKey(fileItem.getFileName())) {
            imageView.setImageBitmap(thumbnailCache.get(fileItem.getFileName()));
            return;
        }
        
        // 后台线程加载缩略图
        executorService.execute(() -> {
            try {
                MediaMetadataRetriever retriever = new MediaMetadataRetriever();
                String videoPath = fileItem.getPreviewUri().replace("file://", "");
                retriever.setDataSource(videoPath);
                
                // 获取视频的第一帧作为缩略图
                Bitmap bitmap = retriever.getFrameAtTime(0);
                retriever.release();
                
                if (bitmap != null) {
                    // 缓存缩略图
                    thumbnailCache.put(fileItem.getFileName(), bitmap);
                    
                    // 在主线程更新 UI
                    mainHandler.post(() -> {
                        imageView.setImageBitmap(bitmap);
                    });
                }
            } catch (Exception e) {
                Log.e(TAG, "无法加载视频缩略图: " + fileItem.getFileName(), e);
            }
        });
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

    @Override
    public int getItemCount() {
        return fileList.size();
    }

    @Override
    public void onViewRecycled(@NonNull FileViewHolder holder) {
        super.onViewRecycled(holder);
        activeViewHolders.remove(holder);
    }

    public void releasePlayer() {
        for (FileViewHolder holder : activeViewHolders) {
            // 清理任何播放器资源
        }
        activeViewHolders.clear();
    }

    /**
     * 释放资源
     */
    public void destroy() {
        releasePlayer();
        executorService.shutdown();
        thumbnailCache.clear();
    }

    // 按名称排序
    public void sortByName() {
        Collections.sort(fileList, (file1, file2) -> file1.getFileName().compareToIgnoreCase(file2.getFileName()));
        notifyDataSetChanged();
    }

    // 按日期排序
    public void sortByDate() {
        Collections.sort(fileList, (file1, file2) -> {
            if (file1.getLastModifiedTimestamp() == file2.getLastModifiedTimestamp()) {
                return 0;
            }
            return file1.getLastModifiedTimestamp() > file2.getLastModifiedTimestamp() ? -1 : 1;
        });
        notifyDataSetChanged();
    }

    // 按大小排序
    public void sortBySize() {
        Collections.sort(fileList, (file1, file2) -> {
            if (file1.getFileSizeBytes() == file2.getFileSizeBytes()) {
                return 0;
            }
            return file1.getFileSizeBytes() > file2.getFileSizeBytes() ? -1 : 1;
        });
        notifyDataSetChanged();
    }

    // 更新文件列表数据
    public void updateData(List<FileItem> newFiles) {
        this.fileList = new ArrayList<>(newFiles);
        this.fileListFull = new ArrayList<>(newFiles);
        notifyDataSetChanged();
    }

    @Override
    public Filter getFilter() {
        return fileFilter;
    }

    private Filter fileFilter = new Filter() {
        @Override
        protected FilterResults performFiltering(CharSequence constraint) {
            List<FileItem> filteredList = new ArrayList<>();

            if (constraint == null || constraint.length() == 0) {
                filteredList.addAll(fileListFull);
            } else {
                String filterPattern = constraint.toString().toLowerCase().trim();
                for (FileItem item : fileListFull) {
                    if (item.getFileName().toLowerCase().contains(filterPattern)) {
                        filteredList.add(item);
                    }
                }
            }

            FilterResults results = new FilterResults();
            results.values = filteredList;
            return results;
        }

        @Override
        protected void publishResults(CharSequence constraint, FilterResults results) {
            fileList.clear();
            fileList.addAll((List) results.values);
            notifyDataSetChanged();
        }
    };

    public static class FileViewHolder extends RecyclerView.ViewHolder {
        TextView fileName, fileSize, fileDate;
        ImageButton shareButton, musicButton;
        ImageView fileThumbnail;

        public FileViewHolder(@NonNull View itemView) {
            super(itemView);
            fileName = itemView.findViewById(R.id.file_name);
            fileSize = itemView.findViewById(R.id.file_size);
            fileDate = itemView.findViewById(R.id.file_date);
            shareButton = itemView.findViewById(R.id.share_button);
            musicButton = itemView.findViewById(R.id.music_button);
            fileThumbnail = itemView.findViewById(R.id.file_thumbnail);
        }
    }
}
