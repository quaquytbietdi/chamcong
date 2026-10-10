package com.example.appchamcong.admin;

import android.content.Context;
import android.text.format.DateFormat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.example.appchamcong.R;
import com.example.appchamcong.model.Staff;

import java.util.ArrayList;
import java.util.List;

public class StaffAdapter extends RecyclerView.Adapter<StaffAdapter.StaffViewHolder> {

    private final List<Staff> items = new ArrayList<>();

    public void submitList(List<Staff> newItems) {
        items.clear();
        items.addAll(newItems);
        notifyDataSetChanged(); // bản rút gọn: danh sách nhỏ, không cần DiffUtil
    }

    @NonNull
    @Override
    public StaffViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_staff_attendance, parent, false);
        return new StaffViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull StaffViewHolder holder, int position) {
        holder.bind(items.get(position));
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class StaffViewHolder extends RecyclerView.ViewHolder {
        private final TextView tvStaffName;
        private final TextView tvCheckInTime;
        private final TextView tvCheckOutTime;
        private final TextView tvWarning;
        private final ImageView imgStatusIcon;

        StaffViewHolder(@NonNull View itemView) {
            super(itemView);
            tvStaffName = itemView.findViewById(R.id.tvStaffName);
            tvCheckInTime = itemView.findViewById(R.id.tvCheckInTime);
            tvCheckOutTime = itemView.findViewById(R.id.tvCheckOutTime);
            tvWarning = itemView.findViewById(R.id.tvWarning);
            imgStatusIcon = itemView.findViewById(R.id.imgStatusIcon);
        }

        void bind(Staff staff) {
            Context context = itemView.getContext();
            tvStaffName.setText(staff.getName());

            tvCheckInTime.setText(context.getString(R.string.staff_checkin_label,
                    staff.getCheckInTimeMillis() != null ? formatTime(staff.getCheckInTimeMillis()) : "--:--"));

            tvCheckOutTime.setText(staff.getCheckOutTimeMillis() != null
                    ? context.getString(R.string.staff_checkout_label, formatTime(staff.getCheckOutTimeMillis()))
                    : context.getString(R.string.staff_checkout_pending));

            boolean isLate = staff.getStatus() == Staff.Status.LATE;
            tvWarning.setVisibility(isLate ? View.VISIBLE : View.GONE);

            switch (staff.getStatus()) {
                case ON_TIME:
                    imgStatusIcon.setImageResource(android.R.drawable.checkbox_on_background);
                    imgStatusIcon.setColorFilter(ContextCompat.getColor(context, R.color.status_success));
                    break;
                case LATE:
                    imgStatusIcon.setImageResource(android.R.drawable.ic_dialog_alert);
                    imgStatusIcon.setColorFilter(ContextCompat.getColor(context, R.color.status_warning));
                    break;
                case ABSENT:
                default:
                    imgStatusIcon.setImageResource(android.R.drawable.ic_delete);
                    imgStatusIcon.setColorFilter(ContextCompat.getColor(context, R.color.status_error));
                    break;
            }
        }

        private String formatTime(long millis) {
            return DateFormat.format("HH:mm", millis).toString();
        }
    }
}
