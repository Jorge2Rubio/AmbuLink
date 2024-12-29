package com.example.ambulink.Controller;

import android.app.Activity;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.example.ambulink.View.StaffManagementView;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.Map;

public class StaffManagementController {
    private WeakReference<Activity> activityRef;
    private FirebaseFirestore firestore;

    public StaffManagementController(Activity activity) {
        this.activityRef = new WeakReference<>(activity);
        this.firestore = FirebaseFirestore.getInstance();
    }

    private Activity getActivity() {
        return activityRef.get();
    }

    /**
     * Load hospital data from Firestore and populate the view fields.
     *
     * @param hospitalId The ID of the hospital document to fetch.
     */
    public void loadHospitalData(@NonNull String hospitalId) {
        Activity activity = getActivity();
        if (activity == null || activity.isFinishing() || activity.isDestroyed()) {
            return;
        }

        firestore.collection("hospitals").document(hospitalId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        // Retrieve the fields, or set default values if they don't exist
                        int numDoctors = documentSnapshot.contains("numDoctors")
                                ? documentSnapshot.get("numDoctors", Integer.class)
                                : 0;
                        int maxDoctors = documentSnapshot.contains("maxDoctors")
                                ? documentSnapshot.get("maxDoctors", Integer.class)
                                : 10;
                        int numNurses = documentSnapshot.contains("numNurses")
                                ? documentSnapshot.get("numNurses", Integer.class)
                                : 0;
                        int maxNurses = documentSnapshot.contains("maxNurses")
                                ? documentSnapshot.get("maxNurses", Integer.class)
                                : 10;
                        int maxSlots = documentSnapshot.contains("maxSlots")
                                ? documentSnapshot.get("maxSlots", Integer.class)
                                : 20;

                        // Update the missing fields in Firestore
                        Map<String, Object> updates = new HashMap<>();
                        if (!documentSnapshot.contains("numDoctors")) updates.put("numDoctors", numDoctors);
                        if (!documentSnapshot.contains("maxDoctors")) updates.put("maxDoctors", maxDoctors);
                        if (!documentSnapshot.contains("numNurses")) updates.put("numNurses", numNurses);
                        if (!documentSnapshot.contains("maxNurses")) updates.put("maxNurses", maxNurses);
                        if (!documentSnapshot.contains("maxSlots")) updates.put("maxSlots", maxSlots);

                        if (!updates.isEmpty()) {
                            firestore.collection("hospitals").document(hospitalId)
                                    .update(updates)
                                    .addOnSuccessListener(aVoid -> Log.d("Firestore", "Missing fields added successfully."))
                                    .addOnFailureListener(e -> Log.e("Firestore", "Error adding missing fields", e));
                        }

                        // Pass data to the view for display and editing
                        ((StaffManagementView) activity).updateFields(numDoctors, maxDoctors, numNurses, maxNurses, maxSlots);
                    } else {
                        Toast.makeText(activity, "Hospital data not found.", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e("StaffManagement", "Error loading hospital data", e);
                    Toast.makeText(activity, "Error loading hospital data: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }


    /**
     * Update hospital data in Firestore based on user input.
     *
     * @param hospitalId  The ID of the hospital document to update.
     * @param numDoctors  Current number of doctors.
     * @param maxDoctors  Maximum number of doctors.
     * @param numNurses   Current number of nurses.
     * @param maxNurses   Maximum number of nurses.
     * @param maxSlots    Maximum number of slots.
     */
    public void updateHospitalData(@NonNull String hospitalId, int numDoctors, int maxDoctors, int numNurses, int maxNurses, int maxSlots) {
        Activity activity = getActivity();
        if (activity == null || activity.isFinishing() || activity.isDestroyed()) {
            return;
        }

        // Prepare data for Firestore update
        Map<String, Object> updates = new HashMap<>();
        updates.put("numDoctors", numDoctors);
        updates.put("maxDoctors", maxDoctors);
        updates.put("numNurses", numNurses);
        updates.put("maxNurses", maxNurses);
        updates.put("maxSlots", maxSlots);

        firestore.collection("hospitals").document(hospitalId)
                .update(updates)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(activity, "Hospital data updated successfully.", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    Log.e("StaffManagement", "Error updating hospital data", e);
                    Toast.makeText(activity, "Failed to update hospital data: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }
}
