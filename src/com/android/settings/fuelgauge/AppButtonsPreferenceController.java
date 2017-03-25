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

package com.android.settings.fuelgauge;

import android.app.Activity;
import android.os.UserHandle;
import android.support.v7.preference.PreferenceScreen;
import android.view.View;
import android.widget.Button;

import com.android.settings.R;
import com.android.settings.applications.LayoutPreference;
import com.android.settings.core.PreferenceController;
import com.android.settings.core.lifecycle.Lifecycle;
import com.android.settings.core.lifecycle.LifecycleObserver;
import com.android.settings.core.lifecycle.events.OnResume;
import com.android.settingslib.applications.ApplicationsState;

/**
 * Controller to control the uninstall button and forcestop button
 */
//TODO(b/35810915): refine the button logic and make InstalledAppDetails use this controller
//TODO(b/35810915): add test for this file
public class AppButtonsPreferenceController extends PreferenceController implements
        LifecycleObserver, OnResume {
    private static final String KEY_ACTION_BUTTONS = "action_buttons";

    private ApplicationsState.AppEntry mAppEntry;
    private LayoutPreference mButtonsPref;
    private Button mForceStopButton;
    private Button mUninstallButton;

    public AppButtonsPreferenceController(Activity activity, Lifecycle lifecycle,
            String packageName) {
        super(activity);

        lifecycle.addObserver(this);
        ApplicationsState state = ApplicationsState.getInstance(activity.getApplication());

        if (packageName != null) {
            mAppEntry = state.getEntry(packageName, UserHandle.myUserId());
        }
    }

    @Override
    public boolean isAvailable() {
        return mAppEntry != null;
    }

    @Override
    public void displayPreference(PreferenceScreen screen) {
        super.displayPreference(screen);
        if (isAvailable()) {
            mButtonsPref = (LayoutPreference) screen.findPreference(KEY_ACTION_BUTTONS);

            mUninstallButton = (Button) mButtonsPref.findViewById(R.id.left_button);
            mUninstallButton.setText(R.string.uninstall_text);

            mForceStopButton = (Button) mButtonsPref.findViewById(R.id.right_button);
            mForceStopButton.setText(R.string.force_stop);
        }
    }

    @Override
    public String getPreferenceKey() {
        return KEY_ACTION_BUTTONS;
    }

    @Override
    public void onResume() {
        //TODO(b/35810915): check and update the status of buttons
    }
}
