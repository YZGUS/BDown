package com.yz.bdown.utils;

import com.arthenica.mobileffmpeg.FFmpeg;

public class AudioConverterUtils {

    public static boolean convertM4sToMp3(String inputPath, String outputPath) {
        String[] cmd = {                // FFmpeg 命令行参数
                "-y",                   // 覆盖输出文件
                "-i", inputPath,        // 输入文件
                "-vn",                  // 忽略视频流
                "-c:a", "libmp3lame",   // 使用 MP3 编码器
                "-q:a", "9",            // 音质参数（0-9，0 为最高质量）
                "-map_metadata", "0",   // 保留元数据
                outputPath
        };
        return FFmpeg.execute(cmd) == 0;
    }
}