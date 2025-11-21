package com.example.whatsapp;

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

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link ContactsFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class ContactsFragment extends Fragment {

    private View contactView;
    private RecyclerView myContactsList;
    private DatabaseReference contactsRef, usersRef;
    private FirebaseAuth mAuth;
    private String currentUserID;

    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";

    private String mParam1;
    private String mParam2;

    public ContactsFragment() {
        // Required empty public constructor
    }

    public static ContactsFragment newInstance(String param1, String param2) {
        ContactsFragment fragment = new ContactsFragment();
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

        contactView = inflater.inflate(R.layout.fragment_contacts, container, false);

        myContactsList = contactView.findViewById(R.id.contacts_list);
        myContactsList.setLayoutManager(new LinearLayoutManager(getContext()));

        mAuth = FirebaseAuth.getInstance();
        if (mAuth.getCurrentUser() != null) {
            currentUserID = mAuth.getCurrentUser().getUid();
            contactsRef = FirebaseDatabase.getInstance().getReference()
                    .child("Contacts").child(currentUserID);
        } else {
            currentUserID = null;
            contactsRef = FirebaseDatabase.getInstance().getReference().child("Contacts");
        }

        usersRef = FirebaseDatabase.getInstance().getReference().child("Users");

        return contactView;
    }

    @Override
    public void onStart() {
        super.onStart();

        FirebaseRecyclerOptions<Contacts> options =
                new FirebaseRecyclerOptions.Builder<Contacts>()
                        .setQuery(contactsRef, Contacts.class)
                        .build();

        FirebaseRecyclerAdapter<Contacts, ContactViewHolder> adapter =
                new FirebaseRecyclerAdapter<Contacts, ContactViewHolder>(options) {
                    @Override
                    protected void onBindViewHolder(@NonNull final ContactViewHolder holder,
                                                    int position, @NonNull Contacts model) {

                        final String userIDs = getRef(position).getKey();

                        usersRef.child(userIDs).addValueEventListener(new ValueEventListener() {
                            @Override
                            public void onDataChange(@NonNull DataSnapshot snapshot) {
                                if (snapshot.exists()) {
                                    if(snapshot.child("userState").hasChild("state")){
                                        String date = snapshot.child("userState").child("date").getValue().toString();
                                        String time = snapshot.child("userState").child("time").getValue().toString();
                                        String state = snapshot.child("userState").child("state").getValue().toString();

                                        if(state.equals("online")){
                                            holder.onlineIcon.setVisibility(View.VISIBLE);
                                        }else if(state.equals("offline")){
                                            holder.onlineIcon.setVisibility(View.INVISIBLE);
                                        }
                                    }else{
                                        holder.onlineIcon.setVisibility(View.INVISIBLE);
                                    }

                                    // Lấy thông tin người dùng
                                    if (snapshot.hasChild("image")) {
                                        String profileImage = snapshot.child("image")
                                                .getValue(String.class);
                                        String profileName = snapshot.child("name")
                                                .getValue(String.class);
                                        String profileStatus = snapshot.child("status")
                                                .getValue(String.class);

                                        holder.userName.setText(profileName);
                                        holder.userStatus.setText(profileStatus);
                                        Picasso.get()
                                                .load(profileImage)
                                                .placeholder(R.drawable.profile_image)
                                                .into(holder.profileImage);
                                    } else {
                                        String profileName = snapshot.child("name")
                                                .getValue(String.class);
                                        String profileStatus = snapshot.child("status")
                                                .getValue(String.class);

                                        holder.userName.setText(profileName);
                                        holder.userStatus.setText(profileStatus);
                                    }
                                }
                            }

                            @Override
                            public void onCancelled(@NonNull DatabaseError error) {
                                // Không cần xử lý
                            }
                        });
                    }

                    @NonNull
                    @Override
                    public ContactViewHolder onCreateViewHolder(@NonNull ViewGroup parent,
                                                                int viewType) {
                        View view = LayoutInflater.from(parent.getContext())
                                .inflate(R.layout.users_display_layout, parent, false);
                        return new ContactViewHolder(view);
                    }
                };

        myContactsList.setAdapter(adapter);
        adapter.startListening();
    }

    public static class ContactViewHolder extends RecyclerView.ViewHolder {
        TextView userName, userStatus;
        CircleImageView profileImage;
        ImageView onlineIcon;

        public ContactViewHolder(@NonNull View itemView) {
            super(itemView);
            userName = itemView.findViewById(R.id.user_profile_name);
            userStatus = itemView.findViewById(R.id.user_status);
            profileImage = itemView.findViewById(R.id.users_profile_image);
            onlineIcon = itemView.findViewById(R.id.user_online_status);
        }
    }
}
