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

package com.android.settings.bluetooth;

import android.content.Context;

import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceScreen;

import com.android.settings.core.PreferenceControllerMixin;
import com.android.settings.overlay.FeatureFactory;
import com.android.settingslib.bluetooth.CachedBluetoothDevice;
import com.android.settingslib.core.AbstractPreferenceController;
import com.android.settingslib.core.instrumentation.MetricsFeatureProvider;
import com.android.settingslib.core.lifecycle.Lifecycle;
import com.android.settingslib.core.lifecycle.LifecycleObserver;
import com.android.settingslib.core.lifecycle.events.OnPause;
import com.android.settingslib.core.lifecycle.events.OnResume;

/**
 * This class provides common lifecycle and bluetooth device event registration for Bluetooth device
 * details controllers.
 */
public abstract class BluetoothDetailsController extends AbstractPreferenceController
        implements PreferenceControllerMixin, CachedBluetoothDevice.Callback, LifecycleObserver,
        OnPause, OnResume {

    protected final Context mContext;
    protected final PreferenceFragmentCompat mFragment;
    protected final CachedBluetoothDevice mCachedDevice;
    protected final MetricsFeatureProvider mMetricsFeatureProvider;

    public BluetoothDetailsController(Context context, PreferenceFragmentCompat fragment,
            CachedBluetoothDevice device, Lifecycle lifecycle) {
        super(context);
        mContext = context;
        mFragment = fragment;
        mCachedDevice = device;
        lifecycle.addObserver(this);
        mMetricsFeatureProvider = FeatureFactory.getFeatureFactory().getMetricsFeatureProvider();
    }

    @Override
    public void onPause() {
        mCachedDevice.unregisterCallback(this);
    }

    @Override
    public void onResume() {
        mCachedDevice.registerCallback(this);
        refresh();
    }

    @Override
    public boolean isAvailable() {
        return true;
    }

    @Override
    public void onDeviceAttributesChanged() {
        refresh();
    }

    @Override
    public final void displayPreference(PreferenceScreen screen) {
        init(screen);
        super.displayPreference(screen);
    }

    /**
     * This is a method to do one-time initialization when the screen is first created, such as
     * adding preferences.
     * @param screen the screen where this controller's preferences should be added
     */
    protected abstract void init(PreferenceScreen screen);

    /**
     * This method is called when something about the bluetooth device has changed, and this object
     * should update the preferences it manages based on the new state.
     */
    protected abstract void refresh();
}