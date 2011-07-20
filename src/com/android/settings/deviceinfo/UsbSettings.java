/*
 * Copyright (C) 2011 The Android Open Source Project
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

package com.android.settings.deviceinfo;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.BroadcastReceiver;
import android.content.ContentQueryMap;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.usb.UsbManager;
import android.os.Bundle;
import android.os.storage.StorageManager;
import android.os.storage.StorageVolume;
import android.preference.CheckBoxPreference;
import android.preference.Preference;
import android.preference.PreferenceScreen;
import android.provider.Settings;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;

import com.android.settings.R;
import com.android.settings.SettingsPreferenceFragment;

import java.io.File;

/**
 * USB storage settings.
 */
public class UsbSettings extends SettingsPreferenceFragment {

    private static final String TAG = "UsbSettings";

    private static final String KEY_MTP = "usb_mtp";
    private static final String KEY_PTP = "usb_ptp";
    private static final String KEY_INSTALLER_CD = "usb_installer_cd";
    private static final int MENU_ID_INSTALLER_CD = Menu.FIRST;

    private static final int DLG_INSTALLER_CD = 1;

    private UsbManager mUsbManager;
    private String mInstallerImagePath;
    private CheckBoxPreference mMtp;
    private CheckBoxPreference mPtp;
    private MenuItem mInstallerCd;

    private final BroadcastReceiver mStateReceiver = new BroadcastReceiver() {
        public void onReceive(Context content, Intent intent) {
            if (!intent.getBooleanExtra(UsbManager.USB_CONNECTED, false)) {
                removeDialog(DLG_INSTALLER_CD);
            }
            updateToggles();
        }
    };

    private PreferenceScreen createPreferenceHierarchy() {
        PreferenceScreen root = getPreferenceScreen();
        if (root != null) {
            root.removeAll();
        }
        addPreferencesFromResource(R.xml.usb_settings);
        root = getPreferenceScreen();

        mMtp = (CheckBoxPreference)root.findPreference(KEY_MTP);
        mPtp = (CheckBoxPreference)root.findPreference(KEY_PTP);

        return root;
    }

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        mUsbManager = (UsbManager)getSystemService(Context.USB_SERVICE);
        mInstallerImagePath = getString(com.android.internal.R.string.config_isoImagePath);
        if (!(new File(mInstallerImagePath)).exists()) {
            mInstallerImagePath = null;
        }
        setHasOptionsMenu(mInstallerImagePath != null);
    }

    @Override
    public void onPause() {
        super.onPause();
        getActivity().unregisterReceiver(mStateReceiver);
    }

    @Override
    public void onResume() {
        super.onResume();

        // Make sure we reload the preference hierarchy since some of these settings
        // depend on others...
        createPreferenceHierarchy();

        // ACTION_USB_STATE is sticky so this will call updateToggles
        getActivity().registerReceiver(mStateReceiver,
                new IntentFilter(UsbManager.ACTION_USB_STATE));
    }

    @Override
    public Dialog onCreateDialog(int id) {
        switch (id) {
        case DLG_INSTALLER_CD:
                return new AlertDialog.Builder(getActivity())
                    .setTitle(R.string.dlg_installer_cd_title)
                    .setMessage(R.string.dlg_installer_cd_text)
                    .setPositiveButton(R.string.dlg_installer_cd_ok,
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                               // Disable installer CD, return to default function.
                                mUsbManager.setCurrentFunction(null, false);
                            }})
                    .create();
        }
        return null;
    }

    private void updateToggles() {
        if (mUsbManager.isFunctionEnabled(UsbManager.USB_FUNCTION_MTP)) {
            mMtp.setChecked(true);
            mPtp.setChecked(false);
        } else if (mUsbManager.isFunctionEnabled(UsbManager.USB_FUNCTION_PTP)) {
            mMtp.setChecked(false);
            mPtp.setChecked(true);
        } else  {
            mMtp.setChecked(false);
            mPtp.setChecked(false);
        }
        if (mInstallerCd != null) {
            if (mUsbManager.isFunctionEnabled(UsbManager.USB_FUNCTION_MASS_STORAGE)) {
                mInstallerCd.setTitle( R.string.usb_label_installer_cd_done);
            } else {
                mInstallerCd.setTitle( R.string.usb_label_installer_cd);
            }
        }
    }

    @Override
    public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen, Preference preference) {
        Log.d(TAG, "onPreferenceTreeClick " + preference);

        // temporary hack - using check boxes as radio buttons
        // don't allow unchecking them
        if (preference instanceof CheckBoxPreference) {
            CheckBoxPreference checkBox = (CheckBoxPreference)preference;
            if (!checkBox.isChecked()) {
                checkBox.setChecked(true);
                return true;
            }
        }
        if (preference == mMtp) {
            mUsbManager.setCurrentFunction(UsbManager.USB_FUNCTION_MTP, true);
        } else if (preference == mPtp) {
            mUsbManager.setCurrentFunction(UsbManager.USB_FUNCTION_PTP, true);
        }
        updateToggles();
        return true;
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        mInstallerCd = menu.add(Menu.NONE, MENU_ID_INSTALLER_CD, 0,
                R.string.usb_label_installer_cd);
        mInstallerCd.setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case MENU_ID_INSTALLER_CD:
                if (mUsbManager.isFunctionEnabled(UsbManager.USB_FUNCTION_MASS_STORAGE)) {
                    // Disable installer CD, return to default function.
                    mUsbManager.setCurrentFunction(null, false);
                    removeDialog(DLG_INSTALLER_CD);
                } else {
                    // Enable installer CD.  Don't set as default function.
                    mUsbManager.setCurrentFunction(UsbManager.USB_FUNCTION_MASS_STORAGE, false);
                    mUsbManager.setMassStorageBackingFile(mInstallerImagePath);
                    showDialog(DLG_INSTALLER_CD);
                }
                updateToggles();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
