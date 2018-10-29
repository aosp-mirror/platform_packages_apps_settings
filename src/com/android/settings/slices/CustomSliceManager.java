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

import com.android.settings.homepage.contextualcards.deviceinfo.BatterySlice;
import com.android.settings.homepage.contextualcards.deviceinfo.DataUsageSlice;
import com.android.settings.homepage.contextualcards.deviceinfo.DeviceInfoSlice;
import com.android.settings.homepage.contextualcards.deviceinfo.StorageSlice;
import com.android.settings.homepage.contextualcards.slices.ConnectedDeviceSlice;
import com.android.settings.wifi.WifiSlice;

import java.util.Map;
import java.util.WeakHashMap;

/**
 * Manages custom {@link androidx.slice.Slice Slices}, which are all Slices not backed by
 * preferences.
 * <p>
 *     By default, all Slices in Settings should be built by a
 * </p>
 */
public class CustomSliceManager {

    protected final Map<Uri, Class<? extends CustomSliceable>> mUriMap;

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
        mUriMap.put(WifiSlice.WIFI_URI, WifiSlice.class);
        mUriMap.put(DataUsageSlice.DATA_USAGE_CARD_URI, DataUsageSlice.class);
        mUriMap.put(DeviceInfoSlice.DEVICE_INFO_CARD_URI, DeviceInfoSlice.class);
        mUriMap.put(StorageSlice.STORAGE_CARD_URI, StorageSlice.class);
        mUriMap.put(BatterySlice.BATTERY_CARD_URI, BatterySlice.class);
        mUriMap.put(ConnectedDeviceSlice.CONNECTED_DEVICE_URI, ConnectedDeviceSlice.class);
    }
}
