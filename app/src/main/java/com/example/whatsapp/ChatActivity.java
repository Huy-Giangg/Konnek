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
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.activity.EdgeToEdge;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.squareup.picasso.Picasso;

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

import de.hdodenhof.circleimageview.CircleImageView;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class ChatActivity extends AppCompatActivity {

    private String messageReceiverID, messageReceiverName, messageReceiverImage, messageSenderID;

    // Các biến cho Custom Toolbar
    private TextView customProfileName, customUserLastSeen;
    private CircleImageView customProfileImage;
    private View customOnlineStatus; // Chấm xanh
    private Toolbar ChatToolBar;

    private DatabaseReference RootRef;
    private ImageButton SendMessageButton, SendFilesButton;
    private EditText MessageInputText;
    private FirebaseAuth mAuth;

    private final List<Messages> messagesList = new ArrayList<>();
    private LinearLayoutManager linearLayoutManager;
    private MessageAdapter messageAdapter;
    private RecyclerView userMessagesList;

    private String saveCurrentTime, saveCurrentDate; // Biến lưu thời gian thực
    private String checker = "";
    private Uri fileUri;
    private ProgressDialog loadingBar;

    private ChildEventListener messageEventListener;
    private DatabaseReference messageQueryRef;

    private final OkHttpClient client = new OkHttpClient();

    private ValueEventListener seenListener;
    private DatabaseReference userMessageRef;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_chat);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        mAuth = FirebaseAuth.getInstance();
        messageSenderID = mAuth.getCurrentUser().getUid();
        RootRef = FirebaseDatabase.getInstance().getReference();

        messageReceiverID = getIntent().getStringExtra("visit_user_id");
        messageReceiverName = getIntent().getStringExtra("visit_user_name");
        messageReceiverImage = getIntent().getStringExtra("visit_image");

        InitializeFields();

        // Gán dữ liệu vào Custom Toolbar
        customProfileName.setText(messageReceiverName);
        Picasso.get().load(messageReceiverImage)
                .placeholder(R.drawable.profile_image)
                .into(customProfileImage);

        SendMessageButton.setOnClickListener(v -> SendMessage());

        SendFilesButton.setOnClickListener(v -> {
            CharSequence options[] = new CharSequence[]{
                    "Images",
                    "PDF Files",
                    "MS Word Files"
            };

            AlertDialog.Builder builder = new AlertDialog.Builder(ChatActivity.this);
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
                    selectFile("application/msword"); // Word
                }
            });
            builder.show();
        });

        DisplayLastSeen();
    }

    private void InitializeFields() {
        ChatToolBar = findViewById(R.id.chat_toolbar);

        // 1. Cấu hình Toolbar trước khi set làm ActionBar
        ChatToolBar.setTitle(""); // Xóa title ngay trên Toolbar
        ChatToolBar.setSubtitle("");
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

        // Ánh xạ các View trong Custom Layout
        customProfileImage = findViewById(R.id.custom_profile_image);
        customProfileName = findViewById(R.id.custom_profile_name);
        customUserLastSeen = findViewById(R.id.custom_user_last_seen);
        customOnlineStatus = findViewById(R.id.custom_online_status);

        SendMessageButton = findViewById(R.id.send_message_btn);
        SendFilesButton = findViewById(R.id.send_files_btn);
        MessageInputText = findViewById(R.id.input_message);

        messageAdapter = new MessageAdapter(messagesList, false);
        userMessagesList = findViewById(R.id.private_messages_list_of_users);

        linearLayoutManager = new LinearLayoutManager(this);
        userMessagesList.setLayoutManager(linearLayoutManager);
        userMessagesList.setAdapter(messageAdapter);

        loadingBar = new ProgressDialog(this);
        messageAdapter.setReceiverAvatarUrl(messageReceiverImage);

        if (ChatToolBar.getNavigationIcon() != null) {
            ChatToolBar.getNavigationIcon().setColorFilter(getResources().getColor(android.R.color.white), PorterDuff.Mode.SRC_ATOP);
        }
    }

    // --- HÀM CẬP NHẬT THỜI GIAN THỰC (FIX LỖI THỜI GIAN) ---
    private void updateTime() {
        Calendar calendar = Calendar.getInstance();

        SimpleDateFormat currentDate = new SimpleDateFormat("MMM dd, yyyy");
        saveCurrentDate = currentDate.format(calendar.getTime()); // Lưu Ngày

        SimpleDateFormat currentTime = new SimpleDateFormat("hh:mm a");
        saveCurrentTime = currentTime.format(calendar.getTime()); // Lưu Giờ
    }

    // --- HIỂN THỊ MENU GỌI ĐIỆN ---
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.chat_menu, menu); // Đảm bảo bạn đã tạo file res/menu/chat_menu.xml
        return true;
    }

    private void DisplayLastSeen() {
        RootRef.child("Users").child(messageReceiverID)
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        if (snapshot.child("userState").hasChild("state")) {
                            String date = snapshot.child("userState").child("date").getValue().toString();
                            String time = snapshot.child("userState").child("time").getValue().toString();
                            String state = snapshot.child("userState").child("state").getValue().toString();

                            if (state.equals("online")) {
                                customUserLastSeen.setText("Online");
                                customOnlineStatus.setVisibility(View.VISIBLE); // Hiện chấm xanh
                            } else if (state.equals("offline")) {
                                customUserLastSeen.setText("Last Seen: " + date + " " + time);
                                customOnlineStatus.setVisibility(View.GONE); // Ẩn chấm xanh
                            }
                        } else {
                            customUserLastSeen.setText("Offline");
                            customOnlineStatus.setVisibility(View.GONE);
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) { }
                });
    }

    private void SendMessage() {
        String messageText = MessageInputText.getText().toString();
        if (TextUtils.isEmpty(messageText)) {
            Toast.makeText(this, "First write your message...", Toast.LENGTH_SHORT).show();
            return;
        }

        updateTime(); // Cập nhật giờ ngay lúc gửi

        String messageSenderRef = "Messages/" + messageSenderID + "/" + messageReceiverID;
        String messageReceiverRef = "Messages/" + messageReceiverID + "/" + messageSenderID;

        DatabaseReference userMessageKeyRef = RootRef.child("Messages")
                .child(messageSenderID).child(messageReceiverID).push();
        String messagePushID = userMessageKeyRef.getKey();

        Map<String, Object> messageTextBody = new HashMap<>();
        messageTextBody.put("message", messageText);
        messageTextBody.put("type", "text");
        messageTextBody.put("from", messageSenderID);
        messageTextBody.put("to", messageReceiverID);
        messageTextBody.put("messageID", messagePushID);
        // FIX LỖI: Gán đúng biến Time vào key Time
        messageTextBody.put("time", saveCurrentTime);
        messageTextBody.put("date", saveCurrentDate);

        messageTextBody.put("isSeen", false); // Mặc định là chưa xem

        Map<String, Object> messageBodyDetails = new HashMap<>();
        messageBodyDetails.put(messageSenderRef + "/" + messagePushID, messageTextBody);
        messageBodyDetails.put(messageReceiverRef + "/" + messagePushID, messageTextBody);

        RootRef.updateChildren(messageBodyDetails).addOnCompleteListener(task -> {
            if (task.isSuccessful()) {

                HashMap<String, String> chatNotificationMap = new HashMap<>();
                chatNotificationMap.put("from", messageSenderID); // ID của mình
                chatNotificationMap.put("type", "message");       // Loại là tin nhắn
                chatNotificationMap.put("body", messageText);     // Nội dung tin nhắn (để hiện lên thông báo)

                // Ghi vào node Notifications của NGƯỜI NHẬN (messageReceiverID)
                RootRef.child("Notifications").child(messageReceiverID).push()
                        .setValue(chatNotificationMap);
                // Tin nhắn gửi thành công
                updateChatList();
            } else {
                Toast.makeText(ChatActivity.this, "Error Occurred...", Toast.LENGTH_SHORT).show();
            }
            MessageInputText.setText("");
        });
    }

    private void updateChatList() {
        DatabaseReference chatRef = FirebaseDatabase.getInstance().getReference().child("Chatlist");

        // Lấy thời gian hiện tại dạng miliseconds (Số càng lớn nghĩa là càng mới)
        long timestamp = System.currentTimeMillis();

        // Cập nhật cho Người Gửi (Mình)
        Map<String, Object> senderMap = new HashMap<>();
        senderMap.put("time", timestamp); // Dùng biến này để sắp xếp
        chatRef.child(messageSenderID).child(messageReceiverID).updateChildren(senderMap);

        // Cập nhật cho Người Nhận (Họ) -> Để mình cũng hiện lên đầu danh sách của họ
        Map<String, Object> receiverMap = new HashMap<>();
        receiverMap.put("time", timestamp);
        chatRef.child(messageReceiverID).child(messageSenderID).updateChildren(receiverMap);
    }

    // --- CÁC HÀM XỬ LÝ FILE / ẢNH ---
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

        startActivityForResult(Intent.createChooser(intent, "Select File"), 438);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == 438 && resultCode == RESULT_OK && data != null && data.getData() != null) {
            fileUri = data.getData();

            loadingBar.setTitle("Sending File");
            loadingBar.setMessage("Please wait, we are sending that file...");
            loadingBar.setCanceledOnTouchOutside(false);
            loadingBar.show();

            if (checker.equals("image")) {
                uploadImageToCloudinary(fileUri);
            } else if (checker.equals("pdf") || checker.equals("docx")) {
                uploadFileToCloudinary(fileUri, checker);
            } else {
                loadingBar.dismiss();
                Toast.makeText(this, "Nothing Selected, Error.", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void uploadImageToCloudinary(Uri imageUri) {
        // ... (Giữ nguyên logic Cloudinary của bạn)
        // Lưu ý: Tôi tóm tắt lại để code ngắn gọn, bạn giữ nguyên code upload cũ
        // Chỉ cần gọi updateTime() trước khi gọi sendImageMessage

        // Code demo ngắn gọn (Giữ nguyên code của bạn ở đây)
        // ...
        // Khi thành công gọi:
        // runOnUiThread(() -> sendImageMessage(imageUrl));

        // Code đầy đủ của bạn ở dưới đây (giữ nguyên logic upload):
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
                        Toast.makeText(ChatActivity.this, "Upload failed", Toast.LENGTH_SHORT).show();
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
                            loadingBar.dismiss();
                        }
                    }
                }
            });
        } catch (Exception e) { e.printStackTrace(); loadingBar.dismiss(); }
    }

    private void uploadFileToCloudinary(Uri fileUri, String fileType) {
        // ... (Giữ nguyên logic upload file của bạn)
        // Code đầy đủ của bạn ở dưới đây:
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
                        } catch (Exception e) { loadingBar.dismiss(); }
                    }
                }
            });

        } catch (Exception e) { e.printStackTrace(); loadingBar.dismiss(); }
    }

    private void sendImageMessage(String imageUrl) {
        updateTime(); // Cập nhật giờ

        String messageSenderRef = "Messages/" + messageSenderID + "/" + messageReceiverID;
        String messageReceiverRef = "Messages/" + messageReceiverID + "/" + messageSenderID;

        DatabaseReference userMessageKeyRef = RootRef.child("Messages").child(messageSenderID)
                .child(messageReceiverID).push();
        String messagePushID = userMessageKeyRef.getKey();

        Map<String, Object> messageTextBody = new HashMap<>();
        messageTextBody.put("message", imageUrl);
        messageTextBody.put("name", "chat_image.jpg");
        messageTextBody.put("type", "image");
        messageTextBody.put("from", messageSenderID);
        messageTextBody.put("to", messageReceiverID);
        messageTextBody.put("messageID", messagePushID);
        // FIX TIME
        messageTextBody.put("time", saveCurrentTime);
        messageTextBody.put("date", saveCurrentDate);

        messageTextBody.put("isSeen", false); // Mặc định là chưa xem

        Map<String, Object> messageBodyDetails = new HashMap<>();
        messageBodyDetails.put(messageSenderRef + "/" + messagePushID, messageTextBody);
        messageBodyDetails.put(messageReceiverRef + "/" + messagePushID, messageTextBody);

        RootRef.updateChildren(messageBodyDetails).addOnCompleteListener(task -> {
            loadingBar.dismiss();
            if (task.isSuccessful()) {
                Toast.makeText(ChatActivity.this, "Image sent successfully!", Toast.LENGTH_SHORT).show();
                updateChatList();
            }
        });
    }

    private void sendFileMessage(String fileUrl, String fileType) {
        updateTime(); // Cập nhật giờ

        String messageSenderRef = "Messages/" + messageSenderID + "/" + messageReceiverID;
        String messageReceiverRef = "Messages/" + messageReceiverID + "/" + messageSenderID;

        DatabaseReference userMessageKeyRef = RootRef.child("Messages").child(messageSenderID)
                .child(messageReceiverID).push();
        String messagePushID = userMessageKeyRef.getKey();

        Map<String, Object> messageTextBody = new HashMap<>();
        messageTextBody.put("message", fileUrl);
        messageTextBody.put("type", fileType);
        messageTextBody.put("from", messageSenderID);
        messageTextBody.put("to", messageReceiverID);
        messageTextBody.put("messageID", messagePushID);
        // FIX TIME
        messageTextBody.put("time", saveCurrentTime);
        messageTextBody.put("date", saveCurrentDate);

        messageTextBody.put("isSeen", false); // Mặc định là chưa xem

        Map<String, Object> messageBodyDetails = new HashMap<>();
        messageBodyDetails.put(messageSenderRef + "/" + messagePushID, messageTextBody);
        messageBodyDetails.put(messageReceiverRef + "/" + messagePushID, messageTextBody);

        RootRef.updateChildren(messageBodyDetails).addOnCompleteListener(task -> {
            loadingBar.dismiss();
            if (task.isSuccessful()) {
                Toast.makeText(ChatActivity.this, "File sent successfully!", Toast.LENGTH_SHORT).show();
                updateChatList();
            }
        });
    }

    @Override
    protected void onStart() {
        super.onStart();
        messagesList.clear();
        messageAdapter.notifyDataSetChanged();
        seenMessage(messageReceiverID);

        messageQueryRef = RootRef.child("Messages").child(messageSenderID).child(messageReceiverID);

        if (messageEventListener == null) {
            messageEventListener = new ChildEventListener() {
                @Override
                public void onChildAdded(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {
                    Messages messages = snapshot.getValue(Messages.class);
                    if (messages != null) {
                        if ((messages.getFrom().equals(messageSenderID) && messages.getTo().equals(messageReceiverID)) ||
                                (messages.getFrom().equals(messageReceiverID) && messages.getTo().equals(messageSenderID))) {

                            messagesList.add(messages);
                            messageAdapter.notifyItemInserted(messagesList.size() - 1);
                            userMessagesList.smoothScrollToPosition(messagesList.size() - 1);
                        }
                    }
                }

                @Override
                public void onChildChanged(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {
                    Messages changedMessage = snapshot.getValue(Messages.class);
                    if (changedMessage != null) {
                        int index = -1;
                        for (int i = 0; i < messagesList.size(); i++) {
                            if (messagesList.get(i).getMessageID().equals(changedMessage.getMessageID())) {
                                index = i;
                                break;
                            }
                        }
                        if (index != -1) {
                            messagesList.set(index, changedMessage);
                            messageAdapter.notifyItemChanged(index);
                        }
                    }
                }
                @Override public void onChildRemoved(@NonNull DataSnapshot snapshot) {}
                @Override public void onChildMoved(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {}
                @Override public void onCancelled(@NonNull DatabaseError error) {}
            };
        }
        messageQueryRef.addChildEventListener(messageEventListener);
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (messageQueryRef != null && messageEventListener != null) {
            messageQueryRef.removeEventListener(messageEventListener);
        }
        if (seenListener != null && userMessageRef != null) {
            userMessageRef.removeEventListener(seenListener);
        }
    }

    // Hàm xóa tin nhắn
    public void deleteMessageForEveryone(String messageId) {
        DatabaseReference rootRef = FirebaseDatabase.getInstance().getReference();

        String senderRef = "Messages/" + messageSenderID + "/" + messageReceiverID + "/" + messageId;
        String receiverRef = "Messages/" + messageReceiverID + "/" + messageSenderID + "/" + messageId;

        Map<String, Object> updateMap = new HashMap<>();
        updateMap.put(senderRef + "/message", "Tin nhắn đã bị thu hồi.");
        updateMap.put(senderRef + "/type", "deleted");
        updateMap.put(receiverRef + "/message", "Tin nhắn đã bị thu hồi.");
        updateMap.put(receiverRef + "/type", "deleted");

        rootRef.updateChildren(updateMap).addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                int index = -1;
                for (int i = 0; i < messagesList.size(); i++) {
                    if (messagesList.get(i).getMessageID().equals(messageId)) {
                        index = i;
                        messagesList.get(i).setMessage("Tin nhắn đã bị thu hồi.");
                        messagesList.get(i).setType("deleted");
                        break;
                    }
                }
                if (index != -1) {
                    messageAdapter.notifyItemChanged(index);
                }
                Toast.makeText(ChatActivity.this, "Đã thu hồi tin nhắn.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void seenMessage(final String userid){
        userMessageRef = RootRef.child("Messages").child(messageSenderID).child(userid);

        seenListener = userMessageRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if(snapshot.exists()){
                    for(DataSnapshot ds : snapshot.getChildren()){
                        // Nếu tin nhắn là của NGƯỜI KIA gửi cho MÌNH
                        if(ds.hasChild("from") && ds.child("from").getValue().toString().equals(userid)){
                            // Cập nhật lại thành true (đã xem)
                            HashMap<String, Object> hashMap = new HashMap<>();
                            hashMap.put("isSeen", true);
                            ds.getRef().updateChildren(hashMap);
                        }
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
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
            Intent profileIntent = new Intent(ChatActivity.this, ProfileActivity.class);
            profileIntent.putExtra("visit_user_id", messageReceiverID);

            startActivity(profileIntent);
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
}