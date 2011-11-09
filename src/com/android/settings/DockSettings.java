/*
 * Copyright (C) 2010 The Android Open Source Project
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

package com.android.settings;

import com.android.settings.bluetooth.DockEventReceiver;

import android.app.AlertDialog;
import android.app.Dialog;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.Preference;
import android.preference.PreferenceScreen;
import android.provider.Settings;

public class DockSettings extends SettingsPreferenceFragment {

    private static final int DIALOG_NOT_DOCKED = 1;
    private static final String KEY_AUDIO_SETTINGS = "dock_audio";
    private static final String KEY_DOCK_SOUNDS = "dock_sounds";
    private Preference mAudioSettings;
    private CheckBoxPreference mDockSounds;
    private Intent mDockIntent;

    private BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(Intent.ACTION_DOCK_EVENT)) {
                handleDockChange(intent);
            }
        }
    };

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.dock_settings);

        initDockSettings();
    }

    @Override
    public void onResume() {
        super.onResume();

        IntentFilter filter = new IntentFilter(Intent.ACTION_DOCK_EVENT);
        getActivity().registerReceiver(mReceiver, filter);
    }

    @Override
    public void onPause() {
        super.onPause();

        getActivity().unregisterReceiver(mReceiver);
    }

    private void initDockSettings() {
        ContentResolver resolver = getContentResolver();

        mAudioSettings = findPreference(KEY_AUDIO_SETTINGS);
        if (mAudioSettings != null) {
            mAudioSettings.setSummary(R.string.dock_audio_summary_none);
        }

        mDockSounds = (CheckBoxPreference) findPreference(KEY_DOCK_SOUNDS);
        mDockSounds.setPersistent(false);
        mDockSounds.setChecked(Settings.System.getInt(resolver,
                Settings.System.DOCK_SOUNDS_ENABLED, 0) != 0);
    }

    private void handleDockChange(Intent intent) {
        if (mAudioSettings != null) {
            int dockState = intent.getIntExtra(Intent.EXTRA_DOCK_STATE, 0);

            boolean isBluetooth = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE) != null;

            if (!isBluetooth) {
                // No dock audio if not on Bluetooth.
                mAudioSettings.setEnabled(false);
                mAudioSettings.setSummary(R.string.dock_audio_summary_unknown);
            } else {
                mAudioSettings.setEnabled(true);

                mDockIntent = intent;
                int resId = R.string.dock_audio_summary_unknown;
                switch (dockState) {
                case Intent.EXTRA_DOCK_STATE_CAR:
                    resId = R.string.dock_audio_summary_car;
                    break;
                case Intent.EXTRA_DOCK_STATE_DESK:
                case Intent.EXTRA_DOCK_STATE_LE_DESK:
                case Intent.EXTRA_DOCK_STATE_HE_DESK:
                    resId = R.string.dock_audio_summary_desk;
                    break;
                case Intent.EXTRA_DOCK_STATE_UNDOCKED:
                    resId = R.string.dock_audio_summary_none;
                }
                mAudioSettings.setSummary(resId);
            }

            if (dockState != Intent.EXTRA_DOCK_STATE_UNDOCKED) {
                // remove undocked dialog if currently showing.
                try {
                    removeDialog(DIALOG_NOT_DOCKED);
                } catch (IllegalArgumentException iae) {
                    // Maybe it was already dismissed
                }
            }
        }
    }

    @Override
    public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen, Preference preference) {
        if (preference == mAudioSettings) {
            int dockState = mDockIntent != null
                    ? mDockIntent.getIntExtra(Intent.EXTRA_DOCK_STATE, 0)
                    : Intent.EXTRA_DOCK_STATE_UNDOCKED;
            if (dockState == Intent.EXTRA_DOCK_STATE_UNDOCKED) {
                showDialog(DIALOG_NOT_DOCKED);
            } else {
                Intent i = new Intent(mDockIntent);
                i.setAction(DockEventReceiver.ACTION_DOCK_SHOW_UI);
                i.setClass(getActivity(), DockEventReceiver.class);
                getActivity().sendBroadcast(i);
            }
        } else if (preference == mDockSounds) {
            Settings.System.putInt(getContentResolver(), Settings.System.DOCK_SOUNDS_ENABLED,
                    mDockSounds.isChecked() ? 1 : 0);
        }

        return true;
    }

    @Override
    public Dialog onCreateDialog(int id) {
        if (id == DIALOG_NOT_DOCKED) {
            return createUndockedMessage();
        }
        return null;
    }

    private Dialog createUndockedMessage() {
        final AlertDialog.Builder ab = new AlertDialog.Builder(getActivity());
        ab.setTitle(R.string.dock_not_found_title);
        ab.setMessage(R.string.dock_not_found_text);
        ab.setPositiveButton(android.R.string.ok, null);
        return ab.create();
    }
}
