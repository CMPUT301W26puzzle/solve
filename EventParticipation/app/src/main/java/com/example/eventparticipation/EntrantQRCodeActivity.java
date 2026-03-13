package com.example.eventparticipation;

import android.content.Intent;
import android.os.Bundle;

import com.google.android.material.button.MaterialButton;

/**
 * Activity for the scanning qr code screen
 *
 * <p>Relevant user stories:</p>
 * <ul>
 *     <li>US 01.06.01 - As an entrant, I want to view event details within the app by scanning the promotional QR code.</li>
 * </ul>
 * Also contains information on how to correctly scan QR codes
 */
public class EntrantQRCodeActivity extends BaseEntrantActivity {

    private MaterialButton scanBtn;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_scan_qr);

        setupBottomNav(R.id.nav_scan);
        initViews();

        scanBtn.setOnClickListener(v -> openCamera());
    }

    private void initViews() {
        scanBtn = findViewById(R.id.btnScanQr);
    }

    private void openCamera() {
        Intent intent = new Intent(this, CameraQRActivity.class);
        startActivity(intent);
    }
}