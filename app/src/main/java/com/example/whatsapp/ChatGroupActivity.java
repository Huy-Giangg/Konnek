package com.example.whatsapp;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

public class ChatGroupActivity extends AppCompatActivity {

    private Toolbar mToolbar;
    private ImageButton SendMessageButton;
    private EditText userMessageInput;

    // Sử dụng RecyclerView để hiển thị tin nhắn (giống chat cá nhân)
    private RecyclerView userMessagesList;
    private final List<Messages> messagesList = new ArrayList<>();
    private MessageAdapter messageAdapter;

    private String currentGroupName, currentUserID, currentUserName;
    private FirebaseAuth mAuth;
    private DatabaseReference UsersRef, GroupNameRef, GroupMessageKeyRef;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // TÁI SỬ DỤNG GIAO DIỆN CHAT CÁ NHÂN
        setContentView(R.layout.activity_chat);

        // 1. Nhận tên nhóm được gửi từ GroupsFragment
        currentGroupName = getIntent().getExtras().get("groupName").toString();

        // 2. Khởi tạo Firebase
        mAuth = FirebaseAuth.getInstance();
        currentUserID = mAuth.getCurrentUser().getUid();
        UsersRef = FirebaseDatabase.getInstance().getReference().child("Users");

        // Trỏ tới Node chứa tin nhắn của nhóm cụ thể này
        GroupNameRef = FirebaseDatabase.getInstance().getReference().child("Groups").child(currentGroupName).child("Messages");

        InitializeFields();

        GetUserInfo();

        SendMessageButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                SaveMessageInfoToDatabase();
                userMessageInput.setText(""); // Xóa ô nhập sau khi gửi
            }
        });
    }

    @Override
    protected void onStart() {
        super.onStart();

        // 3. Lắng nghe tin nhắn mới từ Firebase về
        GroupNameRef.addChildEventListener(new ChildEventListener() {
            @Override
            public void onChildAdded(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {
                if (snapshot.exists()) {
                    // Convert dữ liệu JSON thành Object Messages
                    Messages messages = snapshot.getValue(Messages.class);

                    messagesList.add(messages);

                    // Cập nhật Adapter
                    messageAdapter.notifyItemInserted(messagesList.size() - 1);
                    // Cuộn xuống dòng tin nhắn mới nhất
                    userMessagesList.smoothScrollToPosition(messagesList.size() - 1);
                }
            }

            @Override public void onChildChanged(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {}
            @Override public void onChildRemoved(@NonNull DataSnapshot snapshot) {}
            @Override public void onChildMoved(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {}
            @Override public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void InitializeFields() {
        mToolbar = findViewById(R.id.chat_toolbar);
        setSupportActionBar(mToolbar);
        getSupportActionBar().setTitle(currentGroupName); // Set Tiêu đề là Tên nhóm
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        // Sự kiện nút Back
        mToolbar.setNavigationOnClickListener(v -> onBackPressed());

        SendMessageButton = findViewById(R.id.send_message_btn);
        userMessageInput = findViewById(R.id.input_message);

        // Cấu hình RecyclerView
        userMessagesList = findViewById(R.id.private_messages_list_of_users);

        // QUAN TRỌNG: Tham số 'true' báo cho Adapter biết đây là Group Chat để hiển thị tên người gửi
        //messageAdapter = new MessageAdapter(messagesList, true);

        userMessagesList.setLayoutManager(new LinearLayoutManager(this));
        userMessagesList.setAdapter(messageAdapter);
    }

    private void GetUserInfo() {
        // Lấy tên của chính mình để lưu vào tin nhắn (cho người khác biết ai gửi)
        UsersRef.child(currentUserID).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    currentUserName = snapshot.child("name").getValue().toString();
                }
            }
            @Override public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void SaveMessageInfoToDatabase() {
        String message = userMessageInput.getText().toString();
        // Tạo khóa ngẫu nhiên cho tin nhắn
        String messageKey = GroupNameRef.push().getKey();

        if (TextUtils.isEmpty(message)) {
            Toast.makeText(this, "Please write message...", Toast.LENGTH_SHORT).show();
        } else {
            // Lấy ngày giờ hiện tại
            Calendar calForDate = Calendar.getInstance();
            SimpleDateFormat currentDateFormat = new SimpleDateFormat("MMM dd, yyyy");
            String currentDate = currentDateFormat.format(calForDate.getTime());

            Calendar calForTime = Calendar.getInstance();
            SimpleDateFormat currentTimeFormat = new SimpleDateFormat("hh:mm a");
            String currentTime = currentTimeFormat.format(calForTime.getTime());

            // Tạo cấu trúc dữ liệu tin nhắn
            HashMap<String, Object> groupMessageKey = new HashMap<>();
            GroupNameRef.updateChildren(groupMessageKey);

            GroupMessageKeyRef = GroupNameRef.child(messageKey);

            HashMap<String, Object> messageInfoMap = new HashMap<>();
            messageInfoMap.put("name", currentUserName); // Tên người gửi (quan trọng cho nhóm)
            messageInfoMap.put("message", message);
            messageInfoMap.put("date", currentDate);
            messageInfoMap.put("time", currentTime);
            messageInfoMap.put("from", currentUserID); // ID người gửi (để xác định trái/phải)
            messageInfoMap.put("type", "text");

            GroupMessageKeyRef.updateChildren(messageInfoMap);
        }
    }
}