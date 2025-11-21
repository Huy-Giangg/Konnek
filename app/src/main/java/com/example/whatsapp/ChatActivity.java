package com.example.whatsapp;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
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
import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

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
import java.io.InputStream;
import java.io.IOException;
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

public class ChatActivity extends AppCompatActivity {

    private String messageReceiverID, messageReceiverName, messageReceiverImage, messageSenderID;
    private TextView userName, userLastSeen;
    private CircleImageView usersImage;
    private Toolbar ChatToolBar;
    private DatabaseReference RootRef;

    private ImageButton SendMessageButton, SendFilesButton;
    private EditText MessageInputText;
    private FirebaseAuth mAuth;

    private final List<Messages> messagesList = new ArrayList<>();
    private LinearLayoutManager linearLayoutManager;
    private MessageAdapter messageAdapter;
    private RecyclerView userMessagesList;

    private String saveCurrentTime, getSaveCurrentTime;
    private String checker = "", myUrl = "";
    private Uri fileUri;
    private ProgressDialog loadingBar;

    private ChildEventListener messageEventListener;
    private DatabaseReference messageQueryRef;

    private final OkHttpClient client = new OkHttpClient();


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

        messageReceiverID = getIntent().getStringExtra("visit_user_id");
        messageReceiverName = getIntent().getStringExtra("visit_user_name");
        messageReceiverImage = getIntent().getStringExtra("visit_image");

        mAuth = FirebaseAuth.getInstance();
        messageSenderID = mAuth.getCurrentUser().getUid();
        RootRef = FirebaseDatabase.getInstance().getReference();

        InitializeFields();

        userName.setText(messageReceiverName);
        Picasso.get().load(messageReceiverImage)
                .placeholder(R.drawable.profile_image)
                .into(usersImage);

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
                    selectFile("application/msword");
                }
            });
            builder.show();
        });

        DisplayLastSeen();
    }


    private void InitializeFields() {
        ChatToolBar = findViewById(R.id.chat_toolbar);
        setSupportActionBar(ChatToolBar);

        ActionBar actionBar = getSupportActionBar();
        actionBar.setDisplayHomeAsUpEnabled(true);
        actionBar.setDisplayShowCustomEnabled(true);

        LayoutInflater layoutInflater = (LayoutInflater) this.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View actionBarView = layoutInflater.inflate(R.layout.custom_chat_bar, null);
        actionBar.setCustomView(actionBarView);

        usersImage = findViewById(R.id.custom_profile_IMAGE);
        userName = findViewById(R.id.custom_profile_name);
        userLastSeen = findViewById(R.id.custom_user_last_seen);

        SendMessageButton = findViewById(R.id.send_message_btn);
        SendFilesButton = findViewById(R.id.send_files_btn);
        MessageInputText = findViewById(R.id.input_message);

        messageAdapter = new MessageAdapter(messagesList);
        userMessagesList = findViewById(R.id.private_messages_list_of_users);

        linearLayoutManager = new LinearLayoutManager(this);
        userMessagesList.setLayoutManager(linearLayoutManager);
        userMessagesList.setAdapter(messageAdapter);

        loadingBar = new ProgressDialog(this);

        Calendar calendar = Calendar.getInstance();
        SimpleDateFormat curentDate = new SimpleDateFormat("MMM dd, yyyy");
        saveCurrentTime = curentDate.format(calendar.getTime());

        SimpleDateFormat currentTime = new SimpleDateFormat("hh:mm a");
        getSaveCurrentTime = currentTime.format(calendar.getTime());

        messageAdapter.setReceiverAvatarUrl(messageReceiverImage);
    }

    private void selectFile(String type) {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);

        // Nếu yêu cầu là Word, ta cho phép chọn cả .doc và .docx
        if (type.equals("application/msword")) {
            intent.setType("*/*"); // Đặt tạm là tất cả để không bị lỗi bộ lọc
            String[] mimetypes = {
                    "application/msword", // .doc
                    "application/vnd.openxmlformats-officedocument.wordprocessingml.document" // .docx
            };
            intent.putExtra(Intent.EXTRA_MIME_TYPES, mimetypes);
        } else {
            // Các loại khác (ảnh, pdf) giữ nguyên
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
        loadingBar.setTitle("Uploading...");
        loadingBar.setMessage("Please wait while we upload the image...");
        loadingBar.setCanceledOnTouchOutside(false);
        loadingBar.show();

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
                        Toast.makeText(ChatActivity.this, "Upload failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
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
                            runOnUiThread(() -> {
                                loadingBar.dismiss();
                                Toast.makeText(ChatActivity.this, "Error parsing Cloudinary response!", Toast.LENGTH_SHORT).show();
                            });
                        }
                    } else {
                        runOnUiThread(() -> {
                            loadingBar.dismiss();
                            Toast.makeText(ChatActivity.this, "Upload failed: " + response.message(), Toast.LENGTH_SHORT).show();
                        });
                    }
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
            loadingBar.dismiss();
            Toast.makeText(this, "Error reading image: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void uploadFileToCloudinary(Uri fileUri, String fileType) {
        loadingBar.setTitle("Sending File");
        loadingBar.setMessage("Please wait while we upload the file...");
        loadingBar.setCanceledOnTouchOutside(false);
        loadingBar.show();

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
                    runOnUiThread(() -> {
                        loadingBar.dismiss();
                        Toast.makeText(ChatActivity.this, "Upload failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    });
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
                            runOnUiThread(() -> {
                                loadingBar.dismiss();
                                Toast.makeText(ChatActivity.this, "Error parsing response!", Toast.LENGTH_SHORT).show();
                            });
                        }
                    } else {
                        runOnUiThread(() -> {
                            loadingBar.dismiss();
                            Toast.makeText(ChatActivity.this, "Upload failed: " + response.message(), Toast.LENGTH_SHORT).show();
                        });
                    }
                }
            });

        } catch (Exception e) {
            e.printStackTrace();
            loadingBar.dismiss();
            Toast.makeText(this, "Error reading file: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void sendImageMessage(String imageUrl) {
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
        messageTextBody.put("time", saveCurrentTime);
        messageTextBody.put("date", getSaveCurrentTime);

        Map<String, Object> messageBodyDetails = new HashMap<>();
        messageBodyDetails.put(messageSenderRef + "/" + messagePushID, messageTextBody);
        messageBodyDetails.put(messageReceiverRef + "/" + messagePushID, messageTextBody);

        RootRef.updateChildren(messageBodyDetails).addOnCompleteListener(task -> {
            loadingBar.dismiss();
            if (task.isSuccessful()) {
                Toast.makeText(ChatActivity.this, "Image sent successfully!", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(ChatActivity.this, "Failed to send message!", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void sendFileMessage(String fileUrl, String fileType) {
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
        messageTextBody.put("time", saveCurrentTime);
        messageTextBody.put("date", getSaveCurrentTime);

        Map<String, Object> messageBodyDetails = new HashMap<>();
        messageBodyDetails.put(messageSenderRef + "/" + messagePushID, messageTextBody);
        messageBodyDetails.put(messageReceiverRef + "/" + messagePushID, messageTextBody);

        RootRef.updateChildren(messageBodyDetails).addOnCompleteListener(task -> {
            loadingBar.dismiss();
            if (task.isSuccessful()) {
                Toast.makeText(ChatActivity.this, "File sent successfully!", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(ChatActivity.this, "Error sending file!", Toast.LENGTH_SHORT).show();
            }
        });
    }


    @Override
    protected void onStart() {
        super.onStart();

        //updateUserStatus("online");

        messagesList.clear();
        messageAdapter.notifyDataSetChanged();

        // 1. Tạo đường dẫn tham chiếu (để dùng cho cả việc thêm và xóa listener)
        messageQueryRef = RootRef.child("Messages").child(messageSenderID).child(messageReceiverID);

        // 2. Định nghĩa Listener (Nếu chưa có)
        if (messageEventListener == null) {
            messageEventListener = new ChildEventListener() {
                @Override
                public void onChildAdded(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {
                    Messages messages = snapshot.getValue(Messages.class);
                    if (messages != null) {
                        // Code cũ của bạn giữ nguyên
                        if ((messages.getFrom().equals(messageSenderID) && messages.getTo().equals(messageReceiverID)) ||
                                (messages.getFrom().equals(messageReceiverID) && messages.getTo().equals(messageSenderID))) {

                            messagesList.add(messages);
                            messageAdapter.notifyItemInserted(messagesList.size() - 1);
                            userMessagesList.smoothScrollToPosition(messagesList.size() - 1); // Dùng smoothScroll đẹp hơn
                        }
                    }
                }

                // Trong ChatActivity.java, bên trong ChildEventListener (trong hàm onStart)

                @Override
                public void onChildChanged(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {
                    // Chuyển DataSnapshot thành đối tượng Messages (Object mới)
                    Messages changedMessage = snapshot.getValue(Messages.class);

                    if (changedMessage != null) {
                        // 1. Tìm vị trí (index) của tin nhắn đã thay đổi trong danh sách local
                        int index = -1;
                        // Giả sử userMessagesList là List<Messages> data source của bạn
                        for (int i = 0; i < messagesList.size(); i++) {
                            // So sánh MessageID để tìm tin nhắn cũ
                            if (messagesList.get(i).getMessageID().equals(changedMessage.getMessageID())) {
                                index = i;
                                break;
                            }
                        }

                        // 2. Cập nhật danh sách và giao diện
                        if (index != -1) {
                            // Thay thế đối tượng Messages cũ bằng đối tượng mới (đã có type="deleted")
                            messagesList.set(index, changedMessage);

                            // 🔔 Báo cho Adapter cập nhật View ngay lập tức
                            messageAdapter.notifyItemChanged(index);
                        }
                    }
                }

                // ⚠️ Lưu ý: Các hàm onChildRemoved, onCancelled,... giữ nguyên (hoặc bỏ trống nếu chưa cần)
                @Override
                public void onChildRemoved(@NonNull DataSnapshot snapshot) {}
                @Override
                public void onChildMoved(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {}
                @Override
                public void onCancelled(@NonNull DatabaseError error) {}
            };
        }

        // 3. Gắn Listener vào Database
        messageQueryRef.addChildEventListener(messageEventListener);
    }

    @Override
    protected void onStop() {
        super.onStop();
        // Khi màn hình bị ẩn hoặc tắt, gỡ bỏ người nghe để không bị trùng lặp
        if (messageQueryRef != null && messageEventListener != null) {
            messageQueryRef.removeEventListener(messageEventListener);
        }

        //updateUserStatus("offline");
    }


    private void SendMessage() {
        String messageText = MessageInputText.getText().toString();
        if (TextUtils.isEmpty(messageText)) {
            Toast.makeText(this, "First write your message...", Toast.LENGTH_SHORT).show();
            return;
        }

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
        messageTextBody.put("time", saveCurrentTime);
        messageTextBody.put("date", getSaveCurrentTime);

        Map<String, Object> messageBodyDetails = new HashMap<>();
        messageBodyDetails.put(messageSenderRef + "/" + messagePushID, messageTextBody);
        messageBodyDetails.put(messageReceiverRef + "/" + messagePushID, messageTextBody);

        RootRef.updateChildren(messageBodyDetails).addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                Toast.makeText(ChatActivity.this, "Message Sent Successfully...", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(ChatActivity.this, "Error Occurred! While Sending Message...", Toast.LENGTH_SHORT).show();
            }
            MessageInputText.setText("");
        });
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
                                userLastSeen.setText("Online");
                            } else if (state.equals("offline")) {
                                userLastSeen.setText("Last Seen: " + date + " " + time);
                            }
                        } else {
                            userLastSeen.setText("Offline");
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) { }
                });
    }

    public void deleteMessageForEveryone(String messageId) {

        // Tham chiếu đến RootRef đã được khởi tạo trong onCreate
        DatabaseReference rootRef = FirebaseDatabase.getInstance().getReference();

        // 1. Định nghĩa đường dẫn cần cập nhật (Sử dụng các biến messageSenderID/messageReceiverID đã có)
        String senderRef = "Messages/" + messageSenderID + "/" + messageReceiverID + "/" + messageId;
        String receiverRef = "Messages/" + messageReceiverID + "/" + messageSenderID + "/" + messageId;

        // 2. Tạo Map chứa các cập nhật đa đường dẫn (Multi-path Update)
        Map<String, Object> updateMap = new HashMap<>();

        // Cập nhật nội dung và type cho node người gửi
        updateMap.put(senderRef + "/message", "Tin nhắn đã bị thu hồi.");
        updateMap.put(senderRef + "/type", "deleted");

        // Cập nhật nội dung và type cho node người nhận
        updateMap.put(receiverRef + "/message", "Tin nhắn đã bị thu hồi.");
        updateMap.put(receiverRef + "/type", "deleted");

        // 3. Thực hiện cập nhật đồng thời
        rootRef.updateChildren(updateMap)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        // 🚀 BƯỚC 1: TÌM TIN NHẮN TRONG DANH SÁCH
                        int index = -1;
                        // 🚨 DÙNG DANH SÁCH DỮ LIỆU messagesList THAY VÌ userMessagesList
                        for (int i = 0; i < messagesList.size(); i++) {
                            if (messagesList.get(i).getMessageID().equals(messageId)) {
                                index = i;

                                // Cập nhật TRỰC TIẾP đối tượng trong danh sách
                                messagesList.get(i).setMessage("Tin nhắn đã bị thu hồi.");
                                messagesList.get(i).setType("deleted");

                                break;
                            }
                        }

                        // BƯỚC 3: CẬP NHẬT GIAO DIỆN NẾU TÌM THẤY
                        if (index != -1) {
                            // 🔔 Thông báo cho Adapter chỉ cập nhật vị trí này
                            messageAdapter.notifyItemChanged(index);

                            // Optional: Cuộn xuống cuối nếu tin nhắn là tin mới nhất
                            // userMessagesList.size() - 1 == index
                        }

                        Toast.makeText(this, "Đã thu hồi tin nhắn cho mọi người.", Toast.LENGTH_SHORT).show();

                    } else {
                        // ... (Xử lý lỗi) ...
                    }
                });
    }

}
