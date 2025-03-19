package com.yz.bdown.fragment.bilibili;

import static android.content.Context.MODE_PRIVATE;
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
import com.yz.bdown.model.bilibili.BilibiliTvPart;
import com.yz.bdown.callback.DownloadCallback;
import com.yz.bdown.model.bilibili.BilibiliTvInfo;
import com.yz.bdown.utils.FileUtils;
import com.yz.bdown.utils.GlideUtils;
import com.yz.bdown.utils.NotificationUtils;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * B站视频搜索Fragment
 * 用于搜索和下载B站视频
 */
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

    // UI组件
    private ProgressBar progressBar;
    private View resultCard;
    private TextView videoTitle;
    private TextView episodesTitle;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_bilibili_search, container, false);

        // 初始化视图
        initViews(view);

        // 初始化API和Handler
        initDependencies();

        // 初始化点击事件
        setupListeners();

        // 初始化 recyclerView
        setupRecyclerView();

        return view;
    }

    /**
     * 初始化视图组件
     *
     * @param view 根视图
     */
    private void initViews(View view) {
        bvidInput = view.findViewById(R.id.bsf_input_bvid);
        searchBtn = view.findViewById(R.id.bsf_btn_search);
        coverImage = view.findViewById(R.id.bsf_image_cover);
        recyclerView = view.findViewById(R.id.bsf_tv_part_list);

        // 初始化新增组件
        progressBar = view.findViewById(R.id.progress_bar);
        resultCard = view.findViewById(R.id.result_card);
        videoTitle = view.findViewById(R.id.video_title);
        episodesTitle = view.findViewById(R.id.episodes_title);
    }

    /**
     * 初始化API和Handler
     */
    private void initDependencies() {
        bilibiliTvApi = new BilibiliTvApi(requireActivity().getSharedPreferences("Bilibili", MODE_PRIVATE));
        handler = new Handler(Looper.getMainLooper());
    }

    /**
     * 设置监听器
     */
    private void setupListeners() {
        searchBtn.setOnClickListener(this::search);
    }

    /**
     * 初始化RecyclerView
     */
    private void setupRecyclerView() {
        recyclerAdapter = new BilibiliTvPartAdapter(getActivity(), tvParts);
        recyclerAdapter.setOnItemClickListener(this::downloadItem);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerView.setAdapter(recyclerAdapter);
    }

    /**
     * 执行搜索操作
     *
     * @param v 触发搜索的视图
     */
    private void search(View v) {
        String bvid = getBvid();
        if (isBlank(bvid)) {
            Snackbar.make(v, "BVID 非法, 请重试", LENGTH_SHORT).show();
            return;
        }

        // 显示加载进度条，隐藏结果卡片
        showLoading();

        // 异步查询视频信息
        fetchVideoInfo(bvid, v);
    }

    /**
     * 显示加载状态
     */
    private void showLoading() {
        progressBar.setVisibility(View.VISIBLE);
        resultCard.setVisibility(View.GONE);
    }

    /**
     * 异步获取视频信息
     *
     * @param bvid BVID
     * @param v    视图
     */
    private void fetchVideoInfo(String bvid, View v) {
        supplyAsync(() -> bilibiliTvApi.queryBTvParts(bvid))
                .thenAccept(bilibiliTvInfo -> {
                    if (bilibiliTvInfo == null || isEmpty(bilibiliTvInfo.getbTvParts())) {
                        handler.post(() -> {
                            progressBar.setVisibility(View.GONE);
                            Snackbar.make(v, "视频信息获取失败", LENGTH_SHORT).show();
                        });
                        return;
                    }

                    handler.post(() -> updateUIWithVideoInfo(bilibiliTvInfo));
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

    /**
     * 使用视频信息更新UI
     *
     * @param bilibiliTvInfo 视频信息
     */
    private void updateUIWithVideoInfo(BilibiliTvInfo bilibiliTvInfo) {
        // 隐藏加载进度条
        progressBar.setVisibility(View.GONE);

        // 显示结果卡片
        resultCard.setVisibility(View.VISIBLE);

        // 设置标题
        updateVideoTitle(bilibiliTvInfo.getTitle());

        // 加载封面图片
        loadCoverImage(bilibiliTvInfo.getCoverUrl());

        // 显示分集标题
        episodesTitle.setVisibility(View.VISIBLE);

        // 更新列表数据
        updateVideoPartsList(bilibiliTvInfo.getbTvParts());
    }

    /**
     * 更新视频标题
     *
     * @param title 标题
     */
    private void updateVideoTitle(String title) {
        if (title != null && !title.isEmpty()) {
            videoTitle.setText(title);
            videoTitle.setVisibility(View.VISIBLE);
        } else {
            videoTitle.setVisibility(View.GONE);
        }
    }

    /**
     * 加载封面图片
     *
     * @param coverUrl 封面URL
     */
    private void loadCoverImage(String coverUrl) {
        if (coverUrl != null && !coverUrl.isEmpty()) {
            coverImage.setVisibility(View.VISIBLE);
            // 使用GlideHelper加载网络图片
            GlideUtils.loadBilibiliImage(getContext(), coverUrl, coverImage);

            // 设置列表项的封面URL
            ((BilibiliTvPartAdapter) recyclerAdapter).setCoverUrl(coverUrl);
        } else {
            coverImage.setVisibility(View.GONE);
        }
    }

    /**
     * 更新视频分P列表
     *
     * @param parts 分P列表
     */
    private void updateVideoPartsList(List<BilibiliTvPart> parts) {
        tvParts.clear();
        tvParts.addAll(parts);
        recyclerAdapter.notifyDataSetChanged();
        recyclerView.setVisibility(View.VISIBLE);

        // 打印日志确认数据
        Log.d(TAG, "加载了 " + tvParts.size() + " 个视频分P");
    }

    /**
     * 获取BVID
     *
     * @return BVID
     */
    private String getBvid() {
        String shareUrl = bvidInput.getText().toString();
        if (isBlank(shareUrl)) {
            return "";
        }
        return parseBvid(shareUrl);
    }

    /**
     * 从分享链接中解析BVID
     *
     * @param shareUrl 分享链接
     * @return BVID
     */
    private String parseBvid(String shareUrl) {
        final String[] parts = shareUrl.split("/");
        for (String part : parts) {
            if (part.startsWith("BV")) {
                return part;
            }
        }
        return "";
    }

    /**
     * 下载视频分P
     *
     * @param bTvPart 视频分P信息
     */
    private void downloadItem(BilibiliTvPart bTvPart) {
        View view = getView();
        String title = bTvPart.getTitle();

        // 创建下载进度对话框
        AlertDialog progressDialog = createDownloadProgressDialog(title);

        // 显示对话框
        progressDialog.show();

        // 创建下载回调
        DownloadCallback downloadCallback = createDownloadCallback(progressDialog, title, view);

        // 开始下载
        String fileName = FileUtils.sanitizeFileName(title + ".mp4");
        File downloadDir = FileUtils.getFolder("bilibiliDown");

        CompletableFuture
                .supplyAsync(() -> bilibiliTvApi.downloadBTvPart(bTvPart, downloadDir, fileName, downloadCallback))
                .exceptionally(throwable -> {
                    handler.post(() -> {
                        progressDialog.dismiss();
                        if (view != null) {
                            Snackbar.make(view, "下载失败: " + throwable.getMessage(), LENGTH_SHORT).show();
                        }
                        Log.e(TAG, "下载异常", throwable);
                    });
                    return null;
                });
    }

    /**
     * 创建下载进度对话框
     *
     * @param title 下载标题
     * @return 对话框
     */
    private AlertDialog createDownloadProgressDialog(String title) {
        // 创建下载进度对话框，使用圆角样式
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext(), R.style.RoundedCornerDialog);
        View progressView = LayoutInflater.from(requireContext()).inflate(R.layout.download_progress_dialog, null);

        // 初始化进度对话框控件
        TextView downloadTitle = progressView.findViewById(R.id.download_title);
        TextView downloadFilename = progressView.findViewById(R.id.download_filename);

        // 设置初始值
        downloadTitle.setText("正在下载: " + title);
        downloadFilename.setText("准备下载...");

        // 创建对话框
        builder.setView(progressView);
        builder.setCancelable(false);
        return builder.create();
    }

    /**
     * 创建下载回调
     *
     * @param progressDialog 进度对话框
     * @param title          标题
     * @param view           视图
     * @return 下载回调
     */
    private DownloadCallback createDownloadCallback(AlertDialog progressDialog, String title, View view) {
        return new DownloadCallback() {
            @Override
            public void onDownloadStart(long totalBytes, String fileName) {
                handler.post(() -> {
                    TextView downloadFilename = progressDialog.findViewById(R.id.download_filename);
                    TextView downloadSizeInfo = progressDialog.findViewById(R.id.download_size_info);

                    if (downloadFilename != null) {
                        downloadFilename.setText(fileName);
                    }

                    if (downloadSizeInfo != null) {
                        if (totalBytes > 0) {
                            downloadSizeInfo.setText(formatFileSize(0) + " / " + formatFileSize(totalBytes));
                        } else {
                            downloadSizeInfo.setText("大小未知");
                        }
                    }
                });
            }

            @Override
            public void onProgressUpdate(long bytesRead, long totalBytes, double speed) {
                handler.post(() -> {
                    ProgressBar downloadProgressBar = progressDialog.findViewById(R.id.download_progress_bar);
                    TextView downloadProgressText = progressDialog.findViewById(R.id.download_progress_text);
                    TextView downloadSizeInfo = progressDialog.findViewById(R.id.download_size_info);
                    TextView downloadSpeed = progressDialog.findViewById(R.id.download_speed);

                    if (totalBytes > 0 && downloadProgressBar != null && downloadProgressText != null && downloadSizeInfo != null) {
                        int progress = (int) (bytesRead * 100 / totalBytes);
                        downloadProgressBar.setProgress(progress);
                        downloadProgressText.setText(progress + "%");
                        downloadSizeInfo.setText(formatFileSize(bytesRead) + " / " + formatFileSize(totalBytes));
                    } else if (downloadProgressBar != null) {
                        downloadProgressBar.setIndeterminate(true);
                    }

                    // 更新下载速度
                    if (downloadSpeed != null) {
                        downloadSpeed.setText(formatSpeed(speed));
                    }
                });
            }

            @Override
            public void onDownloadComplete(String fileName, String filePath) {
                handler.post(() -> {
                    progressDialog.dismiss();
                    if (view != null) {
                        Snackbar.make(view, title + " 下载完成!", LENGTH_SHORT).show();
                    }

                    // 发送通知
                    NotificationUtils.showDownloadCompleteNotification(
                            requireContext(),
                            "下载完成",
                            title + " 下载完成",
                            filePath
                    );
                });
            }

            @Override
            public void onDownloadError(String errorMessage) {
                handler.post(() -> {
                    progressDialog.dismiss();
                    if (view != null) {
                        Snackbar.make(view, "下载失败: " + errorMessage, Snackbar.LENGTH_LONG).show();
                    }
                });
            }
        };
    }

    /**
     * 格式化文件大小
     *
     * @param size 文件大小（字节）
     * @return 格式化后的文件大小
     */
    private String formatFileSize(long size) {
        if (size <= 0) return "0 B";
        final String[] units = new String[]{"B", "KB", "MB", "GB", "TB"};
        int digitGroups = (int) (Math.log10(size) / Math.log10(1024));
        return String.format("%.1f %s", size / Math.pow(1024, digitGroups), units[digitGroups]);
    }

    /**
     * 格式化下载速度
     *
     * @param kbps 下载速度（KB/s）
     * @return 格式化后的下载速度
     */
    private String formatSpeed(double kbps) {
        if (kbps < 1024) {
            return String.format("%.1f KB/s", kbps);
        } else {
            return String.format("%.1f MB/s", kbps / 1024);
        }
    }
}