package com.example.ambulink.View;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.fragment.app.FragmentActivity;

import com.example.ambulink.Controller.HospitalDirectionsActivityController;
import com.example.ambulink.Model.HospitalModel;
import com.example.ambulink.R;
import com.google.android.gms.maps.model.LatLng;

import java.util.Locale;

public class HospitalDirectionActivityView extends FragmentActivity {

    private HospitalModel hospitalModel;
    private ActivityResultLauncher<Intent> mapsActivityResultLauncher;
    private HospitalDirectionsActivityController hospitalDirectionsController;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_hospital_directions);

        // Initialize controller
        hospitalDirectionsController = new HospitalDirectionsActivityController(this);

        // Initialize ActivityResultLauncher for launching Google Maps
        mapsActivityResultLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> finish()
        );

        // Retrieve hospital data from the intent
        hospitalModel = getIntent().getParcelableExtra("hospital_model");

        if (hospitalModel == null) {
            showToastAndFinish("No hospital data provided");
            return;
        }

        // Handle location permission and location retrieval via the controller
        hospitalDirectionsController.checkLocationPermissionAndProceed(hospitalModel);
    }

    public void launchNavigationIntent(LatLng origin, LatLng destination) {
        try {
            // Create a Google Maps navigation URI with a specific locale (Locale.US)
            String uri = String.format(Locale.US,
                    "https://www.google.com/maps/dir/?api=1&destination=%f,%f&origin=%f,%f&mode=d",
                    destination.latitude, destination.longitude, origin.latitude, origin.longitude);

            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(uri));

            // Verify that an app exists to handle the intent
            if (intent.resolveActivity(getPackageManager()) != null) {
                mapsActivityResultLauncher.launch(intent);
            } else {
                showToast("Google Maps app is not installed.");
            }
        } catch (Exception e) {
            showToast("Failed to launch navigation: " + e.getMessage());
        }
    }

    public void showToastAndFinish(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
        finish();
    }

    public void showToast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }
}
