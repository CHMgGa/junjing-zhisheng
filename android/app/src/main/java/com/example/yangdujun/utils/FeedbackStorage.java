package com.example.yangdujun.utils;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;

import java.text.SimpleDateFormat;
import java.util.Date;

public class FeedbackStorage {
    private static final String TAG = "FeedbackStorage";
    private JSONObject root;
    private JSONArray feedbacks;

    public FeedbackStorage() {
        // 初始化内存中的JSON对象
        root = new JSONObject();
        feedbacks = new JSONArray();
        root.put("feedbacks", feedbacks);
    }

    // 保存反馈
    public void saveFeedback(String content) {
        try {
            // 创建反馈对象
            JSONObject feedbackObj = new JSONObject();
            feedbackObj.put("id", System.currentTimeMillis());
            feedbackObj.put("content", content);
            feedbackObj.put("timestamp", new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()));
            feedbackObj.put("status", "pending"); //  pending, submitted, failed

            // 添加到列表开头
            feedbacks.add(0, feedbackObj);

            // 限制反馈数量为50条
            if (feedbacks.size() > 50) {
                JSONArray newFeedbacks = new JSONArray();
                for (int i = 0; i < 50; i++) {
                    newFeedbacks.add(feedbacks.get(i));
                }
                feedbacks = newFeedbacks;
                root.put("feedbacks", feedbacks);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // 获取所有反馈
    public JSONArray getFeedbacks() {
        try {
            return root.getJSONArray("feedbacks");
        } catch (Exception e) {
            e.printStackTrace();
            return new JSONArray();
        }
    }

    // 更新反馈状态
    public void updateFeedbackStatus(long id, String status) {
        try {
            for (int i = 0; i < feedbacks.size(); i++) {
                JSONObject feedbackObj = feedbacks.getJSONObject(i);
                if (feedbackObj.getLong("id") == id) {
                    feedbackObj.put("status", status);
                    break;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // 清空反馈列表
    public void clearFeedbacks() {
        feedbacks = new JSONArray();
        root.put("feedbacks", feedbacks);
    }
}

