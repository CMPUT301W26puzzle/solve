package com.example.eventparticipation;


import android.content.Intent;


import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.Test;
import org.junit.runner.RunWith;

import androidx.test.platform.app.InstrumentationRegistry;

@RunWith(AndroidJUnit4.class)
public class QRCodeScannerTest {

    @Test
    public void launchEventDetailActivity_withEventId() {

        Intent intent = new Intent(
                InstrumentationRegistry.getInstrumentation().getTargetContext(),
                EntrantEventDetailActivity.class
        );

        intent.putExtra("EVENT_ID", "testEvent123");

        ActivityScenario.launch(intent);
    }

    @Test
    public void activityReceivesEventId() {

        Intent intent = new Intent(
                InstrumentationRegistry.getInstrumentation().getTargetContext(),
                EntrantEventDetailActivity.class
        );

        intent.putExtra("EVENT_ID", "event123");

        try (ActivityScenario<EntrantEventDetailActivity> scenario =
                     ActivityScenario.launch(intent)) {

            scenario.onActivity(activity -> {
                String eventId = activity.getIntent().getStringExtra("EVENT_ID");
                assert(eventId.equals("event123"));
            });
        }
    }
}
