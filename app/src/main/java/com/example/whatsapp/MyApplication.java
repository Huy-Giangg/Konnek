package com.example.whatsapp;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.DefaultLifecycleObserver;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.ProcessLifecycleOwner;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.HashMap;

public class MyApplication extends Application implements DefaultLifecycleObserver {

    @Override
    public void onCreate() {
        super.onCreate();
        // Đăng ký lắng nghe vòng đời toàn ứng dụng
        ProcessLifecycleOwner.get().getLifecycle().addObserver(this);
    }

    // 🟢 KHI APP HIỆN LÊN MÀN HÌNH (FOREGROUND) -> ONLINE
    @Override
    public void onStart(@NonNull LifecycleOwner owner) {
        updateUserStatus("online");
    }

    // 🔴 KHI APP BỊ ẨN HOẶC THOÁT (BACKGROUND) -> OFFLINE
    @Override
    public void onStop(@NonNull LifecycleOwner owner) {
        updateUserStatus("offline");
    }

    private void updateUserStatus(String state) {
        FirebaseAuth mAuth = FirebaseAuth.getInstance();
        FirebaseUser currentUser = mAuth.getCurrentUser();

        if (currentUser != null) {
            DatabaseReference rootRef = FirebaseDatabase.getInstance().getReference();
            String currentUserID = currentUser.getUid();

            String saveCurrentTime, saveCurrentDate;
            Calendar calendar = Calendar.getInstance();

            SimpleDateFormat currentDate = new SimpleDateFormat("MMM dd, yyyy");
            saveCurrentDate = currentDate.format(calendar.getTime());

            SimpleDateFormat currentTime = new SimpleDateFormat("hh:mm a");
            saveCurrentTime = currentTime.format(calendar.getTime());

            HashMap<String, Object> onlineStateMap = new HashMap<>();
            onlineStateMap.put("time", saveCurrentTime);
            onlineStateMap.put("date", saveCurrentDate);
            onlineStateMap.put("state", state);

            rootRef.child("Users").child(currentUserID).child("userState")
                    .updateChildren(onlineStateMap);

            // Quan trọng: Đặt chế độ tự động Offline nếu mất mạng đột ngột
            if (state.equals("online")) {
                rootRef.child("Users").child(currentUserID).child("userState").child("state")
                        .onDisconnect().setValue("offline");
            }
        }
    }
}