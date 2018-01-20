/*
 * Copyright (C) 2018 The Android Open Source Project
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

package com.android.settings.applications;

import static android.os.storage.StorageVolume.ScopedAccessProviderContract.AUTHORITY;
import static android.os.storage.StorageVolume.ScopedAccessProviderContract.COL_DIRECTORY;
import static android.os.storage.StorageVolume.ScopedAccessProviderContract.COL_GRANTED;
import static android.os.storage.StorageVolume.ScopedAccessProviderContract.COL_PACKAGE;
import static android.os.storage.StorageVolume.ScopedAccessProviderContract.COL_VOLUME_UUID;
import static android.os.storage.StorageVolume.ScopedAccessProviderContract.TABLE_PERMISSIONS;
import static android.os.storage.StorageVolume.ScopedAccessProviderContract.TABLE_PERMISSIONS_COLUMNS;
import static android.os.storage.StorageVolume.ScopedAccessProviderContract.TABLE_PERMISSIONS_COL_DIRECTORY;
import static android.os.storage.StorageVolume.ScopedAccessProviderContract.TABLE_PERMISSIONS_COL_PACKAGE;
import static android.os.storage.StorageVolume.ScopedAccessProviderContract.TABLE_PERMISSIONS_COL_VOLUME_UUID;

import static com.android.settings.applications.AppStateDirectoryAccessBridge.DEBUG;
import static com.android.settings.applications.AppStateDirectoryAccessBridge.VERBOSE;

import android.annotation.Nullable;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.storage.StorageManager;
import android.os.storage.VolumeInfo;
import android.support.v14.preference.SwitchPreference;
import android.support.v7.preference.Preference;
import android.support.v7.preference.Preference.OnPreferenceChangeListener;
import android.support.v7.preference.Preference.OnPreferenceClickListener;
import android.text.TextUtils;
import android.support.v7.preference.PreferenceManager;
import android.support.v7.preference.PreferenceScreen;
import android.util.ArrayMap;
import android.util.ArraySet;
import android.util.IconDrawableFactory;
import android.util.Log;
import android.util.Pair;

import com.android.internal.logging.nano.MetricsProto.MetricsEvent;
import com.android.settings.R;
import com.android.settings.widget.EntityHeaderController;
import com.android.settings.widget.EntityHeaderController.ActionType;
import com.android.settingslib.applications.AppUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Detailed settings for an app's directory access permissions (A.K.A Scoped Directory Access).
 *
 * <p>Currently, it shows the entry for which the user denied access with the "Do not ask again"
 * flag checked on: the user than can use the settings toggle to reset that deniel.
 *
 * <p>This fragments dynamically lists all such permissions, starting with one preference per
 * directory in the primary storage, then adding additional entries for the external volumes (one
 * entry for the whole volume).
 */
// TODO(b/72055774): add unit tests
public class DirectoryAccessDetails extends AppInfoBase {

    @SuppressWarnings("hiding")
    private static final String TAG = "DirectoryAccessDetails";

    private boolean mCreated;

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        if (mCreated) {
            Log.w(TAG, "onActivityCreated(): ignoring duplicate call");
            return;
        }
        mCreated = true;
        if (mPackageInfo == null) {
            Log.w(TAG, "onActivityCreated(): no package info");
            return;
        }
        final Activity activity = getActivity();
        final Preference pref = EntityHeaderController
                .newInstance(activity, this, /* header= */ null )
                .setRecyclerView(getListView(), getLifecycle())
                .setIcon(IconDrawableFactory.newInstance(getPrefContext())
                        .getBadgedIcon(mPackageInfo.applicationInfo))
                .setLabel(mPackageInfo.applicationInfo.loadLabel(mPm))
                .setIsInstantApp(AppUtils.isInstant(mPackageInfo.applicationInfo))
                .setPackageName(mPackageName)
                .setUid(mPackageInfo.applicationInfo.uid)
                .setHasAppInfoLink(false)
                .setButtonActions(ActionType.ACTION_NONE, ActionType.ACTION_NONE)
                .done(activity, getPrefContext());
        getPreferenceScreen().addPreference(pref);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        final Context context = getPrefContext();
        addPreferencesFromResource(R.xml.directory_access_details);
        final PreferenceScreen prefsGroup = getPreferenceScreen();

        // Set external directory UUIDs.
        ArraySet<String> externalDirectoryUuids = null;

        final Uri providerUri = new Uri.Builder().scheme(ContentResolver.SCHEME_CONTENT)
                .authority(AUTHORITY).appendPath(TABLE_PERMISSIONS).appendPath("*")
                .build();
        // Query provider for entries.
        try (Cursor cursor = context.getContentResolver().query(providerUri,
                TABLE_PERMISSIONS_COLUMNS, null, new String[] { mPackageName }, null)) {
            if (cursor == null) {
                Log.w(TAG, "Didn't get cursor for " + mPackageName);
                return;
            }
            final int count = cursor.getCount();
            if (count == 0) {
                if (DEBUG) {
                    Log.d(TAG, "No permissions for " + mPackageName);
                }
                // TODO(b/72055774): display empty message
                return;
            }

            while (cursor.moveToNext()) {
                final String pkg = cursor.getString(TABLE_PERMISSIONS_COL_PACKAGE);
                final String uuid = cursor.getString(TABLE_PERMISSIONS_COL_VOLUME_UUID);
                final String dir = cursor.getString(TABLE_PERMISSIONS_COL_DIRECTORY);
                if (VERBOSE) {
                    Log.v(TAG, "Pkg:"  + pkg + " uuid: " + uuid + " dir: " + dir);
                }

                if (!mPackageName.equals(pkg)) {
                    // Sanity check, shouldn't happen
                    Log.w(TAG, "Ignoring " + uuid + "/" + dir + " due to package mismatch: "
                            + "expected " + mPackageName + ", got " + pkg);
                    continue;
                }

                if (uuid == null) {
                    // Primary storage entry: add right away
                    prefsGroup.addPreference(
                            newPreference(context, dir, providerUri, /* uuid= */ null, dir));
                } else {
                    // External volume entry: save it for later.
                    if (externalDirectoryUuids == null) {
                        externalDirectoryUuids = new ArraySet<>(1);
                    }
                    externalDirectoryUuids.add(uuid);
                }
            }
        }

        // Add entries from external volumes
        if (externalDirectoryUuids != null) {
            if (VERBOSE) {
                Log.v(TAG, "adding external directories: " + externalDirectoryUuids);
            }

            // Query StorageManager to get the user-friendly volume names.
            final StorageManager sm = context.getSystemService(StorageManager.class);
            final List<VolumeInfo> volumes = sm.getVolumes();
            if (volumes.isEmpty()) {
                Log.w(TAG, "StorageManager returned no secondary volumes");
                return;
            }
            final Map<String, String> volumeNames = new HashMap<>(volumes.size());
            for (VolumeInfo volume : volumes) {
                final String uuid = volume.getFsUuid();
                if (uuid == null) continue; // Primary storage; not used.

                String name = sm.getBestVolumeDescription(volume);
                if (name == null) {
                    Log.w(TAG, "No description for " + volume + "; using uuid instead: " + uuid);
                    name = uuid;
                }
                volumeNames.put(uuid, name);
            }
            if (VERBOSE) {
                Log.v(TAG, "UUID -> name mapping: " + volumeNames);
            }

            externalDirectoryUuids.forEach((uuid) ->{
                final String name = volumeNames.get(uuid);
                // TODO(b/72055774): add separator
                prefsGroup.addPreference(
                        newPreference(context, name, providerUri, uuid, /* dir= */ null));
            });
        }
        return;
    }


    private SwitchPreference newPreference(Context context, String title, Uri providerUri,
            String uuid, String dir) {
        final SwitchPreference pref = new SwitchPreference(context);
        pref.setKey(String.format("%s:%s", uuid, dir));
        pref.setTitle(title);
        pref.setChecked(false);
        pref.setOnPreferenceChangeListener((unused, value) -> {
            resetDoNotAskAgain(context, value, providerUri, uuid, dir);
            return true;
        });
        return pref;
    }

    private void resetDoNotAskAgain(Context context, Object value, Uri providerUri,
            @Nullable String uuid, @Nullable String directory) {
        if (!Boolean.class.isInstance(value)) {
            // Sanity check
            Log.wtf(TAG, "Invalid value from switch: " + value);
            return;
        }
        final boolean newValue = ((Boolean) value).booleanValue();
        if (DEBUG) {
            Log.d(TAG, "Asking " + providerUri  + " to update " + uuid + "/" + directory + " to "
                    + newValue);
        }
        final ContentValues values = new ContentValues(1);
        values.put(COL_GRANTED, newValue);
        final int updated = context.getContentResolver().update(providerUri, values,
                null, new String[] { mPackageName, uuid, directory });
        if (DEBUG) {
            Log.d(TAG, "Updated " + updated + " entries for " + uuid + "/" + directory);
        }
    }

    @Override
    protected boolean refreshUi() {
        return true;
    }

    @Override
    protected AlertDialog createDialog(int id, int errorCode) {
        return null;
    }

    @Override
    public int getMetricsCategory() {
        return MetricsEvent.APPLICATIONS_DIRECTORY_ACCESS_DETAIL;
    }
}
