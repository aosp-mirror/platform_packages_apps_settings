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

import android.app.ActivityManagerNative;
import android.app.ActivityThread;
import android.app.DownloadManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.IPackageManager;
import android.content.pm.UserInfo;
import android.content.res.Resources;
import android.hardware.usb.UsbManager;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.os.RemoteException;
import android.os.UserHandle;
import android.os.UserManager;
import android.os.storage.StorageManager;
import android.os.storage.StorageVolume;
import android.preference.Preference;
import android.preference.PreferenceCategory;
import android.text.format.Formatter;

import com.android.settings.R;
import com.android.settings.deviceinfo.StorageMeasurement.MeasurementReceiver;
import com.google.android.collect.Lists;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class StorageVolumePreferenceCategory extends PreferenceCategory
        implements MeasurementReceiver {
    private static final String KEY_TOTAL_SIZE = "total_size";
    private static final String KEY_APPLICATIONS = "applications";
    private static final String KEY_DCIM = "dcim"; // Pictures and Videos
    private static final String KEY_MUSIC = "music";
    private static final String KEY_DOWNLOADS = "downloads";
    private static final String KEY_MISC = "misc";
    private static final String KEY_AVAILABLE = "available";
    private static final String KEY_USER_PREFIX = "user";

    private static final int ORDER_USAGE_BAR = -2;
    private static final int ORDER_STORAGE_LOW = -1;

    private UsageBarPreference mUsageBarPreference;
    private Preference mMountTogglePreference;
    private Preference mFormatPreference;
    private Preference mStorageLow;

    private final Resources mResources;

    private final StorageVolume mStorageVolume;
    private final StorageManager mStorageManager;

    /** Measurement for local user. */
    private StorageMeasurement mLocalMeasure;
    /** All used measurements, including other users. */
    private List<StorageMeasurement> mAllMeasures = Lists.newArrayList();

    private boolean mAllowFormat;
    private final boolean mMeasureUsers;

    private boolean mUsbConnected;
    private String mUsbFunction;
    private boolean mShowingApprox;

    public static final Set<String> sPathsExcludedForMisc = new HashSet<String>();

    static class MediaCategory {
        final String[] mDirPaths;
        final String mCategory;

        public MediaCategory(String category, String... directories) {
            mCategory = category;
            final int length = directories.length;
            mDirPaths = new String[length];
            for (int i = 0; i < length; i++) {
                final String name = directories[i];
                final String path = Environment.getExternalStoragePublicDirectory(name).
                        getAbsolutePath();
                mDirPaths[i] = path;
                sPathsExcludedForMisc.add(path);
            }
        }
    }

    static final MediaCategory[] sMediaCategories = new MediaCategory[] {
        new MediaCategory(KEY_DCIM, Environment.DIRECTORY_DCIM, Environment.DIRECTORY_MOVIES,
                Environment.DIRECTORY_PICTURES),
        new MediaCategory(KEY_MUSIC, Environment.DIRECTORY_MUSIC, Environment.DIRECTORY_ALARMS,
                Environment.DIRECTORY_NOTIFICATIONS, Environment.DIRECTORY_RINGTONES,
                Environment.DIRECTORY_PODCASTS)
    };

    static {
        // Downloads
        sPathsExcludedForMisc.add(Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_DOWNLOADS).getAbsolutePath());
        // Apps
        sPathsExcludedForMisc.add(Environment.getExternalStorageDirectory().getAbsolutePath() +
                "/Android");
    }

    // Updates the memory usage bar graph.
    private static final int MSG_UI_UPDATE_APPROXIMATE = 1;

    // Updates the memory usage bar graph.
    private static final int MSG_UI_UPDATE_EXACT = 2;

    private Handler mUpdateHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MSG_UI_UPDATE_APPROXIMATE: {
                    final UserHandle user = (UserHandle) msg.obj;
                    final Bundle bundle = msg.getData();
                    final long totalSize = bundle.getLong(StorageMeasurement.TOTAL_SIZE);
                    final long availSize = bundle.getLong(StorageMeasurement.AVAIL_SIZE);

                    if (user.getIdentifier() == UserHandle.USER_CURRENT) {
                        updateApproximate(totalSize, availSize);
                    }
                    break;
                }
                case MSG_UI_UPDATE_EXACT: {
                    final UserHandle user = (UserHandle) msg.obj;
                    final Bundle bundle = msg.getData();
                    final long totalSize = bundle.getLong(StorageMeasurement.TOTAL_SIZE);
                    final long availSize = bundle.getLong(StorageMeasurement.AVAIL_SIZE);
                    final long appsUsed = bundle.getLong(StorageMeasurement.APPS_USED);
                    final long downloadsSize = bundle.getLong(StorageMeasurement.DOWNLOADS_SIZE);
                    final long miscSize = bundle.getLong(StorageMeasurement.MISC_SIZE);
                    final long[] mediaSizes = bundle.getLongArray(StorageMeasurement.MEDIA_SIZES);

                    if (user.getIdentifier() == UserHandle.USER_CURRENT) {
                        updateExact(totalSize, availSize, appsUsed, downloadsSize, miscSize,
                                mediaSizes);
                    } else {
                        long usedSize = appsUsed + downloadsSize + miscSize;
                        for (long mediaSize : mediaSizes) {
                            usedSize += mediaSize;
                        }
                        updateUserExact(user, totalSize, usedSize);
                    }
                    break;
                }
            }
        }
    };

    public StorageVolumePreferenceCategory(Context context, Resources resources,
            StorageVolume storageVolume, StorageManager storageManager, boolean isPrimary) {
        super(context);
        mResources = resources;
        mStorageVolume = storageVolume;
        mStorageManager = storageManager;
        setTitle(storageVolume != null ? storageVolume.getDescription(context)
                : resources.getText(R.string.internal_storage));
        mLocalMeasure = StorageMeasurement.getInstance(
                context, storageVolume, new UserHandle(UserHandle.USER_CURRENT), isPrimary);
        mAllMeasures.add(mLocalMeasure);

        // Cannot format emulated storage
        mAllowFormat = mStorageVolume != null && !mStorageVolume.isEmulated();
        // For now we are disabling reformatting secondary external storage
        // until some interoperability problems with MTP are fixed
        if (!isPrimary) mAllowFormat = false;

        // Measure other users when showing primary emulated storage
        mMeasureUsers = (mStorageVolume != null && mStorageVolume.isEmulated()) && isPrimary;
    }

    private void addStorageItem(String key, int titleRes, int colorRes) {
        addPreference(new StorageItemPreference(getContext(), key, titleRes, colorRes));
    }

    private static String buildUserKey(UserHandle user) {
        return KEY_USER_PREFIX + user.getIdentifier();
    }

    public void init() {
        final Context context = getContext();

        mUsageBarPreference = new UsageBarPreference(context);
        mUsageBarPreference.setOrder(ORDER_USAGE_BAR);
        addPreference(mUsageBarPreference);

        addStorageItem(KEY_TOTAL_SIZE, R.string.memory_size, 0);
        addStorageItem(KEY_APPLICATIONS, R.string.memory_apps_usage, R.color.memory_apps_usage);
        addStorageItem(KEY_DCIM, R.string.memory_dcim_usage, R.color.memory_dcim);
        addStorageItem(KEY_MUSIC, R.string.memory_music_usage, R.color.memory_music);
        addStorageItem(KEY_DOWNLOADS, R.string.memory_downloads_usage, R.color.memory_downloads);
        addStorageItem(KEY_MISC, R.string.memory_media_misc_usage, R.color.memory_misc);
        addStorageItem(KEY_AVAILABLE, R.string.memory_available, R.color.memory_avail);

        if (mMeasureUsers) {
            final UserManager userManager = (UserManager) context.getSystemService(
                    Context.USER_SERVICE);

            final UserInfo currentUser;
            try {
                currentUser = ActivityManagerNative.getDefault().getCurrentUser();
            } catch (RemoteException e) {
                throw new RuntimeException("Failed to get current user");
            }

            int count = 0;
            for (UserInfo info : userManager.getUsers()) {
                // Only measure other users
                if (info.id == currentUser.id) {
                    continue;
                }

                final UserHandle user = new UserHandle(info.id);
                final String key = buildUserKey(user);

                final StorageMeasurement measure = StorageMeasurement.getInstance(
                        context, mStorageVolume, user, true);
                measure.setIncludeAppCodeSize(false);
                mAllMeasures.add(measure);

                final int colorRes = count++ % 2 == 0 ? R.color.memory_user_light
                        : R.color.memory_user_dark;
                addPreference(new StorageItemPreference(getContext(), key, info.name, colorRes));
            }
        }

        mMountTogglePreference = new Preference(context);
        mMountTogglePreference.setTitle(R.string.sd_eject);
        mMountTogglePreference.setSummary(R.string.sd_eject_summary);
        addPreference(mMountTogglePreference);

        if (mAllowFormat) {
            mFormatPreference = new Preference(context);
            mFormatPreference.setTitle(R.string.sd_format);
            mFormatPreference.setSummary(R.string.sd_format_summary);
            addPreference(mFormatPreference);
        }

        final IPackageManager pm = ActivityThread.getPackageManager();
        try {
            if (pm.isStorageLow()) {
                mStorageLow = new Preference(context);
                mStorageLow.setOrder(ORDER_STORAGE_LOW);
                mStorageLow.setTitle(R.string.storage_low_title);
                mStorageLow.setSummary(R.string.storage_low_summary);
                addPreference(mStorageLow);
            } else if (mStorageLow != null) {
                removePreference(mStorageLow);
                mStorageLow = null;
            }
        } catch (RemoteException e) {
        }
    }

    public StorageVolume getStorageVolume() {
        return mStorageVolume;
    }

    private void updatePreferencesFromState() {
        mMountTogglePreference.setEnabled(true);

        String state = mStorageVolume != null
                ? mStorageManager.getVolumeState(mStorageVolume.getPath())
                : Environment.MEDIA_MOUNTED;

        String readOnly = "";
        if (Environment.MEDIA_MOUNTED_READ_ONLY.equals(state)) {
            state = Environment.MEDIA_MOUNTED;
            readOnly = mResources.getString(R.string.read_only);
            if (mFormatPreference != null) {
                removePreference(mFormatPreference);
            }
        }

        if ((mStorageVolume == null || !mStorageVolume.isRemovable())
                && !Environment.MEDIA_UNMOUNTED.equals(state)) {
            // This device has built-in storage that is not removable.
            // There is no reason for the user to unmount it.
            removePreference(mMountTogglePreference);
        }

        if (Environment.MEDIA_MOUNTED.equals(state)) {
            final Preference pref = findPreference(KEY_AVAILABLE);
            pref.setSummary(pref.getSummary() + readOnly);

            mMountTogglePreference.setEnabled(true);
            mMountTogglePreference.setTitle(mResources.getString(R.string.sd_eject));
            mMountTogglePreference.setSummary(mResources.getString(R.string.sd_eject_summary));
        } else {
            if (Environment.MEDIA_UNMOUNTED.equals(state) || Environment.MEDIA_NOFS.equals(state)
                    || Environment.MEDIA_UNMOUNTABLE.equals(state)) {
                mMountTogglePreference.setEnabled(true);
                mMountTogglePreference.setTitle(mResources.getString(R.string.sd_mount));
                mMountTogglePreference.setSummary(mResources.getString(R.string.sd_mount_summary));
            } else {
                mMountTogglePreference.setEnabled(false);
                mMountTogglePreference.setTitle(mResources.getString(R.string.sd_mount));
                mMountTogglePreference.setSummary(mResources.getString(R.string.sd_insert_summary));
            }

            removePreference(mUsageBarPreference);
            removePreference(findPreference(KEY_TOTAL_SIZE));
            removePreference(findPreference(KEY_AVAILABLE));
            if (mFormatPreference != null) {
                removePreference(mFormatPreference);
            }
        }

        if (mUsbConnected && (UsbManager.USB_FUNCTION_MTP.equals(mUsbFunction) ||
                UsbManager.USB_FUNCTION_PTP.equals(mUsbFunction))) {
            mMountTogglePreference.setEnabled(false);
            if (Environment.MEDIA_MOUNTED.equals(state)) {
                mMountTogglePreference.setSummary(mResources.getString(R.string.mtp_ptp_mode_summary));
            }

            if (mFormatPreference != null) {
                mFormatPreference.setEnabled(false);
                mFormatPreference.setSummary(mResources.getString(R.string.mtp_ptp_mode_summary));
            }
        } else if (mFormatPreference != null) {
            mFormatPreference.setEnabled(true);
            mFormatPreference.setSummary(mResources.getString(R.string.sd_format_summary));
        }

    }

    public void updateApproximate(long totalSize, long availSize) {
        findPreference(KEY_TOTAL_SIZE).setSummary(formatSize(totalSize));
        findPreference(KEY_AVAILABLE).setSummary(formatSize(availSize));

        final long usedSize = totalSize - availSize;

        mUsageBarPreference.clear();
        mUsageBarPreference.addEntry(usedSize / (float) totalSize, android.graphics.Color.GRAY);
        mUsageBarPreference.commit();
        mShowingApprox = true;

        updatePreferencesFromState();
    }

    public void updateExact(long totalSize, long availSize, long appsSize, long downloadsSize,
            long miscSize, long[] mediaSizes) {
        if (mShowingApprox) {
            mUsageBarPreference.clear();
            mShowingApprox = false;
        }

        findPreference(KEY_TOTAL_SIZE).setSummary(formatSize(totalSize));

        if (mLocalMeasure.isExternalSDCard()) {
            // TODO FIXME: external SD card will not report any size. Show used space in bar graph
            final long usedSize = totalSize - availSize;
            mUsageBarPreference.addEntry(usedSize / (float) totalSize, android.graphics.Color.GRAY);
        }

        updatePreference(appsSize, totalSize, KEY_APPLICATIONS);

        long totalMediaSize = 0;
        for (int i = 0; i < sMediaCategories.length; i++) {
            final String category = sMediaCategories[i].mCategory;
            final long size = mediaSizes[i];
            updatePreference(size, totalSize, category);
            totalMediaSize += size;
        }

        updatePreference(downloadsSize, totalSize, KEY_DOWNLOADS);

        // Note miscSize != totalSize - availSize - appsSize - downloadsSize - totalMediaSize
        // Block size is taken into account. That can be extra space from folders. TODO Investigate
        updatePreference(miscSize, totalSize, KEY_MISC);

        updatePreference(availSize, totalSize, KEY_AVAILABLE, false);

        mUsageBarPreference.commit();
    }

    public void updateUserExact(UserHandle user, long totalSize, long usedSize) {
        if (mShowingApprox) {
            mUsageBarPreference.clear();
            mShowingApprox = false;
        }

        final String key = buildUserKey(user);

        findPreference(key).setSummary(formatSize(usedSize));
        updatePreference(usedSize, totalSize, key);

        mUsageBarPreference.commit();
    }

    private void updatePreference(long size, long totalSize, String category) {
        updatePreference(size, totalSize, category, true);
    }

    private void updatePreference(long size, long totalSize, String category, boolean showBar) {
        final StorageItemPreference pref = (StorageItemPreference) findPreference(category);
        if (pref != null) {
            if (size > 0) {
                pref.setSummary(formatSize(size));
                if (showBar) {
                    mUsageBarPreference.addEntry(size / (float) totalSize, pref.getColor());
                }
            } else {
                removePreference(pref);
            }
        }
    }

    private void measure() {
        for (StorageMeasurement measure : mAllMeasures) {
            measure.invalidate();
            measure.measure();
        }
    }

    public void onResume() {
        for (StorageMeasurement measure : mAllMeasures) {
            measure.setReceiver(this);
        }
        measure();
    }

    public void onStorageStateChanged() {
        measure();
    }

    public void onUsbStateChanged(boolean isUsbConnected, String usbFunction) {
        mUsbConnected = isUsbConnected;
        mUsbFunction = usbFunction;
        measure();
    }
    public void onMediaScannerFinished() {
        measure();
    }

    public void onPause() {
        for (StorageMeasurement measure : mAllMeasures) {
            measure.cleanUp();
        }
    }

    private String formatSize(long size) {
        return Formatter.formatFileSize(getContext(), size);
    }

    @Override
    public void updateApproximate(StorageMeasurement meas, Bundle bundle) {
        final Message message = mUpdateHandler.obtainMessage(MSG_UI_UPDATE_APPROXIMATE);
        message.obj = meas.getUser();
        message.setData(bundle);
        mUpdateHandler.sendMessage(message);
    }

    @Override
    public void updateExact(StorageMeasurement meas, Bundle bundle) {
        final Message message = mUpdateHandler.obtainMessage(MSG_UI_UPDATE_EXACT);
        message.obj = meas.getUser();
        message.setData(bundle);
        mUpdateHandler.sendMessage(message);
    }

    public boolean mountToggleClicked(Preference preference) {
        return preference == mMountTogglePreference;
    }

    public Intent intentForClick(Preference pref) {
        Intent intent = null;

        // TODO The current "delete" story is not fully handled by the respective applications.
        // When it is done, make sure the intent types below are correct.
        // If that cannot be done, remove these intents.
        final String key = pref.getKey();
        if (pref == mFormatPreference) {
            intent = new Intent(Intent.ACTION_VIEW);
            intent.setClass(getContext(), com.android.settings.MediaFormat.class);
            intent.putExtra(StorageVolume.EXTRA_STORAGE_VOLUME, mStorageVolume);
        } else if (KEY_APPLICATIONS.equals(key)) {
            intent = new Intent(Intent.ACTION_MANAGE_PACKAGE_STORAGE);
            intent.setClass(getContext(),
                    com.android.settings.Settings.ManageApplicationsActivity.class);
        } else if (KEY_DOWNLOADS.equals(key)) {
            intent = new Intent(DownloadManager.ACTION_VIEW_DOWNLOADS).putExtra(
                    DownloadManager.INTENT_EXTRAS_SORT_BY_SIZE, true);
        } else if (KEY_MUSIC.equals(key)) {
            intent = new Intent(Intent.ACTION_GET_CONTENT);
            intent.setType("audio/mp3");
        } else if (KEY_DCIM.equals(key)) {
            intent = new Intent(Intent.ACTION_VIEW);
            intent.putExtra(Intent.EXTRA_LOCAL_ONLY, true);
            // TODO Create a Videos category, type = vnd.android.cursor.dir/video
            intent.setType("vnd.android.cursor.dir/image");
        } else if (KEY_MISC.equals(key)) {
            Context context = getContext().getApplicationContext();
            if (mLocalMeasure.getMiscSize() > 0) {
                intent = new Intent(context, MiscFilesHandler.class);
                intent.putExtra(StorageVolume.EXTRA_STORAGE_VOLUME, mStorageVolume);
            }
        }

        return intent;
    }
}
