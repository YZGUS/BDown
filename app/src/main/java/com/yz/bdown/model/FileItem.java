package com.yz.bdown.model;

public class FileItem {
    private String fileName;
    private String fileSize;
    private String fileType;
    private String previewUri; // 图片或视频的资源 URI

    public FileItem(String fileName, String fileSize, String fileType, String previewUri) {
        this.fileName = fileName;
        this.fileSize = fileSize;
        this.fileType = fileType;
        this.previewUri = previewUri;
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

    @Override
    public String toString() {
        return "FileItem{" +
                "fileName='" + fileName + '\'' +
                ", fileSize='" + fileSize + '\'' +
                ", fileType='" + fileType + '\'' +
                ", previewUri='" + previewUri + '\'' +
                '}';
    }
}