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

package com.android.settings.fuelgauge;

import static android.app.usage.UsageStatsManager.STANDBY_BUCKET_ACTIVE;
import static android.app.usage.UsageStatsManager.STANDBY_BUCKET_EXEMPTED;
import static android.app.usage.UsageStatsManager.STANDBY_BUCKET_FREQUENT;
import static android.app.usage.UsageStatsManager.STANDBY_BUCKET_NEVER;
import static android.app.usage.UsageStatsManager.STANDBY_BUCKET_RARE;
import static android.app.usage.UsageStatsManager.STANDBY_BUCKET_RESTRICTED;
import static android.app.usage.UsageStatsManager.STANDBY_BUCKET_WORKING_SET;

import android.app.settings.SettingsEnums;
import android.app.usage.UsageStatsManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.res.Resources;
import android.os.Bundle;
import android.text.TextUtils;

import androidx.preference.ListPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceGroup;

import com.android.settings.R;
import com.android.settings.SettingsPreferenceFragment;

import java.util.Arrays;
import java.util.List;

public class InactiveApps extends SettingsPreferenceFragment
        implements Preference.OnPreferenceChangeListener {

    private static final CharSequence[] FULL_SETTABLE_BUCKETS_NAMES = {
        "ACTIVE", "WORKING_SET", "FREQUENT", "RARE", "RESTRICTED"
    };

    private static final CharSequence[] FULL_SETTABLE_BUCKETS_VALUES = {
        Integer.toString(STANDBY_BUCKET_ACTIVE),
        Integer.toString(STANDBY_BUCKET_WORKING_SET),
        Integer.toString(STANDBY_BUCKET_FREQUENT),
        Integer.toString(STANDBY_BUCKET_RARE),
        Integer.toString(STANDBY_BUCKET_RESTRICTED)
    };

    private UsageStatsManager mUsageStats;

    @Override
    public int getMetricsCategory() {
        return SettingsEnums.FUELGAUGE_INACTIVE_APPS;
    }

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);

        mUsageStats = getActivity().getSystemService(UsageStatsManager.class);
        addPreferencesFromResource(R.xml.placeholder_preference_screen);
        getActivity().setTitle(com.android.settingslib.R.string.inactive_apps_title);
    }

    @Override
    public void onResume() {
        super.onResume();
        init();
    }

    private void init() {
        PreferenceGroup screen = getPreferenceScreen();
        screen.removeAll();
        screen.setOrderingAsAdded(false);
        final Context context = getActivity();
        final PackageManager pm = context.getPackageManager();
        final String settingsPackage = context.getPackageName();
        final CharSequence[] bucketNames = FULL_SETTABLE_BUCKETS_NAMES;
        final CharSequence[] bucketValues = FULL_SETTABLE_BUCKETS_VALUES;

        Intent launcherIntent = new Intent(Intent.ACTION_MAIN);
        launcherIntent.addCategory(Intent.CATEGORY_LAUNCHER);
        List<ResolveInfo> apps = pm.queryIntentActivities(launcherIntent, 0);
        for (ResolveInfo app : apps) {
            String packageName = app.activityInfo.applicationInfo.packageName;
            ListPreference p = new ListPreference(getPrefContext());
            p.setTitle(app.loadLabel(pm));
            p.setIcon(app.loadIcon(pm));
            p.setKey(packageName);
            p.setEntries(getAllowableBuckets(packageName, bucketNames));
            p.setEntryValues(getAllowableBuckets(packageName, bucketValues));
            updateSummary(p);
            // Don't allow Settings to change its own standby bucket.
            if (TextUtils.equals(packageName, settingsPackage)) {
                p.setEnabled(false);
            }
            p.setOnPreferenceChangeListener(this);

            screen.addPreference(p);
        }
    }

    private CharSequence[] getAllowableBuckets(String packageName, CharSequence[] possibleBuckets) {
        final int minBucket = mUsageStats.getAppMinStandbyBucket(packageName);
        if (minBucket > STANDBY_BUCKET_RESTRICTED) {
            return possibleBuckets;
        }
        if (minBucket < STANDBY_BUCKET_ACTIVE) {
            return new CharSequence[] {};
        }
        // Use FULL_SETTABLE_BUCKETS_VALUES since we're searching using the int value. The index
        // should apply no matter which array we're going to copy from.
        final int idx =
                Arrays.binarySearch(FULL_SETTABLE_BUCKETS_VALUES, Integer.toString(minBucket));
        if (idx < 0) {
            // Include everything
            return possibleBuckets;
        }
        return Arrays.copyOfRange(possibleBuckets, 0, idx + 1);
    }

    static String bucketToName(int bucket) {
        switch (bucket) {
            case STANDBY_BUCKET_EXEMPTED:
                return "EXEMPTED";
            case STANDBY_BUCKET_ACTIVE:
                return "ACTIVE";
            case STANDBY_BUCKET_WORKING_SET:
                return "WORKING_SET";
            case STANDBY_BUCKET_FREQUENT:
                return "FREQUENT";
            case STANDBY_BUCKET_RARE:
                return "RARE";
            case STANDBY_BUCKET_RESTRICTED:
                return "RESTRICTED";
            case STANDBY_BUCKET_NEVER:
                return "NEVER";
        }
        return "";
    }

    private void updateSummary(ListPreference p) {
        final Resources res = getActivity().getResources();
        final int appBucket = mUsageStats.getAppStandbyBucket(p.getKey());
        final String bucketName = bucketToName(appBucket);
        p.setSummary(
                res.getString(com.android.settingslib.R.string.standby_bucket_summary, bucketName));
        // Buckets outside of the range of the dynamic ones are only used for special
        // purposes and can either not be changed out of, or might have undesirable
        // side-effects in combination with other assumptions.
        final boolean changeable =
                appBucket >= STANDBY_BUCKET_ACTIVE && appBucket <= STANDBY_BUCKET_RESTRICTED;
        if (changeable) {
            p.setValue(Integer.toString(appBucket));
        }
        p.setEnabled(changeable);
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        mUsageStats.setAppStandbyBucket(preference.getKey(), Integer.parseInt((String) newValue));
        updateSummary((ListPreference) preference);
        return false;
    }
}
