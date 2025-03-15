package com.yz.bdown.api;

import static com.alibaba.fastjson2.JSON.parseObject;
import static com.yz.bdown.contents.BilibiliConstants.COOKIES;
import static com.yz.bdown.contents.BilibiliConstants.KEY_BILI_JCT;
import static com.yz.bdown.contents.BilibiliConstants.KEY_SESSDATA;
import static com.yz.bdown.contents.BilibiliConstants.REFERER;
import static com.yz.bdown.contents.BilibiliConstants.USER_AGENT;
import static com.yz.bdown.utils.AudioConverterUtils.convertM4sToMp3;
import static com.yz.bdown.utils.FileUtils.toFile;
import static com.yz.bdown.utils.M4sDownloadUtils.downloadM4sFile;
import static com.yz.bdown.utils.M4sMergerUtils.mergeVideoAndAudio;
import static org.apache.commons.collections4.MapUtils.isEmpty;
import static org.apache.commons.lang3.tuple.Pair.of;
import static java.lang.String.format;
import static java.lang.String.valueOf;
import static java.util.concurrent.TimeUnit.MINUTES;

import android.content.SharedPreferences;
import android.util.Log;

import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.yz.bdown.model.BilibiliBaseResp;
import com.yz.bdown.model.BilibiliTvInfo;
import com.yz.bdown.model.BilibiliTvPart;
import com.yz.bdown.utils.DownloadCallback;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.tuple.Pair;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import okhttp3.Headers;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class BilibiliTvApi {

    private static final String TAG = "BilibiliTvApi";

    private static final OkHttpClient CLIENT = new OkHttpClient.Builder()
            .connectTimeout(10, MINUTES)
            .readTimeout(10, MINUTES)
            .build();

    private static final String PART_LIST = "https://api.bilibili.com/x/player/pagelist?bvid=%s";

    private static final String VIDEO_DOWNLOAD = "https://api.bilibili.com/x/player/wbi/playurl";

    private static final String BILIBILI_FOLDER = "bilibiliDown";

    // 获取下载文件夹名称的方法
    public String getBilibiliFolder() {
        return BILIBILI_FOLDER;
    }

    private final SharedPreferences sharedPref;

    public BilibiliTvApi(SharedPreferences sharedPreferences) {
        this.sharedPref = sharedPreferences;
    }

    public BilibiliTvInfo queryBTvParts(String bvid) {
        final Request request = new Request.Builder()
                .url(format(PART_LIST, bvid))
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
            return parseBilibiliTvInfo(bvid, (JSONArray) baseResp.getData());
        } catch (Throwable t) {
            Log.e(TAG, "queryBTvParts err, bvid=" + bvid, t);
            return null;
        }
    }

    private BilibiliTvInfo parseBilibiliTvInfo(String bvid, JSONArray parts) {
        if (parts == null || parts.isEmpty()) {
            return null;
        }

        // 获取封面图
        String cover = ((JSONObject) parts.get(0)).getString("first_frame");
        // 获取标题（默认使用第一个分P的标题，如果是多P视频则会有多个标题）
        JSONObject firstPart = (JSONObject) parts.get(0);
        // 视频标题（如果有多P，使用第一个分P标题作为主标题）
        String mainTitle = firstPart.getString("part");

        List<BilibiliTvPart> BilibiliTvParts = new ArrayList<>(parts.size());
        for (Object part : parts) {
            JSONObject partJson = (JSONObject) part;
            String title = partJson.getString("part");
            Integer duration = partJson.getInteger("duration");
            Long cid = partJson.getLong("cid");
            BilibiliTvParts.add(new BilibiliTvPart(bvid, cid, title, duration));
        }

        // 如果只有一个分P，使用其标题作为视频标题
        // 如果有多个分P，生成一个总标题
        String videoTitle;
        if (parts.size() > 1) {
            videoTitle = mainTitle + " (共" + parts.size() + "个视频)";
        } else {
            videoTitle = mainTitle;
        }

        return new BilibiliTvInfo(cover, videoTitle, BilibiliTvParts);
    }

    public boolean download(BilibiliTvPart BilibiliTvPart) {
        return download(BilibiliTvPart, null);
    }

    public boolean download(BilibiliTvPart BilibiliTvPart, DownloadCallback callback) {
        try {
            String bvid = BilibiliTvPart.getBvid();
            long cid = BilibiliTvPart.getCid();
            Pair<String, String> urlPair = getVideoAndAudioUrl(bvid, cid);
            if (urlPair == null) {
                Log.w(TAG, "download urlPair is null, BilibiliTvPart=" + BilibiliTvPart);
                if (callback != null) {
                    callback.onDownloadError("获取视频地址失败");
                }
                return false;
            }

            final String title = BilibiliTvPart.getTitle().replace('/', ' ');
            File videoFile = toFile(title + "_video.m4s", BILIBILI_FOLDER);
            File audioFile = toFile(title + "_audio.m4s", BILIBILI_FOLDER);
            if (videoFile == null || audioFile == null) {
                Log.w(TAG, "videoFile or audioFile is null");
                if (callback != null) {
                    callback.onDownloadError("创建临时文件失败");
                }
                return false;
            }

            // 创建视频和音频下载的回调
            DownloadCallback videoCallback = callback == null ? null : new DownloadCallback() {
                @Override
                public void onDownloadStart(long totalBytes, String fileName) {
                    callback.onDownloadStart(totalBytes, "视频: " + fileName);
                }

                @Override
                public void onProgressUpdate(long bytesRead, long totalBytes, double speed) {
                    callback.onProgressUpdate(bytesRead, totalBytes, speed);
                }

                @Override
                public void onDownloadComplete(String fileName, String filePath) {
                    callback.onDownloadComplete("视频: " + fileName, filePath);
                }

                @Override
                public void onDownloadError(String errorMessage) {
                    callback.onDownloadError("视频下载失败: " + errorMessage);
                }
            };

            DownloadCallback audioCallback = callback == null ? null : new DownloadCallback() {
                @Override
                public void onDownloadStart(long totalBytes, String fileName) {
                    callback.onDownloadStart(totalBytes, "音频: " + fileName);
                }

                @Override
                public void onProgressUpdate(long bytesRead, long totalBytes, double speed) {
                    callback.onProgressUpdate(bytesRead, totalBytes, speed);
                }

                @Override
                public void onDownloadComplete(String fileName, String filePath) {
                    callback.onDownloadComplete("音频: " + fileName, filePath);
                }

                @Override
                public void onDownloadError(String errorMessage) {
                    callback.onDownloadError("音频下载失败: " + errorMessage);
                }
            };

            // 下载视频和音频
            if (!downloadM4sFile(urlPair.getKey(), videoFile, videoCallback) ||
                    !downloadM4sFile(urlPair.getValue(), audioFile, audioCallback)) {
                Log.w(TAG, "download m4s failed, BilibiliTvPart=" + BilibiliTvPart);
                if (callback != null) {
                    callback.onDownloadError("下载视频或音频文件失败");
                }
                return false;
            }

            final File mergeFile = toFile(title + ".mp4", BILIBILI_FOLDER);
            if (mergeFile == null) {
                Log.w(TAG, "mergeFile is null");
                if (callback != null) {
                    callback.onDownloadError("创建合并文件失败");
                }
                return false;
            }

            // 通知开始合并
            if (callback != null) {
                callback.onDownloadStart(0, "正在合并视频和音频...");
            }

            boolean mergeResult = mergeVideoAndAudio(videoFile.getPath(), audioFile.getPath(), mergeFile.getPath());

            if (mergeResult && callback != null) {
                callback.onDownloadComplete(title + ".mp4", mergeFile.getAbsolutePath());
            } else if (!mergeResult && callback != null) {
                callback.onDownloadError("合并视频和音频失败");
            }

            return mergeResult;
        } catch (Throwable t) {
            Log.e(TAG, "download BilibiliTvPart=" + BilibiliTvPart, t);
            if (callback != null) {
                callback.onDownloadError("下载过程发生异常: " + t.getMessage());
            }
            return false;
        }
    }

    public Pair<String, String> getVideoAndAudioUrl(String bvid, long cid) {
        final Request request = new Request.Builder()
                .url(toUrl(VIDEO_DOWNLOAD, params(bvid, cid)))
                .headers(headers())
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

            Object data = baseResp.getData();
            if (data == null) {
                Log.w(TAG, "getVideoAndAudioUrl data is null");
                return null;
            }

            final JSONObject dash = ((JSONObject) data).getJSONObject("dash");
            if (dash == null) {
                Log.w(TAG, "getVideoAndAudioUrl dash is null");
                return null;
            }
            return of(parseVideoUrl(dash.getJSONArray("video")), parseAudioUrl(dash.getJSONArray("audio")));
        } catch (Throwable t) {
            Log.e(TAG, "getVideoAndAudioUrl dash is null", t);
            return null;
        }
    }

    private Map<String, String> params(String bvid, long cid) {
        return Map.of(
                "bvid", bvid,
                "cid", valueOf(cid),
                "qn", "127",
                "fnval", "4048",
                "fnver", "0",
                "fourk", "1",
                "voice_balance", "1"
        );
    }

    public static String toUrl(String url, Map<String, String> params) {
        if (isEmpty(params)) {
            return url;
        }

        // 将 Map 转换为查询字符串
        final StringBuilder query = new StringBuilder(url + '?');
        for (Entry<String, String> entry : params.entrySet()) {
            query.append(entry.getKey()).append("=").append(entry.getValue()).append("&");
        }
        return query.toString();
    }

    private Headers headers() {
        String sessdata = sharedPref.getString(KEY_SESSDATA, "");
        String biliJct = sharedPref.getString(KEY_BILI_JCT, "");
        return new Headers.Builder()
                .add("Referer", REFERER)
                .add("User-Agent", USER_AGENT)
                .add("Cookie", format(COOKIES, sessdata, biliJct))
                .build();
    }

    private String parseVideoUrl(JSONArray videos) {
        if (CollectionUtils.isEmpty(videos)) {
            return "";
        }

        int curId = 0;
        String videoUrl = "";
        for (Object video : videos) {
            final JSONObject videoJson = (JSONObject) video;
            final int videoId = videoJson.getIntValue("id");
            if (videoId > curId) {
                curId = videoId;
                videoUrl = videoJson.getString("baseUrl");
            }
        }
        return videoUrl;
    }

    private String parseAudioUrl(JSONArray audios) {
        if (CollectionUtils.isEmpty(audios)) {
            return "";
        }

        int curId = 0;
        String audioUrl = "";
        for (Object audio : audios) {
            final JSONObject audioJson = (JSONObject) audio;
            final int audioId = audioJson.getIntValue("id");
            if (audioId > curId) {
                curId = audioId;
                audioUrl = audioJson.getString("baseUrl");
            }
        }
        return audioUrl;
    }

    public boolean transformMp3(String fileName) {
        File m4sAudioFile = toFile(fileName + "_audio.m4s", BILIBILI_FOLDER);
        File mp3File = toFile(fileName + ".mp3", BILIBILI_FOLDER);
        return convertM4sToMp3(m4sAudioFile.getAbsolutePath(), mp3File.getPath());
    }

    /**
     * 下载B站视频分P到指定目录
     *
     * @param bTvPart     视频分P信息
     * @param downloadDir 下载目录
     * @param fileName    文件名
     * @param callback    下载回调
     * @return 下载结果
     */
    public boolean downloadBTvPart(BilibiliTvPart bTvPart, File downloadDir, String fileName, DownloadCallback callback) {
        try {
            String bvid = bTvPart.getBvid();
            long cid = bTvPart.getCid();
            Pair<String, String> urlPair = getVideoAndAudioUrl(bvid, cid);
            if (urlPair == null) {
                Log.w(TAG, "downloadBTvPart urlPair is null, BilibiliTvPart=" + bTvPart);
                if (callback != null) {
                    callback.onDownloadError("获取视频地址失败");
                }
                return false;
            }

            // 确保下载目录存在
            if (downloadDir == null || !downloadDir.exists()) {
                if (downloadDir != null && !downloadDir.mkdirs()) {
                    Log.w(TAG, "Failed to create download directory: " + downloadDir);
                    if (callback != null) {
                        callback.onDownloadError("创建下载目录失败");
                    }
                    return false;
                }
            }

            // 准备临时文件
            final String title = bTvPart.getTitle().replace('/', ' ');
            File videoFile = new File(downloadDir, title + "_video.m4s");
            File audioFile = new File(downloadDir, title + "_audio.m4s");

            // 创建视频和音频下载的回调
            DownloadCallback videoCallback = callback == null ? null : new DownloadCallback() {
                @Override
                public void onDownloadStart(long totalBytes, String name) {
                    callback.onDownloadStart(totalBytes, "视频: " + name);
                }

                @Override
                public void onProgressUpdate(long bytesRead, long totalBytes, double speed) {
                    callback.onProgressUpdate(bytesRead, totalBytes, speed);
                }

                @Override
                public void onDownloadComplete(String name, String filePath) {
                    callback.onDownloadComplete("视频: " + name, filePath);
                }

                @Override
                public void onDownloadError(String errorMessage) {
                    callback.onDownloadError("视频下载失败: " + errorMessage);
                }
            };

            DownloadCallback audioCallback = callback == null ? null : new DownloadCallback() {
                @Override
                public void onDownloadStart(long totalBytes, String name) {
                    callback.onDownloadStart(totalBytes, "音频: " + name);
                }

                @Override
                public void onProgressUpdate(long bytesRead, long totalBytes, double speed) {
                    callback.onProgressUpdate(bytesRead, totalBytes, speed);
                }

                @Override
                public void onDownloadComplete(String name, String filePath) {
                    callback.onDownloadComplete("音频: " + name, filePath);
                }

                @Override
                public void onDownloadError(String errorMessage) {
                    callback.onDownloadError("音频下载失败: " + errorMessage);
                }
            };

            // 下载视频和音频
            if (!downloadM4sFile(urlPair.getKey(), videoFile, videoCallback) ||
                    !downloadM4sFile(urlPair.getValue(), audioFile, audioCallback)) {
                Log.w(TAG, "downloadBTvPart m4s failed, BilibiliTvPart=" + bTvPart);
                if (callback != null) {
                    callback.onDownloadError("下载视频或音频文件失败");
                }
                return false;
            }

            final File mergeFile = new File(downloadDir, fileName);

            // 通知开始合并
            if (callback != null) {
                callback.onDownloadStart(0, "正在合并视频和音频...");
            }

            boolean mergeResult = mergeVideoAndAudio(videoFile.getPath(), audioFile.getPath(), mergeFile.getPath());

            if (mergeResult && callback != null) {
                callback.onDownloadComplete(fileName, mergeFile.getAbsolutePath());
            } else if (!mergeResult && callback != null) {
                callback.onDownloadError("合并视频和音频失败");
            }

            return mergeResult;
        } catch (Throwable t) {
            Log.e(TAG, "downloadBTvPart BilibiliTvPart=" + bTvPart, t);
            if (callback != null) {
                callback.onDownloadError("下载过程发生异常: " + t.getMessage());
            }
            return false;
        }
    }
}
