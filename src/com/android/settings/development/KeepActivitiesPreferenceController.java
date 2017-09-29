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
import android.support.annotation.VisibleForTesting;
import android.support.v14.preference.SwitchPreference;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceScreen;

public class KeepActivitiesPreferenceController extends DeveloperOptionsPreferenceController
        implements Preference.OnPreferenceChangeListener {

    private static final String IMMEDIATELY_DESTROY_ACTIVITIES_KEY =
            "immediately_destroy_activities";

    @VisibleForTesting
    static final int SETTING_VALUE_OFF = 0;

    private IActivityManager mActivityManager;
    private SwitchPreference mPreference;

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

        mPreference = (SwitchPreference) screen.findPreference(getPreferenceKey());
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
        mPreference.setChecked(mode != SETTING_VALUE_OFF);
    }

    @Override
    protected void onDeveloperOptionsSwitchEnabled() {
        mPreference.setEnabled(true);
    }

    @Override
    protected void onDeveloperOptionsSwitchDisabled() {
        writeImmediatelyDestroyActivitiesOptions(false);
        mPreference.setEnabled(false);
        mPreference.setChecked(false);
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
