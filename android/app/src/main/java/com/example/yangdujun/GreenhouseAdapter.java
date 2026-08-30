package com.example.yangdujun;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class GreenhouseAdapter extends RecyclerView.Adapter<GreenhouseAdapter.VH> {

    public interface Listener {
        void onView(Greenhouse item);
        void onRemove(Greenhouse item);
    }

    private final List<Greenhouse> data;
    private final Listener listener;

    public GreenhouseAdapter(List<Greenhouse> data, Listener listener) {
        this.data = data;
        this.listener = listener;
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_greenhouse, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int position) {
        Greenhouse item = data.get(position);
        h.tvTitle.setText(item.name);
        h.tvDistance.setText(item.distance);

        h.btnView.setOnClickListener(v -> listener.onView(item));
        h.btnDelete.setOnClickListener(v -> listener.onRemove(item));
    }

    @Override
    public int getItemCount() {
        return data.size();
    }

    public void removeAt(int pos) {
        data.remove(pos);
        notifyItemRemoved(pos);
    }

    static class VH extends RecyclerView.ViewHolder {
        TextView tvTitle, tvDistance;
        Button btnView, btnDelete;
        VH(@NonNull View itemView) {
            super(itemView);
            tvTitle = itemView.findViewById(R.id.tvTitle);
            tvDistance = itemView.findViewById(R.id.tvDistance);
            btnView = itemView.findViewById(R.id.btnView);
            btnDelete = itemView.findViewById(R.id.btnDelete);
        }
    }
}



