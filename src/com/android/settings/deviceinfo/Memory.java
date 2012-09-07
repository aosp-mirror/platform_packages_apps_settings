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

package com.android.settings.deviceinfo;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.usb.UsbManager;
import android.os.Bundle;
import android.os.Environment;
import android.os.IBinder;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.storage.IMountService;
import android.os.storage.StorageEventListener;
import android.os.storage.StorageManager;
import android.os.storage.StorageVolume;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceScreen;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.Toast;

import com.android.settings.R;
import com.android.settings.SettingsPreferenceFragment;
import com.android.settings.Utils;
import com.google.common.collect.Lists;

import java.util.ArrayList;

/**
 * Panel showing storage usage on disk for known {@link StorageVolume} returned
 * by {@link StorageManager}. Calculates and displays usage of data types.
 */
public class Memory extends SettingsPreferenceFragment {
    private static final String TAG = "MemorySettings";

    private static final int DLG_CONFIRM_UNMOUNT = 1;
    private static final int DLG_ERROR_UNMOUNT = 2;

    // The mountToggle Preference that has last been clicked.
    // Assumes no two successive unmount event on 2 different volumes are performed before the first
    // one's preference is disabled
    private static Preference sLastClickedMountToggle;
    private static String sClickedMountPoint;

    // Access using getMountService()
    private IMountService mMountService = null;
    private StorageManager mStorageManager = null;
    private UsbManager mUsbManager = null;

    private ArrayList<StorageVolumePreferenceCategory> mCategories = Lists.newArrayList();

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);

        mUsbManager = (UsbManager)getSystemService(Context.USB_SERVICE);

        if (mStorageManager == null) {
            mStorageManager = (StorageManager) getSystemService(Context.STORAGE_SERVICE);
            mStorageManager.registerListener(mStorageListener);
        }

        addPreferencesFromResource(R.xml.device_info_memory);

        if (!Environment.isExternalStorageEmulated()) {
            // External storage is separate from internal storage; need to
            // show internal storage as a separate item.
            addCategoryForVolume(null);
        }

        final StorageVolume[] storageVolumes = mStorageManager.getVolumeList();
        for (StorageVolume volume : storageVolumes) {
            addCategoryForVolume(volume);
        }

        setHasOptionsMenu(true);
    }

    private void addCategoryForVolume(StorageVolume volume) {
        final StorageVolumePreferenceCategory category = new StorageVolumePreferenceCategory(
                getActivity(), volume);
        mCategories.add(category);
        getPreferenceScreen().addPreference(category);
        category.init();
    }

    private boolean isMassStorageEnabled() {
        // mass storage is enabled if primary volume supports it
        final StorageVolume[] storageVolumes = mStorageManager.getVolumeList();
        return (storageVolumes.length > 0 && storageVolumes[0].allowMassStorage());
    }

    @Override
    public void onResume() {
        super.onResume();
        IntentFilter intentFilter = new IntentFilter(Intent.ACTION_MEDIA_SCANNER_STARTED);
        intentFilter.addAction(Intent.ACTION_MEDIA_SCANNER_FINISHED);
        intentFilter.addDataScheme("file");
        getActivity().registerReceiver(mMediaScannerReceiver, intentFilter);

        intentFilter = new IntentFilter();
        intentFilter.addAction(UsbManager.ACTION_USB_STATE);
        getActivity().registerReceiver(mMediaScannerReceiver, intentFilter);

        for (StorageVolumePreferenceCategory category : mCategories) {
            category.onResume();
        }
    }

    StorageEventListener mStorageListener = new StorageEventListener() {
        @Override
        public void onStorageStateChanged(String path, String oldState, String newState) {
            Log.i(TAG, "Received storage state changed notification that " + path +
                    " changed state from " + oldState + " to " + newState);
            for (StorageVolumePreferenceCategory category : mCategories) {
                final StorageVolume volume = category.getStorageVolume();
                if (volume != null && path.equals(volume.getPath())) {
                    category.onStorageStateChanged();
                    break;
                }
            }
        }
    };

    @Override
    public void onPause() {
        super.onPause();
        getActivity().unregisterReceiver(mMediaScannerReceiver);
        for (StorageVolumePreferenceCategory category : mCategories) {
            category.onPause();
        }
    }

    @Override
    public void onDestroy() {
        if (mStorageManager != null && mStorageListener != null) {
            mStorageManager.unregisterListener(mStorageListener);
        }
        super.onDestroy();
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.storage, menu);
    }

    @Override
    public void onPrepareOptionsMenu(Menu menu) {
        final MenuItem usb = menu.findItem(R.id.storage_usb);
        usb.setVisible(!isMassStorageEnabled());
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.storage_usb:
                if (getActivity() instanceof PreferenceActivity) {
                    ((PreferenceActivity) getActivity()).startPreferencePanel(
                            UsbSettings.class.getCanonicalName(),
                            null,
                            R.string.storage_title_usb, null,
                            this, 0);
                } else {
                    startFragment(this, UsbSettings.class.getCanonicalName(), -1, null);
                }
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private synchronized IMountService getMountService() {
       if (mMountService == null) {
           IBinder service = ServiceManager.getService("mount");
           if (service != null) {
               mMountService = IMountService.Stub.asInterface(service);
           } else {
               Log.e(TAG, "Can't get mount service");
           }
       }
       return mMountService;
    }

    @Override
    public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen, Preference preference) {
        for (StorageVolumePreferenceCategory category : mCategories) {
            Intent intent = category.intentForClick(preference);
            if (intent != null) {
                // Don't go across app boundary if monkey is running
                if (!Utils.isMonkeyRunning()) {
                    startActivity(intent);
                }
                return true;
            }

            final StorageVolume volume = category.getStorageVolume();
            if (volume != null && category.mountToggleClicked(preference)) {
                sLastClickedMountToggle = preference;
                sClickedMountPoint = volume.getPath();
                String state = mStorageManager.getVolumeState(volume.getPath());
                if (Environment.MEDIA_MOUNTED.equals(state) ||
                        Environment.MEDIA_MOUNTED_READ_ONLY.equals(state)) {
                    unmount();
                } else {
                    mount();
                }
                return true;
            }
        }

        return false;
    }

    private final BroadcastReceiver mMediaScannerReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action.equals(UsbManager.ACTION_USB_STATE)) {
               boolean isUsbConnected = intent.getBooleanExtra(UsbManager.USB_CONNECTED, false);
               String usbFunction = mUsbManager.getDefaultFunction();
               for (StorageVolumePreferenceCategory category : mCategories) {
                   category.onUsbStateChanged(isUsbConnected, usbFunction);
               }
            } else if (action.equals(Intent.ACTION_MEDIA_SCANNER_FINISHED)) {
                for (StorageVolumePreferenceCategory category : mCategories) {
                    category.onMediaScannerFinished();
                }
            }
        }
    };

    @Override
    public Dialog onCreateDialog(int id) {
        switch (id) {
        case DLG_CONFIRM_UNMOUNT:
                return new AlertDialog.Builder(getActivity())
                    .setTitle(R.string.dlg_confirm_unmount_title)
                    .setPositiveButton(R.string.dlg_ok, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            doUnmount();
                        }})
                    .setNegativeButton(R.string.cancel, null)
                    .setMessage(R.string.dlg_confirm_unmount_text)
                    .create();
        case DLG_ERROR_UNMOUNT:
                return new AlertDialog.Builder(getActivity())
            .setTitle(R.string.dlg_error_unmount_title)
            .setNeutralButton(R.string.dlg_ok, null)
            .setMessage(R.string.dlg_error_unmount_text)
            .create();
        }
        return null;
    }

    private void doUnmount() {
        // Present a toast here
        Toast.makeText(getActivity(), R.string.unmount_inform_text, Toast.LENGTH_SHORT).show();
        IMountService mountService = getMountService();
        try {
            sLastClickedMountToggle.setEnabled(false);
            sLastClickedMountToggle.setTitle(getString(R.string.sd_ejecting_title));
            sLastClickedMountToggle.setSummary(getString(R.string.sd_ejecting_summary));
            mountService.unmountVolume(sClickedMountPoint, true, false);
        } catch (RemoteException e) {
            // Informative dialog to user that unmount failed.
            showDialogInner(DLG_ERROR_UNMOUNT);
        }
    }

    private void showDialogInner(int id) {
        removeDialog(id);
        showDialog(id);
    }

    private boolean hasAppsAccessingStorage() throws RemoteException {
        IMountService mountService = getMountService();
        int stUsers[] = mountService.getStorageUsers(sClickedMountPoint);
        if (stUsers != null && stUsers.length > 0) {
            return true;
        }
        // TODO FIXME Parameterize with mountPoint and uncomment.
        // On HC-MR2, no apps can be installed on sd and the emulated internal storage is not
        // removable: application cannot interfere with unmount
        /*
        ActivityManager am = (ActivityManager)getSystemService(Context.ACTIVITY_SERVICE);
        List<ApplicationInfo> list = am.getRunningExternalApplications();
        if (list != null && list.size() > 0) {
            return true;
        }
        */
        // Better safe than sorry. Assume the storage is used to ask for confirmation.
        return true;
    }

    private void unmount() {
        // Check if external media is in use.
        try {
           if (hasAppsAccessingStorage()) {
               // Present dialog to user
               showDialogInner(DLG_CONFIRM_UNMOUNT);
           } else {
               doUnmount();
           }
        } catch (RemoteException e) {
            // Very unlikely. But present an error dialog anyway
            Log.e(TAG, "Is MountService running?");
            showDialogInner(DLG_ERROR_UNMOUNT);
        }
    }

    private void mount() {
        IMountService mountService = getMountService();
        try {
            if (mountService != null) {
                mountService.mountVolume(sClickedMountPoint);
            } else {
                Log.e(TAG, "Mount service is null, can't mount");
            }
        } catch (RemoteException ex) {
            // Not much can be done
        }
    }
}
