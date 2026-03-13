package com.example.eventparticipation;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.util.Map;

public class NotificationRepositoryPayloadTest {

    @Test
    public void selectedPayload_containsActionFields() {
        Map<String, Object> payload = NotificationRepository.buildSelectedNotificationData(
                "entrant_1",
                "event_1",
                "Tech Event"
        );

        assertEquals("entrant_1", payload.get("entrantId"));
        assertEquals("event_1", payload.get("eventId"));
        assertEquals(NotificationItem.TYPE_SELECTED, payload.get("type"));
        assertTrue((Boolean) payload.get("unread"));
        assertTrue((Boolean) payload.get("actionRequired"));
        assertEquals(NotificationItem.ACTION_PENDING, payload.get("actionStatus"));
    }

    @Test
    public void notSelectedPayload_hasNoActionRequired() {
        Map<String, Object> payload = NotificationRepository.buildNotSelectedNotificationData(
                "entrant_1",
                "event_1",
                "Tech Event"
        );

        assertEquals(NotificationItem.TYPE_NOT_SELECTED, payload.get("type"));
        assertFalse((Boolean) payload.get("actionRequired"));
        assertEquals(NotificationItem.ACTION_NONE, payload.get("actionStatus"));
    }
}
