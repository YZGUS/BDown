package com.yz.bdown.model;

import androidx.annotation.NonNull;

import java.util.List;

public class BilibiliTvInfo {

    private String coverUrl;
    private String title;
    private List<BilibiliTvPart> bilibiliTvParts;

    public BilibiliTvInfo(String coverUrl, String title, List<BilibiliTvPart> bilibiliTvParts) {
        this.coverUrl = coverUrl;
        this.title = title;
        this.bilibiliTvParts = bilibiliTvParts;
    }

    // 兼容旧构造函数
    public BilibiliTvInfo(String coverUrl, List<BilibiliTvPart> bilibiliTvParts) {
        this(coverUrl, null, bilibiliTvParts);
    }

    public String getCoverUrl() {
        return coverUrl;
    }

    public String getTitle() {
        return title;
    }

    public List<BilibiliTvPart> getbTvParts() {
        return bilibiliTvParts;
    }

    @NonNull
    @Override
    public String toString() {
        return "BilibiliTvInfo{" +
                "coverUrl='" + coverUrl + '\'' +
                ", title='" + title + '\'' +
                ", bilibiliTvParts=" + bilibiliTvParts +
                '}';
    }
}
