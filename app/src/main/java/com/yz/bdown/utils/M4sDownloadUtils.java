package com.yz.bdown.utils;

import static org.apache.commons.lang3.StringUtils.isBlank;
import static java.util.concurrent.TimeUnit.MINUTES;

import android.util.Log;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;

public class M4sDownloadUtils {

    private static final String TAG = "M4sDownloadUtils";

    private static final OkHttpClient CLIENT = new OkHttpClient.Builder()
            .connectTimeout(10, MINUTES)
            .readTimeout(10, MINUTES)
            .build();

    /**
     * 下载M4S文件
     *
     * @param url        下载地址
     * @param outputFile 输出文件
     * @return 是否下载成功
     */
    public static boolean downloadM4sFile(String url, File outputFile) {
        return downloadM4sFile(url, outputFile, null);
    }

    /**
     * 下载M4S文件（带进度回调）
     *
     * @param url        下载地址
     * @param outputFile 输出文件
     * @param callback   进度回调
     * @return 是否下载成功
     */
    public static boolean downloadM4sFile(String url, File outputFile, DownloadCallback callback) {
        if (isBlank(url) || outputFile == null) {
            Log.e(TAG, "invalid params");
            if (callback != null) {
                callback.onDownloadError("无效的下载参数");
            }
            return false;
        }

        Request request = new Request.Builder()
                .url(url)
                .header("Referer", "https://www.bilibili.com")
                .build();
        try (Response response = CLIENT.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                String errorMsg = "下载失败: " + response.code() + " " + response.message();
                Log.e(TAG, errorMsg);
                if (callback != null) {
                    callback.onDownloadError(errorMsg);
                }
                return false;
            }

            ResponseBody body = response.body();
            if (body == null) {
                String errorMsg = "下载失败: 响应体为空";
                Log.e(TAG, errorMsg);
                if (callback != null) {
                    callback.onDownloadError(errorMsg);
                }
                return false;
            }

            long totalBytes = body.contentLength();
            if (callback != null) {
                callback.onDownloadStart(totalBytes, outputFile.getName());
            }

            try (InputStream inputStream = body.byteStream();
                 FileOutputStream outputStream = new FileOutputStream(outputFile)) {

                byte[] buffer = new byte[4096];
                int bytesRead;
                long totalBytesRead = 0;
                long startTime = System.currentTimeMillis();
                long lastProgressUpdate = 0;

                while ((bytesRead = inputStream.read(buffer)) != -1) {
                    outputStream.write(buffer, 0, bytesRead);
                    totalBytesRead += bytesRead;

                    if (callback != null) {
                        long currentTime = System.currentTimeMillis();
                        // 每300毫秒更新一次进度，避免过于频繁的UI更新
                        if (currentTime - lastProgressUpdate > 300) {
                            double elapsedTimeInSeconds = (currentTime - startTime) / 1000.0;
                            double speedKBps = totalBytesRead / 1024.0 / Math.max(elapsedTimeInSeconds, 0.1);

                            callback.onProgressUpdate(totalBytesRead, totalBytes, speedKBps);
                            lastProgressUpdate = currentTime;
                        }
                    }
                }

                return true;
            }
        } catch (Throwable t) {
            String errorMsg = "下载失败: " + t.getMessage();
            Log.e(TAG, errorMsg, t);
            if (callback != null) {
                callback.onDownloadError(errorMsg);
            }
            return false;
        }
    }
}