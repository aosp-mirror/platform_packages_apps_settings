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
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.os.RemoteException;
import android.os.UserManager;
import android.os.storage.StorageManager;
import android.os.storage.StorageVolume;
import android.preference.Preference;
import android.preference.PreferenceCategory;
import android.provider.MediaStore;
import android.text.format.Formatter;

import com.android.settings.R;
import com.android.settings.Settings;
import com.android.settings.deviceinfo.StorageMeasurement.MeasurementDetails;
import com.android.settings.deviceinfo.StorageMeasurement.MeasurementReceiver;
import com.google.android.collect.Lists;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

public class StorageVolumePreferenceCategory extends PreferenceCategory {
    public static final String KEY_CACHE = "cache";

    private static final int ORDER_USAGE_BAR = -2;
    private static final int ORDER_STORAGE_LOW = -1;

    /** Physical volume being measured, or {@code null} for internal. */
    private final StorageVolume mVolume;
    private final StorageMeasurement mMeasure;

    private final Resources mResources;
    private final StorageManager mStorageManager;
    private final UserManager mUserManager;

    private UsageBarPreference mUsageBarPreference;
    private Preference mMountTogglePreference;
    private Preference mFormatPreference;
    private Preference mStorageLow;

    private StorageItemPreference mItemTotal;
    private StorageItemPreference mItemAvailable;
    private StorageItemPreference mItemApps;
    private StorageItemPreference mItemDcim;
    private StorageItemPreference mItemMusic;
    private StorageItemPreference mItemDownloads;
    private StorageItemPreference mItemCache;
    private StorageItemPreference mItemMisc;
    private List<StorageItemPreference> mItemUsers = Lists.newArrayList();

    private boolean mUsbConnected;
    private String mUsbFunction;

    private long mTotalSize;

    private static final int MSG_UI_UPDATE_APPROXIMATE = 1;
    private static final int MSG_UI_UPDATE_DETAILS = 2;

    private Handler mUpdateHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MSG_UI_UPDATE_APPROXIMATE: {
                    final long[] size = (long[]) msg.obj;
                    updateApproximate(size[0], size[1]);
                    break;
                }
                case MSG_UI_UPDATE_DETAILS: {
                    final MeasurementDetails details = (MeasurementDetails) msg.obj;
                    updateDetails(details);
                    break;
                }
            }
        }
    };

    /**
     * Build category to summarize internal storage, including any emulated
     * {@link StorageVolume}.
     */
    public static StorageVolumePreferenceCategory buildForInternal(Context context) {
        return new StorageVolumePreferenceCategory(context, null);
    }

    /**
     * Build category to summarize specific physical {@link StorageVolume}.
     */
    public static StorageVolumePreferenceCategory buildForPhysical(
            Context context, StorageVolume volume) {
        return new StorageVolumePreferenceCategory(context, volume);
    }

    private StorageVolumePreferenceCategory(Context context, StorageVolume volume) {
        super(context);

        mVolume = volume;
        mMeasure = StorageMeasurement.getInstance(context, volume);

        mResources = context.getResources();
        mStorageManager = StorageManager.from(context);
        mUserManager = (UserManager) context.getSystemService(Context.USER_SERVICE);

        setTitle(volume != null ? volume.getDescription(context)
                : context.getText(R.string.internal_storage));
    }

    private StorageItemPreference buildItem(int titleRes, int colorRes) {
        return new StorageItemPreference(getContext(), titleRes, colorRes);
    }

    public void init() {
        final Context context = getContext();

        removeAll();

        final UserInfo currentUser;
        try {
            currentUser = ActivityManagerNative.getDefault().getCurrentUser();
        } catch (RemoteException e) {
            throw new RuntimeException("Failed to get current user");
        }

        final List<UserInfo> otherUsers = getUsersExcluding(currentUser);
        final boolean showUsers = mVolume == null && otherUsers.size() > 0;

        mUsageBarPreference = new UsageBarPreference(context);
        mUsageBarPreference.setOrder(ORDER_USAGE_BAR);
        addPreference(mUsageBarPreference);

        mItemTotal = buildItem(R.string.memory_size, 0);
        mItemAvailable = buildItem(R.string.memory_available, R.color.memory_avail);
        addPreference(mItemTotal);
        addPreference(mItemAvailable);

        mItemApps = buildItem(R.string.memory_apps_usage, R.color.memory_apps_usage);
        mItemDcim = buildItem(R.string.memory_dcim_usage, R.color.memory_dcim);
        mItemMusic = buildItem(R.string.memory_music_usage, R.color.memory_music);
        mItemDownloads = buildItem(R.string.memory_downloads_usage, R.color.memory_downloads);
        mItemCache = buildItem(R.string.memory_media_cache_usage, R.color.memory_cache);
        mItemMisc = buildItem(R.string.memory_media_misc_usage, R.color.memory_misc);

        mItemCache.setKey(KEY_CACHE);

        final boolean showDetails = mVolume == null || mVolume.isPrimary();
        if (showDetails) {
            if (showUsers) {
                addPreference(new PreferenceHeader(context, currentUser.name));
            }

            addPreference(mItemApps);
            addPreference(mItemDcim);
            addPreference(mItemMusic);
            addPreference(mItemDownloads);
            addPreference(mItemCache);
            addPreference(mItemMisc);

            if (showUsers) {
                addPreference(new PreferenceHeader(context, R.string.storage_other_users));

                int count = 0;
                for (UserInfo info : otherUsers) {
                    final int colorRes = count++ % 2 == 0 ? R.color.memory_user_light
                            : R.color.memory_user_dark;
                    final StorageItemPreference userPref = new StorageItemPreference(
                            getContext(), info.name, colorRes, info.id);
                    mItemUsers.add(userPref);
                    addPreference(userPref);
                }
            }
        }

        final boolean isRemovable = mVolume != null ? mVolume.isRemovable() : false;
        // Always create the preference since many code rely on it existing
        mMountTogglePreference = new Preference(context);
        if (isRemovable) {
            mMountTogglePreference.setTitle(R.string.sd_eject);
            mMountTogglePreference.setSummary(R.string.sd_eject_summary);
            addPreference(mMountTogglePreference);
        }

        final boolean allowFormat = mVolume != null;
        if (allowFormat) {
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
        return mVolume;
    }

    private void updatePreferencesFromState() {
        // Only update for physical volumes
        if (mVolume == null) return;

        mMountTogglePreference.setEnabled(true);

        final String state = mStorageManager.getVolumeState(mVolume.getPath());

        if (Environment.MEDIA_MOUNTED_READ_ONLY.equals(state)) {
            mItemAvailable.setTitle(R.string.memory_available_read_only);
        } else {
            mItemAvailable.setTitle(R.string.memory_available);
        }

        if (Environment.MEDIA_MOUNTED.equals(state)
                || Environment.MEDIA_MOUNTED_READ_ONLY.equals(state)) {
            mMountTogglePreference.setEnabled(true);
            mMountTogglePreference.setTitle(mResources.getString(R.string.sd_eject));
            mMountTogglePreference.setSummary(mResources.getString(R.string.sd_eject_summary));
            addPreference(mUsageBarPreference);
            addPreference(mItemTotal);
            addPreference(mItemAvailable);
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
            removePreference(mItemTotal);
            removePreference(mItemAvailable);
        }

        if (mUsbConnected && (UsbManager.USB_FUNCTION_MTP.equals(mUsbFunction) ||
                UsbManager.USB_FUNCTION_PTP.equals(mUsbFunction))) {
            mMountTogglePreference.setEnabled(false);
            if (Environment.MEDIA_MOUNTED.equals(state)
                    || Environment.MEDIA_MOUNTED_READ_ONLY.equals(state)) {
                mMountTogglePreference.setSummary(
                        mResources.getString(R.string.mtp_ptp_mode_summary));
            }

            if (mFormatPreference != null) {
                mFormatPreference.setEnabled(false);
                mFormatPreference.setSummary(mResources.getString(R.string.mtp_ptp_mode_summary));
            }
        } else if (mFormatPreference != null) {
            mFormatPreference.setEnabled(mMountTogglePreference.isEnabled());
            mFormatPreference.setSummary(mResources.getString(R.string.sd_format_summary));
        }
    }

    public void updateApproximate(long totalSize, long availSize) {
        mItemTotal.setSummary(formatSize(totalSize));
        mItemAvailable.setSummary(formatSize(availSize));

        mTotalSize = totalSize;

        final long usedSize = totalSize - availSize;

        mUsageBarPreference.clear();
        mUsageBarPreference.addEntry(0, usedSize / (float) totalSize, android.graphics.Color.GRAY);
        mUsageBarPreference.commit();

        updatePreferencesFromState();
    }

    private static long totalValues(HashMap<String, Long> map, String... keys) {
        long total = 0;
        for (String key : keys) {
            if (map.containsKey(key)) {
                total += map.get(key);
            }
        }
        return total;
    }

    public void updateDetails(MeasurementDetails details) {
        final boolean showDetails = mVolume == null || mVolume.isPrimary();
        if (!showDetails) return;

        // Count caches as available space, since system manages them
        mItemTotal.setSummary(formatSize(details.totalSize));
        mItemAvailable.setSummary(formatSize(details.availSize));

        mUsageBarPreference.clear();

        updatePreference(mItemApps, details.appsSize);

        final long dcimSize = totalValues(details.mediaSize, Environment.DIRECTORY_DCIM,
                Environment.DIRECTORY_MOVIES, Environment.DIRECTORY_PICTURES);
        updatePreference(mItemDcim, dcimSize);

        final long musicSize = totalValues(details.mediaSize, Environment.DIRECTORY_MUSIC,
                Environment.DIRECTORY_ALARMS, Environment.DIRECTORY_NOTIFICATIONS,
                Environment.DIRECTORY_RINGTONES, Environment.DIRECTORY_PODCASTS);
        updatePreference(mItemMusic, musicSize);

        final long downloadsSize = totalValues(details.mediaSize, Environment.DIRECTORY_DOWNLOADS);
        updatePreference(mItemDownloads, downloadsSize);

        updatePreference(mItemCache, details.cacheSize);
        updatePreference(mItemMisc, details.miscSize);

        for (StorageItemPreference userPref : mItemUsers) {
            final long userSize = details.usersSize.get(userPref.userHandle);
            updatePreference(userPref, userSize);
        }

        mUsageBarPreference.commit();
    }

    private void updatePreference(StorageItemPreference pref, long size) {
        if (size > 0) {
            pref.setSummary(formatSize(size));
            final int order = pref.getOrder();
            mUsageBarPreference.addEntry(order, size / (float) mTotalSize, pref.color);
        } else {
            removePreference(pref);
        }
    }

    private void measure() {
        mMeasure.invalidate();
        mMeasure.measure();
    }

    public void onResume() {
        mMeasure.setReceiver(mReceiver);
        measure();
    }

    public void onStorageStateChanged() {
        init();
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

    public void onCacheCleared() {
        measure();
    }

    public void onPause() {
        mMeasure.cleanUp();
    }

    private String formatSize(long size) {
        return Formatter.formatFileSize(getContext(), size);
    }

    private MeasurementReceiver mReceiver = new MeasurementReceiver() {
        @Override
        public void updateApproximate(StorageMeasurement meas, long totalSize, long availSize) {
            mUpdateHandler.obtainMessage(MSG_UI_UPDATE_APPROXIMATE, new long[] {
                    totalSize, availSize }).sendToTarget();
        }

        @Override
        public void updateDetails(StorageMeasurement meas, MeasurementDetails details) {
            mUpdateHandler.obtainMessage(MSG_UI_UPDATE_DETAILS, details).sendToTarget();
        }
    };

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
            intent.putExtra(StorageVolume.EXTRA_STORAGE_VOLUME, mVolume);
        } else if (pref == mItemApps) {
            intent = new Intent(Intent.ACTION_MANAGE_PACKAGE_STORAGE);
            intent.setClass(getContext(), Settings.ManageApplicationsActivity.class);
        } else if (pref == mItemDownloads) {
            intent = new Intent(DownloadManager.ACTION_VIEW_DOWNLOADS).putExtra(
                    DownloadManager.INTENT_EXTRAS_SORT_BY_SIZE, true);
        } else if (pref == mItemMusic) {
            intent = new Intent(Intent.ACTION_GET_CONTENT);
            intent.setType("audio/mp3");
        } else if (pref == mItemDcim) {
            intent = new Intent(Intent.ACTION_VIEW);
            intent.putExtra(Intent.EXTRA_LOCAL_ONLY, true);
            // TODO Create a Videos category, MediaStore.Video.Media.EXTERNAL_CONTENT_URI
            intent.setData(MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        } else if (pref == mItemMisc) {
            Context context = getContext().getApplicationContext();
            intent = new Intent(context, MiscFilesHandler.class);
            intent.putExtra(StorageVolume.EXTRA_STORAGE_VOLUME, mVolume);
        }

        return intent;
    }

    public static class PreferenceHeader extends Preference {
        public PreferenceHeader(Context context, int titleRes) {
            super(context, null, com.android.internal.R.attr.preferenceCategoryStyle);
            setTitle(titleRes);
        }

        public PreferenceHeader(Context context, CharSequence title) {
            super(context, null, com.android.internal.R.attr.preferenceCategoryStyle);
            setTitle(title);
        }

        @Override
        public boolean isEnabled() {
            return false;
        }
    }

    /**
     * Return list of other users, excluding the current user.
     */
    private List<UserInfo> getUsersExcluding(UserInfo excluding) {
        final List<UserInfo> users = mUserManager.getUsers();
        final Iterator<UserInfo> i = users.iterator();
        while (i.hasNext()) {
            if (i.next().id == excluding.id) {
                i.remove();
            }
        }
        return users;
    }
}
