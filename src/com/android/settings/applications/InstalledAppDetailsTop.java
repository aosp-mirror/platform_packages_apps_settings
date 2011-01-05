package com.android.settings.applications;

import android.content.Intent;
import android.preference.PreferenceActivity;

public class InstalledAppDetailsTop extends PreferenceActivity {
    @Override
    public Intent getIntent() {
        Intent modIntent = new Intent(super.getIntent());
        modIntent.putExtra(EXTRA_SHOW_FRAGMENT, InstalledAppDetails.class.getName());
        modIntent.putExtra(EXTRA_NO_HEADERS, true);
        return modIntent;
    }
}
