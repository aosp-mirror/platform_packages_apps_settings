/*
 * Copyright (C) 2018 The Android Open Source Project
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

package com.android.settings.slices;

import android.content.Context;
import android.net.Uri;
import android.util.ArrayMap;

import androidx.annotation.VisibleForTesting;

import com.android.settings.flashlight.FlashlightSlice;
import com.android.settings.homepage.contextualcards.deviceinfo.BatterySlice;
import com.android.settings.homepage.contextualcards.deviceinfo.DataUsageSlice;
import com.android.settings.homepage.contextualcards.deviceinfo.DeviceInfoSlice;
import com.android.settings.homepage.contextualcards.deviceinfo.EmergencyInfoSlice;
import com.android.settings.homepage.contextualcards.deviceinfo.StorageSlice;
import com.android.settings.homepage.contextualcards.slices.BatteryFixSlice;
import com.android.settings.homepage.contextualcards.slices.BluetoothDevicesSlice;
import com.android.settings.homepage.contextualcards.slices.LowStorageSlice;
import com.android.settings.location.LocationSlice;
import com.android.settings.wifi.slice.ContextualWifiSlice;
import com.android.settings.wifi.slice.WifiSlice;

import java.util.Map;
import java.util.WeakHashMap;

/**
 * Manages custom {@link androidx.slice.Slice Slices}, which are all Slices not backed by
 * preferences.
 */
public class CustomSliceManager {

    @VisibleForTesting
    final Map<Uri, Class<? extends CustomSliceable>> mUriMap;

    private final Context mContext;
    private final Map<Uri, CustomSliceable> mSliceableCache;

    public CustomSliceManager(Context context) {
        mContext = context.getApplicationContext();
        mUriMap = new ArrayMap<>();
        mSliceableCache = new WeakHashMap<>();
        addSlices();
    }

    /**
     * Return a {@link CustomSliceable} associated to the Uri.
     * <p>
     * Do not change this method signature to accommodate for a special-case slicable - a context is
     * the only thing that should be needed to create the object.
     */
    public CustomSliceable getSliceableFromUri(Uri uri) {
        if (mSliceableCache.containsKey(uri)) {
            return mSliceableCache.get(uri);
        }

        final Class clazz = mUriMap.get(uri);
        if (clazz == null) {
            throw new IllegalArgumentException("No Slice found for uri: " + uri);
        }

        final CustomSliceable sliceable = CustomSliceable.createInstance(mContext, clazz);
        mSliceableCache.put(uri, sliceable);
        return sliceable;
    }

    /**
     * Return a {@link CustomSliceable} associated to the Action.
     * <p>
     * Do not change this method signature to accommodate for a special-case sliceable - a context
     * is the only thing that should be needed to create the object.
     */
    public CustomSliceable getSliceableFromIntentAction(String action) {
        return getSliceableFromUri(Uri.parse(action));
    }

    /**
     * Returns {@code true} if {@param uri} is a valid Slice Uri handled by
     * {@link CustomSliceManager}.
     */
    public boolean isValidUri(Uri uri) {
        return mUriMap.containsKey(uri);
    }

    /**
     * Returns {@code true} if {@param action} is a valid intent action handled by
     * {@link CustomSliceManager}.
     */
    public boolean isValidAction(String action) {
        return isValidUri(Uri.parse(action));
    }

    private void addSlices() {
        mUriMap.put(CustomSliceRegistry.BATTERY_FIX_SLICE_URI, BatteryFixSlice.class);
        mUriMap.put(CustomSliceRegistry.BATTERY_INFO_SLICE_URI, BatterySlice.class);
        mUriMap.put(CustomSliceRegistry.BLUETOOTH_DEVICES_SLICE_URI, BluetoothDevicesSlice.class);
        mUriMap.put(CustomSliceRegistry.CONTEXTUAL_WIFI_SLICE_URI, ContextualWifiSlice.class);
        mUriMap.put(CustomSliceRegistry.DATA_USAGE_SLICE_URI, DataUsageSlice.class);
        mUriMap.put(CustomSliceRegistry.DEVICE_INFO_SLICE_URI, DeviceInfoSlice.class);
        mUriMap.put(CustomSliceRegistry.EMERGENCY_INFO_SLICE_URI, EmergencyInfoSlice.class);
        mUriMap.put(CustomSliceRegistry.FLASHLIGHT_SLICE_URI, FlashlightSlice.class);
        mUriMap.put(CustomSliceRegistry.LOCATION_SLICE_URI, LocationSlice.class);
        mUriMap.put(CustomSliceRegistry.LOW_STORAGE_SLICE_URI, LowStorageSlice.class);
        mUriMap.put(CustomSliceRegistry.STORAGE_SLICE_URI, StorageSlice.class);
        mUriMap.put(CustomSliceRegistry.WIFI_SLICE_URI, WifiSlice.class);
    }
}
