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

import android.app.Fragment;
import android.content.Context;
import android.support.v7.preference.PreferenceViewHolder;
import android.util.AttributeSet;
import com.android.settingslib.wifi.AccessPoint;
import com.android.settingslib.wifi.AccessPointPreference;

public class LongPressAccessPointPreference extends AccessPointPreference {

    private final Fragment mFragment;

    // Used for dummy pref.
    public LongPressAccessPointPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        mFragment = null;
    }

    public LongPressAccessPointPreference(AccessPoint accessPoint, Context context,
            UserBadgeCache cache, boolean forSavedNetworks, Fragment fragment) {
        super(accessPoint, context, cache, forSavedNetworks);
        mFragment = fragment;
    }

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
