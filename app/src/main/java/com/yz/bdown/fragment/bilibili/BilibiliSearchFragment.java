package com.yz.bdown.fragment.bilibili;

import static android.content.Context.MODE_PRIVATE;
import static android.view.View.VISIBLE;
import static com.google.android.material.snackbar.BaseTransientBottomBar.LENGTH_SHORT;
import static org.apache.commons.collections4.CollectionUtils.isEmpty;
import static org.apache.commons.lang3.StringUtils.isBlank;

import static java.util.concurrent.CompletableFuture.supplyAsync;

import android.os.Bundle;

import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;

import com.google.android.material.snackbar.Snackbar;
import com.yz.bdown.R;
import com.yz.bdown.adapter.BilibiliTvPartAdapter;
import com.yz.bdown.api.BilibiliTvApi;
import com.yz.bdown.model.BilibiliTvPart;
import com.yz.bdown.utils.FileUtils;
import com.yz.bdown.utils.NotificationHelper;

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

        supplyAsync(() -> bilibiliTvApi.queryBTvParts(bvid))
                .thenAccept(bilibiliTvInfo -> {
                    if (bilibiliTvInfo == null || isEmpty(bilibiliTvInfo.getbTvParts())) {
                        Snackbar.make(v, "视频信息获取失败", LENGTH_SHORT).show();
                        return;
                    }

                    handler.post(() -> {
                        // TODO 读取网络图片
                        tvParts.clear();
                        tvParts.addAll(bilibiliTvInfo.getbTvParts());
                        recyclerAdapter.notifyDataSetChanged();
                        recyclerView.setVisibility(VISIBLE);
                    });
                })
                .exceptionally(throwable -> {
                    Snackbar.make(v, "获取视频信息异常!!!", LENGTH_SHORT).show();
                    Log.e(TAG, "获取视频信息异常", throwable);
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
        Snackbar.make(view, title + " 开始下载 !!!", LENGTH_SHORT).show();
        
        CompletableFuture
                .supplyAsync(() -> bilibiliTvApi.download(bTvPart))
                .thenAccept(result -> {
                    if (result) {
                        // 使用新的通知样式
                        String fileName = title + ".mp4";
                        File downloadedFile = new File(
                            FileUtils.getFolder(bilibiliTvApi.getBilibiliFolder()), 
                            fileName
                        );
                        
                        // 在UI线程上显示通知
                        handler.post(() -> {
                            NotificationHelper.showDownloadCompleteNotification(
                                getContext(),
                                "下载完成",
                                "文件 " + fileName + " 已保存至下载目录",
                                downloadedFile
                            );
                        });
                    } else {
                        Snackbar.make(view, title + " 下载失败", LENGTH_SHORT).show();
                    }
                })
                .exceptionally(throwable -> {
                    Snackbar.make(view, "视频下载异常!!!", LENGTH_SHORT).show();
                    return null;
                });
    }
}