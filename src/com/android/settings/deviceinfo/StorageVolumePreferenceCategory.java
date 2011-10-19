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

import android.app.DownloadManager;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.drawable.ShapeDrawable;
import android.graphics.drawable.shapes.RectShape;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.os.storage.StorageManager;
import android.os.storage.StorageVolume;
import android.preference.Preference;
import android.preference.PreferenceCategory;
import android.text.format.Formatter;

import com.android.settings.R;
import com.android.settings.deviceinfo.StorageMeasurement.MeasurementReceiver;

import java.util.HashSet;
import java.util.Set;

public class StorageVolumePreferenceCategory extends PreferenceCategory implements
        MeasurementReceiver {

    static final int TOTAL_SIZE = 0;
    static final int APPLICATIONS = 1;
    static final int DCIM = 2; // Pictures and Videos
    static final int MUSIC = 3;
    static final int DOWNLOADS = 4;
    static final int MISC = 5;
    static final int AVAILABLE = 6;

    private UsageBarPreference mUsageBarPreference;
    private Preference[] mPreferences;
    private Preference mMountTogglePreference;
    private Preference mFormatPreference;
    private int[] mColors;

    private Resources mResources;

    private StorageVolume mStorageVolume;

    private StorageManager mStorageManager = null;

    private StorageMeasurement mMeasurement;

    private boolean mAllowFormat;

    static class CategoryInfo {
        final int mTitle;
        final int mColor;

        public CategoryInfo(int title, int color) {
            mTitle = title;
            mColor = color;
        }
    }

    static final CategoryInfo[] sCategoryInfos = new CategoryInfo[] {
        new CategoryInfo(R.string.memory_size, 0),
        new CategoryInfo(R.string.memory_apps_usage, R.color.memory_apps_usage),
        new CategoryInfo(R.string.memory_dcim_usage, R.color.memory_dcim),
        new CategoryInfo(R.string.memory_music_usage, R.color.memory_music),
        new CategoryInfo(R.string.memory_downloads_usage, R.color.memory_downloads),
        new CategoryInfo(R.string.memory_media_misc_usage, R.color.memory_misc),
        new CategoryInfo(R.string.memory_available, R.color.memory_avail),
    };

    public static final Set<String> sPathsExcludedForMisc = new HashSet<String>();

    static class MediaCategory {
        final String[] mDirPaths;
        final int mCategory;
        //final int mMediaType;

        public MediaCategory(int category, String... directories) {
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
        new MediaCategory(DCIM, Environment.DIRECTORY_DCIM, Environment.DIRECTORY_MOVIES,
                Environment.DIRECTORY_PICTURES),
        new MediaCategory(MUSIC, Environment.DIRECTORY_MUSIC, Environment.DIRECTORY_ALARMS,
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
                    Bundle bundle = msg.getData();
                    final long totalSize = bundle.getLong(StorageMeasurement.TOTAL_SIZE);
                    final long availSize = bundle.getLong(StorageMeasurement.AVAIL_SIZE);
                    updateApproximate(totalSize, availSize);
                    break;
                }
                case MSG_UI_UPDATE_EXACT: {
                    Bundle bundle = msg.getData();
                    final long totalSize = bundle.getLong(StorageMeasurement.TOTAL_SIZE);
                    final long availSize = bundle.getLong(StorageMeasurement.AVAIL_SIZE);
                    final long appsUsed = bundle.getLong(StorageMeasurement.APPS_USED);
                    final long downloadsSize = bundle.getLong(StorageMeasurement.DOWNLOADS_SIZE);
                    final long miscSize = bundle.getLong(StorageMeasurement.MISC_SIZE);
                    final long[] mediaSizes = bundle.getLongArray(StorageMeasurement.MEDIA_SIZES);
                    updateExact(totalSize, availSize, appsUsed, downloadsSize, miscSize,
                            mediaSizes);
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
        setTitle(storageVolume != null ? storageVolume.getDescription()
                : resources.getText(R.string.internal_storage));
        mMeasurement = StorageMeasurement.getInstance(context, storageVolume, isPrimary);
        mMeasurement.setReceiver(this);

        // Cannot format emulated storage
        mAllowFormat = mStorageVolume != null && !mStorageVolume.isEmulated();
        // For now we are disabling reformatting secondary external storage
        // until some interoperability problems with MTP are fixed
        if (!isPrimary) mAllowFormat = false;
    }

    public void init() {
        mUsageBarPreference = new UsageBarPreference(getContext());

        final int width = (int) mResources.getDimension(R.dimen.device_memory_usage_button_width);
        final int height = (int) mResources.getDimension(R.dimen.device_memory_usage_button_height);

        final int numberOfCategories = sCategoryInfos.length;
        mPreferences = new Preference[numberOfCategories];
        mColors = new int[numberOfCategories];
        for (int i = 0; i < numberOfCategories; i++) {
            final Preference preference = new Preference(getContext());
            mPreferences[i] = preference;
            preference.setTitle(sCategoryInfos[i].mTitle);
            preference.setSummary(R.string.memory_calculating_size);
            if (i != TOTAL_SIZE) {
                // TOTAL_SIZE has no associated color
                mColors[i] = mResources.getColor(sCategoryInfos[i].mColor);
                preference.setIcon(createRectShape(width, height, mColors[i]));
            }
        }

        mMountTogglePreference = new Preference(getContext());
        mMountTogglePreference.setTitle(R.string.sd_eject);
        mMountTogglePreference.setSummary(R.string.sd_eject_summary);

        if (mAllowFormat) {
            mFormatPreference = new Preference(getContext());
            mFormatPreference.setTitle(R.string.sd_format);
            mFormatPreference.setSummary(R.string.sd_format_summary);
        }
    }

    public StorageVolume getStorageVolume() {
        return mStorageVolume;
    }

    /**
     * Successive mounts can change the list of visible preferences.
     * This makes sure all preferences are visible and displayed in the right order.
     */
    private void resetPreferences() {
        final int numberOfCategories = sCategoryInfos.length;

        removePreference(mUsageBarPreference);
        for (int i = 0; i < numberOfCategories; i++) {
            removePreference(mPreferences[i]);
        }
        removePreference(mMountTogglePreference);
        if (mFormatPreference != null) {
            removePreference(mFormatPreference);
        }

        addPreference(mUsageBarPreference);
        for (int i = 0; i < numberOfCategories; i++) {
            addPreference(mPreferences[i]);
        }
        addPreference(mMountTogglePreference);
        if (mFormatPreference != null) {
            addPreference(mFormatPreference);
        }

        mMountTogglePreference.setEnabled(true);
    }

    private void updatePreferencesFromState() {
        resetPreferences();

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
            mPreferences[AVAILABLE].setSummary(mPreferences[AVAILABLE].getSummary() + readOnly);

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
            removePreference(mPreferences[TOTAL_SIZE]);
            removePreference(mPreferences[AVAILABLE]);
            if (mFormatPreference != null) {
                removePreference(mFormatPreference);
            }
        }
    }

    public void updateApproximate(long totalSize, long availSize) {
        mPreferences[TOTAL_SIZE].setSummary(formatSize(totalSize));
        mPreferences[AVAILABLE].setSummary(formatSize(availSize));

        final long usedSize = totalSize - availSize;

        mUsageBarPreference.clear();
        mUsageBarPreference.addEntry(usedSize / (float) totalSize, android.graphics.Color.GRAY);
        mUsageBarPreference.commit();

        updatePreferencesFromState();
    }

    public void updateExact(long totalSize, long availSize, long appsSize, long downloadsSize,
            long miscSize, long[] mediaSizes) {
        mUsageBarPreference.clear();

        mPreferences[TOTAL_SIZE].setSummary(formatSize(totalSize));

        if (mMeasurement.isExternalSDCard()) {
            // TODO FIXME: external SD card will not report any size. Show used space in bar graph
            final long usedSize = totalSize - availSize;
            mUsageBarPreference.addEntry(usedSize / (float) totalSize, android.graphics.Color.GRAY);
        }

        updatePreference(appsSize, totalSize, APPLICATIONS);

        long totalMediaSize = 0;
        for (int i = 0; i < sMediaCategories.length; i++) {
            final int category = sMediaCategories[i].mCategory;
            final long size = mediaSizes[i];
            updatePreference(size, totalSize, category);
            totalMediaSize += size;
        }

        updatePreference(downloadsSize, totalSize, DOWNLOADS);

        // Note miscSize != totalSize - availSize - appsSize - downloadsSize - totalMediaSize
        // Block size is taken into account. That can be extra space from folders. TODO Investigate
        updatePreference(miscSize, totalSize, MISC);

        updatePreference(availSize, totalSize, AVAILABLE);

        mUsageBarPreference.commit();
    }

    private void updatePreference(long size, long totalSize, int category) {
        if (size > 0) {
            mPreferences[category].setSummary(formatSize(size));
            mUsageBarPreference.addEntry(size / (float) totalSize, mColors[category]);
        } else {
            removePreference(mPreferences[category]);
        }
    }

    private void measure() {
        mMeasurement.invalidate();
        mMeasurement.measure();
    }

    public void onResume() {
        mMeasurement.setReceiver(this);
        measure();
    }

    public void onStorageStateChanged() {
        measure();
    }

    public void onMediaScannerFinished() {
        measure();
    }

    public void onPause() {
        mMeasurement.cleanUp();
    }

    private static ShapeDrawable createRectShape(int width, int height, int color) {
        ShapeDrawable shape = new ShapeDrawable(new RectShape());
        shape.setIntrinsicHeight(height);
        shape.setIntrinsicWidth(width);
        shape.getPaint().setColor(color);
        return shape;
    }

    private String formatSize(long size) {
        return Formatter.formatFileSize(getContext(), size);
    }

    @Override
    public void updateApproximate(Bundle bundle) {
        final Message message = mUpdateHandler.obtainMessage(MSG_UI_UPDATE_APPROXIMATE);
        message.setData(bundle);
        mUpdateHandler.sendMessage(message);
    }

    @Override
    public void updateExact(Bundle bundle) {
        final Message message = mUpdateHandler.obtainMessage(MSG_UI_UPDATE_EXACT);
        message.setData(bundle);
        mUpdateHandler.sendMessage(message);
    }

    public boolean mountToggleClicked(Preference preference) {
        return preference == mMountTogglePreference;
    }

    public Intent intentForClick(Preference preference) {
        Intent intent = null;

        // TODO The current "delete" story is not fully handled by the respective applications.
        // When it is done, make sure the intent types below are correct.
        // If that cannot be done, remove these intents.
        if (preference == mFormatPreference) {
            intent = new Intent(Intent.ACTION_VIEW);
            intent.setClass(getContext(), com.android.settings.MediaFormat.class);
            intent.putExtra(StorageVolume.EXTRA_STORAGE_VOLUME, mStorageVolume);
        } else if (preference == mPreferences[APPLICATIONS]) {
            intent = new Intent(Intent.ACTION_MANAGE_PACKAGE_STORAGE);
            intent.setClass(getContext(),
                    com.android.settings.Settings.ManageApplicationsActivity.class);
        } else if (preference == mPreferences[DOWNLOADS]) {
            intent = new Intent(DownloadManager.ACTION_VIEW_DOWNLOADS).putExtra(
                    DownloadManager.INTENT_EXTRAS_SORT_BY_SIZE, true);
        } else if (preference == mPreferences[MUSIC]) {
            intent = new Intent(Intent.ACTION_GET_CONTENT);
            intent.setType("audio/mp3");
        } else if (preference == mPreferences[DCIM]) {
            intent = new Intent(Intent.ACTION_VIEW);
            intent.putExtra(Intent.EXTRA_LOCAL_ONLY, true);
            // TODO Create a Videos category, type = vnd.android.cursor.dir/video
            intent.setType("vnd.android.cursor.dir/image");
        } else if (preference == mPreferences[MISC]) {
            Context context = getContext().getApplicationContext();
            if (mMeasurement.getMiscSize() > 0) {
                intent = new Intent(context, MiscFilesHandler.class);
                intent.putExtra(StorageVolume.EXTRA_STORAGE_VOLUME, mStorageVolume);
            }
        }

        return intent;
    }
}
