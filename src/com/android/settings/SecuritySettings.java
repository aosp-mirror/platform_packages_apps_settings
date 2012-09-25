/*
 * Copyright (C) 2007 The Android Open Source Project
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


import static android.provider.Settings.System.SCREEN_OFF_TIMEOUT;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.admin.DevicePolicyManager;
import android.appwidget.AppWidgetHost;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProviderInfo;
import android.content.ActivityNotFoundException;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.os.Bundle;
import android.os.UserHandle;
import android.os.Vibrator;
import android.preference.CheckBoxPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.PreferenceGroup;
import android.preference.PreferenceScreen;
import android.provider.Settings;
import android.security.KeyStore;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.widget.Toast;

import com.android.internal.widget.LockPatternUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * Gesture lock pattern settings.
 */
public class SecuritySettings extends SettingsPreferenceFragment
        implements OnPreferenceChangeListener, DialogInterface.OnClickListener {
    static final String TAG = "SecuritySettings";

    // Lock Settings
    private static final String KEY_UNLOCK_SET_OR_CHANGE = "unlock_set_or_change";
    private static final String KEY_CHOOSE_LOCKSCREEN_STATUS_WIDGET =
            "choose_lockscreen_status_widget";
    private static final String KEY_CHOOSE_USER_SELECTED_LOCKSCREEN_WIDGET =
            "choose_user_selected_lockscreen_widget";
    private static final String KEY_BIOMETRIC_WEAK_IMPROVE_MATCHING =
            "biometric_weak_improve_matching";
    private static final String KEY_BIOMETRIC_WEAK_LIVELINESS = "biometric_weak_liveliness";
    private static final String KEY_LOCK_ENABLED = "lockenabled";
    private static final String KEY_VISIBLE_PATTERN = "visiblepattern";
    private static final String KEY_TACTILE_FEEDBACK_ENABLED = "unlock_tactile_feedback";
    private static final String KEY_SECURITY_CATEGORY = "security_category";
    private static final String KEY_LOCK_AFTER_TIMEOUT = "lock_after_timeout";
    private static final String EXTRA_NO_WIDGET = "com.android.settings.NO_WIDGET";
    private static final String EXTRA_DEFAULT_WIDGET = "com.android.settings.DEFAULT_WIDGET";
    private static final int SET_OR_CHANGE_LOCK_METHOD_REQUEST = 123;
    private static final int CONFIRM_EXISTING_FOR_BIOMETRIC_WEAK_IMPROVE_REQUEST = 124;
    private static final int CONFIRM_EXISTING_FOR_BIOMETRIC_WEAK_LIVELINESS_OFF = 125;
    private static final int REQUEST_PICK_USER_SELECTED_APPWIDGET = 126;
    private static final int REQUEST_PICK_STATUS_APPWIDGET = 127;
    private static final int REQUEST_CREATE_USER_SELECTED_APPWIDGET = 128;
    private static final int REQUEST_CREATE_STATUS_APPWIDGET = 129;

    // Misc Settings
    private static final String KEY_SIM_LOCK = "sim_lock";
    private static final String KEY_SHOW_PASSWORD = "show_password";
    private static final String KEY_RESET_CREDENTIALS = "reset_credentials";
    private static final String KEY_TOGGLE_INSTALL_APPLICATIONS = "toggle_install_applications";
    private static final String KEY_TOGGLE_VERIFY_APPLICATIONS = "toggle_verify_applications";
    private static final String KEY_POWER_INSTANTLY_LOCKS = "power_button_instantly_locks";
    private static final String PACKAGE_MIME_TYPE = "application/vnd.android.package-archive";

    DevicePolicyManager mDPM;

    private ChooseLockSettingsHelper mChooseLockSettingsHelper;
    private Preference mUserSelectedWidget;
    private LockPatternUtils mLockPatternUtils;
    private ListPreference mLockAfter;

    private CheckBoxPreference mBiometricWeakLiveliness;
    private CheckBoxPreference mVisiblePattern;
    private CheckBoxPreference mTactileFeedback;

    private CheckBoxPreference mShowPassword;

    private Preference mResetCredentials;

    private CheckBoxPreference mToggleAppInstallation;
    private DialogInterface mWarnInstallApps;
    private CheckBoxPreference mToggleVerifyApps;
    private CheckBoxPreference mPowerButtonInstantlyLocks;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mLockPatternUtils = new LockPatternUtils(getActivity());

        mDPM = (DevicePolicyManager)getSystemService(Context.DEVICE_POLICY_SERVICE);

        mChooseLockSettingsHelper = new ChooseLockSettingsHelper(getActivity());
    }

    private PreferenceScreen createPreferenceHierarchy() {
        PreferenceScreen root = getPreferenceScreen();
        if (root != null) {
            root.removeAll();
        }
        addPreferencesFromResource(R.xml.security_settings);
        root = getPreferenceScreen();

        // Add options for lock/unlock screen
        int resid = 0;
        if (!mLockPatternUtils.isSecure()) {
            if (mLockPatternUtils.isLockScreenDisabled()) {
                resid = R.xml.security_settings_lockscreen;
            } else {
                resid = R.xml.security_settings_chooser;
            }
        } else if (mLockPatternUtils.usingBiometricWeak() &&
                mLockPatternUtils.isBiometricWeakInstalled()) {
            resid = R.xml.security_settings_biometric_weak;
        } else {
            switch (mLockPatternUtils.getKeyguardStoredPasswordQuality()) {
                case DevicePolicyManager.PASSWORD_QUALITY_SOMETHING:
                    resid = R.xml.security_settings_pattern;
                    break;
                case DevicePolicyManager.PASSWORD_QUALITY_NUMERIC:
                    resid = R.xml.security_settings_pin;
                    break;
                case DevicePolicyManager.PASSWORD_QUALITY_ALPHABETIC:
                case DevicePolicyManager.PASSWORD_QUALITY_ALPHANUMERIC:
                case DevicePolicyManager.PASSWORD_QUALITY_COMPLEX:
                    resid = R.xml.security_settings_password;
                    break;
            }
        }
        addPreferencesFromResource(resid);


        // Add options for device encryption
        DevicePolicyManager dpm =
                (DevicePolicyManager) getSystemService(Context.DEVICE_POLICY_SERVICE);

        if (UserHandle.myUserId() == 0) {
            switch (dpm.getStorageEncryptionStatus()) {
            case DevicePolicyManager.ENCRYPTION_STATUS_ACTIVE:
                // The device is currently encrypted.
                addPreferencesFromResource(R.xml.security_settings_encrypted);
                break;
            case DevicePolicyManager.ENCRYPTION_STATUS_INACTIVE:
                // This device supports encryption but isn't encrypted.
                addPreferencesFromResource(R.xml.security_settings_unencrypted);
                break;
            }
        }

        // lock after preference
        mLockAfter = (ListPreference) root.findPreference(KEY_LOCK_AFTER_TIMEOUT);
        if (mLockAfter != null) {
            setupLockAfterPreference();
            updateLockAfterPreferenceSummary();
        }

        // biometric weak liveliness
        mBiometricWeakLiveliness =
                (CheckBoxPreference) root.findPreference(KEY_BIOMETRIC_WEAK_LIVELINESS);

        // visible pattern
        mVisiblePattern = (CheckBoxPreference) root.findPreference(KEY_VISIBLE_PATTERN);

        // lock instantly on power key press
        mPowerButtonInstantlyLocks = (CheckBoxPreference) root.findPreference(
                KEY_POWER_INSTANTLY_LOCKS);

        // don't display visible pattern if biometric and backup is not pattern
        if (resid == R.xml.security_settings_biometric_weak &&
                mLockPatternUtils.getKeyguardStoredPasswordQuality() !=
                DevicePolicyManager.PASSWORD_QUALITY_SOMETHING) {
            PreferenceGroup securityCategory = (PreferenceGroup)
                    root.findPreference(KEY_SECURITY_CATEGORY);
            if (securityCategory != null && mVisiblePattern != null) {
                securityCategory.removePreference(root.findPreference(KEY_VISIBLE_PATTERN));
            }
        }

        // tactile feedback. Should be common to all unlock preference screens.
        mTactileFeedback = (CheckBoxPreference) root.findPreference(KEY_TACTILE_FEEDBACK_ENABLED);
        if (!((Vibrator) getSystemService(Context.VIBRATOR_SERVICE)).hasVibrator()) {
            PreferenceGroup securityCategory = (PreferenceGroup)
                    root.findPreference(KEY_SECURITY_CATEGORY);
            if (securityCategory != null && mTactileFeedback != null) {
                securityCategory.removePreference(mTactileFeedback);
            }
        }

        if (UserHandle.myUserId() > 0) {
            return root;
        }
        // Rest are for primary user...

        // Append the rest of the settings
        addPreferencesFromResource(R.xml.security_settings_misc);

        // Do not display SIM lock for devices without an Icc card
        TelephonyManager tm = TelephonyManager.getDefault();
        if (!tm.hasIccCard()) {
            root.removePreference(root.findPreference(KEY_SIM_LOCK));
        } else {
            // Disable SIM lock if sim card is missing or unknown
            if ((TelephonyManager.getDefault().getSimState() ==
                                 TelephonyManager.SIM_STATE_ABSENT) ||
                (TelephonyManager.getDefault().getSimState() ==
                                 TelephonyManager.SIM_STATE_UNKNOWN)) {
                root.findPreference(KEY_SIM_LOCK).setEnabled(false);
            }
        }

        // Show password
        mShowPassword = (CheckBoxPreference) root.findPreference(KEY_SHOW_PASSWORD);

        // Credential storage
        mResetCredentials = root.findPreference(KEY_RESET_CREDENTIALS);

        mToggleAppInstallation = (CheckBoxPreference) findPreference(
                KEY_TOGGLE_INSTALL_APPLICATIONS);
        mToggleAppInstallation.setChecked(isNonMarketAppsAllowed());

        // Package verification
        if (showVerifierSetting()) {
            mToggleVerifyApps = (CheckBoxPreference) findPreference(KEY_TOGGLE_VERIFY_APPLICATIONS);
            if (isVerifierInstalled()) {
                mToggleVerifyApps.setChecked(isVerifyAppsEnabled());
            } else {
                mToggleVerifyApps.setChecked(false);
                mToggleVerifyApps.setEnabled(false);
            }
        }

        mUserSelectedWidget = root.findPreference(KEY_CHOOSE_LOCKSCREEN_STATUS_WIDGET);
        if (mUserSelectedWidget != null) {
            AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(getActivity());
            int appWidgetId = getStatusAppWidgetId();
            if (appWidgetId == -1) {
                mUserSelectedWidget.setSummary(getResources().getString(R.string.widget_default));
            } else {
                AppWidgetProviderInfo appWidget = appWidgetManager.getAppWidgetInfo(appWidgetId);
                if (appWidget != null) {
                    mUserSelectedWidget.setSummary(appWidget.label);
                }
            }
        }

        mUserSelectedWidget = root.findPreference(KEY_CHOOSE_USER_SELECTED_LOCKSCREEN_WIDGET);
        if (mUserSelectedWidget != null) {
            AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(getActivity());
            int appWidgetId = getUserSelectedAppWidgetId();
            if (appWidgetId == -1) {
                mUserSelectedWidget.setSummary(getResources().getString(R.string.widget_none));
            } else {
                AppWidgetProviderInfo appWidget = appWidgetManager.getAppWidgetInfo(appWidgetId);
                if (appWidget != null) {
                    mUserSelectedWidget.setSummary(appWidget.label);
                }
            }
        }

        return root;
    }

    private int getStatusAppWidgetId() {
        int appWidgetId = -1;
        String appWidgetIdString = Settings.Secure.getString(
                getContentResolver(), Settings.Secure.LOCK_SCREEN_STATUS_APPWIDGET_ID);
        if (appWidgetIdString != null) {;
            appWidgetId = (int) Integer.decode(appWidgetIdString);
        }
        return appWidgetId;
    }

    private int getUserSelectedAppWidgetId() {
        int appWidgetId = -1;
        String appWidgetIdString = Settings.Secure.getString(
                getContentResolver(), Settings.Secure.LOCK_SCREEN_USER_SELECTED_APPWIDGET_ID);
        if (appWidgetIdString != null) {;
            appWidgetId = (int) Integer.decode(appWidgetIdString);
        }
        return appWidgetId;
    }

    private boolean isNonMarketAppsAllowed() {
        return Settings.Global.getInt(getContentResolver(),
                                      Settings.Global.INSTALL_NON_MARKET_APPS, 0) > 0;
    }

    private void setNonMarketAppsAllowed(boolean enabled) {
        // Change the system setting
        Settings.Global.putInt(getContentResolver(), Settings.Global.INSTALL_NON_MARKET_APPS,
                                enabled ? 1 : 0);
    }

    private boolean isVerifyAppsEnabled() {
        return Settings.Global.getInt(getContentResolver(),
                                      Settings.Global.PACKAGE_VERIFIER_ENABLE, 1) > 0;
    }

    private boolean isVerifierInstalled() {
        final PackageManager pm = getPackageManager();
        final Intent verification = new Intent(Intent.ACTION_PACKAGE_NEEDS_VERIFICATION);
        verification.setType(PACKAGE_MIME_TYPE);
        verification.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        final List<ResolveInfo> receivers = pm.queryBroadcastReceivers(verification, 0);
        return (receivers.size() > 0) ? true : false;
    }

    private boolean showVerifierSetting() {
        return Settings.Global.getInt(getContentResolver(),
                                      Settings.Global.PACKAGE_VERIFIER_SETTING_VISIBLE, 1) > 0;
    }

    private void warnAppInstallation() {
        // TODO: DialogFragment?
        mWarnInstallApps = new AlertDialog.Builder(getActivity()).setTitle(
                getResources().getString(R.string.error_title))
                .setIcon(com.android.internal.R.drawable.ic_dialog_alert)
                .setMessage(getResources().getString(R.string.install_all_warning))
                .setPositiveButton(android.R.string.yes, this)
                .setNegativeButton(android.R.string.no, null)
                .show();
    }

    public void onClick(DialogInterface dialog, int which) {
        if (dialog == mWarnInstallApps && which == DialogInterface.BUTTON_POSITIVE) {
            setNonMarketAppsAllowed(true);
            if (mToggleAppInstallation != null) {
                mToggleAppInstallation.setChecked(true);
            }
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mWarnInstallApps != null) {
            mWarnInstallApps.dismiss();
        }
    }

    private void setupLockAfterPreference() {
        // Compatible with pre-Froyo
        long currentTimeout = Settings.Secure.getLong(getContentResolver(),
                Settings.Secure.LOCK_SCREEN_LOCK_AFTER_TIMEOUT, 5000);
        mLockAfter.setValue(String.valueOf(currentTimeout));
        mLockAfter.setOnPreferenceChangeListener(this);
        final long adminTimeout = (mDPM != null ? mDPM.getMaximumTimeToLock(null) : 0);
        final long displayTimeout = Math.max(0,
                Settings.System.getInt(getContentResolver(), SCREEN_OFF_TIMEOUT, 0));
        if (adminTimeout > 0) {
            // This setting is a slave to display timeout when a device policy is enforced.
            // As such, maxLockTimeout = adminTimeout - displayTimeout.
            // If there isn't enough time, shows "immediately" setting.
            disableUnusableTimeouts(Math.max(0, adminTimeout - displayTimeout));
        }
    }

    private void updateLockAfterPreferenceSummary() {
        // Update summary message with current value
        long currentTimeout = Settings.Secure.getLong(getContentResolver(),
                Settings.Secure.LOCK_SCREEN_LOCK_AFTER_TIMEOUT, 5000);
        final CharSequence[] entries = mLockAfter.getEntries();
        final CharSequence[] values = mLockAfter.getEntryValues();
        int best = 0;
        for (int i = 0; i < values.length; i++) {
            long timeout = Long.valueOf(values[i].toString());
            if (currentTimeout >= timeout) {
                best = i;
            }
        }
        mLockAfter.setSummary(getString(R.string.lock_after_timeout_summary, entries[best]));
    }

    private void disableUnusableTimeouts(long maxTimeout) {
        final CharSequence[] entries = mLockAfter.getEntries();
        final CharSequence[] values = mLockAfter.getEntryValues();
        ArrayList<CharSequence> revisedEntries = new ArrayList<CharSequence>();
        ArrayList<CharSequence> revisedValues = new ArrayList<CharSequence>();
        for (int i = 0; i < values.length; i++) {
            long timeout = Long.valueOf(values[i].toString());
            if (timeout <= maxTimeout) {
                revisedEntries.add(entries[i]);
                revisedValues.add(values[i]);
            }
        }
        if (revisedEntries.size() != entries.length || revisedValues.size() != values.length) {
            mLockAfter.setEntries(
                    revisedEntries.toArray(new CharSequence[revisedEntries.size()]));
            mLockAfter.setEntryValues(
                    revisedValues.toArray(new CharSequence[revisedValues.size()]));
            final int userPreference = Integer.valueOf(mLockAfter.getValue());
            if (userPreference <= maxTimeout) {
                mLockAfter.setValue(String.valueOf(userPreference));
            } else {
                // There will be no highlighted selection since nothing in the list matches
                // maxTimeout. The user can still select anything less than maxTimeout.
                // TODO: maybe append maxTimeout to the list and mark selected.
            }
        }
        mLockAfter.setEnabled(revisedEntries.size() > 0);
    }

    @Override
    public void onResume() {
        super.onResume();

        // Make sure we reload the preference hierarchy since some of these settings
        // depend on others...
        createPreferenceHierarchy();

        final LockPatternUtils lockPatternUtils = mChooseLockSettingsHelper.utils();
        if (mBiometricWeakLiveliness != null) {
            mBiometricWeakLiveliness.setChecked(
                    lockPatternUtils.isBiometricWeakLivelinessEnabled());
        }
        if (mVisiblePattern != null) {
            mVisiblePattern.setChecked(lockPatternUtils.isVisiblePatternEnabled());
        }
        if (mTactileFeedback != null) {
            mTactileFeedback.setChecked(lockPatternUtils.isTactileFeedbackEnabled());
        }
        if (mPowerButtonInstantlyLocks != null) {
            mPowerButtonInstantlyLocks.setChecked(lockPatternUtils.getPowerButtonInstantlyLocks());
        }

        if (mShowPassword != null) {
            mShowPassword.setChecked(Settings.System.getInt(getContentResolver(),
                    Settings.System.TEXT_SHOW_PASSWORD, 1) != 0);
        }

        KeyStore.State state = KeyStore.getInstance().state();
        if (mResetCredentials != null) {
            mResetCredentials.setEnabled(state != KeyStore.State.UNINITIALIZED);
        }
    }

    void startActivityForResultSafely(Intent intent, int requestCode) {
        try {
            startActivityForResult(intent, requestCode);
        } catch (ActivityNotFoundException e) {
            Toast.makeText(getActivity(), R.string.activity_not_found, Toast.LENGTH_SHORT).show();
        } catch (SecurityException e) {
            Toast.makeText(getActivity(), R.string.activity_not_found, Toast.LENGTH_SHORT).show();
            Log.e(TAG, "Settings does not have the permission to launch " + intent, e);
        }
    }

    private void launchPickActivityIntent(int featuresFilter, int defaultLabelId, int defaultIconId,
            ComponentName defaultComponentName, String defaultTag) {
        // Create intent to pick widget
        Intent pickIntent = new Intent(AppWidgetManager.ACTION_APPWIDGET_PICK);

        // Found in KeyguardHostView.java
        final int KEYGUARD_HOST_ID = 0x4B455947;
        int appWidgetId = AppWidgetHost.allocateAppWidgetIdForSystem(KEYGUARD_HOST_ID);
        if (appWidgetId != -1) {
            pickIntent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);
            pickIntent.putExtra(AppWidgetManager.EXTRA_CUSTOM_SORT, false);
            pickIntent.putExtra(AppWidgetManager.EXTRA_CATEGORY_FILTER,
                    AppWidgetProviderInfo.WIDGET_CATEGORY_KEYGUARD);
            if (featuresFilter != AppWidgetProviderInfo.WIDGET_FEATURES_NONE) {
                pickIntent.putExtra(AppWidgetManager.EXTRA_FEATURES_FILTER, featuresFilter);
            }

            // Add an entry for "none" to let someone select no widget
            AppWidgetProviderInfo defaultInfo = new AppWidgetProviderInfo();
            ArrayList<AppWidgetProviderInfo> extraInfos = new ArrayList<AppWidgetProviderInfo>();
            defaultInfo.label = getResources().getString(defaultLabelId);
            defaultInfo.icon = defaultIconId;
            defaultInfo.provider = defaultComponentName;
            extraInfos.add(defaultInfo);

            ArrayList<Bundle> extraExtras = new ArrayList<Bundle>();
            Bundle b = new Bundle();
            b.putBoolean(defaultTag, true);
            extraExtras.add(b);

            // Launch the widget picker
            pickIntent.putExtra(AppWidgetManager.EXTRA_CUSTOM_INFO, extraInfos);
            pickIntent.putExtra(AppWidgetManager.EXTRA_CUSTOM_EXTRAS, extraExtras);
            pickIntent.putExtra(Intent.EXTRA_INTENT, getBaseIntent());
            startActivityForResult(pickIntent, REQUEST_PICK_USER_SELECTED_APPWIDGET);
        } else {
            Log.e(TAG, "Unable to allocate an AppWidget id in lock screen");
        }
    }

    private Intent getBaseIntent() {
        Intent baseIntent = new Intent(Intent.ACTION_MAIN, null);
        baseIntent.addCategory(Intent.CATEGORY_DEFAULT);

        Bundle options = new Bundle();
        options.putInt(AppWidgetManager.OPTION_APPWIDGET_HOST_CATEGORY,
                AppWidgetProviderInfo.WIDGET_CATEGORY_KEYGUARD);
        baseIntent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_OPTIONS, options);
        return baseIntent;
    }

    @Override
    public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen, Preference preference) {
        final String key = preference.getKey();

        final LockPatternUtils lockPatternUtils = mChooseLockSettingsHelper.utils();
        if (KEY_UNLOCK_SET_OR_CHANGE.equals(key)) {
            startFragment(this, "com.android.settings.ChooseLockGeneric$ChooseLockGenericFragment",
                    SET_OR_CHANGE_LOCK_METHOD_REQUEST, null);
        } else if (KEY_CHOOSE_USER_SELECTED_LOCKSCREEN_WIDGET.equals(key)) {
            launchPickActivityIntent(AppWidgetProviderInfo.WIDGET_FEATURES_NONE,
                    R.string.widget_none, 0, new ComponentName("", ""), EXTRA_NO_WIDGET);
        } else if (KEY_CHOOSE_LOCKSCREEN_STATUS_WIDGET.equals(key)) {
            int defaultIconId;
            ComponentName clock = new ComponentName(
                    "com.google.android.deskclock", "com.android.deskclock.DeskClock");
            try {
                defaultIconId = getActivity().getPackageManager().getActivityInfo(clock, 0).icon;
            } catch (PackageManager.NameNotFoundException e) {
                defaultIconId = 0;
            }
            launchPickActivityIntent(AppWidgetProviderInfo.WIDGET_FEATURES_STATUS,
                    R.string.widget_default, defaultIconId, clock, EXTRA_DEFAULT_WIDGET);
        } else if (KEY_BIOMETRIC_WEAK_IMPROVE_MATCHING.equals(key)) {
            ChooseLockSettingsHelper helper =
                    new ChooseLockSettingsHelper(this.getActivity(), this);
            if (!helper.launchConfirmationActivity(
                    CONFIRM_EXISTING_FOR_BIOMETRIC_WEAK_IMPROVE_REQUEST, null, null)) {
                // If this returns false, it means no password confirmation is required, so
                // go ahead and start improve.
                // Note: currently a backup is required for biometric_weak so this code path
                // can't be reached, but is here in case things change in the future
                startBiometricWeakImprove();
            }
        } else if (KEY_BIOMETRIC_WEAK_LIVELINESS.equals(key)) {
            if (isToggled(preference)) {
                lockPatternUtils.setBiometricWeakLivelinessEnabled(true);
            } else {
                // In this case the user has just unchecked the checkbox, but this action requires
                // them to confirm their password.  We need to re-check the checkbox until
                // they've confirmed their password
                mBiometricWeakLiveliness.setChecked(true);
                ChooseLockSettingsHelper helper =
                        new ChooseLockSettingsHelper(this.getActivity(), this);
                if (!helper.launchConfirmationActivity(
                                CONFIRM_EXISTING_FOR_BIOMETRIC_WEAK_LIVELINESS_OFF, null, null)) {
                    // If this returns false, it means no password confirmation is required, so
                    // go ahead and uncheck it here.
                    // Note: currently a backup is required for biometric_weak so this code path
                    // can't be reached, but is here in case things change in the future
                    lockPatternUtils.setBiometricWeakLivelinessEnabled(false);
                    mBiometricWeakLiveliness.setChecked(false);
                }
            }
        } else if (KEY_LOCK_ENABLED.equals(key)) {
            lockPatternUtils.setLockPatternEnabled(isToggled(preference));
        } else if (KEY_VISIBLE_PATTERN.equals(key)) {
            lockPatternUtils.setVisiblePatternEnabled(isToggled(preference));
        } else if (KEY_TACTILE_FEEDBACK_ENABLED.equals(key)) {
            lockPatternUtils.setTactileFeedbackEnabled(isToggled(preference));
        } else if (KEY_POWER_INSTANTLY_LOCKS.equals(key)) {
            lockPatternUtils.setPowerButtonInstantlyLocks(isToggled(preference));
        } else if (preference == mShowPassword) {
            Settings.System.putInt(getContentResolver(), Settings.System.TEXT_SHOW_PASSWORD,
                    mShowPassword.isChecked() ? 1 : 0);
        } else if (preference == mToggleAppInstallation) {
            if (mToggleAppInstallation.isChecked()) {
                mToggleAppInstallation.setChecked(false);
                warnAppInstallation();
            } else {
                setNonMarketAppsAllowed(false);
            }
        } else if (KEY_TOGGLE_VERIFY_APPLICATIONS.equals(key)) {
            Settings.Global.putInt(getContentResolver(), Settings.Global.PACKAGE_VERIFIER_ENABLE,
                    mToggleVerifyApps.isChecked() ? 1 : 0);
        } else {
            // If we didn't handle it, let preferences handle it.
            return super.onPreferenceTreeClick(preferenceScreen, preference);
        }

        return true;
    }

    private boolean isToggled(Preference pref) {
        return ((CheckBoxPreference) pref).isChecked();
    }

    /**
     * see confirmPatternThenDisableAndClear
     */
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == CONFIRM_EXISTING_FOR_BIOMETRIC_WEAK_IMPROVE_REQUEST &&
                resultCode == Activity.RESULT_OK) {
            startBiometricWeakImprove();
            return;
        } else if (requestCode == CONFIRM_EXISTING_FOR_BIOMETRIC_WEAK_LIVELINESS_OFF &&
                resultCode == Activity.RESULT_OK) {
            final LockPatternUtils lockPatternUtils = mChooseLockSettingsHelper.utils();
            lockPatternUtils.setBiometricWeakLivelinessEnabled(false);
            // Setting the mBiometricWeakLiveliness checked value to false is handled when onResume
            // is called by grabbing the value from lockPatternUtils.  We can't set it here
            // because mBiometricWeakLiveliness could be null
            return;
        } else if (requestCode == REQUEST_PICK_USER_SELECTED_APPWIDGET ||
                requestCode == REQUEST_PICK_STATUS_APPWIDGET ||
                requestCode == REQUEST_CREATE_USER_SELECTED_APPWIDGET ||
                requestCode == REQUEST_CREATE_STATUS_APPWIDGET) {
            int appWidgetId = (data == null) ? -1 : data.getIntExtra(
                    AppWidgetManager.EXTRA_APPWIDGET_ID, -1);
            if ((requestCode == REQUEST_PICK_USER_SELECTED_APPWIDGET ||
                 requestCode == REQUEST_PICK_STATUS_APPWIDGET) &&
                resultCode == Activity.RESULT_OK) {
                AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(getActivity());
                boolean defaultOrNoWidget = data.getBooleanExtra(EXTRA_NO_WIDGET, false) ||
                        data.getBooleanExtra(EXTRA_DEFAULT_WIDGET, false);

                AppWidgetProviderInfo appWidget = null;
                if (!defaultOrNoWidget) {
                    appWidget = appWidgetManager.getAppWidgetInfo(appWidgetId);
                }

                int newRequestCode = requestCode == REQUEST_PICK_USER_SELECTED_APPWIDGET ?
                    REQUEST_CREATE_USER_SELECTED_APPWIDGET :
                    REQUEST_CREATE_STATUS_APPWIDGET;
                if (!defaultOrNoWidget && appWidget.configure != null) {
                    // Launch over to configure widget, if needed
                    Intent intent = new Intent(AppWidgetManager.ACTION_APPWIDGET_CONFIGURE);
                    intent.setComponent(appWidget.configure);
                    intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);

                    startActivityForResultSafely(intent, newRequestCode);
                } else {
                    // Otherwise just add it
                    if (defaultOrNoWidget) {
                        // If we selected "none", delete the allocated id
                        AppWidgetHost.deleteAppWidgetIdForSystem(appWidgetId);
                        data.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, -1);
                    }
                    onActivityResult(newRequestCode, Activity.RESULT_OK, data);
                }
            } else if ((requestCode == REQUEST_CREATE_USER_SELECTED_APPWIDGET ||
                        requestCode == REQUEST_CREATE_STATUS_APPWIDGET) &&
                       resultCode == Activity.RESULT_OK) {
                // If a widget existed before, delete it
                int oldAppWidgetId = requestCode == REQUEST_CREATE_USER_SELECTED_APPWIDGET ?
                        getUserSelectedAppWidgetId() : getStatusAppWidgetId();
                if (oldAppWidgetId != -1) {
                    AppWidgetHost.deleteAppWidgetIdForSystem(oldAppWidgetId);
                }
                Settings.Secure.putString(getContentResolver(),
                        (requestCode == REQUEST_CREATE_USER_SELECTED_APPWIDGET ?
                                Settings.Secure.LOCK_SCREEN_USER_SELECTED_APPWIDGET_ID :
                                Settings.Secure.LOCK_SCREEN_STATUS_APPWIDGET_ID),
                        Integer.toString(appWidgetId));
            } else {
                AppWidgetHost.deleteAppWidgetIdForSystem(appWidgetId);
            }
        }
        createPreferenceHierarchy();
    }

    public boolean onPreferenceChange(Preference preference, Object value) {
        if (preference == mLockAfter) {
            int timeout = Integer.parseInt((String) value);
            try {
                Settings.Secure.putInt(getContentResolver(),
                        Settings.Secure.LOCK_SCREEN_LOCK_AFTER_TIMEOUT, timeout);
            } catch (NumberFormatException e) {
                Log.e("SecuritySettings", "could not persist lockAfter timeout setting", e);
            }
            updateLockAfterPreferenceSummary();
        }
        return true;
    }

    public void startBiometricWeakImprove(){
        Intent intent = new Intent();
        intent.setClassName("com.android.facelock", "com.android.facelock.AddToSetup");
        startActivity(intent);
    }
}
