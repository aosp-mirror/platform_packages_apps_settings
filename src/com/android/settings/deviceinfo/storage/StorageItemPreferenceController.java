/*
 * Copyright (C) 2016 The Android Open Source Project
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

import android.app.Fragment;
import android.app.LoaderManager;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.Loader;
import android.os.Bundle;
import android.os.UserHandle;
import android.os.UserManager;
import android.os.storage.VolumeInfo;
import android.provider.DocumentsContract;
import android.support.annotation.VisibleForTesting;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceScreen;
import android.util.Log;

import com.android.settings.R;
import com.android.settings.Settings;
import com.android.settings.Utils;
import com.android.settings.applications.ManageApplications;
import com.android.settings.applications.PackageManagerWrapperImpl;
import com.android.settings.core.PreferenceController;
import com.android.settings.core.instrumentation.MetricsFeatureProvider;
import com.android.settings.overlay.FeatureFactory;
import com.android.settingslib.deviceinfo.StorageMeasurement;
import com.android.settingslib.deviceinfo.StorageVolumeProvider;

import java.util.HashMap;

/**
 * StorageItemPreferenceController handles the storage line items which summarize the storage
 * categorization breakdown.
 */
public class StorageItemPreferenceController extends PreferenceController
        implements LoaderManager.LoaderCallbacks<StorageAsyncLoader.AppsStorageResult> {
    private static final String TAG = "StorageItemPreference";

    private static final String IMAGE_MIME_TYPE = "image/*";

    @VisibleForTesting
    static final String PHOTO_KEY = "pref_photos_videos";
    @VisibleForTesting
    static final String AUDIO_KEY = "pref_music_audio";
    @VisibleForTesting
    static final String GAME_KEY = "pref_games";
    @VisibleForTesting
    static final String OTHER_APPS_KEY = "pref_other_apps";
    @VisibleForTesting
    static final String SYSTEM_KEY = "pref_system";
    @VisibleForTesting
    static final String FILES_KEY = "pref_files";

    private final Fragment mFragment;
    private final  MetricsFeatureProvider mMetricsFeatureProvider;
    private final StorageVolumeProvider mSvp;
    private VolumeInfo mVolume;
    private final int mUserId;
    private long mSystemSize;

    private StorageItemPreferenceAlternate mPhotoPreference;
    private StorageItemPreferenceAlternate mAudioPreference;
    private StorageItemPreferenceAlternate mGamePreference;
    private StorageItemPreferenceAlternate mAppPreference;
    private StorageItemPreferenceAlternate mFilePreference;
    private StorageItemPreferenceAlternate mSystemPreference;

    private static final String AUTHORITY_MEDIA = "com.android.providers.media.documents";

    public StorageItemPreferenceController(
            Context context, Fragment hostFragment, VolumeInfo volume, StorageVolumeProvider svp) {
        super(context);
        mFragment = hostFragment;
        mVolume = volume;
        mSvp = svp;
        mMetricsFeatureProvider = FeatureFactory.getFactory(context).getMetricsFeatureProvider();
        UserManager um = mContext.getSystemService(UserManager.class);
        mUserId = um.getUserHandle();
    }

    @Override
    public boolean isAvailable() {
        return true;
    }

    @Override
    public boolean handlePreferenceTreeClick(Preference preference) {
        if (preference == null) {
            return false;
        }

        // TODO: Currently, this reflects the existing behavior for these toggles.
        //       After the intermediate views are built, swap them in.
        Intent intent = null;
        switch (preference.getKey()) {
            case PHOTO_KEY:
                intent = getPhotosIntent();
                break;
            case AUDIO_KEY:
                intent = getAudioIntent();
                break;
            case GAME_KEY:
                intent = getGamesIntent();
                break;
            case OTHER_APPS_KEY:
                // Because we are likely constructed with a null volume, this is theoretically
                // possible.
                if (mVolume == null) {
                    break;
                }
                intent = getAppsIntent();
                break;
            case FILES_KEY:
                intent = getFilesIntent();
                break;
        }

        if (intent != null) {
            intent.putExtra(Intent.EXTRA_USER_ID, mUserId);

            launchIntent(intent);
            return true;
        }

        return super.handlePreferenceTreeClick(preference);
    }

    @Override
    public String getPreferenceKey() {
        return null;
    }

    /**
     * Sets the storage volume to use for when handling taps.
     */
    public void setVolume(VolumeInfo volume) {
        mVolume = volume;
    }

    @Override
    public void displayPreference(PreferenceScreen screen) {
        mPhotoPreference = (StorageItemPreferenceAlternate) screen.findPreference(PHOTO_KEY);
        mAudioPreference = (StorageItemPreferenceAlternate) screen.findPreference(AUDIO_KEY);
        mGamePreference = (StorageItemPreferenceAlternate) screen.findPreference(GAME_KEY);
        mAppPreference = (StorageItemPreferenceAlternate) screen.findPreference(OTHER_APPS_KEY);
        mSystemPreference = (StorageItemPreferenceAlternate) screen.findPreference(SYSTEM_KEY);
        mFilePreference = (StorageItemPreferenceAlternate) screen.findPreference(FILES_KEY);
    }

    @Override
    public Loader<StorageAsyncLoader.AppsStorageResult> onCreateLoader(int id,
            Bundle args) {
        return new StorageAsyncLoader(mContext, UserHandle.myUserId(), mVolume.fsUuid,
                new StorageStatsSource(mContext),
                new PackageManagerWrapperImpl(mContext.getPackageManager()));
    }

    @Override
    public void onLoadFinished(Loader<StorageAsyncLoader.AppsStorageResult> loader,
            StorageAsyncLoader.AppsStorageResult data) {
        mPhotoPreference.setStorageSize(
                data.externalStats.imageBytes + data.externalStats.videoBytes);
        mAudioPreference.setStorageSize(data.musicAppsSize + data.externalStats.audioBytes);
        mGamePreference.setStorageSize(data.gamesSize);
        mAppPreference.setStorageSize(data.otherAppsSize);
        mSystemPreference.setStorageSize(mSystemSize);

        long unattributedBytes = data.externalStats.totalBytes - data.externalStats.audioBytes
                - data.externalStats.videoBytes - data.externalStats.imageBytes;
        mFilePreference.setStorageSize(unattributedBytes);
    }

    @Override
    public void onLoaderReset(Loader<StorageAsyncLoader.AppsStorageResult> loader) {
    }

    /**
     * Sets the system size for the system size preference.
     * @param systemSize the size of the system in bytes
     */
    public void setSystemSize(long systemSize) {
        mSystemSize = systemSize;
    }

    private Intent getPhotosIntent() {
        Intent intent = new Intent();
        intent.setAction(android.content.Intent.ACTION_VIEW);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_DOCUMENT);
        intent.setType(IMAGE_MIME_TYPE);
        return intent;
    }

    private Intent getAudioIntent() {
        Bundle args = new Bundle();
        args.putString(ManageApplications.EXTRA_CLASSNAME,
                Settings.StorageUseActivity.class.getName());
        args.putString(ManageApplications.EXTRA_VOLUME_UUID, mVolume.getFsUuid());
        args.putString(ManageApplications.EXTRA_VOLUME_NAME, mVolume.getDescription());
        args.putInt(ManageApplications.EXTRA_STORAGE_TYPE, ManageApplications.STORAGE_TYPE_MUSIC);
        return Utils.onBuildStartFragmentIntent(mContext,
                ManageApplications.class.getName(), args, null, R.string.audio_storage_title, null,
                false, mMetricsFeatureProvider.getMetricsCategory(mFragment));
    }

    private Intent getAppsIntent() {
        Bundle args = new Bundle();
        args.putString(ManageApplications.EXTRA_CLASSNAME,
                Settings.StorageUseActivity.class.getName());
        args.putString(ManageApplications.EXTRA_VOLUME_UUID, mVolume.getFsUuid());
        args.putString(ManageApplications.EXTRA_VOLUME_NAME, mVolume.getDescription());
        return Utils.onBuildStartFragmentIntent(mContext,
                ManageApplications.class.getName(), args, null, R.string.apps_storage, null,
                false, mMetricsFeatureProvider.getMetricsCategory(mFragment));
    }

    private Intent getGamesIntent() {
            Bundle args = new Bundle(1);
            args.putString(ManageApplications.EXTRA_CLASSNAME,
                    Settings.GamesStorageActivity.class.getName());
            return Utils.onBuildStartFragmentIntent(mContext,
                    ManageApplications.class.getName(), args, null, R.string.game_storage_settings,
                    null, false, mMetricsFeatureProvider.getMetricsCategory(mFragment));
    }

    private Intent getFilesIntent() {
        return mSvp.findEmulatedForPrivate(mVolume).buildBrowseIntent();
    }

    private void launchIntent(Intent intent) {
        try {
            final int userId = intent.getIntExtra(Intent.EXTRA_USER_ID, -1);

            if (userId == -1) {
                mFragment.startActivity(intent);
            } else {
                mFragment.getActivity().startActivityAsUser(intent, new UserHandle(userId));
            }
        } catch (ActivityNotFoundException e) {
            Log.w(TAG, "No activity found for " + intent);
        }
    }

    private static long totalValues(StorageMeasurement.MeasurementDetails details, int userId,
            String... keys) {
        long total = 0;
        HashMap<String, Long> map = details.mediaSize.get(userId);
        if (map != null) {
            for (String key : keys) {
                if (map.containsKey(key)) {
                    total += map.get(key);
                }
            }
        } else {
            Log.w(TAG, "MeasurementDetails mediaSize array does not have key for user " + userId);
        }
        return total;
    }
}
