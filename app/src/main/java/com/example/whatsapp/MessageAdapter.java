package com.example.whatsapp;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Environment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.squareup.picasso.Picasso;

import java.util.List;

import de.hdodenhof.circleimageview.CircleImageView;

public class MessageAdapter extends RecyclerView.Adapter<MessageAdapter.MessageViewHolder> {

    private final List<Messages> userMessagesList;
    private final FirebaseAuth mAuth = FirebaseAuth.getInstance();
    private String receiverAvatarUrl = null;
    private boolean isGroupChat; // Biến cờ xác định là Chat nhóm hay Chat riêng

    // Constructor nhận thêm biến isGroupChat
    public MessageAdapter(List<Messages> userMessagesList, boolean isGroupChat) {
        this.userMessagesList = userMessagesList;
        this.isGroupChat = isGroupChat;
    }

    public void setReceiverAvatarUrl(String url) {
        this.receiverAvatarUrl = url;
    }

    @NonNull
    @Override
    public MessageViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // Ánh xạ layout custom_messages_layout
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

        // 1. RESET TRẠNG THÁI VIEW (Tránh lỗi hiển thị khi cuộn)
        holder.senderMessageText.setVisibility(View.GONE);
        holder.receiverMessageText.setVisibility(View.GONE);
        holder.receiverProfileImage.setVisibility(View.GONE);
        holder.messageSenderPicture.setVisibility(View.GONE);
        holder.messageReceiverPicture.setVisibility(View.GONE);

        // Reset Tên người gửi
        holder.senderName.setVisibility(View.GONE);
        holder.senderName.setText("");

        // Reset sự kiện click/style
        holder.senderMessageText.setTypeface(null, Typeface.NORMAL);
        holder.receiverMessageText.setTypeface(null, Typeface.NORMAL);
        holder.senderMessageText.setOnClickListener(null);
        holder.receiverMessageText.setOnClickListener(null);
        holder.senderMessageText.setOnLongClickListener(null);
        holder.messageSenderPicture.setOnLongClickListener(null);

        // 2. LOAD AVATAR NGƯỜI NHẬN
        if (!fromUserID.equals(currentUserId)) {

            // Luôn hiện khung ảnh cho người nhận
            holder.receiverProfileImage.setVisibility(View.VISIBLE);

            if (isGroupChat) {
                // --- LOGIC CHO NHÓM: Lấy ảnh theo ID từng người ---
                DatabaseReference usersRef = FirebaseDatabase.getInstance().getReference().child("Users").child(fromUserID);

                usersRef.addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        if (snapshot.exists() && snapshot.hasChild("image")) {
                            String receiverImage = snapshot.child("image").getValue().toString();

                            // Load ảnh người gửi tin nhắn đó
                            Picasso.get().load(receiverImage)
                                    .placeholder(R.drawable.profile_image)
                                    .error(R.drawable.profile_image)
                                    .into(holder.receiverProfileImage);
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) { }
                });

            } else {
                // --- LOGIC CHO CHAT CÁ NHÂN (Giữ nguyên cũ) ---
                // Dùng biến receiverAvatarUrl đã truyền từ Activity sang
                if (receiverAvatarUrl != null) {
                    Picasso.get().load(receiverAvatarUrl)
                            .placeholder(R.drawable.profile_image)
                            .into(holder.receiverProfileImage);
                }
            }
        }

        // 3. XỬ LÝ HIỂN THỊ THEO LOẠI TIN NHẮN
        switch (fromMessageType) {
            case "text":
                // Format: Nội dung + Giờ (nhỏ)
                String timeColor = fromUserID.equals(currentUserId) ? "#e0e0e0" : "#757575";
                String formattedMessage = messages.getMessage() + "<br><small><font color='" + timeColor + "'>" + messages.getTime() + "</font></small>";

                if (fromUserID.equals(currentUserId)) {
                    // --- SENDER (Gửi đi) ---
                    holder.senderMessageText.setVisibility(View.VISIBLE);
                    holder.senderMessageText.setBackgroundResource(R.drawable.sender_message_layout);
                    holder.senderMessageText.setTextColor(Color.WHITE);
                    holder.senderMessageText.setText(android.text.Html.fromHtml(formattedMessage));

                    // Sự kiện xóa (Chỉ người gửi mới xóa được)
                    holder.senderMessageText.setOnLongClickListener(v -> {
                        showDeleteConfirmationDialog(holder.itemView.getContext(), messages.getMessageID());
                        return true;
                    });
                } else {
                    // --- RECEIVER (Nhận về) ---
                    holder.receiverProfileImage.setVisibility(View.VISIBLE);
                    holder.receiverMessageText.setVisibility(View.VISIBLE);
                    holder.receiverMessageText.setBackgroundResource(R.drawable.receiver_messager_layout);
                    holder.receiverMessageText.setTextColor(Color.BLACK);
                    holder.receiverMessageText.setText(android.text.Html.fromHtml(formattedMessage));

                    // 🔥 LOGIC QUAN TRỌNG: HIỂN THỊ TÊN NGƯỜI GỬI TRONG NHÓM
                    if (isGroupChat) {
                        holder.senderName.setVisibility(View.VISIBLE);
                        holder.senderName.setText(messages.getName() != null ? messages.getName() : "Unknown");
                    }
                }
                break;

            case "image":
                if (fromUserID.equals(currentUserId)) {
                    // SENDER IMAGE
                    holder.messageSenderPicture.setVisibility(View.VISIBLE);
                    Picasso.get().load(messages.getMessage()).placeholder(R.drawable.profile_image).into(holder.messageSenderPicture);

                    holder.messageSenderPicture.setOnLongClickListener(v -> {
                        showDeleteConfirmationDialog(holder.itemView.getContext(), messages.getMessageID());
                        return true;
                    });
                } else {
                    // RECEIVER IMAGE
                    holder.receiverProfileImage.setVisibility(View.VISIBLE);
                    holder.messageReceiverPicture.setVisibility(View.VISIBLE);
                    Picasso.get().load(messages.getMessage()).placeholder(R.drawable.profile_image).into(holder.messageReceiverPicture);

                    // Hiện tên người gửi nếu là nhóm
                    if (isGroupChat) {
                        holder.senderName.setVisibility(View.VISIBLE);
                        holder.senderName.setText(messages.getName());
                    }
                }
                break;

            case "pdf":
            case "docx":
                String typeLabel = fromMessageType.equals("pdf") ? "PDF" : "MS Word";
                String fileIcon = "📄";

                // Format hiển thị File
                String displayText = fileIcon + " <b>" + typeLabel + " File</b><br><small>(Nhấn để mở)</small><br>" +
                        "<small><font color='#e0e0e0'>" + messages.getTime() + "</font></small>";

                String displayTextReceiver = fileIcon + " <b>" + typeLabel + " File</b><br><small>(Nhấn để mở)</small><br>" +
                        "<small><font color='#757575'>" + messages.getTime() + "</font></small>";

                if (fromUserID.equals(currentUserId)) {
                    // SENDER FILE
                    holder.senderMessageText.setVisibility(View.VISIBLE);
                    holder.senderMessageText.setBackgroundResource(R.drawable.sender_message_layout);
                    holder.senderMessageText.setTextColor(Color.WHITE);
                    holder.senderMessageText.setText(android.text.Html.fromHtml(displayText));

                    holder.senderMessageText.setOnClickListener(v -> downloadFile(holder.itemView.getContext(), messages.getMessage()));

                    holder.senderMessageText.setOnLongClickListener(v -> {
                        showDeleteConfirmationDialog(holder.itemView.getContext(), messages.getMessageID());
                        return true;
                    });
                } else {
                    // RECEIVER FILE
                    holder.receiverProfileImage.setVisibility(View.VISIBLE);
                    holder.receiverMessageText.setVisibility(View.VISIBLE);
                    holder.receiverMessageText.setBackgroundResource(R.drawable.receiver_messager_layout);
                    holder.receiverMessageText.setTextColor(Color.BLACK);
                    holder.receiverMessageText.setText(android.text.Html.fromHtml(displayTextReceiver));

                    holder.receiverMessageText.setOnClickListener(v -> downloadFile(holder.itemView.getContext(), messages.getMessage()));

                    // Hiện tên người gửi nếu là nhóm
                    if (isGroupChat) {
                        holder.senderName.setVisibility(View.VISIBLE);
                        holder.senderName.setText(messages.getName());
                    }
                }
                break;

            case "deleted":
                // Xử lý tin nhắn đã thu hồi
                String deletedText = "<i>🚫 Tin nhắn đã bị thu hồi</i>";

                if (fromUserID.equals(currentUserId)) {
                    holder.senderMessageText.setVisibility(View.VISIBLE);
                    holder.senderMessageText.setBackgroundResource(R.drawable.sender_message_layout);
                    holder.senderMessageText.setTextColor(Color.LTGRAY);
                    holder.senderMessageText.setText(android.text.Html.fromHtml(deletedText));
                } else {
                    holder.receiverProfileImage.setVisibility(View.VISIBLE);
                    holder.receiverMessageText.setVisibility(View.VISIBLE);
                    holder.receiverMessageText.setBackgroundResource(R.drawable.receiver_messager_layout);
                    holder.receiverMessageText.setTextColor(Color.GRAY);
                    holder.receiverMessageText.setText(android.text.Html.fromHtml(deletedText));

                    if (isGroupChat) {
                        holder.senderName.setVisibility(View.VISIBLE);
                        holder.senderName.setText(messages.getName());
                    }
                }
                break;
        }
    }

    @Override
    public int getItemCount() {
        return userMessagesList.size();
    }

    // --- VIEWHOLDER ---
    public static class MessageViewHolder extends RecyclerView.ViewHolder {
        public TextView senderMessageText, receiverMessageText, senderName;
        public CircleImageView receiverProfileImage;
        public ImageView messageSenderPicture, messageReceiverPicture;

        public MessageViewHolder(@NonNull View itemView) {
            super(itemView);

            senderMessageText = itemView.findViewById(R.id.sender_message_text);
            receiverMessageText = itemView.findViewById(R.id.receiver_message_text);
            receiverProfileImage = itemView.findViewById(R.id.message_profile_image);
            messageSenderPicture = itemView.findViewById(R.id.message_sender_image_view);
            messageReceiverPicture = itemView.findViewById(R.id.message_receiver_image_view);

            // Ánh xạ Tên người gửi (Bắt buộc phải có ID này trong custom_messages_layout.xml)
            senderName = itemView.findViewById(R.id.message_sender_name);
        }
    }

    // --- CÁC HÀM HỖ TRỢ ---

    private void downloadFile(Context context, String url) {
        if (url == null || url.isEmpty()) {
            Toast.makeText(context, "Link lỗi!", Toast.LENGTH_SHORT).show();
            return;
        }

        // Ép về HTTPS để bảo mật và mở được trên Google Docs
        if (url.startsWith("http://")) url = url.replace("http://", "https://");

        try {
            // Mở link bằng Google Docs Viewer
            Uri uri = Uri.parse("https://docs.google.com/viewer?embedded=true&url=" + url);
            Intent intent = new Intent(Intent.ACTION_VIEW, uri);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(intent);
        } catch (Exception e) {
            Toast.makeText(context, "Không thể mở file", Toast.LENGTH_SHORT).show();
        }
    }

    private void showDeleteConfirmationDialog(Context context, String messageId) {
        new AlertDialog.Builder(context)
                .setTitle("Thu hồi tin nhắn")
                .setMessage("Bạn có chắc chắn muốn thu hồi tin nhắn này cho mọi người?")
                .setPositiveButton("Thu hồi", (dialog, which) -> {

                    // 1. Nếu đang ở Chat Cá Nhân
                    if (context instanceof ChatActivity) {
                        ((ChatActivity) context).deleteMessageForEveryone(messageId);
                    }
                    // 2. THÊM ĐOẠN NÀY: Nếu đang ở Chat Nhóm
                    else if (context instanceof GroupChatActivity) {
                        ((GroupChatActivity) context).deleteMessageForEveryone(messageId);
                    }

                })
                .setNegativeButton("Hủy", null)
                .show();
    }
}