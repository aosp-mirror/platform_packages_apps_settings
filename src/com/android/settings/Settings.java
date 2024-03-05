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

import static android.provider.Settings.ACTION_PRIVACY_SETTINGS;

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.os.Bundle;
import android.telephony.ims.ImsRcsManager;
import android.text.TextUtils;
import android.util.Log;

import com.android.internal.annotations.VisibleForTesting;
import com.android.settings.biometrics.face.FaceSettings;
import com.android.settings.communal.CommunalPreferenceController;
import com.android.settings.enterprise.EnterprisePrivacySettings;
import com.android.settings.network.MobileNetworkIntentConverter;
import com.android.settings.overlay.FeatureFactory;
import com.android.settings.safetycenter.SafetyCenterManagerWrapper;
import com.android.settings.security.SecuritySettingsFeatureProvider;

import com.google.android.setupdesign.util.ThemeHelper;

/**
 * Top-level Settings activity
 */
public class Settings extends SettingsActivity {

    /*
    * Settings subclasses for launching independently.
    */

    public static class MemtagPageActivity extends SettingsActivity { /* empty */}
    public static class BluetoothSettingsActivity extends SettingsActivity { /* empty */ }
    public static class CreateShortcutActivity extends SettingsActivity { /* empty */ }
    public static class FaceSettingsActivity extends SettingsActivity { /* empty */ }
    /** Container for {@link FaceSettings} to use with a pre-defined task affinity. */
    public static class FaceSettingsInternalActivity extends SettingsActivity { /* empty */ }
    public static class FingerprintSettingsActivity extends SettingsActivity { /* empty */ }
    public static class FingerprintSettingsActivityV2 extends SettingsActivity { /* empty */ }
    public static class CombinedBiometricSettingsActivity extends SettingsActivity { /* empty */ }
    public static class CombinedBiometricProfileSettingsActivity extends SettingsActivity { /* empty */ }
    public static class TetherSettingsActivity extends SettingsActivity { /* empty */ }
    public static class WifiTetherSettingsActivity extends SettingsActivity { /* empty */ }
    public static class PrivateSpaceBiometricSettingsActivity extends SettingsActivity {
        /* empty */
    }
    public static class VpnSettingsActivity extends SettingsActivity { /* empty */ }
    /** Activity for Data saver settings. */
    public static class DataSaverSummaryActivity extends SettingsActivity { /* empty */ }
    public static class DateTimeSettingsActivity extends SettingsActivity { /* empty */ }
    public static class PrivateVolumeForgetActivity extends SettingsActivity { /* empty */ }
    public static class PublicVolumeSettingsActivity extends SettingsActivity { /* empty */ }
    public static class WifiSettingsActivity extends SettingsActivity { /* empty */ }
    public static class NetworkProviderSettingsActivity extends SettingsActivity { /* empty */ }
    public static class NetworkSelectActivity extends SettingsActivity { /* empty */ }
    /** Activity for the Wi-Fi network details settings. */
    public static class WifiDetailsSettingsActivity extends SettingsActivity { /* empty */ }
    public static class WifiP2pSettingsActivity extends SettingsActivity { /* empty */ }
    public static class AvailableVirtualKeyboardActivity extends SettingsActivity { /* empty */ }
    public static class KeyboardLayoutPickerActivity extends SettingsActivity { /* empty */ }
    public static class PhysicalKeyboardActivity extends SettingsActivity { /* empty */ }
    public static class InputMethodAndSubtypeEnablerActivity extends SettingsActivity { /* empty */ }
    public static class SpellCheckersSettingsActivity extends SettingsActivity { /* empty */ }
    public static class LocalePickerActivity extends SettingsActivity { /* empty */ }
    public static class LanguageAndInputSettingsActivity extends SettingsActivity { /* empty */ }
    public static class LanguageSettingsActivity extends SettingsActivity { /* empty */ }
    /** Activity for the regional preferences settings. */
    public static class RegionalPreferencesActivity extends SettingsActivity { /* empty */ }
    public static class KeyboardSettingsActivity extends SettingsActivity { /* empty */ }
    /** Activity for the navigation mode settings. */
    public static class NavigationModeSettingsActivity extends SettingsActivity { /* empty */ }
    public static class UserDictionarySettingsActivity extends SettingsActivity { /* empty */ }
    public static class DarkThemeSettingsActivity extends SettingsActivity { /* empty */ }
    public static class DisplaySettingsActivity extends SettingsActivity { /* empty */ }
    public static class NightDisplaySettingsActivity extends SettingsActivity { /* empty */ }
    public static class NightDisplaySuggestionActivity extends NightDisplaySettingsActivity { /* empty */ }
    public static class SmartAutoRotateSettingsActivity extends SettingsActivity { /* empty */ }
    public static class MyDeviceInfoActivity extends SettingsActivity { /* empty */ }
    public static class ModuleLicensesActivity extends SettingsActivity { /* empty */ }
    public static class ApplicationSettingsActivity extends SettingsActivity { /* empty */ }
    public static class ManageApplicationsActivity extends SettingsActivity { /* empty */ }
    public static class ManageAssistActivity extends SettingsActivity { /* empty */ }
    public static class HighPowerApplicationsActivity extends SettingsActivity { /* empty */ }
    public static class BackgroundCheckSummaryActivity extends SettingsActivity { /* empty */ }
    public static class StorageUseActivity extends SettingsActivity { /* empty */ }
    public static class DevelopmentSettingsActivity extends SettingsActivity { /* empty */ }
    public static class AccessibilitySettingsActivity extends SettingsActivity { /* empty */ }
    public static class AccessibilityDetailsSettingsActivity extends SettingsActivity { /* empty */ }
    public static class AccessibilityEditShortcutsActivity extends SettingsActivity { /* empty */ }
    public static class CaptioningSettingsActivity extends SettingsActivity { /* empty */ }
    public static class AccessibilityInversionSettingsActivity extends SettingsActivity { /* empty */ }
    public static class AccessibilityContrastSettingsActivity extends SettingsActivity { /* empty */ }
    public static class AccessibilityDaltonizerSettingsActivity extends SettingsActivity { /* empty */ }
    /** Activity for lockscreen settings. */
    public static class LockScreenSettingsActivity extends SettingsActivity { /* empty */ }
    /** Activity for bluetooth pairing settings. */
    public static class BlueToothPairingActivity extends SettingsActivity { /* empty */ }
    /** Activity for Reduce Bright Colors. */
    public static class ReduceBrightColorsSettingsActivity extends SettingsActivity { /* empty */ }
    /** Activity for text reading settings. */
    public static class TextReadingSettingsActivity extends SettingsActivity { /* empty */ }
    /** Activity for text color and motion settings. */
    public static class ColorAndMotionActivity extends SettingsActivity { /* empty */ }
    /** Activity for color contrast settings. */
    public static class ColorContrastActivity extends SettingsActivity { /* empty */ }
    /** Activity for the security dashboard. */
    public static class SecurityDashboardActivity extends SettingsActivity {

        private static final String TAG = "SecurityDashboardActivity";

        @Override
        protected void onCreate(Bundle savedState) {
            super.onCreate(savedState);
            handleSafetyCenterRedirection();
        }

        /** Redirects to SafetyCenter if enabled. */
        @VisibleForTesting
        public void handleSafetyCenterRedirection() {
            if (isFinishing()) {
                // Don't trampoline if already exiting this activity.
                return;
            }

            if (SafetyCenterManagerWrapper.get().isEnabled(this)) {
                try {
                    startActivity(new Intent(Intent.ACTION_SAFETY_CENTER));
                    finish();
                } catch (ActivityNotFoundException e) {
                    Log.e(TAG, "Unable to open safety center", e);
                }
            }
        }

        /** Whether the given fragment is allowed. */
        @VisibleForTesting
        @Override
        public boolean isValidFragment(String fragmentName) {
            return super.isValidFragment(fragmentName)
                    || (fragmentName != null
                            && TextUtils.equals(fragmentName, getAlternativeFragmentName()));
        }

        @Override
        public String getInitialFragmentName(Intent intent) {
            final String alternativeFragmentName = getAlternativeFragmentName();
            if (alternativeFragmentName != null) {
                return alternativeFragmentName;
            }

            return super.getInitialFragmentName(intent);
        }

        private String getAlternativeFragmentName() {
            String alternativeFragmentClassname = null;
            final SecuritySettingsFeatureProvider securitySettingsFeatureProvider =
                    FeatureFactory.getFeatureFactory().getSecuritySettingsFeatureProvider();
            if (securitySettingsFeatureProvider.hasAlternativeSecuritySettingsFragment()) {
                alternativeFragmentClassname = securitySettingsFeatureProvider
                        .getAlternativeSecuritySettingsFragmentClassname();
            }
            return alternativeFragmentClassname;
        }
    }
    /** Activity for the Advanced security settings. */
    public static class SecurityAdvancedSettings extends SettingsActivity {
        private static final String TAG = "SecurityAdvancedActivity";
        @Override
        protected void onCreate(Bundle savedState) {
            super.onCreate(savedState);
            handleMoreSettingsRedirection();
        }

        /** Redirects to More Settings if Safety center is enabled. */
        @VisibleForTesting
        public void handleMoreSettingsRedirection() {
            if (isFinishing()) {
                // Don't trampoline if already exiting this activity.
                return;
            }

            if (SafetyCenterManagerWrapper.get().isEnabled(this)) {
                try {
                    startActivity(
                            new Intent("com.android.settings.MORE_SECURITY_PRIVACY_SETTINGS"));
                    finish();
                } catch (ActivityNotFoundException e) {
                    Log.e(TAG, "Unable to open More Settings", e);
                }
            }
        }
    }
    /** Activity for the More settings page. */
    public static class MoreSecurityPrivacySettingsActivity extends SettingsActivity { /* empty */ }
    public static class UsageAccessSettingsActivity extends SettingsActivity { /* empty */ }
    public static class AppUsageAccessSettingsActivity extends SettingsActivity { /* empty */ }
    public static class LocationSettingsActivity extends SettingsActivity { /* empty */ }
    public static class ScanningSettingsActivity extends SettingsActivity { /* empty */ }
    public static class WifiScanningSettingsActivity extends SettingsActivity { /* empty */ }
    /** Activity for the privacy dashboard. */
    public static class PrivacyDashboardActivity extends SettingsActivity {

        private static final String TAG = "PrivacyDashboardActivity";

        @Override
        protected void onCreate(Bundle savedState) {
            super.onCreate(savedState);
            handleSafetyCenterRedirection();
        }

        /** Redirects to SafetyCenter if enabled. */
        @VisibleForTesting
        public void handleSafetyCenterRedirection() {
            if (isFinishing()) {
                // Don't trampoline if already exiting this activity.
                return;
            }

            if (ACTION_PRIVACY_SETTINGS.equals(getIntent().getAction())
                    && SafetyCenterManagerWrapper.get().isEnabled(this)) {
                try {
                    startActivity(new Intent(Intent.ACTION_SAFETY_CENTER));
                    finish();
                } catch (ActivityNotFoundException e) {
                    Log.e(TAG, "Unable to open safety center", e);
                }
            }
        }
    }
    public static class PrivacyControlsActivity extends SettingsActivity { /* empty */ }
    public static class PrivacySettingsActivity extends SettingsActivity { /* empty */ }
    public static class FactoryResetActivity extends SettingsActivity {
        @Override
        protected void onCreate(Bundle savedState) {
            setTheme(SetupWizardUtils.getTheme(this, getIntent()));
            ThemeHelper.trySetDynamicColor(this);
            super.onCreate(savedState);
        }

        @Override
        protected boolean isToolbarEnabled() {
            return false;
        }
    }
    public static class FactoryResetConfirmActivity extends SettingsActivity {
        @Override
        protected void onCreate(Bundle savedState) {
            setTheme(SetupWizardUtils.getTheme(this, getIntent()));
            ThemeHelper.trySetDynamicColor(this);
            super.onCreate(savedState);
        }

        @Override
        protected boolean isToolbarEnabled() {
            return false;
        }
    }
    public static class RunningServicesActivity extends SettingsActivity { /* empty */ }
    public static class BatterySaverSettingsActivity extends SettingsActivity { /* empty */ }
    public static class BatterySaverScheduleSettingsActivity extends SettingsActivity { /* empty */ }
    public static class AccountSyncSettingsActivity extends SettingsActivity { /* empty */ }
    public static class AccountSyncSettingsInAddAccountActivity extends SettingsActivity { /* empty */ }
    public static class DeviceAdminSettingsActivity extends SettingsActivity { /* empty */ }
    public static class DataUsageSummaryActivity extends SettingsActivity { /* empty */ }
    public static class MobileDataUsageListActivity extends SettingsActivity { /* empty */ }
    public static class ConfigureWifiSettingsActivity extends SettingsActivity { /* empty */ }
    public static class SavedAccessPointsSettingsActivity extends SettingsActivity { /* empty */ }
    public static class TextToSpeechSettingsActivity extends SettingsActivity { /* empty */ }
    public static class WifiDisplaySettingsActivity extends SettingsActivity { /* empty */ }
    public static class DreamSettingsActivity extends SettingsActivity { /* empty */ }
    /** Activity to manage communal settings */
    public static class CommunalSettingsActivity extends SettingsActivity {
        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            if (!CommunalPreferenceController.isAvailable(this)) {
                finish();
            }
        }
    }
    public static class NotificationStationActivity extends SettingsActivity { /* empty */ }
    public static class UserSettingsActivity extends SettingsActivity { /* empty */ }
    public static class NotificationAccessSettingsActivity extends SettingsActivity { /* empty */ }
    public static class NotificationAccessDetailsActivity extends SettingsActivity { /* empty */ }
    public static class VrListenersSettingsActivity extends SettingsActivity { /* empty */ }
    public static class PremiumSmsAccessActivity extends SettingsActivity { /* empty */ }
    public static class PictureInPictureSettingsActivity extends SettingsActivity { /* empty */ }
    public static class TurnScreenOnSettingsActivity extends SettingsActivity { /* empty */ }
    public static class AppTurnScreenOnSettingsActivity extends SettingsActivity { /* empty */ }
    public static class AppPictureInPictureSettingsActivity extends SettingsActivity { /* empty */ }
    public static class ZenAccessSettingsActivity extends SettingsActivity { /* empty */ }
    public static class ZenAccessDetailSettingsActivity extends SettingsActivity {}
    public static class ConditionProviderSettingsActivity extends SettingsActivity { /* empty */ }
    public static class UsbSettingsActivity extends SettingsActivity { /* empty */ }
    public static class UsbDetailsActivity extends SettingsActivity { /* empty */ }
    public static class TrustedCredentialsSettingsActivity extends SettingsActivity { /* empty */ }
    public static class PaymentSettingsActivity extends SettingsActivity { /* empty */ }
    public static class PrintSettingsActivity extends SettingsActivity { /* empty */ }
    public static class PrintJobSettingsActivity extends SettingsActivity { /* empty */ }
    public static class ZenModeSettingsActivity extends SettingsActivity { /* empty */ }
    public static class ZenModeBehaviorSettingsActivity extends SettingsActivity { /* empty */ }
    public static class ZenModeBlockedEffectsSettingsActivity extends SettingsActivity { /* empty */ }
    public static class ZenModeAutomationSettingsActivity extends SettingsActivity { /* empty */ }
    public static class ZenModeScheduleRuleSettingsActivity extends SettingsActivity { /* empty */ }
    public static class ZenModeEventRuleSettingsActivity extends SettingsActivity { /* empty */ }
    public static class SoundSettingsActivity extends SettingsActivity { /* empty */ }
    public static class ConfigureNotificationSettingsActivity extends SettingsActivity { /* empty */ }
    public static class ConversationListSettingsActivity extends SettingsActivity { /* empty */ }
    public static class AppBubbleNotificationSettingsActivity extends SettingsActivity { /* empty */ }
    public static class NotificationAssistantSettingsActivity extends SettingsActivity{ /* empty */ }
    public static class NotificationAppListActivity extends SettingsActivity { /* empty */ }
    /** Activity to manage Cloned Apps page */
    public static class ClonedAppsListActivity extends SettingsActivity { /* empty */ }
    /** Activity to manage Aspect Ratio app list page */
    public static class UserAspectRatioAppListActivity extends SettingsActivity { /* empty */ }
    /** Activity to manage Aspect Ratio app page */
    public static class UserAspectRatioAppActivity extends SettingsActivity { /* empty */ }
    public static class NotificationReviewPermissionsActivity extends SettingsActivity { /* empty */ }
    public static class AppNotificationSettingsActivity extends SettingsActivity { /* empty */ }
    public static class ChannelNotificationSettingsActivity extends SettingsActivity { /* empty */ }
    public static class ChannelGroupNotificationSettingsActivity extends SettingsActivity { /* empty */ }
    public static class ManageDomainUrlsActivity extends SettingsActivity { /* empty */ }
    public static class AutomaticStorageManagerSettingsActivity extends SettingsActivity { /* empty */ }
    public static class GamesStorageActivity extends SettingsActivity { /* empty */ }
    public static class GestureNavigationSettingsActivity extends SettingsActivity { /* empty */ }
    /** Activity to manage 2-/3-button navigation configuration. */
    public static class ButtonNavigationSettingsActivity extends SettingsActivity { /* empty */ }
    public static class InteractAcrossProfilesSettingsActivity extends SettingsActivity {
        /* empty */
    }
    public static class AppInteractAcrossProfilesSettingsActivity extends SettingsActivity {
        /* empty */
    }

    public static class SatelliteSettingActivity extends SettingsActivity { /* empty */ }
    public static class ApnSettingsActivity extends SettingsActivity { /* empty */ }
    public static class WifiCallingSettingsActivity extends SettingsActivity { /* empty */ }
    public static class MemorySettingsActivity extends SettingsActivity { /* empty */ }
    public static class AppMemoryUsageActivity extends SettingsActivity { /* empty */ }
    public static class OverlaySettingsActivity extends SettingsActivity { /* empty */ }
    public static class ManageExternalStorageActivity extends SettingsActivity { /* empty */ }
    public static class AppManageExternalStorageActivity extends SettingsActivity { /* empty */ }
    public static class MediaManagementAppsActivity extends SettingsActivity { /* empty */ }
    public static class AppMediaManagementAppsActivity extends SettingsActivity { /* empty */ }
    public static class WriteSettingsActivity extends SettingsActivity { /* empty */ }
    public static class ChangeWifiStateActivity extends SettingsActivity { /* empty */ }
    /** Activity to manage NFC Tag applications. */
    public static class ChangeNfcTagAppsActivity extends SettingsActivity { /* empty */ }
    public static class AppDrawOverlaySettingsActivity extends SettingsActivity { /* empty */ }
    public static class AppWriteSettingsActivity extends SettingsActivity { /* empty */ }
    /** Activity to manage app battery usage details. */
    public static class AppBatteryUsageActivity extends SettingsActivity { /* empty */ }

    public static class ManageExternalSourcesActivity extends SettingsActivity {/* empty */ }
    public static class ManageAppExternalSourcesActivity extends SettingsActivity { /* empty */ }
    public static class WallpaperSettingsActivity extends SettingsActivity { /* empty */ }
    public static class ManagedProfileSettingsActivity extends SettingsActivity { /* empty */ }
    public static class DeletionHelperActivity extends SettingsActivity { /* empty */ }

    /** Actviity to manage apps with {@link android.Manifest.permission#SCHEDULE_EXACT_ALARM} */
    public static class AlarmsAndRemindersActivity extends SettingsActivity {/* empty */ }
    /** App specific version of {@link AlarmsAndRemindersActivity} */
    public static class AlarmsAndRemindersAppActivity extends SettingsActivity {/* empty */ }

    public static class ApnEditorActivity extends SettingsActivity { /* empty */ }
    public static class ChooseAccountActivity extends SettingsActivity { /* empty */ }
    public static class IccLockSettingsActivity extends SettingsActivity { /* empty */ }
    public static class TestingSettingsActivity extends SettingsActivity { /* empty */ }
    public static class WifiAPITestActivity extends SettingsActivity { /* empty */ }
    public static class WifiInfoActivity extends SettingsActivity { /* empty */ }
    public static class EnterprisePrivacySettingsActivity extends SettingsActivity {
        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            if (FeatureFactory.getFeatureFactory().getEnterprisePrivacyFeatureProvider()
                    .showParentalControls()) {
                finish();
            } else if (!EnterprisePrivacySettings.isPageEnabled(this)) {
                finish();
            }
        }
    }
    public static class WebViewAppPickerActivity extends SettingsActivity { /* empty */ }
    public static class AdvancedConnectedDeviceActivity extends SettingsActivity { /* empty */ }
    public static class NfcSettingsActivity extends SettingsActivity { /* empty */ }
    public static class BluetoothDeviceDetailActivity extends SettingsActivity { /* empty */ }
    public static class StylusUsiDetailsActivity extends SettingsActivity { /* empty */ }
    public static class BluetoothBroadcastActivity extends SettingsActivity { /* empty */ }
    public static class BluetoothFindBroadcastsActivity extends SettingsActivity { /* empty */ }
    public static class WifiCallingDisclaimerActivity extends SettingsActivity { /* empty */ }
    public static class MobileNetworkListActivity extends SettingsActivity {}
    public static class PowerMenuSettingsActivity extends SettingsActivity {}
    public static class MobileNetworkActivity extends SettingsActivity {

        public static final String TAG = "MobileNetworkActivity";
        public static final String EXTRA_MMS_MESSAGE = "mms_message";
        public static final String EXTRA_SHOW_CAPABILITY_DISCOVERY_OPT_IN =
                "show_capability_discovery_opt_in";

        private MobileNetworkIntentConverter mIntentConverter;

        /**
         * Override of #onNewIntent() requires Activity to have "singleTop" launch mode within
         * AndroidManifest.xml
         */
        @Override
        protected void onNewIntent(Intent intent) {
            super.onNewIntent(intent);

            Log.d(TAG, "Starting onNewIntent");

            createUiFromIntent(null /* savedState */, convertIntent(intent));
        }

        @Override
        public Intent getIntent() {
            return convertIntent(super.getIntent());
        }

        private Intent convertIntent(Intent copyFrom) {
            if (mIntentConverter == null) {
                mIntentConverter = new MobileNetworkIntentConverter(this);
            }
            Intent intent = mIntentConverter.apply(copyFrom);
            return (intent == null) ? copyFrom : intent;
        }

        public static boolean doesIntentContainOptInAction(Intent intent) {
            String intentAction = (intent != null ? intent.getAction() : null);
            return TextUtils.equals(intentAction,
                    ImsRcsManager.ACTION_SHOW_CAPABILITY_DISCOVERY_OPT_IN);
        }
    }

    /** Actviity to manage apps with {@link android.Manifest.permission#RUN_USER_INITIATED_JOBS} */
    public static class LongBackgroundTasksActivity extends SettingsActivity { /* empty */ }
    /** App specific version of {@link LongBackgroundTasksActivity} */
    public static class LongBackgroundTasksAppActivity extends SettingsActivity { /* empty */ }

    /**
     * Activity for BugReportHandlerPicker.
     */
    public static class BugReportHandlerPickerActivity extends SettingsActivity { /* empty */ }

    // Top level categories for new IA
    public static class NetworkDashboardActivity extends SettingsActivity {}
    public static class ConnectedDeviceDashboardActivity extends SettingsActivity {}
    public static class PowerUsageSummaryActivity extends SettingsActivity { /* empty */ }
    public static class StorageDashboardActivity extends SettingsActivity {}
    public static class AccountDashboardActivity extends SettingsActivity {}
    public static class SystemDashboardActivity extends SettingsActivity {}

    /**
     * Activity for MediaControlsSettings
     */
    public static class MediaControlsSettingsActivity extends SettingsActivity {}

    /**
     * Activity for AppDashboard.
     */
    public static class AppDashboardActivity extends SettingsActivity {}

    public static class AdaptiveBrightnessActivity extends SettingsActivity { /* empty */ }

    /**
     * Activity for OneHandedSettings
     */
    public static class OneHandedSettingsActivity extends SettingsActivity { /* empty */ }

    public static class PreviouslyConnectedDeviceActivity extends SettingsActivity { /* empty */ }

    public static class ScreenTimeoutActivity extends SettingsActivity { /* empty */ }

    /** Activity for the Reset mobile network settings. */
    public static class ResetMobileNetworkSettingsActivity extends SettingsActivity { /* empty */ }
}
