package com.yz.bdown.api;

import static com.alibaba.fastjson2.JSON.parseObject;
import static com.yz.bdown.contents.BilibiliConstants.KEY_BILI_JCT;
import static com.yz.bdown.contents.BilibiliConstants.KEY_SESSDATA;
import static com.yz.bdown.utils.QRCodeUtil.generateQRCodeBitmap;
import static org.apache.commons.lang3.StringUtils.isBlank;
import static java.lang.String.format;
import static java.util.concurrent.TimeUnit.MINUTES;

import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.util.Log;

import com.alibaba.fastjson2.JSONObject;
import com.yz.bdown.model.BilibiliBaseResp;

import kotlin.Pair;
import okhttp3.Headers;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class BilibiliLoginApi {

    private static final String TAG = "BilibiliLoginApi";

    private static final OkHttpClient CLIENT = new OkHttpClient.Builder()
            .connectTimeout(10, MINUTES)
            .readTimeout(10, MINUTES)
            .build();

    private static final String LOGIN_QRCODE =
            "https://passport.bilibili.com/x/passport-login/web/qrcode/generate";

    public static final String LOGIN_COOKIE =
            "https://passport.bilibili.com/x/passport-login/web/qrcode/poll?qrcode_key=%s";

    private final SharedPreferences sharedPreferences;

    private String qrCodeKey;

    public BilibiliLoginApi(SharedPreferences sharedPreferences) {
        this.sharedPreferences = sharedPreferences;
        this.qrCodeKey = "";
    }

    public Bitmap getQRCode() {
        final Request request = new Request.Builder()
                .url(LOGIN_QRCODE)
                .get()
                .build();
        try (Response response = CLIENT.newCall(request).execute()) {
            if (!response.isSuccessful() || response.body() == null) {
                Log.w(TAG, "响应结果错误, response=" + response);
                return null;
            }

            String respBody = response.body().string();
            final BilibiliBaseResp baseResp = parseObject(respBody, BilibiliBaseResp.class);
            if (baseResp == null || baseResp.getCode() != 0) {
                Log.w(TAG, "响应结果错误, baseResp=" + respBody);
                return null;
            }

            final JSONObject data = (JSONObject) baseResp.getData();
            final String qrCodeUrl = data.getString("url");
            final String qrCodeKey = data.getString("qrcode_key");
            if (isBlank(qrCodeUrl) || isBlank(qrCodeKey)) {
                Log.w(TAG, "响应结果错误, qrCodeUrl=" + qrCodeUrl + ", qrCodeKey=" + qrCodeKey);
            }

            this.qrCodeKey = qrCodeKey;
            return generateQRCodeBitmap(qrCodeUrl, 300, 300);
        } catch (Throwable t) {
            Log.e(TAG, "初始化登录二维码错误", t);
            return null;
        }
    }

    public boolean isScanned() {
        final Request request = new Request.Builder()
                .url(format(LOGIN_COOKIE, qrCodeKey))
                .get()
                .build();
        try (Response response = CLIENT.newCall(request).execute()) {
            if (response.isSuccessful() && response.body() != null) {
                String respBody = response.body().string();
                final BilibiliBaseResp baseResp = parseObject(respBody, BilibiliBaseResp.class);
                if (baseResp == null || baseResp.getCode() != 0) {
                    Log.w(TAG, "响应结果错误, baseResp=" + respBody);
                    return false;
                }

                final JSONObject data = (JSONObject) baseResp.getData();
                if (data.getIntValue("code", -1) != 0) {
                    return false;
                }

                Headers headers = response.headers();
                return parseSessdata(headers) && parseBiliJct(headers);
            }
            return false;
        } catch (Throwable t) {
            Log.e(TAG, "扫码异常", t);
            return false;
        }
    }

    private boolean parseSessdata(Headers headers) {
        for (Pair<? extends String, ? extends String> header : headers) {
            String key = header.getFirst();
            String value = header.getSecond();
            if (key.startsWith("set-cookie") && value.startsWith("SESSDATA=")) {
                final int startIndex = "SESSDATA=".length();
                final int endIndex = value.indexOf(';', startIndex);
                if (endIndex > startIndex) {
                    saveLocal(KEY_SESSDATA, value.substring("SESSDATA=".length(), endIndex));
                    return true;
                }
                return false;
            }
        }
        return false;
    }

    private void saveLocal(String key, String value) {
        final SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString(key, value);
        editor.apply();
    }

    private boolean parseBiliJct(Headers headers) {
        for (Pair<? extends String, ? extends String> header : headers) {
            String key = header.getFirst();
            String value = header.getSecond();
            if (key.startsWith("set-cookie") && value.startsWith("bili_jct=")) {
                final int startIndex = "bili_jct=".length();
                final int endIndex = value.indexOf(';', startIndex);
                if (endIndex > startIndex) {
                    saveLocal(KEY_BILI_JCT, value.substring("bili_jct=".length(), endIndex));
                    return true;
                }
                return false;
            }
        }
        return false;
    }
}
