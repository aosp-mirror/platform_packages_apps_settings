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


import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.ContentQueryMap;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager.NameNotFoundException;
import android.database.Cursor;
import android.location.LocationManager;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.EditTextPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceCategory;
import android.preference.PreferenceGroup;
import android.preference.PreferenceScreen;
import android.provider.Settings;
import android.security.CertTool;
import android.security.Keystore;
import android.text.Html;
import android.text.TextUtils;
import android.text.method.LinkMovementMethod;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.android.internal.widget.LockPatternUtils;
import android.telephony.TelephonyManager;

import java.util.ArrayList;
import java.util.List;
import java.util.Observable;
import java.util.Observer;

/**
 * Gesture lock pattern settings.
 */
public class SecuritySettings extends PreferenceActivity implements
        DialogInterface.OnDismissListener, DialogInterface.OnClickListener {

    // Lock Settings

    private static final String KEY_LOCK_ENABLED = "lockenabled";
    private static final String KEY_VISIBLE_PATTERN = "visiblepattern";
    private static final String KEY_TACTILE_FEEDBACK_ENABLED = "tactilefeedback";
    private static final int CONFIRM_PATTERN_THEN_DISABLE_AND_CLEAR_REQUEST_CODE = 55;

    private LockPatternUtils mLockPatternUtils;
    private CheckBoxPreference mLockEnabled;
    private CheckBoxPreference mVisiblePattern;
    private CheckBoxPreference mTactileFeedback;
    private Preference mChoosePattern;

    private CheckBoxPreference mShowPassword;

    // Location Settings
    private static final String LOCATION_CATEGORY = "location_category";
    private static final String LOCATION_NETWORK = "location_network";
    private static final String LOCATION_GPS = "location_gps";

    // Credential storage
    public static final String ACTION_ADD_CREDENTIAL =
            "android.security.ADD_CREDENTIAL";
    public static final String ACTION_UNLOCK_CREDENTIAL_STORAGE =
            "android.security.UNLOCK_CREDENTIAL_STORAGE";
    private static final String KEY_CSTOR_TYPE_NAME = "typeName";
    private static final String KEY_CSTOR_ITEM = "item";
    private static final String KEY_CSTOR_NAMESPACE = "namespace";
    private static final String KEY_CSTOR_DESCRIPTION = "description";
    private static final int CSTOR_MIN_PASSWORD_LENGTH = 8;

    private static final int CSTOR_INIT_DIALOG = 1;
    private static final int CSTOR_CHANGE_PASSWORD_DIALOG = 2;
    private static final int CSTOR_UNLOCK_DIALOG = 3;
    private static final int CSTOR_RESET_DIALOG = 4;
    private static final int CSTOR_NAME_CREDENTIAL_DIALOG = 5;

    private CstorHelper mCstorHelper = new CstorHelper();

    // Vendor specific
    private static final String GSETTINGS_PROVIDER = "com.google.android.providers.settings";
    private static final String USE_LOCATION = "use_location";
    private static final String KEY_DONE_USE_LOCATION = "doneLocation";
    private CheckBoxPreference mUseLocation;
    private boolean mOkClicked;
    private Dialog mUseLocationDialog;

    private CheckBoxPreference mNetwork;
    private CheckBoxPreference mGps;

    // These provide support for receiving notification when Location Manager settings change.
    // This is necessary because the Network Location Provider can change settings
    // if the user does not confirm enabling the provider.
    private ContentQueryMap mContentQueryMap;
    private final class SettingsObserver implements Observer {
        public void update(Observable o, Object arg) {
            updateToggles();
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.security_settings);

        mLockPatternUtils = new LockPatternUtils(getContentResolver());

        createPreferenceHierarchy();

        mNetwork = (CheckBoxPreference) getPreferenceScreen().findPreference(LOCATION_NETWORK);
        mGps = (CheckBoxPreference) getPreferenceScreen().findPreference(LOCATION_GPS);
        mUseLocation = (CheckBoxPreference) getPreferenceScreen().findPreference(USE_LOCATION);

        // Vendor specific
        try {
            if (mUseLocation != null
                    && getPackageManager().getPackageInfo(GSETTINGS_PROVIDER, 0) == null) {
                ((PreferenceGroup)findPreference(LOCATION_CATEGORY))
                        .removePreference(mUseLocation);
            }
        } catch (NameNotFoundException nnfe) {
        }
        updateToggles();

        // listen for Location Manager settings changes
        Cursor settingsCursor = getContentResolver().query(Settings.Secure.CONTENT_URI, null,
                "(" + Settings.System.NAME + "=?)",
                new String[]{Settings.Secure.LOCATION_PROVIDERS_ALLOWED},
                null);
        mContentQueryMap = new ContentQueryMap(settingsCursor, Settings.System.NAME, true, null);
        mContentQueryMap.addObserver(new SettingsObserver());
        boolean doneUseLocation = savedInstanceState == null
                ? false : savedInstanceState.getBoolean(KEY_DONE_USE_LOCATION, true);
        if (!doneUseLocation && (getIntent().getBooleanExtra("SHOW_USE_LOCATION", false)
                || savedInstanceState != null)) {
            showUseLocationDialog(true);
        }

        mCstorHelper.handleCstorIntents(getIntent());
    }

    private PreferenceScreen createPreferenceHierarchy() {
        // Root
        PreferenceScreen root = this.getPreferenceScreen();

        // Inline preferences
        PreferenceCategory inlinePrefCat = new PreferenceCategory(this);
        inlinePrefCat.setTitle(R.string.lock_settings_title);
        root.addPreference(inlinePrefCat);

        // autolock toggle
        mLockEnabled = new LockEnabledPref(this);
        mLockEnabled.setTitle(R.string.lockpattern_settings_enable_title);
        mLockEnabled.setSummary(R.string.lockpattern_settings_enable_summary);
        mLockEnabled.setKey(KEY_LOCK_ENABLED);
        inlinePrefCat.addPreference(mLockEnabled);

        // visible pattern
        mVisiblePattern = new CheckBoxPreference(this);
        mVisiblePattern.setKey(KEY_VISIBLE_PATTERN);
        mVisiblePattern.setTitle(R.string.lockpattern_settings_enable_visible_pattern_title);
        inlinePrefCat.addPreference(mVisiblePattern);

        // tactile feedback
        mTactileFeedback = new CheckBoxPreference(this);
        mTactileFeedback.setKey(KEY_TACTILE_FEEDBACK_ENABLED);
        mTactileFeedback.setTitle(R.string.lockpattern_settings_enable_tactile_feedback_title);
        inlinePrefCat.addPreference(mTactileFeedback);

        // change pattern lock
        Intent intent = new Intent();
        intent.setClassName("com.android.settings",
                    "com.android.settings.ChooseLockPatternTutorial");
        mChoosePattern = getPreferenceManager().createPreferenceScreen(this);
        mChoosePattern.setIntent(intent);
        inlinePrefCat.addPreference(mChoosePattern);

        int activePhoneType = TelephonyManager.getDefault().getPhoneType();

        // do not display SIM lock for CDMA phone
        if (TelephonyManager.PHONE_TYPE_CDMA != activePhoneType)
        {
            PreferenceScreen simLockPreferences = getPreferenceManager()
                    .createPreferenceScreen(this);
            simLockPreferences.setTitle(R.string.sim_lock_settings_category);
            // Intent to launch SIM lock settings
            intent = new Intent();
            intent.setClassName("com.android.settings", "com.android.settings.IccLockSettings");
            simLockPreferences.setIntent(intent);

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

        // Credential storage
        PreferenceCategory credStoreCat = new PreferenceCategory(this);
        credStoreCat.setTitle(R.string.cstor_settings_category);
        root.addPreference(credStoreCat);
        int s = mCstorHelper.getCstorState();
        credStoreCat.addPreference(mCstorHelper.createAccessCheckBox(s));
        credStoreCat.addPreference(mCstorHelper.createSetPasswordPreference());
        credStoreCat.addPreference(mCstorHelper.createResetPreference(s));

        return root;
    }

    @Override
    protected void onResume() {
        super.onResume();

        boolean patternExists = mLockPatternUtils.savedPatternExists();
        mLockEnabled.setEnabled(patternExists);
        mVisiblePattern.setEnabled(patternExists);
        mTactileFeedback.setEnabled(patternExists);

        mLockEnabled.setChecked(mLockPatternUtils.isLockPatternEnabled());
        mVisiblePattern.setChecked(mLockPatternUtils.isVisiblePatternEnabled());
        mTactileFeedback.setChecked(mLockPatternUtils.isTactileFeedbackEnabled());

        int chooseStringRes = mLockPatternUtils.savedPatternExists() ?
                R.string.lockpattern_settings_change_lock_pattern :
                R.string.lockpattern_settings_choose_lock_pattern;
        mChoosePattern.setTitle(chooseStringRes);

        mShowPassword
                .setChecked(Settings.System.getInt(getContentResolver(),
                Settings.System.TEXT_SHOW_PASSWORD, 1) != 0);
    }

    @Override
    public void onStop() {
        if (mUseLocationDialog != null && mUseLocationDialog.isShowing()) {
            mUseLocationDialog.dismiss();
        }
        mUseLocationDialog = null;
        super.onStop();
    }

    @Override
    public void onSaveInstanceState(Bundle icicle) {
        if (mUseLocationDialog != null && mUseLocationDialog.isShowing()) {
            icicle.putBoolean(KEY_DONE_USE_LOCATION, false);
        }
        super.onSaveInstanceState(icicle);
    }

    @Override
    public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen,
            Preference preference) {
        final String key = preference.getKey();

        if (KEY_LOCK_ENABLED.equals(key)) {
            mLockPatternUtils.setLockPatternEnabled(isToggled(preference));
        } else if (KEY_VISIBLE_PATTERN.equals(key)) {
            mLockPatternUtils.setVisiblePatternEnabled(isToggled(preference));
        } else if (KEY_TACTILE_FEEDBACK_ENABLED.equals(key)) {
            mLockPatternUtils.setTactileFeedbackEnabled(isToggled(preference));
        } else if (preference == mShowPassword) {
            Settings.System.putInt(getContentResolver(), Settings.System.TEXT_SHOW_PASSWORD,
                    mShowPassword.isChecked() ? 1 : 0);
        } else if (preference == mNetwork) {
            Settings.Secure.setLocationProviderEnabled(getContentResolver(),
                    LocationManager.NETWORK_PROVIDER, mNetwork.isChecked());
        } else if (preference == mGps) {
            Settings.Secure.setLocationProviderEnabled(getContentResolver(),
                    LocationManager.GPS_PROVIDER, mGps.isChecked());
        } else if (preference == mUseLocation) {
            //normally called on the toggle click
            if (mUseLocation.isChecked()) {
                showUseLocationDialog(false);
            } else {
                updateUseLocation();
            }
        }

        return false;
    }

    private void showPrivacyPolicy() {
        Intent intent = new Intent("android.settings.TERMS");
        startActivity(intent);
    }

    private void showUseLocationDialog(boolean force) {
        // Show a warning to the user that location data will be shared
        mOkClicked = false;
        if (force) {
            mUseLocation.setChecked(true);
        }

        CharSequence msg = getResources().getText(R.string.use_location_warning_message);
        mUseLocationDialog = new AlertDialog.Builder(this).setMessage(msg)
                .setTitle(R.string.use_location_title)
                .setIcon(android.R.drawable.ic_dialog_alert)
                .setPositiveButton(R.string.agree, this)
                .setNegativeButton(R.string.disagree, this)
                .show();
        ((TextView)mUseLocationDialog.findViewById(android.R.id.message))
                .setMovementMethod(LinkMovementMethod.getInstance());
        mUseLocationDialog.setOnDismissListener(this);
    }

    /*
     * Creates toggles for each available location provider
     */
    private void updateToggles() {
        ContentResolver res = getContentResolver();
        mNetwork.setChecked(Settings.Secure.isLocationProviderEnabled(
                res, LocationManager.NETWORK_PROVIDER));
        mGps.setChecked(Settings.Secure.isLocationProviderEnabled(
                res, LocationManager.GPS_PROVIDER));
        mUseLocation.setChecked(Settings.Secure.getInt(res,
                Settings.Secure.USE_LOCATION_FOR_SERVICES, 2) == 1);
    }

    private boolean isToggled(Preference pref) {
        return ((CheckBoxPreference) pref).isChecked();
    }

    private void updateUseLocation() {
        boolean use = mUseLocation.isChecked();
        Settings.Secure.putInt(getContentResolver(),
                Settings.Secure.USE_LOCATION_FOR_SERVICES, use ? 1 : 0);
    }


    /**
     * For the user to disable keyguard, we first make them verify their
     * existing pattern.
     */
    private class LockEnabledPref extends CheckBoxPreference {

        public LockEnabledPref(Context context) {
            super(context);
        }

        @Override
        protected void onClick() {
            if (mLockPatternUtils.savedPatternExists() && isChecked()) {
                confirmPatternThenDisableAndClear();
            } else {
                super.onClick();
            }
        }
    }

    /**
     * Launch screen to confirm the existing lock pattern.
     * @see #onActivityResult(int, int, android.content.Intent)
     */
    private void confirmPatternThenDisableAndClear() {
        final Intent intent = new Intent();
        intent.setClassName("com.android.settings", "com.android.settings.ConfirmLockPattern");
        startActivityForResult(intent, CONFIRM_PATTERN_THEN_DISABLE_AND_CLEAR_REQUEST_CODE);
    }

    /**
     * @see #confirmPatternThenDisableAndClear
     */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        final boolean resultOk = resultCode == Activity.RESULT_OK;

        if ((requestCode == CONFIRM_PATTERN_THEN_DISABLE_AND_CLEAR_REQUEST_CODE)
                && resultOk) {
            mLockPatternUtils.setLockPatternEnabled(false);
            mLockPatternUtils.saveLockPattern(null);
        }
    }

    public void onClick(DialogInterface dialog, int which) {
        if (which == DialogInterface.BUTTON_POSITIVE) {
            //updateProviders();
            mOkClicked = true;
        } else {
            // Reset the toggle
            mUseLocation.setChecked(false);
        }
        updateUseLocation();
    }

    public void onDismiss(DialogInterface dialog) {
        // Assuming that onClick gets called first
        if (!mOkClicked) {
            mUseLocation.setChecked(false);
        }
    }

    @Override
    protected Dialog onCreateDialog (int id) {
        switch (id) {
            case CSTOR_INIT_DIALOG:
            case CSTOR_CHANGE_PASSWORD_DIALOG:
                return mCstorHelper.createSetPasswordDialog(id);

            case CSTOR_UNLOCK_DIALOG:
                return mCstorHelper.createUnlockDialog();

            case CSTOR_RESET_DIALOG:
                return mCstorHelper.createResetDialog();

            case CSTOR_NAME_CREDENTIAL_DIALOG:
                return mCstorHelper.createNameCredentialDialog();

            default:
                return null;
        }
    }

    private class CstorHelper implements DialogInterface.OnClickListener,
            DialogInterface.OnDismissListener,
            DialogInterface.OnCancelListener {
        private Keystore mKeystore = Keystore.getInstance();
        private View mView;
        private int mDialogId;
        private boolean mConfirm = true;

        private CheckBoxPreference mAccessCheckBox;
        private Preference mResetButton;

        private Intent mSpecialIntent;
        private CstorAddCredentialHelper mCstorAddCredentialHelper;

        void handleCstorIntents(Intent intent) {
            if (intent == null) return;
            String action = intent.getAction();

            if (ACTION_ADD_CREDENTIAL.equals(action)) {
                mCstorAddCredentialHelper = new CstorAddCredentialHelper(intent);
                showCstorDialog(CSTOR_NAME_CREDENTIAL_DIALOG);
            } else if (ACTION_UNLOCK_CREDENTIAL_STORAGE.equals(action)) {
                mSpecialIntent = intent;
                showCstorDialog(mCstorHelper.isCstorInitialized()
                        ? CSTOR_UNLOCK_DIALOG
                        : CSTOR_INIT_DIALOG);
            }
        }

        private void showCstorDialog(int dialogId) {
            mDialogId = dialogId;
            showDialog(dialogId);

            if (dialogId == CSTOR_NAME_CREDENTIAL_DIALOG) {
                // set mView back as mView may be replaced by CSTOR_INIT_DIALOG
                // or CSTOR_UNLOCK_DIALOG
                mView = mCstorAddCredentialHelper.mView;
            }
        }

        private int getCstorState() {
            return mKeystore.getState();
        }

        private boolean isCstorUnlocked() {
            return (mKeystore.getState() == Keystore.UNLOCKED);
        }

        private boolean isCstorInitialized() {
            return (mKeystore.getState() != Keystore.UNINITIALIZED);
        }

        private void lockCstor() {
            mKeystore.lock();
            mAccessCheckBox.setChecked(false);
        }

        private int unlockCstor(String passwd) {
            int ret = mKeystore.unlock(passwd);
            if (ret == -1) resetCstor();
            if (ret == 0) {
                Toast.makeText(SecuritySettings.this, R.string.cstor_is_enabled,
                        Toast.LENGTH_SHORT).show();
            }
            return ret;
        }

        private int changeCstorPassword(String oldPasswd, String newPasswd) {
            int ret = mKeystore.changePassword(oldPasswd, newPasswd);
            if (ret == -1) resetCstor();
            return ret;
        }

        private void initCstor(String passwd) {
            mKeystore.setPassword(passwd);
            enablePreferences(true);
            mAccessCheckBox.setChecked(true);
            Toast.makeText(SecuritySettings.this, R.string.cstor_is_enabled,
                    Toast.LENGTH_SHORT).show();
        }

        private void resetCstor() {
            mKeystore.reset();
            enablePreferences(false);
            mAccessCheckBox.setChecked(false);
            Toast.makeText(SecuritySettings.this, R.string.cstor_is_reset,
                    Toast.LENGTH_LONG).show();
        }

        private boolean addCredential() {
            if (mCstorAddCredentialHelper.saveToStorage() != 0) {
                // set mView back as mView may be replaced by CSTOR_INIT_DIALOG
                // or CSTOR_UNLOCK_DIALOG
                mView = mCstorAddCredentialHelper.mView;
                if (mCstorAddCredentialHelper.isPkcs12Keystore()) {
                    showError(R.string.cstor_password_error);
                } else {
                    showError(R.string.cstor_storage_error);
                }
                Log.d("CSTOR", "failed to add credential");
                return false;
            }
            Log.d("CSTOR", "credential is added: "
                    + mCstorAddCredentialHelper.getName());
            String formatString =
                    getString(R.string.cstor_is_added);
            String message = String.format(formatString,
                    mCstorAddCredentialHelper.getName());
            Toast.makeText(SecuritySettings.this, message,
                    Toast.LENGTH_LONG).show();
            return true;
        }

        public void onCancel(DialogInterface dialog) {
            if (mCstorAddCredentialHelper == null) return;

            switch (mDialogId) {
                case CSTOR_INIT_DIALOG:
                case CSTOR_UNLOCK_DIALOG:
                    Toast.makeText(SecuritySettings.this,
                            R.string.cstor_unable_to_save_cert,
                            Toast.LENGTH_LONG).show();
                    break;

                case CSTOR_NAME_CREDENTIAL_DIALOG:
                    Toast.makeText(SecuritySettings.this,
                            R.string.cstor_cert_not_saved,
                            Toast.LENGTH_LONG).show();
                    break;
            }
            mCstorAddCredentialHelper = null;
            finish();
        }

        public void onClick(DialogInterface dialog, int which) {
            if (which == DialogInterface.BUTTON_NEGATIVE) {
                onCancel(dialog);
                return;
            }

            switch (mDialogId) {
                case CSTOR_INIT_DIALOG:
                case CSTOR_CHANGE_PASSWORD_DIALOG:
                    mConfirm = checkPasswords((Dialog) dialog);
                    break;

                case CSTOR_UNLOCK_DIALOG:
                    mConfirm = checkUnlockPassword((Dialog) dialog);
                    break;

                case CSTOR_RESET_DIALOG:
                    resetCstor();
                    break;

                case CSTOR_NAME_CREDENTIAL_DIALOG:
                    mConfirm = checkAddCredential();
                    break;
            }
        }

        public void onDismiss(DialogInterface dialog) {
            if (!mConfirm) {
                mConfirm = true;
                showCstorDialog(mDialogId);
            } else {
                if (mDialogId == CSTOR_UNLOCK_DIALOG) {
                    mAccessCheckBox.setChecked(isCstorUnlocked());
                }

                if (mCstorAddCredentialHelper != null) {
                    if (!isCstorInitialized()) {
                        showCstorDialog(CSTOR_INIT_DIALOG);
                    } else if (!isCstorUnlocked()) {
                        showCstorDialog(CSTOR_UNLOCK_DIALOG);
                    } else {
                        if (addCredential()) {
                            // succeeded
                            finish();
                        } else {
                            // failed
                            if (mDialogId != CSTOR_NAME_CREDENTIAL_DIALOG) {
                                removeDialog(mDialogId);
                            }
                            showCstorDialog(CSTOR_NAME_CREDENTIAL_DIALOG);
                        }
                    }
                    return;
                } else if (mSpecialIntent != null) {
                    finish();
                }
                removeDialog(mDialogId);
            }
        }

        private void showResetWarning(int count) {
            TextView v = showError(count <= 3
                    ? R.string.cstor_password_error_reset_warning
                    : R.string.cstor_password_error);
            if (count <= 3) {
                if (count == 1) {
                    v.setText(R.string.cstor_password_error_reset_warning);
                } else {
                    String format = getString(
                            R.string.cstor_password_error_reset_warning_plural);
                    v.setText(String.format(format, count));
                }
            }
        }

        private boolean checkAddCredential() {
            hideError();

            String name = getText(R.id.cstor_credential_name);
            if (TextUtils.isEmpty(name)) {
                showError(R.string.cstor_name_empty_error);
                return false;
            }

            for (int i = 0, len = name.length(); i < len; i++) {
                if (!Character.isLetterOrDigit(name.charAt(i))) {
                    showError(R.string.cstor_name_char_error);
                    return false;
                }
            }

            mCstorAddCredentialHelper.setName(name);

            if (mCstorAddCredentialHelper.isPkcs12Keystore()) {
                String password = getText(R.id.cstor_credential_password);
                if (TextUtils.isEmpty(password)) {
                    showError(R.string.cstor_password_empty_error);
                    return false;
                }

                mCstorAddCredentialHelper.setPassword(password);
            }

            return true;
        }

        // returns true if the password is long enough and does not contain
        // characters that we don't like
        private boolean verifyPassword(String passwd) {
            if (passwd == null) {
                showError(R.string.cstor_passwords_empty_error);
                return false;
            } else if ((passwd.length() < CSTOR_MIN_PASSWORD_LENGTH)
                    || passwd.contains(" ")) {
                showError(R.string.cstor_password_verification_error);
                return false;
            } else {
                return true;
            }
        }

        // returns true if the password is ok
        private boolean checkUnlockPassword(Dialog d) {
            hideError();

            String passwd = getText(R.id.cstor_password);
            if (TextUtils.isEmpty(passwd)) {
                showError(R.string.cstor_password_empty_error);
                return false;
            }

            int count = unlockCstor(passwd);
            if (count > 0) {
                showResetWarning(count);
                return false;
            } else {
                // done or reset
                return true;
            }
        }

        // returns true if the passwords are ok
        private boolean checkPasswords(Dialog d) {
            hideError();

            String oldPasswd = getText(R.id.cstor_old_password);
            String newPasswd = getText(R.id.cstor_new_password);
            String confirmPasswd = getText(R.id.cstor_confirm_password);

            if ((mDialogId == CSTOR_CHANGE_PASSWORD_DIALOG)
                    && TextUtils.isEmpty(oldPasswd)) {
                showError(R.string.cstor_password_empty_error);
                return false;
            }

            if (TextUtils.isEmpty(newPasswd)
                    && TextUtils.isEmpty(confirmPasswd)) {
                showError(R.string.cstor_passwords_empty_error);
                return false;
            }

            if (!verifyPassword(newPasswd)) {
                return false;
            } else if (!newPasswd.equals(confirmPasswd)) {
                showError(R.string.cstor_passwords_error);
                return false;
            }

            if (mDialogId == CSTOR_CHANGE_PASSWORD_DIALOG) {
                int count = changeCstorPassword(oldPasswd, newPasswd);
                if (count > 0) {
                    showResetWarning(count);
                    return false;
                } else {
                    // done or reset
                    return true;
                }
            } else {
                initCstor(newPasswd);
                return true;
            }
        }

        private TextView showError(int messageId) {
            TextView v = (TextView) mView.findViewById(R.id.cstor_error);
            v.setText(messageId);
            if (v != null) v.setVisibility(View.VISIBLE);
            return v;
        }

        private void hide(int viewId) {
            View v = mView.findViewById(viewId);
            if (v != null) v.setVisibility(View.GONE);
        }

        private void hideError() {
            hide(R.id.cstor_error);
        }

        private String getText(int viewId) {
            return ((TextView) mView.findViewById(viewId)).getText().toString();
        }

        private void setText(int viewId, String text) {
            TextView v = (TextView) mView.findViewById(viewId);
            if (v != null) v.setText(text);
        }

        private void setText(int viewId, int textId) {
            TextView v = (TextView) mView.findViewById(viewId);
            if (v != null) v.setText(textId);
        }

        private void enablePreferences(boolean enabled) {
            mAccessCheckBox.setEnabled(enabled);
            mResetButton.setEnabled(enabled);
        }

        private Preference createAccessCheckBox(int state) {
            CheckBoxPreference pref = new CheckBoxPreference(
                    SecuritySettings.this);
            pref.setTitle(R.string.cstor_access_title);
            pref.setSummary(R.string.cstor_access_summary);
            pref.setEnabled(state != Keystore.UNINITIALIZED);
            pref.setChecked(state == Keystore.UNLOCKED);
            pref.setOnPreferenceChangeListener(
                    new Preference.OnPreferenceChangeListener() {
                        public boolean onPreferenceChange(
                                Preference pref, Object value) {
                            if (((Boolean) value)) {
                                showCstorDialog(CSTOR_UNLOCK_DIALOG);
                            } else {
                                lockCstor();
                            }
                            return true;
                        }
                    });
            mAccessCheckBox = pref;
            return pref;
        }

        private Preference createSetPasswordPreference() {
            Preference pref = new Preference(SecuritySettings.this);
            pref.setTitle(R.string.cstor_set_passwd_title);
            pref.setSummary(R.string.cstor_set_passwd_summary);
            pref.setOnPreferenceClickListener(
                    new Preference.OnPreferenceClickListener() {
                        public boolean onPreferenceClick(Preference pref) {
                            showCstorDialog(isCstorInitialized()
                                    ? CSTOR_CHANGE_PASSWORD_DIALOG
                                    : CSTOR_INIT_DIALOG);
                            return true;
                        }
                    });
            return pref;
        }

        private Preference createResetPreference(int state) {
            Preference pref = new Preference(SecuritySettings.this);
            pref.setTitle(R.string.cstor_reset_title);
            pref.setSummary(R.string.cstor_reset_summary);
            pref.setOnPreferenceClickListener(
                    new Preference.OnPreferenceClickListener() {
                        public boolean onPreferenceClick(Preference pref) {
                            showCstorDialog(CSTOR_RESET_DIALOG);
                            return true;
                        }
                    });
            pref.setEnabled(state != Keystore.UNINITIALIZED);
            mResetButton = pref;
            return pref;
        }

        private Dialog createUnlockDialog() {
            mView = View.inflate(SecuritySettings.this,
                    R.layout.cstor_unlock_dialog_view, null);
            hideError();

            // show extra hint only when the action comes from outside
            if ((mSpecialIntent == null)
                    && (mCstorAddCredentialHelper == null)) {
                hide(R.id.cstor_access_dialog_hint_from_action);
            }

            Dialog d = new AlertDialog.Builder(SecuritySettings.this)
                    .setView(mView)
                    .setTitle(R.string.cstor_access_dialog_title)
                    .setPositiveButton(android.R.string.ok, this)
                    .setNegativeButton(android.R.string.cancel, this)
                    .setOnCancelListener(this)
                    .create();
            d.setOnDismissListener(this);
            return d;
        }

        private Dialog createSetPasswordDialog(int id) {
            mView = View.inflate(SecuritySettings.this,
                    R.layout.cstor_set_password_dialog_view, null);
            hideError();

            // show extra hint only when the action comes from outside
            if ((mSpecialIntent != null)
                    || (mCstorAddCredentialHelper != null)) {
                setText(R.id.cstor_first_time_hint,
                        R.string.cstor_first_time_hint_from_action);
            }

            switch (id) {
                case CSTOR_INIT_DIALOG:
                    mView.findViewById(R.id.cstor_old_password_block)
                            .setVisibility(View.GONE);
                    break;

                case CSTOR_CHANGE_PASSWORD_DIALOG:
                    mView.findViewById(R.id.cstor_first_time_hint)
                            .setVisibility(View.GONE);
                    break;

                default:
                    throw new RuntimeException(
                            "Unknown dialog id: " + mDialogId);
            }

            Dialog d = new AlertDialog.Builder(SecuritySettings.this)
                    .setView(mView)
                    .setTitle(R.string.cstor_set_passwd_dialog_title)
                    .setPositiveButton(android.R.string.ok, this)
                    .setNegativeButton(android.R.string.cancel, this)
                    .setOnCancelListener(this)
                    .create();
            d.setOnDismissListener(this);
            return d;
        }

        private Dialog createResetDialog() {
            return new AlertDialog.Builder(SecuritySettings.this)
                    .setTitle(android.R.string.dialog_alert_title)
                    .setIcon(android.R.drawable.ic_dialog_alert)
                    .setMessage(R.string.cstor_reset_hint)
                    .setPositiveButton(getString(android.R.string.ok), this)
                    .setNegativeButton(getString(android.R.string.cancel), this)
                    .create();
        }

        private Dialog createNameCredentialDialog() {
            mView = View.inflate(SecuritySettings.this,
                    R.layout.cstor_name_credential_dialog_view, null);
            if (mCstorAddCredentialHelper != null) {
                mCstorAddCredentialHelper.mView = mView;
            }

            hideError();
            if (!mCstorAddCredentialHelper.isPkcs12Keystore()) {
                hide(R.id.cstor_credential_password_container);
            }

            setText(R.id.cstor_credential_name_title,
                    R.string.cstor_credential_name);
            setText(R.id.cstor_credential_info_title,
                    R.string.cstor_credential_info);
            setText(R.id.cstor_credential_info,
                    mCstorAddCredentialHelper.getDescription().toString());

            Dialog d = new AlertDialog.Builder(SecuritySettings.this)
                    .setView(mView)
                    .setTitle(R.string.cstor_name_credential_dialog_title)
                    .setPositiveButton(android.R.string.ok, this)
                    .setNegativeButton(android.R.string.cancel, this)
                    .setOnCancelListener(this)
                    .create();
            d.setOnDismissListener(this);
            return d;
        }
    }

    private class CstorAddCredentialHelper {
        private String mTypeName;
        private List<byte[]> mItemList;
        private List<String> mNamespaceList;
        private String mDescription;
        private String mName;
        private String mPassword;
        private View mView;

        CstorAddCredentialHelper(Intent intent) {
            parse(intent);
        }

        String getTypeName() {
            return mTypeName;
        }

        boolean isPkcs12Keystore() {
            return CertTool.TITLE_PKCS12_KEYSTORE.equals(mTypeName);
        }

        CharSequence getDescription() {
            return Html.fromHtml(mDescription);
        }

        void setName(String name) {
            mName = name;
        }

        String getName() {
            return mName;
        }

        void setPassword(String password) {
            mPassword = password;
        }

        String getPassword() {
            return mPassword;
        }

        int saveToStorage() {
            if (isPkcs12Keystore()) {
                return CertTool.getInstance().addPkcs12Keystore(
                        mItemList.get(0), mPassword, mName);
            } else {
                Keystore ks = Keystore.getInstance();
                for (int i = 0, count = mItemList.size(); i < count; i++) {
                    byte[] blob = mItemList.get(i);
                    int ret = ks.put(mNamespaceList.get(i), mName,
                            new String(blob));
                    if (ret != 0) return ret;
                }
            }
            return 0;
        }

        private void parse(Intent intent) {
            mTypeName = intent.getStringExtra(KEY_CSTOR_TYPE_NAME);
            mItemList = new ArrayList<byte[]>();
            mNamespaceList = new ArrayList<String>();
            for (int i = 0; ; i++) {
                byte[] blob = intent.getByteArrayExtra(KEY_CSTOR_ITEM + i);
                if (blob == null) break;
                mItemList.add(blob);
                mNamespaceList.add(intent.getStringExtra(
                        KEY_CSTOR_NAMESPACE + i));
            }

            // build description string
            StringBuilder sb = new StringBuilder();
            for (int i = 0; ; i++) {
                String s = intent.getStringExtra(KEY_CSTOR_DESCRIPTION + i);
                if (s == null) break;
                sb.append(s).append("<br>");
            }
            mDescription = sb.toString();
        }
    }
}
