package com.example.eventparticipation;

import android.app.Activity;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.view.Gravity;
import android.view.WindowManager;

/**
 * A dialog or popup screen that explains the lottery system rules to users.
 *
 * <p>Relevant user story:</p>
 * <ul>
 * <li>US 01.05.05 As an entrant, I want to be informed about the criteria or
 * guidelines for the lottery selection process.</li>
 * </ul>
 */
public class InfoPopup extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.info_popup);

        DisplayMetrics display = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(display);

        int width = display.widthPixels;
        int height = display.heightPixels;

        getWindow().setLayout((int)(width*.9), (int)(height*.9));

        WindowManager.LayoutParams params = getWindow().getAttributes();
        params.gravity = Gravity.CENTER;
        params.x = 0;
        params.y = +160;

        getWindow().setAttributes(params);
    }

}
