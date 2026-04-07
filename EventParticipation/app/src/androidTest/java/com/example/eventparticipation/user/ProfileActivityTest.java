package com.example.eventparticipation.user;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import android.content.Context;
import android.content.Intent;
import android.os.SystemClock;

import androidx.test.core.app.ActivityScenario;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.example.eventparticipation.R;
import com.example.eventparticipation.universal.SessionManager;
import com.google.android.gms.tasks.Tasks;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * UI and Integration tests for {@link ProfileActivity}.
 *
 * <p><b>Purpose & Role:</b> This test class verifies the UI validation, profile loading,
 * profile saving, and account deletion behaviors of the Entrant's profile screen. It uses
 * {@link ActivityScenario} to launch the Activity in isolation and direct Firestore calls
 * to verify backend data state modifications.</p>
 *
 * <p>Relevant User Stories:</p>
 * <ul>
 * <li>US 01.02.01 - As an entrant, I want to provide my personal information in the app.</li>
 * <li>US 01.02.02 - As an entrant, I want to update information on my profile.</li>
 * <li>US 01.02.04 - As an entrant, I want to delete my profile.</li>
 * </ul>
 */
@RunWith(AndroidJUnit4.class)
public class ProfileActivityTest {

    /** Hardcoded Entrant ID used exclusively for isolating test data in Firestore. */
    private static final String TEST_ENTRANT_ID = "test-entrant-profile-001";

    /** Hardcoded Event ID used to test waitlist removals during account deletion. */
    private static final String TEST_EVENT_1 = "test-event-001";

    /** Hardcoded Event ID used to test waitlist removals during account deletion. */
    private static final String TEST_EVENT_2 = "test-event-002";

    /** Reference to the Firestore database instance used for setup and assertions. */
    private FirebaseFirestore db;

    /**
     * Sets up the testing environment before each test runs.
     * Mocks the user session to prevent the Activity from redirecting to the login screen,
     * and initializes dummy event documents required for deletion tests.
     *
     * @throws Exception if Firestore data population is interrupted or fails.
     */
    @Before
    public void setUp() throws Exception {
        // Mock the session so Profile Activity doesn't redirect to login
        Context context = ApplicationProvider.getApplicationContext();
        SessionManager.getInstance(context).saveSession(TEST_ENTRANT_ID, "entrant");

        db = FirebaseFirestore.getInstance();

        // FIX: Create a mock profile in Firestore so the Activity doesn't think
        // the account was deleted and force a logout/redirect to SelectRoleActivity.
        Map<String, Object> profile = new HashMap<>();
        profile.put("entrantId", TEST_ENTRANT_ID);
        profile.put("role", "entrant");
        profile.put("name", "Test User");
        profile.put("email", "test@example.com");
        profile.put("phone", "1234567890");

        Tasks.await(db.collection("entrants").document(TEST_ENTRANT_ID).set(profile), 10, TimeUnit.SECONDS);

        Map<String, Object> event = new HashMap<>();
        event.put("title", "Test Event");

        // Pre-populate events needed for testing waitlist cascading deletes
        Tasks.await(db.collection("events").document(TEST_EVENT_1).set(event), 10, TimeUnit.SECONDS);
        Tasks.await(db.collection("events").document(TEST_EVENT_2).set(event), 10, TimeUnit.SECONDS);
    }

    /**
     * Cleans up the testing environment after each test runs.
     * Clears the mocked session to prevent data leakage into other test suites.
     */
    @After
    public void tearDown() {
        Context context = ApplicationProvider.getApplicationContext();
        SessionManager.getInstance(context).clearSession();
    }

    /**
     * Pauses the main thread briefly to allow the profile screen to finish
     * fetching and displaying data from Firestore.
     */
    private void waitForProfileLoad() {
        SystemClock.sleep(1500);
    }

    /**
     * Helper method to safely extract text from a {@link TextInputEditText}.
     *
     * @param editText The input field to read from.
     * @return The current String value of the field, or an empty string if null.
     */
    private String getText(TextInputEditText editText) {
        return editText.getText() == null ? "" : editText.getText().toString();
    }

    /**
     * Utility method utilizing Java Reflection to invoke a private method within the Activity.
     * Used primarily to trigger the protected {@code deleteAccount()} logic.
     *
     * @param activity   The instance of the ProfileActivity.
     * @param methodName The exact string name of the private method to invoke.
     * @throws RuntimeException if the method cannot be found or accessed.
     */
    private void invokePrivateMethod(ProfileActivity activity, String methodName) {
        try {
            Method method = ProfileActivity.class.getDeclaredMethod(methodName);
            method.setAccessible(true);
            method.invoke(activity);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Launches the {@link ProfileActivity} and passes the required test entrant ID
     * through the Intent to ensure the Activity loads the isolated test data.
     *
     * @return The {@link ActivityScenario} managing the activity lifecycle.
     */
    private ActivityScenario<ProfileActivity> launchProfileActivity() {
        Intent intent = new Intent(
                ApplicationProvider.getApplicationContext(),
                ProfileActivity.class
        );
        intent.putExtra(ProfileActivity.EXTRA_TEST_ENTRANT_ID, TEST_ENTRANT_ID);
        return ActivityScenario.launch(intent);
    }

    /**
     * Actively polls a Firestore document until it matches the expected existence state
     * or a timeout is reached. Useful for verifying asynchronous database writes.
     *
     * @param docRef      The Firestore DocumentReference to observe.
     * @param shouldExist True if the document is expected to exist, false if it should be deleted.
     * @return The final {@link DocumentSnapshot} reflecting the expected state.
     * @throws AssertionError if the document does not reach the expected state within 10 seconds.
     * @throws Exception if the Firestore read task is interrupted.
     */
    private DocumentSnapshot waitForDocumentState(DocumentReference docRef, boolean shouldExist) throws Exception {
        long deadline = System.currentTimeMillis() + 10000;

        while (System.currentTimeMillis() < deadline) {
            DocumentSnapshot snapshot = Tasks.await(docRef.get(), 5, TimeUnit.SECONDS);

            if (snapshot.exists() == shouldExist) {
                return snapshot;
            }
            SystemClock.sleep(300);
        }
        throw new AssertionError("Timed out waiting for document state: " + docRef.getPath());
    }

    /**
     * Tests that all primary UI components (text fields, buttons, labels) are
     * successfully bound and visible when the Activity is created.
     */
    @Test
    public void profileScreen_displaysMainViews() {
        try (ActivityScenario<ProfileActivity> scenario = launchProfileActivity()) {
            waitForProfileLoad();

            scenario.onActivity(activity -> {
                assertNotNull(activity.findViewById(R.id.tvProfileTitle));
                assertNotNull(activity.findViewById(R.id.etName));
                assertNotNull(activity.findViewById(R.id.etEmail));
                assertNotNull(activity.findViewById(R.id.etPhone));
                assertNotNull(activity.findViewById(R.id.btnSaveChanges));
                assertNotNull(activity.findViewById(R.id.btnDeleteAccount));
            });
        }
    }

    /**
     * Tests input validation: verifies that submitting the form with an empty name
     * triggers the appropriate localized error message on the EditText.
     */
    @Test
    public void saveProfile_emptyName_showsNameError() {
        try (ActivityScenario<ProfileActivity> scenario = launchProfileActivity()) {
            waitForProfileLoad();

            scenario.onActivity(activity -> {
                TextInputEditText etName = activity.findViewById(R.id.etName);
                TextInputEditText etEmail = activity.findViewById(R.id.etEmail);
                TextInputEditText etPhone = activity.findViewById(R.id.etPhone);
                MaterialButton btnSave = activity.findViewById(R.id.btnSaveChanges);

                etName.setText("");
                etEmail.setText("test@example.com");
                etPhone.setText("");

                btnSave.performClick();

                assertEquals("Name is required", etName.getError());
            });
        }
    }

    /**
     * Tests input validation: verifies that submitting the form with an empty email
     * triggers the appropriate localized error message on the EditText.
     */
    @Test
    public void saveProfile_emptyEmail_showsEmailError() {
        try (ActivityScenario<ProfileActivity> scenario = launchProfileActivity()) {
            waitForProfileLoad();

            scenario.onActivity(activity -> {
                TextInputEditText etName = activity.findViewById(R.id.etName);
                TextInputEditText etEmail = activity.findViewById(R.id.etEmail);
                TextInputEditText etPhone = activity.findViewById(R.id.etPhone);
                MaterialButton btnSave = activity.findViewById(R.id.btnSaveChanges);

                etName.setText("Blake");
                etEmail.setText("");
                etPhone.setText("");

                btnSave.performClick();

                assertEquals("Email is required", etEmail.getError());
            });
        }
    }

    /**
     * Tests input validation: verifies that submitting the form with a poorly formatted
     * email string triggers an invalid email error on the EditText.
     */
    @Test
    public void saveProfile_invalidEmail_showsEmailError() {
        try (ActivityScenario<ProfileActivity> scenario = launchProfileActivity()) {
            waitForProfileLoad();

            scenario.onActivity(activity -> {
                TextInputEditText etName = activity.findViewById(R.id.etName);
                TextInputEditText etEmail = activity.findViewById(R.id.etEmail);
                TextInputEditText etPhone = activity.findViewById(R.id.etPhone);
                MaterialButton btnSave = activity.findViewById(R.id.btnSaveChanges);

                etName.setText("Blake");
                etEmail.setText("not-an-email");
                etPhone.setText("");

                btnSave.performClick();

                assertEquals("Enter a valid email", etEmail.getError());
            });
        }
    }

    /**
     * Tests input validation: verifies that submitting the form with a phone number
     * that does not meet the 10-digit requirement triggers an error on the EditText.
     */
    @Test
    public void saveProfile_invalidPhone_showsPhoneError() {
        try (ActivityScenario<ProfileActivity> scenario = launchProfileActivity()) {
            waitForProfileLoad();

            scenario.onActivity(activity -> {
                TextInputEditText etName = activity.findViewById(R.id.etName);
                TextInputEditText etEmail = activity.findViewById(R.id.etEmail);
                TextInputEditText etPhone = activity.findViewById(R.id.etPhone);
                MaterialButton btnSave = activity.findViewById(R.id.btnSaveChanges);

                etName.setText("Blake");
                etEmail.setText("test@example.com");
                etPhone.setText("123456789"); // Missing one digit

                btnSave.performClick();

                assertEquals("Enter a 10-digit phone number", etPhone.getError());
            });
        }
    }

    /**
     * Integration test: verifies that submitting the form with fully valid inputs
     * successfully constructs and uploads the profile map to the Firestore database.
     *
     * @throws Exception if Firestore synchronization times out or fails.
     */
    @Test
    public void saveProfile_validInput_writesProfileDocument() throws Exception {
        try (ActivityScenario<ProfileActivity> scenario = launchProfileActivity()) {
            waitForProfileLoad();

            scenario.onActivity(activity -> {
                TextInputEditText etName = activity.findViewById(R.id.etName);
                TextInputEditText etEmail = activity.findViewById(R.id.etEmail);
                TextInputEditText etPhone = activity.findViewById(R.id.etPhone);
                MaterialButton btnSave = activity.findViewById(R.id.btnSaveChanges);

                etName.setText("Saved User");
                etEmail.setText("saved@example.com");
                etPhone.setText("5871234567");

                btnSave.performClick();
            });

            // Assert that the database successfully receives the payload
            DocumentSnapshot snapshot = waitForDocumentState(
                    db.collection("entrants").document(TEST_ENTRANT_ID),
                    true
            );

            assertTrue(snapshot.exists());
            assertEquals("Saved User", snapshot.getString("name"));
            assertEquals("saved@example.com", snapshot.getString("email"));
            assertEquals("5871234567", snapshot.getString("phone"));
            assertEquals("entrant", snapshot.getString("role"));
            assertEquals(TEST_ENTRANT_ID, snapshot.getString("entrantId"));
        }
    }

    /**
     * Integration test for User Story US 01.02.04: verifies that executing the account
     * deletion sequence successfully removes the user's primary profile document AND
     * cascades to remove them from any event waitlists they were joined to.
     *
     * @throws Exception if Firestore data population or verification times out or fails.
     */
    @Test
    public void deleteAccount_removesProfileAndWaitingListEntries() throws Exception {
        // 1. Inject fake waitlist entries across multiple events for this user
        Map<String, Object> waitingEntry = new HashMap<>();
        waitingEntry.put("deviceId", TEST_ENTRANT_ID);
        waitingEntry.put("status", "waiting");
        waitingEntry.put("joinedAt", System.currentTimeMillis());

        Tasks.await(
                db.collection("events").document(TEST_EVENT_1)
                        .collection("waitlist").document(TEST_ENTRANT_ID)
                        .set(waitingEntry),
                10, TimeUnit.SECONDS
        );

        Tasks.await(
                db.collection("events").document(TEST_EVENT_2)
                        .collection("waitlist").document(TEST_ENTRANT_ID)
                        .set(waitingEntry),
                10, TimeUnit.SECONDS
        );

        // 2. Launch the activity and invoke the deletion logic
        try (ActivityScenario<ProfileActivity> scenario = launchProfileActivity()) {
            waitForProfileLoad();

            scenario.onActivity(activity -> invokePrivateMethod(activity, "deleteAccount"));

            // 3. Assert that all traces of the user are wiped from the database
            DocumentSnapshot profileSnapshot = waitForDocumentState(
                    db.collection("entrants").document(TEST_ENTRANT_ID),
                    false
            );

            DocumentSnapshot waiting1 = waitForDocumentState(
                    db.collection("events").document(TEST_EVENT_1)
                            .collection("waitlist").document(TEST_ENTRANT_ID),
                    false
            );

            DocumentSnapshot waiting2 = waitForDocumentState(
                    db.collection("events").document(TEST_EVENT_2)
                            .collection("waitlist").document(TEST_ENTRANT_ID),
                    false
            );

            assertFalse(profileSnapshot.exists());
            assertFalse(waiting1.exists());
            assertFalse(waiting2.exists());
        }
    }
}