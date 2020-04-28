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

import static android.provider.SettingsSlicesContract.KEY_LOCATION;
import static android.provider.SettingsSlicesContract.KEY_WIFI;

import android.content.ContentResolver;
import android.net.Uri;
import android.provider.SettingsSlicesContract;
import android.util.ArrayMap;

import androidx.annotation.VisibleForTesting;

import com.android.settings.display.AdaptiveSleepPreferenceController;
import com.android.settings.flashlight.FlashlightSlice;
import com.android.settings.fuelgauge.batterytip.BatteryTipPreferenceController;
import com.android.settings.homepage.contextualcards.deviceinfo.DataUsageSlice;
import com.android.settings.homepage.contextualcards.deviceinfo.DeviceInfoSlice;
import com.android.settings.homepage.contextualcards.deviceinfo.EmergencyInfoSlice;
import com.android.settings.homepage.contextualcards.deviceinfo.StorageSlice;
import com.android.settings.homepage.contextualcards.slices.BatteryFixSlice;
import com.android.settings.homepage.contextualcards.slices.BluetoothDevicesSlice;
import com.android.settings.homepage.contextualcards.slices.ContextualAdaptiveSleepSlice;
import com.android.settings.homepage.contextualcards.slices.ContextualNotificationChannelSlice;
import com.android.settings.homepage.contextualcards.slices.DarkThemeSlice;
import com.android.settings.homepage.contextualcards.slices.FaceSetupSlice;
import com.android.settings.homepage.contextualcards.slices.LowStorageSlice;
import com.android.settings.homepage.contextualcards.slices.NotificationChannelSlice;
import com.android.settings.location.LocationSlice;
import com.android.settings.media.MediaOutputIndicatorSlice;
import com.android.settings.media.MediaOutputSlice;
import com.android.settings.network.telephony.MobileDataSlice;
import com.android.settings.notification.ZenModeButtonPreferenceController;
import com.android.settings.wifi.calling.WifiCallingSliceHelper;
import com.android.settings.wifi.slice.ContextualWifiSlice;
import com.android.settings.wifi.slice.WifiSlice;
import com.android.settingslib.media.MediaOutputSliceConstants;

import java.util.Map;

/**
 * A registry of custom slice Uris.
 */
public class CustomSliceRegistry {

    /**
     * Uri for Airplane mode Slice.
     */
    public static final Uri AIRPLANE_URI = new Uri.Builder()
            .scheme(ContentResolver.SCHEME_CONTENT)
            .authority(SettingsSlicesContract.AUTHORITY)
            .appendPath(SettingsSlicesContract.PATH_SETTING_ACTION)
            .appendPath(SettingsSlicesContract.KEY_AIRPLANE_MODE)
            .build();

    /**
     *  Uri for Contextual Adaptive Sleep Slice
     */
    public static final Uri CONTEXTUAL_ADAPTIVE_SLEEP_URI = new Uri.Builder()
            .scheme(ContentResolver.SCHEME_CONTENT)
            .authority(SettingsSliceProvider.SLICE_AUTHORITY)
            .appendPath(SettingsSlicesContract.PATH_SETTING_INTENT)
            .appendPath(AdaptiveSleepPreferenceController.PREF_NAME)
            .build();

    /**
     * Uri for Battery Fix Slice.
     */
    public static final Uri BATTERY_FIX_SLICE_URI = new Uri.Builder()
            .scheme(ContentResolver.SCHEME_CONTENT)
            .authority(SettingsSliceProvider.SLICE_AUTHORITY)
            .appendEncodedPath(SettingsSlicesContract.PATH_SETTING_INTENT)
            .appendPath(BatteryTipPreferenceController.PREF_NAME)
            .build();

    /**
     * Backing Uri for the Bluetooth Slice.
     */
    public static final Uri BLUETOOTH_URI = new Uri.Builder()
            .scheme(ContentResolver.SCHEME_CONTENT)
            .authority(SettingsSlicesContract.AUTHORITY)
            .appendPath(SettingsSlicesContract.PATH_SETTING_ACTION)
            .appendPath(SettingsSlicesContract.KEY_BLUETOOTH)
            .build();

    /**
     * Backing Uri for Bluetooth devices Slice.
     */
    public static final Uri BLUETOOTH_DEVICES_SLICE_URI = new Uri.Builder()
            .scheme(ContentResolver.SCHEME_CONTENT)
            .authority(SettingsSliceProvider.SLICE_AUTHORITY)
            .appendPath(SettingsSlicesContract.PATH_SETTING_ACTION)
            .appendPath("bluetooth_devices")
            .build();

    /**
     * Backing Uri for Contextual Notification channel Slice.
     */
    public static final Uri CONTEXTUAL_NOTIFICATION_CHANNEL_SLICE_URI = new Uri.Builder()
            .scheme(ContentResolver.SCHEME_CONTENT)
            .authority(SettingsSliceProvider.SLICE_AUTHORITY)
            .appendPath(SettingsSlicesContract.PATH_SETTING_ACTION)
            .appendPath("contextual_notification_channel")
            .build();

    /**
     * Backing Uri for the Wifi Slice.
     */
    public static final Uri CONTEXTUAL_WIFI_SLICE_URI = new Uri.Builder()
            .scheme(ContentResolver.SCHEME_CONTENT)
            .authority(SettingsSlicesContract.AUTHORITY)
            .appendPath(SettingsSlicesContract.PATH_SETTING_ACTION)
            .appendPath("contextual_wifi")
            .build();

    /**
     * Backing Uri for the Data usage Slice.
     */
    public static final Uri DATA_USAGE_SLICE_URI = new Uri.Builder()
            .scheme(ContentResolver.SCHEME_CONTENT)
            .authority(SettingsSliceProvider.SLICE_AUTHORITY)
            .appendPath(SettingsSlicesContract.PATH_SETTING_ACTION)
            .appendPath("data_usage_card")
            .build();
    /**
     * Backing Uri for the Device info Slice.
     */
    public static final Uri DEVICE_INFO_SLICE_URI = new Uri.Builder()
            .scheme(ContentResolver.SCHEME_CONTENT)
            .authority(SettingsSliceProvider.SLICE_AUTHORITY)
            .appendPath(SettingsSlicesContract.PATH_SETTING_INTENT)
            .appendPath("device_info_card")
            .build();
    /**
     * Backing Uri for the Emergency Info Slice.
     */
    public static final Uri EMERGENCY_INFO_SLICE_URI = new Uri.Builder()
            .scheme(ContentResolver.SCHEME_CONTENT)
            .authority(SettingsSliceProvider.SLICE_AUTHORITY)
            .appendPath(SettingsSlicesContract.PATH_SETTING_INTENT)
            .appendPath("emergency_info_card")
            .build();
    /**
     * Slice Uri for Enhanced 4G slice
     */
    public static final Uri ENHANCED_4G_SLICE_URI = new Uri.Builder()
            .scheme(ContentResolver.SCHEME_CONTENT)
            .authority(SettingsSliceProvider.SLICE_AUTHORITY)
            .appendPath(SettingsSlicesContract.PATH_SETTING_ACTION)
            .appendPath("enhanced_4g_lte")
            .build();
    /**
     * Slice Uri for Face Enrollment
     */
    public static final Uri FACE_ENROLL_SLICE_URI = new Uri.Builder()
            .scheme(ContentResolver.SCHEME_CONTENT)
            .authority(SettingsSliceProvider.SLICE_AUTHORITY)
            .appendPath(SettingsSlicesContract.PATH_SETTING_ACTION)
            .appendPath("face_unlock_greeting_card")
            .build();
    /**
     * Backing Uri for the Flashlight Slice.
     */
    public static final Uri FLASHLIGHT_SLICE_URI = new Uri.Builder()
            .scheme(ContentResolver.SCHEME_CONTENT)
            .authority(SettingsSliceProvider.SLICE_AUTHORITY)
            .appendPath(SettingsSlicesContract.PATH_SETTING_ACTION)
            .appendPath("flashlight")
            .build();
    /**
     * Backing Uri for the Location Slice.
     */
    public static final Uri LOCATION_SLICE_URI = new Uri.Builder()
            .scheme(ContentResolver.SCHEME_CONTENT)
            .authority(SettingsSlicesContract.AUTHORITY)
            .appendPath(SettingsSlicesContract.PATH_SETTING_ACTION)
            .appendPath(KEY_LOCATION)
            .build();
    /**
     * Backing Uri for Low storage Slice.
     */
    public static final Uri LOW_STORAGE_SLICE_URI = new Uri.Builder()
            .scheme(ContentResolver.SCHEME_CONTENT)
            .authority(SettingsSliceProvider.SLICE_AUTHORITY)
            .appendEncodedPath(SettingsSlicesContract.PATH_SETTING_INTENT)
            .appendPath("low_storage")
            .build();
    /**
     * Backing Uri for NFC Slice
     */
    public static final Uri NFC_SLICE_URI = new Uri.Builder()
            .scheme(ContentResolver.SCHEME_CONTENT)
            .authority(SettingsSliceProvider.SLICE_AUTHORITY)
            .appendPath(SettingsSlicesContract.PATH_SETTING_ACTION)
            .appendPath("toggle_nfc")
            .build();

    /**
     * Backing Uri for Mobile Data Slice.
     */
    public static final Uri MOBILE_DATA_SLICE_URI = new Uri.Builder()
            .scheme(ContentResolver.SCHEME_CONTENT)
            .authority(SettingsSliceProvider.SLICE_AUTHORITY)
            .appendEncodedPath(SettingsSlicesContract.PATH_SETTING_ACTION)
            .appendPath("mobile_data")
            .build();
    /**
     * Backing Uri for Notification channel Slice.
     */
    public static final Uri NOTIFICATION_CHANNEL_SLICE_URI = new Uri.Builder()
            .scheme(ContentResolver.SCHEME_CONTENT)
            .authority(SettingsSliceProvider.SLICE_AUTHORITY)
            .appendPath(SettingsSlicesContract.PATH_SETTING_ACTION)
            .appendPath("notification_channel")
            .build();
    /**
     * Backing Uri for the storage slice.
     */
    public static final Uri STORAGE_SLICE_URI = new Uri.Builder()
            .scheme(ContentResolver.SCHEME_CONTENT)
            .authority(SettingsSliceProvider.SLICE_AUTHORITY)
            .appendPath(SettingsSlicesContract.PATH_SETTING_INTENT)
            .appendPath("storage_card")
            .build();
    /**
     * Full {@link Uri} for the Alarm volume Slice.
     */
    public static final Uri VOLUME_ALARM_URI = new Uri.Builder()
            .scheme(ContentResolver.SCHEME_CONTENT)
            .authority(SettingsSliceProvider.SLICE_AUTHORITY)
            .appendPath(SettingsSlicesContract.PATH_SETTING_ACTION)
            .appendPath("alarm_volume")
            .build();
    /**
     * Full {@link Uri} for the Call Volume Slice.
     */
    public static final Uri VOLUME_CALL_URI = new Uri.Builder()
            .scheme(ContentResolver.SCHEME_CONTENT)
            .authority(SettingsSliceProvider.SLICE_AUTHORITY)
            .appendPath(SettingsSlicesContract.PATH_SETTING_ACTION)
            .appendPath("call_volume")
            .build();
    /**
     * Full {@link Uri} for the Media Volume Slice.
     */
    public static final Uri VOLUME_MEDIA_URI = new Uri.Builder()
            .scheme(ContentResolver.SCHEME_CONTENT)
            .authority(SettingsSliceProvider.SLICE_AUTHORITY)
            .appendPath(SettingsSlicesContract.PATH_SETTING_ACTION)
            .appendPath("media_volume")
            .build();

    /**
     * Full {@link Uri} for the Remote Media Volume Slice.
     */
    public static final Uri VOLUME_REMOTE_MEDIA_URI = new Uri.Builder()
            .scheme(ContentResolver.SCHEME_CONTENT)
            .authority(SettingsSliceProvider.SLICE_AUTHORITY)
            .appendPath(SettingsSlicesContract.PATH_SETTING_ACTION)
            .appendPath("remote_volume")
            .build();

    /**
     * Full {@link Uri} for the Ringer volume Slice.
     */
    public static final Uri VOLUME_RINGER_URI = new Uri.Builder()
            .scheme(ContentResolver.SCHEME_CONTENT)
            .authority(SettingsSliceProvider.SLICE_AUTHORITY)
            .appendPath(SettingsSlicesContract.PATH_SETTING_ACTION)
            .appendPath("ring_volume")
            .build();

    /**
     * Full {@link Uri} for the Wifi Calling Slice.
     */
    public static final Uri WIFI_CALLING_URI = new Uri.Builder()
            .scheme(ContentResolver.SCHEME_CONTENT)
            .authority(SettingsSliceProvider.SLICE_AUTHORITY)
            .appendPath(SettingsSlicesContract.PATH_SETTING_INTENT)
            .appendPath(WifiCallingSliceHelper.PATH_WIFI_CALLING)
            .build();
    /**
     * Full {@link Uri} for the Wifi Calling Preference Slice.
     */
    public static final Uri WIFI_CALLING_PREFERENCE_URI = new Uri.Builder()
            .scheme(ContentResolver.SCHEME_CONTENT)
            .authority(SettingsSliceProvider.SLICE_AUTHORITY)
            .appendPath(SettingsSlicesContract.PATH_SETTING_ACTION)
            .appendPath(WifiCallingSliceHelper.PATH_WIFI_CALLING_PREFERENCE)
            .build();
    /**
     * Backing Uri for the Wifi Slice.
     */
    public static final Uri WIFI_SLICE_URI = new Uri.Builder()
            .scheme(ContentResolver.SCHEME_CONTENT)
            .authority(SettingsSlicesContract.AUTHORITY)
            .appendPath(SettingsSlicesContract.PATH_SETTING_ACTION)
            .appendPath(KEY_WIFI)
            .build();

    /**
     * Backing Uri for the Zen Mode Slice.
     */
    public static final Uri ZEN_MODE_SLICE_URI = new Uri.Builder()
            .scheme(ContentResolver.SCHEME_CONTENT)
            .authority(SettingsSliceProvider.SLICE_AUTHORITY)
            .appendPath(SettingsSlicesContract.PATH_SETTING_ACTION)
            .appendPath(ZenModeButtonPreferenceController.KEY)
            .build();

    /**
     * Backing Uri for the Media output Slice.
     */
    public static Uri MEDIA_OUTPUT_SLICE_URI = new Uri.Builder()
            .scheme(ContentResolver.SCHEME_CONTENT)
            .authority(SettingsSliceProvider.SLICE_AUTHORITY)
            .appendPath(SettingsSlicesContract.PATH_SETTING_ACTION)
            .appendPath(MediaOutputSliceConstants.KEY_MEDIA_OUTPUT)
            .build();

    /**
     * Backing Uri for the Media output indicator Slice.
     */
    public static Uri MEDIA_OUTPUT_INDICATOR_SLICE_URI = new Uri.Builder()
            .scheme(ContentResolver.SCHEME_CONTENT)
            .authority(SettingsSliceProvider.SLICE_AUTHORITY)
            .appendPath(SettingsSlicesContract.PATH_SETTING_INTENT)
            .appendPath("media_output_indicator")
            .build();

    /**
     * Backing Uri for the Dark theme Slice.
     */
    public static final Uri DARK_THEME_SLICE_URI = new Uri.Builder()
            .scheme(ContentResolver.SCHEME_CONTENT)
            .authority(SettingsSliceProvider.SLICE_AUTHORITY)
            .appendPath(SettingsSlicesContract.PATH_SETTING_ACTION)
            .appendPath("dark_theme")
            .build();

    @VisibleForTesting
    static final Map<Uri, Class<? extends CustomSliceable>> sUriToSlice;

    static {
        sUriToSlice = new ArrayMap<>();

        sUriToSlice.put(BATTERY_FIX_SLICE_URI, BatteryFixSlice.class);
        sUriToSlice.put(BLUETOOTH_DEVICES_SLICE_URI, BluetoothDevicesSlice.class);
        sUriToSlice.put(CONTEXTUAL_ADAPTIVE_SLEEP_URI, ContextualAdaptiveSleepSlice.class);
        sUriToSlice.put(CONTEXTUAL_NOTIFICATION_CHANNEL_SLICE_URI,
                ContextualNotificationChannelSlice.class);
        sUriToSlice.put(CONTEXTUAL_WIFI_SLICE_URI, ContextualWifiSlice.class);
        sUriToSlice.put(DATA_USAGE_SLICE_URI, DataUsageSlice.class);
        sUriToSlice.put(DEVICE_INFO_SLICE_URI, DeviceInfoSlice.class);
        sUriToSlice.put(EMERGENCY_INFO_SLICE_URI, EmergencyInfoSlice.class);
        sUriToSlice.put(FACE_ENROLL_SLICE_URI, FaceSetupSlice.class);
        sUriToSlice.put(FLASHLIGHT_SLICE_URI, FlashlightSlice.class);
        sUriToSlice.put(LOCATION_SLICE_URI, LocationSlice.class);
        sUriToSlice.put(LOW_STORAGE_SLICE_URI, LowStorageSlice.class);
        sUriToSlice.put(MEDIA_OUTPUT_INDICATOR_SLICE_URI, MediaOutputIndicatorSlice.class);
        sUriToSlice.put(MEDIA_OUTPUT_SLICE_URI, MediaOutputSlice.class);
        sUriToSlice.put(MOBILE_DATA_SLICE_URI, MobileDataSlice.class);
        sUriToSlice.put(NOTIFICATION_CHANNEL_SLICE_URI, NotificationChannelSlice.class);
        sUriToSlice.put(STORAGE_SLICE_URI, StorageSlice.class);
        sUriToSlice.put(WIFI_SLICE_URI, WifiSlice.class);
        sUriToSlice.put(DARK_THEME_SLICE_URI, DarkThemeSlice.class);
    }

    public static Class<? extends CustomSliceable> getSliceClassByUri(Uri uri) {
        return sUriToSlice.get(removeParameterFromUri(uri));
    }

    public static Uri removeParameterFromUri(Uri uri) {
        return uri != null ? uri.buildUpon().clearQuery().build() : null;
    }

    /**
     * Returns {@code true} if {@param uri} is a valid Slice Uri handled by
     * {@link CustomSliceRegistry}.
     */
    public static boolean isValidUri(Uri uri) {
        return sUriToSlice.containsKey(removeParameterFromUri(uri));
    }

    /**
     * Returns {@code true} if {@param action} is a valid intent action handled by
     * {@link CustomSliceRegistry}.
     */
    public static boolean isValidAction(String action) {
        return isValidUri(Uri.parse(action));
    }
}
