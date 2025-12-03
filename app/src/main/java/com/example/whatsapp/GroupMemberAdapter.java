package com.example.whatsapp;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.squareup.picasso.Picasso;
import java.util.ArrayList;
import java.util.List;
import de.hdodenhof.circleimageview.CircleImageView;

// Giả sử bạn đã có class Contacts (model chứa name, image, uid)
public class GroupMemberAdapter extends RecyclerView.Adapter<GroupMemberAdapter.MemberViewHolder> {

    private List<Contacts> userList;
    // Danh sách chứa các ID đã được chọn
    public List<String> selectedUserIds = new ArrayList<>();

    public GroupMemberAdapter(List<Contacts> userList) {
        this.userList = userList;
    }

    @NonNull
    @Override
    public MemberViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_contact_checkbox, parent, false);
        return new MemberViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull MemberViewHolder holder, int position) {
        Contacts user = userList.get(position);

        holder.userName.setText(user.getName());
        // Nếu bạn có ảnh
        if (user.getImage() != null && !user.getImage().isEmpty()) {
            Picasso.get().load(user.getImage()).placeholder(R.drawable.profile_image).into(holder.profileImage);
        }

        // Xử lý sự kiện Checkbox
        // Reset listener để tránh lỗi khi cuộn
        holder.checkBox.setOnCheckedChangeListener(null);

        // Kiểm tra xem user này đã có trong danh sách chọn chưa để set trạng thái
//        holder.checkBox.setChecked(selectedUserIds.contains(user.getUid()));
//
//        // Logic khi bấm Checkbox
//        holder.checkBox.setOnCheckedChangeListener((buttonView, isChecked) -> {
//            if (isChecked) {
//                selectedUserIds.add(user.getUid());
//            } else {
//                selectedUserIds.remove(user.getUid());
//            }
//        });

        // Cho phép bấm vào cả dòng để chọn
        holder.itemView.setOnClickListener(v -> {
            holder.checkBox.setChecked(!holder.checkBox.isChecked());
        });
    }

    @Override
    public int getItemCount() {
        return userList.size();
    }

    // Hàm để Activity lấy danh sách đã chọn
    public List<String> getSelectedMembers() {
        return selectedUserIds;
    }

    public static class MemberViewHolder extends RecyclerView.ViewHolder {
        TextView userName;
        CircleImageView profileImage;
        CheckBox checkBox;

        public MemberViewHolder(@NonNull View itemView) {
            super(itemView);
            userName = itemView.findViewById(R.id.user_profile_name);
            profileImage = itemView.findViewById(R.id.users_profile_image);
            checkBox = itemView.findViewById(R.id.user_checkbox);
        }
    }
}