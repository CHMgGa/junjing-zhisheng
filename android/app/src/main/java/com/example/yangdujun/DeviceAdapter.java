package com.example.yangdujun;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.ArrayList;
import java.util.List;

public class DeviceAdapter extends RecyclerView.Adapter<DeviceAdapter.ViewHolder> {
    private List<DeviceBean> originalList;
    private List<DeviceBean> currentList;
    private OnDeviceStatusChangeListener statusChangeListener;

    public DeviceAdapter(List<DeviceBean> list) {
        originalList = new ArrayList<>(list);
        currentList = new ArrayList<>(list);
    }

    public DeviceAdapter(List<DeviceBean> list, OnDeviceStatusChangeListener listener) {
        originalList = new ArrayList<>(list);
        currentList = new ArrayList<>(list);
        this.statusChangeListener = listener;
    }

    // 设备状态变化监听器接口
    public interface OnDeviceStatusChangeListener {
        void onStatusChange(DeviceBean device, boolean status);
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_device, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        DeviceBean bean = currentList.get(position);
        holder.tvDeviceType.setText(bean.getType());
        holder.tvDeviceName.setText(bean.getName());
        
        // 根据设备类型设置图标文字
        String deviceType = bean.getType();
        if ("采集设备".equals(deviceType)) {
            holder.tvDeviceTypeIcon.setText("采");
        } else if ("控制设备".equals(deviceType)) {
            holder.tvDeviceTypeIcon.setText("控");
        } else if ("监控设备".equals(deviceType)) {
            holder.tvDeviceTypeIcon.setText("监");
        } else if ("显示设备".equals(deviceType)) {
            holder.tvDeviceTypeIcon.setText("显");
        }
        
        // 设置状态
        boolean isOnline = isDeviceOnline(bean);
        if (isOnline) {
            holder.ivStatus.setImageResource(R.drawable.ic_status_online);
            holder.tvStatus.setText("在线");
            holder.tvStatus.setTextColor(holder.itemView.getResources().getColor(R.color.green_dark));
        } else {
            holder.ivStatus.setImageResource(R.drawable.ic_status_offline);
            holder.tvStatus.setText("离线");
            holder.tvStatus.setTextColor(holder.itemView.getResources().getColor(R.color.gray));
        }

        // 禁用点击事件，移除设备控制功能
        holder.itemView.setClickable(false);
        holder.itemView.setFocusable(false);
    }
    
    // 判断设备是否在线
    private boolean isDeviceOnline(DeviceBean device) {
        String deviceId = device.getDeviceId();
        String deviceType = device.getType();
        
        // 监控设备显示在线状态即可
        if ("监控设备".equals(deviceType)) {
            return true;
        }
        
        // 显示设备为显示屏显示在线
        if ("显示设备".equals(deviceType)) {
            return "display_1".equals(deviceId);
        }
        
        // 控制设备：只有灯、挡光板、风机在大棚首页时有on或者off的状态显示时才能显示在线状态
        if ("控制设备".equals(deviceType)) {
            // 这里需要根据实际的状态数据来判断，暂时返回设备的status属性
            // 实际实现中，应该从GreenhouseHomeStorage中获取设备状态
            return device.isStatus();
        }
        
        // 采集设备：有数据时显示在线，否则离线
        if ("采集设备".equals(deviceType)) {
            // 这里需要根据实际的传感器数据来判断，暂时返回设备的status属性
            // 实际实现中，应该从GreenhouseHomeStorage中获取传感器数据
            return device.isStatus();
        }
        
        // 默认返回设备的status属性
        return device.isStatus();
    }

     // 设备操作监听器接口
    public interface OnDeviceOperationListener {
        void onOperation(DeviceBean device, String operation);
    }

    @Override
    public int getItemCount() {
        return currentList.size();
    }

    // 筛选设备
    public void filter(String type) {
        currentList.clear();
        for (DeviceBean bean : originalList) {
            if ("显示设备".equals(type)) {
                // 只显示显示屏设备
                if ("显示设备".equals(bean.getType()) && "display_1".equals(bean.getDeviceId())) {
                    currentList.add(bean);
                }
            } else {
                // 其他类型设备正常筛选
                if (bean.getType().equals(type)) {
                    currentList.add(bean);
                }
            }
        }
        notifyDataSetChanged();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvDeviceType, tvDeviceName, tvStatus, tvDeviceTypeIcon;
        ImageView ivStatus;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvDeviceType = itemView.findViewById(R.id.tv_device_type);
            tvDeviceName = itemView.findViewById(R.id.tv_device_name);
            tvStatus = itemView.findViewById(R.id.tv_status);
            ivStatus = itemView.findViewById(R.id.iv_status);
            tvDeviceTypeIcon = itemView.findViewById(R.id.tv_device_type_icon);
        }
    }
}

