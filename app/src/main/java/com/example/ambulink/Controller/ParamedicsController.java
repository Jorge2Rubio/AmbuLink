package com.example.ambulink.Controller;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.Toast;

import com.example.ambulink.BuildConfig;
import com.example.ambulink.Listener.HospitalDataListener;
import com.example.ambulink.Model.HospitalModel;
import com.example.ambulink.Model.PatientModel;
import com.example.ambulink.Service.LocationUpdateService;
import com.example.ambulink.View.PatientFormView;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ParamedicsController {

    private Context context;
    private FirebaseFirestore fStore;
    private FusedLocationProviderClient fusedLocationProviderClient;
    private ExecutorService executorService;
    private Handler mainHandler;

    private List<HospitalModel> hospitalList;
    private List<HospitalModel> originalHospitalList;

    private HospitalDataListener dataListener;

    public ParamedicsController(Context context, HospitalDataListener dataListener) {
        this.context = context;
        this.fStore = FirebaseFirestore.getInstance();
        this.fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(context);
        this.executorService = Executors.newCachedThreadPool();
        this.mainHandler = new Handler(Looper.getMainLooper());
        this.hospitalList = new ArrayList<>();
        this.originalHospitalList = new ArrayList<>();
        this.dataListener = dataListener;
    }

    public List<HospitalModel> getHospitalList() {
        return hospitalList;
    }

    public void fetchHospitalData() {
        fStore.collection("hospitals").addSnapshotListener((querySnapshot, e) -> {
            if (e != null) {
                Toast.makeText(context, "Failed to fetch hospital data", Toast.LENGTH_SHORT).show();
                Log.e("Firestore", "Error fetching hospital data", e);
                return;
            }

            Log.d("Firestore", "Listener triggered: Data fetched");

            // Map current hospitals by ID to efficiently update existing items
            Map<String, HospitalModel> currentHospitalMap = new HashMap<>();
            for (HospitalModel hospital : hospitalList) {
                currentHospitalMap.put(hospital.getId(), hospital);
            }

            // Track IDs to identify hospitals to remove
            Set<String> updatedHospitalIds = new HashSet<>();

            // Process each document in the Firestore snapshot
            for (DocumentSnapshot document : querySnapshot.getDocuments()) {
                String hospitalId = document.getId();
                updatedHospitalIds.add(hospitalId);

                // Check if the hospital already exists in the current list
                HospitalModel existingHospital = currentHospitalMap.get(hospitalId);
                if (existingHospital != null) {
                    // Update existing fields from Firestore, preserving local fields like isPending
                    existingHospital.setName(document.getString("name"));
                    existingHospital.setEta(document.getString("eta"));
                    existingHospital.setAddress(document.getString("address"));
                    existingHospital.setCapacity(document.getLong("capacity") != null ? document.getLong("capacity").intValue() : 0);
                    existingHospital.setLatitude(parseToDouble(document.get("latitude")));
                    existingHospital.setLongitude(parseToDouble(document.get("longitude")));
                    existingHospital.setSlotsAvailable(document.getLong("slotsAvailable") != null ? document.getLong("slotsAvailable").intValue() : 0);
                    existingHospital.setMaxSlots(document.getLong("maxSlots") != null ? document.getLong("maxSlots").intValue() : 0);
                    if (document.getString("isAccepted") != null) {
                        existingHospital.setIsAccepted(document.getString("isAccepted"));
                    }
                    if (document.getString("isRejected") != null) {
                        existingHospital.setIsRejected(document.getString("isRejected"));
                    }
                    existingHospital.setPatientId(document.getString("patientId"));
                    if(document.getTimestamp("acceptanceStatusDate") != null){
                        existingHospital.setAcceptanceStatusDate(document.getTimestamp("acceptanceStatusDate"));
                    }

                    if(document.getString("hospitalType") != null){
                        existingHospital.setHospitalType(document.getString("hospitalType"));
                    }
                    if(document.getLong("maxDoctors") != null){
                        existingHospital.setMaxDoctors(Objects.requireNonNull(document.getLong("maxDoctors")).intValue());
                    }
                    if(document.getLong("maxNurses") != null){
                        existingHospital.setMaxNurses(Objects.requireNonNull(document.getLong("maxNurses")).intValue());
                    }
                    if(document.getLong("numDoctors") != null){
                        existingHospital.setNumDoctors(Objects.requireNonNull(document.getLong("numDoctors")).intValue());
                    }
                    if(document.getLong("numNurses") != null){
                        existingHospital.setNumNurses(Objects.requireNonNull(document.getLong("numNurses")).intValue());
                    }



                    // Retain the isPending value and any other local fields already set in existingHospital
                } else {
                    // Add a new hospital to the list if it doesn't already exist
                    HospitalModel newHospital = document.toObject(HospitalModel.class);
                    if (newHospital != null) {
                        newHospital.setId(hospitalId);
                        newHospital.setLatitude(parseToDouble(document.get("latitude")));
                        newHospital.setLongitude(parseToDouble(document.get("longitude")));
                        hospitalList.add(newHospital); // Add to hospitalList
                        originalHospitalList.add(newHospital); // fixed
                    }
                }
            }

            // Remove hospitals that are no longer in Firestore
            hospitalList.removeIf(hospital -> !updatedHospitalIds.contains(hospital.getId()));

            // Notify the data listener of the updated list
            dataListener.onHospitalDataChanged();

            // Calculate ETAs and sort hospitals
            List<String> destinations = new ArrayList<>();
            for (HospitalModel hospital : hospitalList) {
                destinations.add(hospital.getLatitude() + "," + hospital.getLongitude());
            }

            if (ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                Log.e("Permissions", "Location permission not granted");
                return;
            }

            fusedLocationProviderClient.getLastLocation().addOnCompleteListener(task -> {
                if (task.isSuccessful() && task.getResult() != null) {
                    Location location = task.getResult();
                    String userLocation = location.getLatitude() + "," + location.getLongitude();
                    calculateETAs(userLocation, destinations);
                } else {
                    Toast.makeText(context, "Unable to get current location for ETA calculation", Toast.LENGTH_SHORT).show();
                    Log.e("Location", "Failed to get current location");
                }
            });
        });
    }




    private double parseToDouble(Object value) {
        if (value instanceof Double) {
            return (Double) value;
        } else if (value instanceof String) {
            try {
                return Double.parseDouble((String) value);
            } catch (NumberFormatException e) {
                Log.e("Firestore", "Error parsing latitude/longitude field to double", e);
            }
        } else if (value instanceof Long) {
            return ((Long) value).doubleValue();
        }
        return 0.0; // Default value if parsing fails
    }



    private void calculateETAs(String userLocation, List<String> destinations) {
        StringBuilder destinationStr = new StringBuilder();
        String mode = "driving";
        for (String destination : destinations) {
            if (destinationStr.length() > 0) {
                destinationStr.append("|");
            }
            destinationStr.append(destination);
        }

        String url = String.format(
                "https://maps.googleapis.com/maps/api/distancematrix/json?origins=%s&destinations=%s&mode=%s&key=%s",
                userLocation, destinationStr.toString(), mode, BuildConfig.GOOGLE_MAPS_API_KEY
        );

        executorService.execute(() -> {
            try {
                URL requestUrl = new URL(url);
                HttpURLConnection connection = (HttpURLConnection) requestUrl.openConnection();
                connection.setRequestMethod("GET");
                InputStream inputStream = connection.getInputStream();
                BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
                StringBuilder result = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    result.append(line);
                }
                reader.close();
                handleDistanceMatrixResponse(result.toString());
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
    }

    private void handleDistanceMatrixResponse(String response) {
        mainHandler.post(() -> {
            try {
                JSONObject jsonObject = new JSONObject(response);
                JSONArray rows = jsonObject.getJSONArray("rows");
                JSONArray elements = rows.getJSONObject(0).getJSONArray("elements");

                for (int i = 0; i < elements.length(); i++) {
                    JSONObject element = elements.getJSONObject(i);
                    String eta = element.getJSONObject("duration").getString("text");
                    String distanceText = element.getJSONObject("distance").getString("text");
                    double distance = parseDistance(distanceText);

                    HospitalModel hospital = hospitalList.get(i);
                    hospital.setDistance(distance);
                    hospital.setEta(eta);
                }

                sortHospitals(hospitalList);

                dataListener.onHospitalDataChanged();
            } catch (JSONException e) {
                e.printStackTrace();
                Toast.makeText(context, "Error parsing ETA information", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private double parseDistance(String distanceText) {
        return Double.parseDouble(distanceText.split(" ")[0]);
    }

    private void sortHospitals(List<HospitalModel> hospitals) {
        hospitals.sort(Comparator
                .comparingInt((HospitalModel h) -> parseEta(h.getEta())) // Lower ETA first
                .thenComparingInt(h -> (h.getNumDoctors() > 0 && h.getNumNurses() > 0) ? 0 : 1) // Hospitals with doctors and nurses available first
                .thenComparing(Comparator.comparingInt(HospitalModel::getSlotsAvailable).reversed())); // Higher slots available third
    }


    public void updateOtherHospitalsWithFinalDecision(String selectedHospitalId) {
        // Iterate over the list of hospitals
        for (HospitalModel hospital : hospitalList) {
            String hospitalId = hospital.getId();
            String patientId = hospital.getPatientId();

            // Skip the selected hospital and hospitals without a patientId
            if (!hospitalId.equals(selectedHospitalId) && patientId != null && !patientId.isEmpty()) {
                Log.d("Firestore", "Updating finalDecision for hospitalId: " + hospitalId + ", patientId: " + patientId);

                // Update the "finalDecision" field in the patient document
                fStore.collection("hospitals")
                        .document(hospitalId)
                        .collection("patients")
                        .document(patientId)
                        .update("finalDecision", "Patient accepted somewhere else")
                        .addOnSuccessListener(aVoid -> Log.d("Firestore", "Updated finalDecision for patient in hospital: " + hospitalId))
                        .addOnFailureListener(e -> Log.e("Firestore", "Failed to update finalDecision", e));
            } else {
                if (hospitalId.equals(selectedHospitalId)) {
                    Log.d("Firestore", "Skipping selected hospitalId: " + hospitalId);
                } else if (patientId == null || patientId.isEmpty()) {
                    Log.e("Firestore", "No valid patientId for hospitalId: " + hospitalId);
                }
            }
        }
    }

    private int parseEta(String etaText) {
        int totalMinutes = 0;
        String[] parts = etaText.split(" ");
        for (int i = 0; i < parts.length; i++) {
            if (parts[i].contains("hour")) {
                totalMinutes += Integer.parseInt(parts[i - 1]) * 60;
            } else if (parts[i].contains("min")) {
                totalMinutes += Integer.parseInt(parts[i - 1]);
            }
        }
        return totalMinutes;
    }


    public void sendPatientFormToHospital(PatientModel originalPatientModel, HospitalModel hospitalModel) {
        String hospitalId = hospitalModel.getId();

        // Create a new PatientModel instance for this hospital
        PatientModel patientModel = new PatientModel(originalPatientModel);

        // Generate a new patient ID for this hospital
        DocumentReference patientDocRef = fStore.collection("hospitals")
                .document(hospitalId)
                .collection("patients")
                .document();

        String newPatientId = patientDocRef.getId();
        patientModel.setPatientId(newPatientId);
        hospitalModel.setPatientId(newPatientId); // Update the HospitalModel

        // Save the patient data to Firestore
        patientDocRef.set(patientModel)
                .addOnSuccessListener(aVoid -> {
                    Log.d("Firestore", "Generated and set Patient ID: " + newPatientId);
                    FieldValue serverTimestamp = FieldValue.serverTimestamp();

                    // Start monitoring with the newly generated patient ID
                    monitorPatientStatus(patientModel, hospitalModel);
                    patientDocRef.update("date", serverTimestamp);

                    // Notify listener that patient form has been sent
                    dataListener.onPatientFormSent(hospitalModel);

                    // Update the HospitalModel in hospitalList
                    updateHospitalModelPatientId(hospitalId, newPatientId);

                    startLocationService(newPatientId, hospitalModel.getId(), hospitalModel.getLatitude(), hospitalModel.getLongitude());
                })
                .addOnFailureListener(e -> {
                    Log.e("Firestore", "Failed to send patient data", e);
                    Toast.makeText(context, "Failed to send patient data", Toast.LENGTH_SHORT).show();
                });
    }

    private void updateHospitalModelPatientId(String hospitalId, String newPatientId) {
        for (HospitalModel hospital : hospitalList) {
            if (hospital.getId().equals(hospitalId)) {
                hospital.setPatientId(newPatientId);
                break;
            }
        }
    }


    public void startLocationService(String patientId, String hospitalId, double hospitalLatitude, double hospitalLongitude) {
        Intent serviceIntent = new Intent(context, LocationUpdateService.class);
        serviceIntent.putExtra("patientId", patientId);
        serviceIntent.putExtra("hospitalId", hospitalId);
        serviceIntent.putExtra("hospitalLatitude", hospitalLatitude);
        serviceIntent.putExtra("hospitalLongitude", hospitalLongitude);
        ContextCompat.startForegroundService(context, serviceIntent);
    }

    // Method to stop the LocationUpdateService
    public void stopLocationService() {
        Intent serviceIntent = new Intent(context, LocationUpdateService.class);
        context.stopService(serviceIntent);
    }

    private void monitorPatientStatus(PatientModel patientModel, HospitalModel hospitalModel) {
        String hospitalId = hospitalModel.getId();
        String patientId = patientModel.getPatientId();

        if (patientId != null && !patientId.isEmpty()) {
            fStore.collection("hospitals")
                    .document(hospitalId)
                    .collection("patients")
                    .document(patientId)
                    .addSnapshotListener((snapshot, e) -> {
                        if (e != null) {
                            Log.w("Firestore", "Listen failed.", e);
                            return;
                        }

                        if (snapshot != null && snapshot.exists()) {
                            String isAccepted = snapshot.getString("isAccepted");
                            String isRejected = snapshot.getString("isRejected");
                            String isAcceptedBy = snapshot.getString("isAcceptedBy");
                            String isRejectedBy = snapshot.getString("isRejectedBy");
                            String rejectionReason = snapshot.getString("rejectionReason");
                            Timestamp acceptanceStatusDate = snapshot.getTimestamp("acceptanceStatusDate");
                            Timestamp Date = snapshot.getTimestamp("Date");

                            // Update only the relevant hospital model
                            hospitalModel.setIsAccepted(isAccepted);
                            hospitalModel.setIsRejected(isRejected);
                            hospitalModel.setIsAcceptedBy(isAcceptedBy);
                            hospitalModel.setIsRejectedBy(isRejectedBy);
                            hospitalModel.setRejectionReason(rejectionReason);
                            hospitalModel.setAcceptanceStatusDate(acceptanceStatusDate);

                            // Update the corresponding PatientModel
                            patientModel.setIsAccepted(isAccepted);
                            patientModel.setIsRejected(isRejected);
                            patientModel.setIsAcceptedBy(isAcceptedBy);
                            patientModel.setIsRejectedBy(isRejectedBy);
                            patientModel.setAcceptanceStatusDate(acceptanceStatusDate);
                            patientModel.setDate(Date);

                            dataListener.onPatientStatusUpdated(hospitalModel);
                        } else {
                            Log.d("Firestore", "No such document");
                        }
                    });
        } else {
            Log.e("Firestore", "Invalid patientId: Patient ID is null or empty.");
        }
    }




    public void filter(String text) {
        List<HospitalModel> filteredList = new ArrayList<>();
        String[] searchWords = text.toLowerCase().split("\\s+");

        for (HospitalModel item : originalHospitalList) {
            boolean matches = false;
            for (String word : searchWords) {
                if (item.getName().toLowerCase().contains(word)) {
                    matches = true;
                    break;
                }
            }
            if (matches) {
                filteredList.add(item);
            }
        }
        hospitalList.clear();
        hospitalList.addAll(filteredList);

        dataListener.onHospitalDataChanged();
    }

    public void startNewPatientForm() {
        stopLocationService();
        Intent intent = new Intent(context, PatientFormView.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        context.startActivity(intent);
    }

}
