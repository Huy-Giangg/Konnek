package com.example.whatsapp;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.squareup.picasso.Picasso;
import de.hdodenhof.circleimageview.CircleImageView;
import java.util.ArrayList;
import java.util.List;

public class MemberAdapter extends RecyclerView.Adapter<MemberAdapter.MemberViewHolder> {

    private List<Contacts> originalList; // Danh sách gốc (Full)
    private List<Contacts> displayList;  // Danh sách đang hiển thị (Filter)
    private List<Contacts> selectedUsers = new ArrayList<>(); // Danh sách đã tích chọn

    // 1. Sửa Constructor
    public MemberAdapter(List<Contacts> userList) {
        this.originalList = userList;
        this.displayList = new ArrayList<>(userList); // Ban đầu hiển thị hết
    }

    // 2. Thêm hàm Lọc (Filter)
    public void filter(String text) {
        displayList.clear();
        if (text.isEmpty()) {
            displayList.addAll(originalList);
        } else {
            text = text.toLowerCase();
            for (Contacts item : originalList) {
                // Tìm theo tên
                if (item.getName().toLowerCase().contains(text)) {
                    displayList.add(item);
                }
            }
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public MemberViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_contact_checkbox, parent, false);
        return new MemberViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull MemberViewHolder holder, int position) {
        // 3. Quan trọng: Lấy dữ liệu từ displayList
        Contacts user = displayList.get(position);

        holder.userName.setText(user.getName());
        holder.userStatus.setText(user.getStatus());

        if (user.getImage() != null && !user.getImage().isEmpty()) {
            Picasso.get().load(user.getImage()).placeholder(R.drawable.profile_image).into(holder.profileImage);
        }

        // Xử lý Checkbox (Logic cũ)
        holder.checkBox.setOnCheckedChangeListener(null);
        holder.checkBox.setChecked(selectedUsers.contains(user)); // Kiểm tra xem user này có trong danh sách chọn chưa

        holder.checkBox.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                if (!selectedUsers.contains(user)) selectedUsers.add(user);
            } else {
                selectedUsers.remove(user);
            }
        });

        holder.itemView.setOnClickListener(v -> {
            holder.checkBox.setChecked(!holder.checkBox.isChecked());
        });
    }

    @Override
    public int getItemCount() {
        // 4. Trả về số lượng của list đang hiển thị
        return displayList.size();
    }

    public List<Contacts> getSelectedUsers() {
        return selectedUsers;
    }

    public static class MemberViewHolder extends RecyclerView.ViewHolder {
        // ... (Giữ nguyên ViewHolder cũ)
        TextView userName, userStatus;
        CircleImageView profileImage;
        CheckBox checkBox;

        public MemberViewHolder(@NonNull View itemView) {
            super(itemView);
            userName = itemView.findViewById(R.id.user_profile_name);
            userStatus = itemView.findViewById(R.id.user_profile_status);
            profileImage = itemView.findViewById(R.id.users_profile_image);
            checkBox = itemView.findViewById(R.id.user_checkbox);
        }
    }

    public void updateList(List<Contacts> newList) {
        this.originalList = new ArrayList<>(newList); // Cập nhật list gốc
        this.displayList = new ArrayList<>(newList);  // Cập nhật list hiển thị (reset filter)
        notifyDataSetChanged();
    }
}