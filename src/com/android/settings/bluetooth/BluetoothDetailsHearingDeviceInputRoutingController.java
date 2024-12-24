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

package com.android.settings.bluetooth;

import static com.android.settings.bluetooth.BluetoothDetailsHearingDeviceController.KEY_HEARING_DEVICE_GROUP;
import static com.android.settings.bluetooth.BluetoothDetailsHearingDeviceController.ORDER_HEARING_DEVICE_INPUT_ROUTING;

import android.content.Context;
import android.media.AudioManager;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.preference.PreferenceCategory;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceScreen;

import com.android.settings.R;
import com.android.settings.bluetooth.HearingDeviceInputRoutingPreference.InputRoutingValue;
import com.android.settingslib.bluetooth.CachedBluetoothDevice;
import com.android.settingslib.bluetooth.HapClientProfile;
import com.android.settingslib.bluetooth.HearingAidAudioRoutingConstants;
import com.android.settingslib.bluetooth.HearingAidAudioRoutingHelper;
import com.android.settingslib.core.lifecycle.Lifecycle;

import java.util.Arrays;

/**
 * The controller of the hearing device input routing
 *
 * <p> It manages the input routing preference and update the routing according to the value.
 */
public class BluetoothDetailsHearingDeviceInputRoutingController extends
        BluetoothDetailsController implements
        HearingDeviceInputRoutingPreference.InputRoutingCallback {

    private static final String TAG = "BluetoothDetailsHearingDeviceInputRoutingController";
    static final String KEY_HEARING_DEVICE_INPUT_ROUTING = "hearing_device_input_routing";

    private final HearingAidAudioRoutingHelper mAudioRoutingHelper;
    private final AudioManager mAudioManager;

    public BluetoothDetailsHearingDeviceInputRoutingController(
            @NonNull Context context,
            @NonNull PreferenceFragmentCompat fragment,
            @NonNull CachedBluetoothDevice device,
            @NonNull Lifecycle lifecycle) {
        super(context, fragment, device, lifecycle);
        mAudioRoutingHelper = new HearingAidAudioRoutingHelper(context);
        mAudioManager = mContext.getSystemService(AudioManager.class);
    }

    @Override
    public boolean isAvailable() {
        boolean isSupportedProfile = mCachedDevice.getProfiles().stream().anyMatch(
                profile -> profile instanceof HapClientProfile);
        boolean isSupportedInputDevice = Arrays.stream(
                mAudioManager.getDevices(AudioManager.GET_DEVICES_INPUTS)).anyMatch(
                    info -> mCachedDevice.getAddress().equals(info.getAddress()));
        if (isSupportedProfile && !isSupportedInputDevice) {
            Log.d(TAG, "Not supported input type hearing device.");
        }
        return isSupportedProfile && isSupportedInputDevice;
    }

    @Override
    protected void init(PreferenceScreen screen) {
        PreferenceCategory hearingCategory = screen.findPreference(KEY_HEARING_DEVICE_GROUP);
        if (hearingCategory != null) {
            hearingCategory.addPreference(
                    createInputRoutingPreference(hearingCategory.getContext()));
        }
    }

    @Override
    protected void refresh() {}

    @Nullable
    @Override
    public String getPreferenceKey() {
        return KEY_HEARING_DEVICE_INPUT_ROUTING;
    }

    private HearingDeviceInputRoutingPreference createInputRoutingPreference(Context context) {
        HearingDeviceInputRoutingPreference pref = new HearingDeviceInputRoutingPreference(context);
        pref.setKey(KEY_HEARING_DEVICE_INPUT_ROUTING);
        pref.setOrder(ORDER_HEARING_DEVICE_INPUT_ROUTING);
        pref.setTitle(context.getString(R.string.bluetooth_hearing_device_input_routing_title));
        pref.setChecked(getUserPreferredInputRoutingValue());
        pref.setInputRoutingCallback(this);
        return pref;
    }

    @InputRoutingValue
    private int getUserPreferredInputRoutingValue() {
        return mCachedDevice.getDevice().isMicrophonePreferredForCalls()
                ? InputRoutingValue.HEARING_DEVICE : InputRoutingValue.BUILTIN_MIC;
    }

    @Override
    public void onInputRoutingUpdated(int selectedInputRoutingUiValue) {
        boolean useBuiltinMic =
                (selectedInputRoutingUiValue == InputRoutingValue.BUILTIN_MIC);
        boolean status = mAudioRoutingHelper.setPreferredInputDeviceForCalls(mCachedDevice,
                useBuiltinMic ? HearingAidAudioRoutingConstants.RoutingValue.BUILTIN_DEVICE
                        : HearingAidAudioRoutingConstants.RoutingValue.AUTO);
        if (!status) {
            Log.d(TAG, "Fail to configure setPreferredInputDeviceForCalls");
        }
        mCachedDevice.getDevice().setMicrophonePreferredForCalls(!useBuiltinMic);
    }
}
