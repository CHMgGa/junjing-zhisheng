package com.example.yangdujun.utils;

public class NullSafeUtils {
    public static String safeString(String str) {
        return str == null ? "" : str;
    }

    public static <T> T safeObj(T obj, T defaultValue) {
        return obj == null ? defaultValue : obj;
    }
}