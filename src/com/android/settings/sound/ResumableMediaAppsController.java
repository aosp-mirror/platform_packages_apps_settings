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

package com.android.settings.sound;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.drawable.Drawable;
import android.provider.Settings;
import android.service.media.MediaBrowserService;
import android.text.TextUtils;
import android.util.ArraySet;
import android.util.Log;

import androidx.preference.PreferenceGroup;
import androidx.preference.PreferenceScreen;
import androidx.preference.SwitchPreference;

import com.android.settings.core.BasePreferenceController;

import java.util.Collections;
import java.util.List;
import java.util.Set;

/**
 * Section of media controls settings that contains a list of potentially resumable apps
 */
public class ResumableMediaAppsController extends BasePreferenceController {
    private static final String TAG = "ResumableMediaAppsCtrl";

    private PreferenceGroup mPreferenceGroup;
    private PackageManager mPackageManager;
    private List<ResolveInfo> mResumeInfo;

    public ResumableMediaAppsController(Context context, String key) {
        super(context, key);
        mPackageManager = mContext.getPackageManager();
        Intent serviceIntent = new Intent(MediaBrowserService.SERVICE_INTERFACE);
        mResumeInfo = mPackageManager.queryIntentServices(serviceIntent, 0);
    }

    @Override
    public int getAvailabilityStatus() {
        // Update list, since this will be called when the app goes to onStart / onPause
        Intent serviceIntent = new Intent(MediaBrowserService.SERVICE_INTERFACE);
        mResumeInfo = mPackageManager.queryIntentServices(serviceIntent, 0);
        return (mResumeInfo.size() > 0) ? AVAILABLE : CONDITIONALLY_UNAVAILABLE;
    }

    @Override
    public void displayPreference(PreferenceScreen screen) {
        super.displayPreference(screen);
        mPreferenceGroup = screen.findPreference(getPreferenceKey());
        Set<String> blockedApps = getBlockedMediaApps();
        for (ResolveInfo inf : mResumeInfo) {
            String packageName = inf.getComponentInfo().packageName;
            MediaSwitchPreference pref = new MediaSwitchPreference(mContext, packageName);
            CharSequence appTitle = packageName;
            try {
                appTitle = mPackageManager.getApplicationLabel(
                        mPackageManager.getApplicationInfo(packageName, 0));
                Drawable appIcon = mPackageManager.getApplicationIcon(packageName);
                pref.setIcon(appIcon);
            } catch (PackageManager.NameNotFoundException e) {
                Log.e(TAG, "Couldn't get app title", e);
            }
            pref.setTitle(appTitle);

            pref.setOnPreferenceChangeListener((preference, status) -> {
                MediaSwitchPreference mediaPreference = (MediaSwitchPreference) preference;
                boolean isEnabled = (boolean) status;
                Log.d(TAG, "preference " + mediaPreference + " changed " + isEnabled);

                if (isEnabled) {
                    blockedApps.remove(mediaPreference.getPackageName());
                } else {
                    blockedApps.add(mediaPreference.getPackageName());
                }
                setBlockedMediaApps(blockedApps);
                return true;
            });

            pref.setChecked(!blockedApps.contains(packageName));
            mPreferenceGroup.addPreference(pref);
        }
    }

    class MediaSwitchPreference extends SwitchPreference {
        private String mPackageName;

        MediaSwitchPreference(Context context, String packageName) {
            super(context);
            mPackageName = packageName;
        }

        public String getPackageName() {
            return mPackageName;
        }
    }

    private Set<String> getBlockedMediaApps() {
        String list = Settings.Secure.getString(mContext.getContentResolver(),
                Settings.Secure.MEDIA_CONTROLS_RESUME_BLOCKED);
        if (TextUtils.isEmpty(list)) {
            return new ArraySet<>();
        }
        String[] names = list.split(":");
        Set<String> set = new ArraySet<>(names.length);
        Collections.addAll(set, names);
        return set;
    }

    private void setBlockedMediaApps(Set<String> apps) {
        if (apps == null || apps.size() == 0) {
            Settings.Secure.putString(mContext.getContentResolver(),
                    Settings.Secure.MEDIA_CONTROLS_RESUME_BLOCKED, "");
            return;
        }
        String list = String.join(":", apps);
        Settings.Secure.putString(mContext.getContentResolver(),
                Settings.Secure.MEDIA_CONTROLS_RESUME_BLOCKED, list);
    }
}
