package com.example.evchargingfinal.envo_impact;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;

import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.example.evchargingfinal.EnvTrackingAdapter;
import com.example.evchargingfinal.R;
import com.example.evchargingfinal.User;
import com.example.evchargingfinal.databinding.ActivityEnviromentalImpactBinding;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;


import java.io.File;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class EnviromentalImpactActivity extends AppCompatActivity {

    private int points, energy, petrol_disel;
    private List<User> users;
    private User user;
    private FirebaseAuth firebaseAuth;
    private FirebaseFirestore firebaseFirestore;
    private ActivityEnviromentalImpactBinding binding;
    private EnvTrackingAdapter dishAdapter;
    private LinearLayoutManager layoutManager;
    private List<User> ratings;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityEnviromentalImpactBinding.inflate(getLayoutInflater());

        init();

        setEventLis();

        getUserData();

        setContentView(binding.getRoot());
    }


    private void sortUsers() {
        Collections.sort(users, new Comparator<User>() {
            @Override
            public int compare(User user1, User user2) {
                // Compare points in descending order
                return Integer.compare(user2.getPoints(), user1.getPoints());
            }
        });
    }

    private void getAllUsers() {
        firebaseFirestore
                .collection("User")
                .get()
                .addOnSuccessListener(new OnSuccessListener<QuerySnapshot>() {
                    @Override
                    public void onSuccess(QuerySnapshot snaps) {
                        users.addAll(snaps.toObjects(User.class));
                        sortUsers();

                        dishAdapter = new EnvTrackingAdapter(users, EnviromentalImpactActivity.this);
                        binding.rvData.setAdapter(dishAdapter);
//                        evStations.addAll(snaps.getDocuments());
//                        addLearderboard();
                    }
                }).addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Toast.makeText(EnviromentalImpactActivity.this, e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void setUserData(){
        binding.tvEnergy.setText(Integer.toString(energy));
        binding.tvPetrolDisel.setText(Integer.toString(petrol_disel));
        binding.tvPoints.setText(Integer.toString(points));
    }

    private void getUserData() {
        Log.d("TAG", "getUserData: " + firebaseAuth.getCurrentUser().toString());
        firebaseFirestore
                .collection("User")
                .document(firebaseAuth.getCurrentUser().getEmail())
                .get()
                .addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
                    @Override
                    public void onSuccess(DocumentSnapshot doc) {
                        if (doc==null) return;

                        user = doc.toObject(User.class);

                        Log.d("TAG", "onSuccess: " + user.getUser_id());

                        energy = user.getEnergy_used();
                        petrol_disel = energy * 7; //can require this much fuel if not used ee
                        points = energy * 2;

                        setUserData();
                        getAllUsers();
                    }
                }).addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Toast.makeText(EnviromentalImpactActivity.this, e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void setEventLis() {
        binding.btnShare.setOnClickListener(v -> shareScreenshot());

    }
    private void shareScreenshot() {
        try {
            // Capture the screenshot
            View rootView = getWindow().getDecorView().getRootView();
            rootView.setDrawingCacheEnabled(true);
            Bitmap bitmap = Bitmap.createBitmap(rootView.getDrawingCache());
            rootView.setDrawingCacheEnabled(false);

            // Save the screenshot to a file
            File screenshotFile = new File(getExternalFilesDir(Environment.DIRECTORY_PICTURES), "env_impact_screenshot.png");
            FileOutputStream fos = new FileOutputStream(screenshotFile);
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, fos);
            fos.flush();
            fos.close();

            // Use FileProvider to get a content URI
            Uri screenshotUri = androidx.core.content.FileProvider.getUriForFile(
                    this,
                    getApplicationContext().getPackageName() + ".provider",
                    screenshotFile
            );

            // Share the screenshot
            Intent shareIntent = new Intent(Intent.ACTION_SEND);
            shareIntent.setType("image/png");
            shareIntent.putExtra(Intent.EXTRA_STREAM, screenshotUri);
            shareIntent.putExtra(Intent.EXTRA_TEXT, "Check out my Environmental Impact achievements!");
            shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            startActivity(Intent.createChooser(shareIntent, "Share via"));
        } catch (Exception e) {
            Toast.makeText(this, "Failed to share screenshot: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void init() {
        users = new ArrayList<>();

        layoutManager = new LinearLayoutManager(this);
//        layoutManager.setOrientation(RecyclerView.VERTICAL);
        binding.rvData.setLayoutManager(layoutManager);
//        ratings = new ArrayList<>();

        points = energy = petrol_disel = 0;

        firebaseAuth = FirebaseAuth.getInstance();
        firebaseFirestore = FirebaseFirestore.getInstance();
    }
}