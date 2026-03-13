package com.example.eventparticipation;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * Instrumented tests for ProfileInitializer.
 *
 * <p>User story covered:</p>
 * <ul>
 *     <li>US 01.07.01 - As an entrant, I want to be identified by my device so I don’t need a username and password.</li>
 * </ul>
 */
@RunWith(AndroidJUnit4.class)
public class ProfileInitializerFirestoreTest {

    private FirebaseFirestore db;
    private ProfileInitializer initializer;

    @Before
    public void setUp() {
        db = FirebaseFirestore.getInstance();
        initializer = new ProfileInitializer();
    }

    @Test
    public void ensureEntrantProfileExists_createsBlankProfile_whenProfileDoesNotExist() throws InterruptedException {
        String entrantId = uniqueId("entrant_test");

        deleteDocument("entrants", entrantId);

        CountDownLatch latch = new CountDownLatch(1);
        final Exception[] error = new Exception[1];

        initializer.ensureEntrantProfileExists(entrantId, new ProfileInitializer.Callback() {
            @Override
            public void onSuccess() {
                latch.countDown();
            }

            @Override
            public void onError(Exception e) {
                error[0] = e;
                latch.countDown();
            }
        });

        assertTrue(latch.await(5, TimeUnit.SECONDS));
        if (error[0] != null) {
            fail(error[0].getMessage());
        }

        DocumentSnapshot snapshot = getDocument("entrants", entrantId);

        assertNotNull(snapshot);
        assertTrue(snapshot.exists());
        assertEquals(entrantId, snapshot.getString("entrantId"));
        assertEquals("entrant", snapshot.getString("role"));
        assertEquals("", snapshot.getString("name"));
        assertEquals("", snapshot.getString("email"));
        assertEquals("", snapshot.getString("phone"));

        deleteDocument("entrants", entrantId);
    }

    @Test
    public void ensureEntrantProfileExists_doesNotOverwriteExistingProfile() throws InterruptedException {
        String entrantId = uniqueId("entrant_existing");

        Map<String, Object> profile = new HashMap<>();
        profile.put("entrantId", entrantId);
        profile.put("role", "entrant");
        profile.put("name", "Tom Lee");
        profile.put("email", "tom@test.com");
        profile.put("phone", "123456789");

        setDocument("entrants", entrantId, profile);

        CountDownLatch latch = new CountDownLatch(1);
        final Exception[] error = new Exception[1];

        initializer.ensureEntrantProfileExists(entrantId, new ProfileInitializer.Callback() {
            @Override
            public void onSuccess() {
                latch.countDown();
            }

            @Override
            public void onError(Exception e) {
                error[0] = e;
                latch.countDown();
            }
        });

        assertTrue(latch.await(5, TimeUnit.SECONDS));
        if (error[0] != null) {
            fail(error[0].getMessage());
        }

        DocumentSnapshot snapshot = getDocument("entrants", entrantId);

        assertNotNull(snapshot);
        assertTrue(snapshot.exists());
        assertEquals(entrantId, snapshot.getString("entrantId"));
        assertEquals("entrant", snapshot.getString("role"));
        assertEquals("Tom Lee", snapshot.getString("name"));
        assertEquals("tom@test.com", snapshot.getString("email"));
        assertEquals("123456789", snapshot.getString("phone"));

        deleteDocument("entrants", entrantId);
    }

    @Test
    public void ensureOrganizerProfileExists_createsBlankProfile_whenProfileDoesNotExist() throws InterruptedException {
        String organizerId = uniqueId("organizer_test");

        deleteDocument("organizers", organizerId);

        CountDownLatch latch = new CountDownLatch(1);
        final Exception[] error = new Exception[1];

        initializer.ensureOrganizerProfileExists(organizerId, new ProfileInitializer.Callback() {
            @Override
            public void onSuccess() {
                latch.countDown();
            }

            @Override
            public void onError(Exception e) {
                error[0] = e;
                latch.countDown();
            }
        });

        assertTrue(latch.await(5, TimeUnit.SECONDS));
        if (error[0] != null) {
            fail(error[0].getMessage());
        }

        DocumentSnapshot snapshot = getDocument("organizers", organizerId);

        assertNotNull(snapshot);
        assertTrue(snapshot.exists());
        assertEquals(organizerId, snapshot.getString("organizerId"));
        assertEquals("organizer", snapshot.getString("role"));
        assertEquals("", snapshot.getString("name"));
        assertEquals("", snapshot.getString("email"));
        assertEquals("", snapshot.getString("phone"));

        deleteDocument("organizers", organizerId);
    }

    @Test
    public void ensureAdminProfileExists_createsBlankProfile_whenProfileDoesNotExist() throws InterruptedException {
        String adminId = uniqueId("admin_test");

        deleteDocument("admins", adminId);

        CountDownLatch latch = new CountDownLatch(1);
        final Exception[] error = new Exception[1];

        initializer.ensureAdminProfileExists(adminId, new ProfileInitializer.Callback() {
            @Override
            public void onSuccess() {
                latch.countDown();
            }

            @Override
            public void onError(Exception e) {
                error[0] = e;
                latch.countDown();
            }
        });

        assertTrue(latch.await(5, TimeUnit.SECONDS));
        if (error[0] != null) {
            fail(error[0].getMessage());
        }

        DocumentSnapshot snapshot = getDocument("admins", adminId);

        assertNotNull(snapshot);
        assertTrue(snapshot.exists());
        assertEquals(adminId, snapshot.getString("adminId"));
        assertEquals("admin", snapshot.getString("role"));
        assertEquals("", snapshot.getString("name"));
        assertEquals("", snapshot.getString("email"));
        assertEquals("", snapshot.getString("phone"));

        deleteDocument("admins", adminId);
    }

    private String uniqueId(String prefix) {
        return prefix + "_" + System.currentTimeMillis();
    }

    private void setDocument(String collection, String documentId, Map<String, Object> data)
            throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);
        final Exception[] error = new Exception[1];

        db.collection(collection)
                .document(documentId)
                .set(data)
                .addOnSuccessListener(unused -> latch.countDown())
                .addOnFailureListener(e -> {
                    error[0] = e;
                    latch.countDown();
                });

        assertTrue(latch.await(5, TimeUnit.SECONDS));
        if (error[0] != null) {
            fail(error[0].getMessage());
        }
    }

    private void deleteDocument(String collection, String documentId) throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);
        final Exception[] error = new Exception[1];

        db.collection(collection)
                .document(documentId)
                .delete()
                .addOnSuccessListener(unused -> latch.countDown())
                .addOnFailureListener(e -> {
                    error[0] = e;
                    latch.countDown();
                });

        assertTrue(latch.await(5, TimeUnit.SECONDS));
        if (error[0] != null) {
            fail(error[0].getMessage());
        }
    }

    private DocumentSnapshot getDocument(String collection, String documentId) throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);
        final DocumentSnapshot[] result = new DocumentSnapshot[1];
        final Exception[] error = new Exception[1];

        db.collection(collection)
                .document(documentId)
                .get()
                .addOnSuccessListener(snapshot -> {
                    result[0] = snapshot;
                    latch.countDown();
                })
                .addOnFailureListener(e -> {
                    error[0] = e;
                    latch.countDown();
                });

        assertTrue(latch.await(5, TimeUnit.SECONDS));
        if (error[0] != null) {
            fail(error[0].getMessage());
        }

        return result[0];
    }
}