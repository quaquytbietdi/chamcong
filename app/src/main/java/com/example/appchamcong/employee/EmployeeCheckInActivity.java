package com.example.appchamcong.employee;

import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.CountDownTimer;
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
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class EmployeeCheckInActivity extends AppCompatActivity {

    private static final long TICK_INTERVAL_MS = 200L;
    private static final int QR_SIZE_PX = 600;
    private static final long QR_VALIDITY_DURATION_MS = 30000L; // 30 giây

    private TextView tvEmployeeWelcome;
    private TextView tvTodayStatus;
    private ImageView imgDynamicQR;
    private ProgressBar progressQrLoading;
    private TextView tvQrError;
    private ProgressBar progressQRTimer;
    private TextView tvTimerCountdown;
    private MaterialButton btnScanPortalQR;
    private MaterialButton btnScanRfid;

    private DatabaseReference databaseReference;
    private FirebaseAuth mAuth;
    private CountDownTimer refreshTimer;
    private ValueEventListener checkinListener;

    private String employeeId;

    @Nullable
    private Long checkedInAtMillis = null;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_employee_checkin);

        mAuth = FirebaseAuth.getInstance();
        databaseReference = FirebaseDatabase.getInstance().getReference();

        bindViews();
        updateTodayStatusUi();

        if (mAuth.getCurrentUser() != null) {
            employeeId = mAuth.getCurrentUser().getUid();
            loadEmployeeProfile(employeeId);
        } else {
            employeeId = "unknown_user";
            tvEmployeeWelcome.setText(getString(R.string.employee_welcome_prefix) + " Nhân viên");
        }

        // Nút giả lập quét thành công tại cổng (dùng để test thay cho ESP32-CAM)
        btnScanPortalQR.setOnClickListener(v -> {
            simulateCheckInSuccess();
        });

        btnScanRfid.setOnClickListener(v ->
                Toast.makeText(this, "Đưa thẻ RFID lại gần để chấm công…", Toast.LENGTH_SHORT).show());
    }

    private void bindViews() {
        tvEmployeeWelcome = findViewById(R.id.tvEmployeeWelcome);
        tvTodayStatus = findViewById(R.id.tvTodayStatus);
        imgDynamicQR = findViewById(R.id.imgDynamicQR);
        progressQrLoading = findViewById(R.id.progressQrLoading);
        tvQrError = findViewById(R.id.tvQrError);
        progressQRTimer = findViewById(R.id.progressQRTimer);
        tvTimerCountdown = findViewById(R.id.tvTimerCountdown);
        btnScanPortalQR = findViewById(R.id.btnScanPortalQR);
        btnScanRfid = findViewById(R.id.btnScanRfid);
    }

    @Override
    protected void onResume() {
        super.onResume();
        listenCheckinStatusFromFirebase();
        fetchNewTokenAndRender();
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (refreshTimer != null) refreshTimer.cancel();
        if (checkinListener != null && employeeId != null) {
            databaseReference.child("Users").child(employeeId).removeEventListener(checkinListener);
        }
    }

    private void loadEmployeeProfile(String uid) {
        databaseReference.child("Users").child(uid).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    String fullName = snapshot.child("fullname").getValue(String.class);
                    if (fullName != null && !fullName.isEmpty()) {
                        tvEmployeeWelcome.setText(getString(R.string.employee_welcome_prefix) + " " + fullName);
                    } else {
                        tvEmployeeWelcome.setText(getString(R.string.employee_welcome_prefix) + " Nhân viên");
                    }
                } else {
                    tvEmployeeWelcome.setText(getString(R.string.employee_welcome_prefix) + " Nhân viên");
                }
            }

            @Override
            public void onCancelled(DatabaseError error) {
                tvEmployeeWelcome.setText(getString(R.string.employee_welcome_prefix) + " Nhân viên");
            }
        });
    }

    private void listenCheckinStatusFromFirebase() {
        if (employeeId == null) return;
        DatabaseReference userRef = databaseReference.child("Users").child(employeeId);

        checkinListener = new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                if (snapshot.exists() && snapshot.hasChild("checkedInMillis")) {
                    checkedInAtMillis = snapshot.child("checkedInMillis").getValue(Long.class);
                    updateTodayStatusUi();
                }
            }

            @Override
            public void onCancelled(DatabaseError error) {
            }
        };
        userRef.addValueEventListener(checkinListener);
    }

    /** Tạo và đồng bộ đầy đủ các thuộc tính QrTokens lên Firebase Realtime Database */
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
                tvTimerCountdown.setText(
                        getString(R.string.employee_qr_refresh_prefix) + " " + seconds + "s");
            }

            @Override
            public void onFinish() {
                fetchNewTokenAndRender();
            }
        };
        refreshTimer.start();
    }

    // Hàm mô phỏng hành động quét cổng thành công (thay thế tạm thời cho ESP32-CAM)
    private void simulateCheckInSuccess() {
        if (employeeId == null) return;
        long checkInTime = System.currentTimeMillis();

        DatabaseReference userRef = databaseReference.child("Users").child(employeeId);
        Map<String, Object> updateData = new HashMap<>();
        updateData.put("checkedInMillis", checkInTime);
        updateData.put("status", "ON_TIME");

        userRef.updateChildren(updateData).addOnSuccessListener(aVoid -> {
            checkedInAtMillis = checkInTime;
            updateTodayStatusUi();

            String timeFormatted = new SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(checkInTime);
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
        if (checkedInAtMillis == null) {
            tvTodayStatus.setText(R.string.employee_status_not_checked_in);
            tvTodayStatus.setTextColor(ContextCompat.getColor(this, R.color.status_error));
        } else {
            String time = new SimpleDateFormat("HH:mm", Locale.getDefault()).format(checkedInAtMillis);
            tvTodayStatus.setText(getString(R.string.employee_status_checked_in, time));
            tvTodayStatus.setTextColor(ContextCompat.getColor(this, R.color.status_success));
        }
    }
}