package com.example.whatsapp;

import android.app.ProgressDialog;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;

public class ForgotPasswordActivity extends AppCompatActivity {
    private EditText inputEmailPhone;
    private Button btnSendReset;
    private TextView tvBackToLogin;
    private FirebaseAuth mAuth;
    private ProgressDialog loadingBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_forgot_password);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        mAuth = FirebaseAuth.getInstance();
        loadingBar = new ProgressDialog(this);

        // Liên kết các View với ID từ XML
        inputEmailPhone = findViewById(R.id.forget_email_phone);
        btnSendReset = findViewById(R.id.forget_send_reset);
        tvBackToLogin = findViewById(R.id.tv_back_to_login);

        // Đặt sự kiện click
        btnSendReset.setOnClickListener(v -> sendResetLink());
        tvBackToLogin.setOnClickListener(v -> finish()); // Quay lại màn hình Login
    }

    private void sendResetLink() {
        String email = inputEmailPhone.getText().toString().trim();

        // 1. Kiểm tra Email rỗng
        if (TextUtils.isEmpty(email)) {
            Toast.makeText(this, "Vui lòng nhập Email hoặc Số điện thoại.", Toast.LENGTH_SHORT).show();
            return;
        }

        // 2. Hiện Progress Dialog
        loadingBar.setTitle("Đang gửi yêu cầu");
        loadingBar.setMessage("Vui lòng đợi, chúng tôi đang gửi link reset mật khẩu...");
        loadingBar.setCanceledOnTouchOutside(false);
        loadingBar.show();

        // 3. Gọi hàm Firebase để gửi email reset
        mAuth.sendPasswordResetEmail(email)
                .addOnCompleteListener(new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        loadingBar.dismiss();
                        if (task.isSuccessful()) {
                            // Thành công: Thông báo và quay lại màn hình Login
                            Toast.makeText(ForgotPasswordActivity.this, "Link reset đã được gửi đến email của bạn. Vui lòng kiểm tra hộp thư!", Toast.LENGTH_LONG).show();
                            finish();
                        } else {
                            // Thất bại: Hiển thị lỗi từ Firebase (ví dụ: email không tồn tại)
                            String message = task.getException().getMessage();
                            Toast.makeText(ForgotPasswordActivity.this, "Lỗi: " + message, Toast.LENGTH_LONG).show();
                        }
                    }
                });
    }
}