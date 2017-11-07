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

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.usb.UsbManager;
import android.os.Bundle;
import android.support.annotation.VisibleForTesting;
import android.support.v7.preference.ListPreference;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceScreen;
import android.text.TextUtils;

import com.android.settings.R;
import com.android.settings.Utils;
import com.android.settings.core.PreferenceControllerMixin;
import com.android.settingslib.core.lifecycle.Lifecycle;
import com.android.settingslib.core.lifecycle.LifecycleObserver;
import com.android.settingslib.core.lifecycle.events.OnCreate;
import com.android.settingslib.core.lifecycle.events.OnDestroy;
import com.android.settingslib.development.DeveloperOptionsPreferenceController;

public class SelectUsbConfigPreferenceController extends
        DeveloperOptionsPreferenceController implements
        Preference.OnPreferenceChangeListener, LifecycleObserver, OnCreate, OnDestroy,
        PreferenceControllerMixin {

    private static final String USB_CONFIGURATION_KEY = "select_usb_configuration";

    private final String[] mListValues;
    private final String[] mListSummaries;
    private final UsbManager mUsbManager;
    private BroadcastReceiver mUsbReceiver;
    private ListPreference mPreference;

    public SelectUsbConfigPreferenceController(Context context, Lifecycle lifecycle) {
        super(context);

        mListValues = context.getResources().getStringArray(R.array.usb_configuration_values);
        mListSummaries = context.getResources().getStringArray(R.array.usb_configuration_titles);
        mUsbManager = (UsbManager) context.getSystemService(Context.USB_SERVICE);
        mUsbReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (mPreference != null) {
                    updateUsbConfigurationValues();
                }
            }
        };
        if (lifecycle != null) {
            lifecycle.addObserver(this);
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        IntentFilter filter = new IntentFilter();
        filter.addAction(UsbManager.ACTION_USB_STATE);
        mContext.registerReceiver(mUsbReceiver, filter);
    }

    @Override
    public String getPreferenceKey() {
        return USB_CONFIGURATION_KEY;
    }

    @Override
    public void displayPreference(PreferenceScreen screen) {
        super.displayPreference(screen);

        mPreference = (ListPreference) screen.findPreference(getPreferenceKey());
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        if (Utils.isMonkeyRunning()) {
            return false;
        }

        writeUsbConfigurationOption(newValue.toString());
        updateUsbConfigurationValues();
        return true;
    }

    @Override
    public void updateState(Preference preference) {
        updateUsbConfigurationValues();
    }

    @Override
    public void onDestroy() {
        mContext.unregisterReceiver(mUsbReceiver);
    }

    @Override
    protected void onDeveloperOptionsSwitchEnabled() {
        mPreference.setEnabled(true);
    }

    @Override
    protected void onDeveloperOptionsSwitchDisabled() {
        mPreference.setEnabled(false);
    }

    @VisibleForTesting
    void setCurrentFunction(String newValue, boolean usbDataUnlocked) {
        mUsbManager.setCurrentFunction(newValue, usbDataUnlocked);
    }

    private void updateUsbConfigurationValues() {
        int index = 0;
        for (int i = 0; i < mListValues.length; i++) {
            if (mUsbManager.isFunctionEnabled(mListValues[i])) {
                index = i;
                break;
            }
        }
        mPreference.setValue(mListValues[index]);
        mPreference.setSummary(mListSummaries[index]);
    }

    private void writeUsbConfigurationOption(String newValue) {
        if (TextUtils.equals(newValue, "none")) {
            setCurrentFunction(newValue, false);
        } else {
            setCurrentFunction(newValue, true);
        }
    }
}
