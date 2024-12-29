package com.example.ambulink.View;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.example.ambulink.Controller.ForgotPasswordController;
import com.example.ambulink.R;
import com.example.ambulink.View.LoginView;

public class ForgotPasswordView extends AppCompatActivity {

    private EditText userEmail;
    private Button resetBtn;
    private ForgotPasswordController forgotPasswordController;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.forgotpassword);

        // Initialize controller
        forgotPasswordController = new ForgotPasswordController(this);

        initializeUI();

        resetBtn.setOnClickListener(view -> {
            String emailAddress = userEmail.getText().toString().trim();
            forgotPasswordController.sendPasswordResetEmail(emailAddress);
        });
    }

    private void initializeUI() {
        userEmail = findViewById(R.id.userEmail);
        resetBtn = findViewById(R.id.resetBtn);
    }

    public void showSuccessMessage() {
        Toast.makeText(ForgotPasswordView.this, "Email Sent: Please Check Your Email", Toast.LENGTH_SHORT).show();
        navigateToLogin();
    }

    public void showErrorMessage(String errorMessage) {
        Toast.makeText(ForgotPasswordView.this, "Error: " + errorMessage, Toast.LENGTH_SHORT).show();
    }

    public void navigateToLogin() {
        Intent intent = new Intent(ForgotPasswordView.this, LoginView.class);
        intent.putExtra("LOGIN_TYPE", "paramedics");
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}
