package com.example.whatsapp;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.Cancellable;
import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.squareup.picasso.Picasso;

import java.util.HashMap;

import de.hdodenhof.circleimageview.CircleImageView;

public class ProfileActivity extends AppCompatActivity {

    // Vùng Khai báo Fields
    private String receiverUserID, senderUserID;
    private String currentStatus; // Đã đổi tên từ current_status

    // Vùng Khai báo UI
    private CircleImageView userProfileImage;
    private TextView userProfileName, userProfileStatus;
    private Button sendMessageRequestButton, DeclineMessageButton; // Đã đổi tên từ SendMessageRequestButton

    // Vùng Khai báo Firebase
    private DatabaseReference usersRef, chatRequestsRef, ContactsRef, NotificationRef;// Đã đổi tên UsersRef, ChatRequestsRef
    private FirebaseAuth mAuth;

    // --- Phương thức Vòng đời (Lifecycle Methods) ---

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_profile);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // 1. Khởi tạo Firebase
        mAuth = FirebaseAuth.getInstance();
        usersRef = FirebaseDatabase.getInstance().getReference().child("Users");
        chatRequestsRef = FirebaseDatabase.getInstance().getReference().child("Chat Requests");
        ContactsRef = FirebaseDatabase.getInstance().getReference().child("Contacts");
        NotificationRef = FirebaseDatabase.getInstance().getReference().child("Notifications");


        // 2. Lấy User IDs
        if (mAuth.getCurrentUser() != null) {
            senderUserID = mAuth.getCurrentUser().getUid();
        }

        if (getIntent().getExtras() != null && getIntent().getExtras().get("visit_user_id") != null) {
            receiverUserID = getIntent().getExtras().get("visit_user_id").toString();
            Toast.makeText(this, "User ID: " + receiverUserID, Toast.LENGTH_SHORT).show();
        }

        // 3. Khởi tạo UI
        userProfileImage = findViewById(R.id.visit_profile_image);
        userProfileName = findViewById(R.id.visit_user_name);
        sendMessageRequestButton = findViewById(R.id.send_message_button);
        userProfileStatus = findViewById(R.id.visit_profile_status);

        DeclineMessageButton = findViewById(R.id.decline_message_button);

        // 4. Khởi tạo trạng thái
        currentStatus = "new";

        // 5. Lấy thông tin người dùng
        retrieveUserInfo();
    }

    // --- Phương thức Xử lý Dữ liệu và Logic ---

    /**
     * Lấy thông tin người dùng được truy cập và hiển thị lên UI.
     */
    private void retrieveUserInfo() {
        usersRef.child(receiverUserID).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    String userName = snapshot.child("name").getValue(String.class);
                    String userStatus = snapshot.child("status").getValue(String.class);

                    userProfileName.setText(userName);
                    userProfileStatus.setText(userStatus);

                    if (snapshot.hasChild("image")) {
                        String userImage = snapshot.child("image").getValue(String.class);
                        Picasso.get().load(userImage).placeholder(R.drawable.profile_image).into(userProfileImage);
                    } else {
                        // Đặt ảnh mặc định nếu không có
                        userProfileImage.setImageResource(R.drawable.profile_image);
                    }

                    // Gọi ManageChatRequests sau khi thông tin người dùng đã được tải
                    manageChatRequests();
                } else {
                    Toast.makeText(ProfileActivity.this, "User profile not found.", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(ProfileActivity.this, "Database Error: " + error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    /**
     * Quản lý trạng thái yêu cầu trò chuyện và thiết lập trình lắng nghe nút bấm.
     */
    private void manageChatRequests() {

        chatRequestsRef.child(senderUserID)
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        if(snapshot.hasChild(receiverUserID)){
                            String request_type = snapshot.child(receiverUserID).child("request_type").getValue(String.class);
                            if(request_type.equals("sent")){
                                currentStatus = "request_sent";
                                sendMessageRequestButton.setText("Cancel Chat Request");
                            }
                            else if(request_type.equals("received")){
                                currentStatus = "request_received";
                                sendMessageRequestButton.setText("Accept Chat Request");

                                DeclineMessageButton.setVisibility(View.VISIBLE);
                                DeclineMessageButton.setEnabled(false);

                                DeclineMessageButton.setOnClickListener(new View.OnClickListener() {
                                    @Override
                                    public void onClick(View v) {
                                        cancelChatRequest();
                                    }
                                });
                            }

                        }else{
                            ContactsRef.child(senderUserID)
                                    .addListenerForSingleValueEvent(new ValueEventListener() {
                                        @Override
                                        public void onDataChange(@NonNull DataSnapshot snapshot) {
                                            if(snapshot.hasChild(receiverUserID)){
                                                currentStatus = "friends";
                                                sendMessageRequestButton.setText("Remove this Contact");
                                            }

                                        }

                                        @Override
                                        public void onCancelled(@NonNull DatabaseError error) {

                                        }
                                    });
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {

                    }
                });
        // Kiểm tra nếu là hồ sơ của chính mình
        if (senderUserID.equals(receiverUserID)) {
            sendMessageRequestButton.setVisibility(View.INVISIBLE);
        } else {
            sendMessageRequestButton.setVisibility(View.VISIBLE);

            // Logic kiểm tra trạng thái ban đầu (friends, received, sent) nên được thêm ở đây
            // (Hiện tại chỉ xử lý trường hợp 'new' trong listener)

            sendMessageRequestButton.setOnClickListener(v -> {
                sendMessageRequestButton.setEnabled(false);

                // **LỖI LOGIC ĐÃ SỬA:** Xử lý các trạng thái yêu cầu trò chuyện đúng đắn.
                switch (currentStatus) {
                    case "new":
                        sendChatRequest();
                        break;
                    case "request_sent":
                        cancelChatRequest();
                        break;
                    // Cần thêm case "request_received" và "friends"
                     case "request_received":
                         AcceptChatRequest();
                         break;
                     case "friends":
                         removeSpecificContact();
                         break;
                    default:
                        // Nếu không phải "new" và không phải các trạng thái khác đã kiểm tra
                        sendMessageRequestButton.setEnabled(true);
                        break;
                }
            });
        }
    }

    private void removeSpecificContact() {
        ContactsRef.child(senderUserID).child(receiverUserID).removeValue()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        ContactsRef.child(receiverUserID).child(senderUserID).removeValue()
                                .addOnCompleteListener(task2 -> {
                                    if (task2.isSuccessful()) {
                                        sendMessageRequestButton.setEnabled(true);
                                        currentStatus = "new";
                                        sendMessageRequestButton.setText("Send Message");
                                        Toast.makeText(ProfileActivity.this, "Chat Request Canceled.", Toast.LENGTH_SHORT).show();
                                    }
                                });
                    }
                });
    }

    private void AcceptChatRequest() {
        ContactsRef.child(senderUserID).child(receiverUserID)
                .child("Contact").setValue("Saved")
                .addOnCompleteListener(new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        if (task.isSuccessful()) {
                            ContactsRef.child(receiverUserID).child(senderUserID)
                                    .child("Contact").setValue("Saved")
                                    .addOnCompleteListener(new OnCompleteListener<Void>() {
                                        @Override
                                        public void onComplete(@NonNull Task<Void> task) {
                                            if (task.isSuccessful()) {
                                                // Xóa yêu cầu trong Chat Requests (chứ không phải trong Contacts)
                                                chatRequestsRef.child(senderUserID).child(receiverUserID)
                                                        .removeValue()
                                                        .addOnCompleteListener(new OnCompleteListener<Void>() {
                                                            @Override
                                                            public void onComplete(@NonNull Task<Void> task) {
                                                                if (task.isSuccessful()) {
                                                                    chatRequestsRef.child(receiverUserID).child(senderUserID)
                                                                            .removeValue()
                                                                            .addOnCompleteListener(new OnCompleteListener<Void>() {
                                                                                @Override
                                                                                public void onComplete(@NonNull Task<Void> task) {
                                                                                    sendMessageRequestButton.setEnabled(true);
                                                                                    currentStatus = "friends";
                                                                                    sendMessageRequestButton.setText("Remove this Contact");

                                                                                    DeclineMessageButton.setVisibility(View.INVISIBLE);
                                                                                    DeclineMessageButton.setEnabled(false);
                                                                                }
                                                                            });
                                                                }
                                                            }
                                                        });
                                            }
                                        }
                                    });
                        }
                    }
                });
    }


    /**
     * Gửi yêu cầu trò chuyện: sender -> sent, receiver -> received.
     */
    private void sendChatRequest() {
        chatRequestsRef.child(senderUserID).child(receiverUserID).child("request_type").setValue("sent")
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        chatRequestsRef.child(receiverUserID).child(senderUserID).child("request_type").setValue("received")
                                .addOnCompleteListener(task2 -> {
                                    if (task2.isSuccessful()) {

                                        HashMap<String, String> chatnotificationMap = new HashMap<>();
                                        chatnotificationMap.put("from", senderUserID);
                                        chatnotificationMap.put("type", "request");
                                        NotificationRef.child(receiverUserID).push().setValue(chatnotificationMap)
                                                        .addOnCompleteListener(new OnCompleteListener<Void>() {
                                                            @Override
                                                            public void onComplete(@NonNull Task<Void> task) {
                                                                if(task.isSuccessful()){
                                                                    sendMessageRequestButton.setEnabled(true);
                                                                    currentStatus = "request_sent";
                                                                    sendMessageRequestButton.setText("Cancel Chat Request");
                                                                    Toast.makeText(ProfileActivity.this, "Chat Request Sent.", Toast.LENGTH_SHORT).show();
                                                                }
                                                            }
                                                        });
                                    } else {
                                        Toast.makeText(ProfileActivity.this, "Failed to update receiver status.", Toast.LENGTH_SHORT).show();
                                        sendMessageRequestButton.setEnabled(true);
                                    }
                                });
                    } else {
                        Toast.makeText(ProfileActivity.this, "Failed to send request.", Toast.LENGTH_SHORT).show();
                        sendMessageRequestButton.setEnabled(true);
                    }
                });
    }

    /**
     * Hủy yêu cầu trò chuyện (Chỉ cần thiết lập nút enabled và status)
     */
    private void cancelChatRequest() {
        chatRequestsRef.child(senderUserID).child(receiverUserID).removeValue()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        chatRequestsRef.child(receiverUserID).child(senderUserID).removeValue()
                                .addOnCompleteListener(task2 -> {
                                    if (task2.isSuccessful()) {
                                        sendMessageRequestButton.setEnabled(true);
                                        currentStatus = "new";
                                        sendMessageRequestButton.setText("Send Message");
                                        Toast.makeText(ProfileActivity.this, "Chat Request Canceled.", Toast.LENGTH_SHORT).show();
                                    }
                                });
                    }
                });
    }

    // Ghi chú: Các phương thức acceptChatRequest() và removeSpecificContact()
    // cần được thêm vào để hoàn thiện logic quản lý trạng thái.
}