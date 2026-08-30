package com.example.yangdujun;

import android.Manifest;
import android.content.ContentResolver;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.example.yangdujun.model.User;
import com.example.yangdujun.utils.LocalUserManager;
import com.example.yangdujun.utils.UserStorage;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;

public class EditProfileActivity extends AppCompatActivity {

    private static final int PICK_IMAGE_REQUEST = 1;
    private static final int PERMISSION_REQUEST_READ_EXTERNAL_STORAGE = 2;
    private static final int PERMISSION_REQUEST_READ_MEDIA_IMAGES = 3;

    private UserStorage userStorage;
    private User currentUser;
    private Uri selectedImageUri;

    private EditText etNickname;
    private TextView tvPhone;
    private TextView tvToken;
    private ImageView ivAvatar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_profile);

        // 初始化用户存储
        userStorage = new UserStorage();
        currentUser = userStorage.getUser();

        // 初始化控件
        initViews();
        // 加载用户信息
        loadUserInfo();

        // 设置返回按钮点击事件
        View ivBack = findViewById(R.id.iv_back);
        if (ivBack != null) {
            ivBack.setOnClickListener(v -> finish());
        }

        // 设置保存按钮点击事件
        View tvSave = findViewById(R.id.tv_save);
        if (tvSave != null) {
            tvSave.setOnClickListener(v -> saveUserInfo());
        }

        // 设置更换头像点击事件
        View tvChangeAvatar = findViewById(R.id.tv_change_avatar);
        if (tvChangeAvatar != null) {
            tvChangeAvatar.setOnClickListener(v -> {
                // 检查权限
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    // Android 13+ 使用 READ_MEDIA_IMAGES 权限
                    if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_IMAGES) != PackageManager.PERMISSION_GRANTED) {
                        ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_MEDIA_IMAGES}, PERMISSION_REQUEST_READ_MEDIA_IMAGES);
                    } else {
                        openGallery();
                    }
                } else {
                    // Android 12 及以下使用 READ_EXTERNAL_STORAGE 权限
                    if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                        ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, PERMISSION_REQUEST_READ_EXTERNAL_STORAGE);
                    } else {
                        openGallery();
                    }
                }
            });
        }
    }

    private void initViews() {
        etNickname = findViewById(R.id.et_nickname);
        tvPhone = findViewById(R.id.tv_phone);
        tvToken = findViewById(R.id.tv_token);
        ivAvatar = findViewById(R.id.iv_avatar);
    }

    private void loadUserInfo() {
        if (currentUser != null) {
            if (etNickname != null) {
                etNickname.setText(currentUser.getNickname());
            }
            if (tvPhone != null) {
                tvPhone.setText(currentUser.getPhone());
            }
            if (tvToken != null) {
                tvToken.setText(currentUser.getToken());
            }
            // 加载用户头像
            if (ivAvatar != null) {
                if (!TextUtils.isEmpty(currentUser.getAvatar())) {
                    try {
                        selectedImageUri = Uri.parse(currentUser.getAvatar());
                        ivAvatar.setImageURI(selectedImageUri);
                    } catch (Exception e) {
                        // 头像加载失败，使用默认头像
                        ivAvatar.setImageResource(R.drawable.image_1);
                    }
                } else {
                    // 用户没有头像，使用默认头像
                    ivAvatar.setImageResource(R.drawable.image_1);
                }
            }
        }
    }

    private void saveUserInfo() {
        if (etNickname != null) {
            String nickname = etNickname.getText().toString().trim();

            if (TextUtils.isEmpty(nickname)) {
                Toast.makeText(this, "昵称不能为空", Toast.LENGTH_SHORT).show();
                return;
            }

            if (currentUser != null) {
                // 获取头像URI字符串
                String avatarUri = currentUser.getAvatar();
                if (selectedImageUri != null) {
                    // 复制图片到应用内部存储
                    String copiedUri = copyImageToInternalStorage(selectedImageUri);
                    if (copiedUri != null) {
                        avatarUri = copiedUri;
                    }
                }

                // 使用LocalUserManager更新用户信息
                LocalUserManager localUserManager = new LocalUserManager(this);
                boolean updated = localUserManager.updateUserInfo(currentUser.getToken(), nickname, avatarUri);

                if (updated) {
                    // 更新本地currentUser对象
                    currentUser.setNickname(nickname);
                    currentUser.setAvatar(avatarUri);
                    
                    // 重新加载用户信息到UserStorage
                    if (userStorage != null) {
                        userStorage.saveUser(currentUser);
                    }
                    
                    Toast.makeText(this, "保存成功", Toast.LENGTH_SHORT).show();
                    // 返回上一页
                    finish();
                } else {
                    Toast.makeText(this, "保存失败，请重试", Toast.LENGTH_SHORT).show();
                }
            } else {
                Toast.makeText(this, "用户信息不存在", Toast.LENGTH_SHORT).show();
            }
        } else {
            Toast.makeText(this, "页面初始化失败", Toast.LENGTH_SHORT).show();
        }
    }

    // 打开相册选择图片
    private void openGallery() {
        Intent intent = new Intent();
        intent.setType("image/*");
        intent.setAction(Intent.ACTION_GET_CONTENT);
        startActivityForResult(Intent.createChooser(intent, "选择图片"), PICK_IMAGE_REQUEST);
    }

    // 处理图片选择结果
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICK_IMAGE_REQUEST && resultCode == RESULT_OK && data != null && data.getData() != null) {
            selectedImageUri = data.getData();
            if (ivAvatar != null) {
                ivAvatar.setImageURI(selectedImageUri);
            }
        }
    }

    // 复制图片到应用内部存储
    private String copyImageToInternalStorage(Uri uri) {
        try {
            ContentResolver contentResolver = getContentResolver();
            InputStream inputStream = contentResolver.openInputStream(uri);
            
            // 创建目标文件
            File outputDir = new File(getFilesDir(), "avatars");
            if (!outputDir.exists()) {
                outputDir.mkdirs();
            }
            String fileName = "avatar_" + System.currentTimeMillis() + ".jpg";
            File outputFile = new File(outputDir, fileName);
            
            // 复制文件
            FileOutputStream outputStream = new FileOutputStream(outputFile);
            byte[] buffer = new byte[1024];
            int length;
            while ((length = inputStream.read(buffer)) > 0) {
                outputStream.write(buffer, 0, length);
            }
            outputStream.flush();
            outputStream.close();
            inputStream.close();
            
            // 返回文件URI
            return Uri.fromFile(outputFile).toString();
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    // 处理权限请求结果
    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQUEST_READ_EXTERNAL_STORAGE || requestCode == PERMISSION_REQUEST_READ_MEDIA_IMAGES) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                openGallery();
            } else {
                Toast.makeText(this, "需要读取存储权限才能选择头像", Toast.LENGTH_SHORT).show();
            }
        }
    }
}


