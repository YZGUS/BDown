package com.yz.bdown.model;

import androidx.annotation.NonNull;

public class BilibiliTvPart {

    private String bvid;
    private String title;
    private int duration; // 单位(s)
    private long cid;

    public BilibiliTvPart(String bvid, long cid, String title, int duration) {
        this.bvid = bvid;
        this.title = title;
        this.duration = duration;
        this.cid = cid;
    }

    public String getBvid() {
        return bvid;
    }

    public String getTitle() {
        return title;
    }

    public int getDuration() {
        return duration;
    }

    public long getCid() {
        return cid;
    }

    public String getFormatDuration() {
        return formatToCompact(duration);
    }

    public String formatToCompact(long seconds) {
        long hours = seconds / 3600; // 计算小时
        long minutes = (seconds % 3600) / 60; // 计算分钟
        long secs = seconds % 60; // 计算秒数
        StringBuilder result = new StringBuilder();
        if (hours > 0) {
            result.append(hours).append("h ");
        }

        if (minutes > 0) {
            result.append(minutes).append("m ");
        }

        if (secs > 0 || result.length() == 0) { // 如果总时间为 0，显示 "0s"
            result.append(secs).append("s");
        }
        return result.toString().trim(); // 去除末尾空格
    }

    @NonNull
    @Override
    public String toString() {
        return "BilibiliTvPart{" +
                "bvid='" + bvid + '\'' +
                ", title='" + title + '\'' +
                ", duration=" + duration +
                ", cid=" + cid +
                '}';
    }
}
