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

package com.android.settings.slices;

import android.content.ContentResolver;
import android.net.Uri;
import android.provider.SettingsSlicesContract;

/**
 * A utility class to check slice Uris for restriction.
 */
public class RestrictedSliceUtils {

    /**
     * Uri for the notifying open networks Slice.
     */
    private static final Uri NOTIFY_OPEN_NETWORKS_SLICE_URI = new Uri.Builder()
        .scheme(ContentResolver.SCHEME_CONTENT)
        .authority(SettingsSliceProvider.SLICE_AUTHORITY)
        .appendPath(SettingsSlicesContract.PATH_SETTING_ACTION)
        .appendPath("notify_open_networks")
        .build();

    /**
     * Uri for the auto turning on Wi-Fi Slice.
     */
    private static final Uri AUTO_TURN_ON_WIFI_SLICE_URI = new Uri.Builder()
        .scheme(ContentResolver.SCHEME_CONTENT)
        .authority(SettingsSliceProvider.SLICE_AUTHORITY)
        .appendPath(SettingsSlicesContract.PATH_SETTING_ACTION)
        .appendPath("enable_wifi_wakeup")
        .build();

    /**
     * Uri for the usb tethering Slice.
     */
    private static final Uri USB_TETHERING_SLICE_URI = new Uri.Builder()
        .scheme(ContentResolver.SCHEME_CONTENT)
        .authority(SettingsSliceProvider.SLICE_AUTHORITY)
        .appendPath(SettingsSlicesContract.PATH_SETTING_ACTION)
        .appendPath("enable_usb_tethering")
        .build();

    /**
     * Uri for the bluetooth tethering Slice.
     */
    private static final Uri BLUETOOTH_TETHERING_SLICE_URI = new Uri.Builder()
        .scheme(ContentResolver.SCHEME_CONTENT)
        .authority(SettingsSliceProvider.SLICE_AUTHORITY)
        .appendPath(SettingsSlicesContract.PATH_SETTING_ACTION)
        .appendPath("enable_bluetooth_tethering_2")
        .build();

    /**
     * Returns true if the slice Uri restricts access to guest user.
     */
    public static boolean isGuestRestricted(Uri sliceUri) {
        if (AUTO_TURN_ON_WIFI_SLICE_URI.equals(sliceUri)
            || NOTIFY_OPEN_NETWORKS_SLICE_URI.equals(sliceUri)
            || BLUETOOTH_TETHERING_SLICE_URI.equals(sliceUri)
            || USB_TETHERING_SLICE_URI.equals(sliceUri)
            || CustomSliceRegistry.MOBILE_DATA_SLICE_URI.equals(sliceUri)) {
            return true;
        }
        return false;
    }
}
