/*
 * Copyright (C) 2017 The Android Open Source Project
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
import android.app.IActivityManager;
import android.content.Context;
import android.os.RemoteException;
import android.provider.Settings;

import androidx.annotation.VisibleForTesting;
import androidx.preference.Preference;
import androidx.preference.PreferenceScreen;
import androidx.preference.SwitchPreference;

import com.android.settings.core.PreferenceControllerMixin;
import com.android.settingslib.development.DeveloperOptionsPreferenceController;

public class KeepActivitiesPreferenceController extends DeveloperOptionsPreferenceController
        implements Preference.OnPreferenceChangeListener, PreferenceControllerMixin {

    private static final String IMMEDIATELY_DESTROY_ACTIVITIES_KEY =
            "immediately_destroy_activities";

    @VisibleForTesting
    static final int SETTING_VALUE_OFF = 0;

    private IActivityManager mActivityManager;

    public KeepActivitiesPreferenceController(Context context) {
        super(context);
    }

    @Override
    public String getPreferenceKey() {
        return IMMEDIATELY_DESTROY_ACTIVITIES_KEY;
    }

    @Override
    public void displayPreference(PreferenceScreen screen) {
        super.displayPreference(screen);

        mActivityManager = getActivityManager();
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        final boolean isEnabled = (Boolean) newValue;
        writeImmediatelyDestroyActivitiesOptions(isEnabled);
        return true;
    }

    @Override
    public void updateState(Preference preference) {
        final int mode = Settings.Global.getInt(mContext.getContentResolver(),
                Settings.Global.ALWAYS_FINISH_ACTIVITIES, SETTING_VALUE_OFF);
        ((SwitchPreference) mPreference).setChecked(mode != SETTING_VALUE_OFF);
    }

    @Override
    protected void onDeveloperOptionsSwitchDisabled() {
        super.onDeveloperOptionsSwitchDisabled();
        writeImmediatelyDestroyActivitiesOptions(false);
        ((SwitchPreference) mPreference).setChecked(false);
    }

    private void writeImmediatelyDestroyActivitiesOptions(boolean isEnabled) {
        try {
            mActivityManager.setAlwaysFinish(isEnabled);
        } catch (RemoteException ex) {
            // intentional no-op
        }
    }

    @VisibleForTesting
    IActivityManager getActivityManager() {
        return ActivityManager.getService();
    }
}
