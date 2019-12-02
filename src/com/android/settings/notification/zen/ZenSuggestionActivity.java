package com.android.settings.notification.zen;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.provider.Settings;

public class ZenSuggestionActivity extends Activity {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // start up zen settings activity
        Intent settingsIntent = new Intent(Settings.ACTION_ZEN_MODE_SETTINGS);
        startActivity(settingsIntent);

        // start up onboarding activity
        Intent onboardingActivity = new Intent(Settings.ZEN_MODE_ONBOARDING);
        startActivity(onboardingActivity);

        finish();
    }
}
