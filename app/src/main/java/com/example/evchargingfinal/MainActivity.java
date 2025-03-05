package com.example.evchargingfinal;



import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.ResultReceiver;
import android.util.Log;
import android.util.Pair;
import android.view.LayoutInflater;
import android.view.View;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.evchargingfinal.databinding.ActivityMainBinding;
import com.google.android.gms.internal.maps.zzad;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;


import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import org.json.JSONArray;
import org.json.JSONObject;
import org.w3c.dom.Text;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public class MainActivity extends AppCompatActivity implements OnMapReadyCallback {

    GoogleMap map;
    double longi;
    double lati;
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1;
    ProgressBar progressBar;
    ResultReceiver resultReceiver;
    Owner owner;
    FusedLocationProviderClient fusedLocationClient;
    LatLng userlocation,dlocation;
    private MarkerOptions place1, place2;
    Button getDirection;
    private Polyline currentPolyline;
    List<EVStation> evStations = new ArrayList<>();
    Map<String,Owner> mp = new HashMap<>();
    Map<String,EVStation> mpevstation = new HashMap<>();
    private static final double EARTH_RADIUS = 6371000; // meters



//    private ActivityProfileBinding binding;
    public ActivityMainBinding binding;
//
//    Owner owner;
    private FirebaseAuth firebaseAuth;
    private FirebaseFirestore firebaseFirestore;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Initialize Firebase Firestore
        firebaseFirestore = FirebaseFirestore.getInstance();

        // Initialize Google Maps
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.mapview);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }

        // Initialize FusedLocationProviderClient
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        // Initialize ProgressBar
        progressBar = findViewById(R.id.progressBar);

        // Check location permissions
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, LOCATION_PERMISSION_REQUEST_CODE);
        } else {
            getCurrentLocation();
        }
    }

    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        map = googleMap;

        // Enable My Location layer
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            map.setMyLocationEnabled(true);
        }

        // Set marker click listener
        map.setOnMarkerClickListener(marker -> {
            showBottomSheetDialog(marker);
            return false; // Allow default behavior
        });
    }

    public void getCurrentLocation1()
    {


        if (ContextCompat.checkSelfPermission(getApplicationContext(),
                android.Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(MainActivity.this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    LOCATION_PERMISSION_REQUEST_CODE);
        } else {
//            Toast.makeText(this, "Vasudev", Toast.LENGTH_SHORT).show();
            getCurrentLocation();
        }
        LocationRequest locationRequest = new LocationRequest();
        locationRequest.setInterval(10000);
        locationRequest.setFastestInterval(3000);
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }

        fusedLocationClient.getLastLocation().addOnSuccessListener(this, location -> {
            if (location != null) {
                lati = location.getLatitude();
                longi = location.getLongitude();
                LatLng userLocation = new LatLng(lati, longi);

                // Move camera to user's location
                map.moveCamera(CameraUpdateFactory.newLatLngZoom(userLocation, 12));

                // Fetch and display registered stations
                fetchRegisteredStations(lati, longi);

                // Fetch and display non-registered stations
                fetchNonRegisteredStations(lati, longi);
            }
        });

    }
    private void fetchRegisteredStations(double lat, double lng) {
        firebaseFirestore.collection("Owner")
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    for (QueryDocumentSnapshot doc : querySnapshot) {
                        Owner owner = doc.toObject(Owner.class);
                        LatLng stationLocation = new LatLng(owner.getOwner_location().getLatitude(), owner.getOwner_location().getLongitude());

                        // Add marker for registered station
                        Marker marker = map.addMarker(new MarkerOptions()
                                .position(stationLocation)
                                .title(owner.getEv_station_name())
                                .snippet("Registered Station")
                                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN)));

                        // Tag the marker with the document ID
                        marker.setTag(doc.getId());
                    }
                })
                .addOnFailureListener(e -> Toast.makeText(this, "Error fetching registered stations", Toast.LENGTH_SHORT).show());
    }

    Set<String> addedLocations = new HashSet<>();

    private void fetchNonRegisteredStations(double lat, double lng) {
        String apiKey = "AIzaSyAEVQ0v49sMbRO23umRRr2PoCGD_DHFkHo";
        String[] keywords = {"electric_vehicle_charging_station", "ev_charging", "charging_station", "electric_car_charging"};

        for (String keyword : keywords) {
            String url = "https://maps.googleapis.com/maps/api/place/nearbysearch/json?" +
                    "location=" + lat + "," + lng +
                    "&radius=5000" +
                    "&type=electric_vehicle_charging_station" +
                    "&keyword=" + keyword +
                    "&key=" + apiKey;

            new Thread(() -> {
                try {
                    URL obj = new URL(url);
                    HttpURLConnection con = (HttpURLConnection) obj.openConnection();
                    con.setRequestMethod("GET");

                    BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()));
                    StringBuilder response = new StringBuilder();
                    String inputLine;

                    while ((inputLine = in.readLine()) != null) {
                        response.append(inputLine);
                    }
                    in.close();

                    JSONObject jsonResponse = new JSONObject(response.toString());
                    JSONArray results = jsonResponse.getJSONArray("results");

                    for (int i = 0; i < results.length(); i++) {
                        JSONObject station = results.getJSONObject(i);
                        JSONObject geometry = station.getJSONObject("geometry");
                        JSONObject location = geometry.getJSONObject("location");

                        double stationLat = location.getDouble("lat");
                        double stationLng = location.getDouble("lng");
                        LatLng stationLocation = new LatLng(stationLat, stationLng);

                        String stationName = station.optString("name", "Unnamed Station");

                        // Create a unique key for the location
                        String locationKey = stationLat + "," + stationLng;

                        // Check if the location is already added
                        if (!addedLocations.contains(locationKey)) {
                            addedLocations.add(locationKey);

                            runOnUiThread(() -> {
                                Marker marker = map.addMarker(new MarkerOptions()
                                        .position(stationLocation)
                                        .title(stationName)
                                        .snippet("Non-Registered Station") // Set snippet to identify
                                        .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_BLUE)));
                                // Store coordinates in the tag as a string
                                marker.setTag(stationLat + "," + stationLng);
                            });
                        }
                    }
                } catch (Exception e) {
                    Log.e("MainActivity", "Error fetching non-registered stations", e);
                }
            }).start();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE && grantResults.length > 0) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                getCurrentLocation();
            } else {
                Toast.makeText(this, "Permission is denied!", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void getCurrentLocation() {
        progressBar.setVisibility(View.VISIBLE); // Show ProgressBar

        LocationRequest locationRequest = null;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
            locationRequest = new LocationRequest();
        }

        if (locationRequest != null) {
            locationRequest.setInterval(10000);
            locationRequest.setFastestInterval(3000);
            locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);

            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                    ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                progressBar.setVisibility(View.GONE); // Hide ProgressBar if permissions are missing
                return;
            }

            LocationServices.getFusedLocationProviderClient(MainActivity.this)
                    .requestLocationUpdates(locationRequest, new LocationCallback() {
                        @Override
                        public void onLocationResult(LocationResult locationResult) {
                            super.onLocationResult(locationResult);
                            LocationServices.getFusedLocationProviderClient(getApplicationContext())
                                    .removeLocationUpdates(this);

                            if (locationResult != null && locationResult.getLocations().size() > 0) {
                                int latestlocIndex = locationResult.getLocations().size() - 1;
                                lati = locationResult.getLocations().get(latestlocIndex).getLatitude();
                                longi = locationResult.getLocations().get(latestlocIndex).getLongitude();
                                LatLng userLocation = new LatLng(lati, longi);

                                map.addMarker(new MarkerOptions().position(userLocation).title("I am here"));
                                map.moveCamera(CameraUpdateFactory.newLatLngZoom(userLocation, 12));

                                fetchRegisteredStations(lati, longi);
                                fetchNonRegisteredStations(lati, longi);
                            }

                            progressBar.setVisibility(View.GONE); // Hide ProgressBar after fetching data
                        }
                    }, Looper.getMainLooper());
        }
    }
    private class AddressResultReceiver extends ResultReceiver {
        public AddressResultReceiver(Handler handler) {
            super(handler);
        }

        @Override
        protected void onReceiveResult(int resultCode, Bundle resultData) {
            super.onReceiveResult(resultCode, resultData);
            progressBar.setVisibility(View.GONE);
            if (resultCode == Constants.SUCCESS_RESULT) {

            } else {
//                Toast.makeText(MainActivity.this, resultData.getString(Constants.RESULT_DATA_KEY), Toast.LENGTH_SHORT).show();
            }

        }


    }

    public void showBottomSheetDialog(Marker marker) {
        final BottomSheetDialog bottomSheetDialog1 = new BottomSheetDialog(
                MainActivity.this, R.style.BottomSheetDialogTheme
        );

        View bottomSheetView = getLayoutInflater().inflate(
                R.layout.layout_bottom_sheet,
                findViewById(R.id.bottomsheetcontainer)
        );

        TextView stationName = bottomSheetView.findViewById(R.id.stationname);
        TextView price = bottomSheetView.findViewById(R.id.price);
        TextView remainingEnergy = bottomSheetView.findViewById(R.id.remainingenergy);
        TextView address = bottomSheetView.findViewById(R.id.address);
        Button direction = bottomSheetView.findViewById(R.id.btnUpdate);
        Button book = bottomSheetView.findViewById(R.id.book);

        String snippet = marker.getSnippet();
        String tag = marker.getTag() != null ? marker.getTag().toString() : "";
        String title = marker.getTitle();

        // Check if the station is registered
        if ("Registered Station".equals(snippet)) {
            // Fetch registered station details from Firestore
            firebaseFirestore.collection("Owner").document(tag).get()
                    .addOnSuccessListener(documentSnapshot -> {
                        Owner owner = documentSnapshot.toObject(Owner.class);
                        if (owner != null) {
                            // Populate registered station data
                            stationName.setText(owner.getEv_station_name());
                            price.setText(String.valueOf(owner.getPrice()));
                            address.setText(getAddress(owner.getOwner_location().getLatitude(), owner.getOwner_location().getLongitude()));

                            // Directions for registered
                            direction.setOnClickListener(v -> {
                                String origin = lati + "," + longi;
                                String dest = owner.getOwner_location().getLatitude() + "," + owner.getOwner_location().getLongitude();
                                openGoogleMaps(origin, dest);
                            });
                        }
                    })
                    .addOnFailureListener(e -> handleUnregisteredStation(marker, bottomSheetView, stationName, address, direction));
        } else {
            // Handle unregistered station
            handleUnregisteredStation(marker, bottomSheetView, stationName, address, direction);
        }

        bottomSheetDialog1.setContentView(bottomSheetView);
        bottomSheetDialog1.show();
    }

    private void handleUnregisteredStation(Marker marker, View bottomSheetView, TextView stationName, TextView address, Button direction) {
        String tag = marker.getTag().toString();
        String[] coords = tag.split(",");
        if (coords.length == 2) {
            double lat = Double.parseDouble(coords[0]);
            double lng = Double.parseDouble(coords[1]);

            // Set station name from marker's title
            stationName.setText(marker.getTitle());
            address.setText(getAddress(lat, lng));

            // Directions for unregistered
            direction.setOnClickListener(v -> {
                String origin = lati + "," + longi;
                String dest = lat + "," + lng;
                openGoogleMaps(origin, dest);
            });
        }
        // Hide price and disable booking
        bottomSheetView.findViewById(R.id.price).setVisibility(View.GONE);
        bottomSheetView.findViewById(R.id.book).setVisibility(View.GONE);
    }

    private void openGoogleMaps(String origin, String destination) {
        Uri gmmIntentUri = Uri.parse("http://maps.google.com/maps?saddr=" + origin + "&daddr=" + destination);
        Intent mapIntent = new Intent(Intent.ACTION_VIEW, gmmIntentUri);
        mapIntent.setPackage("com.google.android.apps.maps");
        if (mapIntent.resolveActivity(getPackageManager()) != null) {
            startActivity(mapIntent);
        }
    }


    private String getAddress(double lati, double longi) {
        String add = "";
        try {
            Geocoder geocoder = new Geocoder(this, Locale.getDefault());
            List<Address> addresses = geocoder.getFromLocation(lati, longi, 1);
            if (addresses != null && !addresses.isEmpty()) {
                add = addresses.get(0).getAddressLine(0);
            }
        } catch (Exception e) {
            Toast.makeText(this, e.getMessage(), Toast.LENGTH_SHORT).show();
        }
        return add;
    }




    private void getData1(){
        firebaseFirestore.collection("Owner")
                .get()
                .addOnSuccessListener(new OnSuccessListener<QuerySnapshot>() {
                    @Override
                    public void onSuccess(QuerySnapshot queryDocumentSnapshots) {
                        for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                            Owner owner = doc.toObject(Owner.class);
                            LatLng latLng2 = new LatLng(owner.getOwner_location().getLatitude(), owner.getOwner_location().getLongitude());
//                            map.addMarker(new MarkerOptions().position(latLng2).title(owner.getEv_station_name()).icon(BitmapDescriptorFactory.fromResource(R.drawable.carev)));
//                            Marker marker = new Marker((zzad) new MarkerOptions());


                            TextView label  = findViewById(R.id.label);
//                          label.setText(""+doc);
//                            Toast.makeText(MainActivity.this, ""+doc.get("EV_Station"), Toast.LENGTH_SHORT).show();

                            double red = calculateDistance(lati,longi,latLng2.latitude,latLng2.longitude);
//                            Toast.makeText(MainActivity.this, "REdius :"+red, Toast.LENGTH_SHORT).show();

                            if(red<50000)
                            {
                                mp.put(owner.getOwner_email(),owner);
                                Marker marker = map.addMarker(new MarkerOptions()
                                        .position(latLng2)
                                        .title(owner.getEv_station_name())
                                        .icon(BitmapDescriptorFactory.fromResource(R.drawable.custom)));

                                // Tag the marker with the document ID to identify it later
                                marker.setTag(doc.getId());
                            }





                        }
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
//                        Toast.makeText(MainActivity.this, e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });








    }

    private void init(){
        firebaseAuth = FirebaseAuth.getInstance();
        firebaseFirestore = FirebaseFirestore.getInstance();

}

    public static double calculateDistance(double lat1, double lon1, double lat2, double lon2) {
        // Convert latitude and longitude from degrees to radians
        double lat1Radians = Math.toRadians(lat1);
        double lon1Radians = Math.toRadians(lon1);
        double lat2Radians = Math.toRadians(lat2);
        double lon2Radians = Math.toRadians(lon2);

        // Compute the differences between the latitudes and longitudes
        double deltaLat = lat2Radians - lat1Radians;
        double deltaLon = lon2Radians - lon1Radians;

        // Compute the distance using Haversine formula
        double a = Math.pow(Math.sin(deltaLat / 2), 2) +
                Math.cos(lat1Radians) * Math.cos(lat2Radians) *
                        Math.pow(Math.sin(deltaLon / 2), 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        double distance = EARTH_RADIUS * c;

        return distance;
    }









}