package com.example.appchamcong.login;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.example.appchamcong.R;
import com.example.appchamcong.admin.AdminAttendanceActivity;
import com.example.appchamcong.employee.EmployeeCheckInActivity;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;

public class activity_login extends AppCompatActivity {
    private EditText edtEmail, edtPassword;
    private Button btnLogin;
    private TextView tvForgotPassword, tvRegister;
    private FirebaseAuth mAuth;
    private DatabaseReference mDatabase;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        // Khởi tạo Firebase Auth và Database
        mAuth = FirebaseAuth.getInstance();
        mDatabase = FirebaseDatabase.getInstance().getReference();

        // Ánh xạ các View từ file XML
        edtEmail = findViewById(R.id.edtEmail);
        edtPassword = findViewById(R.id.edtPassword);
        btnLogin = findViewById(R.id.btnLogin);
        tvForgotPassword = findViewById(R.id.tvForgotPassword);
        tvRegister = findViewById(R.id.tvRegister);

        // Xử lý sự kiện nút Đăng nhập
        btnLogin.setOnClickListener(v -> handleLogin());

        // Xử lý sự kiện chuyển sang màn hình Đăng ký nhân viên mới
        tvRegister.setOnClickListener(v -> {
            Intent intent = new Intent(activity_login.this, activity_register.class);
            startActivity(intent);
        });

        // Xử lý sự kiện Quên mật khẩu
        tvForgotPassword.setOnClickListener(v -> showForgotPasswordDialog());
    }

    private void handleLogin() {
        String email = edtEmail.getText().toString().trim();
        String password = edtPassword.getText().toString().trim();

        if (TextUtils.isEmpty(email) || TextUtils.isEmpty(password)) {
            Toast.makeText(this, "Vui lòng nhập đầy đủ email và mật khẩu", Toast.LENGTH_SHORT).show();
            return;
        }

        mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        checkUserRole();
                    } else {
                        Toast.makeText(activity_login.this, "Đăng nhập thất bại: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void checkUserRole() {
        if (mAuth.getCurrentUser() == null) return;

        String authUid = mAuth.getCurrentUser().getUid();

        // 1. Tra cứu thông qua bảng UserMapping để lấy ra ID số (ví dụ: "1", "2", "3"...)
        mDatabase.child("UserMapping").child(authUid).get().addOnSuccessListener(mappingSnapshot -> {
            if (mappingSnapshot.exists()) {
                String shortId = mappingSnapshot.getValue(String.class);

                // 2. Truy cập vào Users/{shortId} để đọc quyền "role"
                mDatabase.child("Users").child(shortId).get().addOnSuccessListener(userSnapshot -> {
                    if (userSnapshot.exists()) {
                        String role = userSnapshot.child("role").getValue(String.class);

                        if ("admin".equals(role)) {
                            // CHUYỂN SANG MÀN HÌNH ADMIN (QUẢN LÝ)
                            Toast.makeText(this, "Đăng nhập thành công với quyền Quản lý (Admin)", Toast.LENGTH_SHORT).show();
                            Intent intent = new Intent(activity_login.this, AdminAttendanceActivity.class);
                            startActivity(intent);
                        } else {
                            // CHUYỂN SANG MÀN HÌNH NHÂN VIÊN (EMPLOYEE)
                            Toast.makeText(this, "Đăng nhập thành công", Toast.LENGTH_SHORT).show();
                            Intent intent = new Intent(activity_login.this, EmployeeCheckInActivity.class);
                            startActivity(intent);
                        }
                        finish();
                    } else {
                        Toast.makeText(this, "Không tìm thấy thông tin tài khoản trên hệ thống!", Toast.LENGTH_SHORT).show();
                    }
                }).addOnFailureListener(e -> {
                    Toast.makeText(this, "Lỗi đọc dữ liệu người dùng: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });

            } else {
                // Trường hợp tài khoản cũ chưa có mapping, fallback về kiểm tra trực tiếp UID hoặc báo lỗi
                Toast.makeText(this, "Tài khoản chưa được liên kết mã nhân viên!", Toast.LENGTH_SHORT).show();
            }
        }).addOnFailureListener(e -> {
            Toast.makeText(this, "Lỗi kết nối UserMapping: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        });
    }

    private void showForgotPasswordDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Khôi phục mật khẩu");

        final EditText input = new EditText(this);
        input.setHint("Nhập email đã đăng ký của bạn");
        input.setPadding(40, 30, 40, 30);
        builder.setView(input);

        builder.setPositiveButton("Gửi", (dialog, which) -> {
            String email = input.getText().toString().trim();
            if (!TextUtils.isEmpty(email)) {
                mAuth.sendPasswordResetEmail(email)
                        .addOnCompleteListener(task -> {
                            if (task.isSuccessful()) {
                                Toast.makeText(activity_login.this, "Đã gửi link đặt lại mật khẩu vào email của bạn.", Toast.LENGTH_LONG).show();
                            } else {
                                Toast.makeText(activity_login.this, "Lỗi: Không thể gửi email khôi phục.", Toast.LENGTH_SHORT).show();
                            }
                        });
            } else {
                Toast.makeText(this, "Vui lòng không để trống email!", Toast.LENGTH_SHORT).show();
            }
        });
        builder.setNegativeButton("Hủy", (dialog, which) -> dialog.cancel());
        builder.show();
    }
}