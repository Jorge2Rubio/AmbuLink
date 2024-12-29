package com.example.ambulink.Service;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;

import com.example.ambulink.BuildConfig;
import com.example.ambulink.R;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Locale;

public class LocationUpdateService extends Service {

    private FusedLocationProviderClient fusedLocationProviderClient;
    private Handler handler;
    private Runnable locationUpdateRunnable;
    private FirebaseFirestore fStore;

    private double hospitalLatitude;
    private double hospitalLongitude;
    private static final float DISTANCE_THRESHOLD = 5.0f; // 5 meters
    private static final String CHANNEL_ID = "ambulink_channel";

    @Override
    public void onCreate() {
        super.onCreate();
        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this);
        handler = new Handler(Looper.getMainLooper());
        fStore = FirebaseFirestore.getInstance();

        // Create the notification channel
        createNotificationChannel();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        String patientId = intent.getStringExtra("patientId");
        String hospitalId = intent.getStringExtra("hospitalId");
        hospitalLatitude = intent.getDoubleExtra("hospitalLatitude", 0.0);
        hospitalLongitude = intent.getDoubleExtra("hospitalLongitude", 0.0);

        // Start the location updates
        locationUpdateRunnable = new Runnable() {
            @Override
            public void run() {
                updatePatientLocation(patientId, hospitalId);
                handler.postDelayed(this, 10000); // 10 seconds
            }
        };
        handler.post(locationUpdateRunnable);

        // Create a persistent notification
        Notification notification = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("AmbuLink Location Service")
                .setContentText("Updating patient location...")
                .setSmallIcon(R.drawable.ambu_logo) // Ensure this icon resource exists
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .build();

        startForeground(1, notification);

        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        handler.removeCallbacks(locationUpdateRunnable);
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "AmbuLink Location Service",
                    NotificationManager.IMPORTANCE_DEFAULT
            );
            channel.setDescription("Channel for AmbuLink location updates");

            NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            if (manager != null) {
                manager.createNotificationChannel(channel);
            }
        }
    }

    private void updatePatientLocation(String patientId, String hospitalId) {
        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }

        fusedLocationProviderClient.getLastLocation().addOnSuccessListener(location -> {
            if (location != null) {
                double patientLatitude = location.getLatitude();
                double patientLongitude = location.getLongitude();

                // Calculate the distance to the hospital using the Distance Matrix API
                fetchDistanceAndUpdateFirestore(patientId, hospitalId, patientLatitude, patientLongitude);

                // Update the patient's location in Firestore
                DocumentReference patientDocRef = fStore.collection("hospitals")
                        .document(hospitalId)
                        .collection("patients")
                        .document(patientId);

                patientDocRef.update("latitude", patientLatitude, "longitude", patientLongitude)
                        .addOnSuccessListener(aVoid -> Log.d("Firestore", "Location updated for Patient ID: " + patientId))
                        .addOnFailureListener(e -> Log.e("Firestore", "Failed to update location", e));
            }
        });
    }


    private void fetchDistanceAndUpdateFirestore(String patientId, String hospitalId, double patientLatitude, double patientLongitude) {
        String url = String.format(
                Locale.US, // Specify the locale explicitly
                "https://maps.googleapis.com/maps/api/distancematrix/json?origins=%f,%f&destinations=%f,%f&mode=driving&key=%s",
                patientLatitude, patientLongitude, hospitalLatitude, hospitalLongitude, BuildConfig.GOOGLE_MAPS_API_KEY
        );

        new Thread(() -> {
            try {
                URL requestUrl = new URL(url);
                HttpURLConnection connection = (HttpURLConnection) requestUrl.openConnection();
                connection.setRequestMethod("GET");
                InputStream inputStream = connection.getInputStream();
                BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
                StringBuilder response = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    response.append(line);
                }
                reader.close();

                parseDistanceMatrixResponse(response.toString(), patientId, hospitalId);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }

    private void parseDistanceMatrixResponse(String response, String patientId, String hospitalId) {
        try {
            JSONObject jsonResponse = new JSONObject(response);
            JSONArray rows = jsonResponse.getJSONArray("rows");
            JSONArray elements = rows.getJSONObject(0).getJSONArray("elements");
            JSONObject element = elements.getJSONObject(0);

            if (element.has("distance") && element.has("duration")) {
                double distanceInMeters = element.getJSONObject("distance").getDouble("value");
                String durationText = element.getJSONObject("duration").getString("text");

                Log.d("Distance to hospital", String.valueOf(distanceInMeters));

                // Corrected Comparison
                if (distanceInMeters <= DISTANCE_THRESHOLD) {
                    Log.d("DistanceCheck", "Patient is nearby the hospital. Stopping service.");
                    stopSelf();
                    return;
                }

                // Update Firestore with distance and ETA
                DocumentReference patientDocRef = fStore.collection("hospitals")
                        .document(hospitalId)
                        .collection("patients")
                        .document(patientId);

                patientDocRef.update("distance", distanceInMeters / 1000.0, "eta", durationText)
                        .addOnSuccessListener(aVoid -> Log.d("Firestore", "Distance and ETA updated for Patient ID: " + patientId))
                        .addOnFailureListener(e -> Log.e("Firestore", "Failed to update distance and ETA", e));
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onTaskRemoved(Intent rootIntent) {
        super.onTaskRemoved(rootIntent);
        // Stop the service when the app is removed from recent apps
        stopSelf();
    }
}
