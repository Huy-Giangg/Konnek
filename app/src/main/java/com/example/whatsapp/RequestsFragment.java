package com.example.whatsapp;

import android.content.DialogInterface;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

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

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.HashMap;

import de.hdodenhof.circleimageview.CircleImageView;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link RequestsFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class RequestsFragment extends Fragment {

    // TODO: Rename parameter arguments, choose names that match
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";

    // TODO: Rename and change types of parameters
    private String mParam1;
    private String mParam2;

    private View RequestFragmentView;
    private RecyclerView myRequestlList;
    private DatabaseReference ChatRequestRef, UsersRef, ContactsRef;
    private FirebaseAuth mAuth;
    private String currentUserID;



    public RequestsFragment() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param param1 Parameter 1.
     * @param param2 Parameter 2.
     * @return A new instance of fragment RequestsFragment.
     */
    // TODO: Rename and change types and number of parameters
    public static RequestsFragment newInstance(String param1, String param2) {
        RequestsFragment fragment = new RequestsFragment();
        Bundle args = new Bundle();
        args.putString(ARG_PARAM1, param1);
        args.putString(ARG_PARAM2, param2);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            mParam1 = getArguments().getString(ARG_PARAM1);
            mParam2 = getArguments().getString(ARG_PARAM2);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        RequestFragmentView = inflater.inflate(R.layout.fragment_requests, container, false);
        myRequestlList = RequestFragmentView.findViewById(R.id.chat_request_list);
        myRequestlList.setLayoutManager(new LinearLayoutManager(getContext()));
        ChatRequestRef = FirebaseDatabase.getInstance().getReference().child("Chat Requests");
        mAuth = FirebaseAuth.getInstance();
        currentUserID = mAuth.getCurrentUser().getUid();
        UsersRef = FirebaseDatabase.getInstance().getReference().child("Users");
        ContactsRef = FirebaseDatabase.getInstance().getReference().child("Contacts");



        // Inflate the layout for this fragment
        return RequestFragmentView;
    }

    @Override
    public void onStart() {
        super.onStart();

        FirebaseRecyclerOptions<Contacts> options
                = new FirebaseRecyclerOptions.Builder<Contacts>()
                .setQuery(ChatRequestRef.child(currentUserID), Contacts.class)
                .build();

        FirebaseRecyclerAdapter<Contacts, RequestsViewHolder> adapter
                = new FirebaseRecyclerAdapter<Contacts, RequestsViewHolder>(options) {
            @Override
            protected void onBindViewHolder(@NonNull RequestsViewHolder holder, int position, @NonNull Contacts model) {
                holder.itemView.findViewById(R.id.request_accept_btn).setVisibility(View.VISIBLE);
                holder.itemView.findViewById(R.id.request_cancel_btn).setVisibility(View.VISIBLE);

                final String list_user_id = getRef(position).getKey();

                DatabaseReference getTypeRef = getRef(position).child("request_type").getRef();
                getTypeRef.addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        if(snapshot.exists()){
                            String type = snapshot.getValue().toString();

                            if(type.equals("received")){
                                UsersRef.child(list_user_id).addValueEventListener(new ValueEventListener() {
                                    @Override
                                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                                        if(snapshot.hasChild("image")){
                                            final String requestUserProfileImage = snapshot.child("image").getValue().toString();

                                            Picasso.get().load(requestUserProfileImage).into(holder.profileImage);
                                        }

                                        final String requestUserName = snapshot.child("name").getValue().toString();
                                        final String requestUserStatus = snapshot.child("status").getValue().toString();

                                        holder.userName.setText(requestUserName);
                                        holder.userStatus.setText("Wants to connect which you");


                                        holder.itemView.setOnClickListener(view -> {
                                            CharSequence[] options = {"Accept", "Cancel"};

                                            AlertDialog.Builder builder = new AlertDialog.Builder(view.getContext());
                                            builder.setTitle(requestUserName + " Chat Request");

                                            builder.setItems(options, (dialog, i) -> {

                                                if (i == 0) { // ACCEPT REQUEST
                                                    ContactsRef.child(currentUserID).child(list_user_id).child("Contact")
                                                            .setValue("Saved")
                                                            .addOnCompleteListener(task -> {
                                                                if (task.isSuccessful()) {
                                                                    ContactsRef.child(list_user_id).child(currentUserID).child("Contact")
                                                                            .setValue("Saved")
                                                                            .addOnCompleteListener(task2 -> {
                                                                                if (task2.isSuccessful()) {
                                                                                    removeChatRequest(currentUserID, list_user_id, "New Contact Saved");
                                                                                } else {
                                                                                    Toast.makeText(view.getContext(),
                                                                                            "Failed to save contact (2)", Toast.LENGTH_SHORT).show();
                                                                                }
                                                                            });
                                                                } else {
                                                                    Toast.makeText(view.getContext(),
                                                                            "Failed to save contact (1)", Toast.LENGTH_SHORT).show();
                                                                }
                                                            });
                                                }

                                                else if (i == 1) { // CANCEL REQUEST
                                                    removeChatRequest(currentUserID, list_user_id, "Contact Deleted");
                                                }

                                            });

                                            builder.show();
                                        });
                                    }

                                    @Override
                                    public void onCancelled(@NonNull DatabaseError error) {

                                    }
                                });
                            }
                            else if(type.equals("sent")){
                                Button request_sent_btn = holder.itemView.findViewById(R.id.request_accept_btn);
                                request_sent_btn.setText("Req_Sent");

                                holder.itemView.findViewById(R.id.request_cancel_btn).setVisibility(View.INVISIBLE);

                                UsersRef.child(list_user_id).addValueEventListener(new ValueEventListener() {
                                    @Override
                                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                                        if(snapshot.hasChild("image")){
                                            final String requestUserProfileImage = snapshot.child("image").getValue().toString();

                                            Picasso.get().load(requestUserProfileImage).into(holder.profileImage);
                                        }

                                        final String requestUserName = snapshot.child("name").getValue().toString();
                                        final String requestUserStatus = snapshot.child("status").getValue().toString();

                                        holder.userName.setText(requestUserName);
                                        holder.userStatus.setText("You have sent a request to " + requestUserName);


                                        holder.itemView.setOnClickListener(view -> {
                                            CharSequence[] options = {"Cancel chat Request"};

                                            AlertDialog.Builder builder = new AlertDialog.Builder(view.getContext());
                                            builder.setTitle("Already Sent Request");

                                            builder.setItems(options, (dialog, i) -> {

                                                if (i == 0) { // CANCEL REQUEST
                                                    removeChatRequest(currentUserID, list_user_id, "You have canceled the friend request");
                                                }
                                            });

                                            builder.show();
                                        });
                                    }

                                    @Override
                                    public void onCancelled(@NonNull DatabaseError error) {

                                    }
                                });
                            }

                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {

                    }
                });

            }

            @NonNull
            @Override
            public RequestsViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
                View view = LayoutInflater.from(parent.getContext())
                        .inflate(R.layout.users_display_layout, parent, false);
                RequestsViewHolder holder = new RequestsViewHolder(view);
                return holder;
            }
        };
        myRequestlList.setAdapter(adapter);
        adapter.startListening();
    }

    public static class RequestsViewHolder extends RecyclerView.ViewHolder {

        TextView userName, userStatus;
        CircleImageView profileImage;
        Button acceptBtn, cancelBtn;

        public RequestsViewHolder(@NonNull View itemView) {
            super(itemView);

            userName = itemView.findViewById(R.id.user_profile_name);
            userStatus = itemView.findViewById(R.id.user_status);
            profileImage = itemView.findViewById(R.id.users_profile_image);
            acceptBtn = itemView.findViewById(R.id.request_accept_btn);
            cancelBtn = itemView.findViewById(R.id.request_cancel_btn);
        }
    }

    private void removeChatRequest(String currentUserID, String list_user_id, String message) {
        ChatRequestRef.child(currentUserID).child(list_user_id)
                .removeValue()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        ChatRequestRef.child(list_user_id).child(currentUserID)
                                .removeValue()
                                .addOnCompleteListener(task2 -> {
                                    if (task2.isSuccessful()) {
                                        Toast.makeText(getContext(), message, Toast.LENGTH_SHORT).show();
                                    } else {
                                        Toast.makeText(getContext(),
                                                "Failed to remove request (2)", Toast.LENGTH_SHORT).show();
                                    }
                                });
                    } else {
                        Toast.makeText(getContext(),
                                "Failed to remove request (1)", Toast.LENGTH_SHORT).show();
                    }
                });
    }

}