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

import android.app.job.JobInfo;
import android.app.job.JobScheduler;
import android.content.ComponentName;
import android.content.Context;
import android.content.pm.PackageManager;
import android.content.pm.ServiceInfo;
import android.net.Uri;
import android.os.Binder;
import android.os.Build;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.Log;

import com.android.settings.R;
import com.android.settings.Utils;
import com.android.settings.slices.SettingsSliceProvider;

import java.util.List;
import java.util.Locale;

public interface DeviceIndexFeatureProvider {

    String TAG = "DeviceIndex";

    String INDEX_VERSION = "settings:index_version";
    String INDEX_LANGUAGE = "settings:language";

    // Increment when new items are added to ensure they get pushed to the device index.
    String VERSION = Build.FINGERPRINT;

    // When the device language changes, re-index so Slices trigger in device language.
    Locale LANGUAGE = Locale.getDefault();

    boolean isIndexingEnabled();

    void index(Context context, CharSequence title, Uri sliceUri, Uri launchUri,
            List<String> keywords);

    void clearIndex(Context context);

    default void updateIndex(Context context, boolean force) {
        if (!isIndexingEnabled()) {
            Log.i(TAG, "Skipping: device index is not enabled");
            return;
        }

        if (!Utils.isDeviceProvisioned(context)) {
            Log.w(TAG, "Skipping: device is not provisioned");
            return;
        }

        final ComponentName jobComponent = new ComponentName(context.getPackageName(),
                DeviceIndexUpdateJobService.class.getName());

        try {
            final int callerUid = Binder.getCallingUid();
            final ServiceInfo si = context.getPackageManager().getServiceInfo(jobComponent,
                    PackageManager.MATCH_DIRECT_BOOT_AWARE
                            | PackageManager.MATCH_DIRECT_BOOT_UNAWARE);
            if (si == null) {
                Log.w(TAG, "Skipping: No such service " + jobComponent);
                return;
            }
            if (si.applicationInfo.uid != callerUid) {
                Log.w(TAG, "Skipping: Uid cannot schedule DeviceIndexUpdate: " + callerUid);
                return;
            }
        } catch (PackageManager.NameNotFoundException e) {
            Log.w(TAG, "Skipping: error finding DeviceIndexUpdateJobService from packageManager");
            return;
        }

        if (!force && skipIndex(context)) {
            Log.i(TAG, "Skipping: already indexed.");
            // No need to update.
            return;
        }

        // Prevent scheduling multiple jobs
        setIndexState(context);

        final int jobId = context.getResources().getInteger(R.integer.device_index_update);
        // Schedule a job so that we know it'll be able to complete, but try to run as
        // soon as possible.
        context.getSystemService(JobScheduler.class).schedule(
                new JobInfo.Builder(jobId, jobComponent)
                        .setPersisted(true)
                        .setMinimumLatency(1000)
                        .setOverrideDeadline(1)
                        .build());

    }

    static Uri createDeepLink(String s) {
        return new Uri.Builder().scheme(SETTINGS)
                .authority(SettingsSliceProvider.SLICE_AUTHORITY)
                .appendQueryParameter(INTENT, s)
                .build();
    }

    static boolean skipIndex(Context context) {
        final boolean isSameVersion = TextUtils.equals(
                Settings.Secure.getString(context.getContentResolver(), INDEX_VERSION), VERSION);
        final boolean isSameLanguage = TextUtils.equals(
                Settings.Secure.getString(context.getContentResolver(), INDEX_LANGUAGE),
                LANGUAGE.toString());
        return isSameLanguage && isSameVersion;
    }

    static void setIndexState(Context context) {
        Settings.Secure.putString(context.getContentResolver(), INDEX_VERSION, VERSION);
        Settings.Secure.putString(context.getContentResolver(), INDEX_LANGUAGE,
                LANGUAGE.toString());
    }
}
