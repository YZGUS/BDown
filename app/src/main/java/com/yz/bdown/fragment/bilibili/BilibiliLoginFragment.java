package com.yz.bdown.fragment.bilibili;

import static android.content.Context.MODE_PRIVATE;
import static android.os.Looper.getMainLooper;
import static android.view.View.GONE;
import static android.view.View.VISIBLE;
import static com.google.android.material.snackbar.BaseTransientBottomBar.LENGTH_SHORT;

import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;

import androidx.fragment.app.Fragment;

import com.google.android.material.snackbar.Snackbar;
import com.yz.bdown.R;
import com.yz.bdown.api.BilibiliLoginApi;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class BilibiliLoginFragment extends Fragment {

    private static final ExecutorService EXECUTOR = Executors.newSingleThreadExecutor();

    private static final String TAG = "BilibiliLoginFragment";

    private Button loginBtn;
    private Button scannedBtn;
    private ImageView qrcodeImage;
    private BilibiliLoginApi bilibiliLoginApi;
    private Handler handler;


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_bilibili_login, container, false);
        loginBtn = view.findViewById(R.id.blf_btn_login);
        scannedBtn = view.findViewById(R.id.blf_btn_scanned);
        qrcodeImage = view.findViewById(R.id.blf_image_qrcode);
        bilibiliLoginApi = new BilibiliLoginApi(requireActivity().getSharedPreferences("Bilibili", MODE_PRIVATE));
        handler = new Handler(getMainLooper());

        // 设置点击回调
        loginBtn.setOnClickListener(this::login);
        scannedBtn.setOnClickListener(this::scanned);
        return view;
    }

    private void login(View v) {
        CompletableFuture
                .supplyAsync(() -> bilibiliLoginApi.getQRCode(), EXECUTOR)
                .thenAccept(bitmap -> {
                    if (bitmap != null) {
                        Snackbar.make(v, "请在 3 分钟内扫码", LENGTH_SHORT).show();
                        handler.post(() -> {
                            loginBtn.setVisibility(GONE);
                            scannedBtn.setVisibility(VISIBLE);
                            qrcodeImage.setImageBitmap(bitmap);
                        });
                    } else {
                        Snackbar.make(v, "生成二维码失败, 请重试", LENGTH_SHORT).show();
                    }
                }).exceptionally(throwable -> {
                    Snackbar.make(v, "登录异常", LENGTH_SHORT).show();
                    Log.e(TAG, "获取登录信息错误", throwable);
                    return null;
                });
    }

    private void scanned(View v) {
        CompletableFuture
                .supplyAsync(() -> bilibiliLoginApi.isScanned(), EXECUTOR)
                .thenAccept(scanned -> {
                    if (scanned) {
                        // TODO 待支持登录完成自动跳转到搜索页面
                        Snackbar.make(v, "扫码成功 ~~~", LENGTH_SHORT).show();
                    } else {
                        Snackbar.make(v, "扫码失败, 请重新生成!!!", LENGTH_SHORT).show();
                        handler.post(() -> {
                            loginBtn.setVisibility(VISIBLE);
                            scannedBtn.setVisibility(GONE);
                        });
                    }
                }).exceptionally(throwable -> {
                    Snackbar.make(v, "扫码异常!!!", LENGTH_SHORT).show();
                    Log.e(TAG, "扫码异常", throwable);
                    return null;
                });
    }
}