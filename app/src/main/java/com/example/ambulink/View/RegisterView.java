package com.example.ambulink.View;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.InputFilter;
import android.text.Selection;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.example.ambulink.Controller.RegisterController;
import com.example.ambulink.R;

public class RegisterView extends AppCompatActivity {

    private EditText fullNameField, emailField, passwordField, phoneField;
    private Button registerBtn;
    private TextView backBtnReg;
    private RegisterController registerController;
    private Spinner spinnerHospital;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Get the registration type from the intent
        String registerType = getIntent().getStringExtra("LOGIN_TYPE");

        // Initialize controller with registerType
        registerController = new RegisterController(this, this, registerType);

        // Set up UI based on registration type
        if ("hospital".equals(registerType)) {
            setupHospitalRegister();
        } else if ("paramedics".equals(registerType)) {
            setupParamedicsRegister();
        } else {
            // Default to paramedics registration if registerType is null or unrecognized
            setupParamedicsRegister();
        }
    }

    // Setup for Paramedics registration
    private void setupParamedicsRegister() {
        setContentView(R.layout.register_paramedics);

        // Initialize UI elements
        initializeUIForParamedics();

        backBtnReg.setOnClickListener(v -> {
            Intent intent = new Intent(RegisterView.this, LoginView.class);
            intent.putExtra("LOGIN_TYPE", "paramedics");
            startActivity(intent);
            finish();
        });

        registerBtn.setOnClickListener(v -> {
            // Show the privacy policy dialog
            showPrivacyPolicyDialog(() -> {
                // This code will execute if the user accepts the terms and conditions
                String fullName = fullNameField.getText().toString().trim();
                String email = emailField.getText().toString().trim();
                String password = passwordField.getText().toString().trim();
                String phone = phoneField.getText().toString().trim();

                if (registerController.validateParamedicInput(fullName, email, password, phone)) {
                    registerController.checkIfParamedicExists(fullName, phone, email, password);
                    registerController.logOutUser();
                }
            });
        });

        // Specific UI adjustments for paramedics
        setupPhoneNumberInput();
    }

    // Setup for Hospital registration
    private void setupHospitalRegister() {
        setContentView(R.layout.register_hospital);

        // Initialize UI elements
        initializeUIForHospital();

        // Load hospital names and IDs from resources
        String[] hospitalNames = getResources().getStringArray(R.array.hospital_names);
        final String[] hospitalIds = getResources().getStringArray(R.array.hospital_ids);

        // Set up the Spinner with hospital names
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, hospitalNames);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerHospital.setAdapter(adapter);

        backBtnReg.setOnClickListener(v -> {
            Intent intent = new Intent(RegisterView.this, LoginView.class);
            intent.putExtra("LOGIN_TYPE", "hospital");
            startActivity(intent);
            finish();
        });

        registerBtn.setOnClickListener(v -> {
            // Show the privacy policy dialog
            showPrivacyPolicyDialog(() -> {
                // This code will execute if the user accepts the terms and conditions
                String fullName = fullNameField.getText().toString().trim();
                String email = emailField.getText().toString().trim();
                String password = passwordField.getText().toString().trim();
                String phone = phoneField.getText().toString().trim();

                // Get the selected hospital name and ID
                int selectedIndex = spinnerHospital.getSelectedItemPosition();
                String selectedHospitalId = hospitalIds[selectedIndex]; // Get the ID corresponding to the selected name

                if (registerController.validateHospitalInput(fullName, email, password, phone, selectedHospitalId)) {
                    registerController.checkIfHospitalExists(fullName, phone, email, password, selectedHospitalId);
                    registerController.logOutUser();
                }
            });
        });

        // Specific UI adjustments for hospital registration
        setupPhoneNumberInput();
    }

    private void initializeUIForHospital() {
        fullNameField = findViewById(R.id.hospitalFullName);
        emailField = findViewById(R.id.hospitalEmail);
        passwordField = findViewById(R.id.hospitalPassWord);
        phoneField = findViewById(R.id.hospitalPhone);
        registerBtn = findViewById(R.id.hospitalRegSubmit);
        backBtnReg = findViewById(R.id.backBtnReg);
        spinnerHospital = findViewById(R.id.spinnerHospital);
    }

    private void initializeUIForParamedics() {
        fullNameField = findViewById(R.id.paramFullName);
        emailField = findViewById(R.id.paramEmail);
        passwordField = findViewById(R.id.paramPassWord);
        phoneField = findViewById(R.id.paramPhone);
        registerBtn = findViewById(R.id.paramRegSubmit);
        backBtnReg = findViewById(R.id.backBtnReg);
    }

    private void setupPhoneNumberInput() {
        // Set the maximum length to 13 characters (including "+63")
        phoneField.setFilters(new InputFilter[]{new InputFilter.LengthFilter(13)});

        // Set "+63" as initial text
        phoneField.setText("+63");

        phoneField.addTextChangedListener(new TextWatcher() {
            boolean isEditing;

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                // Do nothing
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                // Do nothing
            }

            @Override
            public void afterTextChanged(Editable s) {
                if (isEditing) return;
                isEditing = true;

                // Enforce "+63" at the start
                if (!s.toString().startsWith("+63")) {
                    phoneField.setText("+63");
                }

                // Force cursor position after "+63" if it’s set anywhere else
                if (Selection.getSelectionStart(s) < 3) {
                    Selection.setSelection(phoneField.getText(), phoneField.getText().length());
                }

                // Limit to 13 characters
                if (s.length() > 13) {
                    s.delete(13, s.length());
                }

                isEditing = false;
            }
        });

        // Set focus listener to force cursor to the end after "+63"
        phoneField.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) {
                phoneField.post(() -> Selection.setSelection(phoneField.getText(), phoneField.getText().length()));
            }
        });

        // Handle click to keep cursor at the end after "+63"
        phoneField.setOnClickListener(v -> {
            if (Selection.getSelectionStart(phoneField.getText()) < 3) {
                Selection.setSelection(phoneField.getText(), phoneField.getText().length());
            }
        });
    }

    private void showPrivacyPolicyDialog(Runnable onAccepted) {
        // Inflate the dialog layout
        LayoutInflater inflater = LayoutInflater.from(this);
        View dialogView = inflater.inflate(R.layout.privacy_policy_dialog, null);

        CheckBox acceptCheckBox = dialogView.findViewById(R.id.acceptCheckBox);

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setView(dialogView);
        builder.setTitle("Privacy Policy Consent");
        builder.setPositiveButton("Accept", null);
        builder.setNegativeButton("Cancel", (dialog, which) -> {
            // User cancelled
            dialog.dismiss();
        });

        AlertDialog dialog = builder.create();
        dialog.setOnShowListener(dialogInterface -> {
            Button positiveButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
            positiveButton.setOnClickListener(view -> {
                if (acceptCheckBox.isChecked()) {
                    dialog.dismiss();
                    onAccepted.run(); // Proceed with registration
                } else {
                    Toast.makeText(RegisterView.this, "Please accept the terms and conditions.", Toast.LENGTH_SHORT).show();
                }
            });
        });
        dialog.show();
    }

    public void showToast(String message) {
        Toast.makeText(RegisterView.this, message, Toast.LENGTH_SHORT).show();
    }
}
