package com.example.whatsapp;

import android.app.Activity;
import android.app.Application;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.HashMap;

public class MyApplication extends Application implements Application.ActivityLifecycleCallbacks {

    private int activityReferences = 0;
    private boolean isActivityChangingConfigurations = false;
    private FirebaseAuth mAuth;
    private FirebaseAuth.AuthStateListener authStateListener;

    @Override
    public void onCreate() {
        super.onCreate();

        // 1. Đăng ký lắng nghe vòng đời Activity (Để biết App Đóng/Mở)
        registerActivityLifecycleCallbacks(this);

        // 2. Đăng ký lắng nghe trạng thái Đăng Nhập/Đăng Xuất
        mAuth = FirebaseAuth.getInstance();
        setupAuthStateListener();
    }

    // --- LOGIC LẮNG NGHE ĐĂNG NHẬP / ĐĂNG XUẤT ---
    private void setupAuthStateListener() {
        authStateListener = new FirebaseAuth.AuthStateListener() {
            @Override
            public void onAuthStateChanged(@NonNull FirebaseAuth firebaseAuth) {
                FirebaseUser user = firebaseAuth.getCurrentUser();
                if (user != null) {
                    // A. NGƯỜI DÙNG VỪA ĐĂNG NHẬP (Hoặc mở app khi đã đăng nhập)
                    // Nếu App đang mở (activityReferences > 0) -> Set Online ngay
                    if (activityReferences > 0) {
                        updateUserStatus("online");
                    }
                } else {
                    // B. NGƯỜI DÙNG VỪA ĐĂNG XUẤT -> Không làm gì cả (Hoặc log ra)
                    // Vì khi đăng xuất, biến currentUser sẽ null, hàm updateUserStatus sẽ tự chặn
                }
            }
        };
        mAuth.addAuthStateListener(authStateListener);
    }

    // --- LOGIC VÒNG ĐỜI ỨNG DỤNG (APP OPEN/CLOSE) ---

    @Override
    public void onActivityStarted(@NonNull Activity activity) {
        if (++activityReferences == 1 && !isActivityChangingConfigurations) {
            // App vừa được mở lên hoặc quay lại từ nền
            updateUserStatus("online");
        }
    }

    @Override
    public void onActivityStopped(@NonNull Activity activity) {
        isActivityChangingConfigurations = activity.isChangingConfigurations();
        if (--activityReferences == 0 && !isActivityChangingConfigurations) {
            // App bị ẩn xuống hoặc tắt hẳn
            updateUserStatus("offline");
        }
    }

    // --- HÀM CẬP NHẬT TRẠNG THÁI ---
    private void updateUserStatus(String state) {
        // Luôn lấy user mới nhất tại thời điểm gọi hàm
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();

        if (currentUser != null) {
            String currentUserID = currentUser.getUid();
            DatabaseReference rootRef = FirebaseDatabase.getInstance().getReference();

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

            // Đăng ký onDisconnect để chắc chắn offline khi mất mạng
            if(state.equals("online")){
                rootRef.child("Users").child(currentUserID).child("userState")
                        .child("state").onDisconnect().setValue("offline");
            }
        }
    }

    // --- CÁC HÀM KHÁC (BẮT BUỘC PHẢI CÓ DÙ ĐỂ TRỐNG) ---
    @Override public void onActivityCreated(@NonNull Activity activity, @Nullable Bundle savedInstanceState) {}
    @Override public void onActivityResumed(@NonNull Activity activity) {}
    @Override public void onActivityPaused(@NonNull Activity activity) {}
    @Override public void onActivitySaveInstanceState(@NonNull Activity activity, @NonNull Bundle outState) {}
    @Override public void onActivityDestroyed(@NonNull Activity activity) {}
}