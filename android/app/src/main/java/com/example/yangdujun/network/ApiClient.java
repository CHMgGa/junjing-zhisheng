package com.example.yangdujun.network;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class ApiClient {
    private static final String BASE_URL = "";
    private static final OkHttpClient client = new OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build();

    // 发送GET请求
    public static String get(String endpoint) throws IOException {
        Request request = new Request.Builder()
                .url(BASE_URL + endpoint)
                .build();

        try (Response response = client.newCall(request).execute()) {
            return response.body() != null ? response.body().string() : "";
        }
    }

    // 发送POST请求
    public static String post(String endpoint, RequestBody body) throws IOException {
        Request request = new Request.Builder()
                .url(BASE_URL + endpoint)
                .post(body)
                .build();

        try (Response response = client.newCall(request).execute()) {
            return response.body() != null ? new String(response.body().bytes(), "UTF-8") : "";
        }
    }

    // 发送带Token的GET请求
    public static String getWithToken(String endpoint, String token) throws IOException {
        Request request = new Request.Builder()
                .url(BASE_URL + endpoint)
                .addHeader("Authorization", "Bearer " + token)
                .build();

        try (Response response = client.newCall(request).execute()) {
            return response.body() != null ? new String(response.body().bytes(), "UTF-8") : "";
        }
    }

    // 发送带Token的POST请求
    public static String postWithToken(String endpoint, RequestBody body, String token) throws IOException {
        Request request = new Request.Builder()
                .url(BASE_URL + endpoint)
                .addHeader("Authorization", "Bearer " + token)
                .post(body)
                .build();

        try (Response response = client.newCall(request).execute()) {
            return response.body() != null ? new String(response.body().bytes(), "UTF-8") : "";
        }
    }

    // 发送带Token的异步POST请求（支持回调）
    public static void postWithToken(String endpoint, String jsonBody, String token, okhttp3.Callback callback) {
        RequestBody body = okhttp3.RequestBody.create(
                okhttp3.MediaType.parse("application/json; charset=utf-8"),
                jsonBody
                );
        
        Request request = new Request.Builder()
                .url(BASE_URL + endpoint)
                .addHeader("Authorization", "Bearer " + token)
                .post(body)
                .build();

        client.newCall(request).enqueue(callback);
    }
}


