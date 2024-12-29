package com.example.ambulink.Controller;

import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FieldValue;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;


public class PatientDetailController {

    private FirebaseAuth mAuth;
    private FirebaseFirestore firestore;
    private FirebaseUser user;

    public PatientDetailController() {
        mAuth = FirebaseAuth.getInstance();
        firestore = FirebaseFirestore.getInstance();
        user = mAuth.getCurrentUser();
    }

    // Callback interface for patient status updates
    public interface PatientStatusCallback {
        void onSuccess(String message);
        void onFailure(String error);
    }

    // Method to update patient status (accept/reject)
    public void updatePatientStatus(String hospitalId, String patientId, boolean isAccepted, String rejectionReason, PatientStatusCallback callback) {
        if (user != null) {
            String userID = user.getUid();
            DocumentReference userDocRef = firestore.collection("users").document(userID);

            // Retrieve user full name and update patient status in Firestore
            userDocRef.get().addOnSuccessListener(documentSnapshot -> {
                if (documentSnapshot.exists()) {
                    String fullName = documentSnapshot.getString("fullName");

                    // Prepare fields for update
                    String acceptedField = isAccepted ? "true" : "false";
                    String rejectedField = isAccepted ? "false" : "true";
                    String acceptedByField = isAccepted ? fullName : null;
                    String rejectedByField = !isAccepted ? fullName : null;

                    // Use Firestore's server timestamp
                    FieldValue serverTimestamp = FieldValue.serverTimestamp();

                    // Format the date into a human-readable string
                    String formattedAcceptanceStatusDate = new SimpleDateFormat("MMMM d, yyyy - h:mm a", Locale.getDefault()).format(new Date());

                    // Update patient status, reason for rejection, and formatted date in Firestore
                    firestore.collection("hospitals").document(hospitalId)
                            .collection("patients").document(patientId)
                            .update(
                                    "isAccepted", acceptedField,
                                    "isRejected", rejectedField,
                                    "isAcceptedBy", acceptedByField,
                                    "isRejectedBy", rejectedByField,
                                    "rejectionReason", rejectionReason,
                                    "acceptanceStatusDate", serverTimestamp, // Use server timestamp
                                    "formattedAcceptanceStatusDate", formattedAcceptanceStatusDate // Add formatted date
                            )
                            .addOnSuccessListener(aVoid -> {
                                String message = isAccepted ? "Patient accepted" : "Patient rejected";
                                callback.onSuccess(message);
                            })
                            .addOnFailureListener(e -> callback.onFailure("Failed to update patient status"));
                } else {
                    callback.onFailure("User document not found");
                }
            }).addOnFailureListener(e -> callback.onFailure("Failed to retrieve user data"));
        } else {
            callback.onFailure("User not authenticated");
        }
    }

}
