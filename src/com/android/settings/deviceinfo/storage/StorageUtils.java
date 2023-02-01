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
import android.content.Intent;
import android.content.pm.ResolveInfo;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.storage.DiskInfo;
import android.os.storage.StorageManager;
import android.os.storage.VolumeInfo;
import android.os.storage.VolumeRecord;
import android.text.TextUtils;
import android.text.format.Formatter;
import android.util.Log;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;

import com.android.settings.R;
import com.android.settings.Utils;
import com.android.settings.core.SubSettingLauncher;
import com.android.settings.core.instrumentation.InstrumentedDialogFragment;
import com.android.settings.deviceinfo.PrivateVolumeForget;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/** Storage utilities */
public class StorageUtils {

    private static final String TAG = "StorageUtils";

    /**
     * Collects and returns all kinds of StorageEntry which will show in Storage Settings.
     */
    public static List<StorageEntry> getAllStorageEntries(Context context,
            StorageManager storageManager) {
        final List<StorageEntry> storageEntries = new ArrayList<>();
        storageEntries.addAll(storageManager.getVolumes().stream()
                .filter(volumeInfo -> isStorageSettingsInterestedVolume(volumeInfo))
                .map(volumeInfo -> new StorageEntry(context, volumeInfo))
                .collect(Collectors.toList()));
        storageEntries.addAll(storageManager.getDisks().stream()
                .filter(disk -> isDiskUnsupported(disk))
                .map(disk -> new StorageEntry(disk))
                .collect(Collectors.toList()));
        storageEntries.addAll(storageManager.getVolumeRecords().stream()
                .filter(volumeRecord -> isVolumeRecordMissed(storageManager, volumeRecord))
                .map(volumeRecord -> new StorageEntry(volumeRecord))
                .collect(Collectors.toList()));
        return storageEntries;
    }

    /**
     * Returns true if the volumeInfo may be displayed in Storage Settings.
     */
    public static boolean isStorageSettingsInterestedVolume(VolumeInfo volumeInfo) {
        switch (volumeInfo.getType()) {
            case VolumeInfo.TYPE_PRIVATE:
            case VolumeInfo.TYPE_PUBLIC:
            case VolumeInfo.TYPE_STUB:
                return true;
            default:
                return false;
        }
    }

    /**
     * VolumeRecord is a metadata of VolumeInfo, this is the case where a VolumeInfo is missing.
     * (e.g., internal SD card is removed.)
     */
    public static boolean isVolumeRecordMissed(StorageManager storageManager,
            VolumeRecord volumeRecord) {
        return volumeRecord.getType() == VolumeInfo.TYPE_PRIVATE
                && storageManager.findVolumeByUuid(volumeRecord.getFsUuid()) == null;
    }

    /**
     * A unsupported disk is the disk of problem format, android is not able to mount automatically.
     */
    public static boolean isDiskUnsupported(DiskInfo disk) {
        return disk.volumeCount == 0 && disk.size > 0;
    }

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

    /** Returns size label of changing units. (e.g., 1kB, 2MB, 3GB) */
    public static String getStorageSizeLabel(Context context, long bytes) {
        final Formatter.BytesResult result = Formatter.formatBytes(context.getResources(),
                bytes, Formatter.FLAG_SHORTER);
        return TextUtils.expandTemplate(context.getText(R.string.storage_size_large),
                result.value, result.units).toString();
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

    /** Shows information about system storage. */
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
                            Build.VERSION.RELEASE_OR_PREVIEW_DISPLAY))
                    .setPositiveButton(android.R.string.ok, null)
                    .create();
        }
    }

    /** Gets a summary which has a byte size information. */
    public static String getStorageSummary(Context context, int resId, long bytes) {
        final Formatter.BytesResult result = Formatter.formatBytes(context.getResources(),
                bytes, Formatter.FLAG_SHORTER);
        return context.getString(resId, result.value, result.units);
    }

    /** Gets icon for Preference of Free up space. */
    public static Drawable getManageStorageIcon(Context context, int userId) {
        ResolveInfo resolveInfo = context.getPackageManager().resolveActivityAsUser(
                new Intent(StorageManager.ACTION_MANAGE_STORAGE), 0 /* flags */, userId);
        if (resolveInfo == null || resolveInfo.activityInfo == null) {
            return null;
        }

        return Utils.getBadgedIcon(context, resolveInfo.activityInfo.applicationInfo);
    }
}
