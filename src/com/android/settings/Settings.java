/*
 * Copyright (C) 2008 The Android Open Source Project
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

package com.android.settings;

import com.android.settings.applications.AppOpsSummary;

/**
 * Top-level Settings activity
 */
public class Settings extends SettingsActivity {

    /*
    * Settings subclasses for launching independently.
    */
    public static class BluetoothSettingsActivity extends SettingsActivity { /* empty */ }
    public static class WirelessSettingsActivity extends SettingsActivity { /* empty */ }
    public static class SimSettingsActivity extends SettingsActivity { /* empty */ }
    public static class TetherSettingsActivity extends SettingsActivity { /* empty */ }
    public static class VpnSettingsActivity extends SettingsActivity { /* empty */ }
    public static class DateTimeSettingsActivity extends SettingsActivity { /* empty */ }
    public static class StorageSettingsActivity extends SettingsActivity { /* empty */ }
    public static class WifiSettingsActivity extends SettingsActivity { /* empty */ }
    public static class WifiP2pSettingsActivity extends SettingsActivity { /* empty */ }
    public static class InputMethodAndLanguageSettingsActivity extends SettingsActivity { /* empty */ }
    public static class KeyboardLayoutPickerActivity extends SettingsActivity { /* empty */ }
    public static class InputMethodAndSubtypeEnablerActivity extends SettingsActivity { /* empty */ }
    public static class VoiceInputSettingsActivity extends SettingsActivity { /* empty */ }
    public static class SpellCheckersSettingsActivity extends SettingsActivity { /* empty */ }
    public static class LocalePickerActivity extends SettingsActivity { /* empty */ }
    public static class UserDictionarySettingsActivity extends SettingsActivity { /* empty */ }
    public static class HomeSettingsActivity extends SettingsActivity { /* empty */ }
    public static class DisplaySettingsActivity extends SettingsActivity { /* empty */ }
    public static class DeviceInfoSettingsActivity extends SettingsActivity { /* empty */ }
    public static class ApplicationSettingsActivity extends SettingsActivity { /* empty */ }
    public static class ManageApplicationsActivity extends SettingsActivity { /* empty */ }
    public static class AppOpsSummaryActivity extends SettingsActivity {
        @Override
        public boolean isValidFragment(String className) {
            if (AppOpsSummary.class.getName().equals(className)) {
                return true;
            }
            return super.isValidFragment(className);
            }
    }
    public static class StorageUseActivity extends SettingsActivity { /* empty */ }
    public static class DevelopmentSettingsActivity extends SettingsActivity { /* empty */ }
    public static class AccessibilitySettingsActivity extends SettingsActivity { /* empty */ }
    public static class CaptioningSettingsActivity extends SettingsActivity { /* empty */ }
    public static class AccessibilityInversionSettingsActivity extends SettingsActivity { /* empty */ }
    public static class AccessibilityContrastSettingsActivity extends SettingsActivity { /* empty */ }
    public static class AccessibilityDaltonizerSettingsActivity extends SettingsActivity { /* empty */ }
    public static class SecuritySettingsActivity extends SettingsActivity { /* empty */ }
    public static class UsageAccessSettingsActivity extends SettingsActivity { /* empty */ }
    public static class LocationSettingsActivity extends SettingsActivity { /* empty */ }
    public static class PrivacySettingsActivity extends SettingsActivity { /* empty */ }
    public static class RunningServicesActivity extends SettingsActivity { /* empty */ }
    public static class ManageAccountsSettingsActivity extends SettingsActivity { /* empty */ }
    public static class PowerUsageSummaryActivity extends SettingsActivity { /* empty */ }
    public static class BatterySaverSettingsActivity extends SettingsActivity { /* empty */ }
    public static class AccountSyncSettingsActivity extends SettingsActivity { /* empty */ }
    public static class AccountSettingsActivity extends SettingsActivity { /* empty */ }
    public static class AccountSyncSettingsInAddAccountActivity extends SettingsActivity { /* empty */ }
    public static class CryptKeeperSettingsActivity extends SettingsActivity { /* empty */ }
    public static class DeviceAdminSettingsActivity extends SettingsActivity { /* empty */ }
    public static class DataUsageSummaryActivity extends SettingsActivity { /* empty */ }
    public static class AdvancedWifiSettingsActivity extends SettingsActivity { /* empty */ }
    public static class SavedAccessPointsSettingsActivity extends SettingsActivity { /* empty */ }
    public static class TextToSpeechSettingsActivity extends SettingsActivity { /* empty */ }
    public static class AndroidBeamSettingsActivity extends SettingsActivity { /* empty */ }
    public static class WifiDisplaySettingsActivity extends SettingsActivity { /* empty */ }
    public static class DreamSettingsActivity extends SettingsActivity { /* empty */ }
    public static class NotificationStationActivity extends SettingsActivity { /* empty */ }
    public static class UserSettingsActivity extends SettingsActivity { /* empty */ }
    public static class NotificationAccessSettingsActivity extends SettingsActivity { /* empty */ }
    public static class ConditionProviderSettingsActivity extends SettingsActivity { /* empty */ }
    public static class UsbSettingsActivity extends SettingsActivity { /* empty */ }
    public static class TrustedCredentialsSettingsActivity extends SettingsActivity { /* empty */ }
    public static class PaymentSettingsActivity extends SettingsActivity { /* empty */ }
    public static class PrintSettingsActivity extends SettingsActivity { /* empty */ }
    public static class PrintJobSettingsActivity extends SettingsActivity { /* empty */ }
    public static class ZenModeSettingsActivity extends SettingsActivity { /* empty */ }
    public static class NotificationSettingsActivity extends SettingsActivity { /* empty */ }
    public static class NotificationAppListActivity extends SettingsActivity { /* empty */ }
    public static class AppNotificationSettingsActivity extends SettingsActivity { /* empty */ }
    public static class OtherSoundSettingsActivity extends SettingsActivity { /* empty */ }
    public static class QuickLaunchSettingsActivity extends SettingsActivity { /* empty */ }

    public static class TopLevelSettings extends SettingsActivity { /* empty */ }
    public static class ApnSettingsActivity extends SettingsActivity { /* empty */ }
}

