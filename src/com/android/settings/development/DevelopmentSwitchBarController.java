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
    private final DevelopmentSettings mSettings;
    private final DevelopmentSettingsDashboardFragment mNewSettings;

    /**
     * @deprecated in favor of the other constructor.
     */
    @Deprecated
    public DevelopmentSwitchBarController(DevelopmentSettings settings, SwitchBar switchBar,
            boolean isAvailable, Lifecycle lifecycle) {
        mSwitchBar = switchBar;
        mIsAvailable = isAvailable && !Utils.isMonkeyRunning();
        mSettings = settings;
        mNewSettings = null;

        if (mIsAvailable) {
            lifecycle.addObserver(this);
        } else {
            mSwitchBar.setEnabled(false);
        }
    }

    public DevelopmentSwitchBarController(DevelopmentSettingsDashboardFragment settings,
            SwitchBar switchBar, boolean isAvailable, Lifecycle lifecycle) {
        mSwitchBar = switchBar;
        mIsAvailable = isAvailable && !Utils.isMonkeyRunning();
        mSettings = null;
        mNewSettings = settings;

        if (mIsAvailable) {
            lifecycle.addObserver(this);
        } else {
            mSwitchBar.setEnabled(false);
        }
    }

    @Override
    public void onStart() {
        if (mSettings != null) {
            mSwitchBar.addOnSwitchChangeListener(mSettings);
        }
        if (mNewSettings != null) {
            final boolean developmentEnabledState = DevelopmentSettingsEnabler
                    .isDevelopmentSettingsEnabled(mNewSettings.getContext());
            mSwitchBar.setChecked(developmentEnabledState);
            mSwitchBar.addOnSwitchChangeListener(mNewSettings);
        }
    }

    @Override
    public void onStop() {
        if (mSettings != null) {
            mSwitchBar.removeOnSwitchChangeListener(mSettings);
        }
        if (mNewSettings != null) {
            mSwitchBar.removeOnSwitchChangeListener(mNewSettings);
        }
    }
}
