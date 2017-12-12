/**
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

package com.android.settings.deviceinfo.storage;

import android.app.ActivityManager;
import android.app.FragmentManager;
import android.content.Context;
import android.os.SystemProperties;
import android.provider.Settings;
import android.support.annotation.VisibleForTesting;
import android.support.v7.preference.PreferenceScreen;
import com.android.internal.logging.nano.MetricsProto.MetricsEvent;
import com.android.settings.core.PreferenceControllerMixin;
import com.android.settings.core.instrumentation.MetricsFeatureProvider;
import com.android.settings.deletionhelper.ActivationWarningFragment;
import com.android.settings.widget.MasterSwitchController;
import com.android.settings.widget.MasterSwitchPreference;
import com.android.settings.widget.SwitchWidgetController;
import com.android.settingslib.core.AbstractPreferenceController;
import com.android.settingslib.core.lifecycle.LifecycleObserver;
import com.android.settingslib.core.lifecycle.events.OnResume;

public class AutomaticStorageManagementSwitchPreferenceController extends
        AbstractPreferenceController implements PreferenceControllerMixin, LifecycleObserver,
        OnResume, SwitchWidgetController.OnSwitchChangeListener {
    private static final String KEY_TOGGLE_ASM = "toggle_asm";
    @VisibleForTesting
    static final String STORAGE_MANAGER_ENABLED_BY_DEFAULT_PROPERTY = "ro.storage_manager.enabled";

    private MasterSwitchPreference mSwitch;
    private MasterSwitchController mSwitchController;
    private final MetricsFeatureProvider mMetricsFeatureProvider;
    private final FragmentManager mFragmentManager;

    public AutomaticStorageManagementSwitchPreferenceController(Context context,
            MetricsFeatureProvider metricsFeatureProvider, FragmentManager fragmentManager) {
        super(context);
        mMetricsFeatureProvider = metricsFeatureProvider;
        mFragmentManager = fragmentManager;
    }

    @Override
    public void displayPreference(PreferenceScreen screen) {
        super.displayPreference(screen);
        mSwitch = (MasterSwitchPreference) screen.findPreference(KEY_TOGGLE_ASM);
    }

    @Override
    public boolean isAvailable() {
        return !ActivityManager.isLowRamDeviceStatic();
    }

    @Override
    public String getPreferenceKey() {
        return KEY_TOGGLE_ASM;
    }

    @Override
    public void onResume() {
        if (!isAvailable()) {
            return;
        }
        boolean isStorageManagerEnabled = Settings.Secure.getInt(mContext.getContentResolver(),
                Settings.Secure.AUTOMATIC_STORAGE_MANAGER_ENABLED, 0) != 0;
        mSwitch.setChecked(isStorageManagerEnabled);

        if (mSwitch != null) {
            mSwitchController = new MasterSwitchController(mSwitch);
            mSwitchController.setListener(this);
            mSwitchController.startListening();
        }
    }

    @Override
    public boolean onSwitchToggled(boolean isChecked) {
        mMetricsFeatureProvider.action(mContext,
                MetricsEvent.ACTION_TOGGLE_STORAGE_MANAGER, isChecked);
        Settings.Secure.putInt(mContext.getContentResolver(),
                Settings.Secure.AUTOMATIC_STORAGE_MANAGER_ENABLED,
                isChecked ? 1 : 0);

        final boolean storageManagerEnabledByDefault =
                SystemProperties.getBoolean(STORAGE_MANAGER_ENABLED_BY_DEFAULT_PROPERTY, false);
        final boolean storageManagerDisabledByPolicy =
                Settings.Secure.getInt(
                                mContext.getContentResolver(),
                                Settings.Secure.AUTOMATIC_STORAGE_MANAGER_TURNED_OFF_BY_POLICY,
                                0)
                        != 0;
        // Show warning if it is disabled by default and turning it on or if it was disabled by
        // policy and we're turning it on.
        if ((isChecked && (!storageManagerEnabledByDefault || storageManagerDisabledByPolicy))) {
            ActivationWarningFragment fragment = ActivationWarningFragment.newInstance();
            fragment.show(mFragmentManager, ActivationWarningFragment.TAG);
        }

        return true;
    }
}
