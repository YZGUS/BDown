package com.yz.bdown.model;

import androidx.annotation.NonNull;

import java.util.List;

public class BilibiliTvInfo {

    private String coverUrl;

    private List<BilibiliTvPart> bilibiliTvParts;

    public BilibiliTvInfo(String coverUrl, List<BilibiliTvPart> bilibiliTvParts) {
        this.coverUrl = coverUrl;
        this.bilibiliTvParts = bilibiliTvParts;
    }

    public String getCoverUrl() {
        return coverUrl;
    }

    public List<BilibiliTvPart> getbTvParts() {
        return bilibiliTvParts;
    }

    @NonNull
    @Override
    public String toString() {
        return "BilibiliTvInfo{" +
                "coverUrl='" + coverUrl + '\'' +
                ", bilibiliTvParts=" + bilibiliTvParts +
                '}';
    }
}
