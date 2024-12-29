package com.example.ambulink.View;

import android.content.Intent;
import android.os.Bundle;
import android.widget.*;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import com.example.ambulink.Controller.LoginController;
import com.example.ambulink.MainActivity;
import com.example.ambulink.R;

public class LoginView extends AppCompatActivity {

    private EditText emailField, passwordField;
    private LoginController loginController;
    private String loginType;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Initialize controller
        loginController = new LoginController(this);

        // Get the login type from the intent
        loginType = getIntent().getStringExtra("LOGIN_TYPE");

        // Set up UI based on login type
        if ("hospital".equals(loginType)) {
            setupHospitalLogin();
        } else if ("paramedics".equals(loginType)) {
            setupParamedicsLogin();
        }
    }

    public String getLoginType() {
        return loginType;
    }

    // Setup for Hospital login
    private void setupHospitalLogin() {
        setContentView(R.layout.hospital_login);
        emailField = findViewById(R.id.hospitalEmail);
        passwordField = findViewById(R.id.hospitalPassword);
        Button hospitalLoginBtn = findViewById(R.id.hospitalLoginBtn);
        TextView backBtn = findViewById(R.id.backBtn);
        Button hospitalRegBtn = findViewById(R.id.hospitalRegBtn);
        TextView forgotPasswd = findViewById(R.id.forgotPasswd);

        // Navigate to main activity on back button click
        backBtn.setOnClickListener(view -> navigateToMainActivity());

        // Navigate to forgot password
        forgotPasswd.setOnClickListener(view -> navigateToForgotPassword());

        // Handle login action for hospital staff
        hospitalLoginBtn.setOnClickListener(view -> loginController.handleLogin(emailField, passwordField, loginType));

        // Navigate to hospital registration
        hospitalRegBtn.setOnClickListener(view -> navigateToRegister("hospital"));
    }

    // Setup for Paramedics login
    private void setupParamedicsLogin() {
        setContentView(R.layout.paramedics_login);
        emailField = findViewById(R.id.paramedics_email);
        passwordField = findViewById(R.id.paramedics_password);
        Button paramLoginBtn = findViewById(R.id.paramLoginBtn);
        Button paramRegBtn = findViewById(R.id.paramRegBtn);
        TextView backBtn = findViewById(R.id.backBtn);
        TextView forgotPasswd = findViewById(R.id.forgotPasswd);

        // Handle login action for paramedics
        paramLoginBtn.setOnClickListener(view -> loginController.handleLogin(emailField, passwordField, loginType));

        // Navigate to paramedics registration
        paramRegBtn.setOnClickListener(view -> navigateToRegister("paramedics"));

        // Navigate to forgot password
        forgotPasswd.setOnClickListener(view -> navigateToForgotPassword());

        // Navigate back to main activity
        backBtn.setOnClickListener(view -> navigateToMainActivity());
    }

    // Navigation to MainActivity
    public void navigateToMainActivity() {
        Intent intent = new Intent(LoginView.this, MainActivity.class);
        startActivity(intent);
        finish();
    }

    // Navigation to specific RegisterView based on role
    public void navigateToRegister(String userType) {
        Intent intent = new Intent(LoginView.this, RegisterView.class);
        intent.putExtra("LOGIN_TYPE", userType);
        startActivity(intent);
        finish();
    }

    // Navigation to ForgotPasswordView
    public void navigateToForgotPassword() {
        Intent intent = new Intent(LoginView.this, ForgotPasswordView.class);
        startActivity(intent);
        finish();
    }

    // Navigation to Paramedics (PatientFormView)
    public void navigateToParamedics() {
        Intent intent = new Intent(LoginView.this, PatientFormView.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    // Navigation to HospitalView with hospitalId
    public void navigateToHospital(String hospitalId) {
        Intent intent = new Intent(LoginView.this, HospitalView.class);
        intent.putExtra("hospitalId", hospitalId);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    // Show a Toast message
    public void showToast(String message) {
        Toast.makeText(LoginView.this, message, Toast.LENGTH_SHORT).show();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (loginController != null) {
            loginController.cleanUp();
        }
    }
}
