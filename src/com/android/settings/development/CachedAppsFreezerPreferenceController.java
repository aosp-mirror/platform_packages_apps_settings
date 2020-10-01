/*
 * Copyright (C) 2020 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.android.settings.development;

import android.app.ActivityManager;
import android.content.Context;
import android.content.DialogInterface;
import android.os.PowerManager;
import android.os.RemoteException;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.Log;

import androidx.annotation.VisibleForTesting;
import androidx.appcompat.app.AlertDialog;
import androidx.preference.ListPreference;
import androidx.preference.Preference;

import com.android.settings.R;
import com.android.settings.core.PreferenceControllerMixin;
import com.android.settingslib.development.DeveloperOptionsPreferenceController;


public class CachedAppsFreezerPreferenceController extends DeveloperOptionsPreferenceController
        implements Preference.OnPreferenceChangeListener, PreferenceControllerMixin {

    @VisibleForTesting
    private static final String CACHED_APPS_FREEZER_KEY = "cached_apps_freezer";

    private final String[] mListValues;
    private final String[] mListSummaries;

    public CachedAppsFreezerPreferenceController(Context context) {
        super(context);

        mListValues = context.getResources().getStringArray(R.array.cached_apps_freezer_values);
        mListSummaries = context.getResources().getStringArray(
                R.array.cached_apps_freezer_entries);
    }

    @Override
    public boolean isAvailable() {
        boolean available = false;

        try {
            available = ActivityManager.getService().isAppFreezerSupported();
        } catch (RemoteException e) {
            Log.w(TAG, "Unable to obtain freezer support status from ActivityManager");
        }

        return available;
    }

    @Override
    public String getPreferenceKey() {
        return CACHED_APPS_FREEZER_KEY;
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        final String currentValue = Settings.Global.getString(mContext.getContentResolver(),
                Settings.Global.CACHED_APPS_FREEZER_ENABLED);

        if (!newValue.equals(currentValue)) {
            final AlertDialog dialog = new AlertDialog.Builder(mContext)
                    .setMessage(R.string.cached_apps_freezer_reboot_dialog_text)
                    .setPositiveButton(android.R.string.ok, getRebootDialogOkListener(newValue))
                    .setNegativeButton(android.R.string.cancel, getRebootDialogCancelListener())
                    .create();
            dialog.show();
        }

        return true;
    }

    private DialogInterface.OnClickListener getRebootDialogOkListener(Object newValue) {
        return (dialog, which) -> {
            Settings.Global.putString(mContext.getContentResolver(),
                    Settings.Global.CACHED_APPS_FREEZER_ENABLED,
                    newValue.toString());

            updateState(mPreference);

            PowerManager pm = mContext.getSystemService(PowerManager.class);
            pm.reboot(null);
        };
    }

    private DialogInterface.OnClickListener getRebootDialogCancelListener() {
        return (dialog, which) -> {
            updateState(mPreference);
        };
    }

    @Override
    public void updateState(Preference preference) {
        final ListPreference listPreference = (ListPreference) preference;
        final String currentValue = Settings.Global.getString(mContext.getContentResolver(),
                Settings.Global.CACHED_APPS_FREEZER_ENABLED);

        int index = 0; // Defaults to device default
        for (int i = 0; i < mListValues.length; i++) {
            if (TextUtils.equals(currentValue, mListValues[i])) {
                index = i;
                break;
            }
        }

        listPreference.setValue(mListValues[index]);
        listPreference.setSummary(mListSummaries[index]);
    }

    @Override
    public void onDeveloperOptionsDisabled() {
        super.onDeveloperOptionsDisabled();

        Settings.Global.putString(mContext.getContentResolver(),
                Settings.Global.CACHED_APPS_FREEZER_ENABLED,
                mListValues[0].toString());
    }
}
