package com.example.eventparticipation;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

/**
 * Unit tests for DeviceIdProvider.
 */
public class DeviceIdProviderTest {

    @Test
    public void isValidId_returnsFalse_whenIdIsNull() {
        assertFalse(DeviceIdProvider.isValidId(null));
    }

    @Test
    public void isValidId_returnsFalse_whenIdIsEmpty() {
        assertFalse(DeviceIdProvider.isValidId(""));
    }

    @Test
    public void isValidId_returnsFalse_whenIdIsBlank() {
        assertFalse(DeviceIdProvider.isValidId("   "));
    }

    @Test
    public void isValidId_returnsFalse_whenIdIsUnknownDevice() {
        assertFalse(DeviceIdProvider.isValidId("unknown_device"));
    }

    @Test
    public void isValidId_returnsTrue_whenIdIsValid() {
        assertTrue(DeviceIdProvider.isValidId("abc123deviceid"));
    }
}