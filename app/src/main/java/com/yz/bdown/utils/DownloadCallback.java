package com.yz.bdown.utils;

/**
 * 下载进度回调接口
 */
public interface DownloadCallback {
    
    /**
     * 下载开始
     * @param totalBytes 总字节数
     * @param fileName 文件名
     */
    void onDownloadStart(long totalBytes, String fileName);
    
    /**
     * 下载进度更新
     * @param bytesRead 已读取字节数
     * @param totalBytes 总字节数
     * @param speed 下载速度(KB/s)
     */
    void onProgressUpdate(long bytesRead, long totalBytes, double speed);
    
    /**
     * 下载完成
     * @param fileName 文件名
     * @param filePath 文件路径
     */
    void onDownloadComplete(String fileName, String filePath);
    
    /**
     * 下载失败
     * @param errorMessage 错误信息
     */
    void onDownloadError(String errorMessage);
} 