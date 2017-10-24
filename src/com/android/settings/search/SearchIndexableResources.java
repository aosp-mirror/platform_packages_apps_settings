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
import com.android.settings.development.DevelopmentSettingsDashboardFragment;
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
import com.android.settings.notification.ZenModeBehaviorSettings;
import com.android.settings.notification.ZenModeSettings;
import com.android.settings.notification.ZenModeVisualInterruptionSettings;
import com.android.settings.print.PrintSettingsFragment;
import com.android.settings.security.LockscreenDashboardFragment;
import com.android.settings.sim.SimSettings;
import com.android.settings.support.SupportDashboardActivity;
import com.android.settings.system.ResetDashboardFragment;
import com.android.settings.system.SystemDashboardFragment;
import com.android.settings.tts.TextToSpeechSettings;
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
    public static final int NO_RES_ID = 0;

    @VisibleForTesting
    static final HashMap<String, SearchIndexableResource> sResMap = new HashMap<>();

    @VisibleForTesting
    static void addIndex(Class<?> indexClass, @XmlRes int xmlResId) {
        addIndex(indexClass, xmlResId, null /* targetAction */);
    }

    @VisibleForTesting
    static void addIndex(Class<?> indexClass, @XmlRes int xmlResId, String targetAction) {
        String className = indexClass.getName();
        SearchIndexableResource resource = new SearchIndexableResource(0, xmlResId, className,
                NO_RES_ID);

        if (!TextUtils.isEmpty(targetAction)) {
            resource.intentAction = targetAction;
            resource.intentTargetPackage = SUBSETTING_TARGET_PACKAGE;
        }

        sResMap.put(className, resource);
    }

    static {
        addIndex(WifiSettings.class, NO_RES_ID);
        addIndex(NetworkDashboardFragment.class, NO_RES_ID);
        addIndex(ConfigureWifiSettings.class, NO_RES_ID);
        addIndex(SavedAccessPointsWifiSettings.class, NO_RES_ID);
        addIndex(BluetoothSettings.class, NO_RES_ID);
        addIndex(SimSettings.class, NO_RES_ID);
        addIndex(DataUsageSummary.class, NO_RES_ID);
        addIndex(DataUsageMeteredSettings.class, NO_RES_ID);
        addIndex(ScreenZoomSettings.class, NO_RES_ID);
        addIndex(DisplaySettings.class, NO_RES_ID, "android.settings.DISPLAY_SETTINGS");
        addIndex(AmbientDisplaySettings.class, NO_RES_ID);
        addIndex(WallpaperTypeSettings.class, NO_RES_ID);
        addIndex(AppAndNotificationDashboardFragment.class, NO_RES_ID);
        addIndex(SoundSettings.class, NO_RES_ID, "android.settings.SOUND_SETTINGS");
        addIndex(ZenModeSettings.class, R.xml.zen_mode_settings);
        addIndex(StorageSettings.class, NO_RES_ID);
        addIndex(PowerUsageAdvanced.class, NO_RES_ID);
        addIndex(DefaultAppSettings.class, NO_RES_ID);
        addIndex(ManageAssist.class, NO_RES_ID);
        addIndex(SpecialAccessSettings.class, NO_RES_ID);
        addIndex(UserSettings.class, NO_RES_ID);
        addIndex(AssistGestureSettings.class, NO_RES_ID);
        addIndex(PickupGestureSettings.class, NO_RES_ID);
        addIndex(DoubleTapScreenSettings.class, NO_RES_ID);
        addIndex(DoubleTapPowerSettings.class, NO_RES_ID);
        addIndex(DoubleTwistGestureSettings.class, NO_RES_ID);
        addIndex(SwipeToNotificationSettings.class, NO_RES_ID);
        addIndex(GestureSettings.class, NO_RES_ID);
        addIndex(LanguageAndInputSettings.class, NO_RES_ID);
        addIndex(LocationSettings.class, R.xml.location_settings);
        addIndex(ScanningSettings.class, R.xml.location_scanning);
        addIndex(SecuritySettings.class, NO_RES_ID);
        addIndex(EncryptionAndCredential.class, NO_RES_ID);
        addIndex(ScreenPinningSettings.class, NO_RES_ID);
        addIndex(UserAndAccountDashboardFragment.class, NO_RES_ID);
        addIndex(VirtualKeyboardFragment.class, NO_RES_ID);
        addIndex(AvailableVirtualKeyboardFragment.class, NO_RES_ID);
        addIndex(PhysicalKeyboardFragment.class, NO_RES_ID);
        addIndex(BackupSettingsActivity.class, NO_RES_ID);
        addIndex(BackupSettingsFragment.class, NO_RES_ID);
        addIndex(DateTimeSettings.class, NO_RES_ID);
        addIndex(AccessibilitySettings.class, NO_RES_ID);
        addIndex(PrintSettingsFragment.class, NO_RES_ID);
        addIndex(DevelopmentSettingsDashboardFragment.class, NO_RES_ID);
        addIndex(DeviceInfoSettings.class, NO_RES_ID);
        addIndex(Status.class, NO_RES_ID);
        addIndex(LegalSettings.class, NO_RES_ID);
        addIndex(SystemDashboardFragment.class, NO_RES_ID);
        addIndex(ResetDashboardFragment.class, NO_RES_ID);
        addIndex(StorageDashboardFragment.class, NO_RES_ID);
        addIndex(ConnectedDeviceDashboardFragment.class, NO_RES_ID);
        addIndex(EnterprisePrivacySettings.class, NO_RES_ID);
        addIndex(PaymentSettings.class, NO_RES_ID);
        addIndex(TextToSpeechSettings.class, NO_RES_ID);
        addIndex(TtsEnginePreferenceFragment.class, NO_RES_ID);
        addIndex(MagnificationPreferenceFragment.class, NO_RES_ID);
        addIndex(AccessibilityShortcutPreferenceFragment.class, NO_RES_ID);
        addIndex(ChannelImportanceSettings.class, NO_RES_ID);
        addIndex(DreamSettings.class, NO_RES_ID);
        addIndex(SupportDashboardActivity.class, NO_RES_ID);
        addIndex(AutomaticStorageManagerSettings.class, NO_RES_ID);
        addIndex(ConfigureNotificationSettings.class, R.xml.configure_notification_settings);
        addIndex(ZenModeBehaviorSettings.class, R.xml.zen_mode_behavior_settings);
        addIndex(PowerUsageSummary.class, R.xml.power_usage_summary);
        addIndex(BatterySaverSettings.class, R.xml.battery_saver_settings);
        addIndex(LockscreenDashboardFragment.class, R.xml.security_lockscreen_settings);
        addIndex(ZenModeVisualInterruptionSettings.class,
                R.xml.zen_mode_visual_interruptions_settings);
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
