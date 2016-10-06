/*
 * Copyright (C) 2016 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the
 * License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the specific language governing
 * permissions and limitations under the License.
 */
package com.android.settings.display;

import android.app.ActivityManager;
import android.content.Context;
import android.content.pm.PackageManager;
import android.provider.Settings;
import android.support.v7.preference.DropDownPreference;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceScreen;
import android.util.Log;

import com.android.settings.R;
import com.android.settings.core.PreferenceController;

public class VrDisplayPreferenceController extends PreferenceController implements
        Preference.OnPreferenceChangeListener {

    private static final String TAG = "VrDisplayPrefContr";
    private static final String KEY_VR_DISPLAY_PREF = "vr_display_pref";

    public VrDisplayPreferenceController(Context context) {
        super(context);
    }

    @Override
    protected boolean isAvailable() {
        final PackageManager pm = mContext.getPackageManager();
        return pm.hasSystemFeature(PackageManager.FEATURE_VR_MODE_HIGH_PERFORMANCE);
    }

    @Override
    protected String getPreferenceKey() {
        return KEY_VR_DISPLAY_PREF;
    }

    @Override
    public void updateState(PreferenceScreen screen) {
        final DropDownPreference pref =
                (DropDownPreference) screen.findPreference(KEY_VR_DISPLAY_PREF);
        if (pref == null) {
            Log.d(TAG, "Could not find VR display preference.");
            return;
        }
        pref.setEntries(new CharSequence[]{
                mContext.getString(R.string.display_vr_pref_low_persistence),
                mContext.getString(R.string.display_vr_pref_off),
        });
        pref.setEntryValues(new CharSequence[]{"0", "1"});

        int currentUser = ActivityManager.getCurrentUser();
        int current = Settings.Secure.getIntForUser(mContext.getContentResolver(),
                Settings.Secure.VR_DISPLAY_MODE,
                            /*default*/Settings.Secure.VR_DISPLAY_MODE_LOW_PERSISTENCE,
                currentUser);
        pref.setValueIndex(current);
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        int i = Integer.parseInt((String) newValue);
        int u = ActivityManager.getCurrentUser();
        if (!Settings.Secure.putIntForUser(mContext.getContentResolver(),
                Settings.Secure.VR_DISPLAY_MODE,
                i, u)) {
            Log.e(TAG, "Could not change setting for " +
                    Settings.Secure.VR_DISPLAY_MODE);
        }
        return true;
    }

    @Override
    public boolean handlePreferenceTreeClick(Preference preference) {
        return false;
    }
}
