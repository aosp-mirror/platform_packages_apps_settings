/*
 * Copyright (C) 2014 The Android Open Source Project
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

package com.android.settings.search;

import android.provider.SearchIndexableResource;
import android.support.annotation.DrawableRes;
import android.support.annotation.VisibleForTesting;
import android.support.annotation.XmlRes;
import android.text.TextUtils;
import com.android.settings.DateTimeSettings;
import com.android.settings.DeviceInfoSettings;
import com.android.settings.DisplaySettings;
import com.android.settings.EncryptionAndCredential;
import com.android.settings.LegalSettings;
import com.android.settings.R;
import com.android.settings.ScreenPinningSettings;
import com.android.settings.SecuritySettings;
import com.android.settings.accessibility.AccessibilitySettings;
import com.android.settings.accessibility.AccessibilityShortcutPreferenceFragment;
import com.android.settings.accessibility.MagnificationPreferenceFragment;
import com.android.settings.accounts.UserAndAccountDashboardFragment;
import com.android.settings.applications.AppAndNotificationDashboardFragment;
import com.android.settings.applications.DefaultAppSettings;
import com.android.settings.applications.SpecialAccessSettings;
import com.android.settings.applications.assist.ManageAssist;
import com.android.settings.backup.BackupSettingsActivity;
import com.android.settings.backup.BackupSettingsFragment;
import com.android.settings.bluetooth.BluetoothSettings;
import com.android.settings.connecteddevice.ConnectedDeviceDashboardFragment;
import com.android.settings.datausage.DataUsageMeteredSettings;
import com.android.settings.datausage.DataUsageSummary;
import com.android.settings.deletionhelper.AutomaticStorageManagerSettings;
import com.android.settings.development.DevelopmentSettings;
import com.android.settings.deviceinfo.Status;
import com.android.settings.deviceinfo.StorageDashboardFragment;
import com.android.settings.deviceinfo.StorageSettings;
import com.android.settings.display.AmbientDisplaySettings;
import com.android.settings.display.ScreenZoomSettings;
import com.android.settings.dream.DreamSettings;
import com.android.settings.enterprise.EnterprisePrivacySettings;
import com.android.settings.fuelgauge.BatterySaverSettings;
import com.android.settings.fuelgauge.PowerUsageAdvanced;
import com.android.settings.fuelgauge.PowerUsageSummary;
import com.android.settings.gestures.AssistGestureSettings;
import com.android.settings.gestures.DoubleTapPowerSettings;
import com.android.settings.gestures.DoubleTapScreenSettings;
import com.android.settings.gestures.DoubleTwistGestureSettings;
import com.android.settings.gestures.GestureSettings;
import com.android.settings.gestures.PickupGestureSettings;
import com.android.settings.gestures.SwipeToNotificationSettings;
import com.android.settings.inputmethod.AvailableVirtualKeyboardFragment;
import com.android.settings.inputmethod.PhysicalKeyboardFragment;
import com.android.settings.inputmethod.VirtualKeyboardFragment;
import com.android.settings.language.LanguageAndInputSettings;
import com.android.settings.location.LocationSettings;
import com.android.settings.location.ScanningSettings;
import com.android.settings.network.NetworkDashboardFragment;
import com.android.settings.nfc.PaymentSettings;
import com.android.settings.notification.ChannelImportanceSettings;
import com.android.settings.notification.ConfigureNotificationSettings;
import com.android.settings.notification.SoundSettings;
import com.android.settings.notification.ZenModePrioritySettings;
import com.android.settings.notification.ZenModeSettings;
import com.android.settings.notification.ZenModeVisualInterruptionSettings;
import com.android.settings.print.PrintSettingsFragment;
import com.android.settings.security.LockscreenDashboardFragment;
import com.android.settings.sim.SimSettings;
import com.android.settings.support.SupportDashboardActivity;
import com.android.settings.system.ResetDashboardFragment;
import com.android.settings.system.SystemDashboardFragment;
import com.android.settings.tts.TtsEnginePreferenceFragment;
import com.android.settings.users.UserSettings;
import com.android.settings.wallpaper.WallpaperTypeSettings;
import com.android.settings.wifi.ConfigureWifiSettings;
import com.android.settings.wifi.SavedAccessPointsWifiSettings;
import com.android.settings.wifi.WifiSettings;
import java.util.Collection;
import java.util.HashMap;

public final class SearchIndexableResources {

    /**
     * Identifies subsettings which have an {@link SearchIndexableResource#intentAction} but
     * whose intents should still be treated as subsettings inside of Settings.
     */
    public static final String SUBSETTING_TARGET_PACKAGE = "subsetting_target_package";

    @XmlRes
    public static final int NO_DATA_RES_ID = 0;

    @VisibleForTesting
    static final HashMap<String, SearchIndexableResource> sResMap = new HashMap<>();

    @VisibleForTesting
    static void addIndex(Class<?> indexClass, @XmlRes int xmlResId,
            @DrawableRes int iconResId) {
        addIndex(indexClass, xmlResId, iconResId, null /* targetAction */);
    }

    @VisibleForTesting
    static void addIndex(Class<?> indexClass, @XmlRes int xmlResId,
            @DrawableRes int iconResId, String targetAction) {
        String className = indexClass.getName();
        SearchIndexableResource resource =
                new SearchIndexableResource(0, xmlResId, className, iconResId);

        if (!TextUtils.isEmpty(targetAction)) {
            resource.intentAction = targetAction;
            resource.intentTargetPackage = SUBSETTING_TARGET_PACKAGE;
        }

        sResMap.put(className, resource);
    }

    static {
        addIndex(WifiSettings.class, NO_DATA_RES_ID, R.drawable.ic_settings_wireless);
        addIndex(NetworkDashboardFragment.class, NO_DATA_RES_ID, R.drawable.ic_settings_wireless);
        addIndex(ConfigureWifiSettings.class, NO_DATA_RES_ID, R.drawable.ic_settings_wireless);
        addIndex(SavedAccessPointsWifiSettings.class, NO_DATA_RES_ID,
                R.drawable.ic_settings_wireless);
        addIndex(BluetoothSettings.class, NO_DATA_RES_ID, R.drawable.ic_settings_bluetooth);
        addIndex(SimSettings.class, NO_DATA_RES_ID, R.drawable.ic_sim_sd);
        addIndex(DataUsageSummary.class, NO_DATA_RES_ID, R.drawable.ic_settings_data_usage);
        addIndex(DataUsageMeteredSettings.class, NO_DATA_RES_ID, R.drawable.ic_settings_data_usage);
        addIndex(ScreenZoomSettings.class, NO_DATA_RES_ID, R.drawable.ic_settings_display);
        addIndex(DisplaySettings.class, NO_DATA_RES_ID, R.drawable.ic_settings_display,
                "android.settings.DISPLAY_SETTINGS");
        addIndex(AmbientDisplaySettings.class, NO_DATA_RES_ID, R.drawable.ic_settings_display);
        addIndex(WallpaperTypeSettings.class, NO_DATA_RES_ID, R.drawable.ic_settings_display);
        addIndex(ConfigureNotificationSettings.class,
                R.xml.configure_notification_settings, R.drawable.ic_settings_notifications);
        addIndex(AppAndNotificationDashboardFragment.class, NO_DATA_RES_ID,
                R.drawable.ic_settings_applications);
        addIndex(SoundSettings.class, NO_DATA_RES_ID, R.drawable.ic_settings_sound,
                "android.settings.SOUND_SETTINGS");
        addIndex(ZenModeSettings.class,
                R.xml.zen_mode_settings, R.drawable.ic_settings_notifications);
        addIndex(ZenModePrioritySettings.class,
                R.xml.zen_mode_priority_settings, R.drawable.ic_settings_notifications);
        addIndex(StorageSettings.class, NO_DATA_RES_ID, R.drawable.ic_settings_storage);
        addIndex(PowerUsageSummary.class,
                R.xml.power_usage_summary, R.drawable.ic_settings_battery);
        addIndex(PowerUsageAdvanced.class, NO_DATA_RES_ID, R.drawable.ic_settings_battery);
        addIndex(BatterySaverSettings.class,
                R.xml.battery_saver_settings, R.drawable.ic_settings_battery);
        addIndex(DefaultAppSettings.class, NO_DATA_RES_ID, R.drawable.ic_settings_applications);
        addIndex(ManageAssist.class, NO_DATA_RES_ID, R.drawable.ic_settings_applications);
        addIndex(SpecialAccessSettings.class, NO_DATA_RES_ID, R.drawable.ic_settings_applications);
        addIndex(UserSettings.class, NO_DATA_RES_ID, R.drawable.ic_settings_multiuser);
        addIndex(AssistGestureSettings.class, NO_DATA_RES_ID, R.drawable.ic_settings_gestures);
        addIndex(PickupGestureSettings.class, NO_DATA_RES_ID, R.drawable.ic_settings_gestures);
        addIndex(DoubleTapScreenSettings.class, NO_DATA_RES_ID, R.drawable.ic_settings_gestures);
        addIndex(DoubleTapPowerSettings.class, NO_DATA_RES_ID, R.drawable.ic_settings_gestures);
        addIndex(DoubleTwistGestureSettings.class, NO_DATA_RES_ID, R.drawable.ic_settings_gestures);
        addIndex(SwipeToNotificationSettings.class, NO_DATA_RES_ID,
                R.drawable.ic_settings_gestures);
        addIndex(GestureSettings.class, NO_DATA_RES_ID, R.drawable.ic_settings_gestures);
        addIndex(LanguageAndInputSettings.class, NO_DATA_RES_ID, R.drawable.ic_settings_language);
        addIndex(LocationSettings.class, R.xml.location_settings, R.drawable.ic_settings_location);
        addIndex(ScanningSettings.class, R.xml.location_scanning, R.drawable.ic_settings_location);
        addIndex(SecuritySettings.class, NO_DATA_RES_ID, R.drawable.ic_settings_security);
        addIndex(EncryptionAndCredential.class, NO_DATA_RES_ID, R.drawable.ic_settings_security);
        addIndex(ScreenPinningSettings.class, NO_DATA_RES_ID, R.drawable.ic_settings_security);
        addIndex(UserAndAccountDashboardFragment.class, NO_DATA_RES_ID,
                R.drawable.ic_settings_accounts);
        addIndex(VirtualKeyboardFragment.class, NO_DATA_RES_ID, R.drawable.ic_settings_language);
        addIndex(AvailableVirtualKeyboardFragment.class,
                NO_DATA_RES_ID, R.drawable.ic_settings_language);
        addIndex(PhysicalKeyboardFragment.class, NO_DATA_RES_ID, R.drawable.ic_settings_language);
        addIndex(BackupSettingsActivity.class, NO_DATA_RES_ID, R.drawable.ic_settings_backup);
        addIndex(BackupSettingsFragment.class, NO_DATA_RES_ID, R.drawable.ic_settings_backup);
        addIndex(DateTimeSettings.class, NO_DATA_RES_ID, R.drawable.ic_settings_date_time);
        addIndex(AccessibilitySettings.class, NO_DATA_RES_ID, R.drawable.ic_settings_accessibility);
        addIndex(PrintSettingsFragment.class, NO_DATA_RES_ID, R.drawable.ic_settings_print);
        addIndex(DevelopmentSettings.class, NO_DATA_RES_ID, R.drawable.ic_settings_development);
        addIndex(DeviceInfoSettings.class, NO_DATA_RES_ID, R.drawable.ic_settings_about);
        addIndex(Status.class, NO_DATA_RES_ID, 0 /* icon */);
        addIndex(LegalSettings.class, NO_DATA_RES_ID, R.drawable.ic_settings_about);
        addIndex(ZenModeVisualInterruptionSettings.class,
                R.xml.zen_mode_visual_interruptions_settings,
                R.drawable.ic_settings_notifications);
        addIndex(SystemDashboardFragment.class, NO_DATA_RES_ID, R.drawable.ic_settings_about);
        addIndex(ResetDashboardFragment.class, NO_DATA_RES_ID, R.drawable.ic_restore);
        addIndex(StorageDashboardFragment.class, NO_DATA_RES_ID, R.drawable.ic_settings_storage);
        addIndex(ConnectedDeviceDashboardFragment.class, NO_DATA_RES_ID,
                R.drawable.ic_devices_other);
        addIndex(EnterprisePrivacySettings.class, NO_DATA_RES_ID, R.drawable.ic_settings_about);
        addIndex(PaymentSettings.class, NO_DATA_RES_ID, R.drawable.ic_settings_nfc_payment);
        addIndex(
                TtsEnginePreferenceFragment.class, NO_DATA_RES_ID, R.drawable.ic_settings_language);
        addIndex(LockscreenDashboardFragment.class, R.xml.security_lockscreen_settings,
                R.drawable.ic_settings_security);
        addIndex(MagnificationPreferenceFragment.class, NO_DATA_RES_ID,
                R.drawable.ic_settings_accessibility);
        addIndex(AccessibilityShortcutPreferenceFragment.class, NO_DATA_RES_ID,
                R.drawable.ic_settings_accessibility);
        addIndex(ChannelImportanceSettings.class, NO_DATA_RES_ID,
                R.drawable.ic_settings_notifications);
        addIndex(DreamSettings.class, NO_DATA_RES_ID, R.drawable.ic_settings_display);
        addIndex(SupportDashboardActivity.class, NO_DATA_RES_ID, R.drawable.ic_help);
        addIndex(
                AutomaticStorageManagerSettings.class,
                NO_DATA_RES_ID,
                R.drawable.ic_settings_storage);
    }

    private SearchIndexableResources() {
    }

    public static int size() {
        return sResMap.size();
    }

    public static SearchIndexableResource getResourceByName(String className) {
        return sResMap.get(className);
    }

    public static Collection<SearchIndexableResource> values() {
        return sResMap.values();
    }
}
