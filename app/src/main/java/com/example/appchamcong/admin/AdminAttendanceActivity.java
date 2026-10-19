package com.example.appchamcong.admin;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.CalendarView;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.appchamcong.R;
import com.example.appchamcong.model.Shift;
import com.example.appchamcong.model.Staff;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

public class AdminAttendanceActivity extends AppCompatActivity {

    // ==================== VIEW ====================

    private TextView tvCurrentShift;
    private TextView tvAutoUpdateStatus;
    private TextView tvTitleStaffList;

    private EditText edtSearchStaff;
    private CalendarView calendarView;

    private RecyclerView recyclerShifts;
    private RecyclerView recyclerStaffAttendance;

    private ProgressBar progressLoading;
    private LinearLayout layoutEmptyState;

    private TextView tvChipOnTimeValue;
    private TextView tvChipLateValue;
    private TextView tvChipAbsentValue;

    // ==================== DATA ====================

    private DatabaseReference databaseReference;
    private StaffAdapter staffAdapter;
    private ShiftAdapter shiftAdapter;

    private final List<Staff> allStaff = new ArrayList<>();
    private final List<Shift> shiftList = new ArrayList<>();

    private String currentQuery = "";
    private Calendar selectedDate;
    private Shift selectedShift;

    // ==================== LIFECYCLE ====================

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_attendance);

        databaseReference = FirebaseDatabase.getInstance().getReference();

        bindViews();
        setupShifts();
        setupCalendar();
        setupStaffList();
        setupSearch();
        updateDateTitle();

        showLoading(true);
    }

    @Override
    protected void onStart() {
        super.onStart();
        tvAutoUpdateStatus.setText(R.string.admin_status_auto_update);

        // Tải dữ liệu chấm công thực tế theo ngày được chọn
        loadAttendanceDataForSelectedDate();
    }

    // ==================== BIND VIEW ====================

    private void bindViews() {
        tvCurrentShift = findViewById(R.id.tvCurrentShift);
        tvAutoUpdateStatus = findViewById(R.id.tvAutoUpdateStatus);
        tvTitleStaffList = findViewById(R.id.tvTitleStaffList);
        edtSearchStaff = findViewById(R.id.edtSearchStaff);
        calendarView = findViewById(R.id.calendarView);
        recyclerShifts = findViewById(R.id.recyclerShifts);
        recyclerStaffAttendance = findViewById(R.id.recyclerStaffAttendance);
        progressLoading = findViewById(R.id.progressLoading);
        layoutEmptyState = findViewById(R.id.layoutEmptyState);

        // ==================== SUMMARY ====================
        View chipOnTime = findViewById(R.id.chipOnTime);
        View chipLate = findViewById(R.id.chipLate);
        View chipAbsent = findViewById(R.id.chipAbsent);

        tvChipOnTimeValue = chipOnTime.findViewById(R.id.tvChipValue);
        tvChipLateValue = chipLate.findViewById(R.id.tvChipValue);
        tvChipAbsentValue = chipAbsent.findViewById(R.id.tvChipValue);

        ((TextView) chipOnTime.findViewById(R.id.tvChipLabel)).setText(R.string.admin_count_on_time);
        ((TextView) chipLate.findViewById(R.id.tvChipLabel)).setText(R.string.admin_count_late);
        ((TextView) chipAbsent.findViewById(R.id.tvChipLabel)).setText(R.string.admin_count_absent);
    }

    // ==================== SHIFT ====================

    private void setupShifts() {
        shiftList.clear();
        shiftList.add(new Shift("morning", "Ca Sáng", 8, 0, 12, 0));
        shiftList.add(new Shift("afternoon", "Ca Chiều", 13, 0, 17, 0));
        shiftList.add(new Shift("evening", "Ca Tối", 18, 0, 22, 0));

        shiftAdapter = new ShiftAdapter(shift -> {
            selectedShift = shift;
            updateCurrentShiftUi();
            updateDateTitle();
            applyFilterAndRender();
        });

        recyclerShifts.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        recyclerShifts.setNestedScrollingEnabled(false);
        recyclerShifts.setAdapter(shiftAdapter);
        shiftAdapter.submitList(shiftList);

        selectedShift = shiftList.get(0);
        updateCurrentShiftUi();
    }

    // ==================== CALENDAR ====================

    private void setupCalendar() {
        selectedDate = Calendar.getInstance();
        setStartOfDay(selectedDate);

        calendarView.setDate(selectedDate.getTimeInMillis(), false, true);

        calendarView.setOnDateChangeListener((view, year, month, dayOfMonth) -> {
            selectedDate = Calendar.getInstance();
            selectedDate.set(year, month, dayOfMonth, 0, 0, 0);
            selectedDate.set(Calendar.MILLISECOND, 0);

            updateDateTitle();
            loadAttendanceDataForSelectedDate(); // Tải lại dữ liệu khi đổi ngày trên lịch
        });
    }

    private void setStartOfDay(Calendar calendar) {
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
    }

    // ==================== STAFF LIST ====================

    private void setupStaffList() {
        staffAdapter = new StaffAdapter();
        recyclerStaffAttendance.setLayoutManager(new LinearLayoutManager(this));
        recyclerStaffAttendance.setNestedScrollingEnabled(false);
        recyclerStaffAttendance.setAdapter(staffAdapter);
    }

    // ==================== SEARCH ====================

    private void setupSearch() {
        edtSearchStaff.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                currentQuery = s.toString().trim().toLowerCase(Locale.getDefault());
                applyFilterAndRender();
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });
    }

    // ==================== UI ====================

    private void updateCurrentShiftUi() {
        if (selectedShift == null) return;
        tvCurrentShift.setText(getString(R.string.admin_shift_prefix) + " " + selectedShift.getDisplayLabel());
    }

    private void updateDateTitle() {
        if (selectedDate == null) return;

        SimpleDateFormat formatter = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
        String dateText = formatter.format(selectedDate.getTime());

        String title = "Danh Sách Chấm Công - " + dateText;
        if (selectedShift != null) {
            title += " - " + selectedShift.getName();
        }
        tvTitleStaffList.setText(title);
    }

    // ==================== FIREBASE DATA SYNC ====================

    /**
     * Tải danh sách nhân viên từ Users/ và trạng thái chấm công từ AttendanceLogs/{ngay}/
     */
    private void loadAttendanceDataForSelectedDate() {
        showLoading(true);

        // 1. Định dạng ngày thành chuỗi khóa (ví dụ: "19-10-2026")
        SimpleDateFormat dateKeyFormat = new SimpleDateFormat("dd-MM-yyyy", Locale.getDefault());
        String dateKey = dateKeyFormat.format(selectedDate.getTime());

        // 2. Lấy danh sách toàn bộ nhân viên từ Users/
        databaseReference.child("Users").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot usersSnapshot) {
                List<Staff> staffList = new ArrayList<>();

                // 3. Lấy nhật ký chấm công của ngày tương ứng từ AttendanceLogs/{dateKey}
                databaseReference.child("AttendanceLogs").child(dateKey).addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot logsSnapshot) {
                        for (DataSnapshot userSnap : usersSnapshot.getChildren()) {
                            String id = userSnap.child("id").getValue(String.class);
                            String name = userSnap.child("fullname").getValue(String.class);

                            if (id == null) id = userSnap.getKey();
                            if (name == null) name = "Nhân viên";

                            Staff staff = new Staff();
                            staff.setId(id);
                            staff.setName(name);

                            // Kiểm tra xem nhân viên này đã chấm công trong ngày chưa
                            if (logsSnapshot.hasChild(id)) {
                                DataSnapshot logSnap = logsSnapshot.child(id);
                                String gioChamCong = logSnap.child("gioChamCong").getValue(String.class);

                                staff.setStatus(Staff.Status.ON_TIME);
                                staff.setGioChamCong(gioChamCong);
                            } else {
                                staff.setStatus(Staff.Status.ABSENT);
                                staff.setGioChamCong(null);
                            }

                            staffList.add(staff);
                        }

                        runOnUiThread(() -> {
                            allStaff.clear();
                            allStaff.addAll(staffList);
                            showLoading(false);
                            applyFilterAndRender();
                        });
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        showLoading(false);
                        Toast.makeText(AdminAttendanceActivity.this, "Lỗi tải logs: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                showLoading(false);
                Toast.makeText(AdminAttendanceActivity.this, "Lỗi tải Users: " + error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void applyFilterAndRender() {
        List<Staff> filtered = new ArrayList<>();

        for (Staff staff : allStaff) {
            if (staff == null) continue;

            String name = staff.getName() != null ? staff.getName().toLowerCase(Locale.getDefault()) : "";
            String id = staff.getId() != null ? staff.getId().toLowerCase(Locale.getDefault()) : "";

            if (currentQuery.isEmpty() || name.contains(currentQuery) || id.contains(currentQuery)) {
                filtered.add(staff);
            }
        }

        staffAdapter.submitList(filtered);
        updateSummaryCounts(allStaff);
        toggleEmptyState(filtered.isEmpty());
    }

    // ==================== SUMMARY ====================

    private void updateSummaryCounts(List<Staff> source) {
        int onTime = 0;
        int late = 0;
        int absent = 0;

        for (Staff staff : source) {
            if (staff == null || staff.getStatus() == null) continue;

            switch (staff.getStatus()) {
                case ON_TIME:
                    onTime++;
                    break;
                case LATE:
                    late++;
                    break;
                case ABSENT:
                    absent++;
                    break;
            }
        }

        tvChipOnTimeValue.setText(String.valueOf(onTime));
        tvChipLateValue.setText(String.valueOf(late));
        tvChipAbsentValue.setText(String.valueOf(absent));
    }

    // ==================== LOADING ====================

    private void showLoading(boolean loading) {
        progressLoading.setVisibility(loading ? View.VISIBLE : View.GONE);
        if (loading) {
            recyclerStaffAttendance.setVisibility(View.GONE);
            layoutEmptyState.setVisibility(View.GONE);
        }
    }

    private void toggleEmptyState(boolean isEmpty) {
        layoutEmptyState.setVisibility(isEmpty ? View.VISIBLE : View.GONE);
        recyclerStaffAttendance.setVisibility(isEmpty ? View.GONE : View.VISIBLE);
    }
}