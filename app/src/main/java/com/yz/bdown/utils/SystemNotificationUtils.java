package com.yz.bdown.utils;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.util.Log;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.FileProvider;

import com.yz.bdown.R;

import java.io.File;
import java.util.Random;

/**
 * 系统通知工具类
 * 用于创建通知渠道和发送系统通知
 */
public class SystemNotificationUtils {

    private static final String TAG = "SystemNotificationUtils";
    private static final String CHANNEL_ID = "download_notification";
    private static final String CHANNEL_NAME = "下载通知";
    private static final String CHANNEL_DESC = "显示文件下载完成的通知";

    /**
     * 创建通知渠道，在应用启动时调用
     *
     * @param context 上下文
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
            if (notificationManager != null) {
                notificationManager.createNotificationChannel(channel);
                Log.d(TAG, "通知渠道已创建");
            } else {
                Log.e(TAG, "无法获取NotificationManager服务");
            }
        }
    }

    /**
     * 发送下载完成通知（通过文件路径）
     *
     * @param context  上下文
     * @param title    通知标题
     * @param message  通知内容
     * @param filePath 文件路径
     */
    public static void sendDownloadCompleteNotification(Context context, String title, String message, String filePath) {
        if (filePath == null || filePath.isEmpty()) {
            Log.e(TAG, "文件路径为空，无法发送通知");
            return;
        }

        File file = new File(filePath);
        if (!file.exists()) {
            Log.e(TAG, "文件不存在: " + filePath);
            return;
        }

        sendDownloadCompleteNotification(context, title, message, file);
    }

    /**
     * 发送下载完成通知
     *
     * @param context        上下文
     * @param title          通知标题
     * @param message        通知内容
     * @param downloadedFile 下载的文件
     */
    public static void sendDownloadCompleteNotification(Context context, String title, String message, File downloadedFile) {
        if (context == null || downloadedFile == null || !downloadedFile.exists()) {
            Log.e(TAG, "参数无效或文件不存在");
            return;
        }

        try {
            // 创建打开文件的意图
            Intent openIntent = new Intent(Intent.ACTION_VIEW);
            String authority = context.getPackageName() + ".fileprovider";
            Uri fileUri = FileProvider.getUriForFile(context, authority, downloadedFile);

            // 设置MIME类型
            String mimeType = getMimeType(downloadedFile.getName());
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
            // 生成唯一通知ID
            int notificationId = new Random().nextInt(1000);
            notificationManager.notify(notificationId, builder.build());
            Log.d(TAG, "通知已发送，ID: " + notificationId);
        } catch (SecurityException e) {
            // 缺少通知权限的处理
            Log.e(TAG, "缺少通知权限", e);
        } catch (Exception e) {
            Log.e(TAG, "发送通知时出错", e);
        }
    }

    /**
     * 根据文件名获取MIME类型
     *
     * @param fileName 文件名
     * @return MIME类型字符串
     */
    private static String getMimeType(String fileName) {
        if (fileName == null) {
            return "*/*";
        }

        String lowerCaseFileName = fileName.toLowerCase();
        if (lowerCaseFileName.endsWith(".mp4") || lowerCaseFileName.endsWith(".m4v")) {
            return "video/mp4";
        } else if (lowerCaseFileName.endsWith(".mp3") || lowerCaseFileName.endsWith(".aac") || lowerCaseFileName.endsWith(".flac")) {
            return "audio/*";
        } else {
            return "*/*";
        }
    }
} 