package com.example.whatsapp;

import android.app.ProgressDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.canhub.cropper.CropImageContract;
import com.canhub.cropper.CropImageContractOptions;
import com.canhub.cropper.CropImageOptions;
import com.canhub.cropper.CropImageView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.*;
import com.squareup.picasso.Picasso;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;

import de.hdodenhof.circleimageview.CircleImageView;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.FormBody;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class SettingsActivity extends AppCompatActivity {

    private Button UpdateAccountSettings;
    private EditText userName, userStatus;
    private CircleImageView userProfileImage;
    private String currentUserID;
    private FirebaseAuth mAuth;
    private DatabaseReference RootRef;
    private ProgressDialog loadingBar;
    private String imageUrl = "";

    private ActivityResultLauncher<String> galleryLauncher;
    private ActivityResultLauncher<CropImageContractOptions> cropImageLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_settings);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        mAuth = FirebaseAuth.getInstance();
        currentUserID = mAuth.getCurrentUser().getUid();
        RootRef = FirebaseDatabase.getInstance().getReference();
        loadingBar = new ProgressDialog(this);

        InitializeFields();
        userName.setVisibility(View.VISIBLE);

        UpdateAccountSettings.setOnClickListener(v -> UpdateSettings());
        RetrieveUserInfo();

        // ✅ Cài crop ảnh
        cropImageLauncher = registerForActivityResult(new CropImageContract(), result -> {
            if (result.isSuccessful()) {
                Uri resultUri = result.getUriContent();
                if (resultUri != null) {
                    loadingBar.setTitle("Uploading...");
                    loadingBar.setMessage("Please wait, uploading your profile image...");
                    loadingBar.setCanceledOnTouchOutside(false);
                    loadingBar.show();
                    uploadImageToCloudinary(resultUri);
                }
            } else {
                Exception error = result.getError();
                Toast.makeText(SettingsActivity.this,
                        "Crop error: " + error, Toast.LENGTH_SHORT).show();
            }
        });

        // ✅ Mở thư viện ảnh
        galleryLauncher = registerForActivityResult(
                new ActivityResultContracts.GetContent(),
                uri -> {
                    if (uri != null) {
                        CropImageOptions options = new CropImageOptions();
                        options.guidelines = CropImageView.Guidelines.ON;
                        options.aspectRatioX = 1;
                        options.aspectRatioY = 1;
                        options.fixAspectRatio = true;
                        cropImageLauncher.launch(new CropImageContractOptions(uri, options));
                    }
                }
        );

        userProfileImage.setOnClickListener(v -> galleryLauncher.launch("image/*"));
    }

    private void uploadImageToCloudinary(Uri imageUri) {
        loadingBar.setTitle("Uploading...");
        loadingBar.setMessage("Please wait while we upload your profile picture...");
        loadingBar.setCanceledOnTouchOutside(false);
        loadingBar.show();

        OkHttpClient client = new OkHttpClient();

        try {
            // Đọc dữ liệu ảnh thành byte[]
            InputStream inputStream = getContentResolver().openInputStream(imageUri);
            ByteArrayOutputStream byteBuffer = new ByteArrayOutputStream();
            int bufferSize = 1024;
            byte[] buffer = new byte[bufferSize];
            int len;
            while ((len = inputStream.read(buffer)) != -1) {
                byteBuffer.write(buffer, 0, len);
            }
            byte[] imageBytes = byteBuffer.toByteArray();
            inputStream.close();

            // ⚙️ Thay bằng Cloudinary info của bạn
            String CLOUD_NAME = "dxnblcmbg";
            String UPLOAD_PRESET = "WhatsApp";

            // Gửi request lên Cloudinary
            RequestBody requestBody = new MultipartBody.Builder()
                    .setType(MultipartBody.FORM)
                    .addFormDataPart("file", "profile.jpg",
                            RequestBody.create(MediaType.parse("image/*"), imageBytes))
                    .addFormDataPart("upload_preset", UPLOAD_PRESET)
                    .build();

            Request request = new Request.Builder()
                    .url("https://api.cloudinary.com/v1_1/" + CLOUD_NAME + "/image/upload")
                    .post(requestBody)
                    .build();

            client.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(@NonNull Call call, @NonNull IOException e) {
                    runOnUiThread(() -> {
                        loadingBar.dismiss();
                        Toast.makeText(SettingsActivity.this, "Upload failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    });
                }

                @Override
                public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                    if (response.isSuccessful()) {
                        try {
                            String responseData = response.body().string();
                            JSONObject json = new JSONObject(responseData);
                            imageUrl = json.getString("secure_url"); // ✅ link ảnh

                            // Lưu URL vào Firebase
                            RootRef.child("Users").child(currentUserID).child("image")
                                    .setValue(imageUrl)
                                    .addOnCompleteListener(task -> runOnUiThread(() -> {
                                        loadingBar.dismiss();
                                        if (task.isSuccessful()) {
                                            Picasso.get()
                                                    .load(imageUrl)
                                                    .placeholder(R.drawable.profile_image)
                                                    .into(userProfileImage);
                                            Toast.makeText(SettingsActivity.this, "Profile image updated!", Toast.LENGTH_SHORT).show();
                                        } else {
                                            Toast.makeText(SettingsActivity.this, "Failed to save link in Firebase!", Toast.LENGTH_SHORT).show();
                                        }
                                    }));
                        } catch (Exception e) {
                            runOnUiThread(() -> {
                                loadingBar.dismiss();
                                Toast.makeText(SettingsActivity.this, "Error parsing response!", Toast.LENGTH_SHORT).show();
                            });
                        }
                    } else {
                        runOnUiThread(() -> {
                            loadingBar.dismiss();
                            Toast.makeText(SettingsActivity.this, "Upload failed: " + response.message(), Toast.LENGTH_SHORT).show();
                        });
                    }
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
            loadingBar.dismiss();
            Toast.makeText(this, "Error reading image: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }



    private void RetrieveUserInfo() {
        RootRef.child("Users").child(currentUserID)
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        if (snapshot.exists()) {
                            if (snapshot.hasChild("name")) {
                                userName.setText(snapshot.child("name").getValue(String.class));
                            }
                            if (snapshot.hasChild("status")) {
                                userStatus.setText(snapshot.child("status").getValue(String.class));
                            }
                            if (snapshot.hasChild("image")) {
                                String img = snapshot.child("image").getValue(String.class);
                                Picasso.get()
                                        .load(img)
                                        .placeholder(R.drawable.profile_image)
                                        .into(userProfileImage);
                            }
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Toast.makeText(SettingsActivity.this, "Lỗi khi tải dữ liệu người dùng", Toast.LENGTH_SHORT).show();
                    }
                });
    }


    private void InitializeFields() {
        UpdateAccountSettings = findViewById(R.id.update_settings_button);
        userName = findViewById(R.id.set_user_name);
        userStatus = findViewById(R.id.set_profile_status);
        userProfileImage = findViewById(R.id.set_profile_image);
    }

    private void UpdateSettings() {
        String setUserName = userName.getText().toString().trim();
        String setStatus = userStatus.getText().toString().trim();


        if (TextUtils.isEmpty(setUserName) || TextUtils.isEmpty(setStatus)) {
            Toast.makeText(this, "Please fill in all fields", Toast.LENGTH_SHORT).show();
            return;
        }

        // Tạo map để cập nhật thông tin
        HashMap<String, Object> profileMap = new HashMap<>();
        profileMap.put("uid", currentUserID);
        profileMap.put("name", setUserName);
        profileMap.put("status", setStatus);

        // Nếu có link ảnh (được upload từ Cloudinary), thêm vào
        if (imageUrl != null && !imageUrl.isEmpty()) {
            profileMap.put("image", imageUrl);
        }

        // Chỉ cập nhật các trường có trong map thay vì ghi đè toàn bộ node
        RootRef.child("Users").child(currentUserID)
                .updateChildren(profileMap)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        Toast.makeText(SettingsActivity.this, "Profile Updated Successfully...", Toast.LENGTH_SHORT).show();
                        SendUserToMainActivity();
                    } else {
                        Toast.makeText(SettingsActivity.this, "Error: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }


    private void SendUserToMainActivity() {
        Intent mainIntent = new Intent(SettingsActivity.this, MainActivity.class);
        mainIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(mainIntent);
        finish();
    }
}
