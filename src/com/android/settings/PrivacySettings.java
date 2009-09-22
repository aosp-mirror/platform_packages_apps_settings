/*
 * Copyright (C) 2009 The Android Open Source Project
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

import android.app.AlertDialog;
import android.app.Dialog;
import android.backup.IBackupManager;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.Bundle;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.preference.CheckBoxPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceScreen;
import android.provider.Settings;
import android.text.method.LinkMovementMethod;
import android.widget.TextView;

/**
 * Gesture lock pattern settings.
 */
public class PrivacySettings extends PreferenceActivity implements
        DialogInterface.OnDismissListener, DialogInterface.OnClickListener {

    private static final String PREFS_NAME = "location_prefs";
    private static final String PREFS_USE_LOCATION = "use_location";

    // Vendor specific
    private static final String GSETTINGS_PROVIDER = "com.google.android.providers.settings";
    private static final String LOCATION_CATEGORY = "location_category";
    private static final String SETTINGS_CATEGORY = "settings_category";
    private static final String USE_LOCATION = "use_location";
    private static final String BACKUP_SETTINGS = "backup_settings";
    private static final String KEY_DONE_USE_LOCATION = "doneLocation";
    private CheckBoxPreference mUseLocation;
    private CheckBoxPreference mBackup;
    private boolean mOkClicked;
    private Dialog mConfirmDialog;

    private static final int DIALOG_USE_LOCATION = 1;
    private static final int DIALOG_ERASE_BACKUP = 2;
    private int     mDialogType;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.privacy_settings);

        mUseLocation = (CheckBoxPreference) getPreferenceScreen().findPreference(USE_LOCATION);
        mBackup = (CheckBoxPreference) getPreferenceScreen().findPreference(BACKUP_SETTINGS);

        // Vendor specific
        try {
            if (mUseLocation != null) {
                getPackageManager().getPackageInfo(GSETTINGS_PROVIDER, 0);
            }
        } catch (NameNotFoundException nnfe) {
            getPreferenceScreen().removePreference(findPreference(LOCATION_CATEGORY));
            getPreferenceScreen().removePreference(findPreference(SETTINGS_CATEGORY));
        }
        updateToggles();

        boolean doneUseLocation = savedInstanceState == null
                ? false : savedInstanceState.getBoolean(KEY_DONE_USE_LOCATION, true);
        if (!doneUseLocation && (getIntent().getBooleanExtra("SHOW_USE_LOCATION", false)
                || savedInstanceState != null)) {
            showUseLocationDialog(true);
        }
    }

    @Override
    public void onStop() {
        if (mConfirmDialog != null && mConfirmDialog.isShowing()) {
            mConfirmDialog.dismiss();
        }
        mConfirmDialog = null;
        mDialogType = 0;
        super.onStop();
    }

    @Override
    public void onSaveInstanceState(Bundle icicle) {
        if (mConfirmDialog != null && mConfirmDialog.isShowing()
                && mDialogType == DIALOG_USE_LOCATION) {
            icicle.putBoolean(KEY_DONE_USE_LOCATION, false);
        }
        super.onSaveInstanceState(icicle);
    }

    @Override
    public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen,
            Preference preference) {
        if (preference == mUseLocation) {
            //normally called on the toggle click
            if (mUseLocation.isChecked()) {
                showUseLocationDialog(false);
            } else {
                updateUseLocation();
            }
        } else if (preference == mBackup) {
            if (!mBackup.isChecked()) {
                showEraseBackupDialog();
            } else {
                setBackupEnabled(true);
            }
        }

        return false;
    }

    private void showUseLocationDialog(boolean force) {
        // Show a warning to the user that location data will be shared
        mOkClicked = false;
        if (force) {
            mUseLocation.setChecked(true);
        }

        if (hasAgreedToUseLocation()) {
            updateUseLocation();
            return;
        }

        mDialogType = DIALOG_USE_LOCATION;
        CharSequence msg = getResources().getText(R.string.use_location_warning_message);
        mConfirmDialog = new AlertDialog.Builder(this).setMessage(msg)
                .setTitle(R.string.use_location_title)
                .setIcon(android.R.drawable.ic_dialog_alert)
                .setPositiveButton(R.string.agree, this)
                .setNegativeButton(R.string.disagree, this)
                .show();
        ((TextView)mConfirmDialog.findViewById(android.R.id.message))
                .setMovementMethod(LinkMovementMethod.getInstance());
        mConfirmDialog.setOnDismissListener(this);
    }

    private void showEraseBackupDialog() {
        // Show a warning to the user that location data will be shared
        mOkClicked = false;
        mBackup.setChecked(true);

        mDialogType = DIALOG_ERASE_BACKUP;
        CharSequence msg = getResources().getText(R.string.backup_erase_dialog_message);
        mConfirmDialog = new AlertDialog.Builder(this).setMessage(msg)
                .setTitle(R.string.backup_erase_dialog_title)
                .setIcon(android.R.drawable.ic_dialog_alert)
                .setPositiveButton(android.R.string.ok, this)
                .setNegativeButton(android.R.string.cancel, this)
                .show();
        mConfirmDialog.setOnDismissListener(this);
    }

    /*
     * Creates toggles for each available location provider
     */
    private void updateToggles() {
        ContentResolver res = getContentResolver();
        mUseLocation.setChecked(Settings.Secure.getInt(res,
                Settings.Secure.USE_LOCATION_FOR_SERVICES, 2) == 1);
        mBackup.setChecked(Settings.Secure.getInt(res,
                Settings.Secure.BACKUP_ENABLED, 0) == 1);
    }

    private void updateUseLocation() {
        boolean use = mUseLocation.isChecked();
        Settings.Secure.putInt(getContentResolver(),
                Settings.Secure.USE_LOCATION_FOR_SERVICES, use ? 1 : 0);
    }

    public void onClick(DialogInterface dialog, int which) {
        if (which == DialogInterface.BUTTON_POSITIVE) {
            //updateProviders();
            mOkClicked = true;
            if (mDialogType == DIALOG_USE_LOCATION) {
                setAgreedToUseLocation(true);
            } else if (mDialogType == DIALOG_ERASE_BACKUP) {
                setBackupEnabled(false);
            }
        } else {
            if (mDialogType == DIALOG_USE_LOCATION) {
                // Reset the toggle
                mUseLocation.setChecked(false);
            } else if (mDialogType == DIALOG_ERASE_BACKUP) {
                mBackup.setChecked(true);
            }
        }
        updateUseLocation();
        mDialogType = 0;
    }

    public void onDismiss(DialogInterface dialog) {
        // Assuming that onClick gets called first
        if (!mOkClicked) {
            if (mDialogType == DIALOG_USE_LOCATION) {
                mUseLocation.setChecked(false);
            }
        }
    }

    /**
     * Checks if the user has agreed to the dialog in the past.
     */
    private boolean hasAgreedToUseLocation() {
        SharedPreferences sp = getSharedPreferences(PREFS_NAME, 0);
        if (sp == null) {
            return false;
        }
        return sp.getBoolean(PREFS_USE_LOCATION, false);
    }

    /**
     * Notes that the user has agreed to the dialog and won't need to be prompted in the
     * future.
     */
    private void setAgreedToUseLocation(boolean agreed) {
        if (agreed) {
            SharedPreferences settings = getSharedPreferences(PREFS_NAME, 0);
            SharedPreferences.Editor editor = settings.edit();
            editor.putBoolean(PREFS_USE_LOCATION, true);
            editor.commit();
        }
    }

    /**
     * Informs the BackupManager of a change in backup state - if backup is disabled,
     * the data on the server will be erased.
     * @param enable whether to enable backup
     */
    private void setBackupEnabled(boolean enable) {
        IBackupManager bm = IBackupManager.Stub.asInterface(
                ServiceManager.getService(Context.BACKUP_SERVICE));
        if (bm != null) {
            try {
                bm.setBackupEnabled(enable);
            } catch (RemoteException e) {
                mBackup.setChecked(!enable);
                return;
            }
        }
        mBackup.setChecked(enable);
    }
}
