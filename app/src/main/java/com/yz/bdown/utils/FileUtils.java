package com.yz.bdown.utils;

import static android.os.Environment.DIRECTORY_DOWNLOADS;

import android.content.Context;
import android.net.Uri;
import android.os.Environment;
import android.util.Log;

import androidx.core.content.FileProvider;

import java.io.File;

/**
 * 文件工具类
 * 处理文件创建、获取和URI转换等操作
 */
public class FileUtils {

    private static final String TAG = "FileUtils";

    /**
     * 在指定子文件夹创建文件
     *
     * @param fileName  文件名
     * @param subfolder 子文件夹名称
     * @return 创建的文件对象，如果创建失败则返回null
     */
    public static File toFile(String fileName, String subfolder) {
        File subFolderDir = getFolder(subfolder);
        if (subFolderDir == null) {
            Log.e(TAG, "Failed to get subfolder directory");
            return null;
        }

        if (!subFolderDir.exists()) {
            if (subFolderDir.mkdirs()) {
                Log.d(TAG, "子文件夹已创建: " + subFolderDir.getAbsolutePath());
            } else {
                Log.e(TAG, "创建子文件夹失败");
                return null;
            }
        }

        File outputFile = new File(subFolderDir, fileName);
        try {
            if (!outputFile.exists() && outputFile.createNewFile()) {
                Log.d(TAG, "文件已创建: " + outputFile.getAbsolutePath());
            } else {
                Log.d(TAG, "文件已存在: " + outputFile.getAbsolutePath());
            }
            return outputFile;
        } catch (Throwable t) {
            Log.e(TAG, "创建文件失败: " + t.getMessage());
            return null;
        }
    }

    /**
     * 获取下载目录中的指定子文件夹
     *
     * @param subfolder 子文件夹名称
     * @return 子文件夹对象，如果获取失败则返回null
     */
    public static File getFolder(String subfolder) {
        File storageDir = Environment.getExternalStoragePublicDirectory(DIRECTORY_DOWNLOADS);
        if (storageDir == null) {
            Log.e(TAG, "外部存储目录不可用");
            return null;
        }
        return new File(storageDir, subfolder);
    }

    /**
     * 获取文件的URI，用于分享和访问
     *
     * @param context  上下文
     * @param folder   文件所在文件夹
     * @param fileName 文件名
     * @return 文件的URI，如果文件不存在则返回null
     */
    public static Uri getFileUri(Context context, File folder, String fileName) {
        // 检查文件夹和文件名是否有效
        if (folder == null || fileName == null || fileName.isEmpty()) {
            Log.e(TAG, "文件夹或文件名无效");
            return null;
        }

        // 构建文件对象
        File file = new File(folder, fileName);

        // 检查文件是否存在
        if (!file.exists()) {
            Log.e(TAG, "文件不存在: " + file.getAbsolutePath());
            return null;
        }

        try {
            // 使用 FileProvider 获取文件的 Uri
            return FileProvider.getUriForFile(
                    context,
                    context.getPackageName() + ".fileprovider", // 与 AndroidManifest.xml 中的 authority 一致
                    file
            );
        } catch (Exception e) {
            Log.e(TAG, "获取文件URI失败: " + e.getMessage());
            return null;
        }
    }

    /**
     * 清理文件名，移除非法字符
     *
     * @param fileName 原始文件名
     * @return 清理后的文件名
     */
    public static String sanitizeFileName(String fileName) {
        if (fileName == null) {
            return "file";
        }

        // 移除文件名中的非法字符
        return fileName
                .replaceAll("[\\\\/:*?\"<>|]", "_") // 替换Windows/Unix文件系统中的非法字符
                .replaceAll("\\s+", "_")            // 替换连续空格为单个下划线
                .trim();
    }

    /**
     * 检查文件是否存在
     *
     * @param filePath 文件路径
     * @return 文件是否存在
     */
    public static boolean fileExists(String filePath) {
        if (filePath == null || filePath.isEmpty()) {
            return false;
        }

        File file = new File(filePath);
        return file.exists() && file.isFile();
    }
}
