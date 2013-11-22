package com.android.settings.applications;

import android.app.Fragment;
import android.content.Intent;
import android.preference.PreferenceActivity;

import com.android.settings.ChooseLockGeneric.ChooseLockGenericFragment;

public class InstalledAppDetailsTop extends PreferenceActivity {

    @Override
    public Intent getIntent() {
        Intent modIntent = new Intent(super.getIntent());
        modIntent.putExtra(EXTRA_SHOW_FRAGMENT, InstalledAppDetails.class.getName());
        modIntent.putExtra(EXTRA_NO_HEADERS, true);
        return modIntent;
    }

    @Override
    protected boolean isValidFragment(String fragmentName) {
        if (InstalledAppDetails.class.getName().equals(fragmentName)) return true;
        return false;
    }

}
