package com.yz.bdown.fragment.bilibili;

import static com.google.android.material.snackbar.BaseTransientBottomBar.LENGTH_SHORT;

import android.os.Bundle;

import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.Fragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.google.android.material.snackbar.Snackbar;
import com.yz.bdown.R;

public class BilibiliFragment extends Fragment implements View.OnClickListener {

    private DrawerLayout drawerLayout;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_bilibili, container, false);

        drawerLayout = view.findViewById(R.id.bilibili);
        view.findViewById(R.id.bilibili_login).setOnClickListener(this);
        view.findViewById(R.id.bilibili_search).setOnClickListener(this);
        view.findViewById(R.id.bilibili_tv_folder).setOnClickListener(this);

        // 设置默认显示视图
        loadFragment(BilibiliSearchFragment.class);
        return view;
    }

    private void loadFragment(Class clazz) {
        getChildFragmentManager()
                .beginTransaction()
                .replace(R.id.bilibili_main_fragment, clazz, null)
                .addToBackStack(null)
                .commit();
    }

    @Override
    public void onClick(View v) {
        int vid = v.getId();
        if (vid == R.id.bilibili_login) {
            loadFragment(BilibiliLoginFragment.class);
        } else if (vid == R.id.bilibili_search) {
            loadFragment(BilibiliSearchFragment.class);
        } else if (vid == R.id.bilibili_tv_folder) {
            loadFragment(BilibiliFolderFragment.class);
        } else {
            Snackbar.make(v, "进入异次元了吗？不愧是你呢 哈基米!", LENGTH_SHORT).show();
        }
        drawerLayout.closeDrawers(); // 关闭抽屉
    }
}