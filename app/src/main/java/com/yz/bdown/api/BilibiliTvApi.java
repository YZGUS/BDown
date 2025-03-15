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
import com.yz.bdown.utils.AudioConverterUtils;

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

        String cover = ((JSONObject) parts.get(0)).getString("first_frame");
        List<BilibiliTvPart> BilibiliTvParts = new ArrayList<>(parts.size());
        for (Object part : parts) {
            JSONObject partJson = (JSONObject) part;
            String title = partJson.getString("part");
            Integer duration = partJson.getInteger("duration");
            Long cid = partJson.getLong("cid");
            BilibiliTvParts.add(new BilibiliTvPart(bvid, cid, title, duration));
        }
        return new BilibiliTvInfo(cover, BilibiliTvParts);
    }

    public boolean download(BilibiliTvPart BilibiliTvPart) {
        try {
            String bvid = BilibiliTvPart.getBvid();
            long cid = BilibiliTvPart.getCid();
            Pair<String, String> urlPair = getVideoAndAudioUrl(bvid, cid);
            if (urlPair == null) {
                Log.w(TAG, "download urlPair is null, BilibiliTvPart=" + BilibiliTvPart);
                return false;
            }

            final String title = BilibiliTvPart.getTitle().replace('/', ' ');
            File videoFile = toFile(title + "_video.m4s", BILIBILI_FOLDER);
            File audioFile = toFile(title + "_audio.m4s", BILIBILI_FOLDER);
            if (videoFile == null || audioFile == null) {
                Log.w(TAG, "videoFile or audioFile is null");
                return false;
            }

            if (!downloadM4sFile(urlPair.getKey(), videoFile) || !downloadM4sFile(urlPair.getValue(), audioFile)) {
                Log.w(TAG, "download m4s failed, BilibiliTvPart=" + BilibiliTvPart);
                return false;
            }

            final File mergeFile = toFile(title + ".mp4", BILIBILI_FOLDER);
            if (mergeFile == null) {
                Log.w(TAG, "mergeFile is null");
                return false;
            }
            return mergeVideoAndAudio(videoFile.getPath(), audioFile.getPath(), mergeFile.getPath());
        } catch (Throwable t) {
            Log.e(TAG, "download BilibiliTvPart=" + BilibiliTvPart, t);
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
}
