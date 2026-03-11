package com.example.eventparticipation;

import android.graphics.Bitmap;
import android.graphics.Color;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;

/**
 * Utility model class for generating QR codes from text data.
 *
 * <p>Acts as a helper utility to encode Event IDs into standard QR Code bitmaps
 * that can be displayed in an ImageView or saved to the device</p>
 *
 <p>Relevant user stories:</p>
 * <ul>
 * <li>US 01.06.01 As an entrant I want to view event details within the app by scanning the promotional QR code</li>
 * <li>US 02.01.01 As an organizer I want to create a new event and generate a unique promotional QR code that links to the event description and event poster in the app.</li>
 * </ul>
 */
public class QRCodeGenerator {

    // private constructor to prevent instantiation of utility class
    private QRCodeGenerator() {}

    /**
     * Generates a square QR Code Bitmap from the provided Event ID string.
     *
     * @param eventId The unique identifier for the event to encode.
     * @param width   The desired width (and height) of the resulting bitmap in pixels.
     * @return A generated Bitmap of the QR code.
     * @throws WriterException If the encoding process fails (e.g., input is too large).
     */
    public static Bitmap generateQRCode(String eventId, int width) throws WriterException {
        QRCodeWriter writer = new QRCodeWriter();

        // encode the string into a BitMatrix representing the black and white squares
        BitMatrix bitMatrix = writer.encode(eventId, BarcodeFormat.QR_CODE, width, width); // ignore the warning, its a square

        int[] pixels = new int[width * width];

        // map the boolean BitMatrix to Android Color pixels
        for (int y = 0; y < width; y++) {
            for (int x = 0; x < width; x++) {
                pixels[y * width + x] = bitMatrix.get(x, y) ? Color.BLACK : Color.WHITE;
            }
        }

        // create and return the final Bitmap
        Bitmap bitmap = Bitmap.createBitmap(width, width, Bitmap.Config.ARGB_8888);
        bitmap.setPixels(pixels, 0, width, 0, 0, width, width);
        return bitmap;
    }
}