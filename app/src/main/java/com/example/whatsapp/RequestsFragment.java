package com.example.whatsapp;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.firebase.ui.database.FirebaseRecyclerAdapter;
import com.firebase.ui.database.FirebaseRecyclerOptions;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.squareup.picasso.Picasso;

import de.hdodenhof.circleimageview.CircleImageView;

public class RequestsFragment extends Fragment {

    private View RequestFragmentView;
    private RecyclerView myRequestList;
    private DatabaseReference ChatRequestRef, UsersRef, ContactsRef;
    private FirebaseAuth mAuth;
    private String currentUserID;

    public RequestsFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        RequestFragmentView = inflater.inflate(R.layout.fragment_requests, container, false);

        myRequestList = RequestFragmentView.findViewById(R.id.chat_request_list);
        myRequestList.setLayoutManager(new LinearLayoutManager(getContext()));

        mAuth = FirebaseAuth.getInstance();
        currentUserID = mAuth.getCurrentUser().getUid();

        ChatRequestRef = FirebaseDatabase.getInstance().getReference().child("Chat Requests");
        UsersRef = FirebaseDatabase.getInstance().getReference().child("Users");
        ContactsRef = FirebaseDatabase.getInstance().getReference().child("Contacts");

        return RequestFragmentView;
    }

    @Override
    public void onStart() {
        super.onStart();

        FirebaseRecyclerOptions<Contacts> options =
                new FirebaseRecyclerOptions.Builder<Contacts>()
                        .setQuery(ChatRequestRef.child(currentUserID), Contacts.class)
                        .build();

        FirebaseRecyclerAdapter<Contacts, RequestsViewHolder> adapter =
                new FirebaseRecyclerAdapter<Contacts, RequestsViewHolder>(options) {
                    @Override
                    protected void onBindViewHolder(@NonNull final RequestsViewHolder holder, int position, @NonNull Contacts model) {

                        // Lấy ID người gửi/nhận yêu cầu
                        final String list_user_id = getRef(position).getKey();

                        DatabaseReference getTypeRef = getRef(position).child("request_type").getRef();

                        getTypeRef.addValueEventListener(new ValueEventListener() {
                            @Override
                            public void onDataChange(@NonNull DataSnapshot snapshot) {
                                if (snapshot.exists()) {
                                    String type = snapshot.getValue().toString();

                                    // --- TRƯỜNG HỢP 1: MÌNH NHẬN ĐƯỢC LỜI MỜI (Received) ---
                                    if (type.equals("received")) {
                                        // Hiện nút Accept/Decline, Ẩn nút Sent
                                        holder.ActionButtonsLayout.setVisibility(View.VISIBLE);
                                        holder.RequestSentButton.setVisibility(View.GONE);

                                        holder.userStatus.setText("Wants to connect with you");

                                        // Lấy thông tin người gửi để hiển thị
                                        UsersRef.child(list_user_id).addValueEventListener(new ValueEventListener() {
                                            @Override
                                            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                                                if (dataSnapshot.hasChild("image")) {
                                                    final String requestProfileImage = dataSnapshot.child("image").getValue().toString();
                                                    Picasso.get().load(requestProfileImage).into(holder.profileImage);
                                                }
                                                final String requestUserName = dataSnapshot.child("name").getValue().toString();
                                                holder.userName.setText(requestUserName);

                                                // Xử lý sự kiện nút Accept
                                                holder.AcceptButton.setOnClickListener(new View.OnClickListener() {
                                                    @Override
                                                    public void onClick(View view) {
                                                        AcceptChatRequest(list_user_id);
                                                    }
                                                });

                                                // Xử lý sự kiện nút Cancel (Decline)
                                                holder.DeclineButton.setOnClickListener(new View.OnClickListener() {
                                                    @Override
                                                    public void onClick(View view) {
                                                        CancelChatRequest(list_user_id);
                                                    }
                                                });
                                            }
                                            @Override public void onCancelled(@NonNull DatabaseError error) { }
                                        });
                                    }

                                    // --- TRƯỜNG HỢP 2: MÌNH ĐÃ GỬI LỜI MỜI (Sent) ---
                                    else if (type.equals("sent")) {
                                        // Ẩn nút Accept/Decline, Hiện nút Sent
                                        holder.ActionButtonsLayout.setVisibility(View.GONE);
                                        holder.RequestSentButton.setVisibility(View.VISIBLE);

                                        // Lấy thông tin người nhận
                                        UsersRef.child(list_user_id).addValueEventListener(new ValueEventListener() {
                                            @Override
                                            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                                                if (dataSnapshot.hasChild("image")) {
                                                    final String requestProfileImage = dataSnapshot.child("image").getValue().toString();
                                                    Picasso.get().load(requestProfileImage).into(holder.profileImage);
                                                }
                                                final String requestUserName = dataSnapshot.child("name").getValue().toString();
                                                holder.userName.setText(requestUserName);
                                                holder.userStatus.setText("You have sent a request to " + requestUserName);

                                                // Xử lý sự kiện nút "Request Sent" -> Hủy lời mời
                                                holder.RequestSentButton.setOnClickListener(new View.OnClickListener() {
                                                    @Override
                                                    public void onClick(View view) {
                                                        CharSequence options[] = new CharSequence[]{"Cancel Chat Request"};
                                                        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
                                                        builder.setTitle("Already Sent Request");
                                                        builder.setItems(options, new DialogInterface.OnClickListener() {
                                                            @Override
                                                            public void onClick(DialogInterface dialogInterface, int i) {
                                                                if (i == 0) {
                                                                    CancelChatRequest(list_user_id);
                                                                }
                                                            }
                                                        });
                                                        builder.show();
                                                    }
                                                });
                                            }
                                            @Override public void onCancelled(@NonNull DatabaseError error) { }
                                        });
                                    }
                                }
                            }
                            @Override public void onCancelled(@NonNull DatabaseError error) { }
                        });
                    }

                    @NonNull
                    @Override
                    public RequestsViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
                        // Đảm bảo dùng đúng layout friend_request_item_layout
                        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.friend_request_item_layout, parent, false);
                        return new RequestsViewHolder(view);
                    }
                };

        myRequestList.setAdapter(adapter);
        adapter.startListening();
    }

    // --- ViewHolder (Ánh xạ đúng ID trong friend_request_item_layout) ---
    public static class RequestsViewHolder extends RecyclerView.ViewHolder {
        TextView userName, userStatus;
        CircleImageView profileImage;
        Button AcceptButton, DeclineButton, RequestSentButton;
        View ActionButtonsLayout;

        public RequestsViewHolder(@NonNull View itemView) {
            super(itemView);
            userName = itemView.findViewById(R.id.user_profile_name);
            userStatus = itemView.findViewById(R.id.user_status);
            profileImage = itemView.findViewById(R.id.users_profile_image);

            AcceptButton = itemView.findViewById(R.id.request_accept_btn);
            DeclineButton = itemView.findViewById(R.id.request_decline_btn);
            RequestSentButton = itemView.findViewById(R.id.request_sent_btn);
            ActionButtonsLayout = itemView.findViewById(R.id.action_buttons_layout);
        }
    }

    // --- Hàm Chấp Nhận Kết Bạn ---
    private void AcceptChatRequest(final String list_user_id) {
        ContactsRef.child(currentUserID).child(list_user_id).child("Contact").setValue("Saved")
                .addOnCompleteListener(new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        if (task.isSuccessful()) {
                            ContactsRef.child(list_user_id).child(currentUserID).child("Contact").setValue("Saved")
                                    .addOnCompleteListener(new OnCompleteListener<Void>() {
                                        @Override
                                        public void onComplete(@NonNull Task<Void> task) {
                                            if (task.isSuccessful()) {
                                                // Xóa yêu cầu sau khi đã chấp nhận
                                                ChatRequestRef.child(currentUserID).child(list_user_id).removeValue()
                                                        .addOnCompleteListener(new OnCompleteListener<Void>() {
                                                            @Override
                                                            public void onComplete(@NonNull Task<Void> task) {
                                                                if (task.isSuccessful()) {
                                                                    ChatRequestRef.child(list_user_id).child(currentUserID).removeValue()
                                                                            .addOnCompleteListener(new OnCompleteListener<Void>() {
                                                                                @Override
                                                                                public void onComplete(@NonNull Task<Void> task) {
                                                                                    Toast.makeText(getContext(), "New Contact Saved", Toast.LENGTH_SHORT).show();
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

    // --- Hàm Hủy Yêu Cầu ---
    private void CancelChatRequest(final String list_user_id) {
        ChatRequestRef.child(currentUserID).child(list_user_id).removeValue()
                .addOnCompleteListener(new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        if (task.isSuccessful()) {
                            ChatRequestRef.child(list_user_id).child(currentUserID).removeValue()
                                    .addOnCompleteListener(new OnCompleteListener<Void>() {
                                        @Override
                                        public void onComplete(@NonNull Task<Void> task) {
                                            if (task.isSuccessful()) {
                                                Toast.makeText(getContext(), "Request Deleted", Toast.LENGTH_SHORT).show();
                                            }
                                        }
                                    });
                        }
                    }
                });
    }
}