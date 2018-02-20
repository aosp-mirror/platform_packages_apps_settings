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
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceScreen;

import com.android.internal.annotations.VisibleForTesting;
import com.android.internal.logging.nano.MetricsProto.MetricsEvent;
import com.android.settings.R;
import com.android.settings.core.InstrumentedPreferenceFragment;
import com.android.settings.core.PreferenceControllerMixin;
import com.android.settings.core.SubSettingLauncher;
import com.android.settings.overlay.FeatureFactory;
import com.android.settings.widget.MasterSwitchController;
import com.android.settings.widget.MasterSwitchPreference;
import com.android.settings.widget.SummaryUpdater.OnSummaryChangeListener;
import com.android.settingslib.bluetooth.LocalBluetoothManager;
import com.android.settingslib.core.AbstractPreferenceController;
import com.android.settingslib.core.lifecycle.LifecycleObserver;
import com.android.settingslib.core.lifecycle.events.OnPause;
import com.android.settingslib.core.lifecycle.events.OnResume;
import com.android.settingslib.core.lifecycle.events.OnStart;
import com.android.settingslib.core.lifecycle.events.OnStop;

//TODO(b/69926683): remove this controller in Android P.
public class BluetoothMasterSwitchPreferenceController extends AbstractPreferenceController
        implements PreferenceControllerMixin, OnSummaryChangeListener, LifecycleObserver, OnResume,
        OnPause, OnStart, OnStop {

    public static final String KEY_TOGGLE_BLUETOOTH = "toggle_bluetooth";

    private LocalBluetoothManager mBluetoothManager;
    private MasterSwitchPreference mBtPreference;
    private BluetoothEnabler mBluetoothEnabler;
    private BluetoothSummaryUpdater mSummaryUpdater;
    private RestrictionUtils mRestrictionUtils;
    private InstrumentedPreferenceFragment mFragment;

    public BluetoothMasterSwitchPreferenceController(Context context,
            LocalBluetoothManager bluetoothManager, InstrumentedPreferenceFragment fragment) {
        this(context, bluetoothManager, new RestrictionUtils(), fragment);
    }

    @VisibleForTesting
    public BluetoothMasterSwitchPreferenceController(Context context,
            LocalBluetoothManager bluetoothManager, RestrictionUtils restrictionUtils,
            InstrumentedPreferenceFragment fragment) {
        super(context);
        mBluetoothManager = bluetoothManager;
        mSummaryUpdater = new BluetoothSummaryUpdater(mContext, this, mBluetoothManager);
        mRestrictionUtils = restrictionUtils;
        mFragment = fragment;
    }

    @Override
    public void displayPreference(PreferenceScreen screen) {
        super.displayPreference(screen);
        mBtPreference = (MasterSwitchPreference) screen.findPreference(KEY_TOGGLE_BLUETOOTH);
        mBluetoothEnabler = new BluetoothEnabler(mContext,
                new MasterSwitchController(mBtPreference),
                FeatureFactory.getFactory(mContext).getMetricsFeatureProvider(), mBluetoothManager,
                MetricsEvent.ACTION_SETTINGS_MASTER_SWITCH_BLUETOOTH_TOGGLE,
                mRestrictionUtils);
    }

    @Override
    public boolean handlePreferenceTreeClick(Preference preference) {
        if (KEY_TOGGLE_BLUETOOTH.equals(preference.getKey())) {
            new SubSettingLauncher(mContext)
                    .setDestination(BluetoothSettings.class.getName())
                    .setTitle(R.string.bluetooth)
                    .setSourceMetricsCategory(mFragment.getMetricsCategory())
                    .launch();
            return true;
        }
        return super.handlePreferenceTreeClick(preference);
    }

    @Override
    public boolean isAvailable() {
        return true;
    }

    @Override
    public String getPreferenceKey() {
        return KEY_TOGGLE_BLUETOOTH;
    }

    @Override
    public void onResume() {
        mSummaryUpdater.register(true);
    }

    @Override
    public void onPause() {
        mSummaryUpdater.register(false);
    }

    @Override
    public void onStart() {
        if (mBluetoothEnabler != null) {
            mBluetoothEnabler.resume(mContext);
        }
    }

    @Override
    public void onStop() {
        if (mBluetoothEnabler != null) {
            mBluetoothEnabler.pause();
        }
    }

    @Override
    public void onSummaryChanged(String summary) {
        if (mBtPreference != null) {
            mBtPreference.setSummary(summary);
        }
    }

}
