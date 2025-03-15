package com.yz.bdown.fragment.bilibili;

import android.os.Bundle;

import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.yz.bdown.R;
import com.yz.bdown.adapter.BilibiliFilePartAdapter;
import com.yz.bdown.model.FileItem;
import com.yz.bdown.utils.FileUtils;

import java.io.File;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

public class BilibiliFolderFragment extends Fragment {

    private RecyclerView fileRecyclerView;
    private BilibiliFilePartAdapter fileAdapter;
    private String selectedFolder = "bilibiliDown"; // 默认文件夹

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_bilibili_folder, container, false);
        fileRecyclerView = view.findViewById(R.id.bff_file_list);

        // 设置 RecyclerView
        fileRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        fileAdapter = new BilibiliFilePartAdapter(getFilesFromExternalMovies());
        fileRecyclerView.setAdapter(fileAdapter);
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
            String fileSize = formatFileSize(file.length());
            String fileType = getFileType(fileName);
            String previewUri = file.toURI().toString(); // 文件路径作为预览 URI
            if (fileType.equals("MP4")) {
                fileList.add(new FileItem(fileName, fileSize, fileType, previewUri));
            }
        }
        return fileList;
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
        if (fileAdapter != null) {
            fileAdapter.releasePlayer(); // 释放播放器
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (fileAdapter != null) {
            fileAdapter.releasePlayer(); // 释放播放器
        }
    }

    public void releasePlayer() {
        if (fileAdapter != null) {
            fileAdapter.releasePlayer(); // 释放播放器
        }
    }
}