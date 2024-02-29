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

import com.android.settings.DisplaySettings;
import com.android.settings.IccLockSettings;
import com.android.settings.MainClear;
import com.android.settings.MainClearConfirm;
import com.android.settings.ResetNetwork;
import com.android.settings.Settings;
import com.android.settings.TestingSettings;
import com.android.settings.TrustedCredentialsSettings;
import com.android.settings.accessibility.AccessibilityDetailsSettingsFragment;
import com.android.settings.accessibility.AccessibilitySettings;
import com.android.settings.accessibility.AccessibilitySettingsForSetupWizard;
import com.android.settings.accessibility.CaptioningPropertiesFragment;
import com.android.settings.accessibility.ColorAndMotionFragment;
import com.android.settings.accessibility.ColorContrastFragment;
import com.android.settings.accessibility.TextReadingPreferenceFragment;
import com.android.settings.accessibility.TextReadingPreferenceFragmentForSetupWizard;
import com.android.settings.accessibility.ToggleColorInversionPreferenceFragment;
import com.android.settings.accessibility.ToggleDaltonizerPreferenceFragment;
import com.android.settings.accessibility.ToggleReduceBrightColorsPreferenceFragment;
import com.android.settings.accessibility.VibrationIntensitySettingsFragment;
import com.android.settings.accessibility.shortcuts.EditShortcutsPreferenceFragment;
import com.android.settings.accounts.AccountDashboardFragment;
import com.android.settings.accounts.AccountSyncSettings;
import com.android.settings.accounts.ChooseAccountFragment;
import com.android.settings.accounts.ManagedProfileSettings;
import com.android.settings.applications.AppDashboardFragment;
import com.android.settings.applications.ProcessStatsSummary;
import com.android.settings.applications.ProcessStatsUi;
import com.android.settings.applications.UsageAccessDetails;
import com.android.settings.applications.appcompat.UserAspectRatioDetails;
import com.android.settings.applications.appinfo.AlarmsAndRemindersDetails;
import com.android.settings.applications.appinfo.AppInfoDashboardFragment;
import com.android.settings.applications.appinfo.AppLocaleDetails;
import com.android.settings.applications.appinfo.DrawOverlayDetails;
import com.android.settings.applications.appinfo.ExternalSourcesDetails;
import com.android.settings.applications.appinfo.LongBackgroundTasksDetails;
import com.android.settings.applications.appinfo.ManageExternalStorageDetails;
import com.android.settings.applications.appinfo.MediaManagementAppsDetails;
import com.android.settings.applications.appinfo.TurnScreenOnDetails;
import com.android.settings.applications.appinfo.WriteSettingsDetails;
import com.android.settings.applications.appops.BackgroundCheckSummary;
import com.android.settings.applications.assist.ManageAssist;
import com.android.settings.applications.manageapplications.ManageApplications;
import com.android.settings.applications.managedomainurls.ManageDomainUrls;
import com.android.settings.applications.specialaccess.deviceadmin.DeviceAdminSettings;
import com.android.settings.applications.specialaccess.interactacrossprofiles.InteractAcrossProfilesDetails;
import com.android.settings.applications.specialaccess.interactacrossprofiles.InteractAcrossProfilesSettings;
import com.android.settings.applications.specialaccess.notificationaccess.NotificationAccessDetails;
import com.android.settings.applications.specialaccess.pictureinpicture.PictureInPictureDetails;
import com.android.settings.applications.specialaccess.pictureinpicture.PictureInPictureSettings;
import com.android.settings.applications.specialaccess.premiumsms.PremiumSmsAccess;
import com.android.settings.applications.specialaccess.vrlistener.VrListenerSettings;
import com.android.settings.applications.specialaccess.zenaccess.ZenAccessDetails;
import com.android.settings.backup.PrivacySettings;
import com.android.settings.backup.ToggleBackupSettingFragment;
import com.android.settings.backup.UserBackupSettingsActivity;
import com.android.settings.biometrics.combination.CombinedBiometricProfileSettings;
import com.android.settings.biometrics.combination.CombinedBiometricSettings;
import com.android.settings.biometrics.face.FaceSettings;
import com.android.settings.biometrics.fingerprint.FingerprintSettings;
import com.android.settings.biometrics.fingerprint2.ui.settings.fragment.FingerprintSettingsV2Fragment;
import com.android.settings.bluetooth.BluetoothBroadcastDialog;
import com.android.settings.bluetooth.BluetoothDeviceDetailsFragment;
import com.android.settings.bluetooth.BluetoothFindBroadcastsFragment;
import com.android.settings.bluetooth.BluetoothPairingDetail;
import com.android.settings.bugreporthandler.BugReportHandlerPicker;
import com.android.settings.communal.CommunalDashboardFragment;
import com.android.settings.connecteddevice.AdvancedConnectedDeviceDashboardFragment;
import com.android.settings.connecteddevice.ConnectedDeviceDashboardFragment;
import com.android.settings.connecteddevice.NfcAndPaymentFragment;
import com.android.settings.connecteddevice.PreviouslyConnectedDeviceDashboardFragment;
import com.android.settings.connecteddevice.stylus.StylusUsiDetailsFragment;
import com.android.settings.connecteddevice.usb.UsbDetailsFragment;
import com.android.settings.datausage.DataSaverSummary;
import com.android.settings.datausage.DataUsageList;
import com.android.settings.datausage.DataUsageSummary;
import com.android.settings.datetime.DateTimeSettings;
import com.android.settings.deletionhelper.AutomaticStorageManagerSettings;
import com.android.settings.development.DevelopmentSettingsDashboardFragment;
import com.android.settings.deviceinfo.PrivateVolumeForget;
import com.android.settings.deviceinfo.PublicVolumeSettings;
import com.android.settings.deviceinfo.StorageDashboardFragment;
import com.android.settings.deviceinfo.aboutphone.MyDeviceInfoFragment;
import com.android.settings.deviceinfo.batteryinfo.BatteryInfoFragment;
import com.android.settings.deviceinfo.firmwareversion.FirmwareVersionSettings;
import com.android.settings.deviceinfo.legal.ModuleLicensesDashboard;
import com.android.settings.display.AutoBrightnessSettings;
import com.android.settings.display.NightDisplaySettings;
import com.android.settings.display.ScreenTimeoutSettings;
import com.android.settings.display.SmartAutoRotatePreferenceFragment;
import com.android.settings.display.darkmode.DarkModeSettingsFragment;
import com.android.settings.dream.DreamSettings;
import com.android.settings.enterprise.EnterprisePrivacySettings;
import com.android.settings.fuelgauge.AdvancedPowerUsageDetail;
import com.android.settings.fuelgauge.batterysaver.BatterySaverScheduleSettings;
import com.android.settings.fuelgauge.batterysaver.BatterySaverSettings;
import com.android.settings.fuelgauge.batteryusage.PowerUsageSummary;
import com.android.settings.gestures.ButtonNavigationSettingsFragment;
import com.android.settings.gestures.DoubleTapPowerSettings;
import com.android.settings.gestures.DoubleTapScreenSettings;
import com.android.settings.gestures.DoubleTwistGestureSettings;
import com.android.settings.gestures.GestureNavigationSettingsFragment;
import com.android.settings.gestures.OneHandedSettings;
import com.android.settings.gestures.PickupGestureSettings;
import com.android.settings.gestures.PowerMenuSettings;
import com.android.settings.gestures.SwipeToNotificationSettings;
import com.android.settings.gestures.SystemNavigationGestureSettings;
import com.android.settings.inputmethod.AvailableVirtualKeyboardFragment;
import com.android.settings.inputmethod.KeyboardLayoutPickerFragment;
import com.android.settings.inputmethod.KeyboardSettings;
import com.android.settings.inputmethod.ModifierKeysSettings;
import com.android.settings.inputmethod.NewKeyboardLayoutEnabledLocalesFragment;
import com.android.settings.inputmethod.PhysicalKeyboardFragment;
import com.android.settings.inputmethod.SpellCheckersSettings;
import com.android.settings.inputmethod.TrackpadSettings;
import com.android.settings.inputmethod.UserDictionaryList;
import com.android.settings.inputmethod.UserDictionarySettings;
import com.android.settings.language.LanguageAndInputSettings;
import com.android.settings.language.LanguageSettings;
import com.android.settings.localepicker.LocaleListEditor;
import com.android.settings.location.LocationServices;
import com.android.settings.location.LocationSettings;
import com.android.settings.location.WifiScanningFragment;
import com.android.settings.network.MobileNetworkListFragment;
import com.android.settings.network.NetworkDashboardFragment;
import com.android.settings.network.NetworkProviderSettings;
import com.android.settings.network.apn.ApnEditor;
import com.android.settings.network.apn.ApnSettings;
import com.android.settings.network.telephony.MobileNetworkSettings;
import com.android.settings.network.telephony.NetworkSelectSettings;
import com.android.settings.network.telephony.SatelliteSetting;
import com.android.settings.network.tether.TetherSettings;
import com.android.settings.nfc.PaymentSettings;
import com.android.settings.notification.ConfigureNotificationSettings;
import com.android.settings.notification.NotificationAccessSettings;
import com.android.settings.notification.NotificationAssistantPicker;
import com.android.settings.notification.SoundSettings;
import com.android.settings.notification.app.AppBubbleNotificationSettings;
import com.android.settings.notification.app.AppNotificationSettings;
import com.android.settings.notification.app.ChannelNotificationSettings;
import com.android.settings.notification.app.ConversationListSettings;
import com.android.settings.notification.history.NotificationStation;
import com.android.settings.notification.zen.ZenAccessSettings;
import com.android.settings.notification.zen.ZenModeAutomationSettings;
import com.android.settings.notification.zen.ZenModeBlockedEffectsSettings;
import com.android.settings.notification.zen.ZenModeEventRuleSettings;
import com.android.settings.notification.zen.ZenModeScheduleRuleSettings;
import com.android.settings.notification.zen.ZenModeSettings;
import com.android.settings.password.ChooseLockPassword;
import com.android.settings.password.ChooseLockPattern;
import com.android.settings.print.PrintJobSettingsFragment;
import com.android.settings.print.PrintSettingsFragment;
import com.android.settings.privacy.PrivacyControlsFragment;
import com.android.settings.privacy.PrivacyDashboardFragment;
import com.android.settings.privatespace.delete.PrivateSpaceDeleteFragment;
import com.android.settings.privatespace.delete.PrivateSpaceDeletionProgressFragment;
import com.android.settings.privatespace.onelock.PrivateSpaceBiometricSettings;
import com.android.settings.regionalpreferences.RegionalPreferencesEntriesFragment;
import com.android.settings.safetycenter.MoreSecurityPrivacyFragment;
import com.android.settings.security.LockscreenDashboardFragment;
import com.android.settings.security.MemtagPage;
import com.android.settings.security.SecurityAdvancedSettings;
import com.android.settings.security.SecuritySettings;
import com.android.settings.shortcut.CreateShortcut;
import com.android.settings.sound.MediaControlsSettings;
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
import com.android.settings.wifi.WifiAPITest;
import com.android.settings.wifi.WifiInfo;
import com.android.settings.wifi.calling.WifiCallingDisclaimerFragment;
import com.android.settings.wifi.calling.WifiCallingSettings;
import com.android.settings.wifi.details.WifiNetworkDetailsFragment;
import com.android.settings.wifi.p2p.WifiP2pSettings;
import com.android.settings.wifi.savedaccesspoints2.SavedAccessPointsWifiSettings2;
import com.android.settings.wifi.tether.WifiTetherSettings;

public class SettingsGateway {

    /**
     * A list of fragment that can be hosted by SettingsActivity. SettingsActivity will throw a
     * security exception if the fragment it needs to display is not in this list.
     */
    public static final String[] ENTRY_FRAGMENTS = {
            AdvancedConnectedDeviceDashboardFragment.class.getName(),
            CreateShortcut.class.getName(),
            BluetoothPairingDetail.class.getName(),
            WifiNetworkDetailsFragment.class.getName(),
            ConfigureWifiSettings.class.getName(),
            SavedAccessPointsWifiSettings2.class.getName(),
            TetherSettings.class.getName(),
            SmartAutoRotatePreferenceFragment.class.getName(),
            WifiP2pSettings.class.getName(),
            WifiTetherSettings.class.getName(),
            BackgroundCheckSummary.class.getName(),
            VpnSettings.class.getName(),
            DataSaverSummary.class.getName(),
            DateTimeSettings.class.getName(),
            LocaleListEditor.class.getName(),
            AvailableVirtualKeyboardFragment.class.getName(),
            LanguageAndInputSettings.class.getName(),
            LanguageSettings.class.getName(),
            KeyboardSettings.class.getName(),
            ModifierKeysSettings.class.getName(),
            NewKeyboardLayoutEnabledLocalesFragment.class.getName(),
            TrackpadSettings.class.getName(),
            SpellCheckersSettings.class.getName(),
            UserDictionaryList.class.getName(),
            UserDictionarySettings.class.getName(),
            DisplaySettings.class.getName(),
            MyDeviceInfoFragment.class.getName(),
            ModuleLicensesDashboard.class.getName(),
            ManageApplications.class.getName(),
            FirmwareVersionSettings.class.getName(),
            ManageAssist.class.getName(),
            ProcessStatsUi.class.getName(),
            NotificationStation.class.getName(),
            LocationSettings.class.getName(),
            WifiScanningFragment.class.getName(),
            PrivacyDashboardFragment.class.getName(),
            PrivacyControlsFragment.class.getName(),
            LocationServices.class.getName(),
            SecuritySettings.class.getName(),
            SecurityAdvancedSettings.class.getName(),
            MoreSecurityPrivacyFragment.class.getName(),
            UsageAccessDetails.class.getName(),
            PrivacySettings.class.getName(),
            DeviceAdminSettings.class.getName(),
            AccessibilityDetailsSettingsFragment.class.getName(),
            AccessibilitySettings.class.getName(),
            AccessibilitySettingsForSetupWizard.class.getName(),
            EditShortcutsPreferenceFragment.class.getName(),
            TextReadingPreferenceFragment.class.getName(),
            TextReadingPreferenceFragmentForSetupWizard.class.getName(),
            CaptioningPropertiesFragment.class.getName(),
            ToggleDaltonizerPreferenceFragment.class.getName(),
            ToggleColorInversionPreferenceFragment.class.getName(),
            ToggleReduceBrightColorsPreferenceFragment.class.getName(),
            TextToSpeechSettings.class.getName(),
            PrivateVolumeForget.class.getName(),
            PublicVolumeSettings.class.getName(),
            DevelopmentSettingsDashboardFragment.class.getName(),
            WifiDisplaySettings.class.getName(),
            PowerUsageSummary.class.getName(),
            AccountSyncSettings.class.getName(),
            FaceSettings.class.getName(),
            FingerprintSettings.FingerprintSettingsFragment.class.getName(),
            FingerprintSettingsV2Fragment.class.getName(),
            CombinedBiometricSettings.class.getName(),
            CombinedBiometricProfileSettings.class.getName(),
            PrivateSpaceBiometricSettings.class.getName(),
            PrivateSpaceDeleteFragment.class.getName(),
            PrivateSpaceDeletionProgressFragment.class.getName(),
            SwipeToNotificationSettings.class.getName(),
            DoubleTapPowerSettings.class.getName(),
            DoubleTapScreenSettings.class.getName(),
            PickupGestureSettings.class.getName(),
            DoubleTwistGestureSettings.class.getName(),
            SystemNavigationGestureSettings.class.getName(),
            DataUsageSummary.class.getName(),
            DreamSettings.class.getName(),
            CommunalDashboardFragment.class.getName(),
            UserSettings.class.getName(),
            NotificationAccessSettings.class.getName(),
            NotificationAccessDetails.class.getName(),
            AppBubbleNotificationSettings.class.getName(),
            ZenAccessSettings.class.getName(),
            ZenAccessDetails.class.getName(),
            ZenModeAutomationSettings.class.getName(),
            PrintSettingsFragment.class.getName(),
            PrintJobSettingsFragment.class.getName(),
            TrustedCredentialsSettings.class.getName(),
            PaymentSettings.class.getName(),
            KeyboardLayoutPickerFragment.class.getName(),
            PhysicalKeyboardFragment.class.getName(),
            ZenModeSettings.class.getName(),
            SoundSettings.class.getName(),
            ConversationListSettings.class.getName(),
            ConfigureNotificationSettings.class.getName(),
            ChooseLockPassword.ChooseLockPasswordFragment.class.getName(),
            ChooseLockPattern.ChooseLockPatternFragment.class.getName(),
            AppInfoDashboardFragment.class.getName(),
            BatterySaverSettings.class.getName(),
            AppNotificationSettings.class.getName(),
            NotificationAssistantPicker.class.getName(),
            ChannelNotificationSettings.class.getName(),
            SatelliteSetting.class.getName(),
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
            ManageExternalStorageDetails.class.getName(),
            WallpaperTypeSettings.class.getName(),
            VrListenerSettings.class.getName(),
            PictureInPictureSettings.class.getName(),
            PictureInPictureDetails.class.getName(),
            PremiumSmsAccess.class.getName(),
            ManagedProfileSettings.class.getName(),
            ChooseAccountFragment.class.getName(),
            IccLockSettings.class.getName(),
            TestingSettings.class.getName(),
            WifiAPITest.class.getName(),
            WifiInfo.class.getName(),
            MainClear.class.getName(),
            MainClearConfirm.class.getName(),
            ResetDashboardFragment.class.getName(),
            NightDisplaySettings.class.getName(),
            ManageDomainUrls.class.getName(),
            AutomaticStorageManagerSettings.class.getName(),
            StorageDashboardFragment.class.getName(),
            SystemDashboardFragment.class.getName(),
            NetworkDashboardFragment.class.getName(),
            ConnectedDeviceDashboardFragment.class.getName(),
            UsbDetailsFragment.class.getName(),
            AppDashboardFragment.class.getName(),
            WifiCallingDisclaimerFragment.class.getName(),
            AccountDashboardFragment.class.getName(),
            EnterprisePrivacySettings.class.getName(),
            WebViewAppPicker.class.getName(),
            LockscreenDashboardFragment.class.getName(),
            MemtagPage.class.getName(),
            BluetoothDeviceDetailsFragment.class.getName(),
            BluetoothBroadcastDialog.class.getName(),
            BluetoothFindBroadcastsFragment.class.getName(),
            StylusUsiDetailsFragment.class.getName(),
            DataUsageList.class.getName(),
            ToggleBackupSettingFragment.class.getName(),
            PreviouslyConnectedDeviceDashboardFragment.class.getName(),
            BatterySaverScheduleSettings.class.getName(),
            MobileNetworkListFragment.class.getName(),
            PowerMenuSettings.class.getName(),
            DarkModeSettingsFragment.class.getName(),
            BugReportHandlerPicker.class.getName(),
            GestureNavigationSettingsFragment.class.getName(),
            ButtonNavigationSettingsFragment.class.getName(),
            InteractAcrossProfilesSettings.class.getName(),
            InteractAcrossProfilesDetails.class.getName(),
            MediaControlsSettings.class.getName(),
            NetworkProviderSettings.class.getName(),
            NetworkSelectSettings.class.getName(),
            AlarmsAndRemindersDetails.class.getName(),
            MediaManagementAppsDetails.class.getName(),
            AutoBrightnessSettings.class.getName(),
            OneHandedSettings.class.getName(),
            MobileNetworkSettings.class.getName(),
            AppLocaleDetails.class.getName(),
            TurnScreenOnDetails.class.getName(),
            NfcAndPaymentFragment.class.getName(),
            ColorAndMotionFragment.class.getName(),
            ColorContrastFragment.class.getName(),
            LongBackgroundTasksDetails.class.getName(),
            RegionalPreferencesEntriesFragment.class.getName(),
            BatteryInfoFragment.class.getName(),
            UserAspectRatioDetails.class.getName(),
            ScreenTimeoutSettings.class.getName(),
            ResetNetwork.class.getName(),
            VibrationIntensitySettingsFragment.class.getName(),
    };

    public static final String[] SETTINGS_FOR_RESTRICTED = {
            // Home page
            Settings.NetworkDashboardActivity.class.getName(),
            Settings.ConnectedDeviceDashboardActivity.class.getName(),
            Settings.AppDashboardActivity.class.getName(),
            Settings.DisplaySettingsActivity.class.getName(),
            Settings.SoundSettingsActivity.class.getName(),
            Settings.StorageDashboardActivity.class.getName(),
            Settings.PowerUsageSummaryActivity.class.getName(),
            Settings.AccountDashboardActivity.class.getName(),
            Settings.PrivacySettingsActivity.class.getName(),
            Settings.SecurityDashboardActivity.class.getName(),
            Settings.AccessibilitySettingsActivity.class.getName(),
            Settings.SystemDashboardActivity.class.getName(),
            SupportDashboardActivity.class.getName(),
            // Home page > Network & Internet
            Settings.WifiSettingsActivity.class.getName(),
            Settings.DataUsageSummaryActivity.class.getName(),
            Settings.NetworkProviderSettingsActivity.class.getName(),
            Settings.NetworkSelectActivity.class.getName(),
            // Home page > Connected devices
            Settings.BluetoothSettingsActivity.class.getName(),
            Settings.WifiDisplaySettingsActivity.class.getName(),
            Settings.PrintSettingsActivity.class.getName(),
            // Home page > Apps & Notifications
            Settings.UserSettingsActivity.class.getName(),
            Settings.ConfigureNotificationSettingsActivity.class.getName(),
            Settings.ManageApplicationsActivity.class.getName(),
            Settings.PaymentSettingsActivity.class.getName(),
            // Home page > Display
            Settings.AdaptiveBrightnessActivity.class.getName(),
            // Home page > Security & screen lock
            Settings.LocationSettingsActivity.class.getName(),
            // Home page > System
            Settings.LanguageAndInputSettingsActivity.class.getName(),
            Settings.LanguageSettingsActivity.class.getName(),
            Settings.KeyboardSettingsActivity.class.getName(),
            Settings.DateTimeSettingsActivity.class.getName(),
            Settings.EnterprisePrivacySettingsActivity.class.getName(),
            Settings.MyDeviceInfoActivity.class.getName(),
            Settings.ModuleLicensesActivity.class.getName(),
            UserBackupSettingsActivity.class.getName(),
            Settings.MemtagPageActivity.class.getName(),
            Settings.NavigationModeSettingsActivity.class.getName(),
    };
}
