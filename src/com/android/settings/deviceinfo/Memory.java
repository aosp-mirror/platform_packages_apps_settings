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
import android.content.res.Resources;
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
import android.preference.PreferenceScreen;
import android.util.Log;
import android.widget.Toast;

import com.android.settings.R;
import com.android.settings.SettingsPreferenceFragment;

public class Memory extends SettingsPreferenceFragment {
    private static final String TAG = "MemorySettings";

    private static final int DLG_CONFIRM_UNMOUNT = 1;
    private static final int DLG_ERROR_UNMOUNT = 2;

    private Resources mResources;

    // The mountToggle Preference that has last been clicked.
    // Assumes no two successive unmount event on 2 different volumes are performed before the first
    // one's preference is disabled
    private Preference mLastClickedMountToggle;
    private String mClickedMountPoint;
    
    // Access using getMountService()
    private IMountService mMountService = null;

    private StorageManager mStorageManager = null;

    private StorageVolumePreferenceCategory mInternalStorageVolumePreferenceCategory;
    private StorageVolumePreferenceCategory[] mStorageVolumePreferenceCategories;

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);

        if (mStorageManager == null) {
            mStorageManager = (StorageManager) getSystemService(Context.STORAGE_SERVICE);
            mStorageManager.registerListener(mStorageListener);
        }

        addPreferencesFromResource(R.xml.device_info_memory);

        mResources = getResources();

        if (!Environment.isExternalStorageEmulated()) {
            // External storage is separate from internal storage; need to
            // show internal storage as a separate item.
            StorageVolumePreferenceCategory storagePreferenceCategory =
                new StorageVolumePreferenceCategory(getActivity(), mResources, null,
                        mStorageManager, true);
            mInternalStorageVolumePreferenceCategory = storagePreferenceCategory;
            getPreferenceScreen().addPreference(storagePreferenceCategory);
            storagePreferenceCategory.init();
        }

        StorageVolume[] storageVolumes = mStorageManager.getVolumeList();
        int length = storageVolumes.length;
        mStorageVolumePreferenceCategories = new StorageVolumePreferenceCategory[length];
        for (int i = 0; i < length; i++) {
            StorageVolume storageVolume = storageVolumes[i];
            StorageVolumePreferenceCategory storagePreferenceCategory =
                new StorageVolumePreferenceCategory(getActivity(), mResources, storageVolume,
                        mStorageManager, i == 0); // The first volume is the primary volume
            mStorageVolumePreferenceCategories[i] = storagePreferenceCategory;
            getPreferenceScreen().addPreference(storagePreferenceCategory);
            storagePreferenceCategory.init();
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        IntentFilter intentFilter = new IntentFilter(Intent.ACTION_MEDIA_SCANNER_STARTED);
        intentFilter.addAction(Intent.ACTION_MEDIA_SCANNER_FINISHED);
        intentFilter.addDataScheme("file");
        getActivity().registerReceiver(mMediaScannerReceiver, intentFilter);

        if (mInternalStorageVolumePreferenceCategory != null) {
            mInternalStorageVolumePreferenceCategory.onResume();
        }
        for (int i = 0; i < mStorageVolumePreferenceCategories.length; i++) {
            mStorageVolumePreferenceCategories[i].onResume();
        }
    }

    StorageEventListener mStorageListener = new StorageEventListener() {
        @Override
        public void onStorageStateChanged(String path, String oldState, String newState) {
            Log.i(TAG, "Received storage state changed notification that " +
                    path + " changed state from " + oldState +
                    " to " + newState);
            for (int i = 0; i < mStorageVolumePreferenceCategories.length; i++) {
                StorageVolumePreferenceCategory svpc = mStorageVolumePreferenceCategories[i];
                if (path.equals(svpc.getStorageVolume().getPath())) {
                    svpc.onStorageStateChanged();
                    break;
                }
            }
        }
    };

    @Override
    public void onPause() {
        super.onPause();
        getActivity().unregisterReceiver(mMediaScannerReceiver);
        if (mInternalStorageVolumePreferenceCategory != null) {
            mInternalStorageVolumePreferenceCategory.onPause();
        }
        for (int i = 0; i < mStorageVolumePreferenceCategories.length; i++) {
            mStorageVolumePreferenceCategories[i].onPause();
        }
    }

    @Override
    public void onDestroy() {
        if (mStorageManager != null && mStorageListener != null) {
            mStorageManager.unregisterListener(mStorageListener);
        }
        super.onDestroy();
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
        for (int i = 0; i < mStorageVolumePreferenceCategories.length; i++) {
            StorageVolumePreferenceCategory svpc = mStorageVolumePreferenceCategories[i];
            Intent intent = svpc.intentForClick(preference);
            if (intent != null) {
                startActivity(intent);
                return true;
            }

            if (svpc.mountToggleClicked(preference)) {
                mLastClickedMountToggle = preference;
                final StorageVolume storageVolume = svpc.getStorageVolume();
                mClickedMountPoint = storageVolume.getPath();
                String state = mStorageManager.getVolumeState(storageVolume.getPath());
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
            for (int i = 0; i < mStorageVolumePreferenceCategories.length; i++) {
                mStorageVolumePreferenceCategories[i].onMediaScannerFinished();
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
            mLastClickedMountToggle.setEnabled(false);
            mLastClickedMountToggle.setTitle(mResources.getString(R.string.sd_ejecting_title));
            mLastClickedMountToggle.setSummary(mResources.getString(R.string.sd_ejecting_summary));
            mountService.unmountVolume(mClickedMountPoint, true);
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
        int stUsers[] = mountService.getStorageUsers(mClickedMountPoint);
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
                mountService.mountVolume(mClickedMountPoint);
            } else {
                Log.e(TAG, "Mount service is null, can't mount");
            }
        } catch (RemoteException ex) {
            // Not much can be done
        }
    }
}
