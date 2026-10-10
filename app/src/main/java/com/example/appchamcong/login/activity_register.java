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
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

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

        // Ánh xạ đúng ID từ file XML bạn vừa gửi
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
                        String uid = mAuth.getCurrentUser().getUid();

                        // Đẩy đầy đủ các trường dữ liệu lên Realtime Database
                        Map<String, Object> userObj = new HashMap<>();
                        userObj.put("id", uid);
                        userObj.put("fullname", fullname);
                        userObj.put("email", email);
                        userObj.put("role", "user");
                        userObj.put("avatar", "");

                        mDatabase.child("Users").child(uid).setValue(userObj)
                                .addOnSuccessListener(aVoid -> {
                                    Toast.makeText(activity_register.this, "Đăng ký thành công!", Toast.LENGTH_LONG).show();
                                    mAuth.signOut();
                                    finish();
                                })
                                .addOnFailureListener(e -> {
                                    Toast.makeText(activity_register.this, "Lỗi Realtime DB: " + e.getMessage(), Toast.LENGTH_LONG).show();
                                    Log.e("RegisterError", "Lỗi lưu Database: ", e);
                                });

                    } else {
                        Toast.makeText(activity_register.this, "Đăng ký thất bại: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }
}