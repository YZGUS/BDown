package com.yz.bdown.utils;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.core.content.FileProvider;

import com.yz.bdown.R;

import java.io.File;

/**
 * 通知助手类，用于显示下载完成后的通知提示
 */
public class NotificationHelper {

    private static final String TAG = "NotificationHelper";
    private static AlertDialog currentDialog = null;

    /**
     * 显示下载完成通知
     *
     * @param context     上下文
     * @param title       通知标题
     * @param message     通知消息
     * @param downloadedFile 下载的文件
     */
    public static void showDownloadCompleteNotification(Context context, String title, String message, File downloadedFile) {
        // 确保在主线程运行
        new Handler(Looper.getMainLooper()).post(() -> {
            if (context instanceof Activity && ((Activity) context).isFinishing()) {
                return;
            }

            // 如果有正在显示的对话框，先关闭
            dismissCurrentDialog();

            View notificationView = LayoutInflater.from(context).inflate(R.layout.download_notification, null);
            
            // 设置标题和消息
            TextView titleTextView = notificationView.findViewById(R.id.notification_title);
            TextView messageTextView = notificationView.findViewById(R.id.notification_message);
            Button openButton = notificationView.findViewById(R.id.notification_open);
            Button dismissButton = notificationView.findViewById(R.id.notification_dismiss);
            
            titleTextView.setText(title);
            messageTextView.setText(message);
            
            // 创建对话框
            AlertDialog.Builder builder = new AlertDialog.Builder(context)
                    .setView(notificationView)
                    .setCancelable(true);
            
            currentDialog = builder.create();
            
            // 设置按钮点击事件
            openButton.setOnClickListener(v -> {
                if (downloadedFile != null && downloadedFile.exists()) {
                    openFile(context, downloadedFile);
                }
                dismissCurrentDialog();
            });
            
            dismissButton.setOnClickListener(v -> dismissCurrentDialog());
            
            // 显示对话框
            currentDialog.show();
        });
    }

    /**
     * 关闭当前显示的对话框
     */
    private static void dismissCurrentDialog() {
        if (currentDialog != null && currentDialog.isShowing()) {
            currentDialog.dismiss();
            currentDialog = null;
        }
    }

    /**
     * 打开下载的文件
     */
    private static void openFile(Context context, File file) {
        try {
            String authority = context.getPackageName() + ".fileprovider";
            Uri fileUri = FileProvider.getUriForFile(context, authority, file);
            
            Intent intent = new Intent(Intent.ACTION_VIEW);
            
            // 根据文件类型设置MIME类型
            String mimeType;
            String fileName = file.getName().toLowerCase();
            
            if (fileName.endsWith(".mp4") || fileName.endsWith(".m4v")) {
                mimeType = "video/mp4";
            } else if (fileName.endsWith(".mp3") || fileName.endsWith(".aac") || fileName.endsWith(".flac")) {
                mimeType = "audio/*";
            } else {
                mimeType = "*/*";
            }
            
            intent.setDataAndType(fileUri, mimeType);
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            
            context.startActivity(intent);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
} 