package com.yz.bdown.utils;

import static android.media.MediaExtractor.SAMPLE_FLAG_SYNC;
import static android.media.MediaFormat.KEY_MIME;
import static android.media.MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4;

import android.media.MediaCodec;
import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.media.MediaMuxer;
import android.util.Log;

import org.apache.commons.lang3.tuple.Pair;

import java.nio.ByteBuffer;

public class M4sMergerUtils {

    private static final String TAG = "M4sMergerUtils";

    public static boolean mergeVideoAndAudio(String videoPath, String audioPath, String outputPath) {
        MediaExtractor videoExtractor = new MediaExtractor();
        MediaExtractor audioExtractor = new MediaExtractor();
        MediaMuxer muxer = null;
        try {
            // 提取数据
            Pair<MediaFormat, Integer> videoPair = extractTrackData(videoExtractor, videoPath, "video/");
            Pair<MediaFormat, Integer> audioPair = extractTrackData(audioExtractor, audioPath, "audio/");
            if (videoPair == null || audioPair == null) {
                Log.w(TAG, "videoPair or audioPair is invalid");
                return false;
            }
            muxer = new MediaMuxer(outputPath, MUXER_OUTPUT_MPEG_4);

            // 添加视频轨道
            videoExtractor.selectTrack(videoPair.getValue());
            int muxerVideoTrackIndex = muxer.addTrack(videoPair.getKey());

            // 添加音频轨道
            audioExtractor.selectTrack(audioPair.getValue());
            int muxerAudioTrackIndex = muxer.addTrack(audioPair.getKey());

            // 开始混合
            muxer.start();
            muxerTrackData(videoExtractor, muxer, muxerVideoTrackIndex);
            muxerTrackData(audioExtractor, muxer, muxerAudioTrackIndex);
            muxer.stop();
            return true;
        } catch (Throwable t) {
            Log.e(TAG, "Error merging video and audio", t);
            return false;
        } finally {
            if (muxer != null) {
                muxer.release();
            }
            videoExtractor.release();
            audioExtractor.release();
        }
    }

    private static Pair<MediaFormat, Integer> extractTrackData(MediaExtractor mediaExtractor,
                                                               String path,
                                                               String minePrefix) throws Throwable {
        mediaExtractor.setDataSource(path);
        int mediaTrackIndex = -1;
        MediaFormat mediaFormat = null;
        for (int i = 0; i < mediaExtractor.getTrackCount(); i++) {
            mediaFormat = mediaExtractor.getTrackFormat(i);
            String mime = mediaFormat.getString(KEY_MIME);
            if (mime.startsWith(minePrefix)) {
                mediaTrackIndex = i;
                break;
            }
        }

        if (mediaTrackIndex == -1) {
            Log.e(TAG, "No video track found in video file");
            return null;
        }
        return Pair.of(mediaFormat, mediaTrackIndex);
    }

    private static void muxerTrackData(MediaExtractor mediaExtractor,
                                       MediaMuxer muxer,
                                       int muxerTrackIndex) {
        boolean mediaDone = false;
        while (!mediaDone) {
            int mediaBufferSize = bufferSize(mediaExtractor);
            byte[] buffer = new byte[mediaBufferSize];
            MediaCodec.BufferInfo info = mediaBufferInfo(mediaExtractor);
            int sampleSize = mediaExtractor.readSampleData(ByteBuffer.wrap(buffer), 0);
            if (sampleSize < 0) {
                mediaDone = true;
            } else {
                info.size = sampleSize;
                muxer.writeSampleData(muxerTrackIndex, ByteBuffer.wrap(buffer), info);
                mediaExtractor.advance();
            }
        }
    }

    private static int bufferSize(MediaExtractor mediaExtractor) {
        int bufferSize = (int) mediaExtractor.getSampleSize();
        return bufferSize <= 0 ? 1024 * 1024 : bufferSize;
    }

    private static MediaCodec.BufferInfo mediaBufferInfo(MediaExtractor mediaExtractor) {
        MediaCodec.BufferInfo info = new MediaCodec.BufferInfo();
        info.presentationTimeUs = mediaExtractor.getSampleTime();
        int sampleFlags = mediaExtractor.getSampleFlags();
        if ((sampleFlags & SAMPLE_FLAG_SYNC) != 0) {
            info.flags = MediaCodec.BUFFER_FLAG_KEY_FRAME;
        } else {
            info.flags = 0;
        }
        return info;
    }
}