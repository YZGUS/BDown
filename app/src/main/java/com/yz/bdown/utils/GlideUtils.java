package com.yz.bdown.utils;

import android.content.Context;
import android.util.Log;
import android.widget.ImageView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.load.model.GlideUrl;
import com.bumptech.glide.load.model.LazyHeaders;
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions;
import com.bumptech.glide.request.RequestOptions;
import com.yz.bdown.R;

/**
 * Glide图片加载工具类
 * 专门处理B站图片加载，添加必要的请求头和URL修复
 */
public class GlideUtils {
    private static final String TAG = "GlideUtils";

    /**
     * 加载B站图片
     *
     * @param context   上下文
     * @param url       图片URL
     * @param imageView 要加载到的ImageView
     */
    public static void loadBilibiliImage(Context context, String url, ImageView imageView) {
        if (context == null || url == null || url.isEmpty() || imageView == null) {
            Log.w(TAG, "参数无效，无法加载图片");
            return;
        }

        Log.d(TAG, "原始图片URL: " + url);

        // 修复图片URL
        String fixedUrl = fixBilibiliImageUrl(url);
        Log.d(TAG, "修复后图片URL: " + fixedUrl);

        // 添加必要的请求头
        GlideUrl glideUrl = new GlideUrl(fixedUrl, new LazyHeaders.Builder()
                .addHeader("Referer", "https://www.bilibili.com")
                .addHeader("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36")
                .build());

        // 使用Glide加载图片
        Glide.with(context)
                .load(glideUrl)
                .apply(new RequestOptions()
                        .placeholder(R.drawable.ic_no_login)
                        .error(R.drawable.ic_no_login)
                        .diskCacheStrategy(DiskCacheStrategy.ALL)
                        .centerCrop())
                .transition(DrawableTransitionOptions.withCrossFade())
                .into(imageView);
    }

    /**
     * 修复B站图片URL
     * 处理协议、域名等问题，确保URL可以正常访问
     *
     * @param url 原始URL
     * @return 修复后的URL
     */
    private static String fixBilibiliImageUrl(String url) {
        if (url == null || url.isEmpty()) {
            return url;
        }

        // 确保URL使用HTTPS
        if (url.startsWith("http://")) {
            url = url.replace("http://", "https://");
        } else if (!url.startsWith("https://")) {
            url = "https://" + url;
        }

        // 处理特殊情况 - i0.hdslb.com 域名
        if (url.contains("i0.hdslb.com")) {
            // 尝试将i0.hdslb.com替换为i2.hdslb.com，这可能解决一些CDN问题
            url = url.replace("i0.hdslb.com", "i2.hdslb.com");
        }

        return url;
    }
} 