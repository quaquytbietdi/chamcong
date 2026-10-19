package com.example.appchamcong.employee;

import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.example.appchamcong.R;
import com.example.appchamcong.util.QrCodeGenerator;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class EmployeeCheckInActivity extends AppCompatActivity {

    private static final long TICK_INTERVAL_MS = 200L;
    private static final int QR_SIZE_PX = 600;
    private static final long QR_VALIDITY_DURATION_MS = 30000L; // 30 giây

    // Các thành phần UI
    private TextView tvEmployeeWelcome;
    private TextView tvTodayStatus;
    private ImageView imgDynamicQR;
    private ProgressBar progressQrLoading;
    private TextView tvQrError;
    private ProgressBar progressQRTimer;
    private TextView tvTimerCountdown;
    private MaterialButton btnScanPortalQR;
    private MaterialButton btnScanRfid;

    // Các biến hiển thị Ngày & Giờ thời gian thực
    private TextView tvDate;
    private TextView tvClockTime;
    private Handler timeHandler = new Handler(Looper.getMainLooper());
    private Runnable timeRunnable;

    private DatabaseReference databaseReference;
    private FirebaseAuth mAuth;
    private CountDownTimer refreshTimer;
    private ValueEventListener checkinListener;

    private String employeeId = "1"; // Mặc định ID số
    private String employeeFullName = "Nhân viên";
    @Nullable
    private String checkedInTimeStr = null;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_employee_checkin);

        mAuth = FirebaseAuth.getInstance();
        databaseReference = FirebaseDatabase.getInstance().getReference();

        bindViews();
        updateTodayStatusUi();

        if (mAuth.getCurrentUser() != null) {
            // Tra cứu ID số dựa trên Auth UID đã ánh xạ khi đăng ký
            loadEmployeeShortId();
        } else {
            tvEmployeeWelcome.setText("Xin chào, Nhân viên");
        }

        // Nút giả lập quét thành công tại cổng (lưu vào AttendanceLogs/{ngay}/{id})
        btnScanPortalQR.setOnClickListener(v -> simulateCheckInSuccess());

        btnScanRfid.setOnClickListener(v ->
                Toast.makeText(this, "Đưa thẻ RFID lại gần để chấm công…", Toast.LENGTH_SHORT).show());
    }

    private void bindViews() {
        tvEmployeeWelcome = findViewById(R.id.tvEmployeeWelcome);
        tvTodayStatus = findViewById(R.id.tvTodayStatusBadge);
        imgDynamicQR = findViewById(R.id.imgDynamicQR);
        progressQrLoading = findViewById(R.id.progressQrLoading);
        tvQrError = findViewById(R.id.tvQrError);
        progressQRTimer = findViewById(R.id.progressQRTimer);
        tvTimerCountdown = findViewById(R.id.tvTimerCountdown);
        btnScanPortalQR = findViewById(R.id.btnScanPortalQR);
        btnScanRfid = findViewById(R.id.btnScanRfid);

        tvDate = findViewById(R.id.tvDate);
        tvClockTime = findViewById(R.id.tvClockTime);
    }

    @Override
    protected void onResume() {
        super.onResume();
        startRealTimeClock();
        if (employeeId != null) {
            listenCheckinStatusFromFirebase();
            fetchNewTokenAndRender();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (refreshTimer != null) refreshTimer.cancel();
        if (checkinListener != null && employeeId != null) {
            String dateKey = new SimpleDateFormat("dd-MM-yyyy", Locale.getDefault()).format(new Date());
            databaseReference.child("AttendanceLogs").child(dateKey).child(employeeId).removeEventListener(checkinListener);
        }
        stopRealTimeClock();
    }

    private void startRealTimeClock() {
        timeRunnable = new Runnable() {
            @Override
            public void run() {
                SimpleDateFormat dateFormat = new SimpleDateFormat("EEEE, dd/MM/yyyy", new Locale("vi", "VN"));
                String currentDate = dateFormat.format(new Date());

                SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm", Locale.getDefault());
                String currentTime = timeFormat.format(new Date());

                if (tvDate != null) tvDate.setText(currentDate);
                if (tvClockTime != null) tvClockTime.setText(currentTime);

                timeHandler.postDelayed(this, 1000);
            }
        };
        timeHandler.post(timeRunnable);
    }

    private void stopRealTimeClock() {
        if (timeHandler != null && timeRunnable != null) {
            timeHandler.removeCallbacks(timeRunnable);
        }
    }

    /** Tra cứu ID số (1, 2, 3...) từ Auth UID */
    private void loadEmployeeShortId() {
        String authUid = mAuth.getCurrentUser().getUid();
        databaseReference.child("UserMapping").child(authUid).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    employeeId = snapshot.getValue(String.class);
                    loadEmployeeProfile(employeeId);
                    listenCheckinStatusFromFirebase();
                    fetchNewTokenAndRender();
                } else {
                    employeeId = "1";
                    loadEmployeeProfile(employeeId);
                }
            }

            @Override
            public void onCancelled(DatabaseError error) {
                employeeId = "1";
            }
        });
    }

    private void loadEmployeeProfile(String id) {
        databaseReference.child("Users").child(id).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    String fullname = snapshot.child("fullname").getValue(String.class);
                    if (fullname != null && !fullname.isEmpty()) {
                        employeeFullName = fullname;
                        tvEmployeeWelcome.setText("Xin chào, " + fullname);
                    } else {
                        tvEmployeeWelcome.setText("Xin chào, Nhân viên");
                    }
                }
            }

            @Override
            public void onCancelled(DatabaseError error) {
                tvEmployeeWelcome.setText("Xin chào, Nhân viên");
            }
        });
    }

    private void listenCheckinStatusFromFirebase() {
        if (employeeId == null) return;
        String dateKey = new SimpleDateFormat("dd-MM-yyyy", Locale.getDefault()).format(new Date());
        DatabaseReference logRef = databaseReference.child("AttendanceLogs").child(dateKey).child(employeeId);

        checkinListener = new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                if (snapshot.exists() && snapshot.hasChild("gioChamCong")) {
                    checkedInTimeStr = snapshot.child("gioChamCong").getValue(String.class);
                } else {
                    checkedInTimeStr = null;
                }
                updateTodayStatusUi();
            }

            @Override
            public void onCancelled(DatabaseError error) {}
        };
        logRef.addValueEventListener(checkinListener);
    }

    private void fetchNewTokenAndRender() {
        showQrLoading(true);

        long currentTime = System.currentTimeMillis();
        long slot = currentTime / QR_VALIDITY_DURATION_MS;
        String rawToken = "ATTENDANCE_" + employeeId + "_" + slot;
        long expiresAt = (slot + 1) * QR_VALIDITY_DURATION_MS;

        DatabaseReference qrRef = databaseReference.child("QrTokens").child(employeeId);
        Map<String, Object> qrData = new HashMap<>();
        qrData.put("token", rawToken);
        qrData.put("expiresAt", expiresAt);
        qrData.put("generatedAt", currentTime);
        qrData.put("slot", slot);
        qrData.put("active", true);

        qrRef.setValue(qrData).addOnSuccessListener(aVoid -> {
            renderQrBitmap(rawToken);
            showQrLoading(false);
            startCountdown(expiresAt);
        }).addOnFailureListener(e -> {
            showQrError(true);
            showQrLoading(false);
            imgDynamicQR.postDelayed(this::fetchNewTokenAndRender, 3000);
        });
    }

    private void renderQrBitmap(String qrToken) {
        Bitmap bitmap = QrCodeGenerator.generate(qrToken, QR_SIZE_PX);
        if (bitmap != null) {
            imgDynamicQR.setImageBitmap(bitmap);
            showQrError(false);
        } else {
            showQrError(true);
        }
    }

    private void startCountdown(long expiresAtMillis) {
        long remainingMs = Math.max(0, expiresAtMillis - System.currentTimeMillis());
        progressQRTimer.setMax((int) remainingMs);

        if (refreshTimer != null) refreshTimer.cancel();
        refreshTimer = new CountDownTimer(remainingMs, TICK_INTERVAL_MS) {
            @Override
            public void onTick(long millisUntilFinished) {
                progressQRTimer.setProgress((int) millisUntilFinished);
                int seconds = (int) Math.ceil(millisUntilFinished / 1000.0);
                tvTimerCountdown.setText("Mã sẽ làm mới sau: " + seconds + "s");
            }

            @Override
            public void onFinish() {
                fetchNewTokenAndRender();
            }
        };
        refreshTimer.start();
    }

    // Mô phỏng chấm công: Lưu vào AttendanceLogs/{dd-MM-yyyy}/{id}
    private void simulateCheckInSuccess() {
        if (employeeId == null) return;

        String dateKey = new SimpleDateFormat("dd-MM-yyyy", Locale.getDefault()).format(new Date());
        String timeFormatted = new SimpleDateFormat("HH:mm", Locale.getDefault()).format(new Date());

        DatabaseReference logRef = databaseReference.child("AttendanceLogs")
                .child(dateKey)
                .child(employeeId);

        Map<String, Object> checkInData = new HashMap<>();
        checkInData.put("employeeName", employeeFullName);
        checkInData.put("gioChamCong", timeFormatted);
        checkInData.put("status", "Đã chấm công");

        logRef.setValue(checkInData).addOnSuccessListener(aVoid -> {
            checkedInTimeStr = timeFormatted;
            updateTodayStatusUi();
            Toast.makeText(this, "Chấm công thành công lúc " + timeFormatted, Toast.LENGTH_LONG).show();
        }).addOnFailureListener(e -> {
            Toast.makeText(this, "Lỗi kết nối Firebase: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        });
    }

    private void showQrLoading(boolean loading) {
        progressQrLoading.setVisibility(loading ? View.VISIBLE : View.GONE);
    }

    private void showQrError(boolean hasError) {
        tvQrError.setVisibility(hasError ? View.VISIBLE : View.GONE);
        imgDynamicQR.setVisibility(hasError ? View.GONE : View.VISIBLE);
    }

    private void updateTodayStatusUi() {
        if (checkedInTimeStr == null || checkedInTimeStr.isEmpty()) {
            tvTodayStatus.setText("Chưa chấm");
            tvTodayStatus.setTextColor(ContextCompat.getColor(this, R.color.status_error));
        } else {
            tvTodayStatus.setText(checkedInTimeStr);
            tvTodayStatus.setTextColor(ContextCompat.getColor(this, R.color.status_success));
        }
    }
}