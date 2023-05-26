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

import android.content.ContentResolver;
import android.content.Context;
import android.media.AudioAttributes;
import android.media.AudioDeviceAttributes;
import android.media.audiopolicy.AudioProductStrategy;
import android.util.Log;

import androidx.preference.ListPreference;
import androidx.preference.Preference;

import com.android.settings.core.BasePreferenceController;
import com.android.settingslib.bluetooth.CachedBluetoothDevice;
import com.android.settingslib.bluetooth.HearingAidAudioRoutingConstants;
import com.android.settingslib.bluetooth.HearingAidAudioRoutingHelper;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.primitives.Ints;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Abstract class for providing audio routing {@link ListPreference} common control for hearing
 * device specifically.
 */
public abstract class HearingDeviceAudioRoutingBasePreferenceController extends
        BasePreferenceController implements Preference.OnPreferenceChangeListener {

    private static final String TAG = "HARoutingBasePreferenceController";
    private static final boolean DEBUG = false;

    private final HearingAidAudioRoutingHelper mAudioRoutingHelper;
    private final HearingAidHelper mHearingAidHelper;

    public HearingDeviceAudioRoutingBasePreferenceController(Context context,
            String preferenceKey) {
        this(context, preferenceKey,
                new HearingAidAudioRoutingHelper(context),
                new HearingAidHelper(context));
    }

    @VisibleForTesting
    public HearingDeviceAudioRoutingBasePreferenceController(Context context,
            String preferenceKey, HearingAidAudioRoutingHelper audioRoutingHelper,
            HearingAidHelper hearingAidHelper) {
        super(context, preferenceKey);

        mAudioRoutingHelper = audioRoutingHelper;
        mHearingAidHelper = hearingAidHelper;
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
        final Integer routingValue = Ints.tryParse((String) newValue);

        saveRoutingValue(mContext, routingValue);
        final CachedBluetoothDevice device = mHearingAidHelper.getConnectedHearingAidDevice();
        if (device != null) {
            trySetAudioRoutingConfig(getSupportedAttributeList(),
                    mHearingAidHelper.getConnectedHearingAidDevice(), routingValue);
        }

        return true;
    }

    private void trySetAudioRoutingConfig(int[] audioAttributes,
            CachedBluetoothDevice hearingDevice,
            @HearingAidAudioRoutingConstants.RoutingValue int routingValue) {
        final List<AudioProductStrategy> supportedStrategies =
                mAudioRoutingHelper.getSupportedStrategies(audioAttributes);
        final AudioDeviceAttributes hearingDeviceAttributes =
                mAudioRoutingHelper.getMatchedHearingDeviceAttributes(hearingDevice);
        if (hearingDeviceAttributes == null) {
            if (DEBUG) {
                Log.d(TAG,
                        "Can not find expected AudioDeviceAttributes to config audio routing "
                                + "maybe device is offline: "
                                + hearingDevice.getDevice().getAnonymizedAddress());
            }
            return;
        }

        final boolean status = mAudioRoutingHelper.setPreferredDeviceRoutingStrategies(
                supportedStrategies, hearingDeviceAttributes, routingValue);

        if (!status) {
            final List<String> strategiesName = supportedStrategies.stream()
                    .map(AudioProductStrategy::getName)
                    .collect(Collectors.toList());
            Log.w(TAG, "routingMode: " + strategiesName + " routingValue: " + routingValue
                    + " fail to configure AudioProductStrategy");
        }
    }

    /**
     * Gets a list of usage values defined in {@link AudioAttributes} that are used to identify
     * {@link AudioProductStrategy} to configure audio routing.
     */
    protected abstract int[] getSupportedAttributeList();

    /**
     * Saves the routing value.
     *
     * @param context the valid context used to get the {@link ContentResolver}
     * @param routingValue one of the value defined in
     *                     {@link HearingAidAudioRoutingConstants.RoutingValue}
     */
    protected abstract void saveRoutingValue(Context context, int routingValue);

    /**
     * Restores the routing value and used to reflect status on ListPreference.
     *
     * @param context the valid context used to get the {@link ContentResolver}
     * @return one of the value defined in {@link HearingAidAudioRoutingConstants.RoutingValue}
     */
    protected abstract int restoreRoutingValue(Context context);
}
