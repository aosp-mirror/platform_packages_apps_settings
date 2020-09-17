/*
 * Copyright (C) 2015 The Android Open Source Project
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

import static com.android.settingslib.RestrictedLockUtils.EnforcedAdmin;

import android.app.Dialog;
import android.app.settings.SettingsEnums;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.UserHandle;
import android.os.UserManager;
import android.os.storage.DiskInfo;
import android.os.storage.StorageEventListener;
import android.os.storage.StorageManager;
import android.os.storage.VolumeInfo;
import android.os.storage.VolumeRecord;
import android.text.TextUtils;
import android.text.format.Formatter;
import android.text.format.Formatter.BytesResult;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.VisibleForTesting;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.preference.Preference;
import androidx.preference.PreferenceCategory;

import com.android.settings.R;
import com.android.settings.SettingsPreferenceFragment;
import com.android.settings.core.SubSettingLauncher;
import com.android.settings.core.instrumentation.InstrumentedDialogFragment;
import com.android.settings.search.BaseSearchIndexProvider;
import com.android.settingslib.RestrictedLockUtils;
import com.android.settingslib.RestrictedLockUtilsInternal;
import com.android.settingslib.deviceinfo.PrivateStorageInfo;
import com.android.settingslib.deviceinfo.StorageManagerVolumeProvider;
import com.android.settingslib.search.Indexable;
import com.android.settingslib.search.SearchIndexable;
import com.android.settingslib.search.SearchIndexableRaw;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Panel showing both internal storage (both built-in storage and private
 * volumes) and removable storage (public volumes).
 */
@SearchIndexable
public class StorageSettings extends SettingsPreferenceFragment implements Indexable {
    static final String TAG = "StorageSettings";

    private static final String KEY_STORAGE_SETTINGS = "storage_settings";
    private static final String KEY_INTERNAL_STORAGE = "storage_settings_internal_storage";
    private static final String KEY_STORAGE_SETTINGS_VOLUME = "storage_settings_volume_";
    private static final String KEY_STORAGE_SETTINGS_MEMORY_SIZE = "storage_settings_memory_size";
    private static final String KEY_STORAGE_SETTINGS_MEMORY = "storage_settings_memory_available";
    private static final String KEY_STORAGE_SETTINGS_DCIM = "storage_settings_dcim_space";
    private static final String KEY_STORAGE_SETTINGS_MUSIC = "storage_settings_music_space";
    private static final String KEY_STORAGE_SETTINGS_MISC = "storage_settings_misc_space";
    private static final String KEY_STORAGE_SETTINGS_FREE_SPACE = "storage_settings_free_space";

    private static final String TAG_VOLUME_UNMOUNTED = "volume_unmounted";
    private static final String TAG_DISK_INIT = "disk_init";
    private static final int METRICS_CATEGORY = SettingsEnums.DEVICEINFO_STORAGE;

    private StorageManager mStorageManager;

    private PreferenceCategory mInternalCategory;
    private PreferenceCategory mExternalCategory;

    private StorageSummaryPreference mInternalSummary;
    private static long sTotalInternalStorage;

    private boolean mHasLaunchedPrivateVolumeSettings = false;

    @Override
    public int getMetricsCategory() {
        return METRICS_CATEGORY;
    }

    @Override
    public int getHelpResource() {
        return R.string.help_uri_storage;
    }

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);

        final Context context = getActivity();

        mStorageManager = context.getSystemService(StorageManager.class);

        if (sTotalInternalStorage <= 0) {
            sTotalInternalStorage = mStorageManager.getPrimaryStorageSize();
        }

        addPreferencesFromResource(R.xml.device_info_storage);

        mInternalCategory = (PreferenceCategory) findPreference("storage_internal");
        mExternalCategory = (PreferenceCategory) findPreference("storage_external");

        mInternalSummary = new StorageSummaryPreference(getPrefContext());

        setHasOptionsMenu(true);
    }

    private final StorageEventListener mStorageListener = new StorageEventListener() {
        @Override
        public void onVolumeStateChanged(VolumeInfo vol, int oldState, int newState) {
            if (isInteresting(vol)) {
                refresh();
            }
        }

        @Override
        public void onDiskDestroyed(DiskInfo disk) {
            refresh();
        }
    };

    private static boolean isInteresting(VolumeInfo vol) {
        switch (vol.getType()) {
            case VolumeInfo.TYPE_PRIVATE:
            case VolumeInfo.TYPE_PUBLIC:
            case VolumeInfo.TYPE_STUB:
                return true;
            default:
                return false;
        }
    }

    private synchronized void refresh() {
        final Context context = getPrefContext();

        getPreferenceScreen().removeAll();
        mInternalCategory.removeAll();
        mExternalCategory.removeAll();

        mInternalCategory.addPreference(mInternalSummary);

        final StorageManagerVolumeProvider smvp = new StorageManagerVolumeProvider(mStorageManager);
        final PrivateStorageInfo info = PrivateStorageInfo.getPrivateStorageInfo(smvp);
        final long privateTotalBytes = info.totalBytes;
        final long privateUsedBytes = info.totalBytes - info.freeBytes;

        final List<VolumeInfo> volumes = mStorageManager.getVolumes();
        Collections.sort(volumes, VolumeInfo.getDescriptionComparator());

        for (VolumeInfo vol : volumes) {
            if (vol.getType() == VolumeInfo.TYPE_PRIVATE) {

                if (vol.getState() == VolumeInfo.STATE_UNMOUNTABLE) {
                    mInternalCategory.addPreference(
                            new StorageVolumePreference(context, vol, 0));
                } else {
                    final long volumeTotalBytes = PrivateStorageInfo.getTotalSize(vol,
                            sTotalInternalStorage);
                    mInternalCategory.addPreference(
                            new StorageVolumePreference(context, vol, volumeTotalBytes));
                }
            } else if (vol.getType() == VolumeInfo.TYPE_PUBLIC
                    || vol.getType() == VolumeInfo.TYPE_STUB) {
                mExternalCategory.addPreference(
                        new StorageVolumePreference(context, vol, 0));
            }
        }

        // Show missing private volumes
        final List<VolumeRecord> recs = mStorageManager.getVolumeRecords();
        for (VolumeRecord rec : recs) {
            if (rec.getType() == VolumeInfo.TYPE_PRIVATE
                    && mStorageManager.findVolumeByUuid(rec.getFsUuid()) == null) {
                // TODO: add actual storage type to record
                final Preference pref = new Preference(context);
                pref.setKey(rec.getFsUuid());
                pref.setTitle(rec.getNickname());
                pref.setSummary(com.android.internal.R.string.ext_media_status_missing);
                pref.setIcon(R.drawable.ic_sim_sd);
                mInternalCategory.addPreference(pref);
            }
        }

        // Show unsupported disks to give a chance to init
        final List<DiskInfo> disks = mStorageManager.getDisks();
        for (DiskInfo disk : disks) {
            if (disk.volumeCount == 0 && disk.size > 0) {
                final Preference pref = new Preference(context);
                pref.setKey(disk.getId());
                pref.setTitle(disk.getDescription());
                pref.setSummary(com.android.internal.R.string.ext_media_status_unsupported);
                pref.setIcon(R.drawable.ic_sim_sd);
                mExternalCategory.addPreference(pref);
            }
        }

        final BytesResult result = Formatter.formatBytes(getResources(), privateUsedBytes, 0);
        mInternalSummary.setTitle(TextUtils.expandTemplate(getText(R.string.storage_size_large),
                result.value, result.units));
        mInternalSummary.setSummary(getString(R.string.storage_volume_used_total,
                Formatter.formatFileSize(context, privateTotalBytes)));
        if (mInternalCategory.getPreferenceCount() > 0) {
            getPreferenceScreen().addPreference(mInternalCategory);
        }
        if (mExternalCategory.getPreferenceCount() > 0) {
            getPreferenceScreen().addPreference(mExternalCategory);
        }

        if (mInternalCategory.getPreferenceCount() == 2
                && mExternalCategory.getPreferenceCount() == 0) {
            // Only showing primary internal storage, so just shortcut
            if (!mHasLaunchedPrivateVolumeSettings) {
                mHasLaunchedPrivateVolumeSettings = true;
                final Bundle args = new Bundle();
                args.putString(VolumeInfo.EXTRA_VOLUME_ID, VolumeInfo.ID_PRIVATE_INTERNAL);
                new SubSettingLauncher(getActivity())
                        .setDestination(StorageDashboardFragment.class.getName())
                        .setArguments(args)
                        .setTitleRes(R.string.storage_settings)
                        .setSourceMetricsCategory(getMetricsCategory())
                        .launch();
                finish();
            }
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        mStorageManager.registerListener(mStorageListener);
        refresh();
    }

    @Override
    public void onPause() {
        super.onPause();
        mStorageManager.unregisterListener(mStorageListener);
    }

    @Override
    public boolean onPreferenceTreeClick(Preference pref) {
        final String key = pref.getKey();
        if (pref instanceof StorageVolumePreference) {
            // Picked a normal volume
            final VolumeInfo vol = mStorageManager.findVolumeById(key);

            if (vol == null) {
                return false;
            }

            if (vol.getState() == VolumeInfo.STATE_UNMOUNTED) {
                VolumeUnmountedFragment.show(this, vol.getId());
                return true;
            } else if (vol.getState() == VolumeInfo.STATE_UNMOUNTABLE) {
                DiskInitFragment.show(this, R.string.storage_dialog_unmountable, vol.getDiskId());
                return true;
            }

            if (vol.getType() == VolumeInfo.TYPE_PRIVATE) {
                final Bundle args = new Bundle();
                args.putString(VolumeInfo.EXTRA_VOLUME_ID, vol.getId());

                if (VolumeInfo.ID_PRIVATE_INTERNAL.equals(vol.getId())) {
                    new SubSettingLauncher(getContext())
                            .setDestination(StorageDashboardFragment.class.getCanonicalName())
                            .setTitleRes(R.string.storage_settings)
                            .setSourceMetricsCategory(getMetricsCategory())
                            .setArguments(args)
                            .launch();
                } else {
                    // TODO: Go to the StorageDashboardFragment once it fully handles all of the
                    //       SD card cases and other private internal storage cases.
                    PrivateVolumeSettings.setVolumeSize(args, PrivateStorageInfo.getTotalSize(vol,
                            sTotalInternalStorage));
                    new SubSettingLauncher(getContext())
                            .setDestination(PrivateVolumeSettings.class.getCanonicalName())
                            .setTitleRes(-1)
                            .setSourceMetricsCategory(getMetricsCategory())
                            .setArguments(args)
                            .launch();
                }

                return true;

            } else if (vol.getType() == VolumeInfo.TYPE_PUBLIC) {
                return handlePublicVolumeClick(getContext(), vol);
            } else if (vol.getType() == VolumeInfo.TYPE_STUB) {
                return handleStubVolumeClick(getContext(), vol);
            }

        } else if (key.startsWith("disk:")) {
            // Picked an unsupported disk
            DiskInitFragment.show(this, R.string.storage_dialog_unsupported, key);
            return true;

        } else {
            // Picked a missing private volume
            final Bundle args = new Bundle();
            args.putString(VolumeRecord.EXTRA_FS_UUID, key);
            new SubSettingLauncher(getContext())
                    .setDestination(PrivateVolumeForget.class.getCanonicalName())
                    .setTitleRes(R.string.storage_menu_forget)
                    .setSourceMetricsCategory(getMetricsCategory())
                    .setArguments(args)
                    .launch();
            return true;
        }

        return false;
    }

    @VisibleForTesting
    static boolean handleStubVolumeClick(Context context, VolumeInfo vol) {
        final Intent intent = vol.buildBrowseIntent();
        if (vol.isMountedReadable() && intent != null) {
            context.startActivity(intent);
            return true;
        }
        return false;
    }

    @VisibleForTesting
    static boolean handlePublicVolumeClick(Context context, VolumeInfo vol) {
        final Intent intent = vol.buildBrowseIntent();
        if (vol.isMountedReadable() && intent != null) {
            context.startActivity(intent);
            return true;
        } else {
            final Bundle args = new Bundle();
            args.putString(VolumeInfo.EXTRA_VOLUME_ID, vol.getId());
            new SubSettingLauncher(context)
                    .setDestination(PublicVolumeSettings.class.getCanonicalName())
                    .setTitleRes(-1)
                    .setSourceMetricsCategory(METRICS_CATEGORY)
                    .setArguments(args)
                    .launch();
            return true;
        }
    }

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

    public static class VolumeUnmountedFragment extends InstrumentedDialogFragment {
        public static void show(Fragment parent, String volumeId) {
            final Bundle args = new Bundle();
            args.putString(VolumeInfo.EXTRA_VOLUME_ID, volumeId);

            final VolumeUnmountedFragment dialog = new VolumeUnmountedFragment();
            dialog.setArguments(args);
            dialog.setTargetFragment(parent, 0);
            dialog.show(parent.getFragmentManager(), TAG_VOLUME_UNMOUNTED);
        }

        @Override
        public int getMetricsCategory() {
            return SettingsEnums.DIALOG_VOLUME_UNMOUNT;
        }

        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            final Context context = getActivity();
            final StorageManager sm = context.getSystemService(StorageManager.class);

            final String volumeId = getArguments().getString(VolumeInfo.EXTRA_VOLUME_ID);
            final VolumeInfo vol = sm.findVolumeById(volumeId);

            final AlertDialog.Builder builder = new AlertDialog.Builder(context);
            builder.setMessage(TextUtils.expandTemplate(
                    getText(R.string.storage_dialog_unmounted), vol.getDisk().getDescription()));

            builder.setPositiveButton(R.string.storage_menu_mount,
                    new DialogInterface.OnClickListener() {
                        /**
                         * Check if an {@link
                         * RestrictedLockUtils#sendShowAdminSupportDetailsIntent admin
                         * details intent} should be shown for the restriction and show it.
                         *
                         * @param restriction The restriction to check
                         * @return {@code true} iff a intent was shown.
                         */
                        private boolean wasAdminSupportIntentShown(@NonNull String restriction) {
                            EnforcedAdmin admin = RestrictedLockUtilsInternal
                                    .checkIfRestrictionEnforced(getActivity(), restriction,
                                            UserHandle.myUserId());
                            boolean hasBaseUserRestriction =
                                    RestrictedLockUtilsInternal.hasBaseUserRestriction(
                                            getActivity(), restriction, UserHandle.myUserId());
                            if (admin != null && !hasBaseUserRestriction) {
                                RestrictedLockUtils.sendShowAdminSupportDetailsIntent(getActivity(),
                                        admin);
                                return true;
                            }

                            return false;
                        }

                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            if (wasAdminSupportIntentShown(
                                    UserManager.DISALLOW_MOUNT_PHYSICAL_MEDIA)) {
                                return;
                            }

                            if (vol.disk != null && vol.disk.isUsb() &&
                                    wasAdminSupportIntentShown(
                                            UserManager.DISALLOW_USB_FILE_TRANSFER)) {
                                return;
                            }

                            new MountTask(context, vol).execute();
                        }
                    });
            builder.setNegativeButton(R.string.cancel, null);

            return builder.create();
        }
    }

    public static class DiskInitFragment extends InstrumentedDialogFragment {
        @Override
        public int getMetricsCategory() {
            return SettingsEnums.DIALOG_VOLUME_INIT;
        }

        public static void show(Fragment parent, int resId, String diskId) {
            final Bundle args = new Bundle();
            args.putInt(Intent.EXTRA_TEXT, resId);
            args.putString(DiskInfo.EXTRA_DISK_ID, diskId);

            final DiskInitFragment dialog = new DiskInitFragment();
            dialog.setArguments(args);
            dialog.setTargetFragment(parent, 0);
            dialog.show(parent.getFragmentManager(), TAG_DISK_INIT);
        }

        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            final Context context = getActivity();
            final StorageManager sm = context.getSystemService(StorageManager.class);

            final int resId = getArguments().getInt(Intent.EXTRA_TEXT);
            final String diskId = getArguments().getString(DiskInfo.EXTRA_DISK_ID);
            final DiskInfo disk = sm.findDiskById(diskId);

            final AlertDialog.Builder builder = new AlertDialog.Builder(context);
            builder.setMessage(TextUtils.expandTemplate(getText(resId), disk.getDescription()));

            builder.setPositiveButton(R.string.storage_menu_set_up,
                    new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            final Intent intent = new Intent(context, StorageWizardInit.class);
                            intent.putExtra(DiskInfo.EXTRA_DISK_ID, diskId);
                            startActivity(intent);
                        }
                    });
            builder.setNegativeButton(R.string.cancel, null);

            return builder.create();
        }
    }

    /** Enable indexing of searchable data */
    public static final BaseSearchIndexProvider SEARCH_INDEX_DATA_PROVIDER =
            new BaseSearchIndexProvider() {
                @Override
                public List<SearchIndexableRaw> getRawDataToIndex(
                        Context context, boolean enabled) {
                    final List<SearchIndexableRaw> result = new ArrayList<>();

                    SearchIndexableRaw data = new SearchIndexableRaw(context);
                    data.title = context.getString(R.string.storage_settings);
                    data.key = KEY_STORAGE_SETTINGS;
                    data.screenTitle = context.getString(R.string.storage_settings);
                    data.keywords = context.getString(R.string.keywords_storage_settings);
                    result.add(data);

                    data = new SearchIndexableRaw(context);
                    data.title = context.getString(R.string.internal_storage);
                    data.key = KEY_INTERNAL_STORAGE;
                    data.screenTitle = context.getString(R.string.storage_settings);
                    result.add(data);

                    data = new SearchIndexableRaw(context);
                    final StorageManager storage = context.getSystemService(StorageManager.class);
                    final List<VolumeInfo> vols = storage.getVolumes();
                    for (VolumeInfo vol : vols) {
                        if (isInteresting(vol)) {
                            data.title = storage.getBestVolumeDescription(vol);
                            data.key = KEY_STORAGE_SETTINGS_VOLUME + vol.id;
                            data.screenTitle = context.getString(R.string.storage_settings);
                            result.add(data);
                        }
                    }

                    data = new SearchIndexableRaw(context);
                    data.title = context.getString(R.string.memory_size);
                    data.key = KEY_STORAGE_SETTINGS_MEMORY_SIZE;
                    data.screenTitle = context.getString(R.string.storage_settings);
                    result.add(data);

                    data = new SearchIndexableRaw(context);
                    data.title = context.getString(R.string.memory_available);
                    data.key = KEY_STORAGE_SETTINGS_MEMORY;
                    data.screenTitle = context.getString(R.string.storage_settings);
                    result.add(data);

                    data = new SearchIndexableRaw(context);
                    data.title = context.getString(R.string.memory_dcim_usage);
                    data.key = KEY_STORAGE_SETTINGS_DCIM;
                    data.screenTitle = context.getString(R.string.storage_settings);
                    result.add(data);

                    data = new SearchIndexableRaw(context);
                    data.title = context.getString(R.string.memory_music_usage);
                    data.key = KEY_STORAGE_SETTINGS_MUSIC;
                    data.screenTitle = context.getString(R.string.storage_settings);
                    result.add(data);

                    data = new SearchIndexableRaw(context);
                    data.title = context.getString(R.string.memory_media_misc_usage);
                    data.key = KEY_STORAGE_SETTINGS_MISC;
                    data.screenTitle = context.getString(R.string.storage_settings);
                    result.add(data);

                    data = new SearchIndexableRaw(context);
                    data.title = context.getString(R.string.storage_menu_free);
                    data.key = KEY_STORAGE_SETTINGS_FREE_SPACE;
                    data.screenTitle = context.getString(R.string.storage_menu_free);
                    data.intentAction = StorageManager.ACTION_MANAGE_STORAGE;
                    data.keywords = context.getString(R.string.keywords_storage_menu_free);
                    result.add(data);

                    return result;
                }

                @Override
                public List<String> getNonIndexableKeys(Context context) {
                    final List<String> niks = super.getNonIndexableKeys(context);
                    if (isExternalExist(context)) {
                        niks.add(KEY_STORAGE_SETTINGS);
                        niks.add(KEY_INTERNAL_STORAGE);
                        niks.add(KEY_STORAGE_SETTINGS_MEMORY_SIZE);
                        niks.add(KEY_STORAGE_SETTINGS_MEMORY);
                        niks.add(KEY_STORAGE_SETTINGS_DCIM);
                        niks.add(KEY_STORAGE_SETTINGS_MUSIC);
                        niks.add(KEY_STORAGE_SETTINGS_MISC);
                        niks.add(KEY_STORAGE_SETTINGS_FREE_SPACE);

                        final StorageManager storage = context.getSystemService(
                                StorageManager.class);
                        final List<VolumeInfo> vols = storage.getVolumes();
                        for (VolumeInfo vol : vols) {
                            if (isInteresting(vol)) {
                                niks.add(KEY_STORAGE_SETTINGS_VOLUME + vol.id);
                            }
                        }
                    }
                    return niks;
                }

                @Override
                protected boolean isPageSearchEnabled(Context context) {
                    return !isExternalExist(context);
                }

                private boolean isExternalExist(Context context) {
                    int internalCount = 0;
                    StorageManager storageManager = context.getSystemService(StorageManager.class);
                    final List<VolumeInfo> volumes = storageManager.getVolumes();
                    for (VolumeInfo vol : volumes) {
                        //External storage
                        if (vol.getType() == VolumeInfo.TYPE_PUBLIC
                                || vol.getType() == VolumeInfo.TYPE_STUB) {
                            return true;
                        } else if (vol.getType() == VolumeInfo.TYPE_PRIVATE) {
                            internalCount++;
                        }
                    }

                    // Unsupported disks
                    final List<DiskInfo> disks = storageManager.getDisks();
                    for (DiskInfo disk : disks) {
                        if (disk.volumeCount == 0 && disk.size > 0) {
                            return true;
                        }
                    }

                    // Missing private volumes
                    final List<VolumeRecord> recs = storageManager.getVolumeRecords();
                    for (VolumeRecord rec : recs) {
                        if (rec.getType() == VolumeInfo.TYPE_PRIVATE
                                && storageManager.findVolumeByUuid(rec.getFsUuid()) == null) {
                            internalCount++;
                        }
                    }

                    return (internalCount != 1);
                }
            };
}
