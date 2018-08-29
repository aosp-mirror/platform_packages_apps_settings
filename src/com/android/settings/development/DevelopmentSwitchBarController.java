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

import androidx.annotation.NonNull;

import com.android.settings.Utils;
import com.android.settings.widget.SwitchBar;
import com.android.settingslib.core.lifecycle.Lifecycle;
import com.android.settingslib.core.lifecycle.LifecycleObserver;
import com.android.settingslib.core.lifecycle.events.OnStart;
import com.android.settingslib.core.lifecycle.events.OnStop;
import com.android.settingslib.development.DevelopmentSettingsEnabler;

public class DevelopmentSwitchBarController implements LifecycleObserver, OnStart, OnStop {

    private final SwitchBar mSwitchBar;
    private final boolean mIsAvailable;
    private final DevelopmentSettingsDashboardFragment mSettings;

    public DevelopmentSwitchBarController(@NonNull DevelopmentSettingsDashboardFragment settings,
            SwitchBar switchBar, boolean isAvailable, Lifecycle lifecycle) {
        mSwitchBar = switchBar;
        mIsAvailable = isAvailable && !Utils.isMonkeyRunning();
        mSettings = settings;

        if (mIsAvailable) {
            lifecycle.addObserver(this);
        } else {
            mSwitchBar.setEnabled(false);
        }
    }

    @Override
    public void onStart() {
        final boolean developmentEnabledState = DevelopmentSettingsEnabler
                .isDevelopmentSettingsEnabled(mSettings.getContext());
        mSwitchBar.setChecked(developmentEnabledState);
        mSwitchBar.addOnSwitchChangeListener(mSettings);
    }

    @Override
    public void onStop() {
        mSwitchBar.removeOnSwitchChangeListener(mSettings);
    }
}
