package com.example.ambulink.View;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.ambulink.Model.PatientModel;
import com.example.ambulink.R;
import com.example.ambulink.Controller.PatientDetailController;

import java.util.List;

public class PatientDetailActivityView extends AppCompatActivity {

    private TextView firstNameTextView, lastNameTextView, ageTextView,
            sexTextView, complaintTextView, setReligionTextView, setSignsAndSymptoms,
            setNotes, setAllergies, setMedications, setPastMedicalHistory, setLastOralIntake,
            setEventsLeadingToPresentIllness, setOxygenSaturation, setRespiratoryRate, setHeartRate,
            setBodyTemperature, setBloodPressure, backBtn, setRejectionReason, rejection_reason_holder;
    private Button patientAccept, patientReject;
    private String hospitalId;

    private PatientDetailController patientDetailController;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_patient_detail);

        // Initialize views
        initializeViews();

        // Initialize controller
        patientDetailController = new PatientDetailController();

        // Retrieve passed patient data and hospital ID from intent
        PatientModel patient = getIntent().getParcelableExtra("patientData");
        hospitalId = getIntent().getStringExtra("hospitalId");

        // Display patient data
        if (patient != null) {
            displayPatientData(patient);
        }

        // Handle back button click
        backBtn.setOnClickListener(view -> finish());

        // Handle patient rejection
        patientReject.setOnClickListener(view -> {
            if (patient != null) {
                showRejectionReasonDialog(patient.getPatientId());
            }
        });

        // Handle patient acceptance
        patientAccept.setOnClickListener(view -> {
            if (patient != null) {
                updatePatientStatus(patient.getPatientId(), true, null); // No rejection reason needed for acceptance
            }
        });
    }

    private void initializeViews() {
        firstNameTextView = findViewById(R.id.patient_first_name_detail);
        lastNameTextView = findViewById(R.id.patient_last_name_detail);
        ageTextView = findViewById(R.id.patient_age_detail);
        sexTextView = findViewById(R.id.patient_sex_detail);
        setReligionTextView = findViewById(R.id.patient_religion_detail);
        complaintTextView = findViewById(R.id.patient_complaint_detail);
        setNotes = findViewById(R.id.patient_Notes_detail);
        setSignsAndSymptoms = findViewById(R.id.patient_signsAndSymptoms_detail);
        setAllergies = findViewById(R.id.patient_allergies_detail);
        setMedications = findViewById(R.id.patient_medications_detail);
        setPastMedicalHistory = findViewById(R.id.patient_pastMedicalHistory_detail);
        setLastOralIntake = findViewById(R.id.patient_lastOralIntake_detail);
        setEventsLeadingToPresentIllness = findViewById(R.id.patient_eventsLeadingToPresentIllness_detail);
        setOxygenSaturation = findViewById(R.id.patient_OxygenSaturation_detail);
        setRespiratoryRate = findViewById(R.id.patient_RespiratoryRate_detail);
        setHeartRate = findViewById(R.id.patient_HeartRate_detail);
        setBodyTemperature = findViewById(R.id.patient_BodyTemperature_detail);
        setBloodPressure = findViewById(R.id.patient_BloodPressure_detail);
        patientAccept = findViewById(R.id.patientAccept);
        patientReject = findViewById(R.id.patientReject);
        setRejectionReason = findViewById(R.id.rejection_reason_detail);
        backBtn = findViewById(R.id.backBtn);
        rejection_reason_holder = findViewById(R.id.rejection_reason_holder);
    }

    // Method to show the dialog and get the reason for rejection
    private void showRejectionReasonDialog(String patientId) {
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_rejection_reason, null);
        final EditText input = dialogView.findViewById(R.id.rejectionReasonInput);
        Button btnCancel = dialogView.findViewById(R.id.btnCancel);
        Button btnSubmit = dialogView.findViewById(R.id.btnSubmit);

        androidx.appcompat.app.AlertDialog dialog = new androidx.appcompat.app.AlertDialog.Builder(this)
                .setView(dialogView)
                .setCancelable(false)
                .create();

        btnSubmit.setOnClickListener(v -> {
            String rejectionReason = input.getText().toString().trim();
            if (!rejectionReason.isEmpty()) {
                updatePatientStatus(patientId, false, rejectionReason);
                dialog.dismiss(); // Close the dialog after submission
            } else {
                showToast("Rejection reason cannot be empty");
            }
        });

        btnCancel.setOnClickListener(v -> dialog.cancel());
        dialog.show();
    }

    // Method to update the patient status using the controller
    private void updatePatientStatus(String patientId, boolean isAccepted, String rejectionReason) {
        patientDetailController.updatePatientStatus(hospitalId, patientId, isAccepted, rejectionReason, new PatientDetailController.PatientStatusCallback() {
            @Override
            public void onSuccess(String message) {
                showToast(message);
                finish(); // Close the activity after update
            }

            @Override
            public void onFailure(String error) {
                showToast(error);
            }
        });
    }

    private void displayPatientData(PatientModel patient) {
        firstNameTextView.setText(patient.getFirstName());
        lastNameTextView.setText(patient.getLastName());
        ageTextView.setText(String.valueOf(patient.getAge()));
        sexTextView.setText(patient.getSex());
        setReligionTextView.setText(patient.getReligion());
        complaintTextView.setText(patient.getChiefComplaint());
        setSignsAndSymptoms.setText(patient.getSignsAndSymptoms());
        setAllergies.setText(patient.getAllergies());
        setMedications.setText(patient.getMedications());
        setPastMedicalHistory.setText(patient.getPastMedicalHistory());
        setLastOralIntake.setText(patient.getLastOralIntake());
        setEventsLeadingToPresentIllness.setText(patient.getEventsLeadingToPresentIllness());
        setOxygenSaturation.setText(String.valueOf(patient.getOxygenSaturation()));
        setHeartRate.setText(String.valueOf(patient.getHeartRate()));
        setRespiratoryRate.setText(String.valueOf(patient.getRespiratoryRate()));
        setBodyTemperature.setText(String.valueOf(patient.getBodyTemperature()));
        setBloodPressure.setText(String.valueOf(patient.getBloodPressure()));

        if("true".equals(patient.getIsAccepted())){
            rejection_reason_holder.setText("Accepted at " + patient.getFormattedAcceptanceStatusDate());
            setRejectionReason.setText("");
        }

        if("true".equals(patient.getIsRejected())){
            rejection_reason_holder.setText("Rejected at " + patient.getFormattedAcceptanceStatusDate());
            if(patient.getRejectionReason() != null && !patient.getRejectionReason().isEmpty()){
                String rejectionReason = String.valueOf(patient.getRejectionReason());
                setRejectionReason.setText("Reason: " + rejectionReason);
            }
        }

        List<String> notes = patient.getNotes();
        if (notes != null && !notes.isEmpty()) {
            setNotes.setText(String.join("\n", notes));
        } else {
            setNotes.setText("No notes available");
        }
    }

    private void showToast(String message) {
        Toast.makeText(PatientDetailActivityView.this, message, Toast.LENGTH_SHORT).show();
    }
}
