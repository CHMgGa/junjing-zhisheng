package com.example.yangdujun;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.example.yangdujun.utils.GreenhouseHomeStorage;
import com.example.yangdujun.utils.UserStorage;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import okhttp3.MediaType;
import okhttp3.RequestBody;

public class GreenhouseListActivity extends AppCompatActivity {

    private final List<Greenhouse> list = new ArrayList<>();
    private GreenhouseAdapter adapter;
    private UserStorage userStorage;
    private GreenhouseHomeStorage greenhouseHomeStorage;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_greenhouse_list);

        ImageView ivBack = findViewById(R.id.ivBack);
        ivBack.setOnClickListener(v -> finish());

        userStorage = new UserStorage();
        greenhouseHomeStorage = new GreenhouseHomeStorage(this);
        
        // 注释掉登录检查，让未登录用户也能查看大棚列表
        /*
        String userToken = userStorage.getToken();
        if (userToken == null || userToken.isEmpty()) {
            Toast.makeText(this, "用户未登录", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        */

        RecyclerView rv = findViewById(R.id.rv);
        if (rv != null) {
            rv.setLayoutManager(new LinearLayoutManager(this));

            adapter = new GreenhouseAdapter(list, new GreenhouseAdapter.Listener() {
                @Override
                public void onView(Greenhouse item) {
                    // TODO: 查看页（如果你有查看大棚详情页）
                    // startActivity(new Intent(GreenhouseListActivity.this, GreenhouseDetailActivity.class));
                }

                @Override
                public void onRemove(Greenhouse item) {
                    // 使用对象引用而不是位置来删除
                    showRemoveDialog(item);
                }
            });
            rv.setAdapter(adapter);

            // 获取大棚列表
            getGreenhouseList();
        }

        // 底部大加号区域
        View addArea = findViewById(R.id.addArea);
        if (addArea != null) {
            addArea.setOnClickListener(v ->
                    startActivity(new Intent(this, AddGreenhouseActivity.class)));
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        getGreenhouseList();
    }

    private void getGreenhouseList() {
        // 直接从本地存储加载大棚列表，与大棚页面保持一致
        loadGreenhousesFromStorage();
    }

    // 从本地存储加载大棚列表
    private void loadGreenhousesFromStorage() {
        new Handler(Looper.getMainLooper()).post(() -> {
            list.clear();
            
            // 从GreenhouseHomeStorage获取大棚列表
            if (greenhouseHomeStorage != null) {
                List<String> greenhouseNames = greenhouseHomeStorage.getGreenhouseList();
                List<String> greenhouseIds = greenhouseHomeStorage.getGreenhouseIdList();
                
                // 确保列表不为null
                if (greenhouseNames != null && greenhouseIds != null) {
                    android.util.Log.d("GreenhouseListActivity", "加载大棚列表 - 名称数量: " + greenhouseNames.size() + ", ID数量: " + greenhouseIds.size());
                    
                    // 遍历大棚列表，跳过第一个"请选择大棚"选项
                    for (int i = 1; i < greenhouseNames.size() && i < greenhouseIds.size(); i++) {
                        String id = greenhouseIds.get(i);
                        String name = greenhouseNames.get(i);
                        // 距离默认为0km
                        String distance = "0km";
                        list.add(new Greenhouse(id, name, distance));
                        android.util.Log.d("GreenhouseListActivity", "添加大棚: " + name + " (ID: " + id + ")");
                    }
                }
            }
            
            if (adapter != null) {
                adapter.notifyDataSetChanged();
            }
            
            android.util.Log.d("GreenhouseListActivity", "大棚列表加载完成，总数: " + list.size());
            
            if (list.isEmpty()) {
                Toast.makeText(GreenhouseListActivity.this, "暂无大棚数据", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void deleteGreenhouse(Greenhouse greenhouse) {
        if (greenhouse == null) {
            Toast.makeText(this, "大棚信息错误", Toast.LENGTH_SHORT).show();
            return;
        }
        
        if ("1".equals(greenhouse.id)) {
            Toast.makeText(this, "默认大棚不能删除", Toast.LENGTH_SHORT).show();
            return;
        }

        // 直接从本地存储删除大棚
        new Thread(() -> {
            if (greenhouseHomeStorage != null) {
                List<String> greenhouseNames = greenhouseHomeStorage.getGreenhouseList();
                List<String> greenhouseIds = greenhouseHomeStorage.getGreenhouseIdList();

                // 找到要删除的大棚索引
                int deleteIndex = -1;
                if (greenhouseIds != null) {
                    for (int i = 0; i < greenhouseIds.size(); i++) {
                        if (greenhouseIds.get(i).equals(greenhouse.id)) {
                            deleteIndex = i;
                            break;
                        }
                    }
                }

                // 允许删除索引大于0的大棚（索引0是"请选择大棚"，不能删除）
                if (deleteIndex > 0 && greenhouseNames != null && greenhouseIds != null) {
                    // 从列表中移除
                    greenhouseNames.remove(deleteIndex);
                    greenhouseIds.remove(deleteIndex);

                    // 保存更新后的列表
                    greenhouseHomeStorage.saveGreenhouseList(greenhouseNames, greenhouseIds);

                    new Handler(Looper.getMainLooper()).post(() -> {
                        list.remove(greenhouse);
                        if (adapter != null) {
                            adapter.notifyDataSetChanged();
                        }
                        Toast.makeText(GreenhouseListActivity.this, "删除成功", Toast.LENGTH_SHORT).show();
                    });
                } else if (deleteIndex == 0) {
                    new Handler(Looper.getMainLooper()).post(() -> {
                        Toast.makeText(GreenhouseListActivity.this, "不能删除：请选择大棚选项", Toast.LENGTH_SHORT).show();
                    });
                } else {
                    new Handler(Looper.getMainLooper()).post(() -> {
                        Toast.makeText(GreenhouseListActivity.this, "删除失败：大棚不存在", Toast.LENGTH_SHORT).show();
                    });
                }
            } else {
                new Handler(Looper.getMainLooper()).post(() -> {
                    Toast.makeText(GreenhouseListActivity.this, "删除失败：存储服务异常", Toast.LENGTH_SHORT).show();
                });
            }
        }).start();
    }

    private void showRemoveDialog(Greenhouse item) {
        if (item == null) {
            Toast.makeText(this, "大棚信息错误", Toast.LENGTH_SHORT).show();
            return;
        }
        
        View v = getLayoutInflater().inflate(R.layout.dialog_confirm_remove, null);
        if (v == null) {
            Toast.makeText(this, "对话框加载失败", Toast.LENGTH_SHORT).show();
            return;
        }

        androidx.appcompat.app.AlertDialog dialog = new androidx.appcompat.app.AlertDialog.Builder(this)
                .setView(v)
                .setCancelable(true)
                .create();

        TextView tvCancel = v.findViewById(R.id.tvCancel);
        TextView tvOk = v.findViewById(R.id.tvOk);

        // 防止 TextView 没有点击反馈：确保可点击
        if (tvCancel != null) {
            tvCancel.setClickable(true);
            tvCancel.setOnClickListener(x -> dialog.dismiss());
        }

        if (tvOk != null) {
            tvOk.setClickable(true);
            tvOk.setOnClickListener(x -> {
                dialog.dismiss();
                deleteGreenhouse(item);
            });
        }

        dialog.show();
    }
}



