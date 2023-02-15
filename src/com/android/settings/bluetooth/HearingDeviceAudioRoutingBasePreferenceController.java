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

package com.android.settings.bluetooth;

import android.content.ContentResolver;
import android.content.Context;
import android.media.AudioAttributes;
import android.media.AudioDeviceAttributes;
import android.media.AudioDeviceInfo;
import android.media.AudioManager;
import android.media.audiopolicy.AudioProductStrategy;
import android.util.Log;

import androidx.annotation.IntDef;
import androidx.annotation.VisibleForTesting;
import androidx.preference.ListPreference;
import androidx.preference.Preference;

import com.android.settings.core.BasePreferenceController;
import com.android.settingslib.bluetooth.CachedBluetoothDevice;

import com.google.common.primitives.Ints;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Abstract class for providing audio routing {@link ListPreference} common control for hearing
 * device specifically.
 */
public abstract class HearingDeviceAudioRoutingBasePreferenceController extends
        BasePreferenceController implements Preference.OnPreferenceChangeListener {

    private static final String TAG = "HARoutingBasePreferenceController";

    private static final AudioDeviceAttributes DEVICE_SPEAKER_OUT = new AudioDeviceAttributes(
            AudioDeviceAttributes.ROLE_OUTPUT, AudioDeviceInfo.TYPE_BUILTIN_SPEAKER, "");

    private final AudioManager mAudioManager;

    public HearingDeviceAudioRoutingBasePreferenceController(Context context,
            String preferenceKey) {
        super(context, preferenceKey);
        mAudioManager = mContext.getSystemService(AudioManager.class);
    }

    @Override
    public int getAvailabilityStatus() {
        return AVAILABLE;
    }

    @Override
    public void updateState(Preference preference) {
        super.updateState(preference);

        final ListPreference listPreference = (ListPreference) preference;
        final int routingValue = restoreRoutingValue(mContext);
        listPreference.setValue(String.valueOf(routingValue));
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        final ListPreference listPreference = (ListPreference) preference;
        final Integer routingValue = Ints.tryParse((String) newValue);
        final AudioDeviceAttributes hearingDeviceAttribute = new AudioDeviceAttributes(
                AudioDeviceAttributes.ROLE_OUTPUT,
                AudioDeviceInfo.TYPE_HEARING_AID,
                getHearingDevice().getAddress());
        final List<AudioProductStrategy> supportedStrategies = getSupportedStrategies(
                getSupportedAttributeList());

        boolean status = false;
        if (routingValue != null) {
            switch (routingValue) {
                case RoutingValue.AUTO:
                    status = removePreferredDeviceForStrategies(supportedStrategies);
                    break;
                case RoutingValue.HEARING_DEVICE:
                    removePreferredDeviceForStrategies(supportedStrategies);
                    status = setPreferredDeviceForStrategies(supportedStrategies,
                            hearingDeviceAttribute);
                    break;
                case RoutingValue.DEVICE_SPEAKER:
                    removePreferredDeviceForStrategies(supportedStrategies);
                    status = setPreferredDeviceForStrategies(supportedStrategies,
                            DEVICE_SPEAKER_OUT);
                    break;
                default:
                    throw new IllegalArgumentException("Unexpected routingValue: " + routingValue);
            }
        }
        if (!status) {
            Log.w(TAG, "routingMode: " + listPreference.getKey() + "routingValue: " + routingValue
                    + " fail to configure AudioProductStrategy");
        }

        saveRoutingValue(mContext, routingValue);
        updateState(listPreference);
        return true;
    }

    /**
     * Gets a list of usage value defined in {@link AudioAttributes} that is used to configure
     * audio routing via {@link AudioProductStrategy}.
     */
    protected abstract int[] getSupportedAttributeList();

    /**
     * Gets the {@link CachedBluetoothDevice} hearing device that is used to configure audio
     * routing.
     */
    protected abstract CachedBluetoothDevice getHearingDevice();

    /**
     * Saves the {@link RoutingValue}.
     *
     * @param context the valid context used to get the {@link ContentResolver}
     * @param routingValue the value defined in {@link RoutingValue}
     */
    protected abstract void saveRoutingValue(Context context, int routingValue);

    /**
     * Restores the {@link RoutingValue} and used to reflect status on ListPreference.
     *
     * @param context the valid context used to get the {@link ContentResolver}
     * @return one of {@link RoutingValue}
     */
    protected abstract int restoreRoutingValue(Context context);

    private List<AudioProductStrategy> getSupportedStrategies(int[] attributeSdkUsageList) {
        final List<AudioAttributes> audioAttrList = new ArrayList<>(attributeSdkUsageList.length);
        for (int attributeSdkUsage : attributeSdkUsageList) {
            audioAttrList.add(new AudioAttributes.Builder().setUsage(attributeSdkUsage).build());
        }

        final List<AudioProductStrategy> allStrategies = getAudioProductStrategies();
        final List<AudioProductStrategy> supportedStrategies = new ArrayList<>();
        for (AudioProductStrategy strategy : allStrategies) {
            for (AudioAttributes audioAttr : audioAttrList) {
                if (strategy.supportsAudioAttributes(audioAttr)) {
                    supportedStrategies.add(strategy);
                }
            }
        }

        return supportedStrategies.stream().distinct().collect(Collectors.toList());
    }

    @VisibleForTesting
    List<AudioProductStrategy> getAudioProductStrategies() {
        return AudioManager.getAudioProductStrategies();
    }

    @VisibleForTesting
    boolean setPreferredDeviceForStrategies(List<AudioProductStrategy> strategies,
            AudioDeviceAttributes audioDevice) {
        boolean status = true;
        for (AudioProductStrategy strategy : strategies) {
            status &= mAudioManager.setPreferredDeviceForStrategy(strategy, audioDevice);
        }

        return status;
    }

    @VisibleForTesting
    boolean removePreferredDeviceForStrategies(List<AudioProductStrategy> strategies) {
        boolean status = true;
        for (AudioProductStrategy strategy : strategies) {
            status &= mAudioManager.removePreferredDeviceForStrategy(strategy);
        }

        return status;
    }

    @Retention(RetentionPolicy.SOURCE)
    @IntDef({
            RoutingValue.AUTO,
            RoutingValue.HEARING_DEVICE,
            RoutingValue.DEVICE_SPEAKER,
    })

    @VisibleForTesting
    protected @interface RoutingValue {
        int AUTO = 0;
        int HEARING_DEVICE = 1;
        int DEVICE_SPEAKER = 2;
    }
}
