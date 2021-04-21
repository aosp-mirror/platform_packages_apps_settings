/*
 * Copyright (C) 2021 The Android Open Source Project
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

package com.android.settings.deviceinfo.storage;

import android.app.Dialog;
import android.app.settings.SettingsEnums;
import android.content.Context;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.storage.StorageManager;
import android.os.storage.VolumeInfo;
import android.os.storage.VolumeRecord;
import android.util.Log;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;

import com.android.settings.R;
import com.android.settings.core.SubSettingLauncher;
import com.android.settings.core.instrumentation.InstrumentedDialogFragment;
import com.android.settings.deviceinfo.PrivateVolumeForget;

/** Storage utilities */
public class StorageUtils {

    private static final String TAG = "StorageUtils";

    /** Launches the fragment to forget a specified missing volume record. */
    public static void launchForgetMissingVolumeRecordFragment(Context context,
            StorageEntry storageEntry) {
        if (storageEntry == null || !storageEntry.isVolumeRecordMissed()) {
            return;
        }

        final Bundle args = new Bundle();
        args.putString(VolumeRecord.EXTRA_FS_UUID, storageEntry.getFsUuid());
        new SubSettingLauncher(context)
                .setDestination(PrivateVolumeForget.class.getCanonicalName())
                .setTitleRes(R.string.storage_menu_forget)
                .setSourceMetricsCategory(SettingsEnums.SETTINGS_STORAGE_CATEGORY)
                .setArguments(args)
                .launch();
    }

    /** An AsyncTask to unmount a specified volume. */
    public static class UnmountTask extends AsyncTask<Void, Void, Exception> {
        private final Context mContext;
        private final StorageManager mStorageManager;
        private final String mVolumeId;
        private final String mDescription;

        public UnmountTask(Context context, VolumeInfo volume) {
            mContext = context.getApplicationContext();
            mStorageManager = mContext.getSystemService(StorageManager.class);
            mVolumeId = volume.getId();
            mDescription = mStorageManager.getBestVolumeDescription(volume);
        }

        @Override
        protected Exception doInBackground(Void... params) {
            try {
                mStorageManager.unmount(mVolumeId);
                return null;
            } catch (Exception e) {
                return e;
            }
        }

        @Override
        protected void onPostExecute(Exception e) {
            if (e == null) {
                Toast.makeText(mContext, mContext.getString(R.string.storage_unmount_success,
                        mDescription), Toast.LENGTH_SHORT).show();
            } else {
                Log.e(TAG, "Failed to unmount " + mVolumeId, e);
                Toast.makeText(mContext, mContext.getString(R.string.storage_unmount_failure,
                        mDescription), Toast.LENGTH_SHORT).show();
            }
        }
    }

    /** An AsyncTask to mount a specified volume. */
    public static class MountTask extends AsyncTask<Void, Void, Exception> {
        private final Context mContext;
        private final StorageManager mStorageManager;
        private final String mVolumeId;
        private final String mDescription;

        public MountTask(Context context, VolumeInfo volume) {
            mContext = context.getApplicationContext();
            mStorageManager = mContext.getSystemService(StorageManager.class);
            mVolumeId = volume.getId();
            mDescription = mStorageManager.getBestVolumeDescription(volume);
        }

        @Override
        protected Exception doInBackground(Void... params) {
            try {
                mStorageManager.mount(mVolumeId);
                return null;
            } catch (Exception e) {
                return e;
            }
        }

        @Override
        protected void onPostExecute(Exception e) {
            if (e == null) {
                Toast.makeText(mContext, mContext.getString(R.string.storage_mount_success,
                        mDescription), Toast.LENGTH_SHORT).show();
            } else {
                Log.e(TAG, "Failed to mount " + mVolumeId, e);
                Toast.makeText(mContext, mContext.getString(R.string.storage_mount_failure,
                        mDescription), Toast.LENGTH_SHORT).show();
            }
        }
    }

    /* Shows information about system storage. */
    public static class SystemInfoFragment extends InstrumentedDialogFragment {
        /** Shows the fragment. */
        public static void show(Fragment parent) {
            if (!parent.isAdded()) return;

            final SystemInfoFragment dialog = new SystemInfoFragment();
            dialog.setTargetFragment(parent, 0);
            dialog.show(parent.getFragmentManager(), "systemInfo");
        }

        @Override
        public int getMetricsCategory() {
            return SettingsEnums.DIALOG_STORAGE_SYSTEM_INFO;
        }

        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            return new AlertDialog.Builder(getActivity())
                    .setMessage(getContext().getString(R.string.storage_detail_dialog_system,
                            Build.VERSION.RELEASE_OR_CODENAME))
                    .setPositiveButton(android.R.string.ok, null)
                    .create();
        }
    }
}
