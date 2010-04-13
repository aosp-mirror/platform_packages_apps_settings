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


import java.util.Observable;
import java.util.Observer;

import android.app.Activity;
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
import android.os.SystemProperties;
import android.preference.CheckBoxPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceCategory;
import android.preference.PreferenceManager;
import android.preference.PreferenceScreen;
import android.preference.Preference.OnPreferenceChangeListener;
import android.provider.Settings;
import android.security.Credentials;
import android.security.KeyStore;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.android.internal.widget.LockPatternUtils;

/**
 * Gesture lock pattern settings.
 */
public class SecuritySettings extends PreferenceActivity {
    private static final String KEY_UNLOCK_SET_OR_CHANGE = "unlock_set_or_change";

    // Lock Settings
    private static final String PACKAGE = "com.android.settings";
    private static final String ICC_LOCK_SETTINGS = PACKAGE + ".IccLockSettings";

    private static final String KEY_LOCK_ENABLED = "lockenabled";
    private static final String KEY_VISIBLE_PATTERN = "visiblepattern";
    private static final String KEY_TACTILE_FEEDBACK_ENABLED = "unlock_tactile_feedback";

    // Encrypted File Systems constants
    private static final String PROPERTY_EFS_ENABLED = "persist.security.efs.enabled";
    private static final String PROPERTY_EFS_TRANSITION = "persist.security.efs.trans";

    private CheckBoxPreference mVisiblePattern;
    private CheckBoxPreference mTactileFeedback;

    private CheckBoxPreference mShowPassword;

    // Location Settings
    private static final String LOCATION_NETWORK = "location_network";
    private static final String LOCATION_GPS = "location_gps";
    private static final String ASSISTED_GPS = "assisted_gps";
    private static final int SET_OR_CHANGE_LOCK_METHOD_REQUEST = 123;

    // Credential storage
    private CredentialStorage mCredentialStorage = new CredentialStorage();

    // Encrypted file system
    private  CheckBoxPreference mEncryptedFSEnabled;

    private CheckBoxPreference mNetwork;
    private CheckBoxPreference mGps;
    private CheckBoxPreference mAssistedGps;

    DevicePolicyManager mDPM;

    // These provide support for receiving notification when Location Manager settings change.
    // This is necessary because the Network Location Provider can change settings
    // if the user does not confirm enabling the provider.
    private ContentQueryMap mContentQueryMap;
    private ChooseLockSettingsHelper mChooseLockSettingsHelper;
    private LockPatternUtils mLockPatternUtils;
    private final class SettingsObserver implements Observer {
        public void update(Observable o, Object arg) {
            updateToggles();
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mLockPatternUtils = new LockPatternUtils(this);

        mDPM = (DevicePolicyManager)getSystemService(Context.DEVICE_POLICY_SERVICE);

        mChooseLockSettingsHelper = new ChooseLockSettingsHelper(this);

        createPreferenceHierarchy();

        updateToggles();

        // listen for Location Manager settings changes
        Cursor settingsCursor = getContentResolver().query(Settings.Secure.CONTENT_URI, null,
                "(" + Settings.System.NAME + "=?)",
                new String[]{Settings.Secure.LOCATION_PROVIDERS_ALLOWED},
                null);
        mContentQueryMap = new ContentQueryMap(settingsCursor, Settings.System.NAME, true, null);
        mContentQueryMap.addObserver(new SettingsObserver());
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

        PreferenceManager pm = getPreferenceManager();

        // Lock screen
        if (!mLockPatternUtils.isSecure()) {
            addPreferencesFromResource(R.xml.security_settings_chooser);
        } else {
            switch (mLockPatternUtils.getKeyguardStoredPasswordQuality()) {
                case DevicePolicyManager.PASSWORD_QUALITY_SOMETHING:
                    addPreferencesFromResource(R.xml.security_settings_pattern);
                    break;
                case DevicePolicyManager.PASSWORD_QUALITY_NUMERIC:
                    addPreferencesFromResource(R.xml.security_settings_pin);
                    break;
                case DevicePolicyManager.PASSWORD_QUALITY_ALPHABETIC:
                case DevicePolicyManager.PASSWORD_QUALITY_ALPHANUMERIC:
                    addPreferencesFromResource(R.xml.security_settings_password);
                    break;
            }
        }

        // set or change current. Should be common to all unlock preference screens
        // mSetOrChange = (PreferenceScreen) pm.findPreference(KEY_UNLOCK_SET_OR_CHANGE);

        // visible pattern
        mVisiblePattern = (CheckBoxPreference) pm.findPreference(KEY_VISIBLE_PATTERN);

        // tactile feedback. Should be common to all unlock preference screens.
        mTactileFeedback = (CheckBoxPreference) pm.findPreference(KEY_TACTILE_FEEDBACK_ENABLED);

        int activePhoneType = TelephonyManager.getDefault().getPhoneType();

        // do not display SIM lock for CDMA phone
        if (TelephonyManager.PHONE_TYPE_CDMA != activePhoneType)
        {
            PreferenceScreen simLockPreferences = getPreferenceManager()
                    .createPreferenceScreen(this);
            simLockPreferences.setTitle(R.string.sim_lock_settings_category);
            // Intent to launch SIM lock settings
            simLockPreferences.setIntent(new Intent().setClassName(PACKAGE, ICC_LOCK_SETTINGS));
            PreferenceCategory simLockCat = new PreferenceCategory(this);
            simLockCat.setTitle(R.string.sim_lock_settings_title);
            root.addPreference(simLockCat);
            simLockCat.addPreference(simLockPreferences);
        }

        // Passwords
        PreferenceCategory passwordsCat = new PreferenceCategory(this);
        passwordsCat.setTitle(R.string.security_passwords_title);
        root.addPreference(passwordsCat);

        CheckBoxPreference showPassword = mShowPassword = new CheckBoxPreference(this);
        showPassword.setKey("show_password");
        showPassword.setTitle(R.string.show_password);
        showPassword.setSummary(R.string.show_password_summary);
        showPassword.setPersistent(false);
        passwordsCat.addPreference(showPassword);

        // Device policies
        PreferenceCategory devicePoliciesCat = new PreferenceCategory(this);
        devicePoliciesCat.setTitle(R.string.device_admin_title);
        root.addPreference(devicePoliciesCat);

        Preference deviceAdminButton = new Preference(this);
        deviceAdminButton.setTitle(R.string.manage_device_admin);
        deviceAdminButton.setSummary(R.string.manage_device_admin_summary);
        Intent deviceAdminIntent = new Intent();
        deviceAdminIntent.setClass(this, DeviceAdminSettings.class);
        deviceAdminButton.setIntent(deviceAdminIntent);
        devicePoliciesCat.addPreference(deviceAdminButton);

        // Credential storage
        PreferenceCategory credentialsCat = new PreferenceCategory(this);
        credentialsCat.setTitle(R.string.credentials_category);
        root.addPreference(credentialsCat);
        mCredentialStorage.createPreferences(credentialsCat, CredentialStorage.TYPE_KEYSTORE);

        // File System Encryption
        PreferenceCategory encryptedfsCat = new PreferenceCategory(this);
        encryptedfsCat.setTitle(R.string.encrypted_fs_category);
        //root.addPreference(encryptedfsCat);
        mCredentialStorage.createPreferences(encryptedfsCat, CredentialStorage.TYPE_ENCRYPTEDFS);
        return root;
    }

    @Override
    protected void onResume() {
        super.onResume();

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
            Intent intent = new Intent(this, ChooseLockGeneric.class);
            startActivityForResult(intent, SET_OR_CHANGE_LOCK_METHOD_REQUEST);
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
        }

        return false;
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
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        createPreferenceHierarchy();
    }

    private class CredentialStorage implements DialogInterface.OnClickListener,
            DialogInterface.OnDismissListener, Preference.OnPreferenceChangeListener,
            Preference.OnPreferenceClickListener {
        private static final int MINIMUM_PASSWORD_LENGTH = 8;

        private static final int TYPE_KEYSTORE = 0;
        private static final int TYPE_ENCRYPTEDFS = 1;

        // Dialog identifiers
        private static final int DLG_BASE = 0;
        private static final int DLG_UNLOCK = DLG_BASE + 1;
        private static final int DLG_PASSWORD = DLG_UNLOCK + 1;
        private static final int DLG_RESET = DLG_PASSWORD + 1;
        private static final int DLG_ENABLE_EFS = DLG_RESET + 1;

        private KeyStore mKeyStore = KeyStore.getInstance();
        private int mState;
        private boolean mSubmit = false;
        private boolean mExternal = false;

        private boolean mWillEnableEncryptedFS;
        private int mShowingDialog = 0;

        // Key Store controls
        private CheckBoxPreference mAccessCheckBox;
        private Preference mInstallButton;
        private Preference mPasswordButton;
        private Preference mResetButton;


        // Encrypted file system controls
        private  CheckBoxPreference mEncryptedFSEnabled;

        void resume() {
            mState = mKeyStore.test();
            updatePreferences(mState);

            Intent intent = getIntent();
            if (!mExternal && intent != null &&
                    Credentials.UNLOCK_ACTION.equals(intent.getAction())) {
                mExternal = true;
                if (mState == KeyStore.UNINITIALIZED) {
                    showPasswordDialog();
                } else if (mState == KeyStore.LOCKED) {
                    showUnlockDialog();
                } else {
                    finish();
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
            } else if (preference == mEncryptedFSEnabled) {
                Boolean bval = (Boolean)value;
                mWillEnableEncryptedFS = bval.booleanValue();
                showSwitchEncryptedFSDialog();
            }
            return true;
        }

        public boolean onPreferenceClick(Preference preference) {
            if (preference == mInstallButton) {
                Credentials.getInstance().installFromSdCard(SecuritySettings.this);
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
            if (mShowingDialog != DLG_ENABLE_EFS) {
                mSubmit = (button == DialogInterface.BUTTON_POSITIVE);
                if (button == DialogInterface.BUTTON_NEUTRAL) {
                    reset();
                }
            } else {
                if (button == DialogInterface.BUTTON_POSITIVE) {
                    Intent intent = new Intent("android.intent.action.MASTER_CLEAR");
                    intent.putExtra("enableEFS", mWillEnableEncryptedFS);
                    sendBroadcast(intent);
                    updatePreferences(mState);
                } else if (button == DialogInterface.BUTTON_NEGATIVE) {
                    // Cancel action
                    Toast.makeText(SecuritySettings.this, R.string.encrypted_fs_cancel_confirm,
                            Toast.LENGTH_SHORT).show();
                    updatePreferences(mState);
                } else {
                    // Unknown - should not happen
                    return;
                }
            }
        }

        public void onDismiss(DialogInterface dialog) {
            if (mSubmit && !isFinishing()) {
                mSubmit = false;
                if (!checkPassword((Dialog) dialog)) {
                    ((Dialog) dialog).show();
                    return;
                }
            }
            updatePreferences(mState);
            if (mExternal) {
                finish();
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

        private void createPreferences(PreferenceCategory category, int type) {
            switch(type) {
            case TYPE_KEYSTORE:
                mAccessCheckBox = new CheckBoxPreference(SecuritySettings.this);
                mAccessCheckBox.setTitle(R.string.credentials_access);
                mAccessCheckBox.setSummary(R.string.credentials_access_summary);
                mAccessCheckBox.setOnPreferenceChangeListener(this);
                category.addPreference(mAccessCheckBox);

                mInstallButton = new Preference(SecuritySettings.this);
                mInstallButton.setTitle(R.string.credentials_install_certificates);
                mInstallButton.setSummary(R.string.credentials_install_certificates_summary);
                mInstallButton.setOnPreferenceClickListener(this);
                category.addPreference(mInstallButton);

                mPasswordButton = new Preference(SecuritySettings.this);
                mPasswordButton.setTitle(R.string.credentials_set_password);
                mPasswordButton.setSummary(R.string.credentials_set_password_summary);
                mPasswordButton.setOnPreferenceClickListener(this);
                category.addPreference(mPasswordButton);

                mResetButton = new Preference(SecuritySettings.this);
                mResetButton.setTitle(R.string.credentials_reset);
                mResetButton.setSummary(R.string.credentials_reset_summary);
                mResetButton.setOnPreferenceClickListener(this);
                category.addPreference(mResetButton);
                break;

            case TYPE_ENCRYPTEDFS:
                mEncryptedFSEnabled = new CheckBoxPreference(SecuritySettings.this);
                mEncryptedFSEnabled.setTitle(R.string.encrypted_fs_enable);
                mEncryptedFSEnabled.setSummary(R.string.encrypted_fs_enable_summary);
                mEncryptedFSEnabled.setOnPreferenceChangeListener(this);
                // category.addPreference(mEncryptedFSEnabled);
                break;
            }
        }

        private void updatePreferences(int state) {
            mAccessCheckBox.setChecked(state == KeyStore.NO_ERROR);
            boolean encFSEnabled = SystemProperties.getBoolean(PROPERTY_EFS_ENABLED,
                    false);
            mResetButton.setEnabled((!encFSEnabled) && (state != KeyStore.UNINITIALIZED));
            mAccessCheckBox.setEnabled((state != KeyStore.UNINITIALIZED) && (!encFSEnabled));

            // Encrypted File system preferences
            mEncryptedFSEnabled.setChecked(encFSEnabled);

            // Show a toast message if the state is changed.
            if (mState == state) {
                return;
            } else if (state == KeyStore.NO_ERROR) {
                Toast.makeText(SecuritySettings.this, R.string.credentials_enabled,
                        Toast.LENGTH_SHORT).show();
            } else if (state == KeyStore.UNINITIALIZED) {
                Toast.makeText(SecuritySettings.this, R.string.credentials_erased,
                        Toast.LENGTH_SHORT).show();
            } else if (state == KeyStore.LOCKED) {
                Toast.makeText(SecuritySettings.this, R.string.credentials_disabled,
                        Toast.LENGTH_SHORT).show();
            }
            mState = state;
        }

        private void showUnlockDialog() {
            View view = View.inflate(SecuritySettings.this,
                    R.layout.credentials_unlock_dialog, null);

            // Show extra hint only when the action comes from outside.
            if (mExternal) {
                view.findViewById(R.id.hint).setVisibility(View.VISIBLE);
            }

            Dialog dialog = new AlertDialog.Builder(SecuritySettings.this)
                    .setView(view)
                    .setTitle(R.string.credentials_unlock)
                    .setPositiveButton(android.R.string.ok, this)
                    .setNegativeButton(android.R.string.cancel, this)
                    .create();
            dialog.setOnDismissListener(this);
            mShowingDialog = DLG_UNLOCK;
            dialog.show();
        }

        private void showPasswordDialog() {
            View view = View.inflate(SecuritySettings.this,
                    R.layout.credentials_password_dialog, null);

            if (mState == KeyStore.UNINITIALIZED) {
                view.findViewById(R.id.hint).setVisibility(View.VISIBLE);
            } else {
                view.findViewById(R.id.old_password_prompt).setVisibility(View.VISIBLE);
                view.findViewById(R.id.old_password).setVisibility(View.VISIBLE);
            }

            Dialog dialog = new AlertDialog.Builder(SecuritySettings.this)
                    .setView(view)
                    .setTitle(R.string.credentials_set_password)
                    .setPositiveButton(android.R.string.ok, this)
                    .setNegativeButton(android.R.string.cancel, this)
                    .create();
            dialog.setOnDismissListener(this);
            mShowingDialog = DLG_PASSWORD;
            dialog.show();
        }

        private void showResetDialog() {
            mShowingDialog = DLG_RESET;
            new AlertDialog.Builder(SecuritySettings.this)
                    .setTitle(android.R.string.dialog_alert_title)
                    .setIcon(android.R.drawable.ic_dialog_alert)
                    .setMessage(R.string.credentials_reset_hint)
                    .setNeutralButton(getString(android.R.string.ok), this)
                    .setNegativeButton(getString(android.R.string.cancel), this)
                    .create().show();
        }

        private void showSwitchEncryptedFSDialog() {
            AlertDialog.Builder builder = new AlertDialog.Builder(SecuritySettings.this)
                    .setCancelable(false)
                    .setTitle(R.string.encrypted_fs_alert_dialog_title);

            mShowingDialog = DLG_ENABLE_EFS;
            if (mWillEnableEncryptedFS) {
                 builder.setMessage(R.string.encrypted_fs_enable_dialog)
                         .setPositiveButton(R.string.encrypted_fs_enable_button, this)
                         .setNegativeButton(R.string.encrypted_fs_cancel_button, this)
                         .create().show();
            } else {
                builder.setMessage(R.string.encrypted_fs_disable_dialog)
                        .setPositiveButton(R.string.encrypted_fs_disable_button, this)
                        .setNegativeButton(R.string.encrypted_fs_cancel_button, this)
                        .create().show();
            }
        }
    }
}
