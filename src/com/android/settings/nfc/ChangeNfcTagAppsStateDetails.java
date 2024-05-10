/*
 * Copyright (C) 2023 The Android Open Source Project
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

package com.android.settings.nfc;

import android.app.settings.SettingsEnums;
import android.content.Context;
import android.os.Bundle;
import android.util.Log;

import androidx.appcompat.app.AlertDialog;
import androidx.preference.Preference;
import androidx.preference.Preference.OnPreferenceChangeListener;
import androidx.preference.TwoStatePreference;

import com.android.settings.R;
import com.android.settings.applications.AppInfoWithHeader;
import com.android.settings.nfc.AppStateNfcTagAppsBridge.NfcTagAppState;
import com.android.settingslib.applications.ApplicationsState.AppEntry;

/**
 * Class for displaying app info of the Nfc Tag App
 */
public class ChangeNfcTagAppsStateDetails extends AppInfoWithHeader
        implements OnPreferenceChangeListener {

    private static final String KEY_APP_OPS_SETTINGS_SWITCH = "app_ops_settings_switch";
    private static final String LOG_TAG = "ChangeNfcTagAppsStateDetails";

    private AppStateNfcTagAppsBridge mAppBridge;
    private TwoStatePreference mSwitchPref;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        final Context context = getActivity();
        mAppBridge = new AppStateNfcTagAppsBridge(context, mState, null);

        // find preferences
        addPreferencesFromResource(R.xml.change_nfc_tag_apps_details);
        mSwitchPref = (TwoStatePreference) findPreference(KEY_APP_OPS_SETTINGS_SWITCH);

        // set title/summary for all of them
        mSwitchPref.setTitle(R.string.change_nfc_tag_apps_detail_switch);

        // install event listeners
        mSwitchPref.setOnPreferenceChangeListener(this);

    }

    @Override
    protected AlertDialog createDialog(int id, int errorCode) {
        return null;
    }

    @Override
    public int getMetricsCategory() {
        return SettingsEnums.CONFIG_NFC_TAG_APP_PREF;
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        Boolean enable = (Boolean) newValue;
        if (preference == mSwitchPref) {
            if (mAppBridge != null && mAppBridge.updateApplist(mUserId, mPackageName, enable)) {
                refreshUi();
                return true;
            } else {
                Log.e(LOG_TAG, "Set [" + mPackageName + "]" + " failed.");
                return false;
            }
        }
        return false;
    }

    @Override
    protected boolean refreshUi() {
        if (mPackageInfo == null || mPackageInfo.applicationInfo == null) {
            return false;
        }
        retrieveAppEntry();
        NfcTagAppState state;
        if (mAppEntry.extraInfo instanceof NfcTagAppState) {
            state = (NfcTagAppState) mAppEntry.extraInfo;
        } else {
            state = new NfcTagAppState(/* exist */ false, /* allowed */ false);
        }
        mSwitchPref.setChecked(state.isAllowed());
        mSwitchPref.setEnabled(state.isExisted());
        return true;
    }

    /** Returns the summary string for this setting preference. */
    public static CharSequence getSummary(Context context, AppEntry entry) {
        NfcTagAppState state;
        if (entry.extraInfo instanceof NfcTagAppState) {
            state = (NfcTagAppState) entry.extraInfo;
        } else {
            state = new NfcTagAppState(/* exist */ false, /* allowed */ false);
        }
        return context.getString(state.isAllowed()
                ? R.string.app_permission_summary_allowed
                : R.string.app_permission_summary_not_allowed);
    }
}
