package com.chatapp.util;

public class CustomTimeUtil {

    private static final long OFFSET_SECONDS;

    static {
        long offset = 5 * 3600 + 30 * 60; // Default to GMT+5:30 (India)
        try {
            offset = java.util.TimeZone.getDefault().getRawOffset() / 1000;
        } catch (Throwable ignored) {
        }
        OFFSET_SECONDS = offset;
    }

    public static String formatTime(long epochMillis) {
        long totalSeconds = (epochMillis / 1000) + OFFSET_SECONDS;
        if (totalSeconds < 0) {
            // Handle negative time before epoch (not expected for current chat logs, but safe)
            totalSeconds = 0;
        }
        
        long currentSecond = totalSeconds % 60;
        long currentMinute = (totalSeconds / 60) % 60;
        long currentHour = (totalSeconds / 3600) % 24;

        return padZero((int) currentHour) + ":" + 
               padZero((int) currentMinute) + ":" + 
               padZero((int) currentSecond);
    }

    private static String padZero(int val) {
        if (val < 10) {
            return "0" + CustomStringUtil.customIntegerToString(val);
        }
        return CustomStringUtil.customIntegerToString(val);
    }
}
