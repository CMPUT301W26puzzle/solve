package com.example.eventparticipation;

import android.os.Bundle;
import android.util.TypedValue;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

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
 * <p>This activity loads entrant locations from the waitlist subcollection
 * and displays them as markers on Google Maps.</p>
 *
 * <p>Relevant user story:</p>
 * <ul>
 *     <li>US 02.02.02 - View entrant join locations on a map</li>
 * </ul>
 */
public class WaitlistMapActivity extends AppCompatActivity implements OnMapReadyCallback {

    /** Google Map instance after initialization. */
    private GoogleMap mMap;

    /** Firestore database instance. */
    private FirebaseFirestore db;

    /** Organizer id passed into this activity. */
    private String organizerId;

    /** Event id passed into this activity. */
    private String eventId;

    /**
     * Initializes the activity, validates extras, and requests the map asynchronously.
     *
     * @param savedInstanceState previously saved state bundle, or null
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_waitlist_map);

        applyWindowInsets();
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
     * Applies status bar insets to the toolbar so that the toolbar content
     * stays below the system status bar on edge-to-edge devices.
     */
    private void applyWindowInsets() {
        Toolbar toolbar = findViewById(R.id.toolbar);

        final int originalPaddingLeft = toolbar.getPaddingLeft();
        final int originalPaddingTop = toolbar.getPaddingTop();
        final int originalPaddingRight = toolbar.getPaddingRight();
        final int originalPaddingBottom = toolbar.getPaddingBottom();
        final int originalToolbarHeight = getToolbarHeight();

        ViewCompat.setOnApplyWindowInsetsListener(toolbar, (view, windowInsets) -> {
            Insets insets = windowInsets.getInsets(WindowInsetsCompat.Type.statusBars());

            view.setPadding(
                    originalPaddingLeft,
                    originalPaddingTop + insets.top,
                    originalPaddingRight,
                    originalPaddingBottom
            );

            ViewGroup.LayoutParams layoutParams = view.getLayoutParams();
            layoutParams.height = originalToolbarHeight + insets.top;
            view.setLayoutParams(layoutParams);

            return windowInsets;
        });
    }

    /**
     * Returns the default toolbar height from the current theme.
     *
     * @return toolbar height in pixels
     */
    private int getToolbarHeight() {
        TypedValue typedValue = new TypedValue();
        if (getTheme().resolveAttribute(android.R.attr.actionBarSize, typedValue, true)) {
            return TypedValue.complexToDimensionPixelSize(
                    typedValue.data,
                    getResources().getDisplayMetrics()
            );
        }
        return (int) (56 * getResources().getDisplayMetrics().density);
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
     * Handles the toolbar up button.
     *
     * @return always returns {@code true}
     */
    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }

    /**
     * Called when the map is ready for use.
     *
     * @param googleMap initialized Google Map instance
     */
    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        mMap = googleMap;
        loadMarkers();
    }

    /**
     * Loads waitlist entrant locations from Firestore and places markers on the map.
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