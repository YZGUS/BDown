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
import com.yz.bdown.utils.SystemNotificationHelper;

public class MainActivity extends AppCompatActivity {

    final FragmentManager fragmentManager = getSupportFragmentManager();


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // 创建通知渠道
        SystemNotificationHelper.createNotificationChannel(this);
        
        // 设置 toolbar
        setSupportActionBar(findViewById(R.id.toolbar));

        // 设置默认加载 Fragment
        loadFragment(new BilibiliFragment());
    }

    // 加载 Fragment
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
        } else {
            View rootView = findViewById(android.R.id.content);  // 使用当前根布局作为的父视图
            Snackbar.make(rootView, "o_o ???", Snackbar.LENGTH_SHORT).show();
        }
        return true;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // 释放所有播放器
        FragmentManager fragmentManager = getSupportFragmentManager();
        Fragment fragment = fragmentManager.findFragmentById(R.id.fragment_container);
        if (fragment instanceof BilibiliFolderFragment) {
            ((BilibiliFolderFragment) fragment).releasePlayer();
        }
    }
}