/*
 * Copyright (C) 2016 The Android Open Source Project
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

package com.android.settings.core.gateway;

import com.android.settings.DateTimeSettings;
import com.android.settings.DeviceAdminSettings;
import com.android.settings.DisplaySettings;
import com.android.settings.IccLockSettings;
import com.android.settings.MasterClear;
import com.android.settings.PrivacySettings;
import com.android.settings.Settings;
import com.android.settings.TestingSettings;
import com.android.settings.TetherSettings;
import com.android.settings.TrustedCredentialsSettings;
import com.android.settings.accessibility.AccessibilitySettings;
import com.android.settings.accessibility.AccessibilitySettingsForSetupWizard;
import com.android.settings.accessibility.CaptionPropertiesFragment;
import com.android.settings.accessibility.ToggleDaltonizerPreferenceFragment;
import com.android.settings.accounts.AccountDashboardFragment;
import com.android.settings.accounts.AccountSyncSettings;
import com.android.settings.accounts.ChooseAccountActivity;
import com.android.settings.accounts.ManagedProfileSettings;
import com.android.settings.applications.AppAndNotificationDashboardFragment;
import com.android.settings.applications.DefaultAppSettings;
import com.android.settings.applications.DirectoryAccessDetails;
import com.android.settings.applications.ManageDomainUrls;
import com.android.settings.applications.ProcessStatsSummary;
import com.android.settings.applications.ProcessStatsUi;
import com.android.settings.applications.UsageAccessDetails;
import com.android.settings.applications.VrListenerSettings;
import com.android.settings.applications.appinfo.AppInfoDashboardFragment;
import com.android.settings.applications.appinfo.DrawOverlayDetails;
import com.android.settings.applications.appinfo.ExternalSourcesDetails;
import com.android.settings.applications.appinfo.PictureInPictureDetails;
import com.android.settings.applications.appinfo.PictureInPictureSettings;
import com.android.settings.applications.appinfo.WriteSettingsDetails;
import com.android.settings.applications.appops.BackgroundCheckSummary;
import com.android.settings.applications.assist.ManageAssist;
import com.android.settings.applications.manageapplications.ManageApplications;
import com.android.settings.backup.ToggleBackupSettingFragment;
import com.android.settings.bluetooth.BluetoothDeviceDetailsFragment;
import com.android.settings.connecteddevice.AdvancedConnectedDeviceDashboardFragment;
import com.android.settings.connecteddevice.ConnectedDeviceDashboardFragment;
import com.android.settings.connecteddevice.PreviouslyConnectedDeviceDashboardFragment;
import com.android.settings.connecteddevice.usb.UsbDetailsFragment;
import com.android.settings.datausage.DataUsageList;
import com.android.settings.datausage.DataUsageSummary;
import com.android.settings.datausage.DataUsageSummaryLegacy;
import com.android.settings.deletionhelper.AutomaticStorageManagerSettings;
import com.android.settings.development.DevelopmentSettingsDashboardFragment;
import com.android.settings.deviceinfo.DeviceInfoSettings;
import com.android.settings.deviceinfo.PrivateVolumeForget;
import com.android.settings.deviceinfo.PrivateVolumeSettings;
import com.android.settings.deviceinfo.PublicVolumeSettings;
import com.android.settings.deviceinfo.StorageDashboardFragment;
import com.android.settings.deviceinfo.StorageSettings;
import com.android.settings.deviceinfo.aboutphone.MyDeviceInfoFragment;
import com.android.settings.display.NightDisplaySettings;
import com.android.settings.dream.DreamSettings;
import com.android.settings.enterprise.EnterprisePrivacySettings;
import com.android.settings.fuelgauge.AdvancedPowerUsageDetail;
import com.android.settings.fuelgauge.PowerUsageSummary;
import com.android.settings.fuelgauge.batterysaver.BatterySaverSettings;
import com.android.settings.gestures.AssistGestureSettings;
import com.android.settings.gestures.DoubleTapPowerSettings;
import com.android.settings.gestures.DoubleTapScreenSettings;
import com.android.settings.gestures.DoubleTwistGestureSettings;
import com.android.settings.gestures.SwipeUpGestureSettings;
import com.android.settings.gestures.PickupGestureSettings;
import com.android.settings.gestures.SwipeToNotificationSettings;
import com.android.settings.inputmethod.AvailableVirtualKeyboardFragment;
import com.android.settings.inputmethod.KeyboardLayoutPickerFragment;
import com.android.settings.inputmethod.PhysicalKeyboardFragment;
import com.android.settings.inputmethod.SpellCheckersSettings;
import com.android.settings.inputmethod.UserDictionaryList;
import com.android.settings.inputmethod.UserDictionarySettings;
import com.android.settings.language.LanguageAndInputSettings;
import com.android.settings.localepicker.LocaleListEditor;
import com.android.settings.location.LocationSettings;
import com.android.settings.location.ScanningSettings;
import com.android.settings.network.ApnEditor;
import com.android.settings.network.ApnSettings;
import com.android.settings.network.NetworkDashboardFragment;
import com.android.settings.nfc.AndroidBeam;
import com.android.settings.nfc.PaymentSettings;
import com.android.settings.notification.AppNotificationSettings;
import com.android.settings.notification.ChannelGroupNotificationSettings;
import com.android.settings.notification.ChannelNotificationSettings;
import com.android.settings.notification.ConfigureNotificationSettings;
import com.android.settings.notification.NotificationAccessSettings;
import com.android.settings.notification.NotificationStation;
import com.android.settings.notification.SoundSettings;
import com.android.settings.notification.ZenAccessSettings;
import com.android.settings.notification.ZenModeAutomationSettings;
import com.android.settings.notification.ZenModeMsgEventReminderSettings;
import com.android.settings.notification.ZenModeBlockedEffectsSettings;
import com.android.settings.notification.ZenModeEventRuleSettings;
import com.android.settings.notification.ZenModeRestrictNotificationsSettings;
import com.android.settings.notification.ZenModeScheduleRuleSettings;
import com.android.settings.notification.ZenModeSettings;
import com.android.settings.password.ChooseLockPassword;
import com.android.settings.password.ChooseLockPattern;
import com.android.settings.print.PrintJobSettingsFragment;
import com.android.settings.print.PrintSettingsFragment;
import com.android.settings.security.CryptKeeperSettings;
import com.android.settings.security.LockscreenDashboardFragment;
import com.android.settings.security.SecuritySettings;
import com.android.settings.sim.SimSettings;
import com.android.settings.support.SupportDashboardActivity;
import com.android.settings.system.ResetDashboardFragment;
import com.android.settings.system.SystemDashboardFragment;
import com.android.settings.tts.TextToSpeechSettings;
import com.android.settings.users.UserSettings;
import com.android.settings.vpn2.VpnSettings;
import com.android.settings.wallpaper.WallpaperTypeSettings;
import com.android.settings.webview.WebViewAppPicker;
import com.android.settings.wfd.WifiDisplaySettings;
import com.android.settings.wifi.ConfigureWifiSettings;
import com.android.settings.wifi.SavedAccessPointsWifiSettings;
import com.android.settings.wifi.WifiAPITest;
import com.android.settings.wifi.WifiInfo;
import com.android.settings.wifi.WifiSettings;
import com.android.settings.wifi.calling.WifiCallingSettings;
import com.android.settings.wifi.p2p.WifiP2pSettings;
import com.android.settings.wifi.tether.WifiTetherSettings;

public class SettingsGateway {

    /**
     * A list of fragment that can be hosted by SettingsActivity. SettingsActivity will throw a
     * security exception if the fragment it needs to display is not in this list.
     */
    public static final String[] ENTRY_FRAGMENTS = {
            AdvancedConnectedDeviceDashboardFragment.class.getName(),
            WifiSettings.class.getName(),
            ConfigureWifiSettings.class.getName(),
            SavedAccessPointsWifiSettings.class.getName(),
            SimSettings.class.getName(),
            TetherSettings.class.getName(),
            WifiP2pSettings.class.getName(),
            WifiTetherSettings.class.getName(),
            BackgroundCheckSummary.class.getName(),
            VpnSettings.class.getName(),
            DateTimeSettings.class.getName(),
            LocaleListEditor.class.getName(),
            AvailableVirtualKeyboardFragment.class.getName(),
            LanguageAndInputSettings.class.getName(),
            SpellCheckersSettings.class.getName(),
            UserDictionaryList.class.getName(),
            UserDictionarySettings.class.getName(),
            DisplaySettings.class.getName(),
            DeviceInfoSettings.class.getName(),
            MyDeviceInfoFragment.class.getName(),
            ManageApplications.class.getName(),
            ManageAssist.class.getName(),
            ProcessStatsUi.class.getName(),
            NotificationStation.class.getName(),
            LocationSettings.class.getName(),
            ScanningSettings.class.getName(),
            SecuritySettings.class.getName(),
            UsageAccessDetails.class.getName(),
            PrivacySettings.class.getName(),
            DeviceAdminSettings.class.getName(),
            AccessibilitySettings.class.getName(),
            AccessibilitySettingsForSetupWizard.class.getName(),
            CaptionPropertiesFragment.class.getName(),
            ToggleDaltonizerPreferenceFragment.class.getName(),
            TextToSpeechSettings.class.getName(),
            StorageSettings.class.getName(),
            PrivateVolumeForget.class.getName(),
            PrivateVolumeSettings.class.getName(),
            PublicVolumeSettings.class.getName(),
            DevelopmentSettingsDashboardFragment.class.getName(),
            AndroidBeam.class.getName(),
            WifiDisplaySettings.class.getName(),
            PowerUsageSummary.class.getName(),
            AccountSyncSettings.class.getName(),
            AssistGestureSettings.class.getName(),
            SwipeToNotificationSettings.class.getName(),
            DoubleTapPowerSettings.class.getName(),
            DoubleTapScreenSettings.class.getName(),
            PickupGestureSettings.class.getName(),
            DoubleTwistGestureSettings.class.getName(),
            SwipeUpGestureSettings.class.getName(),
            CryptKeeperSettings.class.getName(),
            DataUsageSummary.class.getName(),
            DataUsageSummaryLegacy.class.getName(),
            DreamSettings.class.getName(),
            UserSettings.class.getName(),
            NotificationAccessSettings.class.getName(),
            ZenAccessSettings.class.getName(),
            ZenModeAutomationSettings.class.getName(),
            PrintSettingsFragment.class.getName(),
            PrintJobSettingsFragment.class.getName(),
            TrustedCredentialsSettings.class.getName(),
            PaymentSettings.class.getName(),
            KeyboardLayoutPickerFragment.class.getName(),
            PhysicalKeyboardFragment.class.getName(),
            ZenModeSettings.class.getName(),
            SoundSettings.class.getName(),
            ConfigureNotificationSettings.class.getName(),
            ChooseLockPassword.ChooseLockPasswordFragment.class.getName(),
            ChooseLockPattern.ChooseLockPatternFragment.class.getName(),
            AppInfoDashboardFragment.class.getName(),
            BatterySaverSettings.class.getName(),
            AppNotificationSettings.class.getName(),
            ChannelNotificationSettings.class.getName(),
            ChannelGroupNotificationSettings.class.getName(),
            ApnSettings.class.getName(),
            ApnEditor.class.getName(),
            WifiCallingSettings.class.getName(),
            ZenModeScheduleRuleSettings.class.getName(),
            ZenModeEventRuleSettings.class.getName(),
            ZenModeBlockedEffectsSettings.class.getName(),
            ProcessStatsUi.class.getName(),
            AdvancedPowerUsageDetail.class.getName(),
            ProcessStatsSummary.class.getName(),
            DrawOverlayDetails.class.getName(),
            WriteSettingsDetails.class.getName(),
            ExternalSourcesDetails.class.getName(),
            DefaultAppSettings.class.getName(),
            WallpaperTypeSettings.class.getName(),
            VrListenerSettings.class.getName(),
            PictureInPictureSettings.class.getName(),
            PictureInPictureDetails.class.getName(),
            ManagedProfileSettings.class.getName(),
            ChooseAccountActivity.class.getName(),
            IccLockSettings.class.getName(),
            TestingSettings.class.getName(),
            WifiAPITest.class.getName(),
            WifiInfo.class.getName(),
            MasterClear.class.getName(),
            ResetDashboardFragment.class.getName(),
            NightDisplaySettings.class.getName(),
            ManageDomainUrls.class.getName(),
            AutomaticStorageManagerSettings.class.getName(),
            StorageDashboardFragment.class.getName(),
            SystemDashboardFragment.class.getName(),
            NetworkDashboardFragment.class.getName(),
            ConnectedDeviceDashboardFragment.class.getName(),
            UsbDetailsFragment.class.getName(),
            AppAndNotificationDashboardFragment.class.getName(),
            AccountDashboardFragment.class.getName(),
            EnterprisePrivacySettings.class.getName(),
            WebViewAppPicker.class.getName(),
            LockscreenDashboardFragment.class.getName(),
            BluetoothDeviceDetailsFragment.class.getName(),
            DataUsageList.class.getName(),
            DirectoryAccessDetails.class.getName(),
            ToggleBackupSettingFragment.class.getName(),
            PreviouslyConnectedDeviceDashboardFragment.class.getName(),
    };

    public static final String[] SETTINGS_FOR_RESTRICTED = {
            // Home page
            Settings.NetworkDashboardActivity.class.getName(),
            Settings.ConnectedDeviceDashboardActivity.class.getName(),
            Settings.AppAndNotificationDashboardActivity.class.getName(),
            Settings.DisplaySettingsActivity.class.getName(),
            Settings.SoundSettingsActivity.class.getName(),
            Settings.StorageDashboardActivity.class.getName(),
            Settings.PowerUsageSummaryActivity.class.getName(),
            Settings.AccountDashboardActivity.class.getName(),
            Settings.SecurityDashboardActivity.class.getName(),
            Settings.AccessibilitySettingsActivity.class.getName(),
            Settings.SystemDashboardActivity.class.getName(),
            SupportDashboardActivity.class.getName(),
            // Home page > Network & Internet
            Settings.WifiSettingsActivity.class.getName(),
            Settings.DataUsageSummaryActivity.class.getName(),
            Settings.SimSettingsActivity.class.getName(),
            // Home page > Connected devices
            Settings.BluetoothSettingsActivity.class.getName(),
            Settings.WifiDisplaySettingsActivity.class.getName(),
            Settings.PrintSettingsActivity.class.getName(),
            // Home page > Apps & Notifications
            Settings.UserSettingsActivity.class.getName(),
            Settings.ConfigureNotificationSettingsActivity.class.getName(),
            Settings.AdvancedAppsActivity.class.getName(),
            Settings.ManageApplicationsActivity.class.getName(),
            Settings.PaymentSettingsActivity.class.getName(),
            // Home page > Security & screen lock
            Settings.LocationSettingsActivity.class.getName(),
            // Home page > System
            Settings.LanguageAndInputSettingsActivity.class.getName(),
            Settings.DateTimeSettingsActivity.class.getName(),
            Settings.DeviceInfoSettingsActivity.class.getName(),
            Settings.EnterprisePrivacySettingsActivity.class.getName(),
            Settings.MyDeviceInfoActivity.class.getName(),
    };
}
