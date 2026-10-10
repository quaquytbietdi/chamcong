package com.example.appchamcong.data;

import androidx.annotation.NonNull;

import com.example.appchamcong.model.Shift;
import com.example.appchamcong.model.Staff;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

public class AttendanceRepository {

    private final DatabaseReference usersRef;
    private ValueEventListener valueEventListener;

    public AttendanceRepository() {
        // Trỏ thẳng tới nhánh Users trên Firebase Realtime Database của bạn
        usersRef = FirebaseDatabase.getInstance().getReference("Users");
    }

    public interface OnAttendanceUpdateListener {
        void onUpdate(List<Staff> staffList);
    }

    public Shift getCurrentShift() {
        return new Shift(
                "morning",
                "Ca Sáng",
                8,
                0,
                12,
                0
        );
    }

    public void startAutoUpdate(OnAttendanceUpdateListener listener) {
        valueEventListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                List<Staff> staffList = new ArrayList<>();

                for (DataSnapshot userSnap : snapshot.getChildren()) {
                    String id = userSnap.child("id").getValue(String.class);
                    String name = userSnap.child("fullname").getValue(String.class);
                    String email = userSnap.child("email").getValue(String.class);
                    String role = userSnap.child("role").getValue(String.class);

                    // Chỉ lấy danh sách tài khoản là nhân viên (bỏ qua admin nếu có)
                    if ("admin".equals(role)) {
                        continue;
                    }

                    Staff staff = new Staff();
                    staff.setId(id != null ? id : userSnap.getKey());
                    staff.setName(name != null && !name.isEmpty() ? name : (email != null ? email : "Nhân viên"));

                    // Kiểm tra xem nhân viên đã có mốc thời gian chấm công chưa
                    if (userSnap.hasChild("checkedInMillis")) {
                        Long checkInTime = userSnap.child("checkedInMillis").getValue(Long.class);
                        staff.setCheckInTimeMillis(checkInTime);
                        staff.setStatus(Staff.Status.ON_TIME); // Đã chấm công -> ON_TIME
                    } else {
                        staff.setStatus(Staff.Status.ABSENT);  // Chưa chấm công -> ABSENT
                        staff.setCheckInTimeMillis(null);
                    }

                    staffList.add(staff);
                }

                if (listener != null) {
                    listener.onUpdate(staffList);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                // Xử lý lỗi đọc cơ sở dữ liệu nếu cần
            }
        };

        // Lắng nghe dữ liệu thời gian thực từ Firebase
        usersRef.addValueEventListener(valueEventListener);
    }

    public void stopAutoUpdate() {
        if (usersRef != null && valueEventListener != null) {
            usersRef.removeEventListener(valueEventListener);
        }
    }
}