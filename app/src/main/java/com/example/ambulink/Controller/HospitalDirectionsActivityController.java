package com.example.ambulink.Controller;

import android.Manifest;
import android.content.pm.PackageManager;
import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.example.ambulink.Model.HospitalModel;
import com.example.ambulink.View.HospitalDirectionActivityView;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.model.LatLng;

public class HospitalDirectionsActivityController {

    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1;
    private HospitalDirectionActivityView activity;
    private FusedLocationProviderClient fusedLocationProviderClient;

    public HospitalDirectionsActivityController(HospitalDirectionActivityView activity) {
        this.activity = activity;
        this.fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(activity);
    }

    public void checkLocationPermissionAndProceed(HospitalModel hospitalModel) {
        if (hasLocationPermission()) {
            getCurrentUserLocation(hospitalModel);
        } else {
            requestLocationPermission();
        }
    }

    private boolean hasLocationPermission() {
        return ContextCompat.checkSelfPermission(activity, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED;
    }

    private void requestLocationPermission() {
        ActivityCompat.requestPermissions(activity, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, LOCATION_PERMISSION_REQUEST_CODE);
    }

    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults, HospitalModel hospitalModel) {
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE && grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            getCurrentUserLocation(hospitalModel);
        } else {
            activity.showToastAndFinish("Location permission is required for navigation");
        }
    }

    private void getCurrentUserLocation(HospitalModel hospitalModel) {
        if (ActivityCompat.checkSelfPermission(activity, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(activity, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        fusedLocationProviderClient.getLastLocation().addOnSuccessListener(location -> {
            if (location != null) {
                LatLng userLocation = new LatLng(location.getLatitude(), location.getLongitude());
                LatLng hospitalLatLng = new LatLng(hospitalModel.getLatitude(), hospitalModel.getLongitude());
                activity.launchNavigationIntent(userLocation, hospitalLatLng);
            } else {
                activity.showToast("Unable to get current location");
            }
        });
    }
}
