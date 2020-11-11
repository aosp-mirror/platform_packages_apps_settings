/*
 * Copyright (C) 2020 The Android Open Source Project
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

package com.android.settings.development.transcode;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.os.SystemProperties;

import androidx.preference.Preference;
import androidx.preference.PreferenceScreen;
import androidx.preference.SwitchPreference;

import com.android.settings.core.BasePreferenceController;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * The controller for the "Skip transcoding for apps" section on the transcode settings
 * screen.
 */
public class TranscodeSkipAppsPreferenceController extends BasePreferenceController implements
        Preference.OnPreferenceChangeListener {

    private static final String SKIP_SELECTED_APPS_PROP_KEY =
            "persist.sys.fuse.transcode_skip_uids";

    private final PackageManager mPackageManager;
    private final List<String> mUidsToSkip = new ArrayList<>();

    public TranscodeSkipAppsPreferenceController(Context context,
            String preferenceKey) {
        super(context, preferenceKey);
        mPackageManager = context.getPackageManager();
    }

    @Override
    public int getAvailabilityStatus() {
        return AVAILABLE;
    }

    @Override
    public void displayPreference(PreferenceScreen screen) {
        super.displayPreference(screen);
        final Context context = screen.getContext();
        mUidsToSkip.addAll(Arrays.asList(
                SystemProperties.get(SKIP_SELECTED_APPS_PROP_KEY).split(",")));
        Intent launcherIntent = new Intent(Intent.ACTION_MAIN);
        launcherIntent.addCategory(Intent.CATEGORY_LAUNCHER);
        List<ResolveInfo> apps = mPackageManager.queryIntentActivities(launcherIntent, 0);
        for (ResolveInfo app : apps) {
            String uid = String.valueOf(app.activityInfo.applicationInfo.uid);
            SwitchPreference preference = new SwitchPreference(context);
            preference.setTitle(app.loadLabel(mPackageManager));
            preference.setIcon(app.loadIcon(mPackageManager));
            preference.setKey(uid);
            preference.setChecked(isSkippedForTranscoding(uid));
            preference.setOnPreferenceChangeListener(this);

            screen.addPreference(preference);
        }
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object o) {
        boolean value = (Boolean) o;
        String uidStr = preference.getKey();
        if (value) {
            mUidsToSkip.add(uidStr);
        } else {
            mUidsToSkip.remove(uidStr);
        }
        SystemProperties.set(SKIP_SELECTED_APPS_PROP_KEY, String.join(",", mUidsToSkip));
        return true;
    }

    private boolean isSkippedForTranscoding(String uid) {
        return mUidsToSkip.contains(uid);
    }
}
