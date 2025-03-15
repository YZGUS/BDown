package com.yz.bdown.utils;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.FileProvider;

import com.yz.bdown.R;

import java.io.File;
import java.util.Random;

public class SystemNotificationUtils {

    private static final String CHANNEL_ID = "download_notification";
    private static final String CHANNEL_NAME = "下载通知";
    private static final String CHANNEL_DESC = "显示文件下载完成的通知";

    /**
     * 创建通知渠道，在应用启动时调用
     */
    public static void createNotificationChannel(Context context) {
        // 在Android 8.0及以上版本需要创建通知渠道
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    CHANNEL_NAME,
                    NotificationManager.IMPORTANCE_DEFAULT
            );
            channel.setDescription(CHANNEL_DESC);

            // 注册通知渠道
            NotificationManager notificationManager = context.getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
        }
    }

    /**
     * 发送下载完成通知
     */
    public static void sendDownloadCompleteNotification(Context context, String title, String message, File downloadedFile) {
        // 创建打开文件的意图
        Intent openIntent = new Intent(Intent.ACTION_VIEW);
        String authority = context.getPackageName() + ".fileprovider";
        Uri fileUri = FileProvider.getUriForFile(context, authority, downloadedFile);

        // 设置MIME类型
        String mimeType;
        String fileName = downloadedFile.getName().toLowerCase();
        if (fileName.endsWith(".mp4") || fileName.endsWith(".m4v")) {
            mimeType = "video/mp4";
        } else if (fileName.endsWith(".mp3") || fileName.endsWith(".aac") || fileName.endsWith(".flac")) {
            mimeType = "audio/*";
        } else {
            mimeType = "*/*";
        }

        openIntent.setDataAndType(fileUri, mimeType);
        openIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

        // 创建PendingIntent
        PendingIntent pendingIntent = PendingIntent.getActivity(
                context,
                0,
                openIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        // 构建通知
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_download_complete)
                .setContentTitle(title)
                .setContentText(message)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true) // 点击后自动关闭
                .setStyle(new NotificationCompat.BigTextStyle().bigText(message));

        // 发送通知
        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(context);
        try {
            // 生成唯一通知ID
            int notificationId = new Random().nextInt(1000);
            notificationManager.notify(notificationId, builder.build());
        } catch (SecurityException e) {
            // 缺少通知权限的处理
            e.printStackTrace();
        }
    }
} 