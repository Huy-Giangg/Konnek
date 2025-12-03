package com.example.whatsapp;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.squareup.picasso.Picasso;
import de.hdodenhof.circleimageview.CircleImageView;
import java.util.List;

public class SelectedMembersAdapter extends RecyclerView.Adapter<SelectedMembersAdapter.ViewHolder> {

    private List<Contacts> selectedList;

    public SelectedMembersAdapter(List<Contacts> selectedList) {
        this.selectedList = selectedList;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_selected_member, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Contacts user = selectedList.get(position);
        holder.name.setText(user.getName());
        holder.status.setText(user.getStatus()); // Hoặc số điện thoại nếu bạn có biến phone

        if (user.getImage() != null && !user.getImage().isEmpty()) {
            Picasso.get().load(user.getImage()).placeholder(R.drawable.profile_image).into(holder.image);
        }

        // Xử lý nút Xóa
        holder.removeBtn.setOnClickListener(v -> {
            selectedList.remove(position);
            notifyItemRemoved(position);
            notifyItemRangeChanged(position, selectedList.size());
        });
    }

    @Override
    public int getItemCount() {
        return selectedList.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        CircleImageView image;
        TextView name, status;
        ImageView removeBtn;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            image = itemView.findViewById(R.id.selected_profile_image);
            name = itemView.findViewById(R.id.selected_name);
            status = itemView.findViewById(R.id.selected_status);
            removeBtn = itemView.findViewById(R.id.btn_remove_member);
        }
    }
}