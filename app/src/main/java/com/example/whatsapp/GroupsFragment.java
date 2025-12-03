package com.example.whatsapp;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.firebase.ui.database.FirebaseRecyclerAdapter;
import com.firebase.ui.database.FirebaseRecyclerOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.squareup.picasso.Picasso;

import de.hdodenhof.circleimageview.CircleImageView;

public class GroupsFragment extends Fragment {

    private View groupFragmentView;
    private RecyclerView groupsList;
    private DatabaseReference GroupRef, UsersRef;
    private FirebaseAuth mAuth;
    private String currentUserID;

    public GroupsFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        groupFragmentView = inflater.inflate(R.layout.fragment_groups, container, false);

        mAuth = FirebaseAuth.getInstance();
        currentUserID = mAuth.getCurrentUser().getUid();

        // --- SỬA ĐỔI QUAN TRỌNG: TRỎ VÀO USERGROUPS ---
        // Thay vì lấy tất cả nhóm, chỉ lấy nhóm của user hiện tại
        GroupRef = FirebaseDatabase.getInstance().getReference().child("UserGroups").child(currentUserID);
        UsersRef = FirebaseDatabase.getInstance().getReference().child("Users");

        groupsList = groupFragmentView.findViewById(R.id.groups_list);
        groupsList.setLayoutManager(new LinearLayoutManager(getContext()));

        return groupFragmentView;
    }

    @Override
    public void onStart() {
        super.onStart();

        // Truy vấn vào UserGroups
        FirebaseRecyclerOptions<Object> options =
                new FirebaseRecyclerOptions.Builder<Object>()
                        .setQuery(GroupRef, Object.class)
                        .build();

        FirebaseRecyclerAdapter<Object, GroupsViewHolder> adapter =
                new FirebaseRecyclerAdapter<Object, GroupsViewHolder>(options) {
                    @Override
                    protected void onBindViewHolder(@NonNull final GroupsViewHolder holder, int position, @NonNull Object model) {

                        // Lấy Tên Nhóm từ Key của node UserGroups
                        final String groupName = getRef(position).getKey();
                        holder.groupName.setText(groupName);

                        // 1. Load ảnh thành viên (Xếp chồng)
                        loadGroupIcons(groupName, holder);

                        // 2. Load tin nhắn cuối cùng (Trỏ vào GroupMessages)
                        loadLastMessage(groupName, holder);

                        // Ẩn badge tin chưa đọc (tính năng nâng cao chưa làm)
                        holder.badge.setVisibility(View.GONE);

                        // 3. Sự kiện click vào nhóm
                        holder.itemView.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View view) {
                                Intent groupChatIntent = new Intent(getContext(), GroupChatActivity.class);
                                groupChatIntent.putExtra("groupName", groupName);
                                startActivity(groupChatIntent);
                            }
                        });
                    }

                    @NonNull
                    @Override
                    public GroupsViewHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int i) {
                        View view = LayoutInflater.from(viewGroup.getContext()).inflate(R.layout.group_display_layout, viewGroup, false);
                        return new GroupsViewHolder(view);
                    }
                };

        groupsList.setAdapter(adapter);
        adapter.startListening();
    }

    // --- HÀM LOAD 3 ẢNH THÀNH VIÊN ---
    private void loadGroupIcons(String groupName, final GroupsViewHolder holder) {
        // Reset view
        holder.img1.setVisibility(View.GONE);
        holder.img2.setVisibility(View.GONE);
        holder.img3.setVisibility(View.GONE);

        // Truy cập vào danh sách thành viên trong node Groups gốc
        DatabaseReference membersRef = FirebaseDatabase.getInstance().getReference()
                .child("Groups").child(groupName).child("members");

        membersRef.limitToFirst(3).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                int count = 0;
                for (DataSnapshot ds : snapshot.getChildren()) {
                    String memberID = ds.getKey();
                    count++;
                    final int finalCount = count;

                    // Lấy ảnh từ Users
                    UsersRef.child(memberID).child("image").addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot imgSnapshot) {
                            String image = "default_image";
                            if (imgSnapshot.exists()) {
                                image = imgSnapshot.getValue().toString();
                            }

                            if (finalCount == 1) {
                                holder.img1.setVisibility(View.VISIBLE);
                                Picasso.get().load(image).placeholder(R.drawable.profile_image).error(R.drawable.profile_image).into(holder.img1);
                            } else if (finalCount == 2) {
                                holder.img2.setVisibility(View.VISIBLE);
                                Picasso.get().load(image).placeholder(R.drawable.profile_image).error(R.drawable.profile_image).into(holder.img2);
                            } else if (finalCount == 3) {
                                holder.img3.setVisibility(View.VISIBLE);
                                Picasso.get().load(image).placeholder(R.drawable.profile_image).error(R.drawable.profile_image).into(holder.img3);
                            }
                        }
                        @Override public void onCancelled(@NonNull DatabaseError error) {}
                    });
                }
            }
            @Override public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    // --- HÀM LOAD TIN NHẮN CUỐI ---
    private void loadLastMessage(String groupName, final GroupsViewHolder holder) {
        // Trỏ đúng vào GroupMessages
        DatabaseReference groupMsgRef = FirebaseDatabase.getInstance().getReference()
                .child("GroupMessages").child(groupName);

        groupMsgRef.limitToLast(1).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    for (DataSnapshot ds : snapshot.getChildren()) {
                        String message = "", time = "", type = "text", name = "";

                        if (ds.hasChild("message")) message = ds.child("message").getValue().toString();
                        if (ds.hasChild("time")) time = ds.child("time").getValue().toString();
                        if (ds.hasChild("type")) type = ds.child("type").getValue().toString();
                        if (ds.hasChild("name")) name = ds.child("name").getValue().toString();

                        String displayMsg = message;
                        if (type.equals("image")) displayMsg = "[Hình ảnh]";
                        else if (type.equals("pdf") || type.equals("docx")) displayMsg = "[Tài liệu]";
                        else if (type.equals("deleted")) displayMsg = "🚫 Tin nhắn đã thu hồi";

                        if (!name.isEmpty()) {
                            holder.lastMsg.setText(name + ": " + displayMsg);
                        } else {
                            holder.lastMsg.setText(displayMsg);
                        }

                        holder.time.setText(time);
                        holder.time.setVisibility(View.VISIBLE);
                    }
                } else {
                    holder.lastMsg.setText("Nhóm mới tạo");
                    holder.time.setVisibility(View.GONE);
                }
            }
            @Override public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    // --- VIEWHOLDER ---
    public static class GroupsViewHolder extends RecyclerView.ViewHolder {
        TextView groupName, lastMsg, time, badge;
        CircleImageView img1, img2, img3;

        public GroupsViewHolder(@NonNull View itemView) {
            super(itemView);
            groupName = itemView.findViewById(R.id.group_name);
            lastMsg = itemView.findViewById(R.id.group_last_message);
            time = itemView.findViewById(R.id.group_time);
            badge = itemView.findViewById(R.id.group_unread_badge);
            img1 = itemView.findViewById(R.id.group_member_image_1);
            img2 = itemView.findViewById(R.id.group_member_image_2);
            img3 = itemView.findViewById(R.id.group_member_image_3);
        }
    }
}