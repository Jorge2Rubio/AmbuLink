package com.example.ambulink.Controller;

import android.util.Log;

import com.example.ambulink.Model.HospitalModel;
import com.example.ambulink.Model.PatientModel;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.List;

public class PatientFormController {

    private FirebaseAuth mAuth;
    private FirebaseFirestore fStore;
    private FirebaseUser user;

    public PatientFormController() {
        mAuth = FirebaseAuth.getInstance();
        fStore = FirebaseFirestore.getInstance();
        user = mAuth.getCurrentUser();
    }

    public interface FirestoreCallback {
        void onSuccess(PatientModel patientModel);
        void onFailure(String error);
    }

    // Method to submit patient form
    public void submitForm(HospitalModel hospitalModel, PatientModel patientModel, FirestoreCallback callback) {
        if (user != null) {
            String userID = user.getUid();
            DocumentReference docRef = fStore.collection("users").document(userID);

            docRef.get().addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    DocumentSnapshot document = task.getResult();
                    if (document.exists()) {
                        // Set the patient model data without saving to Firestore
                        patientModel.setSenderEmail(document.getString("fullName"));
                        callback.onSuccess(patientModel); // Callback to notify the model is populated
                    } else {
                        callback.onFailure("User document does not exist.");
                    }
                } else {
                    callback.onFailure(task.getException().getMessage());
                }
            });
        } else {
            callback.onFailure("User not authenticated.");
        }
    }




    // Helper methods to validate input data
    public boolean validateFields(String firstName, String lastName, String ageInput, String selectedSex, String chiefComplaint, String selectedReligion, String bloodPressure, String oxygenSaturation, String heartRate, String bodyTemperature, List<String> notes) {
        return !(firstName.isEmpty() || lastName.isEmpty() || ageInput.isEmpty() || selectedSex.isEmpty() || chiefComplaint.isEmpty() || selectedReligion.isEmpty() || bloodPressure.isEmpty() || oxygenSaturation.isEmpty() || heartRate.isEmpty() || bodyTemperature.isEmpty() || notes.isEmpty() || notes.get(0).isEmpty());
    }

    public double parseDouble(String value, double defaultValue) {
        return value.isEmpty() ? defaultValue : Double.parseDouble(value);
    }

    public int parseInt(String value, int defaultValue) {
        return value.isEmpty() ? defaultValue : Integer.parseInt(value);
    }
}
