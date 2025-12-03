package com.example.whatsapp;

import android.annotation.SuppressLint;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.view.menu.MenuBuilder;
import androidx.appcompat.widget.Toolbar;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.viewpager.widget.ViewPager;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
// import com.google.android.material.tabs.TabLayout; // Đã xóa thư viện này
import com.google.android.material.bottomnavigation.BottomNavigationView; // Thêm thư viện này
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.messaging.FirebaseMessaging;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.HashMap;

public class MainActivity extends AppCompatActivity {

    private Toolbar mToolBar;
    private ViewPager myViewPager;
    // private TabLayout myTabLayout; // Đã xóa
    private BottomNavigationView bottomNavigationView; // Thêm biến mới

    private TabsAccessorAdapter myTabsAccessorAdapter;
    private FirebaseAuth mAuth;
    private DatabaseReference RootRef;
    private String currentUserID;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        mAuth = FirebaseAuth.getInstance();
        RootRef = FirebaseDatabase.getInstance().getReference();

        mToolBar = (Toolbar) findViewById(R.id.main_page_toolbar);
        setSupportActionBar(mToolBar);
        getSupportActionBar().setTitle("KONNEK");

        myViewPager = (ViewPager) findViewById(R.id.main_tabs_pager);
        myTabsAccessorAdapter = new TabsAccessorAdapter(getSupportFragmentManager());
        myViewPager.setAdapter(myTabsAccessorAdapter);

        // --- BẮT ĐẦU ĐOẠN CODE SỬA ĐỔI CHO BOTTOM NAV ---

        // 1. Ánh xạ Bottom Navigation View
        bottomNavigationView = findViewById(R.id.bottom_navigation);

        // 2. Xử lý sự kiện CLICK vào Bottom Bar -> Chuyển ViewPager
        bottomNavigationView.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();

            if (itemId == R.id.nav_chat) {
                myViewPager.setCurrentItem(0);
                return true;
            } else if (itemId == R.id.nav_groups) {
                myViewPager.setCurrentItem(1);
                return true;
            } else if (itemId == R.id.nav_profile) {
                myViewPager.setCurrentItem(2);
                return true;
            } else if (itemId == R.id.nav_more) {
                // Lưu ý: Đảm bảo TabsAccessorAdapter của bạn có đủ 4 Fragment
                // Nếu Adapter chỉ có 3 tab cũ, dòng này sẽ gây lỗi.
                // Nếu chưa có tab thứ 4, hãy xóa đoạn else-if này hoặc cập nhật Adapter.
                myViewPager.setCurrentItem(3);
                return true;
            }
            return false;
        });

        // 3. Xử lý sự kiện VUỐT ViewPager -> Cập nhật icon sáng trên Bottom Bar
        myViewPager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) { }

            @Override
            public void onPageSelected(int position) {
                switch (position) {
                    case 0:
                        bottomNavigationView.getMenu().findItem(R.id.nav_chat).setChecked(true);
                        break;
                    case 1:
                        bottomNavigationView.getMenu().findItem(R.id.nav_groups).setChecked(true);
                        break;
                    case 2:
                        bottomNavigationView.getMenu().findItem(R.id.nav_profile).setChecked(true);
                        break;
                    case 3:
                        bottomNavigationView.getMenu().findItem(R.id.nav_more).setChecked(true);
                        break;
                }
            }

            @Override
            public void onPageScrollStateChanged(int state) { }
        });

        // Kiểm tra xem có yêu cầu chuyển tab đặc biệt nào không
        int tabIndex = getIntent().getIntExtra("target_index", 0); // Mặc định là 0 (Chats)

        // Chuyển ViewPager sang tab được yêu cầu
        myViewPager.setCurrentItem(tabIndex);

        // --- KẾT THÚC ĐOẠN CODE SỬA ĐỔI ---
    }

    @Override
    protected void onStart() {
        super.onStart();
        updateDeviceToken();
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if(currentUser == null){
            SendUserToLoginActivity();
        }
        else{
            VerifyUserExistance();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    @Override
    protected void onStop() {
        super.onStop();
    }

    private void VerifyUserExistance() {
        String uid = mAuth.getCurrentUser().getUid();

        RootRef.child("Users").child(uid)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        if (!snapshot.hasChild("name")) {
                            SendUserToSettingsActivity();
                        } else {
                            Toast.makeText(MainActivity.this, "Welcome!", Toast.LENGTH_SHORT).show();
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {}
                });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        getMenuInflater().inflate(R.menu.options_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        super.onOptionsItemSelected(item);
        if (item.getItemId() == R.id.main_logout_option) {
            SignOutUser();
        }
        if (item.getItemId() == R.id.main_settings_option) {
            SendUserToSettingsActivity();
        }
        if (item.getItemId() == R.id.main_find_friends_option) {
            SendUserToFindFriendsActivity();
        }
        if (item.getItemId() == R.id.main_create_group_opttion) {
            Intent intent = new Intent(MainActivity.this, CreateGroupActivity.class);
            startActivity(intent);
        }
        return true;
    }

    private void RequestNewGroup() {
        AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this, R.style.AlerDialog);
        builder.setTitle("Enter Group Name: ");
        final EditText groupNameField = new EditText(MainActivity.this);
        groupNameField.setHint("e.g. Coding Cafe");
        builder.setView(groupNameField);

        builder.setPositiveButton("Create", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                String groupName = groupNameField.getText().toString();

                if(TextUtils.isEmpty(groupName.trim())){
                    Toast.makeText(MainActivity.this, "Please wirte Group Name...", Toast.LENGTH_SHORT).show();
                }else{
                    CreateNewGroup(groupName);
                }
            }
        });

        builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                dialogInterface.cancel();
            }
        });

        builder.show();
    }

    private void CreateNewGroup(String groupName) {
        RootRef.child("Groups").child(groupName).setValue("")
                .addOnCompleteListener(new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        if(task.isSuccessful()) {
                            Toast.makeText(MainActivity.this, groupName + " group is created successfully...", Toast.LENGTH_SHORT).show();
                        }else{
                            String error = task.getException() != null ? task.getException().getMessage() : "Unknown error.";
                            Toast.makeText(MainActivity.this, "Failed to create group: " + error, Toast.LENGTH_LONG).show();
                        }
                    }
                });
    }

    private void SendUserToLoginActivity() {
        Intent loginIntent = new Intent(MainActivity.this, LoginActivity.class);
        loginIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(loginIntent);
        finish();
    }

    private void SendUserToSettingsActivity() {
        Intent settingsIntent = new Intent(MainActivity.this, SettingsActivity.class);
        settingsIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(settingsIntent);
        finish();
    }

    private void SendUserToFindFriendsActivity() {
        Intent findFriendsIntent = new Intent(MainActivity.this, FindFriendsActivity.class);
        startActivity(findFriendsIntent);
    }

    private void SignOutUser() {
        FirebaseAuth mAuth = FirebaseAuth.getInstance();
        FirebaseUser currentUser = mAuth.getCurrentUser();

        if (currentUser != null) {
            DatabaseReference RootRef = FirebaseDatabase.getInstance().getReference();
            String currentUserID = currentUser.getUid();

            // 1. Chuẩn bị dữ liệu Offline
            Calendar calendar = Calendar.getInstance();
            SimpleDateFormat currentDate = new SimpleDateFormat("MMM dd, yyyy");
            String saveCurrentDate = currentDate.format(calendar.getTime());

            SimpleDateFormat currentTime = new SimpleDateFormat("hh:mm a");
            String saveCurrentTime = currentTime.format(calendar.getTime());

            HashMap<String, Object> offlineStateMap = new HashMap<>();
            offlineStateMap.put("time", saveCurrentTime);
            offlineStateMap.put("date", saveCurrentDate);
            offlineStateMap.put("state", "offline");

            // 2. Cập nhật lên Firebase TRƯỚC
            RootRef.child("Users").child(currentUserID).child("userState")
                    .updateChildren(offlineStateMap)
                    .addOnCompleteListener(new OnCompleteListener<Void>() {
                        @Override
                        public void onComplete(@NonNull Task<Void> task) {
                            // 3. CHỈ ĐĂNG XUẤT KHI ĐÃ CẬP NHẬT XONG (HOẶC KHI CÓ LỖI)

                            // Hủy chế độ tự động offline khi mất mạng (vì ta đã set offline thủ công rồi)
                            RootRef.child("Users").child(currentUserID).child("userState")
                                    .child("state").onDisconnect().cancel();

                            // Thực hiện đăng xuất
                            mAuth.signOut();

                            // Chuyển màn hình
                            SendUserToLoginActivity();
                        }
                    });
        } else {
            // Trường hợp user null (hiếm gặp), cứ chuyển về Login
            SendUserToLoginActivity();
        }
    }

    private void updateDeviceToken() {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            FirebaseMessaging.getInstance().getToken()
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            String deviceToken = task.getResult();
                            String currentUserID = currentUser.getUid();

                            // Lưu Token vào node Users
                            FirebaseDatabase.getInstance().getReference()
                                    .child("Users").child(currentUserID).child("device_token")
                                    .setValue(deviceToken);
                        }
                    });
        }
    }
}