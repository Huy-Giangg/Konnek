package com.example.whatsapp;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.PorterDuff;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.Toolbar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

import de.hdodenhof.circleimageview.CircleImageView;

public class GroupChatActivity extends AppCompatActivity {

    private String currentGroupName, currentGroupID, currentUserID, currentUserName;
    private TextView userName; // Tên nhóm trên Toolbar
    private Toolbar ChatToolBar;
    private DatabaseReference RootRef, UsersRef;

    private ImageButton SendMessageButton, SendFilesButton;
    private EditText MessageInputText;
    private FirebaseAuth mAuth;

    private final List<Messages> messagesList = new ArrayList<>();
    private LinearLayoutManager linearLayoutManager;
    private MessageAdapter messageAdapter;
    private RecyclerView userMessagesList;

    private String saveCurrentTime, getSaveCurrentTime;
    private String checker = "";
    private Uri fileUri;
    private ProgressDialog loadingBar;

    private ChildEventListener groupChatEventListener;
    private DatabaseReference groupNameRef;

    private final OkHttpClient client = new OkHttpClient();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat); // Tái sử dụng layout chat cá nhân

        getUserInfo();

        // 1. Lấy dữ liệu từ Intent
        // Lưu ý: Key "groupName" phải khớp với bên GroupsFragment gửi sang
        // Nếu bên kia gửi "groupID" thì bạn đổi key tương ứng
        if (getIntent().getExtras() != null) {
            currentGroupName = getIntent().getExtras().getString("groupName");
            // Trong mô hình đơn giản, Group Name có thể dùng làm Group ID
            // Nếu bạn có ID riêng thì lấy thêm: currentGroupID = getIntent().getExtras().getString("groupID");
            currentGroupID = currentGroupName;
        }

        mAuth = FirebaseAuth.getInstance();
        currentUserID = mAuth.getCurrentUser().getUid();
        RootRef = FirebaseDatabase.getInstance().getReference();

        // Tham chiếu đến node tin nhắn của nhóm này (Tách biệt với Messages cá nhân)
        groupNameRef = RootRef.child("GroupMessages").child(currentGroupID);

        // 2. Khởi tạo giao diện
        InitializeFields();

        // 3. Hiển thị tên nhóm
        userName.setText(currentGroupName);

        // 4. Logic nút gửi tin nhắn Text
        SendMessageButton.setOnClickListener(v -> SendMessage());

        // 5. Logic nút gửi File/Ảnh
        SendFilesButton.setOnClickListener(v -> {
            CharSequence options[] = new CharSequence[]{
                    "Images",
                    "PDF Files",
                    "MS Word Files"
            };

            AlertDialog.Builder builder = new AlertDialog.Builder(GroupChatActivity.this);
            builder.setTitle("Select the File");

            builder.setItems(options, (dialog, i) -> {
                if (i == 0) {
                    checker = "image";
                    selectFile("image/*");
                }
                if (i == 1) {
                    checker = "pdf";
                    selectFile("application/pdf");
                }
                if (i == 2) {
                    checker = "docx";
                    selectFile("application/msword");
                }
            });
            builder.show();
        });
    }

    private void InitializeFields() {
        ChatToolBar = findViewById(R.id.chat_toolbar);
        setSupportActionBar(ChatToolBar);

        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            // 2. Tắt hết các thành phần mặc định
            actionBar.setDisplayShowTitleEnabled(false); // Tắt Title
            actionBar.setDisplayUseLogoEnabled(false);   // Tắt Logo
            actionBar.setDisplayShowHomeEnabled(false);  // Tắt Icon Home

            // 3. Bật nút Back và Custom View
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setDisplayShowCustomEnabled(true);

            // 4. Nạp Layout Custom với tham số MATCH_PARENT (Quan trọng)
            LayoutInflater layoutInflater = (LayoutInflater) this.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            View actionBarView = layoutInflater.inflate(R.layout.custom_chat_bar, null);

            // Dòng này ép giao diện custom bung ra lấp đầy khoảng trống của Title cũ
            ActionBar.LayoutParams layoutParams = new ActionBar.LayoutParams(
                    ActionBar.LayoutParams.MATCH_PARENT,
                    ActionBar.LayoutParams.MATCH_PARENT);

            actionBar.setCustomView(actionBarView, layoutParams);

            // 5. Xóa khoảng trắng bên trái (giữa nút Back và Avatar)
            ChatToolBar.setContentInsetsAbsolute(0, 0);
            ChatToolBar.setContentInsetsRelative(0, 0);
        }

        // Tùy chỉnh Toolbar cho Group Chat
        CircleImageView usersImage = findViewById(R.id.custom_profile_image);
        //usersImage.setImageResource(R.drawable.groups); // Đổi icon mặc định thành icon nhóm
        usersImage.setVisibility(View.GONE);

        TextView userLastSeen = findViewById(R.id.custom_user_last_seen);
        userLastSeen.setVisibility(View.GONE); // Ẩn trạng thái Last Seen

        userName = findViewById(R.id.custom_profile_name);

        SendMessageButton = findViewById(R.id.send_message_btn);
        SendFilesButton = findViewById(R.id.send_files_btn);
        MessageInputText = findViewById(R.id.input_message);

        // QUAN TRỌNG: Truyền 'true' vào Adapter để kích hoạt chế độ Group Chat (hiện tên người gửi)
        messageAdapter = new MessageAdapter(messagesList, true);

        userMessagesList = findViewById(R.id.private_messages_list_of_users);
        linearLayoutManager = new LinearLayoutManager(this);
        userMessagesList.setLayoutManager(linearLayoutManager);
        userMessagesList.setAdapter(messageAdapter);

        loadingBar = new ProgressDialog(this);

        updateTime(); // Cập nhật thời gian ban đầu

        if (ChatToolBar.getNavigationIcon() != null) {
            ChatToolBar.getNavigationIcon().setColorFilter(getResources().getColor(android.R.color.white), PorterDuff.Mode.SRC_ATOP);
        }
    }

    // --- HIỂN THỊ MENU GỌI ĐIỆN ---
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.chat_menu, menu); // Đảm bảo bạn đã tạo file res/menu/chat_menu.xml
        return true;
    }

    // Hàm cập nhật thời gian thực
    private void updateTime() {
        Calendar calendar = Calendar.getInstance();
        SimpleDateFormat currentDate = new SimpleDateFormat("MMM dd, yyyy");
        saveCurrentTime = currentDate.format(calendar.getTime()); // Lưu Ngày

        SimpleDateFormat currentTime = new SimpleDateFormat("hh:mm a");
        getSaveCurrentTime = currentTime.format(calendar.getTime()); // Lưu Giờ
    }

    private void selectFile(String type) {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);

        if (type.equals("application/msword")) {
            intent.setType("*/*");
            String[] mimetypes = {
                    "application/msword",
                    "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
            };
            intent.putExtra(Intent.EXTRA_MIME_TYPES, mimetypes);
        } else {
            intent.setType(type);
        }

        startActivityForResult(Intent.createChooser(intent, "Chọn file"), 438);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == 438 && resultCode == RESULT_OK && data != null && data.getData() != null) {
            fileUri = data.getData();

            loadingBar.setTitle("Sending File");
            loadingBar.setMessage("Please wait...");
            loadingBar.setCanceledOnTouchOutside(false);
            loadingBar.show();

            if (checker.equals("image")) {
                uploadImageToCloudinary(fileUri);
            } else if (checker.equals("pdf") || checker.equals("docx")) {
                uploadFileToCloudinary(fileUri, checker);
            } else {
                loadingBar.dismiss();
            }
        }
    }

    // --- UPLOAD LOGIC ---

    private void uploadImageToCloudinary(Uri imageUri) {
        try {
            InputStream inputStream = getContentResolver().openInputStream(imageUri);
            ByteArrayOutputStream byteBuffer = new ByteArrayOutputStream();
            byte[] buffer = new byte[1024];
            int len;
            while ((len = inputStream.read(buffer)) != -1) {
                byteBuffer.write(buffer, 0, len);
            }
            byte[] imageBytes = byteBuffer.toByteArray();
            inputStream.close();

            String CLOUD_NAME = "dxnblcmbg";
            String UPLOAD_PRESET = "WhatsApp";

            RequestBody requestBody = new MultipartBody.Builder()
                    .setType(MultipartBody.FORM)
                    .addFormDataPart("file", "chat_image.jpg",
                            RequestBody.create(MediaType.parse("image/*"), imageBytes))
                    .addFormDataPart("upload_preset", UPLOAD_PRESET)
                    .build();

            Request request = new Request.Builder()
                    .url("https://api.cloudinary.com/v1_1/" + CLOUD_NAME + "/image/upload")
                    .post(requestBody)
                    .build();

            client.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(@NonNull Call call, @NonNull IOException e) {
                    runOnUiThread(() -> {
                        loadingBar.dismiss();
                        Toast.makeText(GroupChatActivity.this, "Upload failed", Toast.LENGTH_SHORT).show();
                    });
                }

                @Override
                public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                    if (response.isSuccessful()) {
                        try {
                            String responseData = response.body().string();
                            JSONObject json = new JSONObject(responseData);
                            String imageUrl = json.getString("secure_url");
                            runOnUiThread(() -> sendImageMessage(imageUrl));
                        } catch (Exception e) {
                            runOnUiThread(() -> loadingBar.dismiss());
                        }
                    } else {
                        runOnUiThread(() -> loadingBar.dismiss());
                    }
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
            loadingBar.dismiss();
        }
    }

    private void uploadFileToCloudinary(Uri fileUri, String fileType) {
        try {
            InputStream inputStream = getContentResolver().openInputStream(fileUri);
            ByteArrayOutputStream byteBuffer = new ByteArrayOutputStream();
            byte[] buffer = new byte[1024];
            int len;
            while ((len = inputStream.read(buffer)) != -1) {
                byteBuffer.write(buffer, 0, len);
            }
            byte[] fileBytes = byteBuffer.toByteArray();
            inputStream.close();

            String CLOUD_NAME = "dxnblcmbg";
            String UPLOAD_PRESET = "WhatsApp";

            RequestBody requestBody = new MultipartBody.Builder()
                    .setType(MultipartBody.FORM)
                    .addFormDataPart("file", "chat_file." + fileType,
                            RequestBody.create(MediaType.parse("*/*"), fileBytes))
                    .addFormDataPart("upload_preset", UPLOAD_PRESET)
                    .build();

            Request request = new Request.Builder()
                    .url("https://api.cloudinary.com/v1_1/" + CLOUD_NAME + "/raw/upload")
                    .post(requestBody)
                    .build();

            client.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(@NonNull Call call, @NonNull IOException e) {
                    runOnUiThread(() -> loadingBar.dismiss());
                }

                @Override
                public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                    if (response.isSuccessful()) {
                        try {
                            String responseData = response.body().string();
                            JSONObject json = new JSONObject(responseData);
                            String fileUrl = json.getString("secure_url");
                            runOnUiThread(() -> sendFileMessage(fileUrl, fileType));
                        } catch (Exception e) {
                            runOnUiThread(() -> loadingBar.dismiss());
                        }
                    } else {
                        runOnUiThread(() -> loadingBar.dismiss());
                    }
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
            loadingBar.dismiss();
        }
    }

    // --- GỬI TIN NHẮN (LOGIC GROUP) ---

    private void sendImageMessage(String imageUrl) {
        updateTime(); // Cập nhật thời gian

        // Tạo key tin nhắn
        DatabaseReference groupMessageKeyRef = RootRef.child("GroupMessages").child(currentGroupID).push();
        String messagePushID = groupMessageKeyRef.getKey();

        // --- SỬA ĐỔI: Lấy thông tin user trước khi lưu ---
        RootRef.child("Users").child(currentUserID).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                String myName = "Thành viên"; // Tên mặc định
                if (snapshot.exists() && snapshot.hasChild("name")) {
                    myName = snapshot.child("name").getValue().toString();
                }

                // Đóng gói dữ liệu
                Map<String, Object> messageTextBody = new HashMap<>();
                messageTextBody.put("message", imageUrl);
                messageTextBody.put("name", myName); // <--- LƯU TÊN Ở ĐÂY
                messageTextBody.put("type", "image");
                messageTextBody.put("from", currentUserID);
                messageTextBody.put("to", currentGroupID);
                messageTextBody.put("messageID", messagePushID);
                messageTextBody.put("time", getSaveCurrentTime);
                messageTextBody.put("date", saveCurrentTime);

                // Cập nhật lên Firebase
                groupNameRef.child(messagePushID).updateChildren(messageTextBody)
                        .addOnCompleteListener(task -> {
                            loadingBar.dismiss();
                            if (task.isSuccessful()) {
                                Toast.makeText(GroupChatActivity.this, "Gửi ảnh thành công", Toast.LENGTH_SHORT).show();
                            }
                        });
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                loadingBar.dismiss();
            }
        });
    }

    private void sendFileMessage(String fileUrl, String fileType) {
        updateTime(); // Cập nhật thời gian

        DatabaseReference groupMessageKeyRef = RootRef.child("GroupMessages").child(currentGroupID).push();
        String messagePushID = groupMessageKeyRef.getKey();

        // --- SỬA ĐỔI: Lấy thông tin user trước khi lưu ---
        RootRef.child("Users").child(currentUserID).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                String myName = "Thành viên";
                if (snapshot.exists() && snapshot.hasChild("name")) {
                    myName = snapshot.child("name").getValue().toString();
                }

                Map<String, Object> messageTextBody = new HashMap<>();
                messageTextBody.put("message", fileUrl);
                messageTextBody.put("name", myName); // <--- LƯU TÊN Ở ĐÂY
                messageTextBody.put("type", fileType);
                messageTextBody.put("from", currentUserID);
                messageTextBody.put("to", currentGroupID);
                messageTextBody.put("messageID", messagePushID);
                messageTextBody.put("time", getSaveCurrentTime);
                messageTextBody.put("date", saveCurrentTime);

                groupNameRef.child(messagePushID).updateChildren(messageTextBody)
                        .addOnCompleteListener(task -> {
                            loadingBar.dismiss();
                            if (task.isSuccessful()) {
                                Toast.makeText(GroupChatActivity.this, "Gửi file thành công", Toast.LENGTH_SHORT).show();
                            }
                        });
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                loadingBar.dismiss();
            }
        });
    }

    private void SendMessage() {
        String messageText = MessageInputText.getText().toString();
        if (TextUtils.isEmpty(messageText)) return;

        MessageInputText.setText(""); // Xóa ô nhập ngay cho mượt

        // 1. Lấy Key và Thời gian trước
        String messageKey = groupNameRef.push().getKey();
        updateTime();

        // 2. TRUY VẤN LẤY TÊN TRƯỚC, RỒI MỚI GỬI TIN
        DatabaseReference userRef = RootRef.child("Users").child(currentUserID);
        userRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                String myName = "Thành viên"; // Tên mặc định nếu lỗi

                if (snapshot.exists() && snapshot.hasChild("name")) {
                    myName = snapshot.child("name").getValue().toString();
                }

                // 3. Đóng gói dữ liệu
                HashMap<String, Object> messageInfoMap = new HashMap<>();
                messageInfoMap.put("name", myName); // <-- Đã có tên chuẩn
                messageInfoMap.put("message", messageText);
                messageInfoMap.put("date", saveCurrentTime);
                messageInfoMap.put("time", getSaveCurrentTime);
                messageInfoMap.put("from", currentUserID);
                messageInfoMap.put("to", currentGroupID);
                messageInfoMap.put("type", "text");
                messageInfoMap.put("messageID", messageKey);

                // 4. Đẩy lên Firebase
                groupNameRef.child(messageKey).updateChildren(messageInfoMap);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) { }
        });
    }

    // --- LẮNG NGHE DATA ---

    @Override
    protected void onStart() {
        super.onStart();
        messagesList.clear();
        messageAdapter.notifyDataSetChanged();

        groupChatEventListener = new ChildEventListener() {
            @Override
            public void onChildAdded(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {
                if (snapshot.exists()) {
                    Messages messages = snapshot.getValue(Messages.class);
                    messagesList.add(messages);
                    messageAdapter.notifyItemInserted(messagesList.size() - 1);
                    userMessagesList.smoothScrollToPosition(messagesList.size());
                }
            }

            @Override
            public void onChildChanged(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {
                if (snapshot.exists()) {
                    // 1. Lấy dữ liệu tin nhắn mới nhất từ Firebase (Đã bị thay đổi/thu hồi)
                    Messages changedMessage = snapshot.getValue(Messages.class);

                    if (changedMessage != null && changedMessage.getMessageID() != null) {

                        // 2. Tìm vị trí (index) của tin nhắn này trong danh sách đang hiển thị
                        int index = -1;
                        for (int i = 0; i < messagesList.size(); i++) {
                            Messages existingMessage = messagesList.get(i);

                            // So sánh bằng MessageID để tìm đúng tin nhắn cần sửa
                            if (existingMessage.getMessageID() != null &&
                                    existingMessage.getMessageID().equals(changedMessage.getMessageID())) {
                                index = i;
                                break;
                            }
                        }

                        // 3. Nếu tìm thấy -> Cập nhật danh sách và giao diện
                        if (index != -1) {
                            // Thay thế object cũ bằng object mới (có nội dung "Đã thu hồi")
                            messagesList.set(index, changedMessage);

                            // Báo cho Adapter biết chỉ dòng này thay đổi để vẽ lại (Hiệu năng cao)
                            messageAdapter.notifyItemChanged(index);
                        }
                    }
                }
            }
            @Override public void onChildRemoved(@NonNull DataSnapshot snapshot) {}
            @Override public void onChildMoved(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {}
            @Override public void onCancelled(@NonNull DatabaseError error) {}
        };

        groupNameRef.addChildEventListener(groupChatEventListener);
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (groupNameRef != null && groupChatEventListener != null) {
            groupNameRef.removeEventListener(groupChatEventListener);
        }
    }

    // Thêm hàm này vào trong GroupChatActivity
    private void getUserInfo() {
        // Thêm check null một lần nữa cho chắc
        if (currentUserID == null) return;

        UsersRef.child(currentUserID).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists() && snapshot.hasChild("name")) {
                    currentUserName = snapshot.child("name").getValue().toString();
                }
            }
            @Override public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    // Thêm vào cuối class GroupChatActivity

    public void deleteMessageForEveryone(String messageId) {
        // Trỏ tới đúng tin nhắn cần xóa trong GroupMessages
        DatabaseReference messageRef = RootRef.child("GroupMessages").child(currentGroupID).child(messageId);

        // Cập nhật trạng thái
        Map<String, Object> updateMap = new HashMap<>();
        updateMap.put("message", "🚫 Tin nhắn đã bị thu hồi.");
        updateMap.put("type", "deleted"); // Đổi loại tin thành deleted để Adapter hiển thị khác đi

        messageRef.updateChildren(updateMap).addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                Toast.makeText(GroupChatActivity.this, "Đã thu hồi tin nhắn.", Toast.LENGTH_SHORT).show();

                // Cập nhật giao diện ngay lập tức (Optional - vì onChildChanged cũng sẽ làm việc này)
                // Nhưng làm ở đây cho cảm giác mượt hơn
                for (int i = 0; i < messagesList.size(); i++) {
                    if (messagesList.get(i).getMessageID().equals(messageId)) {
                        messagesList.get(i).setMessage("🚫 Tin nhắn đã bị thu hồi.");
                        messagesList.get(i).setType("deleted");
                        messageAdapter.notifyItemChanged(i);
                        break;
                    }
                }
            } else {
                Toast.makeText(GroupChatActivity.this, "Lỗi khi thu hồi!", Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();

        if (id == android.R.id.home) {
            finish(); // Đóng Activity
            return true;
        }

        // 2. Xử lý nút Gọi Thoại
        if (id == R.id.menu_voice_call) {
            // Nếu bạn dùng ZegoUIKit (như hướng dẫn trước), hãy kích hoạt nút ẩn
            // voiceCallBtn.performClick();

            Toast.makeText(this, "Đang gọi thoại...", Toast.LENGTH_SHORT).show();
            return true;
        }

        // 3. Xử lý nút Gọi Video
        if (id == R.id.menu_video_call) {
            // Nếu bạn dùng ZegoUIKit
            // videoCallBtn.performClick();

            Toast.makeText(this, "Đang gọi video...", Toast.LENGTH_SHORT).show();
            return true;
        }

        // 4. Xử lý nút More (3 chấm) - Thường sẽ hiện Dialog chọn
        if (id == R.id.menu_setting) {
            Toast.makeText(this, "Đang bảo trì...", Toast.LENGTH_SHORT).show();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
}