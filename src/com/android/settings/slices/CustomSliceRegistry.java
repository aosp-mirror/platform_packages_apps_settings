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

import com.android.settings.display.AlwaysOnDisplaySlice;
import com.android.settings.display.ScreenTimeoutPreferenceController;
import com.android.settings.flashlight.FlashlightSlice;
import com.android.settings.fuelgauge.batterytip.BatteryTipPreferenceController;
import com.android.settings.homepage.contextualcards.slices.BatteryFixSlice;
import com.android.settings.homepage.contextualcards.slices.BluetoothDevicesSlice;
import com.android.settings.homepage.contextualcards.slices.ContextualAdaptiveSleepSlice;
import com.android.settings.homepage.contextualcards.slices.DarkThemeSlice;
import com.android.settings.homepage.contextualcards.slices.FaceSetupSlice;
import com.android.settings.homepage.contextualcards.slices.LowStorageSlice;
import com.android.settings.location.LocationSlice;
import com.android.settings.media.MediaOutputIndicatorSlice;
import com.android.settings.media.RemoteMediaSlice;
import com.android.settings.network.ProviderModelSlice;
import com.android.settings.network.telephony.MobileDataSlice;
import com.android.settings.notification.zen.ZenModeButtonPreferenceController;
import com.android.settings.wifi.calling.WifiCallingSliceHelper;
import com.android.settings.wifi.slice.ContextualWifiSlice;
import com.android.settings.wifi.slice.WifiSlice;
import com.android.settingslib.media.MediaOutputConstants;

import java.util.Map;

/**
 * A registry of custom slice Uris.
 */
public class CustomSliceRegistry {

    /**
     * Uri for Contextual Adaptive Sleep Slice
     */
    public static final Uri CONTEXTUAL_ADAPTIVE_SLEEP_URI = new Uri.Builder()
            .scheme(ContentResolver.SCHEME_CONTENT)
            .authority(SettingsSliceProvider.SLICE_AUTHORITY)
            .appendPath(SettingsSlicesContract.PATH_SETTING_INTENT)
            .appendPath(ScreenTimeoutPreferenceController.PREF_NAME)
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
     * Backing Uri for the Wifi Slice.
     */
    public static final Uri CONTEXTUAL_WIFI_SLICE_URI = new Uri.Builder()
            .scheme(ContentResolver.SCHEME_CONTENT)
            .authority(SettingsSlicesContract.AUTHORITY)
            .appendPath(SettingsSlicesContract.PATH_SETTING_ACTION)
            .appendPath("contextual_wifi")
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
     * Full {@link Uri} for the Provider Model Slice.
     */
    public static final Uri PROVIDER_MODEL_SLICE_URI = new Uri.Builder()
            .scheme(ContentResolver.SCHEME_CONTENT)
            .authority(SettingsSliceProvider.SLICE_AUTHORITY)
            .appendEncodedPath(SettingsSlicesContract.PATH_SETTING_ACTION)
            .appendPath("provider_model")
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
     * Full {@link Uri} for the Notification Volume Slice.
     */
    public static final Uri VOLUME_NOTIFICATION_URI = new Uri.Builder()
            .scheme(ContentResolver.SCHEME_CONTENT)
            .authority(SettingsSliceProvider.SLICE_AUTHORITY)
            .appendPath(SettingsSlicesContract.PATH_SETTING_ACTION)
            .appendPath("notification_volume")
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
     * Full {@link Uri} for the Ringer volume Slice.
     */
    public static final Uri VOLUME_RINGER_URI = new Uri.Builder()
            .scheme(ContentResolver.SCHEME_CONTENT)
            .authority(SettingsSliceProvider.SLICE_AUTHORITY)
            .appendPath(SettingsSlicesContract.PATH_SETTING_ACTION)
            .appendPath("ring_volume")
            .build();

    /**
     * Full {@link Uri} for the all volume Slices.
     */
    public static final Uri VOLUME_SLICES_URI = new Uri.Builder()
            .scheme(ContentResolver.SCHEME_CONTENT)
            .authority(SettingsSliceProvider.SLICE_AUTHORITY)
            .appendPath(SettingsSlicesContract.PATH_SETTING_ACTION)
            .appendPath("volume_slices")
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

    /**
     * Backing Uri for the Remote Media Slice.
     */
    public static Uri REMOTE_MEDIA_SLICE_URI = new Uri.Builder()
            .scheme(ContentResolver.SCHEME_CONTENT)
            .authority(SettingsSliceProvider.SLICE_AUTHORITY)
            .appendPath(SettingsSlicesContract.PATH_SETTING_ACTION)
            .appendPath(MediaOutputConstants.KEY_REMOTE_MEDIA)
            .build();

    /**
     * Backing Uri for the Always On Slice.
     */
    public static final Uri ALWAYS_ON_SLICE_URI = new Uri.Builder()
            .scheme(ContentResolver.SCHEME_CONTENT)
            .authority(SettingsSliceProvider.SLICE_AUTHORITY)
            .appendPath(SettingsSlicesContract.PATH_SETTING_ACTION)
            .appendPath("always_on_display")
            .build();

    /**
     * Backing Uri for the Turn on Wi-Fi Slice.
     */
    public static final Uri TURN_ON_WIFI_SLICE_URI = new Uri.Builder()
            .scheme(ContentResolver.SCHEME_CONTENT)
            .authority(SettingsSliceProvider.SLICE_AUTHORITY)
            .appendPath(SettingsSlicesContract.PATH_SETTING_ACTION)
            .appendPath("turn_on_wifi")
            .build();

    @VisibleForTesting
    static final Map<Uri, Class<? extends CustomSliceable>> sUriToSlice;

    static {
        sUriToSlice = new ArrayMap<>();

        sUriToSlice.put(BATTERY_FIX_SLICE_URI, BatteryFixSlice.class);
        sUriToSlice.put(BLUETOOTH_DEVICES_SLICE_URI, BluetoothDevicesSlice.class);
        sUriToSlice.put(CONTEXTUAL_ADAPTIVE_SLEEP_URI, ContextualAdaptiveSleepSlice.class);
        sUriToSlice.put(CONTEXTUAL_WIFI_SLICE_URI, ContextualWifiSlice.class);
        sUriToSlice.put(FACE_ENROLL_SLICE_URI, FaceSetupSlice.class);
        sUriToSlice.put(FLASHLIGHT_SLICE_URI, FlashlightSlice.class);
        sUriToSlice.put(LOCATION_SLICE_URI, LocationSlice.class);
        sUriToSlice.put(LOW_STORAGE_SLICE_URI, LowStorageSlice.class);
        sUriToSlice.put(MEDIA_OUTPUT_INDICATOR_SLICE_URI, MediaOutputIndicatorSlice.class);
        sUriToSlice.put(MOBILE_DATA_SLICE_URI, MobileDataSlice.class);
        sUriToSlice.put(PROVIDER_MODEL_SLICE_URI, ProviderModelSlice.class);
        sUriToSlice.put(WIFI_SLICE_URI, WifiSlice.class);
        sUriToSlice.put(DARK_THEME_SLICE_URI, DarkThemeSlice.class);
        sUriToSlice.put(REMOTE_MEDIA_SLICE_URI, RemoteMediaSlice.class);
        sUriToSlice.put(ALWAYS_ON_SLICE_URI, AlwaysOnDisplaySlice.class);
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
