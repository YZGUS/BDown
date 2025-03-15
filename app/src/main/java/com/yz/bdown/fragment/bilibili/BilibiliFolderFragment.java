package com.yz.bdown.fragment.bilibili;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;

import androidx.fragment.app.Fragment;
import androidx.media3.common.MediaItem;
import androidx.media3.common.Player;
import androidx.media3.exoplayer.ExoPlayer;
import androidx.media3.ui.PlayerView;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.yz.bdown.R;
import com.yz.bdown.adapter.BilibiliFilePartAdapter;
import com.yz.bdown.model.FileItem;
import com.yz.bdown.utils.FileUtils;
import com.yz.bdown.utils.TextExtractorUtils;

import java.io.File;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

public class BilibiliFolderFragment extends Fragment {

    private RecyclerView fileRecyclerView;
    private BilibiliFilePartAdapter fileAdapter;
    private EditText searchInput;
    private RadioGroup sortGroup;
    private RadioButton sortName, sortDate, sortSize;
    private String selectedFolder = "bilibiliDown"; // 默认文件夹
    
    // 视频播放器相关控件
    private FrameLayout videoPlayerContainer;
    private PlayerView popupVideoPlayer;
    private TextView popupVideoTitle;
    private Button popupCloseButton;
    private ExoPlayer exoPlayer;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_bilibili_folder, container, false);
        
        // 初始化视图控件
        fileRecyclerView = view.findViewById(R.id.bff_file_list);
        searchInput = view.findViewById(R.id.search_input);
        sortGroup = view.findViewById(R.id.sort_group);
        sortName = view.findViewById(R.id.sort_name);
        sortDate = view.findViewById(R.id.sort_date);
        sortSize = view.findViewById(R.id.sort_size);
        
        // 初始化视频播放器相关控件
        videoPlayerContainer = view.findViewById(R.id.video_player_container);
        popupVideoPlayer = view.findViewById(R.id.popup_video_player);
        popupVideoTitle = view.findViewById(R.id.popup_video_title);
        popupCloseButton = view.findViewById(R.id.popup_close_button);

        // 设置 RecyclerView 为网格布局，每行显示两个卡片
        fileRecyclerView.setLayoutManager(new GridLayoutManager(getContext(), 2));
        fileAdapter = new BilibiliFilePartAdapter(getFilesFromExternalMovies());
        fileRecyclerView.setAdapter(fileAdapter);
        
        // 设置文件项点击监听器
        fileAdapter.setOnFileItemClickListener(this::showVideoPlayer);
        
        // 设置搜索监听
        setupSearchListener();
        
        // 设置排序监听
        setupSortListener();
        
        // 设置关闭播放器按钮监听
        popupCloseButton.setOnClickListener(v -> hideVideoPlayer());
        
        // 设置视频播放器容器点击事件 (点击空白区域关闭)
        videoPlayerContainer.setOnClickListener(v -> {
            // 只在点击空白区域时关闭，避免点击播放器内部时也关闭
            if (v.getId() == R.id.video_player_container) {
                hideVideoPlayer();
            }
        });
        
        return view;
    }

    private List<FileItem> getFilesFromExternalMovies() {
        List<FileItem> fileList = new ArrayList<>();
        File extDownloadDir = FileUtils.getFolder(selectedFolder);
        if (extDownloadDir == null || !extDownloadDir.exists()) {
            return fileList;
        }

        File[] files = extDownloadDir.listFiles();
        if (files == null) {
            return fileList;
        }

        for (File file : files) {
            String fileName = file.getName();
            long fileLength = file.length();
            String fileSize = formatFileSize(fileLength);
            String fileType = getFileType(fileName);
            String previewUri = file.toURI().toString(); // 文件路径作为预览 URI
            long lastModified = file.lastModified();
            
            if (fileType.equals("MP4")) {
                fileList.add(new FileItem(fileName, fileSize, fileType, previewUri, fileLength, lastModified));
            }
        }
        return fileList;
    }

    private void setupSearchListener() {
        searchInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                fileAdapter.getFilter().filter(s);
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });
    }

    private void setupSortListener() {
        sortGroup.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.sort_name) {
                fileAdapter.sortByName();
            } else if (checkedId == R.id.sort_date) {
                fileAdapter.sortByDate();
            } else if (checkedId == R.id.sort_size) {
                fileAdapter.sortBySize();
            }
        });
    }

    /**
     * 显示视频播放器弹窗
     */
    private void showVideoPlayer(FileItem fileItem) {
        // 初始化播放器
        if (exoPlayer == null) {
            exoPlayer = new ExoPlayer.Builder(requireContext()).build();
            popupVideoPlayer.setPlayer(exoPlayer);
            
            // 设置播放器监听器以支持全屏播放
            exoPlayer.addListener(new Player.Listener() {
                @Override
                public void onPlaybackStateChanged(int state) {
                    if (state == Player.STATE_READY) {
                        // 视频准备就绪，可以考虑显示全屏按钮等
                        popupVideoPlayer.setControllerShowTimeoutMs(3000); // 控制器显示3秒后自动隐藏
                        popupVideoPlayer.setControllerHideOnTouch(true);
                    }
                }
            });
        }
        
        // 设置视频标题，截断长文件名
        String displayTitle = fileItem.getFileName();
        if (displayTitle.length() > 30) {
            displayTitle = displayTitle.substring(0, 27) + "...";
        }
        popupVideoTitle.setText(displayTitle);
        
        // 加载视频
        MediaItem mediaItem = MediaItem.fromUri(fileItem.getPreviewUri());
        exoPlayer.setMediaItem(mediaItem);
        exoPlayer.prepare();
        exoPlayer.play();
        
        // 显示播放器容器
        videoPlayerContainer.setVisibility(View.VISIBLE);
    }
    
    /**
     * 隐藏视频播放器弹窗
     */
    private void hideVideoPlayer() {
        if (exoPlayer != null) {
            exoPlayer.stop();
        }
        videoPlayerContainer.setVisibility(View.GONE);
    }

    private String getFileType(String fileName) {
        if (fileName.endsWith(".mp4")) {
            return "MP4";
        } else if (fileName.endsWith(".png") || fileName.endsWith(".jpg") || fileName.endsWith(".jpeg")) {
            return "IMAGE";
        } else {
            return "OTHER";
        }
    }

    private String formatFileSize(long size) {
        if (size <= 0) return "0 B";
        final String[] units = new String[]{"B", "KB", "MB", "GB", "TB"};
        int digitGroups = (int) (Math.log10(size) / Math.log10(1024));
        return new DecimalFormat("#,##0.#").format(size / Math.pow(1024, digitGroups)) + " " + units[digitGroups];
    }

    @Override
    public void onPause() {
        super.onPause();
        releasePlayer();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        releasePlayer();
        
        // 销毁适配器资源
        if (fileAdapter != null) {
            fileAdapter.destroy();
        }
        
        // 关闭文本提取工具资源
        TextExtractorUtils.shutdown();
    }

    public void releasePlayer() {
        if (fileAdapter != null) {
            fileAdapter.releasePlayer(); // 释放适配器中的播放器
        }
        
        if (exoPlayer != null) {
            exoPlayer.release();
            exoPlayer = null;
        }
    }
}