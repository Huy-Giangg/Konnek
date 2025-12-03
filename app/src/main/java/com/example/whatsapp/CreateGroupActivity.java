package com.example.whatsapp;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class CreateGroupActivity extends AppCompatActivity {

    private Toolbar mToolbar;
    private EditText groupNameField;
    private Button btnCreateGroup, btnAddMembers;

    // Khai báo thêm cho phần danh sách thành viên
    private RecyclerView selectedMembersRecycler;
    private SelectedMembersAdapter selectedMembersAdapter;
    private List<Contacts> membersList = new ArrayList<>(); // Danh sách chứa thành viên đã chọn

    // Firebase
    private FirebaseAuth mAuth;
    private DatabaseReference RootRef;
    private String currentUserID;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_group);

        // Khởi tạo Firebase
        mAuth = FirebaseAuth.getInstance();
        currentUserID = mAuth.getCurrentUser().getUid();
        RootRef = FirebaseDatabase.getInstance().getReference();

        InitializeFields();

        // 1. Cấu hình RecyclerView hiển thị thành viên đã chọn
        selectedMembersRecycler.setLayoutManager(new LinearLayoutManager(this));
        selectedMembersAdapter = new SelectedMembersAdapter(membersList);
        selectedMembersRecycler.setAdapter(selectedMembersAdapter);

        // 2. Xử lý nút Create Group
        btnCreateGroup.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String groupName = groupNameField.getText().toString();

                if (TextUtils.isEmpty(groupName)) {
                    Toast.makeText(CreateGroupActivity.this, "Please enter group name...", Toast.LENGTH_SHORT).show();
                } else {
                    CreateNewGroup(groupName);
                }
            }
        });

        // 3. Xử lý nút Add Members và nhận dữ liệu trả về
        btnAddMembers.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                AddMembersBottomSheet bottomSheet = new AddMembersBottomSheet();

                // Lắng nghe sự kiện khi chọn xong thành viên
                bottomSheet.setOnMembersSelectedListener(new AddMembersBottomSheet.OnMembersSelectedListener() {
                    @Override
                    public void onMembersSelected(List<Contacts> selectedContacts) {
                        // Xóa danh sách cũ và thêm danh sách mới
                        membersList.clear();
                        membersList.addAll(selectedContacts);

                        // Cập nhật giao diện
                        selectedMembersAdapter.notifyDataSetChanged();
                    }
                });

                bottomSheet.show(getSupportFragmentManager(), "AddMembersBottomSheet");
            }
        });
    }

    private void InitializeFields() {
        mToolbar = findViewById(R.id.create_group_toolbar);

        // Kiểm tra null để tránh Crash
        if (mToolbar != null) {
            setSupportActionBar(mToolbar);
            if (getSupportActionBar() != null) {
                getSupportActionBar().setDisplayHomeAsUpEnabled(true);
                getSupportActionBar().setDisplayShowHomeEnabled(true);
                getSupportActionBar().setTitle("Create Group");
            }
            mToolbar.setNavigationOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    onBackPressed();
                }
            });
        }

        groupNameField = findViewById(R.id.input_group_name);
        btnCreateGroup = findViewById(R.id.btn_create_group);
        btnAddMembers = findViewById(R.id.btn_add_members);

        // Ánh xạ RecyclerView (Bạn nhớ thêm id này vào file XML activity_create_group nhé)
        selectedMembersRecycler = findViewById(R.id.recycler_view_selected_members);
    }

    private void CreateNewGroup(final String groupName) {
        // Tạo Reference đến nhóm mới trong node "Groups"
        final DatabaseReference groupRef = RootRef.child("Groups").child(groupName);

        groupRef.setValue("").addOnCompleteListener(new OnCompleteListener<Void>() {
            @Override
            public void onComplete(@NonNull Task<Void> task) {
                if (task.isSuccessful()) {

                    // --- 1. CHUẨN BỊ DATA THÀNH VIÊN ---
                    HashMap<String, Object> membersMap = new HashMap<>();

                    // Thêm chính mình là Admin
                    membersMap.put(currentUserID, "admin");

                    // Thêm các thành viên đã chọn từ danh sách
                    for (Contacts contact : membersList) {
                        membersMap.put(contact.getUid(), "member");
                    }

                    // --- 2. LƯU VÀO NODE "GROUPS" (Để quản lý ai đang ở trong nhóm) ---
                    groupRef.child("members").updateChildren(membersMap).addOnCompleteListener(new OnCompleteListener<Void>() {
                        @Override
                        public void onComplete(@NonNull Task<Void> innerTask) {
                            if (innerTask.isSuccessful()) {

                                // --- 3. QUAN TRỌNG: LƯU VÀO NODE "USERGROUPS" (Để hiển thị nhóm cho từng người) ---

                                // A. Lưu nhóm này vào danh sách nhóm của Admin (Chính mình)
                                RootRef.child("UserGroups").child(currentUserID).child(groupName).setValue("admin");

                                // B. Lưu nhóm này vào danh sách nhóm của từng Thành viên
                                for (Contacts contact : membersList) {
                                    RootRef.child("UserGroups").child(contact.getUid()).child(groupName).setValue("member");
                                }

                                // --------------------------------------------------------------------

                                Toast.makeText(CreateGroupActivity.this, groupName + " created successfully!", Toast.LENGTH_SHORT).show();
                                SendUserToMainActivity();
                            }
                        }
                    });

                } else {
                    String error = task.getException() != null ? task.getException().getMessage() : "Unknown Error";
                    Toast.makeText(CreateGroupActivity.this, "Error: " + error, Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private void SendUserToMainActivity() {
        Intent mainIntent = new Intent(CreateGroupActivity.this, MainActivity.class);

        // --- THÊM DÒNG NÀY ---
        // Gửi kèm dữ liệu: Key là "target_index", Value là 1
        // (Giả sử trong Adapter của bạn: 0=Chats, 1=Groups, 2=Contacts)
        mainIntent.putExtra("target_index", 1);

        mainIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(mainIntent);
        finish();
    }

}