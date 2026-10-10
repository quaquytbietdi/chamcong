package com.example.appchamcong.admin;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.example.appchamcong.R;
import com.example.appchamcong.model.Shift;
import com.google.android.material.card.MaterialCardView;

import java.util.ArrayList;
import java.util.List;

public class ShiftAdapter
        extends RecyclerView.Adapter<ShiftAdapter.ShiftViewHolder> {

    public interface OnShiftSelectedListener {
        void onShiftSelected(Shift shift);
    }

    private final List<Shift> items = new ArrayList<>();
    private final OnShiftSelectedListener listener;

    private int selectedPosition = 0;

    public ShiftAdapter(OnShiftSelectedListener listener) {
        this.listener = listener;
    }

    public void submitList(List<Shift> newItems) {
        items.clear();

        if (newItems != null) {
            items.addAll(newItems);
        }

        if (items.isEmpty()) {
            selectedPosition = RecyclerView.NO_POSITION;
        } else if (selectedPosition == RecyclerView.NO_POSITION
                || selectedPosition >= items.size()) {
            selectedPosition = 0;
        }

        notifyDataSetChanged();
    }

    public Shift getSelectedShift() {
        if (selectedPosition == RecyclerView.NO_POSITION
                || selectedPosition >= items.size()) {
            return null;
        }

        return items.get(selectedPosition);
    }

    @NonNull
    @Override
    public ShiftViewHolder onCreateViewHolder(
            @NonNull ViewGroup parent,
            int viewType
    ) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_shift_card, parent, false);

        return new ShiftViewHolder(view);
    }

    @Override
    public void onBindViewHolder(
            @NonNull ShiftViewHolder holder,
            int position
    ) {
        holder.bind(
                items.get(position),
                position == selectedPosition
        );
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    class ShiftViewHolder extends RecyclerView.ViewHolder {

        private final MaterialCardView cardShiftItem;
        private final TextView tvShiftName;
        private final TextView tvShiftTime;
        private final TextView tvShiftCount;

        ShiftViewHolder(@NonNull View itemView) {
            super(itemView);

            cardShiftItem = itemView.findViewById(R.id.cardShiftItem);
            tvShiftName = itemView.findViewById(R.id.tvShiftName);
            tvShiftTime = itemView.findViewById(R.id.tvShiftTime);
            tvShiftCount = itemView.findViewById(R.id.tvShiftCount);

            itemView.setOnClickListener(v -> {
                int position = getBindingAdapterPosition();

                if (position == RecyclerView.NO_POSITION) {
                    return;
                }

                int oldPosition = selectedPosition;
                selectedPosition = position;

                if (oldPosition != RecyclerView.NO_POSITION) {
                    notifyItemChanged(oldPosition);
                }

                notifyItemChanged(selectedPosition);

                if (listener != null) {
                    listener.onShiftSelected(items.get(selectedPosition));
                }
            });
        }

        void bind(Shift shift, boolean selected) {
            tvShiftName.setText(shift.getName());
            tvShiftTime.setText(shift.getTimeLabel());

            // Chưa có Attendance Firebase nên chưa thể hiện số nhân viên
            tvShiftCount.setText(selected ? "Đang chọn" : "Chọn ca");

            int strokeColor;
            int backgroundColor;

            if (selected) {
                strokeColor = ContextCompat.getColor(
                        itemView.getContext(),
                        R.color.brand_primary
                );

                backgroundColor = ContextCompat.getColor(
                        itemView.getContext(),
                        R.color.status_success_bg
                );
            } else {
                strokeColor = ContextCompat.getColor(
                        itemView.getContext(),
                        R.color.divider
                );

                backgroundColor = ContextCompat.getColor(
                        itemView.getContext(),
                        R.color.white
                );
            }

            cardShiftItem.setStrokeColor(strokeColor);
            cardShiftItem.setCardBackgroundColor(backgroundColor);

            tvShiftName.setTextColor(
                    ContextCompat.getColor(
                            itemView.getContext(),
                            selected
                                    ? R.color.brand_primary_dark
                                    : R.color.text_primary
                    )
            );
        }
    }
}