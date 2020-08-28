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
package com.android.settings.wifi;

import android.content.Context;

import androidx.fragment.app.Fragment;
import androidx.preference.PreferenceViewHolder;

import com.android.settingslib.wifi.AccessPoint;
import com.android.settingslib.wifi.AccessPointPreference;

/**
 * An AP preference for the currently connected AP.
 *
 * Migrating from Wi-Fi SettingsLib to to WifiTrackerLib, this object will be removed in the near
 * future, please develop in {@link com.android.settingslib.wifi.LongPressWifiEntryPreference}.
 */
public class LongPressAccessPointPreference extends AccessPointPreference {

    private final Fragment mFragment;

    public LongPressAccessPointPreference(AccessPoint accessPoint, Context context,
            UserBadgeCache cache, boolean forSavedNetworks, int iconResId, Fragment fragment) {
        super(accessPoint, context, cache, iconResId, forSavedNetworks);
        mFragment = fragment;
    }

    @Override
    public void onBindViewHolder(final PreferenceViewHolder view) {
        super.onBindViewHolder(view);
        if (mFragment != null) {
            view.itemView.setOnCreateContextMenuListener(mFragment);
            view.itemView.setTag(this);
            view.itemView.setLongClickable(true);
        }
    }
}
