/*
 * Copyright (C) 2024 The Android Open Source Project
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

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothStatusCodes;
import android.content.ContentResolver;
import android.content.Context;
import android.provider.Settings;
import android.sysprop.BluetoothProperties;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;
import androidx.preference.Preference;
import androidx.preference.SwitchPreferenceCompat;

import com.android.settings.core.PreferenceControllerMixin;
import com.android.settingslib.development.DeveloperOptionsPreferenceController;
import com.android.settingslib.flags.Flags;
import com.android.settingslib.utils.ThreadUtils;

/** Preference controller to enable / disable the Bluetooth LE audio sharing UI flow */
public class BluetoothLeAudioUiPreferenceController extends DeveloperOptionsPreferenceController
        implements Preference.OnPreferenceChangeListener,
                PreferenceControllerMixin,
                BluetoothLeAudioModePreferenceController.OnModeChangeListener {
    private static final String TAG = "BluetoothLeAudioUiPreferenceController";
    private static final String PREFERENCE_KEY = "bluetooth_leaudio_broadcast_ui";

    @VisibleForTesting
    static final String VALUE_KEY = "bluetooth_le_audio_sharing_ui_preview_enabled";

    @VisibleForTesting static final int VALUE_OFF = 0;
    @VisibleForTesting static final int VALUE_ON = 1;
    @VisibleForTesting static final int VALUE_UNSET = -1;
    @Nullable private final DevelopmentSettingsDashboardFragment mFragment;
    private final BluetoothAdapter mBluetoothAdapter;
    private boolean mCurrentSettingsValue = false;
    private boolean mShouldToggleCurrentValue = false;

    public BluetoothLeAudioUiPreferenceController(
            @NonNull Context context, @Nullable DevelopmentSettingsDashboardFragment fragment) {
        super(context);
        mFragment = fragment;
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
    }

    @Override
    public boolean isAvailable() {
        return Flags.audioSharingDeveloperOption()
                && BluetoothProperties.isProfileBapBroadcastSourceEnabled().orElse(false)
                && BluetoothProperties.isProfileBapBroadcastAssistEnabled().orElse(false);
    }

    @Override
    public boolean onPreferenceChange(@NonNull Preference preference, @Nullable Object newValue) {
        if (mFragment != null && newValue != null && (boolean) newValue != mCurrentSettingsValue) {
            mShouldToggleCurrentValue = true;
            BluetoothRebootDialog.show(mFragment);
        }
        return false;
    }

    @Override
    public void updateState(@NonNull Preference preference) {
        if (mBluetoothAdapter == null) {
            return;
        }
        var unused = ThreadUtils.postOnBackgroundThread(
                () -> {
                    boolean shouldEnable =
                            mBluetoothAdapter.isEnabled()
                                    && mBluetoothAdapter.isLeAudioBroadcastSourceSupported()
                                            == BluetoothStatusCodes.FEATURE_SUPPORTED
                                    && mBluetoothAdapter.isLeAudioBroadcastAssistantSupported()
                                            == BluetoothStatusCodes.FEATURE_SUPPORTED;
                    boolean valueOn =
                            Settings.Global.getInt(
                                            mContext.getContentResolver(), VALUE_KEY, VALUE_UNSET)
                                    == VALUE_ON;
                    mContext.getMainExecutor()
                            .execute(
                                    () -> {
                                        if (!shouldEnable && valueOn) {
                                            Log.e(
                                                    TAG,
                                                    "Error state: toggle disabled but current"
                                                            + " settings value is true.");
                                        }
                                        mCurrentSettingsValue = valueOn;
                                        preference.setEnabled(shouldEnable);
                                        ((SwitchPreferenceCompat) preference).setChecked(valueOn);
                                    });
                });
    }

    @Override
    public @NonNull String getPreferenceKey() {
        return PREFERENCE_KEY;
    }

    /** Called when the RebootDialog confirm is clicked. */
    public void onRebootDialogConfirmed() {
        if (isAvailable() && mShouldToggleCurrentValue) {
            // Blocking, ensure reboot happens after value is saved.
            Log.d(TAG, "onRebootDialogConfirmed(): setting value to " + !mCurrentSettingsValue);
            toggleSetting(mContext.getContentResolver(), !mCurrentSettingsValue);
        }
    }

    /** Called when the RebootDialog cancel is clicked. */
    public void onRebootDialogCanceled() {
        mShouldToggleCurrentValue = false;
    }

    @Override
    public void onBroadcastDisabled() {
        if (isAvailable() && mCurrentSettingsValue) {
            Log.d(TAG, "onBroadcastDisabled(): setting value to false");
            // Blocking, ensure reboot happens after value is saved.
            toggleSetting(mContext.getContentResolver(), false);
        }
    }

    private static void toggleSetting(ContentResolver contentResolver, boolean valueOn) {
        Settings.Global.putInt(contentResolver, VALUE_KEY, valueOn ? VALUE_ON : VALUE_OFF);
    }
}
