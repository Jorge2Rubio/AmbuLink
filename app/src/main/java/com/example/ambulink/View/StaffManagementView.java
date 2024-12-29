package com.example.ambulink.View;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.example.ambulink.Controller.StaffManagementController;
import com.example.ambulink.R;

public class StaffManagementView extends AppCompatActivity {
    private TextView tvCurrentDoctors, tvCurrentNurses, tvMaxSlots, backBtn;
    private EditText etNumDoctors, etMaxDoctors, etNumNurses, etMaxNurses, etMaxSlots;
    private Button btnSaveChanges;
    private Button btnIncreaseDoctors, btnDecreaseDoctors, btnIncreaseMaxDoctors, btnDecreaseMaxDoctors;
    private Button btnIncreaseNurses, btnDecreaseNurses, btnIncreaseMaxNurses, btnDecreaseMaxNurses;
    private Button btnIncreaseSlots, btnDecreaseSlots;
    private StaffManagementController staffManagementController;
    private String hospitalId;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_staff_management);

        // Initialize views
        tvCurrentDoctors = findViewById(R.id.tvCurrentDoctors);
        tvCurrentNurses = findViewById(R.id.tvCurrentNurses);
        tvMaxSlots = findViewById(R.id.tvMaxSlots);
        etNumDoctors = findViewById(R.id.etNumDoctors);
        etMaxDoctors = findViewById(R.id.etMaxDoctors);
        etNumNurses = findViewById(R.id.etNumNurses);
        etMaxNurses = findViewById(R.id.etMaxNurses);
        etMaxSlots = findViewById(R.id.etMaxSlots);
        btnSaveChanges = findViewById(R.id.btnSaveChanges);
        backBtn = findViewById(R.id.backBtn);

        // Initialize buttons
        btnIncreaseDoctors = findViewById(R.id.btnIncreaseDoctors);
        btnDecreaseDoctors = findViewById(R.id.btnDecreaseDoctors);
        btnIncreaseMaxDoctors = findViewById(R.id.btnIncreaseMaxDoctors);
        btnDecreaseMaxDoctors = findViewById(R.id.btnDecreaseMaxDoctors);

        btnIncreaseNurses = findViewById(R.id.btnIncreaseNurses);
        btnDecreaseNurses = findViewById(R.id.btnDecreaseNurses);
        btnIncreaseMaxNurses = findViewById(R.id.btnIncreaseMaxNurses);
        btnDecreaseMaxNurses = findViewById(R.id.btnDecreaseMaxNurses);

        btnIncreaseSlots = findViewById(R.id.btnIncreaseSlots);
        btnDecreaseSlots = findViewById(R.id.btnDecreaseSlots);

        // Initialize controller
        staffManagementController = new StaffManagementController(this);

        // Get the hospital ID from the intent
        hospitalId = getIntent().getStringExtra("hospitalId");

        // Load existing data
        assert hospitalId != null;
        staffManagementController.loadHospitalData(hospitalId);

        backBtn.setOnClickListener(v -> {
            Intent intent = new Intent(this, HospitalView.class);
            intent.putExtra("hospitalId", hospitalId);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        });

        // Save changes when the button is clicked
        btnSaveChanges.setOnClickListener(v -> {
            String numDoctors = etNumDoctors.getText().toString();
            String maxDoctors = etMaxDoctors.getText().toString();
            String numNurses = etNumNurses.getText().toString();
            String maxNurses = etMaxNurses.getText().toString();
            String maxSlots = etMaxSlots.getText().toString();

            if (numDoctors.isEmpty() || maxDoctors.isEmpty() || numNurses.isEmpty() || maxNurses.isEmpty() || maxSlots.isEmpty()) {
                Toast.makeText(this, "Please fill in all fields", Toast.LENGTH_SHORT).show();
                return;
            }

            int currentNumDoctors = Integer.parseInt(numDoctors);
            int maximumDoctors = Integer.parseInt(maxDoctors);
            int currentNumNurses = Integer.parseInt(numNurses);
            int maximumNurses = Integer.parseInt(maxNurses);
            int maximumSlots = Integer.parseInt(maxSlots);

            staffManagementController.updateHospitalData(
                    hospitalId, currentNumDoctors, maximumDoctors, currentNumNurses, maximumNurses, maximumSlots);
        });

        // Add button listeners
        setupButtonListeners();
    }

    private void setupButtonListeners() {
        // Doctors
        btnIncreaseDoctors.setOnClickListener(v -> adjustValue(etNumDoctors, 1));
        btnDecreaseDoctors.setOnClickListener(v -> adjustValue(etNumDoctors, -1));
        btnIncreaseMaxDoctors.setOnClickListener(v -> adjustValue(etMaxDoctors, 1));
        btnDecreaseMaxDoctors.setOnClickListener(v -> adjustValue(etMaxDoctors, -1));

        // Nurses
        btnIncreaseNurses.setOnClickListener(v -> adjustValue(etNumNurses, 1));
        btnDecreaseNurses.setOnClickListener(v -> adjustValue(etNumNurses, -1));
        btnIncreaseMaxNurses.setOnClickListener(v -> adjustValue(etMaxNurses, 1));
        btnDecreaseMaxNurses.setOnClickListener(v -> adjustValue(etMaxNurses, -1));

        // Slots
        btnIncreaseSlots.setOnClickListener(v -> adjustValue(etMaxSlots, 1));
        btnDecreaseSlots.setOnClickListener(v -> adjustValue(etMaxSlots, -1));
    }

    private void adjustValue(EditText editText, int delta) {
        String valueStr = editText.getText().toString();
        int value = valueStr.isEmpty() ? 0 : Integer.parseInt(valueStr);
        value = Math.max(0, value + delta); // Ensure value doesn't go below 0
        editText.setText(String.valueOf(value));
    }

    public void updateFields(int numDoctors, int maxDoctors, int numNurses, int maxNurses, int maxSlots) {
        tvCurrentDoctors.setText("Available Doctors: " + numDoctors);
        tvCurrentNurses.setText("Available Nurses: " + numNurses);
        tvMaxSlots.setText("Maximum Slots: " + maxSlots);
        etNumDoctors.setText(String.valueOf(numDoctors));
        etMaxDoctors.setText(String.valueOf(maxDoctors));
        etNumNurses.setText(String.valueOf(numNurses));
        etMaxNurses.setText(String.valueOf(maxNurses));
        etMaxSlots.setText(String.valueOf(maxSlots));
    }
}
