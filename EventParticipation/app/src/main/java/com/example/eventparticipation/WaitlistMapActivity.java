package com.example.eventparticipation;

import android.os.Bundle;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.firebase.firestore.FirebaseFirestore;

/**
 * Organizer map screen that displays entrant join locations for a specific event.
 *
 * <p>This activity loads entrant geolocation data from Firestore and places markers
 * on a Google Map for each entrant who joined the waitlist with location enabled.</p>
 *
 * <p>Relevant user story:</p>
 * <ul>
 *     <li>US 02.02.02 - View entrant locations on a map</li>
 * </ul>
 */
public class WaitlistMapActivity extends AppCompatActivity implements OnMapReadyCallback {

    /** Firestore event id passed into this activity. */
    private String eventId;

    /** Firestore database instance. */
    private FirebaseFirestore db;

    /** Google Map instance once ready. */
    private GoogleMap mMap;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_waitlist_map);

        eventId = getIntent().getStringExtra("EVENT_ID");

        if (eventId == null || eventId.trim().isEmpty()) {
            Toast.makeText(this, "Missing EVENT_ID", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        db = FirebaseFirestore.getInstance();

        setupToolbar();

        SupportMapFragment mapFragment =
                (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);

        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        } else {
            Toast.makeText(this, "Map failed to load", Toast.LENGTH_LONG).show();
            finish();
        }
    }

    /**
     * Configures the toolbar and enables back navigation.
     */
    private void setupToolbar() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
        }

        toolbar.setNavigationOnClickListener(v -> finish());
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }

    /**
     * Called when the Google Map is ready to be used.
     *
     * @param googleMap ready map instance
     */
    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        mMap = googleMap;
        loadMarkers();
    }

    /**
     * Loads waitlist entrant locations from Firestore and adds map markers.
     */
    private void loadMarkers() {
        db.collection("events")
                .document(eventId)
                .collection("waitlist")
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    if (querySnapshot.isEmpty()) {
                        Toast.makeText(this, "No entrants found in waitlist", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    boolean moved = false;

                    for (var doc : querySnapshot.getDocuments()) {
                        com.google.firebase.firestore.GeoPoint point = doc.getGeoPoint("joinedLocation");
                        String name = doc.getString("entrantName");
                        String address = doc.getString("joinedAddress");

                        if (point == null) {
                            continue;
                        }

                        LatLng location = new LatLng(point.getLatitude(), point.getLongitude());

                        mMap.addMarker(new MarkerOptions()
                                .position(location)
                                .title(name != null ? name : "Entrant")
                                .snippet(address != null ? address : ""));

                        if (!moved) {
                            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(location, 11f));
                            moved = true;
                        }
                    }

                    if (!moved) {
                        Toast.makeText(this, "No valid joinedLocation found", Toast.LENGTH_LONG).show();
                    }
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Failed to load map data: " + e.getMessage(), Toast.LENGTH_LONG).show());
    }
}