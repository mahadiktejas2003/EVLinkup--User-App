package com.example.evchargingfinal;

import android.Manifest;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.telephony.SmsManager;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

public class SosActivity extends AppCompatActivity {
    private EditText contact1, contact2;
    private Button saveContacts, sendSos;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sos);

        contact1 = findViewById(R.id.contact1);
        contact2 = findViewById(R.id.contact2);
        saveContacts = findViewById(R.id.save_contacts);
        sendSos = findViewById(R.id.send_sos);

        // Load saved contacts
        SharedPreferences preferences = getSharedPreferences("SOS_PREFS", MODE_PRIVATE);
        contact1.setText(preferences.getString("CONTACT_1", ""));
        contact2.setText(preferences.getString("CONTACT_2", ""));

        saveContacts.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                SharedPreferences.Editor editor = preferences.edit();
                editor.putString("CONTACT_1", contact1.getText().toString());
                editor.putString("CONTACT_2", contact2.getText().toString());
                editor.apply();
                Toast.makeText(SosActivity.this, "Contacts saved!", Toast.LENGTH_SHORT).show();
            }
        });

        // Updated SOS message to include fallback for location
        sendSos.setOnClickListener(v -> {
            String sosMessage = "I need help! Unable to fetch location. Please contact me immediately.";

            // Fetch contacts from SharedPreferences
            SharedPreferences sosPreferences = getSharedPreferences("SOS_PREFS", MODE_PRIVATE);
            String phone1 = sosPreferences.getString("CONTACT_1", "");
            String phone2 = sosPreferences.getString("CONTACT_2", "");

            if (!phone1.isEmpty()) sendSMS(phone1, sosMessage);
            if (!phone2.isEmpty()) sendSMS(phone2, sosMessage);
        });

        // Request SMS permission
        ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.SEND_SMS}, 1);
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
}
