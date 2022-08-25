/*
 * Copyright (C) 2022 The Android Open Source Project
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
package com.android.settings.applications.specialaccess.turnscreenon;

import static android.app.AppOpsManager.MODE_ALLOWED;
import static android.app.AppOpsManager.MODE_ERRORED;
import static android.app.AppOpsManager.OP_TURN_SCREEN_ON;

import android.app.AppOpsManager;
import android.app.settings.SettingsEnums;
import android.os.Bundle;

import androidx.appcompat.app.AlertDialog;
import androidx.preference.Preference;
import androidx.preference.Preference.OnPreferenceChangeListener;
import androidx.preference.SwitchPreference;

import com.android.settings.R;
import com.android.settings.applications.AppInfoWithHeader;

/**
 * Detail page for turn screen on special app access.
 */
public class TurnScreenOnDetails extends AppInfoWithHeader
        implements OnPreferenceChangeListener {

    private static final String KEY_APP_OPS_SETTINGS_SWITCH = "app_ops_settings_switch";

    private SwitchPreference mSwitchPref;
    private AppOpsManager mAppOpsManager;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mAppOpsManager = this.getSystemService(AppOpsManager.class);

        // find preferences
        addPreferencesFromResource(R.xml.turn_screen_on_permissions_details);
        mSwitchPref = (SwitchPreference) findPreference(KEY_APP_OPS_SETTINGS_SWITCH);

        // set title/summary for all of them
        mSwitchPref.setTitle(R.string.allow_turn_screen_on);

        // install event listeners
        mSwitchPref.setOnPreferenceChangeListener(this);
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        if (preference == mSwitchPref) {
            setTurnScreenOnAppOp(mPackageInfo.applicationInfo.uid, mPackageName,
                    (Boolean) newValue);
            return true;
        }
        return false;
    }

    @Override
    protected boolean refreshUi() {
        boolean isAllowed = isTurnScreenOnAllowed(mAppOpsManager,
                mPackageInfo.applicationInfo.uid, mPackageName);
        mSwitchPref.setChecked(isAllowed);
        return true;
    }

    @Override
    protected AlertDialog createDialog(int id, int errorCode) {
        return null;
    }

    @Override
    public int getMetricsCategory() {
        return SettingsEnums.SETTINGS_MANAGE_TURN_SCREEN_ON;
    }

    /**
     * Sets whether the app associated with the given {@code packageName} is allowed to turn the
     * screen on.
     */
    void setTurnScreenOnAppOp(int uid, String packageName, boolean value) {
        final int newMode = value ? MODE_ALLOWED : MODE_ERRORED;
        mAppOpsManager.setMode(OP_TURN_SCREEN_ON, uid, packageName, newMode);
    }

    /**
     * @return whether the app associated with the given {@code packageName} is allowed to turn the
     * screen on.
     */
    static boolean isTurnScreenOnAllowed(AppOpsManager appOpsManager, int uid, String packageName) {
        return appOpsManager.checkOpNoThrow(OP_TURN_SCREEN_ON, uid, packageName) == MODE_ALLOWED;
    }

    /**
     * @return the summary for the current state of whether the app associated with the given
     * packageName is allowed to turn the screen on.
     */
    public static int getPreferenceSummary(AppOpsManager appOpsManager, int uid,
            String packageName) {
        final boolean enabled = TurnScreenOnDetails.isTurnScreenOnAllowed(appOpsManager, uid,
                packageName);
        return enabled ? R.string.app_permission_summary_allowed
                : R.string.app_permission_summary_not_allowed;
    }
}
