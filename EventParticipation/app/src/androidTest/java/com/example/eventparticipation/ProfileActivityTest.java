package com.example.eventparticipation;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import android.content.Intent;
import android.os.SystemClock;

import androidx.test.core.app.ActivityScenario;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.google.android.gms.tasks.Tasks;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * Instrumentation tests for `ProfileActivity` covering UI validation, profile loading and saving,
 * and account deletion behavior with Firestore.
 */
@RunWith(AndroidJUnit4.class)
public class ProfileActivityTest {

    private static final String TEST_ENTRANT_ID = "test-entrant-profile-001";
    private static final String TEST_EVENT_1 = "test-event-001";
    private static final String TEST_EVENT_2 = "test-event-002";

    private FirebaseFirestore db;

    @Before
    public void setUp() throws Exception {
        db = FirebaseFirestore.getInstance();

        Map<String, Object> event = new HashMap<>();
        event.put("title", "Test Event");

        Tasks.await(db.collection("events").document(TEST_EVENT_1).set(event), 10, TimeUnit.SECONDS);
        Tasks.await(db.collection("events").document(TEST_EVENT_2).set(event), 10, TimeUnit.SECONDS);
    }

    /**
     * Waits briefly for the profile screen to finish loading data.
     */
    private void waitForProfileLoad() {
        SystemClock.sleep(1500);
    }

    /**
     * Returns text from an input field.
     *
     * @param editText the input field
     * @return the current text value
     */
    private String getText(TextInputEditText editText) {
        return editText.getText() == null ? "" : editText.getText().toString();
    }

    /**
     * Invokes a private method on ProfileActivity by name.
     *
     * @param activity the profile activity instance
     * @param methodName the method name to invoke
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
     * Launches ProfileActivity with a test entrant ID.
     *
     * @return an activity scenario for ProfileActivity
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
     * Waits until a Firestore document reaches the expected existence state.
     *
     * @param docRef the document reference to observe
     * @param shouldExist true if the document should exist; false otherwise
     * @return the latest document snapshot
     * @throws Exception if the expected state is not reached in time
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

    @Test
    public void profileScreen_displaysMainViews() {
        try (ActivityScenario<ProfileActivity> scenario = ActivityScenario.launch(ProfileActivity.class)) {
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

    @Test
    public void saveProfile_emptyName_showsNameError() {
        try (ActivityScenario<ProfileActivity> scenario = ActivityScenario.launch(ProfileActivity.class)) {
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

    @Test
    public void saveProfile_emptyEmail_showsEmailError() {
        try (ActivityScenario<ProfileActivity> scenario = ActivityScenario.launch(ProfileActivity.class)) {
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

    @Test
    public void saveProfile_invalidEmail_showsEmailError() {
        try (ActivityScenario<ProfileActivity> scenario = ActivityScenario.launch(ProfileActivity.class)) {
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

    @Test
    public void saveProfile_invalidPhone_showsPhoneError() {
        try (ActivityScenario<ProfileActivity> scenario = ActivityScenario.launch(ProfileActivity.class)) {
            waitForProfileLoad();

            scenario.onActivity(activity -> {
                TextInputEditText etName = activity.findViewById(R.id.etName);
                TextInputEditText etEmail = activity.findViewById(R.id.etEmail);
                TextInputEditText etPhone = activity.findViewById(R.id.etPhone);
                MaterialButton btnSave = activity.findViewById(R.id.btnSaveChanges);

                etName.setText("Blake");
                etEmail.setText("test@example.com");
                etPhone.setText("123456789");

                btnSave.performClick();

                assertEquals("Enter a 10-digit phone number", etPhone.getError());
            });
        }
    }

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

    @Test
    public void deleteAccount_removesProfileAndWaitingListEntries() throws Exception {
        Map<String, Object> profile = new HashMap<>();
        profile.put("entrantId", TEST_ENTRANT_ID);
        profile.put("role", "entrant");
        profile.put("name", "Delete Me");
        profile.put("email", "delete@example.com");
        profile.put("phone", "7801234567");

        Map<String, Object> waitingEntry = new HashMap<>();
        waitingEntry.put("deviceId", TEST_ENTRANT_ID);
        waitingEntry.put("status", "waiting");
        waitingEntry.put("joinedAt", System.currentTimeMillis());

        Tasks.await(
                db.collection("entrants").document(TEST_ENTRANT_ID).set(profile),
                10, TimeUnit.SECONDS
        );

        Tasks.await(
                db.collection("events").document(TEST_EVENT_1)
                        .collection("waitList").document(TEST_ENTRANT_ID)
                        .set(waitingEntry),
                10, TimeUnit.SECONDS
        );

        Tasks.await(
                db.collection("events").document(TEST_EVENT_2)
                        .collection("waitList").document(TEST_ENTRANT_ID)
                        .set(waitingEntry),
                10, TimeUnit.SECONDS
        );

        try (ActivityScenario<ProfileActivity> scenario = launchProfileActivity()) {
            waitForProfileLoad();

            scenario.onActivity(activity -> invokePrivateMethod(activity, "deleteAccount"));

            DocumentSnapshot profileSnapshot = waitForDocumentState(
                    db.collection("entrants").document(TEST_ENTRANT_ID),
                    false
            );

            DocumentSnapshot waiting1 = waitForDocumentState(
                    db.collection("events").document(TEST_EVENT_1)
                            .collection("waitList").document(TEST_ENTRANT_ID),
                    false
            );

            DocumentSnapshot waiting2 = waitForDocumentState(
                    db.collection("events").document(TEST_EVENT_2)
                            .collection("waitList").document(TEST_ENTRANT_ID),
                    false
            );

            assertFalse(profileSnapshot.exists());
            assertFalse(waiting1.exists());
            assertFalse(waiting2.exists());
        }
    }
}