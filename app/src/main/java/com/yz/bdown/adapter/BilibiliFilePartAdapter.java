package com.yz.bdown.adapter;

import static android.content.Intent.ACTION_SEND;
import static androidx.core.content.ContextCompat.startActivity;
import static com.google.android.material.snackbar.BaseTransientBottomBar.LENGTH_LONG;
import static com.google.android.material.snackbar.BaseTransientBottomBar.LENGTH_SHORT;
import static com.yz.bdown.utils.AudioConverterUtils.convertM4sToMp3;
import static com.yz.bdown.utils.FileUtils.getFileUri;
import static com.yz.bdown.utils.FileUtils.getFolder;
import static com.yz.bdown.utils.FileUtils.toFile;

import android.app.Dialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Typeface;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.Button;
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
import com.yz.bdown.utils.TextExtractorUtils;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * B站文件适配器
 * 用于显示和管理已下载的B站视频文件列表
 */
public class BilibiliFilePartAdapter extends RecyclerView.Adapter<BilibiliFilePartAdapter.FileViewHolder> implements Filterable {

    private static final String TAG = "BilibiliFilePartAdapter";
    private static final String BILIBILI_FOLDER = "bilibiliDown";

    private final List<FileItem> fileList;
    private final List<FileItem> fileListFull; // 用于搜索过滤
    private final List<FileViewHolder> activeViewHolders = new ArrayList<>(); // 追踪所有活跃的 ViewHolder
    private OnFileItemClickListener onFileItemClickListener;
    private final Map<String, Bitmap> thumbnailCache = new HashMap<>(); // 缩略图缓存
    private final ExecutorService executorService = Executors.newFixedThreadPool(3); // 线程池用于加载缩略图
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    // 记录当前选中的排序方式
    private int currentSortMethod = 0; // 0=未排序，1=按名称，2=按日期，3=按大小

    /**
     * 文件项点击监听器接口
     */
    public interface OnFileItemClickListener {
        void onFileItemClick(FileItem fileItem);
    }

    /**
     * 设置文件项点击监听器
     *
     * @param listener 文件项点击监听回调
     */
    public void setOnFileItemClickListener(OnFileItemClickListener listener) {
        this.onFileItemClickListener = listener;
    }

    /**
     * 构造函数
     *
     * @param fileList 文件列表数据
     */
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

        // 设置按钮点击事件
        setupButtonListeners(holder, fileItem);

        // 设置整个卡片的点击事件
        holder.itemView.setOnClickListener(v -> {
            if (onFileItemClickListener != null) {
                onFileItemClickListener.onFileItemClick(fileItem);
            }
        });
    }

    /**
     * 设置按钮的点击监听器
     *
     * @param holder   ViewHolder对象
     * @param fileItem 文件项数据
     */
    private void setupButtonListeners(FileViewHolder holder, FileItem fileItem) {
        // 设置分享和音乐按钮点击事件
        holder.shareButton.setOnClickListener(v -> share(fileItem, v));
        holder.musicButton.setOnClickListener(v -> transformMp3(fileItem, v));

        // 设置文本提取按钮点击事件
        holder.textExtractButton.setOnClickListener(v -> showTextExtractionDialog(fileItem, v));
    }

    /**
     * 加载视频缩略图
     *
     * @param fileItem  文件项数据
     * @param imageView 图片视图
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
                    mainHandler.post(() -> imageView.setImageBitmap(bitmap));
                }
            } catch (Exception e) {
                Log.e(TAG, "无法加载视频缩略图: " + fileItem.getFileName(), e);
            }
        });
    }

    /**
     * 分享文件
     *
     * @param fileItem 要分享的文件项
     * @param view     视图
     */
    private void share(FileItem fileItem, View view) {
        Context context = view.getContext();
        Uri fileUri = getFileUri(context, getFolder(BILIBILI_FOLDER), fileItem.getFileName());
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

    /**
     * 获取文件的 MIME 类型
     *
     * @param fileType 文件类型
     * @return MIME类型字符串
     */
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

    /**
     * 转换视频为MP3格式
     *
     * @param fileItem 文件项
     * @param v        视图
     */
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

    /**
     * 释放播放器资源
     */
    public void releasePlayer() {
        activeViewHolders.clear();
    }

    /**
     * 释放所有资源
     */
    public void destroy() {
        releasePlayer();
        executorService.shutdown();
        thumbnailCache.clear();
    }

    /**
     * 按名称排序文件列表
     */
    public void sortByName() {
        currentSortMethod = 1;
        Collections.sort(fileList, (file1, file2) -> file1.getFileName().compareToIgnoreCase(file2.getFileName()));
        notifyDataSetChanged();
    }

    /**
     * 按日期排序文件列表
     */
    public void sortByDate() {
        currentSortMethod = 2;
        Collections.sort(fileList, (file1, file2) -> {
            if (file1.getLastModifiedTimestamp() == file2.getLastModifiedTimestamp()) {
                return 0;
            }
            return file1.getLastModifiedTimestamp() > file2.getLastModifiedTimestamp() ? -1 : 1;
        });
        notifyDataSetChanged();
    }

    /**
     * 按大小排序文件列表
     */
    public void sortBySize() {
        currentSortMethod = 3;
        Collections.sort(fileList, (file1, file2) -> {
            if (file1.getFileSizeBytes() == file2.getFileSizeBytes()) {
                return 0;
            }
            return file1.getFileSizeBytes() > file2.getFileSizeBytes() ? -1 : 1;
        });
        notifyDataSetChanged();
    }

    @Override
    public Filter getFilter() {
        return fileFilter;
    }

    /**
     * 文件搜索过滤器
     */
    private final Filter fileFilter = new Filter() {
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
            //noinspection unchecked
            fileList.addAll((List<FileItem>) results.values);

            // 应用当前排序方式
            applySortMethod();

            notifyDataSetChanged();
        }
    };

    /**
     * 应用当前选中的排序方式
     */
    private void applySortMethod() {
        switch (currentSortMethod) {
            case 1:
                Collections.sort(fileList, (file1, file2) ->
                        file1.getFileName().compareToIgnoreCase(file2.getFileName()));
                break;
            case 2:
                Collections.sort(fileList, (file1, file2) -> {
                    if (file1.getLastModifiedTimestamp() == file2.getLastModifiedTimestamp()) {
                        return 0;
                    }
                    return file1.getLastModifiedTimestamp() > file2.getLastModifiedTimestamp() ? -1 : 1;
                });
                break;
            case 3:
                Collections.sort(fileList, (file1, file2) -> {
                    if (file1.getFileSizeBytes() == file2.getFileSizeBytes()) {
                        return 0;
                    }
                    return file1.getFileSizeBytes() > file2.getFileSizeBytes() ? -1 : 1;
                });
                break;
            default:
                // 不排序
                break;
        }
    }

    /**
     * 显示文本提取选择对话框
     *
     * @param fileItem 文件项
     * @param view     视图
     */
    private void showTextExtractionDialog(FileItem fileItem, View view) {
        Context context = view.getContext();

        // 创建自定义对话框
        Dialog dialog = new Dialog(context, R.style.RoundedCornerDialog);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.dialog_text_extraction_choice);

        // 设置按钮点击事件
        Button btnVideo = dialog.findViewById(R.id.btn_extract_video);
        Button btnAudio = dialog.findViewById(R.id.btn_extract_audio);
        Button btnCancel = dialog.findViewById(R.id.btn_cancel);

        btnVideo.setOnClickListener(v -> {
            dialog.dismiss();
            extractTextFromVideo(fileItem, view);
        });

        btnAudio.setOnClickListener(v -> {
            dialog.dismiss();
            extractTextFromAudio(fileItem, view);
        });

        btnCancel.setOnClickListener(v -> dialog.dismiss());

        dialog.show();
    }

    /**
     * 从视频中提取文本
     *
     * @param fileItem 文件项
     * @param view     视图
     */
    private void extractTextFromVideo(FileItem fileItem, View view) {
        Context context = view.getContext();
        Dialog progressDialog = createProgressDialog(context, "正在从视频提取文本...");
        progressDialog.show();

        // 创建文本提取参数
        String videoPath = fileItem.getPreviewUri().replace("file://", "");

        // 执行文本提取（这里使用了模拟的回调，实际项目中应使用真实实现）
        TextExtractorUtils.extractTextFromVideo(videoPath, new TextExtractorUtils.ExtractionCallback() {
            @Override
            public void onProgress(int progress) {
                // 更新进度
            }

            @Override
            public void onComplete(String text) {
                mainHandler.post(() -> {
                    progressDialog.dismiss();
                    showExtractedTextDialog(context, "视频文本", text);
                });
            }

            @Override
            public void onError(String errorMessage) {
                mainHandler.post(() -> {
                    progressDialog.dismiss();
                    Snackbar.make(view, "提取文本失败: " + errorMessage, LENGTH_LONG).show();
                });
            }
        });
    }

    /**
     * 从音频中提取文本
     *
     * @param fileItem 文件项
     * @param view     视图
     */
    private void extractTextFromAudio(FileItem fileItem, View view) {
        Context context = view.getContext();
        Dialog progressDialog = createProgressDialog(context, "正在从音频中提取文本...");
        progressDialog.show();

        // 找到对应的音频文件
        String fileName = fileItem.getFileName();
        String fileNamePrefix = fileName.substring(0, fileName.lastIndexOf('.'));
        String m4sPath = toFile(fileNamePrefix + "_audio.m4s", BILIBILI_FOLDER).getAbsolutePath();
        File audioFile = new File(m4sPath);

        if (!audioFile.exists()) {
            progressDialog.dismiss();
            Snackbar.make(view, "未找到对应的音频文件", LENGTH_LONG).show();
            return;
        }

        // 执行音频转文本的操作
        TextExtractorUtils.extractTextFromAudio(m4sPath, new TextExtractorUtils.ExtractionCallback() {
            @Override
            public void onProgress(int progress) {
                // 更新进度
            }

            @Override
            public void onComplete(String text) {
                mainHandler.post(() -> {
                    progressDialog.dismiss();
                    showExtractedTextDialog(context, "音频转文本", text);
                });
            }

            @Override
            public void onError(String errorMessage) {
                mainHandler.post(() -> {
                    progressDialog.dismiss();
                    Snackbar.make(view, "提取文本失败: " + errorMessage, LENGTH_LONG).show();
                });
            }
        });
    }

    /**
     * 创建进度对话框
     *
     * @param context 上下文
     * @param message 显示消息
     * @return 对话框实例
     */
    private Dialog createProgressDialog(Context context, String message) {
        Dialog dialog = new Dialog(context, R.style.RoundedCornerDialog);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.extraction_progress_dialog);
        dialog.setCancelable(false);

        TextView messageTextView = dialog.findViewById(R.id.progress_message);
        messageTextView.setText(message);

        return dialog;
    }

    /**
     * 显示提取的文本对话框
     *
     * @param context 上下文
     * @param title   标题
     * @param text    文本内容
     */
    private void showExtractedTextDialog(Context context, String title, String text) {
        Dialog dialog = new Dialog(context, R.style.RoundedCornerDialog);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.extracted_text_dialog);

        TextView titleTextView = dialog.findViewById(R.id.dialog_title);
        TextView contentTextView = dialog.findViewById(R.id.dialog_content);
        Button copyButton = dialog.findViewById(R.id.copy_button);
        Button closeButton = dialog.findViewById(R.id.close_button);

        // 设置文本内容
        titleTextView.setText(title);

        if (text != null && !text.isEmpty()) {
            contentTextView.setText(text);
            contentTextView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
            contentTextView.setTypeface(Typeface.create("sans-serif", Typeface.NORMAL));
        } else {
            contentTextView.setText("未能提取到任何文本");
            contentTextView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 18);
            contentTextView.setTypeface(Typeface.create("sans-serif-medium", Typeface.ITALIC));
        }

        // 设置复制按钮点击事件
        copyButton.setOnClickListener(v -> {
            ClipboardManager clipboard = (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
            ClipData clip = ClipData.newPlainText("Extracted Text", text);
            clipboard.setPrimaryClip(clip);
            Snackbar.make(v, "已复制到剪贴板", LENGTH_SHORT).show();
        });

        // 设置关闭按钮点击事件
        closeButton.setOnClickListener(v -> dialog.dismiss());

        dialog.show();
    }

    /**
     * 文件ViewHolder类
     */
    public static class FileViewHolder extends RecyclerView.ViewHolder {
        TextView fileName, fileSize, fileDate;
        ImageButton shareButton, musicButton, textExtractButton;
        ImageView fileThumbnail;

        public FileViewHolder(@NonNull View itemView) {
            super(itemView);
            fileName = itemView.findViewById(R.id.file_name);
            fileSize = itemView.findViewById(R.id.file_size);
            fileDate = itemView.findViewById(R.id.file_date);
            shareButton = itemView.findViewById(R.id.share_button);
            musicButton = itemView.findViewById(R.id.music_button);
            textExtractButton = itemView.findViewById(R.id.text_extract_button);
            fileThumbnail = itemView.findViewById(R.id.file_thumbnail);
        }
    }
}
