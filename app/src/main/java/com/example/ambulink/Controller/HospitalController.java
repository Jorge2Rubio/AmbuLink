package com.example.ambulink.Controller;

import android.app.Activity;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.os.Build;
import android.util.Log;

import androidx.core.app.NotificationCompat;

import com.example.ambulink.R;
import com.example.ambulink.View.HospitalView;
import com.example.ambulink.Model.PatientModel;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.lang.ref.WeakReference;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class HospitalController {

    private WeakReference<Activity> activityRef;
    private FirebaseFirestore firestore;
    private ListenerRegistration patientsListener;
    private final Map<String, Boolean> notificationSentForEta10 = new HashMap<>();
    private final Map<String, Boolean> notificationSentForEta1 = new HashMap<>();


    public HospitalController(Activity activity) {
        this.activityRef = new WeakReference<>(activity);
        this.firestore = FirebaseFirestore.getInstance();
    }

    private Activity getActivity() {
        return activityRef.get();
    }

    public void loadInitialData(String hospitalId) {
        loadHospitalData(hospitalId);
        loadAndListenForPatientUpdates(hospitalId);
    }

    private void loadHospitalData(String hospitalId) {
        Activity activity = getActivity();
        if (activity == null || activity.isFinishing() || activity.isDestroyed()) {
            return;
        }

        DocumentReference hospitalRef = firestore.collection("hospitals").document(hospitalId);
        hospitalRef.get().addOnCompleteListener(task -> {
            if (task.isSuccessful() && task.getResult() != null) {
                DocumentSnapshot document = task.getResult();
                if (document.exists()) {
                    int maxSlots = document.get("maxSlots", Integer.class);
                    int currentCapacity = document.get("slotsAvailable", Integer.class);
                    ((HospitalView) activity).updateSlotDetails(maxSlots, currentCapacity);
                } else {
                    ((HospitalView) activity).showToast("Hospital data not found");
                }
            } else {
                ((HospitalView) activity).showToast("Error loading hospital data");
            }
        });
    }

    public void loadAndListenForPatientUpdates(String hospitalId) {
        Activity activity = getActivity();
        if (activity == null || activity.isFinishing() || activity.isDestroyed()) {
            return;
        }

        // Set up real-time listener for both initial load and live updates
        patientsListener = firestore.collection("hospitals").document(hospitalId)
                .collection("patients")
                .addSnapshotListener((querySnapshot, e) -> {
                    if (e != null) {
                        ((HospitalView) activity).showToast("Failed to load patient data: " + e.getMessage());
                        return;
                    }

                    SimpleDateFormat dateFormat = new SimpleDateFormat("MMMM d, yyyy", Locale.getDefault());
                    String todayDate = dateFormat.format(new Date());

                    if (querySnapshot != null) {
                        List<PatientModel> patientList = new ArrayList<>();
                        for (DocumentSnapshot document : querySnapshot.getDocuments()) {
                            PatientModel patient = document.toObject(PatientModel.class);
                            if (patient != null) {
                                patient.setPatientId(document.getId());

                                // Date matching check
                                if (patient.getDate() != null) {
                                    Timestamp timestamp = patient.getDate();
                                    String patientDate = dateFormat.format(timestamp.toDate());

                                    if (patientDate.equals(todayDate)) {
                                        // ETA and notification handling
                                        handleEtaAndNotification(patient, hospitalId);

                                        patientList.add(patient);
                                    }
                                }
                            }
                        }

                        // Sort and update the patient list
                        sortPatientList(patientList);
                        ((HospitalView) activity).updatePatientList(patientList);
                    } else {
                        ((HospitalView) activity).updatePatientList(new ArrayList<>());
                    }
                });
    }


    private void handleEtaAndNotification(PatientModel patient, String hospitalId) {
        int eta = patient.getEtaInMinutesHospital();
        String patientId = patient.getPatientId();

        if (eta <= 10) {
            if (!notificationSentForEta10.getOrDefault(patientId, false)) {
                showNotification("Patient ETA Alert", "A patient is " + eta + " minutes away.");
                notificationSentForEta10.put(patientId, true);
            }
            patient.setNearHospital(true);

            // Update 'nearHospital' in Firestore
            updatePatientField(hospitalId, patientId, "nearHospital", true);
        } else {
            patient.setNearHospital(false);
            notificationSentForEta10.put(patientId, false); // Reset if eta > 10

            // Update 'nearHospital' in Firestore
            updatePatientField(hospitalId, patientId, "nearHospital", false);
        }

        if (eta <= 1) {
            if (!notificationSentForEta1.getOrDefault(patientId, false)) {
                showNotification("Patient ETA Alert", "Patient Arrived!");
                notificationSentForEta1.put(patientId, true);
            }
            patient.setArrived(true);

            // Update 'arrived' in Firestore
            updatePatientField(hospitalId, patientId, "arrived", true);
        } else {
            notificationSentForEta1.put(patientId, false); // Reset if eta > 1

            // Update 'arrived' in Firestore
            updatePatientField(hospitalId, patientId, "arrived", false);
        }
    }


    private void updatePatientField(String hospitalId, String patientId, String fieldName, Object value) {
        DocumentReference patientRef = firestore.collection("hospitals")
                .document(hospitalId)
                .collection("patients")
                .document(patientId);

        patientRef.update(fieldName, value)
                .addOnSuccessListener(aVoid -> Log.d("Firestore", "Patient " + fieldName + " updated successfully"))
                .addOnFailureListener(e -> Log.e("Firestore", "Error updating patient " + fieldName, e));
    }



    private String determineStatus(PatientModel patient) {
        if ("true".equals(patient.getIsAccepted())) {
            return "Accepted";
        } else if ("true".equals(patient.getIsRejected())) {
            return "Rejected";
        } else {
            return "Pending";
        }
    }



    private void sortPatientList(List<PatientModel> patientList) {
        long currentTime = System.currentTimeMillis();
        PatientModel patientModel = new PatientModel();

        Collections.sort(patientList, (p1, p2) -> {
            // Determine if the patients are arrived for over 30 minutes
            boolean isP1Arrived30MinsAgo = isArrivedForOver30Mins(p1, currentTime);
            boolean isP2Arrived30MinsAgo = isArrivedForOver30Mins(p2, currentTime);

            // If both are arrived for over 30 mins, treat them equally
            if (isP1Arrived30MinsAgo && isP2Arrived30MinsAgo) {
                return 0;
            }

            // Patients arrived for over 30 mins go to the bottom
            if (isP1Arrived30MinsAgo) {
                return 1;
            }
            if (isP2Arrived30MinsAgo) {
                return -1;
            }

            // Define sorting priority: Pending > Accepted > Rejected
            String status1 = determineStatus(p1);
            String status2 = determineStatus(p2);

            int priority1 = getStatusPriority(status1);
            int priority2 = getStatusPriority(status2);

            // Compare based on priority first
            if (priority1 != priority2) {
                return Integer.compare(priority1, priority2);
            }

            // If the priorities are the same, sort by ETA (ascending)
            return Integer.compare(p1.getEtaInMinutesHospital(), p2.getEtaInMinutesHospital());
        });
    }

    private boolean isArrivedForOver30Mins(PatientModel patient, long currentTime) {
        if (patient.isArrived() && patient.getAcceptanceStatusDate() != null) {
            long acceptanceTime = patient.getAcceptanceStatusDate().toDate().getTime();
            return (currentTime - acceptanceTime) >= 30 * 60 * 1000; // 30 minutes in milliseconds if you want to change this like 30 seconds this is the formula 30 * 1000
        }
        return false;
    }

    private int getStatusPriority(String status) {
        switch (status) {
            case "Pending":
                return 1; // Highest priority
            case "Accepted":
                return 2;
            case "Rejected":
                return 3; // Lowest priority
            default:
                return Integer.MAX_VALUE; // For unknown statuses, place them at the end
        }
    }


    // Call this method to remove the listener when not needed
    public void removePatientListener() {
        if (patientsListener != null) {
            patientsListener.remove();
            patientsListener = null;
        }
    }



    // Add this method to display notifications
    private void showNotification(String title, String content) {
        Activity activity = getActivity();
        if (activity == null) return;

        NotificationManager notificationManager = (NotificationManager) activity.getSystemService(Context.NOTIFICATION_SERVICE);
        String channelId = "eta_notification_channel";

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    channelId,
                    "ETA Notifications",
                    NotificationManager.IMPORTANCE_HIGH
            );
            notificationManager.createNotificationChannel(channel);
        }

        NotificationCompat.Builder builder = new NotificationCompat.Builder(activity, channelId)
                .setSmallIcon(R.drawable.ambu_logo)  // Use your notification icon
                .setContentTitle(title)
                .setContentText(content)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true);

        notificationManager.notify((int) System.currentTimeMillis(), builder.build());
    }




    public void updateSlots(String hospitalId, int delta) {
        Activity activity = getActivity();
        if (activity == null || activity.isFinishing() || activity.isDestroyed()) {
            return;
        }

        DocumentReference hospitalRef = firestore.collection("hospitals").document(hospitalId);
        hospitalRef.get().addOnCompleteListener(task -> {
            if (task.isSuccessful() && task.getResult() != null) {
                DocumentSnapshot document = task.getResult();
                if (document.exists()) {
                    int currentCapacity = document.get("slotsAvailable", Integer.class);
                    int maxSlots = document.get("maxSlots", Integer.class);

                    int newCapacity = currentCapacity + delta;
                    if (newCapacity < 0) {
                        ((HospitalView) activity).showToast("No slots available to subtract.");
                    } else if (newCapacity > maxSlots) {
                        ((HospitalView) activity).showToast("Maximum slots reached.");
                    } else {
                        hospitalRef.update("slotsAvailable", newCapacity)
                                .addOnSuccessListener(aVoid -> ((HospitalView) activity).updateSlotDetails(maxSlots, newCapacity))
                                .addOnFailureListener(e -> ((HospitalView) activity).showToast("Failed to update slots"));
                    }
                }
            }
        });
    }
}
