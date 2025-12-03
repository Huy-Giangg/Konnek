package com.example.whatsapp;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

public class AddMembersBottomSheet extends BottomSheetDialogFragment {

    private RecyclerView recyclerView;
    private MemberAdapter adapter;
    private List<Contacts> contactList = new ArrayList<>();
    private DatabaseReference usersRef, contactsRef;
    private Button btnCancel, btnAdd;
    private EditText searchInput; // 1. Khai báo biến
    private String currentUserID;

    // Interface để trả dữ liệu về
    public interface OnMembersSelectedListener {
        void onMembersSelected(List<Contacts> selectedContacts);
    }
    private OnMembersSelectedListener listener;
    public void setOnMembersSelectedListener(OnMembersSelectedListener listener) {
        this.listener = listener;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.activity_add_members_bottom_sheet, container, false);

        currentUserID = FirebaseAuth.getInstance().getCurrentUser().getUid();
        usersRef = FirebaseDatabase.getInstance().getReference().child("Users");
        // Quan trọng: Chỉ lấy danh sách bạn bè (Contacts) chứ không phải toàn bộ Users
        contactsRef = FirebaseDatabase.getInstance().getReference().child("Contacts").child(currentUserID);

        recyclerView = view.findViewById(R.id.recycler_view_add_members);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        btnCancel = view.findViewById(R.id.btn_cancel_add);
        btnAdd = view.findViewById(R.id.btn_confirm_add);
        searchInput = view.findViewById(R.id.search_member_input); // 2. Ánh xạ

        loadFriends();

        // 3. Xử lý sự kiện tìm kiếm
        searchInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (adapter != null) {
                    adapter.filter(s.toString()); // Gọi hàm lọc
                }
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        btnCancel.setOnClickListener(v -> dismiss());

        btnAdd.setOnClickListener(v -> {
            if (adapter != null) {
                List<Contacts> selected = adapter.getSelectedUsers();
                if (selected.size() > 0) {
                    if (listener != null) {
                        listener.onMembersSelected(selected);
                    }
                    dismiss();
                } else {
                    Toast.makeText(getContext(), "Please select members", Toast.LENGTH_SHORT).show();
                }
            }
        });

        return view;
    }

    private void loadFriends() {
        // Lấy danh sách ID bạn bè từ node Contacts
        contactsRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                contactList.clear();
                // Duyệt qua từng ID bạn bè
                for (DataSnapshot ds : snapshot.getChildren()) {
                    String friendID = ds.getKey();

                    // Truy vấn thông tin chi tiết từ node Users
                    usersRef.child(friendID).addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot userSnapshot) {
                            if (userSnapshot.exists()) {
                                Contacts contact = userSnapshot.getValue(Contacts.class);
                                if (contact != null) {
                                    contact.setUid(userSnapshot.getKey()); // Gán ID
                                    contactList.add(contact);

                                    // Cập nhật Adapter (vì đây là bất đồng bộ nên cần cập nhật liên tục hoặc đếm đủ số lượng)
                                    // Cách đơn giản nhất là cứ add xong 1 người thì notify 1 lần hoặc tạo mới adapter
                                    if(adapter == null) {
                                        adapter = new MemberAdapter(contactList);
                                        recyclerView.setAdapter(adapter);
                                    } else {
                                        // Gọi hàm updateList để làm mới cả displayList bên trong Adapter
                                        adapter.updateList(contactList);
                                    }
                                }
                            }
                        }
                        @Override public void onCancelled(@NonNull DatabaseError error) {}
                    });
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) { }
        });
    }
}