package com.example.eventparticipation;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import android.graphics.Bitmap;

import com.google.zxing.WriterException;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

/**
 * Unit tests for the QRCodeGenerator utility class.
 */
@RunWith(RobolectricTestRunner.class)
public class QRCodeGeneratorTest {

    /**
     * US 02.01.01: Generate a unique promotional QR code.
     * Verifies that a valid square bitmap is successfully generated from an event ID string.
     */
    @Test
    public void generateQRCode_returnsValidBitmap() throws WriterException {
        String testEventId = "event_12345_xyz";
        int size = 250;

        Bitmap qrCode = QRCodeGenerator.generateQRCode(testEventId, size);

        // verify the bitmap was successfully generated
        assertNotNull("QR Code bitmap should not be null", qrCode);

        // verify dimensions are matching the request
        assertEquals("QR Code width should match requested size", size, qrCode.getWidth());
        assertEquals("QR Code height should match requested size", size, qrCode.getHeight());
    }
}