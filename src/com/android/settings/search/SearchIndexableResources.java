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

import android.support.annotation.VisibleForTesting;

import com.android.settings.DateTimeSettings;
import com.android.settings.DeviceInfoSettings;
import com.android.settings.DisplaySettings;
import com.android.settings.LegalSettings;
import com.android.settings.ScreenPinningSettings;
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
import com.android.settings.notification.ConfigureNotificationSettings;
import com.android.settings.notification.SoundSettings;
import com.android.settings.notification.ZenModeAutomationSettings;
import com.android.settings.notification.ZenModeBehaviorSettings;
import com.android.settings.notification.ZenModeSettings;
import com.android.settings.print.PrintSettingsFragment;
import com.android.settings.security.EncryptionAndCredential;
import com.android.settings.security.LockscreenDashboardFragment;
import com.android.settings.security.SecuritySettings;
import com.android.settings.security.screenlock.ScreenLockSettings;
import com.android.settings.sim.SimSettings;
import com.android.settings.support.SupportDashboardActivity;
import com.android.settings.system.ResetDashboardFragment;
import com.android.settings.system.SystemDashboardFragment;
import com.android.settings.tts.TextToSpeechSettings;
import com.android.settings.tts.TtsEnginePreferenceFragment;
import com.android.settings.users.UserSettings;
import com.android.settings.wallpaper.WallpaperTypeSettings;
import com.android.settings.wifi.ConfigureWifiSettings;
import com.android.settings.wifi.WifiSettings;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

public final class SearchIndexableResources {

    @VisibleForTesting
    static final Set<Class> sProviders = new HashSet<>();

    @VisibleForTesting
    static void addIndex(Class indexClass) {
        sProviders.add(indexClass);
    }

    static {
        addIndex(WifiSettings.class);
        addIndex(NetworkDashboardFragment.class);
        addIndex(ConfigureWifiSettings.class);
        addIndex(BluetoothSettings.class);
        addIndex(SimSettings.class);
        addIndex(DataUsageSummary.class);
        addIndex(DataUsageMeteredSettings.class);
        addIndex(ScreenZoomSettings.class);
        addIndex(DisplaySettings.class);
        addIndex(AmbientDisplaySettings.class);
        addIndex(WallpaperTypeSettings.class);
        addIndex(AppAndNotificationDashboardFragment.class);
        addIndex(SoundSettings.class);
        addIndex(ZenModeSettings.class);
        addIndex(StorageSettings.class);
        addIndex(PowerUsageAdvanced.class);
        addIndex(DefaultAppSettings.class);
        addIndex(ManageAssist.class);
        addIndex(SpecialAccessSettings.class);
        addIndex(UserSettings.class);
        addIndex(AssistGestureSettings.class);
        addIndex(PickupGestureSettings.class);
        addIndex(DoubleTapScreenSettings.class);
        addIndex(DoubleTapPowerSettings.class);
        addIndex(DoubleTwistGestureSettings.class);
        addIndex(SwipeToNotificationSettings.class);
        addIndex(GestureSettings.class);
        addIndex(LanguageAndInputSettings.class);
        addIndex(LocationSettings.class);
        addIndex(ScanningSettings.class);
        addIndex(SecuritySettings.class);
        addIndex(ScreenLockSettings.class);
        addIndex(EncryptionAndCredential.class);
        addIndex(ScreenPinningSettings.class);
        addIndex(UserAndAccountDashboardFragment.class);
        addIndex(VirtualKeyboardFragment.class);
        addIndex(AvailableVirtualKeyboardFragment.class);
        addIndex(PhysicalKeyboardFragment.class);
        addIndex(BackupSettingsActivity.class);
        addIndex(BackupSettingsFragment.class);
        addIndex(DateTimeSettings.class);
        addIndex(AccessibilitySettings.class);
        addIndex(PrintSettingsFragment.class);
        addIndex(DevelopmentSettingsDashboardFragment.class);
        addIndex(DeviceInfoSettings.class);
        addIndex(Status.class);
        addIndex(LegalSettings.class);
        addIndex(SystemDashboardFragment.class);
        addIndex(ResetDashboardFragment.class);
        addIndex(StorageDashboardFragment.class);
        addIndex(ConnectedDeviceDashboardFragment.class);
        addIndex(EnterprisePrivacySettings.class);
        addIndex(PaymentSettings.class);
        addIndex(TextToSpeechSettings.class);
        addIndex(TtsEnginePreferenceFragment.class);
        addIndex(MagnificationPreferenceFragment.class);
        addIndex(AccessibilityShortcutPreferenceFragment.class);
        addIndex(DreamSettings.class);
        addIndex(SupportDashboardActivity.class);
        addIndex(AutomaticStorageManagerSettings.class);
        addIndex(ConfigureNotificationSettings.class);
        addIndex(PowerUsageSummary.class);
        addIndex(BatterySaverSettings.class);
        addIndex(LockscreenDashboardFragment.class);
        addIndex(ZenModeBehaviorSettings.class);
        addIndex(ZenModeAutomationSettings.class);
    }

    private SearchIndexableResources() {
    }

    public static Collection<Class> providerValues() { return sProviders;}
}