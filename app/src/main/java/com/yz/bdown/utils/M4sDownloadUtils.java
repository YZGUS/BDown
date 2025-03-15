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

    public static boolean downloadM4sFile(String url, File outputFile) {
        if (isBlank(url) || outputFile == null) {
            Log.e(TAG, "invalid params");
            return false;
        }

        Request request = new Request.Builder()
                .url(url)
                .header("Referer", "https://www.bilibili.com")
                .build();
        try (Response response = CLIENT.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                Log.e(TAG, "Download failed: " + response.code() + " " + response.message());
                return false;
            }

            ResponseBody body = response.body();
            if (body == null) {
                Log.e(TAG, "Download failed: body=" + body);
                return false;
            }

            try (InputStream inputStream = body.byteStream();
                 FileOutputStream outputStream = new FileOutputStream(outputFile)) {
                byte[] buffer = new byte[4096];
                int bytesRead;
                while ((bytesRead = inputStream.read(buffer)) != -1) {
                    outputStream.write(buffer, 0, bytesRead);
                }
                return true;
            }
        } catch (Throwable t) {
            Log.e(TAG, "Download failed", t);
            return false;
        }
    }
}