package com.example.whatsapp;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.Toolbar;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.firebase.ui.database.FirebaseRecyclerAdapter;
import com.firebase.ui.database.FirebaseRecyclerOptions;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.squareup.picasso.Picasso;

import de.hdodenhof.circleimageview.CircleImageView;

public class FindFriendsActivity extends AppCompatActivity {

    private Toolbar mToolBar;
    private RecyclerView FindFriendsRecyclerList;
    private DatabaseReference UsersRef;
    private EditText SearchInputText; // 1. Khai báo ô tìm kiếm

    // 2. Khai báo Adapter toàn cục để có thể start/stop listening
    private FirebaseRecyclerAdapter<Contacts, FindFriendViewHolder> adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_find_friends);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        UsersRef = FirebaseDatabase.getInstance().getReference().child("Users");

        FindFriendsRecyclerList = (RecyclerView) findViewById(R.id.find_friends_recycler_list);
        FindFriendsRecyclerList.setLayoutManager(new LinearLayoutManager(this));

        mToolBar = (Toolbar) findViewById(R.id.find_friends_toolbar);
        setSupportActionBar(mToolBar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowHomeEnabled(true);
        getSupportActionBar().setTitle("Find Friends");

        // 3. Ánh xạ và xử lý sự kiện tìm kiếm
        SearchInputText = findViewById(R.id.search_input_text);

        SearchInputText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                // Gọi hàm tìm kiếm mỗi khi người dùng gõ phím
                SearchPeopleAndFriends(s.toString());
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });
    }

    // Mặc định khi mở màn hình (chưa tìm kiếm) thì hiện tất cả
    @Override
    protected void onStart() {
        super.onStart();
        SearchPeopleAndFriends("");
    }

    // Khi thoát thì dừng adapter để tiết kiệm tài nguyên
    @Override
    protected void onStop() {
        super.onStop();
        if (adapter != null) {
            adapter.stopListening();
        }
    }

    // 4. Hàm xử lý tìm kiếm và hiển thị danh sách
    private void SearchPeopleAndFriends(String searchBoxInput) {

        Query searchPeopleQuery;

        if (searchBoxInput.isEmpty()) {
            // Nếu ô tìm kiếm trống, hiển thị tất cả (sắp xếp theo tên cho đẹp)
            searchPeopleQuery = UsersRef.orderByChild("name");
        } else {
            // Logic tìm kiếm Firebase: bắt đầu bằng từ khóa và kết thúc bằng mã unicode cao nhất
            // Ví dụ: tìm "A" sẽ ra "An", "Anh", "Alibaba"...
            searchPeopleQuery = UsersRef.orderByChild("name")
                    .startAt(searchBoxInput)
                    .endAt(searchBoxInput + "\uf8ff");
        }

        FirebaseRecyclerOptions<Contacts> options =
                new FirebaseRecyclerOptions.Builder<Contacts>()
                        .setQuery(searchPeopleQuery, Contacts.class)
                        .build();

        adapter = new FirebaseRecyclerAdapter<Contacts, FindFriendViewHolder>(options) {
            @Override
            protected void onBindViewHolder(@NonNull FindFriendViewHolder holder, int position, @NonNull Contacts model) {
                holder.userName.setText(model.getName());
                holder.userStatus.setText(model.getStatus());

                // Kiểm tra null hoặc rỗng trước khi load ảnh
                if (model.getImage() != null && !model.getImage().isEmpty()) {
                    Picasso.get().load(model.getImage()).placeholder(R.drawable.profile_image).into(holder.profileImage);
                } else {
                    holder.profileImage.setImageResource(R.drawable.profile_image);
                }

                // Ẩn các view thừa
                holder.time.setVisibility(View.GONE);
                holder.unreadBadge.setVisibility(View.GONE);

                // --- PHẦN SỬA LỖI CẢNH BÁO ---
                holder.itemView.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        // 1. Lấy vị trí thực tế tại thời điểm bấm
                        int currentPosition = holder.getBindingAdapterPosition();

                        // 2. Kiểm tra vị trí hợp lệ (tránh lỗi crash nếu item đó vừa bị xóa)
                        if (currentPosition != RecyclerView.NO_POSITION) {
                            // 3. Dùng currentPosition để lấy Key
                            String visit_user_id = getRef(currentPosition).getKey();

                            Intent profileIntent = new Intent(FindFriendsActivity.this, ProfileActivity.class);
                            profileIntent.putExtra("visit_user_id", visit_user_id);
                            startActivity(profileIntent);
                        }
                    }
                });
                // -----------------------------
            }

            @NonNull
            @Override
            public FindFriendViewHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int viewType) {
                View view = LayoutInflater.from(viewGroup.getContext()).inflate(R.layout.users_display_layout, viewGroup, false);
                return new FindFriendViewHolder(view);
            }
        };

        FindFriendsRecyclerList.setAdapter(adapter);
        adapter.startListening();
    }

    public static class FindFriendViewHolder extends RecyclerView.ViewHolder {
        TextView userName, userStatus;
        CircleImageView profileImage;
        TextView time, unreadBadge;

        public FindFriendViewHolder(@NonNull View itemView) {
            super(itemView);
            userName = itemView.findViewById(R.id.user_profile_name);
            userStatus = itemView.findViewById(R.id.user_status);
            profileImage = itemView.findViewById(R.id.users_profile_image);
            time = itemView.findViewById(R.id.last_message_time);
            unreadBadge = itemView.findViewById(R.id.unread_message_count);
        }
    }
}