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

package com.android.settings.wallpaper;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.text.TextUtils;

import androidx.preference.Preference;
import androidx.preference.PreferenceScreen;

import com.android.settings.core.BasePreferenceController;
import com.android.settingslib.core.lifecycle.LifecycleObserver;
import com.android.settingslib.core.lifecycle.events.OnStart;

import java.util.List;
import java.util.stream.Collectors;

public class WallpaperTypePreferenceController extends BasePreferenceController
        implements LifecycleObserver, OnStart {

    private PreferenceScreen mScreen;

    public WallpaperTypePreferenceController(Context context, String key) {
        super(context, key);
    }

    @Override
    public void displayPreference(PreferenceScreen screen) {
        super.displayPreference(screen);
        mScreen = screen;
    }

    @Override
    public int getAvailabilityStatus() {
        return AVAILABLE;
    }

    @Override
    public boolean handlePreferenceTreeClick(Preference preference) {
        if (preference.getIntent() == null) {
            return super.handlePreferenceTreeClick(preference);
        }
        mContext.startActivity(preference.getIntent());
        return true;
    }

    @Override
    public void onStart() {
        populateWallpaperTypes();
    }

    private void populateWallpaperTypes() {
        // Search for activities that satisfy the ACTION_SET_WALLPAPER action
        final Intent intent = new Intent(Intent.ACTION_SET_WALLPAPER);
        final PackageManager pm = mContext.getPackageManager();
        final List<ResolveInfo> rList = pm.queryIntentActivities(intent,
                PackageManager.MATCH_DEFAULT_ONLY);

        removeUselessExistingPreference(rList);
        mScreen.setOrderingAsAdded(false);
        // Add Preference items for each of the matching activities
        for (ResolveInfo info : rList) {
            final String packageName = info.activityInfo.packageName;
            Preference pref = mScreen.findPreference(packageName);
            if (pref == null) {
                pref = new Preference(mScreen.getContext());
            }
            final Intent prefIntent = new Intent(intent).addFlags(
                    Intent.FLAG_ACTIVITY_FORWARD_RESULT);
            prefIntent.setComponent(new ComponentName(packageName, info.activityInfo.name));
            pref.setIntent(prefIntent);
            pref.setKey(packageName);
            CharSequence label = info.loadLabel(pm);
            if (label == null) {
                label = packageName;
            }
            pref.setTitle(label);
            pref.setIcon(info.loadIcon(pm));
            mScreen.addPreference(pref);
        }
    }

    private void removeUselessExistingPreference(List<ResolveInfo> rList) {
        final int count = mScreen.getPreferenceCount();
        if (count <= 0) {
            return;
        }
        for (int i = count - 1; i >= 0; i--) {
            final Preference pref = mScreen.getPreference(i);
            final List<ResolveInfo> result = rList.stream().filter(
                    rInfo -> TextUtils.equals(pref.getKey(),
                            rInfo.activityInfo.packageName)).collect(Collectors.toList());
            if (result.isEmpty()) {
                mScreen.removePreference(pref);
            }
        }
    }
}