package com.example.whatsapp;

import android.Manifest;
import android.app.Activity;
import android.app.DownloadManager;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.squareup.picasso.Picasso;

import java.io.File;
import java.util.List;

import de.hdodenhof.circleimageview.CircleImageView;

public class MessageAdapter extends RecyclerView.Adapter<MessageAdapter.MessageViewHolder> {

    private final List<Messages> userMessagesList;
    private final FirebaseAuth mAuth = FirebaseAuth.getInstance();

    // 🟢 Truyền avatar người nhận chỉ 1 lần (fix lỗi load lặp trong onBind)
    private String receiverAvatarUrl = null;

    public void setReceiverAvatarUrl(String url) {
        this.receiverAvatarUrl = url;
    }

    public MessageAdapter(List<Messages> userMessagesList) {
        this.userMessagesList = userMessagesList;
    }

    @NonNull
    @Override
    public MessageViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.custom_messages_layout, parent, false);
        return new MessageViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull MessageViewHolder holder, int position) {

        Messages messages = userMessagesList.get(position);
        String currentUserId = mAuth.getCurrentUser().getUid();
        String fromUserID = messages.getFrom();
        String fromMessageType = messages.getType();

        // 🧹 Reset toàn bộ (để tránh view bị reuse sai nội dung)
        holder.senderMessageText.setVisibility(View.GONE);
        holder.receiverMessageText.setVisibility(View.GONE);
        holder.receiverProfileImage.setVisibility(View.GONE);
        holder.messageSenderPicture.setVisibility(View.GONE);
        holder.messageReceiverPicture.setVisibility(View.GONE);

        holder.senderMessageText.setOnClickListener(null);
        holder.receiverMessageText.setOnClickListener(null);

        // 🧑‍🤝‍🧑 Load ảnh đại diện người nhận (chỉ load 1 lần khi truyền vào Adapter)
        if (!fromUserID.equals(currentUserId)) {
            if (receiverAvatarUrl != null) {
                holder.receiverProfileImage.setVisibility(View.VISIBLE);
                Picasso.get()
                        .load(receiverAvatarUrl)
                        .placeholder(R.drawable.profile_image)
                        .error(R.drawable.profile_image)
                        .into(holder.receiverProfileImage);
            }
        }
        // -----------------------------
        // 🔥 HIỂN THỊ THEO LOẠI TIN NHẮN
        // -----------------------------

        switch (fromMessageType) {

            case "text":

                if (fromUserID.equals(currentUserId)) {   // Sender
                    holder.senderMessageText.setVisibility(View.VISIBLE);
                    holder.senderMessageText.setBackgroundResource(R.drawable.sender_message_layout);
                    holder.senderMessageText.setTextColor(Color.BLACK);

                    holder.senderMessageText.setText(messages.getMessage() +
                            "\n\n" + messages.getTime() + " - " + messages.getDate());

                    holder.senderMessageText.setOnLongClickListener(v -> {
                        showDeleteConfirmationDialog(holder.itemView.getContext(), messages.getMessageID());
                        return true;
                    });

                } else {  // Receiver
                    holder.receiverMessageText.setVisibility(View.VISIBLE);
                    holder.receiverMessageText.setBackgroundResource(R.drawable.receiver_messager_layout);
                    holder.receiverMessageText.setTextColor(Color.BLACK);

                    holder.receiverMessageText.setText(messages.getMessage() +
                            "\n\n" + messages.getTime() + " - " + messages.getDate());
                }
                break;


            case "image":
                if (fromUserID.equals(currentUserId)) {    // Sender (Người Gửi - Ảnh hiển thị bên phải)
                    holder.messageSenderPicture.setVisibility(View.VISIBLE);
                    Picasso.get()
                            .load(messages.getMessage())
                            .placeholder(R.drawable.profile_image)
                            .error(R.drawable.error)
                            .into(holder.messageSenderPicture);

                    holder.messageSenderPicture.setOnLongClickListener(v -> {
                        showDeleteConfirmationDialog(holder.itemView.getContext(), messages.getMessageID());
                        return true;
                    });

                } else {                                  // Receiver (Người Nhận - Ảnh hiển thị bên trái)
                    // 🌟 Sửa lỗi: Bắt buộc hiển thị ảnh đại diện người gửi để căn lề trái đúng cách
                    holder.receiverProfileImage.setVisibility(View.VISIBLE);

                    holder.messageReceiverPicture.setVisibility(View.VISIBLE);
                    Picasso.get()
                            .load(messages.getMessage())
                            .placeholder(R.drawable.profile_image)
                            .error(R.drawable.error)
                            .into(holder.messageReceiverPicture);
                }
                break;


            case "pdf":
            case "docx":
                // 1. Xác định tên hiển thị và đuôi file
                String typeLabel = fromMessageType.equals("pdf") ? "PDF" : "MS Word";
                String fileExtension = fromMessageType.equals("pdf") ? ".pdf" : ".docx";
                String fileIcon = "📄";

                // Tạo tên file duy nhất để khi tải về không bị trùng (Ví dụ: File_1702345678.pdf)
                String fileName = "File_" + System.currentTimeMillis();

                String displayText = fileIcon + " " + typeLabel + "\n(Nhấn để mở)";

                if (fromUserID.equals(currentUserId)) {
                    // --- PHÍA NGƯỜI GỬI (SENDER) ---
                    holder.senderMessageText.setVisibility(View.VISIBLE);
                    holder.senderMessageText.setBackgroundResource(R.drawable.sender_message_layout);
                    holder.senderMessageText.setTextColor(Color.BLACK);
                    holder.senderMessageText.setText(displayText);

                    // 👉 SỰ KIỆN CLICK: Gọi hàm downloadFile
                    holder.senderMessageText.setOnClickListener(v -> {
                        downloadFile(
                                holder.itemView.getContext(),
                                fileName,
                                fileExtension,
                                Environment.DIRECTORY_DOWNLOADS,
                                messages.getMessage() // Link URL từ Firebase
                        );
                    });

                    holder.senderMessageText.setOnLongClickListener(v -> {
                        showDeleteConfirmationDialog(holder.itemView.getContext(), messages.getMessageID());
                        return true;
                    });

                } else {
                    // --- PHÍA NGƯỜI NHẬN (RECEIVER) ---
                    holder.receiverProfileImage.setVisibility(View.VISIBLE); // Hiện Avatar
                    holder.receiverMessageText.setVisibility(View.VISIBLE);
                    holder.receiverMessageText.setBackgroundResource(R.drawable.receiver_messager_layout);
                    holder.receiverMessageText.setTextColor(Color.BLACK);
                    holder.receiverMessageText.setText(displayText);

                    // 👉 SỰ KIỆN CLICK: Gọi hàm downloadFile
                    holder.receiverMessageText.setOnClickListener(v -> {
                        downloadFile(
                                holder.itemView.getContext(),
                                fileName,
                                fileExtension,
                                Environment.DIRECTORY_DOWNLOADS,
                                messages.getMessage() // Link URL từ Firebase
                        );
                    });
                }
                break;
            case "deleted":
                // ... (reset các view khác)

                // 🚨 QUAN TRỌNG: Hiển thị ở giữa hoặc tùy thuộc vào người đang xem
                if (fromUserID.equals(currentUserId)) {
                    // Người gửi: Tái sử dụng senderMessageText để giữ lề phải (hoặc căn giữa)
                    holder.senderMessageText.setVisibility(View.VISIBLE);
                    holder.senderMessageText.setBackground(null); // Xóa background bong bóng
                    holder.senderMessageText.setText("🚫 Tin nhắn đã bị thu hồi (Bạn)");
                    holder.senderMessageText.setTextColor(Color.GRAY);
                } else {
                    // Người nhận: Tái sử dụng receiverMessageText
                    holder.receiverMessageText.setVisibility(View.VISIBLE);
                    holder.receiverMessageText.setBackground(null);
                    holder.receiverMessageText.setText("🚫 Tin nhắn đã bị thu hồi");
                    holder.receiverMessageText.setTextColor(Color.GRAY);
                }
                break;
        }
    }

    @Override
    public int getItemCount() {
        return userMessagesList.size();
    }

    // 📂 Mở file PDF/DOCX an toàn hơn
    private void openFile(android.content.Context context, String url, String mimeType) {
        // 1. Kiểm tra URL/MIME type hợp lệ
        if (url == null || url.isEmpty() || mimeType == null || mimeType.isEmpty()) {
            Toast.makeText(context, "URL hoặc loại tệp không hợp lệ.", Toast.LENGTH_SHORT).show();
            return;
        }

        // 2. Tạo Intent và thiết lập MIME type chính xác
        Intent intent = new Intent(Intent.ACTION_VIEW);
        // Sử dụng setDataAndType để gắn cả URI và loại tệp
        intent.setDataAndType(Uri.parse(url), mimeType);

        // 3. Thêm cờ NEW_TASK (quan trọng khi gọi startActivity từ Adapter)
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

        // Loại bỏ FLAG_GRANT_READ_URI_PERMISSION vì đây là URL công khai (http/https)

        try {
            // 4. Mở Chooser để người dùng chọn ứng dụng
            context.startActivity(Intent.createChooser(intent, "Chọn ứng dụng để mở tệp"));
        } catch (android.content.ActivityNotFoundException e) {
            // 5. Xử lý lỗi nếu không tìm thấy ứng dụng
            Toast.makeText(context, "Không tìm thấy ứng dụng nào phù hợp để mở tệp này.", Toast.LENGTH_LONG).show();
        }
    }


    public static class MessageViewHolder extends RecyclerView.ViewHolder {

        public TextView senderMessageText, receiverMessageText;
        public CircleImageView receiverProfileImage;
        public ImageView messageSenderPicture, messageReceiverPicture;

        public MessageViewHolder(@NonNull View itemView) {
            super(itemView);

            senderMessageText = itemView.findViewById(R.id.sender_message_text);
            receiverMessageText = itemView.findViewById(R.id.receiver_message_text);
            receiverProfileImage = itemView.findViewById(R.id.message_profile_image);

            messageSenderPicture = itemView.findViewById(R.id.message_sender_image_view);
            messageReceiverPicture = itemView.findViewById(R.id.message_receiver_image_view);
        }
    }

    private void downloadFile(Context context, String fileName, String fileExtension, String destinationDirectory, String url) {

        if (url == null || url.isEmpty()) {
            Toast.makeText(context, "Link file lỗi!", Toast.LENGTH_SHORT).show();
            return;
        }

        // 1. Ép dùng HTTPS
        if (url.startsWith("http://")) {
            url = url.replace("http://", "https://");
        }

        try {
            // 2. TẠO MAGIC LINK: Dùng Google Docs Viewer
            // Google sẽ tự tải file của bạn về và hiển thị nó trên trang web
            String googleDocsUrl = "https://docs.google.com/viewer?embedded=true&url=" + url;

            Uri uri = Uri.parse(googleDocsUrl);

            // 3. Mở trình duyệt
            Intent intent = new Intent(Intent.ACTION_VIEW, uri);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(intent);

            Toast.makeText(context, "Đang mở tài liệu...", Toast.LENGTH_SHORT).show();

        } catch (Exception e) {
            Toast.makeText(context, "Không thể mở trình duyệt!", Toast.LENGTH_SHORT).show();
        }
    }

    private void showDeleteConfirmationDialog(Context context, String messageId) {
        new AlertDialog.Builder(context)
                .setTitle("Thu hồi Tin nhắn")
                .setMessage("Bạn có chắc chắn muốn thu hồi tin nhắn này cho mọi người?")
                .setPositiveButton("Thu hồi", (dialog, which) -> {
                    // Chuyển Context về Activity để gọi hàm chính
                    if (context instanceof ChatActivity) {
                        ((ChatActivity) context).deleteMessageForEveryone(messageId);
                    }
                })
                .setNegativeButton("Hủy", null)
                .show();
    }

}
