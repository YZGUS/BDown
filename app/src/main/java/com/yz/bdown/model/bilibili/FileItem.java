package com.yz.bdown.model.bilibili;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class FileItem {
    private String fileName;
    private String fileSize;
    private String fileType;
    private String previewUri; // 图片或视频的资源 URI
    private long fileSizeBytes; // 文件大小（字节数）用于排序
    private long lastModifiedTimestamp; // 文件最后修改时间戳
    private String lastModified; // 格式化的最后修改时间

    public FileItem(String fileName, String fileSize, String fileType, String previewUri) {
        this.fileName = fileName;
        this.fileSize = fileSize;
        this.fileType = fileType;
        this.previewUri = previewUri;
    }

    public FileItem(String fileName, String fileSize, String fileType, String previewUri,
                    long fileSizeBytes, long lastModifiedTimestamp) {
        this.fileName = fileName;
        this.fileSize = fileSize;
        this.fileType = fileType;
        this.previewUri = previewUri;
        this.fileSizeBytes = fileSizeBytes;
        this.lastModifiedTimestamp = lastModifiedTimestamp;

        // 格式化日期 - 只显示年月日
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        this.lastModified = sdf.format(new Date(lastModifiedTimestamp));
    }

    public String getFileName() {
        return fileName;
    }

    public String getFileSize() {
        return fileSize;
    }

    public String getFileType() {
        return fileType;
    }

    public String getPreviewUri() {
        return previewUri;
    }

    public long getFileSizeBytes() {
        return fileSizeBytes;
    }

    public long getLastModifiedTimestamp() {
        return lastModifiedTimestamp;
    }

    public String getLastModified() {
        return lastModified;
    }

    @Override
    public String toString() {
        return "FileItem{" +
                "fileName='" + fileName + '\'' +
                ", fileSize='" + fileSize + '\'' +
                ", fileType='" + fileType + '\'' +
                ", previewUri='" + previewUri + '\'' +
                ", fileSizeBytes=" + fileSizeBytes +
                ", lastModified='" + lastModified + '\'' +
                '}';
    }
}