package com.example.appchamcong.login; // (Hoặc package phù hợp với project của bạn)

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;

import androidx.appcompat.app.AppCompatActivity;

import com.example.appchamcong.R;

public class IntroActivity extends AppCompatActivity {

    private static final long SPLASH_DELAY = 2500; // Thời gian hiển thị Intro (2.5 giây)

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_intro);

        // Dùng Handler để đếm ngược thời gian, sau đó chuyển sang màn hình Đăng nhập
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            Intent intent = new Intent(IntroActivity.this, activity_login.class);
            startActivity(intent);
            finish(); // Đóng IntroActivity để người dùng không bấm nút Back quay lại được
        }, SPLASH_DELAY);
    }
}