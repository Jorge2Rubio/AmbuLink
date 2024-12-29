package com.example.ambulink;

import static android.content.ContentValues.TAG;

import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.LocationManager;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.ambulink.View.HospitalView;
import com.example.ambulink.View.LoginView;
import com.example.ambulink.View.PatientFormView;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

public class MainActivity extends AppCompatActivity {
    private static final int ERROR_DIALOG_REQUEST = 9001;
    private static final int PERMISSIONS_REQUEST_ENABLE_GPS = 9002;
    private static final int PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION = 9003;

    private boolean mLocationPermissionGranted = false;

    private FirebaseAuth mAuth;
    private FirebaseFirestore fStore;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        // Initialize FirebaseAuth and Firestore
        mAuth = FirebaseAuth.getInstance();
        fStore = FirebaseFirestore.getInstance();
        FirebaseUser user = mAuth.getCurrentUser();

        if (user != null) {
            checkUserRole(user.getUid());
            return;
        }

        setupUI();

        // Check map services and location permission
        if (checkMapServices() && !mLocationPermissionGranted) {
            getLocationPermission();
        }
    }

    private void setupUI() {
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        Button paramedicsBtn = findViewById(R.id.paramedicsBtn);
        Button hospitalBtn = findViewById(R.id.hospitalBtn);

        paramedicsBtn.setOnClickListener(view -> navigateToLogin("paramedics"));
        hospitalBtn.setOnClickListener(view -> navigateToLogin("hospital"));
    }

    private void navigateToLogin(String loginType) {
        Intent intent = new Intent(MainActivity.this, LoginView.class);
        intent.putExtra("LOGIN_TYPE", loginType);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
    }

    private void checkUserRole(String uid) {
        DocumentReference docRef = fStore.collection("users").document(uid);
        docRef.get().addOnCompleteListener(task -> {
            if (task.isSuccessful() && task.getResult() != null && task.getResult().exists()) {
                DocumentSnapshot document = task.getResult();
                String role = document.getString("role");
                String hospitalId = document.getString("hospitalID");

                if ("paramedic".equals(role)) {
                    navigateToParamedics();
                } else if ("hospital_staff".equals(role)) {
                    if (hospitalId != null) {
                        navigateToHospital(hospitalId);
                    } else {
                        showToast("Hospital ID not found.");
                    }
                } else {
                    handleUnknownRole();
                }
            } else {
                handleUnknownRole();
            }
        });
    }

    private void handleUnknownRole() {
        mAuth.signOut();
        showToast("Unknown role or unauthorized access.");
        navigateToMain();
    }

    private void navigateToHospital(String hospitalId) {
        if(mAuth.getCurrentUser().getMultiFactor().getEnrolledFactors().size() > 0){
            Intent intent = new Intent(MainActivity.this, HospitalView.class);
            intent.putExtra("hospitalId", hospitalId);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        }else {
            mAuth.signOut();
            navigateToLogin("hospital");
        }
    }

    private void navigateToMain() {
        Intent intent = new Intent(MainActivity.this, MainActivity.class);
        startActivity(intent);
        finish();
    }

    private void navigateToParamedics() {
      if(mAuth.getCurrentUser().getMultiFactor().getEnrolledFactors().size() > 0){
          Intent intent = new Intent(MainActivity.this, PatientFormView.class);
          intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
          startActivity(intent);
          finish();
      }else {
          mAuth.signOut();
          navigateToLogin("paramedics");
      }
    }

    // Checks if Google Map services are available and enabled
    private boolean checkMapServices() {
        return isServicesOK() && isMapsEnabled();
    }

    // Builds an alert dialog to prompt the user to enable GPS
    private void buildAlertMessageNoGps() {
        new AlertDialog.Builder(this)
                .setMessage("This application requires GPS to work properly, do you want to enable it?")
                .setCancelable(false)
                .setPositiveButton("Yes", (dialog, id) -> {
                    Intent enableGpsIntent = new Intent(android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                    startActivityForResult(enableGpsIntent, PERMISSIONS_REQUEST_ENABLE_GPS);
                })
                .show();
    }

    // Checks if GPS provider is enabled
    public boolean isMapsEnabled() {
        LocationManager manager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        if (manager != null && !manager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            buildAlertMessageNoGps();
            return false;
        }
        return true;
    }

    // Requests location permission from the user
    private void getLocationPermission() {
        if (ContextCompat.checkSelfPermission(this.getApplicationContext(), android.Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            mLocationPermissionGranted = true;
        } else {
            ActivityCompat.requestPermissions(this, new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION}, PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION);
        }
    }

    // Checks if Google Play Services are available and up-to-date
    public boolean isServicesOK() {
        int available = GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(MainActivity.this);
        if (available == ConnectionResult.SUCCESS) {
            Log.d(TAG, "isServicesOK: Google Play Services is working");
            return true;
        } else if (GoogleApiAvailability.getInstance().isUserResolvableError(available)) {
            Dialog dialog = GoogleApiAvailability.getInstance().getErrorDialog(MainActivity.this, available, ERROR_DIALOG_REQUEST);
            dialog.show();
        } else {
            showToast("You can't make map requests.");
        }
        return false;
    }

    // Handles the result of location permission requests
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        mLocationPermissionGranted = false;
        if (requestCode == PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION && grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            mLocationPermissionGranted = true;
        }
    }

    // Handles the result of enabling GPS
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PERMISSIONS_REQUEST_ENABLE_GPS && mLocationPermissionGranted) {
            getLocationPermission();
        }
    }

    private void showToast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }
}
