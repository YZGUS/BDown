package com.yz.bdown;

import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

import com.google.android.material.snackbar.Snackbar;
import com.yz.bdown.fragment.bilibili.BilibiliFolderFragment;
import com.yz.bdown.fragment.bilibili.BilibiliFragment;
import com.yz.bdown.fragment.deepseek.DeepSeekFragment;
import com.yz.bdown.utils.SystemNotificationUtils;

/**
 * 应用主Activity
 * 负责管理Fragment的切换和应用的主界面
 */
public class MainActivity extends AppCompatActivity {

    private final FragmentManager fragmentManager = getSupportFragmentManager();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // 初始化应用
        initializeApp();
    }

    /**
     * 初始化应用
     */
    private void initializeApp() {
        // 创建通知渠道
        SystemNotificationUtils.createNotificationChannel(this);

        // 设置 toolbar
        setSupportActionBar(findViewById(R.id.toolbar));

        // 设置默认加载 Fragment
        loadFragment(new BilibiliFragment());
    }

    /**
     * 加载Fragment到容器
     *
     * @param fragment 要加载的Fragment实例
     */
    private void loadFragment(Fragment fragment) {
        fragmentManager.beginTransaction()
                .replace(R.id.fragment_container, fragment)
                .addToBackStack(null)
                .commit();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.toolbar, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int itemId = item.getItemId();
        if (itemId == R.id.item_bilibili) {
            loadFragment(new BilibiliFragment());
        } else if (itemId == R.id.item_deepseek) {
            // 检查是否已经配置了 DeepSeek API 密钥
            checkAndLoadDeepSeekFragment();
        } else {
            // 显示提示信息
            showNotImplementedMessage();
        }
        return true;
    }

    /**
     * 检查 DeepSeek API 密钥配置，然后加载 DeepSeek Fragment
     */
    private void checkAndLoadDeepSeekFragment() {
        // 无论是否有密钥，都加载 DeepSeekFragment
        // 在 Fragment 内部会处理密钥配置
        loadFragment(new DeepSeekFragment());
    }

    /**
     * 显示功能未实现的提示信息
     */
    private void showNotImplementedMessage() {
        View rootView = findViewById(android.R.id.content);
        Snackbar.make(rootView, "o_o ???", Snackbar.LENGTH_SHORT).show();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // 释放所有播放器
        releaseResources();
    }

    /**
     * 释放应用资源
     */
    private void releaseResources() {
        Fragment fragment = fragmentManager.findFragmentById(R.id.fragment_container);
        if (fragment instanceof BilibiliFolderFragment) {
            ((BilibiliFolderFragment) fragment).releasePlayer();
        }
    }
}