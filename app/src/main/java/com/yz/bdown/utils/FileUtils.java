package com.yz.bdown.utils;

import static android.os.Environment.DIRECTORY_DOWNLOADS;

import android.content.Context;
import android.net.Uri;
import android.os.Environment;
import android.util.Log;

import androidx.core.content.FileProvider;

import java.io.File;

public class FileUtils {

    private static final String TAG = "FileUtils";

    public static File toFile(String fileName, String subfolder) {
        File subFolderDir = getFolder(subfolder);
        if (!subFolderDir.exists()) {
            if (subFolderDir.mkdirs()) {
                Log.d(TAG, "sub folder created: " + subFolderDir.getAbsolutePath());
            } else {
                Log.e(TAG, "Failed to create sub folder");
                return null;
            }
        }

        File outputFile = new File(subFolderDir, fileName);
        try {
            if (outputFile.createNewFile()) {
                Log.d(TAG, "File created: " + outputFile.getAbsolutePath());
            } else {
                Log.d(TAG, "File already exists: " + outputFile.getAbsolutePath());
            }
            return outputFile;
        } catch (Throwable t) {
            Log.e(TAG, "Failed to create file: " + t.getMessage());
            return null;
        }
    }

    public static File getFolder(String subfolder) {
        File storageDir = Environment.getExternalStoragePublicDirectory(DIRECTORY_DOWNLOADS);
        if (storageDir == null) {
            Log.e(TAG, "External storage directory is not available");
            return null;
        }
        return new File(storageDir, subfolder);
    }

    public static Uri getFileUri(Context context, File folder, String fileName) {
        // 检查文件夹和文件名是否有效
        if (folder == null || fileName == null || fileName.isEmpty()) {
            return null;
        }

        // 构建文件对象
        File file = new File(folder, fileName);

        // 检查文件是否存在
        if (!file.exists()) {
            return null;
        }

        // 使用 FileProvider 获取文件的 Uri
        return FileProvider.getUriForFile(
                context,
                context.getPackageName() + ".fileprovider", // 与 AndroidManifest.xml 中的 authority 一致
                file
        );
    }
}
