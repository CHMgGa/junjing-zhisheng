package com.example.yangdujun;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.yangdujun.utils.FeedbackStorage;
import com.example.yangdujun.utils.UserStorage;

public class FeedbackActivity extends AppCompatActivity {

    private static final String TAG = "FeedbackActivity";
    private String userToken;
    private FeedbackStorage feedbackStorage;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_feedback);

        // 自定义标题栏返回按钮
        ImageView ivBack = findViewById(R.id.ivBack);
        if (ivBack != null) {
            ivBack.setOnClickListener(v -> finish());
        }

        // 获取用户token
        UserStorage userStorage = new UserStorage();
        userToken = userStorage.getToken();
        
        // 初始化反馈存储
        feedbackStorage = new FeedbackStorage();

        EditText etDesc = findViewById(R.id.etDesc);
        TextView tvCount = findViewById(R.id.tvCount);
        TextView btnSubmit = findViewById(R.id.btnSubmit);

        if (etDesc != null) {
            etDesc.addTextChangedListener(new TextWatcher() {
                @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
                @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                    if (tvCount != null) {
                        tvCount.setText(s.length() + "/200");
                    }
                }
                @Override public void afterTextChanged(Editable s) {}
            });
        }

        if (btnSubmit != null) {
            btnSubmit.setOnClickListener(v -> {
                if (etDesc != null) {
                    String content = etDesc.getText().toString().trim();
                    if (content.isEmpty()) {
                        Toast.makeText(FeedbackActivity.this, "请输入反馈内容", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    submitFeedback(content);
                }
            });
        }
    }

    private void submitFeedback(String content) {
        // 保存到本地存储
        if (feedbackStorage != null) {
            feedbackStorage.saveFeedback(content);
            
            Toast.makeText(FeedbackActivity.this, "提交成功", Toast.LENGTH_SHORT).show();
            finish();
        } else {
            Toast.makeText(FeedbackActivity.this, "存储初始化失败", Toast.LENGTH_SHORT).show();
        }
    }
}


