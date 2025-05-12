package com.example.evchargingfinal;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.telephony.SmsManager;
import android.widget.Toast;

import com.example.evchargingfinal.databinding.ActivityDashboardBinding;
import com.example.evchargingfinal.envo_impact.EnviromentalImpactActivity;
import com.example.evchargingfinal.profile.ProfileActivity;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

public class Dashboard extends AppCompatActivity {
    private Owner owner;
    private FirebaseAuth firebaseAuth;
    private FirebaseFirestore firebaseFirestore;
    private ActivityDashboardBinding binding;
    private FusedLocationProviderClient fusedLocationClient;
    private LocationRequest locationRequest;
    private LocationCallback locationCallback;
    private boolean isSosInProgress = false; // Flag to prevent repeated SOS messages

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityDashboardBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Initialize Firebase and location services
        firebaseAuth = FirebaseAuth.getInstance();
        firebaseFirestore = FirebaseFirestore.getInstance();
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        setupLocationRequest();
        setupLocationCallback();
        setEventLis();

        // SOS Button Listener
        binding.fabSos.setOnClickListener(v -> {
            if (isSosInProgress) {
                Toast.makeText(this, "SOS already in progress. Please wait.", Toast.LENGTH_SHORT).show();
                return;
            }
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 1);
                return;
            }
            isSosInProgress = true; // Set flag to true
            getLastKnownLocation();
        });

        // Navigate to SOS screen
        binding.btnManageSos.setOnClickListener(v -> startActivity(new Intent(this, SosActivity.class)));
    }

    private void setupLocationRequest() {
        locationRequest = LocationRequest.create();
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        locationRequest.setInterval(10000); // 10 seconds
        locationRequest.setFastestInterval(5000); // 5 seconds
    }

    private void setupLocationCallback() {
        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(@NonNull LocationResult locationResult) {
                Location location = locationResult.getLastLocation();
                if (location != null) {
                    double latitude = location.getLatitude();
                    double longitude = location.getLongitude();
                    sendSOSMessage(latitude, longitude);
                } else {
                    Toast.makeText(Dashboard.this, "Unable to retrieve location. Please try again.", Toast.LENGTH_SHORT).show();
                }
            }
        };
    }

    private void requestLocationUpdates() {
        try {
            fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, null);
        } catch (SecurityException e) {
            Toast.makeText(this, "Permission not granted: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            isSosInProgress = false; // Reset flag on exception
        }
    }

    private void getLastKnownLocation() {
        try {
            fusedLocationClient.getLastLocation()
                    .addOnSuccessListener(location -> {
                        if (location != null) {
                            double latitude = location.getLatitude();
                            double longitude = location.getLongitude();
                            sendSOSMessage(latitude, longitude);
                        } else {
                            Toast.makeText(this, "Location not available. Requesting updates...", Toast.LENGTH_SHORT).show();
                            requestLocationUpdates();
                        }
                        isSosInProgress = false; // Reset flag after location is processed
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(this, "Failed to get location: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        isSosInProgress = false; // Reset flag on failure
                    });
        } catch (SecurityException e) {
            Toast.makeText(this, "Permission not granted: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            isSosInProgress = false; // Reset flag on exception
        }
    }

    private void sendSOSMessage(double latitude, double longitude) {
        String sosMessage = "I need help! My current location is: https://www.google.com/maps/search/?api=1&query=" + latitude + "," + longitude;

        SharedPreferences preferences = getSharedPreferences("SOS_PREFS", MODE_PRIVATE);
        String contact1 = preferences.getString("CONTACT_1", "");
        String contact2 = preferences.getString("CONTACT_2", "");

        if (!contact1.isEmpty()) sendSMS(contact1, sosMessage);
        if (!contact2.isEmpty()) sendSMS(contact2, sosMessage);

        // Reset the flag after sending the messages
        isSosInProgress = false;
    }

    private void sendSMS(String phoneNumber, String message) {
        try {
            SmsManager smsManager = SmsManager.getDefault();
            smsManager.sendTextMessage(phoneNumber, null, message, null, null);
            Toast.makeText(this, "SOS message sent to " + phoneNumber, Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            Toast.makeText(this, "Failed to send SOS message: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void setEventLis() {
        binding.btnRating.setOnClickListener(view -> startActivity(new Intent(this, MainActivity.class)));

        binding.btnEnvImpact.setOnClickListener(view -> startActivity(new Intent(this, EnviromentalImpactActivity.class)));

        binding.btnProfile.setOnClickListener(view -> startActivity(new Intent(this, ProfileActivity.class)));

        // Handle "Find Near Station" module
        binding.btnViewStationsLinearLayout.setOnClickListener(view -> {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "Location permission not granted. Please enable it in settings.", Toast.LENGTH_SHORT).show();
                return;
            }
            fusedLocationClient.getLastLocation()
                    .addOnSuccessListener(location -> {
                        if (location != null) {
                            double latitude = location.getLatitude();
                            double longitude = location.getLongitude();
                            Intent intent = new Intent(this, ALLEVList.class); // Updated activity class
                            intent.putExtra("latitude", latitude);
                            intent.putExtra("longitude", longitude);
                            startActivity(intent);
                        } else {
                            Toast.makeText(this, "Unable to fetch location.", Toast.LENGTH_SHORT).show();
                        }
                    })
                    .addOnFailureListener(e -> Toast.makeText(this, "Failed to fetch location: " + e.getMessage(), Toast.LENGTH_SHORT).show());
        });
    }
}
