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

import com.android.internal.widget.LockPatternUtils;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.admin.DevicePolicyManager;
import android.content.ContentQueryMap;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.IBinder;
import android.os.ServiceManager;
import android.os.SystemProperties;
import android.os.Vibrator;
import android.os.storage.IMountService;
import android.preference.CheckBoxPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.PreferenceCategory;
import android.preference.PreferenceGroup;
import android.preference.PreferenceManager;
import android.preference.PreferenceScreen;
import android.provider.Settings;
import android.security.Credentials;
import android.security.KeyStore;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Observable;
import java.util.Observer;

/**
 * Gesture lock pattern settings.
 */
public class SecuritySettings extends SettingsPreferenceFragment
        implements OnPreferenceChangeListener {
    private static final String KEY_UNLOCK_SET_OR_CHANGE = "unlock_set_or_change";

    private static final String KEY_ENCRYPTION = "encryption";

    // Lock Settings
    private static final String PACKAGE = "com.android.settings";
    private static final String ICC_LOCK_SETTINGS = PACKAGE + ".IccLockSettings";

    private static final String KEY_LOCK_ENABLED = "lockenabled";
    private static final String KEY_VISIBLE_PATTERN = "visiblepattern";
    private static final String KEY_TACTILE_FEEDBACK_ENABLED = "unlock_tactile_feedback";
    private static final String KEY_SECURITY_CATEGORY = "security_category";

    private CheckBoxPreference mVisiblePattern;
    private CheckBoxPreference mTactileFeedback;

    private CheckBoxPreference mShowPassword;

    // Location Settings
    private static final String LOCATION_CATEGORY = "location_category";
    private static final String LOCATION_NETWORK = "location_network";
    private static final String LOCATION_GPS = "location_gps";
    private static final String ASSISTED_GPS = "assisted_gps";
    private static final String USE_LOCATION = "location_use_for_services";
    private static final String LOCK_AFTER_TIMEOUT_KEY = "lock_after_timeout";
    private static final int SET_OR_CHANGE_LOCK_METHOD_REQUEST = 123;
    private static final int FALLBACK_LOCK_AFTER_TIMEOUT_VALUE = 5000; // compatible with pre-Froyo

    private static final String TAG = "SecuritySettings";

    // Credential storage
    private final CredentialStorage mCredentialStorage = new CredentialStorage();

    private CheckBoxPreference mNetwork;
    private CheckBoxPreference mGps;
    private CheckBoxPreference mAssistedGps;
    private CheckBoxPreference mUseLocation;

    DevicePolicyManager mDPM;

    // These provide support for receiving notification when Location Manager settings change.
    // This is necessary because the Network Location Provider can change settings
    // if the user does not confirm enabling the provider.
    private ContentQueryMap mContentQueryMap;

    private ChooseLockSettingsHelper mChooseLockSettingsHelper;
    private LockPatternUtils mLockPatternUtils;
    private ListPreference mLockAfter;

    private SettingsObserver mSettingsObserver;

    private final class SettingsObserver implements Observer {
        public void update(Observable o, Object arg) {
            updateToggles();
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mLockPatternUtils = new LockPatternUtils(getActivity());

        mDPM = (DevicePolicyManager)getSystemService(Context.DEVICE_POLICY_SERVICE);

        mChooseLockSettingsHelper = new ChooseLockSettingsHelper(getActivity());

        createPreferenceHierarchy();

        updateToggles();
    }

    @Override
    public void onStart() {
        super.onStart();
        // listen for Location Manager settings changes
        Cursor settingsCursor = getContentResolver().query(Settings.Secure.CONTENT_URI, null,
                "(" + Settings.System.NAME + "=?)",
                new String[]{Settings.Secure.LOCATION_PROVIDERS_ALLOWED},
                null);
        mContentQueryMap = new ContentQueryMap(settingsCursor, Settings.System.NAME, true, null);
        mContentQueryMap.addObserver(mSettingsObserver = new SettingsObserver());
    }

    @Override
    public void onStop() {
        super.onStop();
        mContentQueryMap.deleteObserver(mSettingsObserver);
    }

    private PreferenceScreen createPreferenceHierarchy() {
        PreferenceScreen root = this.getPreferenceScreen();
        if (root != null) {
            root.removeAll();
        }
        addPreferencesFromResource(R.xml.security_settings);
        root = this.getPreferenceScreen();

        mNetwork = (CheckBoxPreference) getPreferenceScreen().findPreference(LOCATION_NETWORK);
        mGps = (CheckBoxPreference) getPreferenceScreen().findPreference(LOCATION_GPS);
        mAssistedGps = (CheckBoxPreference) getPreferenceScreen().findPreference(ASSISTED_GPS);
        if (GoogleLocationSettingHelper.isAvailable(getActivity())) {
            // GSF present, Add setting for 'Use My Location'
            PreferenceGroup locationCat = (PreferenceGroup) root.findPreference(LOCATION_CATEGORY);
            CheckBoxPreference useLocation = new CheckBoxPreference(getActivity());
            useLocation.setKey(USE_LOCATION);
            useLocation.setTitle(R.string.use_location_title);
            useLocation.setSummaryOn(R.string.use_location_summary_enabled);
            useLocation.setSummaryOff(R.string.use_location_summary_disabled);
            useLocation.setChecked(
                    GoogleLocationSettingHelper.getUseLocationForServices(getActivity())
                    == GoogleLocationSettingHelper.USE_LOCATION_FOR_SERVICES_ON);
            useLocation.setPersistent(false);
            useLocation.setOnPreferenceChangeListener(this);
            locationCat.addPreference(useLocation);
            mUseLocation = useLocation;
        }

        PreferenceManager pm = getPreferenceManager();
        
        // Add options for device encryption
        // TODO: It still needs to be determined how a device specifies that it supports
        // encryption. That mechanism needs to be checked before adding the following code
        
        addPreferencesFromResource(R.xml.security_settings_encryption);

        // Add options for lock/unlock screen
        int resid = 0;
        if (!mLockPatternUtils.isSecure()) {
            if (mLockPatternUtils.isLockScreenDisabled()) {
                resid = R.xml.security_settings_lockscreen;
            } else {
                resid = R.xml.security_settings_chooser;
            }
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

        // lock after preference
        mLockAfter = setupLockAfterPreference(pm);
        updateLockAfterPreferenceSummary();

        // visible pattern
        mVisiblePattern = (CheckBoxPreference) pm.findPreference(KEY_VISIBLE_PATTERN);

        // tactile feedback. Should be common to all unlock preference screens.
        mTactileFeedback = (CheckBoxPreference) pm.findPreference(KEY_TACTILE_FEEDBACK_ENABLED);
        if (!((Vibrator) getSystemService(Context.VIBRATOR_SERVICE)).hasVibrator()) {
            PreferenceGroup securityCategory = (PreferenceGroup)
                    pm.findPreference(KEY_SECURITY_CATEGORY);
            if (securityCategory != null && mTactileFeedback != null) {
                securityCategory.removePreference(mTactileFeedback);
            }
        }

        int activePhoneType = TelephonyManager.getDefault().getPhoneType();

        // do not display SIM lock for CDMA phone
        if (TelephonyManager.PHONE_TYPE_CDMA != activePhoneType)
        {
            PreferenceScreen simLockPreferences = getPreferenceManager()
                    .createPreferenceScreen(getActivity());
            simLockPreferences.setTitle(R.string.sim_lock_settings_category);
            // Intent to launch SIM lock settings
            simLockPreferences.setIntent(new Intent().setClassName(PACKAGE, ICC_LOCK_SETTINGS));
            PreferenceCategory simLockCat = new PreferenceCategory(getActivity());
            simLockCat.setTitle(R.string.sim_lock_settings_title);
            root.addPreference(simLockCat);
            simLockCat.addPreference(simLockPreferences);
        }

        // Passwords
        PreferenceCategory passwordsCat = new PreferenceCategory(getActivity());
        passwordsCat.setTitle(R.string.security_passwords_title);
        root.addPreference(passwordsCat);

        CheckBoxPreference showPassword = mShowPassword = new CheckBoxPreference(getActivity());
        showPassword.setKey("show_password");
        showPassword.setTitle(R.string.show_password);
        showPassword.setSummary(R.string.show_password_summary);
        showPassword.setPersistent(false);
        passwordsCat.addPreference(showPassword);

        // Device policies
        PreferenceCategory devicePoliciesCat = new PreferenceCategory(getActivity());
        devicePoliciesCat.setTitle(R.string.device_admin_title);
        root.addPreference(devicePoliciesCat);

        Preference deviceAdminButton = new Preference(getActivity());
        deviceAdminButton.setTitle(R.string.manage_device_admin);
        deviceAdminButton.setSummary(R.string.manage_device_admin_summary);
        Intent deviceAdminIntent = new Intent();
        deviceAdminIntent.setClass(getActivity(), DeviceAdminSettings.class);
        deviceAdminButton.setIntent(deviceAdminIntent);
        devicePoliciesCat.addPreference(deviceAdminButton);

        // Credential storage
        PreferenceCategory credentialsCat = new PreferenceCategory(getActivity());
        credentialsCat.setTitle(R.string.credentials_category);
        root.addPreference(credentialsCat);
        mCredentialStorage.createPreferences(credentialsCat);

        return root;
    }

    private ListPreference setupLockAfterPreference(PreferenceManager pm) {
        ListPreference result = (ListPreference) pm.findPreference(LOCK_AFTER_TIMEOUT_KEY);
        if (result != null) {
            int lockAfterValue = Settings.Secure.getInt(getContentResolver(),
                    Settings.Secure.LOCK_SCREEN_LOCK_AFTER_TIMEOUT,
                    FALLBACK_LOCK_AFTER_TIMEOUT_VALUE);
            result.setValue(String.valueOf(lockAfterValue));
            result.setOnPreferenceChangeListener(this);
            final long adminTimeout = mDPM != null ? mDPM.getMaximumTimeToLock(null) : 0;
            final ContentResolver cr = getContentResolver();
            final long displayTimeout = Math.max(0,
                    Settings.System.getInt(cr, SCREEN_OFF_TIMEOUT, 0));
            if (adminTimeout > 0) {
                // This setting is a slave to display timeout when a device policy is enforced.
                // As such, maxLockTimeout = adminTimeout - displayTimeout.
                // If there isn't enough time, shows "immediately" setting.
                disableUnusableTimeouts(result, Math.max(0, adminTimeout - displayTimeout));
            }
        }
        return result;
    }

    private void updateLockAfterPreferenceSummary() {
        // Not all security types have a "lock after" preference, so ignore those that don't.
        if (mLockAfter == null) return;

        // Update summary message with current value
        long currentTimeout = Settings.Secure.getLong(getContentResolver(),
                Settings.Secure.LOCK_SCREEN_LOCK_AFTER_TIMEOUT, 0);
        final CharSequence[] entries = mLockAfter.getEntries();
        final CharSequence[] values = mLockAfter.getEntryValues();
        int best = 0;
        for (int i = 0; i < values.length; i++) {
            long timeout = Long.valueOf(values[i].toString());
            if (currentTimeout >= timeout) {
                best = i;
            }
        }
        String summary = mLockAfter.getContext()
                .getString(R.string.lock_after_timeout_summary, entries[best]);
        mLockAfter.setSummary(summary);
    }

    private static void disableUnusableTimeouts(ListPreference pref, long maxTimeout) {
        final CharSequence[] entries = pref.getEntries();
        final CharSequence[] values = pref.getEntryValues();
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
            pref.setEntries(
                    revisedEntries.toArray(new CharSequence[revisedEntries.size()]));
            pref.setEntryValues(
                    revisedValues.toArray(new CharSequence[revisedValues.size()]));
            final int userPreference = Integer.valueOf(pref.getValue());
            if (userPreference <= maxTimeout) {
                pref.setValue(String.valueOf(userPreference));
            } else {
                // There will be no highlighted selection since nothing in the list matches
                // maxTimeout. The user can still select anything less than maxTimeout.
                // TODO: maybe append maxTimeout to the list and mark selected.
            }
        }
        pref.setEnabled(revisedEntries.size() > 0);
    }

    @Override
    public void onResume() {
        super.onResume();

        // Make sure we reload the preference hierarchy since some of these settings
        // depend on others...
        createPreferenceHierarchy();

        final LockPatternUtils lockPatternUtils = mChooseLockSettingsHelper.utils();
        if (mVisiblePattern != null) {
            mVisiblePattern.setChecked(lockPatternUtils.isVisiblePatternEnabled());
        }
        if (mTactileFeedback != null) {
            mTactileFeedback.setChecked(lockPatternUtils.isTactileFeedbackEnabled());
        }

        mShowPassword.setChecked(Settings.System.getInt(getContentResolver(),
                Settings.System.TEXT_SHOW_PASSWORD, 1) != 0);

        mCredentialStorage.resume();
    }

    @Override
    public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen,
            Preference preference) {
        final String key = preference.getKey();

        final LockPatternUtils lockPatternUtils = mChooseLockSettingsHelper.utils();
        if (KEY_UNLOCK_SET_OR_CHANGE.equals(key)) {
            startFragment(this, "com.android.settings.ChooseLockGeneric$ChooseLockGenericFragment",
                    SET_OR_CHANGE_LOCK_METHOD_REQUEST, null);
        } else if (KEY_LOCK_ENABLED.equals(key)) {
            lockPatternUtils.setLockPatternEnabled(isToggled(preference));
        } else if (KEY_VISIBLE_PATTERN.equals(key)) {
            lockPatternUtils.setVisiblePatternEnabled(isToggled(preference));
        } else if (KEY_TACTILE_FEEDBACK_ENABLED.equals(key)) {
            lockPatternUtils.setTactileFeedbackEnabled(isToggled(preference));
        } else if (preference == mShowPassword) {
            Settings.System.putInt(getContentResolver(), Settings.System.TEXT_SHOW_PASSWORD,
                    mShowPassword.isChecked() ? 1 : 0);
        } else if (preference == mNetwork) {
            Settings.Secure.setLocationProviderEnabled(getContentResolver(),
                    LocationManager.NETWORK_PROVIDER, mNetwork.isChecked());
        } else if (preference == mGps) {
            boolean enabled = mGps.isChecked();
            Settings.Secure.setLocationProviderEnabled(getContentResolver(),
                    LocationManager.GPS_PROVIDER, enabled);
            if (mAssistedGps != null) {
                mAssistedGps.setEnabled(enabled);
            }
        } else if (preference == mAssistedGps) {
            Settings.Secure.putInt(getContentResolver(), Settings.Secure.ASSISTED_GPS_ENABLED,
                    mAssistedGps.isChecked() ? 1 : 0);
        } else {
            // If we didn't handle it, let preferences handle it.
            return super.onPreferenceTreeClick(preferenceScreen, preference);
        }

        return true;
    }

    /*
     * Creates toggles for each available location provider
     */
    private void updateToggles() {
        ContentResolver res = getContentResolver();
        boolean gpsEnabled = Settings.Secure.isLocationProviderEnabled(
                res, LocationManager.GPS_PROVIDER);
        mNetwork.setChecked(Settings.Secure.isLocationProviderEnabled(
                res, LocationManager.NETWORK_PROVIDER));
        mGps.setChecked(gpsEnabled);
        if (mAssistedGps != null) {
            mAssistedGps.setChecked(Settings.Secure.getInt(res,
                    Settings.Secure.ASSISTED_GPS_ENABLED, 2) == 1);
            mAssistedGps.setEnabled(gpsEnabled);
        }
    }

    private boolean isToggled(Preference pref) {
        return ((CheckBoxPreference) pref).isChecked();
    }

    /**
     * @see #confirmPatternThenDisableAndClear
     */
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        createPreferenceHierarchy();
    }

    private class Encryption implements DialogInterface.OnClickListener,
            DialogInterface.OnDismissListener {

        private boolean mSubmit;

        public void showPasswordDialog() {
            View view = View.inflate(SecuritySettings.this.getActivity(),
                    R.layout.credentials_password_dialog, null);

            Dialog dialog = new AlertDialog.Builder(SecuritySettings.this.getActivity())
                    .setView(view).setTitle(R.string.credentials_set_password)
                    .setPositiveButton(android.R.string.ok, this)
                    .setNegativeButton(android.R.string.cancel, this).create();
            dialog.setOnDismissListener(this);
            dialog.show();
        }

        public void onClick(DialogInterface dialog, int button) {
            if (button == DialogInterface.BUTTON_POSITIVE) {
                mSubmit = true;
            }
        }

        public void onDismiss(DialogInterface dialog) {
            if (mSubmit) {
                mSubmit = false;
                if (!checkPassword((Dialog) dialog)) {
                    ((Dialog) dialog).show();
                    return;
                }
            }
        }

        // Return true if there is no error.
        private boolean checkPassword(Dialog dialog) {
            String newPassword = getText(dialog, R.id.new_password);
            String confirmPassword = getText(dialog, R.id.confirm_password);

            if (newPassword == null || confirmPassword == null || newPassword.length() == 0
                    || confirmPassword.length() == 0) {
                showError(dialog, R.string.credentials_passwords_empty);
            } else if (!newPassword.equals(confirmPassword)) {
                showError(dialog, R.string.credentials_passwords_mismatch);
            } else {

                IBinder service = ServiceManager.getService("mount");
                if (service == null) {
                    return false;
                }

                IMountService mountService = IMountService.Stub.asInterface(service);
                try {
                    mountService.encryptStorage(newPassword);
                } catch (Exception e) {
                    Log.e(TAG, "Error while encrypting...", e);
                }

                return true;
            }

            return false;
        }

        private String getText(Dialog dialog, int viewId) {
            TextView view = (TextView) dialog.findViewById(viewId);
            return (view == null || view.getVisibility() == View.GONE) ? null : view.getText()
                    .toString();
        }

        private void showError(Dialog dialog, int stringId, Object... formatArgs) {
            TextView view = (TextView) dialog.findViewById(R.id.error);
            if (view != null) {
                if (formatArgs == null || formatArgs.length == 0) {
                    view.setText(stringId);
                } else {
                    view.setText(dialog.getContext().getString(stringId, formatArgs));
                }
                view.setVisibility(View.VISIBLE);
            }
        }

    }

    private class CredentialStorage implements DialogInterface.OnClickListener,
            DialogInterface.OnDismissListener, Preference.OnPreferenceChangeListener,
            Preference.OnPreferenceClickListener {
        private static final int MINIMUM_PASSWORD_LENGTH = 8;

        private final KeyStore mKeyStore = KeyStore.getInstance();
        private int mState;
        private boolean mSubmit = false;
        private boolean mExternal = false;

        private CheckBoxPreference mAccessCheckBox;
        private Preference mInstallButton;
        private Preference mPasswordButton;
        private Preference mResetButton;

        void resume() {
            mState = mKeyStore.test();
            updatePreferences(mState);

            Intent intent = getActivity().getIntent();
            if (!mExternal && intent != null &&
                    Credentials.UNLOCK_ACTION.equals(intent.getAction())) {
                mExternal = true;
                if (mState == KeyStore.UNINITIALIZED) {
                    showPasswordDialog();
                } else if (mState == KeyStore.LOCKED) {
                    showUnlockDialog();
                } else {
                    // TODO: Verify if this is the right way
                    SecuritySettings.this.getFragmentManager().popBackStack();
                }
            }
        }

        private void initialize(String password) {
            mKeyStore.password(password);
            updatePreferences(KeyStore.NO_ERROR);
        }

        private void reset() {
            mKeyStore.reset();
            updatePreferences(KeyStore.UNINITIALIZED);
        }

        private void lock() {
            mKeyStore.lock();
            updatePreferences(KeyStore.LOCKED);
        }

        private int unlock(String password) {
            mKeyStore.unlock(password);
            return mKeyStore.getLastError();
        }

        private int changePassword(String oldPassword, String newPassword) {
            mKeyStore.password(oldPassword, newPassword);
            return mKeyStore.getLastError();
        }

        public boolean onPreferenceChange(Preference preference, Object value) {
            if (preference == mAccessCheckBox) {
                if ((Boolean) value) {
                    showUnlockDialog();
                } else {
                    lock();
                }
                return true;
            }
            return false;
        }

        public boolean onPreferenceClick(Preference preference) {
            if (preference == mInstallButton) {
                Credentials.getInstance().installFromSdCard(SecuritySettings.this.getActivity());
            } else if (preference == mPasswordButton) {
                showPasswordDialog();
            } else if (preference == mResetButton) {
                showResetDialog();
            } else {
                return false;
            }
            return true;
        }

        public void onClick(DialogInterface dialog, int button) {
            mSubmit = (button == DialogInterface.BUTTON_POSITIVE);
            if (button == DialogInterface.BUTTON_NEUTRAL) {
                reset();
            }
        }

        public void onDismiss(DialogInterface dialog) {
            // TODO:
            //if (mSubmit && !isFinishing()) {

            if (mSubmit) {
                mSubmit = false;
                if (!checkPassword((Dialog) dialog)) {
                    ((Dialog) dialog).show();
                    return;
                }
            }
            updatePreferences(mState);
            if (mExternal) {
                // TODO:
                // finish();
            }
        }

        // Return true if there is no error.
        private boolean checkPassword(Dialog dialog) {
            String oldPassword = getText(dialog, R.id.old_password);
            String newPassword = getText(dialog, R.id.new_password);
            String confirmPassword = getText(dialog, R.id.confirm_password);

            if (oldPassword != null && oldPassword.length() == 0) {
                showError(dialog, R.string.credentials_password_empty);
                return false;
            } else if (newPassword == null) {
                return !checkError(dialog, unlock(oldPassword));
            } else if (newPassword.length() == 0 || confirmPassword.length() == 0) {
                showError(dialog, R.string.credentials_passwords_empty);
            } else if (newPassword.length() < MINIMUM_PASSWORD_LENGTH) {
                showError(dialog, R.string.credentials_password_too_short);
            } else if (!newPassword.equals(confirmPassword)) {
                showError(dialog, R.string.credentials_passwords_mismatch);
            } else if (oldPassword == null) {
                initialize(newPassword);
                return true;
            } else {
                return !checkError(dialog, changePassword(oldPassword, newPassword));
            }
            return false;
        }

        // Return false if there is no error.
        private boolean checkError(Dialog dialog, int error) {
            if (error == KeyStore.NO_ERROR) {
                updatePreferences(KeyStore.NO_ERROR);
                return false;
            }
            if (error == KeyStore.UNINITIALIZED) {
                updatePreferences(KeyStore.UNINITIALIZED);
                return false;
            }
            if (error < KeyStore.WRONG_PASSWORD) {
                return false;
            }
            int count = error - KeyStore.WRONG_PASSWORD + 1;
            if (count > 3) {
                showError(dialog, R.string.credentials_wrong_password);
            } else if (count == 1) {
                showError(dialog, R.string.credentials_reset_warning);
            } else {
                showError(dialog, R.string.credentials_reset_warning_plural, count);
            }
            return true;
        }

        private String getText(Dialog dialog, int viewId) {
            TextView view = (TextView) dialog.findViewById(viewId);
            return (view == null || view.getVisibility() == View.GONE) ? null :
                            view.getText().toString();
        }

        private void showError(Dialog dialog, int stringId, Object... formatArgs) {
            TextView view = (TextView) dialog.findViewById(R.id.error);
            if (view != null) {
                if (formatArgs == null || formatArgs.length == 0) {
                    view.setText(stringId);
                } else {
                    view.setText(dialog.getContext().getString(stringId, formatArgs));
                }
                view.setVisibility(View.VISIBLE);
            }
        }

        private void createPreferences(PreferenceCategory category) {
            mAccessCheckBox = new CheckBoxPreference(SecuritySettings.this.getActivity());
            mAccessCheckBox.setTitle(R.string.credentials_access);
            mAccessCheckBox.setSummary(R.string.credentials_access_summary);
            mAccessCheckBox.setOnPreferenceChangeListener(this);
            category.addPreference(mAccessCheckBox);

            mInstallButton = new Preference(SecuritySettings.this.getActivity());
            mInstallButton.setTitle(R.string.credentials_install_certificates);
            mInstallButton.setSummary(R.string.credentials_install_certificates_summary);
            mInstallButton.setOnPreferenceClickListener(this);
            category.addPreference(mInstallButton);

            mPasswordButton = new Preference(SecuritySettings.this.getActivity());
            mPasswordButton.setTitle(R.string.credentials_set_password);
            mPasswordButton.setSummary(R.string.credentials_set_password_summary);
            mPasswordButton.setOnPreferenceClickListener(this);
            category.addPreference(mPasswordButton);

            mResetButton = new Preference(SecuritySettings.this.getActivity());
            mResetButton.setTitle(R.string.credentials_reset);
            mResetButton.setSummary(R.string.credentials_reset_summary);
            mResetButton.setOnPreferenceClickListener(this);
            category.addPreference(mResetButton);
        }

        private void updatePreferences(int state) {
            mAccessCheckBox.setEnabled(state != KeyStore.UNINITIALIZED);
            mAccessCheckBox.setChecked(state == KeyStore.NO_ERROR);
            mResetButton.setEnabled(state != KeyStore.UNINITIALIZED);

            // Show a toast message if the state is changed.
            if (mState == state) {
                return;
            } else if (state == KeyStore.NO_ERROR) {
                Toast.makeText(SecuritySettings.this.getActivity(), R.string.credentials_enabled,
                        Toast.LENGTH_SHORT).show();
            } else if (state == KeyStore.UNINITIALIZED) {
                Toast.makeText(SecuritySettings.this.getActivity(), R.string.credentials_erased,
                        Toast.LENGTH_SHORT).show();
            } else if (state == KeyStore.LOCKED) {
                Toast.makeText(SecuritySettings.this.getActivity(), R.string.credentials_disabled,
                        Toast.LENGTH_SHORT).show();
            }
            mState = state;
        }

        private void showUnlockDialog() {
            View view = View.inflate(SecuritySettings.this.getActivity(),
                    R.layout.credentials_unlock_dialog, null);

            // Show extra hint only when the action comes from outside.
            if (mExternal) {
                view.findViewById(R.id.hint).setVisibility(View.VISIBLE);
            }

            Dialog dialog = new AlertDialog.Builder(SecuritySettings.this.getActivity())
                    .setView(view)
                    .setTitle(R.string.credentials_unlock)
                    .setPositiveButton(android.R.string.ok, this)
                    .setNegativeButton(android.R.string.cancel, this)
                    .create();
            dialog.setOnDismissListener(this);
            dialog.show();
        }

        private void showPasswordDialog() {
            View view = View.inflate(SecuritySettings.this.getActivity(),
                    R.layout.credentials_password_dialog, null);

            if (mState == KeyStore.UNINITIALIZED) {
                view.findViewById(R.id.hint).setVisibility(View.VISIBLE);
            } else {
                view.findViewById(R.id.old_password_prompt).setVisibility(View.VISIBLE);
                view.findViewById(R.id.old_password).setVisibility(View.VISIBLE);
            }

            Dialog dialog = new AlertDialog.Builder(SecuritySettings.this.getActivity())
                    .setView(view)
                    .setTitle(R.string.credentials_set_password)
                    .setPositiveButton(android.R.string.ok, this)
                    .setNegativeButton(android.R.string.cancel, this)
                    .create();
            dialog.setOnDismissListener(this);
            dialog.show();
        }

        private void showResetDialog() {
            new AlertDialog.Builder(SecuritySettings.this.getActivity())
                    .setTitle(android.R.string.dialog_alert_title)
                    .setIcon(android.R.drawable.ic_dialog_alert)
                    .setMessage(R.string.credentials_reset_hint)
                    .setNeutralButton(getResources().getString(android.R.string.ok), this)
                    .setNegativeButton(getResources().getString(android.R.string.cancel), this)
                    .create().show();
        }
    }

    public boolean onPreferenceChange(Preference preference, Object value) {
        if (preference == mLockAfter) {
            int lockAfter = Integer.parseInt((String) value);
            try {
                Settings.Secure.putInt(getContentResolver(),
                        Settings.Secure.LOCK_SCREEN_LOCK_AFTER_TIMEOUT, lockAfter);
            } catch (NumberFormatException e) {
                Log.e("SecuritySettings", "could not persist lockAfter timeout setting", e);
            }
            updateLockAfterPreferenceSummary();
        } else if (preference == mUseLocation) {
            boolean newValue = value == null ? false : (Boolean) value;
            GoogleLocationSettingHelper.setUseLocationForServices(getActivity(), newValue);
            // We don't want to change the value immediately here, since the user may click
            // disagree in the dialog that pops up. When the activity we just launched exits, this
            // activity will be restated and the new value re-read, so the checkbox will get its
            // new value then.
            return false;
        }
        return true;
    }
}
