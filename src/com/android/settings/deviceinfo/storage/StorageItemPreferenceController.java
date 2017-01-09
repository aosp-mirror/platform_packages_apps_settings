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
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.UserHandle;
import android.os.UserManager;
import android.os.storage.VolumeInfo;
import android.provider.DocumentsContract;
import android.support.v7.preference.Preference;
import android.util.Log;

import com.android.settings.R;
import com.android.settings.Settings;
import com.android.settings.Utils;
import com.android.settings.applications.ManageApplications;
import com.android.settings.core.PreferenceController;
import com.android.settingslib.deviceinfo.StorageVolumeProvider;


/**
 * StorageItemPreferenceController handles the storage line items which summarize the storage
 * categorization breakdown.
 */
public class StorageItemPreferenceController extends PreferenceController {
    private static final String TAG = "StorageItemPreference";
    private final Fragment mFragment;
    private final StorageVolumeProvider mSvp;
    private VolumeInfo mVolume;
    private final int mUserId;

    private static final String AUTHORITY_MEDIA = "com.android.providers.media.documents";

    public StorageItemPreferenceController(Context context, Fragment hostFragment,
            VolumeInfo volume, StorageVolumeProvider svp) {
        super(context);
        mFragment = hostFragment;
        mVolume = volume;
        mSvp = svp;

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
            case "pref_photos_videos":
                intent = getPhotosIntent();
                break;
            case "pref_music_audio":
                intent = getAudioIntent();
                break;
            case "pref_games":
                // TODO: Once app categorization is added, make this section.
            case "pref_other_apps":
                // Because we are likely constructed with a null volume, this is theoretically
                // possible.
                if (mVolume == null) {
                    break;
                }
                intent = getAppsIntent();
                break;
            case "pref_files":
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

    private Intent getPhotosIntent() {
        Intent intent = new Intent(DocumentsContract.ACTION_BROWSE);
        intent.setData(DocumentsContract.buildRootUri(AUTHORITY_MEDIA, "images_root"));
        intent.addCategory(Intent.CATEGORY_DEFAULT);
        return intent;
    }

    private Intent getAudioIntent() {
        Intent intent = new Intent(DocumentsContract.ACTION_BROWSE);
        intent.setData(DocumentsContract.buildRootUri(AUTHORITY_MEDIA, "audio_root"));
        intent.addCategory(Intent.CATEGORY_DEFAULT);
        return intent;
    }

    private Intent getAppsIntent() {
        Bundle args = new Bundle();
        args.putString(ManageApplications.EXTRA_CLASSNAME,
                Settings.StorageUseActivity.class.getName());
        args.putString(ManageApplications.EXTRA_VOLUME_UUID, mVolume.getFsUuid());
        args.putString(ManageApplications.EXTRA_VOLUME_NAME, mVolume.getDescription());
        return Utils.onBuildStartFragmentIntent(mContext,
                ManageApplications.class.getName(), args, null, R.string.apps_storage, null,
                false);
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
}
