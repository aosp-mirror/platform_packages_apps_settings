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

package com.android.settings.accessibility;

import android.content.Context;

import androidx.preference.Preference;
import androidx.preference.PreferenceScreen;

import com.android.settings.bluetooth.BluetoothDeviceUpdater;
import com.android.settings.connecteddevice.DevicePreferenceCallback;
import com.android.settings.dashboard.DashboardFragment;
import com.android.settingslib.core.lifecycle.LifecycleObserver;
import com.android.settingslib.core.lifecycle.events.OnResume;
import com.android.settingslib.core.lifecycle.events.OnStart;
import com.android.settingslib.core.lifecycle.events.OnStop;

/**
 * Controller to update the {@link androidx.preference.PreferenceCategory} for all
 * saved ((bonded but not connected)) hearing devices, including ASHA and HAP profile.
 * Parent class {@link BaseBluetoothDevicePreferenceController} will use
 * {@link DevicePreferenceCallback} to add/remove {@link Preference}.
 */
public class SavedHearingDevicePreferenceController extends
        BaseBluetoothDevicePreferenceController implements LifecycleObserver, OnStart, OnResume,
        OnStop {

    private BluetoothDeviceUpdater mSavedHearingDeviceUpdater;

    public SavedHearingDevicePreferenceController(Context context,
            String preferenceKey) {
        super(context, preferenceKey);
    }

    /**
     * Initializes objects in this controller. Need to call this before onStart().
     *
     * <p>Should not call this more than 1 time.
     *
     * @param fragment The {@link DashboardFragment} uses the controller.
     */
    public void init(DashboardFragment fragment) {
        if (mSavedHearingDeviceUpdater != null) {
            throw new IllegalStateException("Should not call init() more than 1 time.");
        }
        mSavedHearingDeviceUpdater = new SavedHearingDeviceUpdater(fragment.getContext(), this,
                fragment.getMetricsCategory());
    }

    @Override
    public void onStart() {
        mSavedHearingDeviceUpdater.registerCallback();
    }

    @Override
    public void onResume() {
        mSavedHearingDeviceUpdater.refreshPreference();
    }

    @Override
    public void onStop() {
        mSavedHearingDeviceUpdater.unregisterCallback();
    }

    @Override
    public void displayPreference(PreferenceScreen screen) {
        super.displayPreference(screen);

        if (isAvailable()) {
            final Context context = screen.getContext();
            mSavedHearingDeviceUpdater.setPrefContext(context);
            mSavedHearingDeviceUpdater.forceUpdate();
        }
    }
}
