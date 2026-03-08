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
 * Organizer screen that displays a map of where entrants joined an event waitlist.
 *
 * <p>This activity fulfills US 02.02.02 by loading each waitlist entry's stored
 * geographic coordinates and placing a marker on Google Maps.</p>
 */
public class WaitlistMapActivity extends AppCompatActivity implements OnMapReadyCallback {

    /** Google Map instance once ready. */
    private GoogleMap mMap;

    /** Firestore database reference. */
    private FirebaseFirestore db;

    /** Organizer id owning the event. */
    private String organizerId;

    /** Event id whose waitlist locations are shown. */
    private String eventId;

    /**
     * Initializes the activity, validates extras, and requests the map asynchronously.
     *
     * @param savedInstanceState previously saved state bundle
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_waitlist_map);

        setupToolbar();

        organizerId = getIntent().getStringExtra("ORGANIZER_ID");
        eventId = getIntent().getStringExtra("EVENT_ID");

        if (organizerId == null || organizerId.trim().isEmpty()) {
            Toast.makeText(this, "Missing ORGANIZER_ID", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        if (eventId == null || eventId.trim().isEmpty()) {
            Toast.makeText(this, "Missing EVENT_ID", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        db = FirebaseFirestore.getInstance();

        SupportMapFragment mapFragment =
                (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);

        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
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

    /**
     * Handles toolbar up navigation.
     *
     * @return always returns {@code true}
     */
    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }

    /**
     * Called when the map instance is ready for marker operations.
     *
     * @param googleMap ready GoogleMap instance
     */
    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        mMap = googleMap;
        loadMarkers();
    }

    /**
     * Loads waitlist entries from Firestore and adds a marker for each valid join location.
     */
    private void loadMarkers() {
        db.collection("organizers")
                .document(organizerId)
                .collection("events")
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
