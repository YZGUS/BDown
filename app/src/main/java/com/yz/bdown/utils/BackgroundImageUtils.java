package com.yz.bdown.utils;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.drawable.Drawable;
import android.util.Log;
import android.view.View;

import com.yz.bdown.R;

import java.io.File;
import java.io.FileOutputStream;

/**
 * 背景图片辅助类，用于加载和处理背景图片
 */
public class BackgroundImageUtils {
    private static final String TAG = "BackgroundImageHelper";
    
    /**
     * 在应用启动时初始化背景图片资源
     */
    public static void initBackgroundImage(Context context) {
        try {
            // 检查图片是否已经存在
            File bgFile = new File(context.getFilesDir(), "bg_scream.png");
            if (!bgFile.exists()) {
                // 从资源创建位图
                Bitmap originalBitmap = BitmapFactory.decodeResource(context.getResources(), R.drawable.ic_app);
                if (originalBitmap == null) {
                    // 使用默认图片
                    createDefaultBackgroundImage(context, bgFile);
                    return;
                }
                
                // 保存图片到文件
                FileOutputStream fos = new FileOutputStream(bgFile);
                originalBitmap.compress(Bitmap.CompressFormat.PNG, 100, fos);
                fos.close();
            }
        } catch (Exception e) {
            Log.e(TAG, "Error initializing background image", e);
        }
    }
    
    /**
     * 创建默认的背景图片
     */
    private static void createDefaultBackgroundImage(Context context, File outputFile) {
        try {
            // 使用提供的"尖叫"图片
            Bitmap bitmap = createScreamBitmap();
            if (bitmap != null) {
                FileOutputStream fos = new FileOutputStream(outputFile);
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, fos);
                fos.close();
            }
        } catch (Exception e) {
            Log.e(TAG, "Error creating default background", e);
        }
    }
    
    /**
     * 设置透明背景
     */
    public static void setTransparentBackground(View view, float alpha) {
        try {
            Drawable background = view.getBackground();
            if (background != null) {
                background.setAlpha((int) (alpha * 255));
            }
        } catch (Exception e) {
            Log.e(TAG, "Error setting transparent background", e);
        }
    }
    
    /**
     * 获取尖叫图片位图
     */
    private static Bitmap createScreamBitmap() {
        // 这里我们可以创建一个简单的渐变背景
        Bitmap bitmap = Bitmap.createBitmap(800, 1200, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        
        // 使用蓝色调底色
        Paint paint = new Paint();
        paint.setColor(0xFFE9ECF5); // 浅蓝色背景
        canvas.drawRect(0, 0, 800, 1200, paint);
        
        // 添加一些波浪线条
        paint.setColor(0xFF4A6DB3); // 主题蓝色
        paint.setStrokeWidth(20);
        paint.setAlpha(100);
        
        for (int i = 0; i < 10; i++) {
            canvas.drawLine(0, 200 + i * 100, 800, 150 + i * 100, paint);
        }
        
        return bitmap;
    }
} 