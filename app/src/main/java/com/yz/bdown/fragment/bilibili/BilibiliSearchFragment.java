package com.yz.bdown.fragment.bilibili;

import static android.content.Context.MODE_PRIVATE;
import static android.view.View.VISIBLE;
import static com.google.android.material.snackbar.BaseTransientBottomBar.LENGTH_SHORT;
import static org.apache.commons.collections4.CollectionUtils.isEmpty;
import static org.apache.commons.lang3.StringUtils.isBlank;
import static java.util.concurrent.CompletableFuture.supplyAsync;

import android.app.AlertDialog;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.snackbar.Snackbar;
import com.yz.bdown.R;
import com.yz.bdown.adapter.BilibiliTvPartAdapter;
import com.yz.bdown.api.BilibiliTvApi;
import com.yz.bdown.model.BilibiliTvPart;
import com.yz.bdown.utils.DownloadCallback;
import com.yz.bdown.utils.FileUtils;
import com.yz.bdown.utils.GlideHelper;
import com.yz.bdown.utils.NotificationHelper;
import com.yz.bdown.utils.SystemNotificationHelper;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class BilibiliSearchFragment extends Fragment {

    private static final String TAG = "BilibiliSearchFragment";

    private EditText bvidInput;
    private Button searchBtn;
    private ImageView coverImage;
    private RecyclerView recyclerView;
    private List<BilibiliTvPart> tvParts = new ArrayList<>();
    private BilibiliTvPartAdapter recyclerAdapter;
    private BilibiliTvApi bilibiliTvApi;
    private Handler handler;
    
    // 新增UI组件
    private ProgressBar progressBar;
    private View resultCard;
    private TextView videoTitle;
    private TextView episodesTitle;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_bilibili_search, container, false);

        // 初始化对象
        bvidInput = view.findViewById(R.id.bsf_input_bvid);
        searchBtn = view.findViewById(R.id.bsf_btn_search);
        coverImage = view.findViewById(R.id.bsf_image_cover);
        recyclerView = view.findViewById(R.id.bsf_tv_part_list);
        
        // 初始化新增组件
        progressBar = view.findViewById(R.id.progress_bar);
        resultCard = view.findViewById(R.id.result_card);
        videoTitle = view.findViewById(R.id.video_title);
        episodesTitle = view.findViewById(R.id.episodes_title);
        
        bilibiliTvApi = new BilibiliTvApi(requireActivity().getSharedPreferences("Bilibili", MODE_PRIVATE));
        handler = new Handler(Looper.getMainLooper());

        // 初始化点击事件
        searchBtn.setOnClickListener(this::search);

        // 初始化 recycleView
        recyclerAdapter = new BilibiliTvPartAdapter(getActivity(), tvParts);
        recyclerAdapter.setOnItemClickListener(this::downloadItem);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerView.setAdapter(recyclerAdapter);
        return view;
    }

    private void search(View v) {
        String bvid = getBvid();
        if (isBlank(bvid)) {
            Snackbar.make(v, "BVID 非法, 请重试", LENGTH_SHORT).show();
            return;
        }

        // 显示加载进度条，隐藏结果卡片
        progressBar.setVisibility(View.VISIBLE);
        resultCard.setVisibility(View.GONE);

        supplyAsync(() -> bilibiliTvApi.queryBTvParts(bvid))
                .thenAccept(bilibiliTvInfo -> {
                    if (bilibiliTvInfo == null || isEmpty(bilibiliTvInfo.getbTvParts())) {
                        handler.post(() -> {
                            progressBar.setVisibility(View.GONE);
                            Snackbar.make(v, "视频信息获取失败", LENGTH_SHORT).show();
                        });
                        return;
                    }

                    handler.post(() -> {
                        // 隐藏加载进度条
                        progressBar.setVisibility(View.GONE);
                        
                        // 显示结果卡片
                        resultCard.setVisibility(View.VISIBLE);
                        
                        // 设置标题
                        String videoTitleText = bilibiliTvInfo.getTitle();
                        if (videoTitleText != null && !videoTitleText.isEmpty()) {
                            videoTitle.setText(videoTitleText);
                            videoTitle.setVisibility(View.VISIBLE);
                        } else {
                            videoTitle.setVisibility(View.GONE);
                        }
                        
                        // 加载封面图片
                        String coverUrl = bilibiliTvInfo.getCoverUrl();
                        if (coverUrl != null && !coverUrl.isEmpty()) {
                            coverImage.setVisibility(View.VISIBLE);
                            // 使用GlideHelper加载网络图片
                            GlideHelper.loadBilibiliImage(getContext(), coverUrl, coverImage);
                                
                            // 设置列表项的封面URL
                            ((BilibiliTvPartAdapter)recyclerAdapter).setCoverUrl(coverUrl);
                        } else {
                            coverImage.setVisibility(View.GONE);
                        }
                        
                        // 显示分集标题
                        episodesTitle.setVisibility(View.VISIBLE);
                        
                        // 更新列表数据
                        tvParts.clear();
                        tvParts.addAll(bilibiliTvInfo.getbTvParts());
                        recyclerAdapter.notifyDataSetChanged();
                        recyclerView.setVisibility(View.VISIBLE);
                        
                        // 打印日志确认数据
                        Log.d(TAG, "加载了 " + tvParts.size() + " 个视频分P");
                    });
                })
                .exceptionally(throwable -> {
                    handler.post(() -> {
                        progressBar.setVisibility(View.GONE);
                        Snackbar.make(v, "获取视频信息异常!!!", LENGTH_SHORT).show();
                        Log.e(TAG, "获取视频信息异常", throwable);
                    });
                    return null;
                });
    }

    private String getBvid() {
        String shareUrl = bvidInput.getText().toString();
        if (isBlank(shareUrl)) {
            return "";
        }
        return parseBvid(shareUrl);
    }

    private String parseBvid(String shareUrl) {
        final String[] parts = shareUrl.split("/");
        for (String part : parts) {
            if (part.startsWith("BV")) {
                return part;
            }
        }
        return "";
    }

    private void downloadItem(BilibiliTvPart bTvPart) {
        View view = getView();
        String title = bTvPart.getTitle();
        
        // 创建下载进度对话框
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        View progressView = LayoutInflater.from(requireContext()).inflate(R.layout.download_progress_dialog, null);
        
        // 初始化进度对话框控件
        TextView downloadTitle = progressView.findViewById(R.id.download_title);
        TextView downloadFilename = progressView.findViewById(R.id.download_filename);
        TextView downloadProgressText = progressView.findViewById(R.id.download_progress_text);
        TextView downloadSpeed = progressView.findViewById(R.id.download_speed);
        ProgressBar downloadProgressBar = progressView.findViewById(R.id.download_progress_bar);
        TextView downloadSizeInfo = progressView.findViewById(R.id.download_size_info);
        Button cancelButton = progressView.findViewById(R.id.download_cancel_button);
        
        // 设置初始值
        downloadTitle.setText("正在下载: " + title);
        downloadFilename.setText("准备下载...");
        
        // 创建对话框
        builder.setView(progressView);
        builder.setCancelable(false);
        AlertDialog progressDialog = builder.create();
        
        // 显示对话框
        progressDialog.show();
        
        // 创建下载回调
        DownloadCallback downloadCallback = new DownloadCallback() {
            @Override
            public void onDownloadStart(long totalBytes, String fileName) {
                handler.post(() -> {
                    downloadFilename.setText(fileName);
                    if (totalBytes > 0) {
                        downloadSizeInfo.setText(formatFileSize(0) + " / " + formatFileSize(totalBytes));
                    } else {
                        downloadSizeInfo.setText("大小未知");
                    }
                });
            }

            @Override
            public void onProgressUpdate(long bytesRead, long totalBytes, double speed) {
                handler.post(() -> {
                    if (totalBytes > 0) {
                        int progress = (int) (bytesRead * 100 / totalBytes);
                        downloadProgressBar.setProgress(progress);
                        downloadProgressText.setText(progress + "%");
                        downloadSizeInfo.setText(formatFileSize(bytesRead) + " / " + formatFileSize(totalBytes));
                    } else {
                        downloadProgressBar.setIndeterminate(true);
                    }
                    
                    // 更新下载速度
                    downloadSpeed.setText(formatSpeed(speed));
                });
            }

            @Override
            public void onDownloadComplete(String fileName, String filePath) {
                handler.post(() -> {
                    downloadFilename.setText(fileName + " - 完成");
                    downloadProgressBar.setProgress(100);
                    downloadProgressText.setText("100%");
                });
            }

            @Override
            public void onDownloadError(String errorMessage) {
                handler.post(() -> {
                    progressDialog.dismiss();
                    Snackbar.make(view, "下载失败: " + errorMessage, LENGTH_SHORT).show();
                });
            }
        };
        
        // 设置取消按钮
        cancelButton.setOnClickListener(v -> {
            progressDialog.dismiss();
            Snackbar.make(view, "下载已取消", LENGTH_SHORT).show();
            // 这里可以添加取消下载的逻辑
        });
        
        // 开始下载
        Snackbar.make(view, title + " 开始下载 !!!", LENGTH_SHORT).show();
        
        CompletableFuture
                .supplyAsync(() -> bilibiliTvApi.download(bTvPart, downloadCallback))
                .thenAccept(result -> {
                    if (result) {
                        // 使用新的通知样式
                        String fileName = title + ".mp4";
                        File downloadedFile = new File(
                            FileUtils.getFolder(bilibiliTvApi.getBilibiliFolder()), 
                            fileName
                        );
                        
                        // 在UI线程上显示对话框通知
                        handler.post(() -> {
                            // 关闭进度对话框
                            progressDialog.dismiss();
                            
                            // 显示下载完成通知
                            NotificationHelper.showDownloadCompleteNotification(
                                getContext(),
                                "下载完成",
                                "文件 " + fileName + " 已保存至下载目录",
                                downloadedFile
                            );
                            
                            // 同时发送系统通知
                            SystemNotificationHelper.sendDownloadCompleteNotification(
                                getContext(),
                                "下载完成",
                                "文件 " + fileName + " 已保存至下载目录",
                                downloadedFile
                            );
                        });
                    } else {
                        handler.post(() -> {
                            progressDialog.dismiss();
                            Snackbar.make(view, title + " 下载失败", LENGTH_SHORT).show();
                        });
                    }
                })
                .exceptionally(throwable -> {
                    handler.post(() -> {
                        progressDialog.dismiss();
                        Snackbar.make(view, "视频下载异常!!!", LENGTH_SHORT).show();
                    });
                    return null;
                });
    }
    
    /**
     * 格式化文件大小
     */
    private String formatFileSize(long size) {
        if (size < 1024) {
            return size + " B";
        } else if (size < 1024 * 1024) {
            return String.format("%.1f KB", size / 1024.0);
        } else if (size < 1024 * 1024 * 1024) {
            return String.format("%.1f MB", size / (1024.0 * 1024));
        } else {
            return String.format("%.1f GB", size / (1024.0 * 1024 * 1024));
        }
    }
    
    /**
     * 格式化下载速度
     */
    private String formatSpeed(double kbps) {
        if (kbps < 1024) {
            return String.format("%.1f KB/s", kbps);
        } else {
            return String.format("%.1f MB/s", kbps / 1024);
        }
    }
}