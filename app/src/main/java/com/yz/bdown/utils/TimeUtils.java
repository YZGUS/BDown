package com.yz.bdown.utils;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * 时间工具类
 */
public class TimeUtils {
    
    private static final SimpleDateFormat DEFAULT_FORMAT = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
    
    /**
     * 获取当前时间的字符串表示
     * @return 当前时间字符串，格式为 "yyyy-MM-dd HH:mm:ss"
     */
    public static String getNowTimeString() {
        return DEFAULT_FORMAT.format(new Date());
    }
    
    /**
     * 获取当前时间的字符串表示，使用指定格式
     * @param format 时间格式
     * @return 当前时间字符串
     */
    public static String getNowTimeString(String format) {
        SimpleDateFormat sdf = new SimpleDateFormat(format, Locale.getDefault());
        return sdf.format(new Date());
    }
    
    /**
     * 将Date转换为字符串
     * @param date 日期
     * @return 日期字符串
     */
    public static String formatDate(Date date) {
        return DEFAULT_FORMAT.format(date);
    }
    
    /**
     * 将Date转换为字符串，使用指定格式
     * @param date 日期
     * @param format 时间格式
     * @return 日期字符串
     */
    public static String formatDate(Date date, String format) {
        SimpleDateFormat sdf = new SimpleDateFormat(format, Locale.getDefault());
        return sdf.format(date);
    }
} 