package com.example.whatsapp;

import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.firebase.ui.database.FirebaseRecyclerAdapter;
import com.firebase.ui.database.FirebaseRecyclerOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;
import com.squareup.picasso.Picasso;

import de.hdodenhof.circleimageview.CircleImageView;

public class ChatsFragment extends Fragment {

    private View PrivatechatsView;
    private RecyclerView chatsList;
    private DatabaseReference chatsRef, usersRef, messagesRef;
    private FirebaseAuth mAuth;
    private String currentUserID;

    public ChatsFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        PrivatechatsView = inflater.inflate(R.layout.fragment_chats, container, false);

        mAuth = FirebaseAuth.getInstance();
        currentUserID = mAuth.getCurrentUser().getUid();

        // 1. THAY ĐỔI QUAN TRỌNG: Trỏ vào node "Chatlist" để lấy danh sách đã sắp xếp thời gian
        // (Thay vì node "Contacts" chỉ xếp theo tên)
        chatsRef = FirebaseDatabase.getInstance().getReference().child("Chatlist").child(currentUserID);

        usersRef = FirebaseDatabase.getInstance().getReference().child("Users");
        messagesRef = FirebaseDatabase.getInstance().getReference().child("Messages").child(currentUserID);

        chatsList = (RecyclerView) PrivatechatsView.findViewById(R.id.chats_list);

        // 2. THAY ĐỔI QUAN TRỌNG: Đảo ngược danh sách để tin mới nhất (Time lớn nhất) lên đầu
        LinearLayoutManager layoutManager = new LinearLayoutManager(getContext());
        layoutManager.setReverseLayout(true);
        layoutManager.setStackFromEnd(true);
        chatsList.setLayoutManager(layoutManager);

        return PrivatechatsView;
    }

    @Override
    public void onStart() {
        super.onStart();

        // Sắp xếp theo thời gian
        Query chatQuery = chatsRef.orderByChild("time");

        // 1. ĐỔI Contacts.class THÀNH Chatlist.class
        FirebaseRecyclerOptions<Chatlist> options
                = new FirebaseRecyclerOptions.Builder<Chatlist>()
                .setQuery(chatQuery, Chatlist.class)
                .build();

        // 2. ĐỔI ADAPTER SANG Chatlist
        FirebaseRecyclerAdapter<Chatlist, ChatsViewHolder> adapter =
                new FirebaseRecyclerAdapter<Chatlist, ChatsViewHolder>(options) {
                    @Override
                    protected void onBindViewHolder(@NonNull final ChatsViewHolder holder, int position, @NonNull Chatlist model) {

                        // Lấy User ID từ Key của node
                        final String userIDs = getRef(position).getKey();
                        final String[] retImage = {"default_image"};

                        // --- PHẦN LẤY DỮ LIỆU USER & TIN NHẮN GIỮ NGUYÊN ---
                        usersRef.child(userIDs).addValueEventListener(new ValueEventListener() {
                            @Override
                            public void onDataChange(@NonNull DataSnapshot snapshot) {
                                if (snapshot.exists()) {
                                    if (snapshot.hasChild("image")) {
                                        retImage[0] = snapshot.child("image").getValue().toString();
                                        Picasso.get().load(retImage[0]).placeholder(R.drawable.profile_image).into(holder.profileImage);
                                    }

                                    final String retName = snapshot.child("name").getValue().toString();
                                    holder.userName.setText(retName);

                                    // Gọi hàm lastMessage
                                    lastMessage(userIDs, holder.userStatus, holder.time, holder.unreadBadge);

                                    // Online status logic... (Giữ nguyên)
                                    if (snapshot.hasChild("userState")) {
                                        String state = snapshot.child("userState").child("state").getValue().toString();
                                        if (state.equals("online")) {
                                            holder.onlineIcon.setVisibility(View.VISIBLE);
                                        } else {
                                            holder.onlineIcon.setVisibility(View.GONE);
                                        }
                                    } else {
                                        holder.onlineIcon.setVisibility(View.GONE);
                                    }
                                }
                            }
                            @Override
                            public void onCancelled(@NonNull DatabaseError error) { }
                        });

                        holder.itemView.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                Intent chatIntent = new Intent(getContext(), ChatActivity.class);
                                chatIntent.putExtra("visit_user_id", userIDs);
                                chatIntent.putExtra("visit_user_name", holder.userName.getText().toString());
                                chatIntent.putExtra("visit_image", retImage[0]);
                                startActivity(chatIntent);
                            }
                        });
                    }

                    @NonNull
                    @Override
                    public ChatsViewHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int i) {
                        View view = LayoutInflater.from(viewGroup.getContext()).inflate(R.layout.users_display_layout, viewGroup, false);
                        return new ChatsViewHolder(view);
                    }
                };

        chatsList.setAdapter(adapter);
        adapter.startListening();
    }

    // --- HÀM XỬ LÝ LOGIC TIN NHẮN CUỐI ---
    private void lastMessage(final String chatUserId, final TextView lastMsgView, final TextView timeView, final TextView badgeView) {

        // Truy vấn tin nhắn giữa mình và người kia
        DatabaseReference reference = FirebaseDatabase.getInstance().getReference().child("Messages").child(currentUserID).child(chatUserId);

        reference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                String theLastMessage = "default";
                String theLastTime = "";
                int unreadCount = 0; // Biến đếm

                if (snapshot.exists()) {
                    for (DataSnapshot ds : snapshot.getChildren()) {
                        // 1. Lấy nội dung tin cuối cùng (Code cũ)
                        String type = "";
                        if(ds.hasChild("type")){ type = ds.child("type").getValue().toString(); }

                        String message = "";
                        if(ds.hasChild("message")){ message = ds.child("message").getValue().toString(); }

                        if(ds.hasChild("time")){ theLastTime = ds.child("time").getValue().toString(); }

                        if (type.equals("text")) {
                            theLastMessage = message;
                        } else if (type.equals("image")) {
                            theLastMessage = "[Hình ảnh]";
                        } else if (type.equals("pdf") || type.equals("docx")) {
                            theLastMessage = "[Tài liệu]";
                        } else if (type.equals("deleted")) {
                            theLastMessage = "🚫 Tin nhắn đã thu hồi";
                        }

                        // 2. LOGIC ĐẾM SỐ TIN CHƯA ĐỌC (MỚI)
                        // Logic: Nếu tin nhắn gửi CHO MÌNH (to == currentUserID) và isSeen == false
                        if (ds.hasChild("to") && ds.child("to").getValue().toString().equals(currentUserID)) {
                            if (ds.hasChild("isSeen")) {
                                boolean isSeen = (boolean) ds.child("isSeen").getValue();
                                if (!isSeen) {
                                    unreadCount++;
                                }
                            }
                        }
                    }
                }

                // 3. Cập nhật giao diện
                if ("default".equals(theLastMessage)) {
                    lastMsgView.setText("Các bạn chưa trò chuyện");
                    timeView.setText("");
                    lastMsgView.setTypeface(null, Typeface.NORMAL);
                } else {
                    lastMsgView.setText(theLastMessage);
                    timeView.setText(theLastTime);
                }

                // 4. HIỂN THỊ BADGE SỐ LƯỢNG
                if (unreadCount > 0) {
                    badgeView.setVisibility(View.VISIBLE);
                    badgeView.setText(String.valueOf(unreadCount));

                    // Bôi đậm tin nhắn chưa đọc
                    lastMsgView.setTypeface(null, Typeface.BOLD);
                    lastMsgView.setTextColor(Color.BLACK);
                    timeView.setTextColor(Color.parseColor("#03A9F4")); // Màu xanh
                } else {
                    badgeView.setVisibility(View.GONE);

                    // Trả về bình thường
                    lastMsgView.setTypeface(null, Typeface.NORMAL);
                    lastMsgView.setTextColor(Color.GRAY);
                    timeView.setTextColor(Color.GRAY);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) { }
        });
    }

    // --- VIEWHOLDER ---
    public static class ChatsViewHolder extends RecyclerView.ViewHolder {

        CircleImageView profileImage;
        TextView userName, userStatus; // userStatus đóng vai trò là Last Message
        TextView time, unreadBadge;
        ImageView onlineIcon;

        public ChatsViewHolder(@NonNull View itemView) {
            super(itemView);

            profileImage = itemView.findViewById(R.id.users_profile_image);
            userName = itemView.findViewById(R.id.user_profile_name);

            // Trong layout mới, ID user_status dùng để hiện Last Message
            userStatus = itemView.findViewById(R.id.user_status);

            // Ánh xạ 2 view mới thêm trong file users_display_layout.xml
            time = itemView.findViewById(R.id.last_message_time);
            unreadBadge = itemView.findViewById(R.id.unread_message_count);
            onlineIcon = itemView.findViewById(R.id.user_online_status);
        }
    }
}