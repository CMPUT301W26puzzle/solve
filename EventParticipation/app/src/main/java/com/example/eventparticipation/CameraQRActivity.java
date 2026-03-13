package com.example.eventparticipation;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.appcompat.app.AppCompatActivity;

import com.journeyapps.barcodescanner.ScanContract;
import com.journeyapps.barcodescanner.ScanOptions;
/**
 * Activity for scanning a qr code with the camera
 *
 * <p>Relevant user stories:</p>
 * <ul>
 *     <li>01.06.01 - As an entrant, I want to view event details within the app by scanning the promotional QR code</li>
 * </ul>
 */
public class CameraQRActivity extends AppCompatActivity {
    private ActivityResultLauncher<ScanOptions> scanner;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        ScanOptions options = new ScanOptions();
        options.setPrompt("Scan Event QR Code");
        options.setBeepEnabled(true);

        scanner = registerForActivityResult(
                new ScanContract(),
                result -> {
                    if(result.getContents() == null) {
                        Toast.makeText(this, "Scan cancelled", Toast.LENGTH_SHORT).show();
                        finish();
                        return;
                    }
                    openEvent(result.getContents());
                }
        );
        scanner.launch(options);
    }

    private void openEvent(String eventId) {

        if (eventId == null || eventId.isEmpty()) {
            Toast.makeText(this, "Invalid QR Code", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        Intent intent = new Intent(this, EntrantEventDetailActivity.class);
        intent.putExtra("EVENT_ID", eventId);

        startActivity(intent);
        finish();
    }
}
