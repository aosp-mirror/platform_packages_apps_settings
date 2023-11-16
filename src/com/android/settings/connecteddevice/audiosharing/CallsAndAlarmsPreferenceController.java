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

package com.android.settings.connecteddevice.audiosharing;

import android.content.Context;
import android.util.Log;

import androidx.preference.PreferenceScreen;

import com.android.settings.dashboard.DashboardFragment;

/** PreferenceController to control the dialog to choose the active device for calls and alarms */
public class CallsAndAlarmsPreferenceController extends AudioSharingBasePreferenceController {

    private static final String TAG = "CallsAndAlarmsPreferenceController";

    private static final String PREF_KEY = "calls_and_alarms";
    private DashboardFragment mFragment;

    public CallsAndAlarmsPreferenceController(Context context) {
        super(context, PREF_KEY);
    }

    @Override
    public String getPreferenceKey() {
        return PREF_KEY;
    }

    @Override
    public void displayPreference(PreferenceScreen screen) {
        super.displayPreference(screen);
        mPreference.setOnPreferenceClickListener(
                preference -> {
                    if (mFragment != null) {
                        CallsAndAlarmsDialogFragment.show(mFragment);
                    } else {
                        Log.w(TAG, "Dialog fail to show due to null host.");
                    }
                    return true;
                });
    }

    /**
     * Initialize the controller.
     *
     * @param fragment The fragment to host the {@link CallsAndAlarmsDialogFragment} dialog.
     */
    public void init(DashboardFragment fragment) {
        this.mFragment = fragment;
    }
}
