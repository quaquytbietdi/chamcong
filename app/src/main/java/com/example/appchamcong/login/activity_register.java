package com.example.appchamcong.login;

import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.appchamcong.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.HashMap;
import java.util.Map;

public class activity_register extends AppCompatActivity {
    private EditText edtRegFullname, edtRegEmail, edtRegPassword;
    private Button btnRegister;
    private TextView tvBackToLogin;
    private FirebaseAuth mAuth;
    private DatabaseReference mDatabase;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        mAuth = FirebaseAuth.getInstance();
        mDatabase = FirebaseDatabase.getInstance().getReference();

        // Ánh xạ đúng ID từ file XML
        edtRegFullname = findViewById(R.id.edtRegFullname);
        edtRegEmail = findViewById(R.id.edtRegEmail);
        edtRegPassword = findViewById(R.id.edtRegPassword);
        btnRegister = findViewById(R.id.btnRegister);
        tvBackToLogin = findViewById(R.id.tvBackToLogin);

        btnRegister.setOnClickListener(v -> handleRegister());
        tvBackToLogin.setOnClickListener(v -> finish());
    }

    private void handleRegister() {
        String fullname = edtRegFullname != null ? edtRegFullname.getText().toString().trim() : "";
        String email = edtRegEmail.getText().toString().trim();
        String password = edtRegPassword.getText().toString().trim();

        if (TextUtils.isEmpty(fullname) || TextUtils.isEmpty(email) || TextUtils.isEmpty(password)) {
            Toast.makeText(this, "Vui lòng nhập đầy đủ thông tin (Họ tên, Email, Mật khẩu)", Toast.LENGTH_LONG).show();
            return;
        }

        if (password.length() < 6) {
            Toast.makeText(this, "Mật khẩu phải có ít nhất 6 ký tự", Toast.LENGTH_SHORT).show();
            return;
        }

        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        String authUid = mAuth.getCurrentUser().getUid();

                        // Quét nhánh Users để tìm số thứ tự lớn nhất hiện tại, tránh bị ảnh hưởng bởi các chuỗi UID cũ
                        mDatabase.child("Users").addListenerForSingleValueEvent(new ValueEventListener() {
                            @Override
                            public void onDataChange(DataSnapshot snapshot) {
                                int maxId = 0;
                                for (DataSnapshot child : snapshot.getChildren()) {
                                    try {
                                        int idNum = Integer.parseInt(child.getKey());
                                        if (idNum > maxId) {
                                            maxId = idNum;
                                        }
                                    } catch (NumberFormatException e) {
                                        // Bỏ qua các khóa không phải là số (như các chuỗi UID cũ nếu có)
                                    }
                                }
                                int nextId = maxId + 1;
                                String newIdString = String.valueOf(nextId);

                                Map<String, Object> userObj = new HashMap<>();
                                userObj.put("id", newIdString);
                                userObj.put("fullname", fullname);
                                userObj.put("email", email);
                                userObj.put("role", "user");

                                // 1. Chỉ lưu thông tin vào nhánh Users/{id} (ví dụ: Users/3), TUYỆT ĐỐI KHÔNG LƯU theo Auth UID ở Users nữa
                                mDatabase.child("Users").child(newIdString).setValue(userObj)
                                        .addOnSuccessListener(aVoid -> {
                                            // 2. Lưu ánh xạ ngầm Auth UID sang ID số ở nhánh UserMapping riêng biệt để app đối soát khi đăng nhập
                                            mDatabase.child("UserMapping").child(authUid).setValue(newIdString)
                                                    .addOnSuccessListener(aMapSuccess -> {
                                                        Toast.makeText(activity_register.this, "Đăng ký thành công! Mã NV: " + newIdString, Toast.LENGTH_LONG).show();
                                                        mAuth.signOut();
                                                        finish();
                                                    });
                                        })
                                        .addOnFailureListener(e -> {
                                            Toast.makeText(activity_register.this, "Lỗi Realtime DB: " + e.getMessage(), Toast.LENGTH_LONG).show();
                                            Log.e("RegisterError", "Lỗi lưu Database: ", e);
                                        });
                            }

                            @Override
                            public void onCancelled(DatabaseError error) {
                                Toast.makeText(activity_register.this, "Lỗi hệ thống: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                            }
                        });

                    } else {
                        Toast.makeText(activity_register.this, "Đăng ký thất bại: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }
}