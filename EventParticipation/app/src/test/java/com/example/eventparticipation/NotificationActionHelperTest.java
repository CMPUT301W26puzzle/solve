package com.example.eventparticipation;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.util.Date;

public class NotificationActionHelperTest {

    @Test
    public void selectedNotification_showsActions() {
        NotificationItem item = new NotificationItem();
        item.setType(NotificationItem.TYPE_SELECTED);
        item.setActionRequired(true);
        item.setActionStatus(NotificationItem.ACTION_PENDING);

        assertTrue(NotificationActionHelper.shouldShowAcceptAction(item));
        assertTrue(NotificationActionHelper.shouldShowDeclineAction(item));
        assertEquals("Accept Invitation", NotificationActionHelper.getPrimaryActionLabel(item));
    }

    @Test
    public void nonSelectedNotification_hidesActions() {
        NotificationItem item = new NotificationItem();
        item.setType(NotificationItem.TYPE_NOT_SELECTED);
        item.setActionRequired(false);
        item.setActionStatus(NotificationItem.ACTION_NONE);

        assertFalse(NotificationActionHelper.shouldShowAcceptAction(item));
        assertFalse(NotificationActionHelper.shouldShowDeclineAction(item));
    }

    @Test
    public void applyAccepted_updatesState() {
        NotificationItem item = new NotificationItem();
        item.setType(NotificationItem.TYPE_SELECTED);
        item.setActionRequired(true);
        item.setActionStatus(NotificationItem.ACTION_PENDING);
        item.setUnread(true);

        NotificationActionHelper.applyAccepted(item);

        assertFalse(item.isUnread());
        assertFalse(item.isActionRequired());
        assertEquals(NotificationItem.ACTION_ACCEPTED, item.getActionStatus());
        assertEquals("Invitation accepted", NotificationActionHelper.getActionStateLabel(item));
    }

    @Test
    public void formatRelativeTime_returnsMinutesAgo() {
        long now = 10_000L;
        Date createdAt = new Date(now - 5 * 60_000L);

        assertEquals("5 minutes ago", NotificationActionHelper.formatRelativeTime(createdAt, now));
    }
}
