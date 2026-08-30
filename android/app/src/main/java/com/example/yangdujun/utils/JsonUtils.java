package com.example.yangdujun.utils;

import com.alibaba.fastjson.JSON;

public class JsonUtils {
    // 将对象转换为JSON字符串
    public static String toJson(Object object) {
        return JSON.toJSONString(object);
    }

    // 将JSON字符串转换为对象
    public static <T> T fromJson(String json, Class<T> clazz) {
        return JSON.parseObject(json, clazz);
    }
}


