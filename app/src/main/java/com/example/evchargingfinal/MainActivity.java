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
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.example.evchargingfinal.databinding.ActivityMainBinding;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class MainActivity extends AppCompatActivity implements OnMapReadyCallback {

    private static final String TAG = "MainActivity";
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1;
    private static final double EARTH_RADIUS = 6371000; // meters

    private GoogleMap map;
    private double lati;
    private double longi;
    private ProgressBar progressBar;
    private ResultReceiver resultReceiver;
    private FusedLocationProviderClient fusedLocationClient;
    private LatLng userlocation;
    private List<EVStation> evStations = new ArrayList<>();
    private Map<String, Owner> mp = new HashMap<>();
    private ActivityMainBinding binding;
    private FirebaseAuth firebaseAuth;
    private FirebaseFirestore firebaseFirestore;
    private Executor executor = Executors.newSingleThreadExecutor();
    private Map<String, NearbyStation> nearbyStations = new HashMap<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        resultReceiver = new AddressResultReceiver(new Handler());

        getCurrentLocation1();

        SupportMapFragment mapFragment = (SupportMapFragment)
                getSupportFragmentManager().findFragmentById(R.id.mapview);

        if (mapFragment != null) {
            mapFragment.getMapAsync(MainActivity.this);
        }

        init();
    }

    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        map = googleMap;

        LatLng latLng = new LatLng(18.447265, 73.858926);
        userlocation = latLng;

        map.setOnMarkerClickListener(marker -> {
            if (marker.getTag() != null) {
                if (marker.getTag().equals("current_location")) {
                    return false; // Skip bottom sheet for current location marker
                }
                showBottomSheetDialog(marker.getTag() + "");
            } else {
                showNearbyStationBottomSheet(marker.getTitle());
            }
            return false;
        });
    }

    public void getCurrentLocation1() {
        if (ContextCompat.checkSelfPermission(getApplicationContext(),
                Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(MainActivity.this, new
                            String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    LOCATION_PERMISSION_REQUEST_CODE);
        } else {
            getCurrentLocation();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
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
        LocationRequest locationRequest = new LocationRequest();
        locationRequest.setInterval(10000);
        locationRequest.setFastestInterval(3000);
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);

        if (ActivityCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                &&
                ActivityCompat.checkSelfPermission(this,
                        Manifest.permission.ACCESS_COARSE_LOCATION) !=
                        PackageManager.PERMISSION_GRANTED) {
            return;
        }

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        fusedLocationClient.getLastLocation().addOnSuccessListener(this, location -> {
            if (location != null) {
                lati = location.getLatitude();
                longi = location.getLongitude();
                userlocation = new LatLng(lati, longi);

                // Add current location marker
                Marker currentLocationMarker = map.addMarker(new MarkerOptions()
                        .position(userlocation)
                        .title("Current Location")
                        .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_BLUE)));
                if (currentLocationMarker != null) {
                    currentLocationMarker.setTag("current_location");
                }

                map.addCircle(new CircleOptions()
                        .center(userlocation)
                        .radius(1000)
                        .strokeWidth(2)
                        .strokeColor(Color.BLUE)
                        .fillColor(Color.argb(70, 0, 0, 255)));
                map.addCircle(new CircleOptions()
                        .center(userlocation)
                        .radius(50000)
                        .strokeWidth(2)
                        .strokeColor(Color.BLUE)
                        .fillColor(Color.argb(20, 0, 0, 20)));

                map.moveCamera(CameraUpdateFactory.newLatLng(userlocation));
                map.moveCamera(CameraUpdateFactory.zoomTo(12f));
                map.animateCamera(CameraUpdateFactory.zoomTo(12f));

                getData1();
                fetchNearbyStations();
            }
        });
    }

    private void fetchNearbyStations() {
        String apiKey = getString(R.string.google_maps_api_key);
        int radius = 5000; // 5 km

        List<String> searchQueries = new ArrayList<>();
        searchQueries.add("electric_vehicle_charging_station");
        searchQueries.add("ev_charging");
        searchQueries.add("charging_station");
        searchQueries.add("electric_car_charging");

        for (String query : searchQueries) {
            executor.execute(() -> {
                try {
                    String urlString = "https://maps.googleapis.com/maps/api/place/nearbysearch/json?" +
                            "location=" + lati + "," + longi +
                            "&radius=" + radius +
                            "&keyword=" + query.replace(" ", "+") +
                            "&type=charging_station" +
                            "&key=" + apiKey;

                    URL url = new URL(urlString);
                    HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                    connection.setRequestMethod("GET");

                    BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                    StringBuilder response = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) {
                        response.append(line);
                    }
                    reader.close();

                    JSONObject jsonResponse = new JSONObject(response.toString());
                    JSONArray results = jsonResponse.getJSONArray("results");

                    runOnUiThread(() -> {
                        try {
                            for (int i = 0; i < results.length(); i++) {
                                JSONObject place = results.getJSONObject(i);
                                String placeId = place.getString("place_id");
                                String name = place.getString("name");
                                JSONObject location = place.getJSONObject("geometry").getJSONObject("location");
                                double lat = location.getDouble("lat");
                                double lng = location.getDouble("lng");
                                String address = place.optString("vicinity", "Address not available");

                                if (!nearbyStations.containsKey(placeId)) {
                                    NearbyStation station = new NearbyStation(name, new LatLng(lat, lng), address);
                                    nearbyStations.put(placeId, station);

                                    Marker marker = map.addMarker(new MarkerOptions()
                                            .position(new LatLng(lat, lng))
                                            .title(name)
                                            .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE)));
                                }
                            }
                        } catch (Exception e) {
                            Log.e(TAG, "Error processing nearby results", e);
                        }
                    });
                } catch (Exception e) {
                    Log.e(TAG, "Error fetching nearby stations", e);
                }
            });
        }
    }

    private void showNearbyStationBottomSheet(String stationName) {
        NearbyStation station = null;
        for (Map.Entry<String, NearbyStation> entry : nearbyStations.entrySet()) {
            if (entry.getValue().getName().equals(stationName)) {
                station = entry.getValue();
                break;
            }
        }

        if (station == null) return;

        final BottomSheetDialog bottomSheetDialog = new BottomSheetDialog(MainActivity.this, R.style.BottomSheetDialogTheme);
        View bottomSheetView = LayoutInflater.from(MainActivity.this)
                .inflate(R.layout.layout_bottom_sheet, findViewById(R.id.bottomsheetcontainer));

        TextView stationname = bottomSheetView.findViewById(R.id.stationname);
        TextView price = bottomSheetView.findViewById(R.id.price);
        TextView remainingEnergy = bottomSheetView.findViewById(R.id.remainingenergy);
        TextView address = bottomSheetView.findViewById(R.id.address);
        TextView avg = bottomSheetView.findViewById(R.id.avgr);
        Button direction = bottomSheetView.findViewById(R.id.btnUpdate);
        Button book = bottomSheetView.findViewById(R.id.book);

        // Hide book button for nearby stations
        book.setVisibility(View.GONE);

        stationname.setText(station.getName());
        address.setText(station.getAddress());
        price.setText("N/A");
        remainingEnergy.setText("N/A");
        avg.setText("N/A");

        final NearbyStation finalStation = station;
        direction.setOnClickListener(v -> {
            String origin = lati + ", " + longi;
            String destination = finalStation.getLocation().latitude + ", " + finalStation.getLocation().longitude;
            Uri gmmIntentUri = Uri.parse("http://maps.google.com/maps?saddr=" + origin + "&daddr=" + destination);
            Intent mapIntent = new Intent(Intent.ACTION_VIEW, gmmIntentUri);
            mapIntent.setPackage("com.google.android.apps.maps");
            if (mapIntent.resolveActivity(getPackageManager()) != null) {
                startActivity(mapIntent);
            }
        });

        bottomSheetDialog.setContentView(bottomSheetView);
        bottomSheetDialog.show();
    }

    private class AddressResultReceiver extends ResultReceiver {
        public AddressResultReceiver(Handler handler) {
            super(handler);
        }

        @Override
        protected void onReceiveResult(int resultCode, Bundle resultData) {
            progressBar.setVisibility(View.GONE);
            if (resultCode == Constants.SUCCESS_RESULT) {
                // Handle success
            } else {
                // Handle failure
            }
        }
    }

    public void showBottomSheetDialog(String tag) {
        final BottomSheetDialog bottomSheetDialog1 = new BottomSheetDialog(MainActivity.this, R.style.BottomSheetDialogTheme);

        View bottomSheetView = LayoutInflater.from(MainActivity.this).inflate(R.layout.layout_bottom_sheet, findViewById(R.id.bottomsheetcontainer));
        Owner allowner = mp.get(tag);

        if (allowner == null) return;

        TextView stationname = bottomSheetView.findViewById(R.id.stationname);
        TextView price = bottomSheetView.findViewById(R.id.price);
        TextView remainingEnergy = bottomSheetView.findViewById(R.id.remainingenergy);
        TextView address = bottomSheetView.findViewById(R.id.address);
        TextView avg = bottomSheetView.findViewById(R.id.avgr);
        avg.setText(allowner.getAvg_rating() + "");

        price.setText(allowner.getPrice() + "");
        address.setText(getAddress(allowner.getOwner_location().getLatitude(), allowner.getOwner_location().getLongitude()));
        stationname.setText(allowner.getOwner_name());

        Button direction = bottomSheetView.findViewById(R.id.btnUpdate);
        Button book = bottomSheetView.findViewById(R.id.book);

        book.setOnClickListener(v -> {
            Log.d(TAG, "Book button clicked for owner: " + allowner.getOwner_email());
            firebaseFirestore
                    .collection("Owner")
                    .document(allowner.getOwner_email())
                    .collection("EV_Station")
                    .get()
                    .addOnSuccessListener(snaps -> {
                        if (snaps == null) return;
                        evStations.clear();
                        evStations.addAll(snaps.toObjects(EVStation.class));
                        Log.d(TAG, "Fetched EV stations: " + evStations.size());
                        for (EVStation station : evStations) {
                            Log.d(TAG, "Station: " + station.getEvs_id() + ", Energy: " + station.getEvs_energy());
                            if (station.getEvs_energy() > (allowner.getPrice() * 30)) {
                                Intent intent = new Intent(MainActivity.this, BookSlot.class);
                                intent.putExtra("owner_email", allowner.getOwner_email());
                                intent.putExtra("price", allowner.getPrice() + "");
                                intent.putExtra("owner_name", allowner.getOwner_name());
                                startActivity(intent);
                            }
                        }
                    })
                    .addOnFailureListener(e -> {
                        Log.e(TAG, "Error fetching EV stations:", e);
                        Toast.makeText(MainActivity.this, e.getMessage(), Toast.LENGTH_SHORT).show();
                    });
        });

        direction.setOnClickListener(v -> {
            String origin = lati + ", " + longi;
            String destination = allowner.getOwner_location().getLatitude() + ", " + allowner.getOwner_location().getLongitude();
            Uri gmmIntentUri = Uri.parse("http://maps.google.com/maps?saddr=" + origin + "&daddr=" + destination);
            Intent mapIntent = new Intent(Intent.ACTION_VIEW, gmmIntentUri);
            mapIntent.setPackage("com.google.android.apps.maps");
            if (mapIntent.resolveActivity(getPackageManager()) != null) {
                startActivity(mapIntent);
            }
        });

        bottomSheetDialog1.setContentView(bottomSheetView);
        bottomSheetDialog1.show();
    }

    private String getAddress(double lati, double longi) {
        String add = "";
        try {
            Geocoder geocoder = new Geocoder(this, Locale.getDefault());
            List<Address> addresses = geocoder.getFromLocation(lati, longi, 1);
            add = addresses.get(0).getAddressLine(0);
        } catch (Exception e) {
            Log.e(TAG, "Error getting address", e);
            Toast.makeText(this, e.getMessage(), Toast.LENGTH_SHORT).show();
        }
        return add;
    }

    private void getData1() {
        firebaseFirestore.collection("Owner")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                        Owner owner = doc.toObject(Owner.class);
                        LatLng latLng2 = new LatLng(owner.getOwner_location().getLatitude(), owner.getOwner_location().getLongitude());
                        double red = calculateDistance(lati, longi, latLng2.latitude, latLng2.longitude);
                        if (red < 500000000) {
                            mp.put(owner.getOwner_email(), owner);
                            Marker marker = map.addMarker(new MarkerOptions()
                                    .position(latLng2)
                                    .title(owner.getEv_station_name())
                                    .icon(BitmapDescriptorFactory.fromResource(R.drawable.custom)));
                            marker.setTag(doc.getId());
                        }
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error fetching owners", e);
                    Toast.makeText(MainActivity.this, e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void init() {
        firebaseAuth = FirebaseAuth.getInstance();
        firebaseFirestore = FirebaseFirestore.getInstance();
    }

    public static double calculateDistance(double lat1, double lon1, double lat2, double lon2) {
        double lat1Radians = Math.toRadians(lat1);
        double lon1Radians = Math.toRadians(lon1);
        double lat2Radians = Math.toRadians(lat2);
        double lon2Radians = Math.toRadians(lon2);

        double deltaLat = lat2Radians - lat1Radians;
        double deltaLon = lon2Radians - lon1Radians;

        double a = Math.pow(Math.sin(deltaLat / 2), 2) +
                Math.cos(lat1Radians) * Math.cos(lat2Radians) *
                        Math.pow(Math.sin(deltaLon / 2), 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return EARTH_RADIUS * c;
    }

    private class NearbyStation {
        private String name;
        private LatLng location;
        private String address;

        public NearbyStation(String name, LatLng location, String address) {
            this.name = name;
            this.location = location;
            this.address = address;
        }

        public String getName() {
            return name;
        }

        public LatLng getLocation() {
            return location;
        }

        public String getAddress() {
            return address;
        }
    }
}