package com.example.whatsapp;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
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

public class ContactsFragment extends Fragment {

    private View ContactsView;
    private RecyclerView myContactsList;
    private DatabaseReference ContactsRef, UsersRef;
    private FirebaseAuth mAuth;
    private String currentUserID;

    public ContactsFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        ContactsView = inflater.inflate(R.layout.fragment_contacts, container, false);

        // Khởi tạo RecyclerView
        myContactsList = ContactsView.findViewById(R.id.contacts_list);
        myContactsList.setLayoutManager(new LinearLayoutManager(getContext()));

        mAuth = FirebaseAuth.getInstance();
        currentUserID = mAuth.getCurrentUser().getUid();

        // Tham chiếu Database
        ContactsRef = FirebaseDatabase.getInstance().getReference().child("Contacts").child(currentUserID);
        UsersRef = FirebaseDatabase.getInstance().getReference().child("Users");

        return ContactsView;
    }

    @Override
    public void onStart() {
        super.onStart();

        FirebaseRecyclerOptions<Contacts> options =
                new FirebaseRecyclerOptions.Builder<Contacts>()
                        .setQuery(ContactsRef, Contacts.class)
                        .build();

        FirebaseRecyclerAdapter<Contacts, ContactsViewHolder> adapter =
                new FirebaseRecyclerAdapter<Contacts, ContactsViewHolder>(options) {
                    @Override
                    protected void onBindViewHolder(@NonNull final ContactsViewHolder holder, int position, @NonNull Contacts model) {

                        // Lấy ID của bạn bè
                        final String userIDs = getRef(position).getKey();

                        holder.time.setVisibility(View.GONE);
                        holder.unreadBadge.setVisibility(View.GONE);

                        // Lấy thông tin chi tiết từ node Users
                        UsersRef.child(userIDs).addValueEventListener(new ValueEventListener() {
                            @Override
                            public void onDataChange(DataSnapshot dataSnapshot) {
                                if (dataSnapshot.exists()) {

                                    // 1. Hiển thị Tên & Status (Bio)
                                    if (dataSnapshot.hasChild("name")) {
                                        String userName = dataSnapshot.child("name").getValue().toString();
                                        String userStatus = dataSnapshot.child("status").getValue().toString();

                                        holder.userName.setText(userName);
                                        holder.userStatus.setText(userStatus);
                                    }

                                    // 2. Hiển thị Ảnh đại diện
                                    if (dataSnapshot.hasChild("image")) {
                                        String userImage = dataSnapshot.child("image").getValue().toString();
                                        Picasso.get().load(userImage).placeholder(R.drawable.profile_image).into(holder.profileImage);
                                    }

                                    // 3. XỬ LÝ TRẠNG THÁI ONLINE (Chấm xanh)
                                    if (dataSnapshot.hasChild("userState")) {
                                        String state = dataSnapshot.child("userState").child("state").getValue().toString();

                                        if (state.equals("online")) {
                                            holder.onlineIcon.setVisibility(View.VISIBLE); // Hiện chấm xanh
                                        } else {
                                            holder.onlineIcon.setVisibility(View.GONE);    // Ẩn chấm xanh
                                        }
                                    } else {
                                        holder.onlineIcon.setVisibility(View.GONE);
                                    }

                                    // 4. Sự kiện click để nhắn tin
                                    holder.itemView.setOnClickListener(new View.OnClickListener() {
                                        @Override
                                        public void onClick(View view) {
                                            String visit_image = "default_image";
                                            if(dataSnapshot.hasChild("image")){
                                                visit_image = dataSnapshot.child("image").getValue().toString();
                                            }

                                            Intent chatIntent = new Intent(getContext(), ChatActivity.class);
                                            chatIntent.putExtra("visit_user_id", userIDs);
                                            chatIntent.putExtra("visit_user_name", holder.userName.getText().toString());
                                            chatIntent.putExtra("visit_image", visit_image);
                                            startActivity(chatIntent);
                                        }
                                    });
                                }
                            }

                            @Override
                            public void onCancelled(DatabaseError databaseError) { }
                        });
                    }

                    @NonNull
                    @Override
                    public ContactsViewHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int i) {
                        View view = LayoutInflater.from(viewGroup.getContext()).inflate(R.layout.users_display_layout, viewGroup, false);
                        return new ContactsViewHolder(view);
                    }
                };

        myContactsList.setAdapter(adapter);
        adapter.startListening();
    }

    // Class ViewHolder để ánh xạ view
    public static class ContactsViewHolder extends RecyclerView.ViewHolder {
        TextView userName, userStatus;
        CircleImageView profileImage;
        ImageView onlineIcon; // Icon chấm xanh

        TextView time, unreadBadge;

        public ContactsViewHolder(@NonNull View itemView) {
            super(itemView);
            userName = itemView.findViewById(R.id.user_profile_name);
            userStatus = itemView.findViewById(R.id.user_status);
            profileImage = itemView.findViewById(R.id.users_profile_image);
            onlineIcon = itemView.findViewById(R.id.user_online_status); // Ánh xạ chấm xanh

            time = itemView.findViewById(R.id.last_message_time);
            unreadBadge = itemView.findViewById(R.id.unread_message_count);
        }
    }
}