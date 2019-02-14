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

import android.app.settings.SettingsEnums;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.res.TypedArray;
import android.graphics.drawable.Drawable;
import android.net.TrafficStats;
import android.os.Bundle;
import android.os.UserHandle;
import android.os.storage.VolumeInfo;
import android.util.Log;
import android.util.SparseArray;

import androidx.annotation.VisibleForTesting;
import androidx.fragment.app.Fragment;
import androidx.preference.Preference;
import androidx.preference.PreferenceScreen;

import com.android.settings.R;
import com.android.settings.Settings;
import com.android.settings.applications.manageapplications.ManageApplications;
import com.android.settings.core.PreferenceControllerMixin;
import com.android.settings.core.SubSettingLauncher;
import com.android.settings.deviceinfo.PrivateVolumeSettings.SystemInfoFragment;
import com.android.settings.deviceinfo.StorageItemPreference;
import com.android.settings.overlay.FeatureFactory;
import com.android.settingslib.core.AbstractPreferenceController;
import com.android.settingslib.core.instrumentation.MetricsFeatureProvider;
import com.android.settingslib.deviceinfo.StorageMeasurement;
import com.android.settingslib.deviceinfo.StorageVolumeProvider;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * StorageItemPreferenceController handles the storage line items which summarize the storage
 * categorization breakdown.
 */
public class StorageItemPreferenceController extends AbstractPreferenceController implements
        PreferenceControllerMixin {
    private static final String TAG = "StorageItemPreference";

    private static final String SYSTEM_FRAGMENT_TAG = "SystemInfo";

    @VisibleForTesting
    static final String PHOTO_KEY = "pref_photos_videos";
    @VisibleForTesting
    static final String AUDIO_KEY = "pref_music_audio";
    @VisibleForTesting
    static final String GAME_KEY = "pref_games";
    @VisibleForTesting
    static final String MOVIES_KEY = "pref_movies";
    @VisibleForTesting
    static final String OTHER_APPS_KEY = "pref_other_apps";
    @VisibleForTesting
    static final String SYSTEM_KEY = "pref_system";
    @VisibleForTesting
    static final String FILES_KEY = "pref_files";

    private final Fragment mFragment;
    private final MetricsFeatureProvider mMetricsFeatureProvider;
    private final StorageVolumeProvider mSvp;
    private VolumeInfo mVolume;
    private int mUserId;
    private long mUsedBytes;
    private long mTotalSize;

    private PreferenceScreen mScreen;
    private StorageItemPreference mPhotoPreference;
    private StorageItemPreference mAudioPreference;
    private StorageItemPreference mGamePreference;
    private StorageItemPreference mMoviesPreference;
    private StorageItemPreference mAppPreference;
    private StorageItemPreference mFilePreference;
    private StorageItemPreference mSystemPreference;
    private boolean mIsWorkProfile;

    private static final String AUTHORITY_MEDIA = "com.android.providers.media.documents";

    public StorageItemPreferenceController(
            Context context, Fragment hostFragment, VolumeInfo volume, StorageVolumeProvider svp) {
        super(context);
        mFragment = hostFragment;
        mVolume = volume;
        mSvp = svp;
        mMetricsFeatureProvider = FeatureFactory.getFactory(context).getMetricsFeatureProvider();
        mUserId = UserHandle.myUserId();
    }

    public StorageItemPreferenceController(
            Context context,
            Fragment hostFragment,
            VolumeInfo volume,
            StorageVolumeProvider svp,
            boolean isWorkProfile) {
        this(context, hostFragment, volume, svp);
        mIsWorkProfile = isWorkProfile;
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

        Intent intent = null;
        if (preference.getKey() == null) {
            return false;
        }
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
            case MOVIES_KEY:
                intent = getMoviesIntent();
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
                FeatureFactory.getFactory(mContext).getMetricsFeatureProvider().action(
                        mContext, SettingsEnums.STORAGE_FILES);
                break;
            case SYSTEM_KEY:
                final SystemInfoFragment dialog = new SystemInfoFragment();
                dialog.setTargetFragment(mFragment, 0);
                dialog.show(mFragment.getFragmentManager(), SYSTEM_FRAGMENT_TAG);
                return true;
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
        setFilesPreferenceVisibility();
    }

    private void setFilesPreferenceVisibility() {
        if (mScreen != null) {
            final VolumeInfo sharedVolume = mSvp.findEmulatedForPrivate(mVolume);
            // If we don't have a shared volume for our internal storage (or the shared volume isn't
            // mounted as readable for whatever reason), we should hide the File preference.
            final boolean hideFilePreference =
                    (sharedVolume == null) || !sharedVolume.isMountedReadable();
            if (hideFilePreference) {
                mScreen.removePreference(mFilePreference);
            } else {
                mScreen.addPreference(mFilePreference);
            }
        }
    }

    /**
     * Sets the user id for which this preference controller is handling.
     */
    public void setUserId(UserHandle userHandle) {
        mUserId = userHandle.getIdentifier();

        tintPreference(mPhotoPreference);
        tintPreference(mMoviesPreference);
        tintPreference(mAudioPreference);
        tintPreference(mGamePreference);
        tintPreference(mAppPreference);
        tintPreference(mSystemPreference);
        tintPreference(mFilePreference);
    }

    private void tintPreference(Preference preference) {
        if (preference != null) {
            preference.setIcon(applyTint(mContext, preference.getIcon()));
        }
    }

    private static Drawable applyTint(Context context, Drawable icon) {
        TypedArray array =
                context.obtainStyledAttributes(new int[]{android.R.attr.colorControlNormal});
        icon = icon.mutate();
        icon.setTint(array.getColor(0, 0));
        array.recycle();
        return icon;
    }

    @Override
    public void displayPreference(PreferenceScreen screen) {
        mScreen = screen;
        mPhotoPreference = screen.findPreference(PHOTO_KEY);
        mAudioPreference = screen.findPreference(AUDIO_KEY);
        mGamePreference = screen.findPreference(GAME_KEY);
        mMoviesPreference = screen.findPreference(MOVIES_KEY);
        mAppPreference = screen.findPreference(OTHER_APPS_KEY);
        mSystemPreference = screen.findPreference(SYSTEM_KEY);
        mFilePreference = screen.findPreference(FILES_KEY);

        setFilesPreferenceVisibility();
    }

    public void onLoadFinished(SparseArray<StorageAsyncLoader.AppsStorageResult> result,
            int userId) {
        final StorageAsyncLoader.AppsStorageResult data = result.get(userId);

        // TODO(b/35927909): Figure out how to split out apps which are only installed for work
        //       profiles in order to attribute those app's code bytes only to that profile.
        mPhotoPreference.setStorageSize(
                data.photosAppsSize + data.externalStats.imageBytes + data.externalStats.videoBytes,
                mTotalSize);
        mAudioPreference.setStorageSize(
                data.musicAppsSize + data.externalStats.audioBytes, mTotalSize);
        mGamePreference.setStorageSize(data.gamesSize, mTotalSize);
        mMoviesPreference.setStorageSize(data.videoAppsSize, mTotalSize);
        mAppPreference.setStorageSize(data.otherAppsSize, mTotalSize);

        long otherExternalBytes =
                data.externalStats.totalBytes
                        - data.externalStats.audioBytes
                        - data.externalStats.videoBytes
                        - data.externalStats.imageBytes
                        - data.externalStats.appBytes;
        mFilePreference.setStorageSize(otherExternalBytes, mTotalSize);

        if (mSystemPreference != null) {
            // Everything else that hasn't already been attributed is tracked as
            // belonging to system.
            long attributedSize = 0;
            for (int i = 0; i < result.size(); i++) {
                final StorageAsyncLoader.AppsStorageResult otherData = result.valueAt(i);
                attributedSize +=
                        otherData.gamesSize
                                + otherData.musicAppsSize
                                + otherData.videoAppsSize
                                + otherData.photosAppsSize
                                + otherData.otherAppsSize;
                attributedSize += otherData.externalStats.totalBytes
                        - otherData.externalStats.appBytes;
            }

            final long systemSize = Math.max(TrafficStats.GB_IN_BYTES, mUsedBytes - attributedSize);
            mSystemPreference.setStorageSize(systemSize, mTotalSize);
        }
    }

    public void setUsedSize(long usedSizeBytes) {
        mUsedBytes = usedSizeBytes;
    }

    public void setTotalSize(long totalSizeBytes) {
        mTotalSize = totalSizeBytes;
    }

    /**
     * Returns a list of keys used by this preference controller.
     */
    public static List<String> getUsedKeys() {
        List<String> list = new ArrayList<>();
        list.add(PHOTO_KEY);
        list.add(AUDIO_KEY);
        list.add(GAME_KEY);
        list.add(MOVIES_KEY);
        list.add(OTHER_APPS_KEY);
        list.add(SYSTEM_KEY);
        list.add(FILES_KEY);
        return list;
    }

    private Intent getPhotosIntent() {
        Bundle args = getWorkAnnotatedBundle(2);
        args.putString(
                ManageApplications.EXTRA_CLASSNAME, Settings.PhotosStorageActivity.class.getName());
        args.putInt(
                ManageApplications.EXTRA_STORAGE_TYPE,
                ManageApplications.STORAGE_TYPE_PHOTOS_VIDEOS);
        return new SubSettingLauncher(mContext)
                .setDestination(ManageApplications.class.getName())
                .setTitleRes(R.string.storage_photos_videos)
                .setArguments(args)
                .setSourceMetricsCategory(mMetricsFeatureProvider.getMetricsCategory(mFragment))
                .toIntent();
    }

    private Intent getAudioIntent() {
        if (mVolume == null) {
            return null;
        }

        Bundle args = getWorkAnnotatedBundle(4);
        args.putString(ManageApplications.EXTRA_CLASSNAME,
                Settings.StorageUseActivity.class.getName());
        args.putString(ManageApplications.EXTRA_VOLUME_UUID, mVolume.getFsUuid());
        args.putString(ManageApplications.EXTRA_VOLUME_NAME, mVolume.getDescription());
        args.putInt(ManageApplications.EXTRA_STORAGE_TYPE, ManageApplications.STORAGE_TYPE_MUSIC);
        return new SubSettingLauncher(mContext)
                .setDestination(ManageApplications.class.getName())
                .setTitleRes(R.string.storage_music_audio)
                .setArguments(args)
                .setSourceMetricsCategory(mMetricsFeatureProvider.getMetricsCategory(mFragment))
                .toIntent();
    }

    private Intent getAppsIntent() {
        if (mVolume == null) {
            return null;
        }
        final Bundle args = getWorkAnnotatedBundle(3);
        args.putString(ManageApplications.EXTRA_CLASSNAME,
                Settings.StorageUseActivity.class.getName());
        args.putString(ManageApplications.EXTRA_VOLUME_UUID, mVolume.getFsUuid());
        args.putString(ManageApplications.EXTRA_VOLUME_NAME, mVolume.getDescription());
        return new SubSettingLauncher(mContext)
                .setDestination(ManageApplications.class.getName())
                .setTitleRes(R.string.apps_storage)
                .setArguments(args)
                .setSourceMetricsCategory(mMetricsFeatureProvider.getMetricsCategory(mFragment))
                .toIntent();
    }

    private Intent getGamesIntent() {
        final Bundle args = getWorkAnnotatedBundle(1);
        args.putString(ManageApplications.EXTRA_CLASSNAME,
                Settings.GamesStorageActivity.class.getName());
        return new SubSettingLauncher(mContext)
                .setDestination(ManageApplications.class.getName())
                .setTitleRes(R.string.game_storage_settings)
                .setArguments(args)
                .setSourceMetricsCategory(mMetricsFeatureProvider.getMetricsCategory(mFragment))
                .toIntent();
    }

    private Intent getMoviesIntent() {
        final Bundle args = getWorkAnnotatedBundle(1);
        args.putString(ManageApplications.EXTRA_CLASSNAME,
                Settings.MoviesStorageActivity.class.getName());
        return new SubSettingLauncher(mContext)
                .setDestination(ManageApplications.class.getName())
                .setTitleRes(R.string.storage_movies_tv)
                .setArguments(args)
                .setSourceMetricsCategory(mMetricsFeatureProvider.getMetricsCategory(mFragment))
                .toIntent();
    }

    private Bundle getWorkAnnotatedBundle(int additionalCapacity) {
        final Bundle args = new Bundle(2 + additionalCapacity);
        args.putBoolean(ManageApplications.EXTRA_WORK_ONLY, mIsWorkProfile);
        args.putInt(ManageApplications.EXTRA_WORK_ID, mUserId);
        return args;
    }

    private Intent getFilesIntent() {
        return mSvp.findEmulatedForPrivate(mVolume).buildBrowseIntent();
    }

    private void launchIntent(Intent intent) {
        try {
            final int userId = intent.getIntExtra(Intent.EXTRA_USER_ID, -1);

            // b/33117269: Note that launchIntent may launch activity in different task which set
            // different launchMode (e.g. Files), using startActivityForesult to set task as
            // source task, and set requestCode as 0 means don't care about returnCode currently.
            if (userId == -1) {
                mFragment.startActivityForResult(intent, 0 /* requestCode not used */);
            } else {
                mFragment.getActivity().startActivityForResultAsUser(intent,
                        0 /* requestCode not used */, new UserHandle(userId));
            }
        } catch (ActivityNotFoundException e) {
            Log.w(TAG, "No activity found for " + intent);
        }
    }

    private static long totalValues(StorageMeasurement.MeasurementDetails details, int userId,
            String... keys) {
        long total = 0;
        Map<String, Long> map = details.mediaSize.get(userId);
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
