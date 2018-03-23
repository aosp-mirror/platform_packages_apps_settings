/*
 * Copyright (C) 2018 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the
 * License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the specific language governing
 * permissions and limitations under the License.
 */

package com.android.settings.search;

import static com.android.settings.slices.SliceDeepLinkSpringBoard.INTENT;
import static com.android.settings.slices.SliceDeepLinkSpringBoard.SETTINGS;

import android.app.slice.SliceManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.provider.Settings;
import android.util.Log;

import com.android.settings.slices.SettingsSliceProvider;

public interface DeviceIndexFeatureProvider {

    // TODO: Remove this and index all action and intent slices through search index.
    String[] ACTIONS_TO_INDEX = new String[]{
            Settings.ACTION_WIFI_SETTINGS,
    };

    String TAG = "DeviceIndex";

    String INDEX_VERSION = "settings:index_version";

    // Increment when new items are added to ensure they get pushed to the device index.
    int VERSION = 1;

    boolean isIndexingEnabled();

    void index(Context context, CharSequence title, Uri sliceUri, Uri launchUri);

    default void updateIndex(Context context, boolean force) {
        if (!isIndexingEnabled()) return;

        if (!force && Settings.Secure.getInt(context.getContentResolver(), INDEX_VERSION, -1)
                == VERSION) {
            // No need to update.
            return;
        }

        PackageManager pm = context.getPackageManager();
        for (String action : ACTIONS_TO_INDEX) {
            Intent intent = new Intent(action);
            intent.setPackage(context.getPackageName());
            ResolveInfo activity = pm.resolveActivity(intent, PackageManager.GET_META_DATA);
            if (activity == null) {
                Log.e(TAG, "Unable to resolve " + action);
                continue;
            }
            String sliceUri = activity.activityInfo.metaData
                    .getString(SliceManager.SLICE_METADATA_KEY);
            if (sliceUri != null) {
                Log.d(TAG, "Intent: " + createDeepLink(intent.toUri(Intent.URI_ANDROID_APP_SCHEME)));
                index(context, activity.activityInfo.loadLabel(pm),
                        Uri.parse(sliceUri),
                        Uri.parse(createDeepLink(intent.toUri(Intent.URI_ANDROID_APP_SCHEME))));
            } else {
                Log.e(TAG, "No slice uri found for " + activity.activityInfo.name);
            }
        }

        Settings.Secure.putInt(context.getContentResolver(), INDEX_VERSION, VERSION);
    }

    static String createDeepLink(String s) {
        return new Uri.Builder().scheme(SETTINGS)
                .authority(SettingsSliceProvider.SLICE_AUTHORITY)
                .appendQueryParameter(INTENT, s)
                .build()
                .toString();
    }
}
