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

import android.content.Context;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.UserManager;
import android.os.storage.StorageEventListener;
import android.os.storage.StorageManager;
import android.os.storage.VolumeInfo;
import android.preference.Preference;
import android.preference.PreferenceCategory;
import android.preference.PreferenceScreen;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.Toast;

import com.android.internal.logging.MetricsLogger;
import com.android.settings.R;
import com.android.settings.SettingsPreferenceFragment;
import com.android.settings.search.BaseSearchIndexProvider;
import com.android.settings.search.Indexable;
import com.android.settings.search.SearchIndexableRaw;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Panel showing both internal storage (both built-in storage and private
 * volumes) and removable storage (public volumes).
 */
public class StorageSettings extends SettingsPreferenceFragment implements Indexable {
    static final String TAG = "StorageSettings";

    // TODO: badging to indicate devices running low on storage
    // TODO: show currently ejected private volumes

    private UserManager mUserManager;
    private StorageManager mStorageManager;

    private PreferenceCategory mInternalCategory;
    private PreferenceCategory mExternalCategory;

    @Override
    protected int getMetricsCategory() {
        return MetricsLogger.DEVICEINFO_STORAGE;
    }

    @Override
    protected int getHelpResource() {
        return R.string.help_uri_storage;
    }

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);

        final Context context = getActivity();

        mUserManager = context.getSystemService(UserManager.class);

        mStorageManager = context.getSystemService(StorageManager.class);
        mStorageManager.registerListener(mStorageListener);

        addPreferencesFromResource(R.xml.device_info_storage);

        mInternalCategory = (PreferenceCategory) findPreference("storage_internal");
        mExternalCategory = (PreferenceCategory) findPreference("storage_external");

        // TODO: if only one volume visible, shortcut into it

        setHasOptionsMenu(true);
    }

    private final StorageEventListener mStorageListener = new StorageEventListener() {
        @Override
        public void onVolumeStateChanged(VolumeInfo vol, int oldState, int newState) {
            if (isInteresting(vol)) {
                refresh();
            }
        }
    };

    private static boolean isInteresting(VolumeInfo vol) {
        switch(vol.getType()) {
            case VolumeInfo.TYPE_PRIVATE:
            case VolumeInfo.TYPE_PUBLIC:
                return true;
            default:
                return false;
        }
    }

    private void refresh() {
        final Context context = getActivity();

        getPreferenceScreen().removeAll();
        mInternalCategory.removeAll();
        mExternalCategory.removeAll();

        final List<VolumeInfo> volumes = mStorageManager.getVolumes();
        Collections.sort(volumes, VolumeInfo.getDescriptionComparator());

        for (VolumeInfo vol : volumes) {
            if (vol.getType() == VolumeInfo.TYPE_PRIVATE) {
                mInternalCategory.addPreference(new StorageVolumePreference(context, vol));
            } else if (vol.getType() == VolumeInfo.TYPE_PUBLIC) {
                mExternalCategory.addPreference(new StorageVolumePreference(context, vol));
            }
        }

        if (mInternalCategory.getPreferenceCount() > 0) {
            getPreferenceScreen().addPreference(mInternalCategory);
        }
        if (mExternalCategory.getPreferenceCount() > 0) {
            getPreferenceScreen().addPreference(mExternalCategory);
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
    public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen, Preference pref) {
        final String volId = pref.getKey();
        final VolumeInfo vol = mStorageManager.findVolumeById(volId);
        if (vol == null) {
            return false;

        } else if (vol.getType() == VolumeInfo.TYPE_PRIVATE) {
            final Bundle args = new Bundle();
            args.putString(VolumeInfo.EXTRA_VOLUME_ID, volId);
            startFragment(this, PrivateVolumeSettings.class.getCanonicalName(),
                    -1, 0, args);
            return true;

        } else if (vol.getType() == VolumeInfo.TYPE_PUBLIC) {
            if (vol.isMountedReadable()) {
                startActivity(vol.buildBrowseIntent());
                return true;
            } else {
                final Bundle args = new Bundle();
                args.putString(VolumeInfo.EXTRA_VOLUME_ID, volId);
                startFragment(this, PublicVolumeSettings.class.getCanonicalName(),
                        -1, 0, args);
                return true;
            }
        }

        return false;
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

    /**
     * Enable indexing of searchable data
     */
    public static final SearchIndexProvider SEARCH_INDEX_DATA_PROVIDER =
        new BaseSearchIndexProvider() {
            @Override
            public List<SearchIndexableRaw> getRawDataToIndex(Context context, boolean enabled) {
                final List<SearchIndexableRaw> result = new ArrayList<SearchIndexableRaw>();

                SearchIndexableRaw data = new SearchIndexableRaw(context);
                data.title = context.getString(R.string.storage_settings);
                data.screenTitle = context.getString(R.string.storage_settings);
                result.add(data);

                data = new SearchIndexableRaw(context);
                data.title = context.getString(R.string.internal_storage);
                data.screenTitle = context.getString(R.string.storage_settings);
                result.add(data);

                data = new SearchIndexableRaw(context);
                final StorageManager storage = context.getSystemService(StorageManager.class);
                final List<VolumeInfo> vols = storage.getVolumes();
                for (VolumeInfo vol : vols) {
                    if (isInteresting(vol)) {
                        data.title = storage.getBestVolumeDescription(vol);
                        data.screenTitle = context.getString(R.string.storage_settings);
                        result.add(data);
                    }
                }

                data = new SearchIndexableRaw(context);
                data.title = context.getString(R.string.memory_size);
                data.screenTitle = context.getString(R.string.storage_settings);
                result.add(data);

                data = new SearchIndexableRaw(context);
                data.title = context.getString(R.string.memory_available);
                data.screenTitle = context.getString(R.string.storage_settings);
                result.add(data);

                data = new SearchIndexableRaw(context);
                data.title = context.getString(R.string.memory_apps_usage);
                data.screenTitle = context.getString(R.string.storage_settings);
                result.add(data);

                data = new SearchIndexableRaw(context);
                data.title = context.getString(R.string.memory_dcim_usage);
                data.screenTitle = context.getString(R.string.storage_settings);
                result.add(data);

                data = new SearchIndexableRaw(context);
                data.title = context.getString(R.string.memory_music_usage);
                data.screenTitle = context.getString(R.string.storage_settings);
                result.add(data);

                data = new SearchIndexableRaw(context);
                data.title = context.getString(R.string.memory_downloads_usage);
                data.screenTitle = context.getString(R.string.storage_settings);
                result.add(data);

                data = new SearchIndexableRaw(context);
                data.title = context.getString(R.string.memory_media_cache_usage);
                data.screenTitle = context.getString(R.string.storage_settings);
                result.add(data);

                data = new SearchIndexableRaw(context);
                data.title = context.getString(R.string.memory_media_misc_usage);
                data.screenTitle = context.getString(R.string.storage_settings);
                result.add(data);

                return result;
            }
        };
}
