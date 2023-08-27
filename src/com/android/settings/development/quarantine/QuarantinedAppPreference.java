/*
 * Copyright (C) 2023 The Android Open Source Project
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

package com.android.settings.development.quarantine;

import android.content.Context;
import android.graphics.drawable.Drawable;

import androidx.preference.PreferenceViewHolder;

import com.android.settings.R;
import com.android.settingslib.applications.AppUtils;
import com.android.settingslib.applications.ApplicationsState.AppEntry;
import com.android.settingslib.utils.ThreadUtils;
import com.android.settingslib.widget.AppSwitchPreference;

public class QuarantinedAppPreference extends AppSwitchPreference {
    private final AppEntry mEntry;
    private Drawable mCacheIcon;

    public QuarantinedAppPreference(Context context, AppEntry entry) {
        super(context);
        mEntry = entry;
        mCacheIcon = AppUtils.getIconFromCache(mEntry);

        mEntry.ensureLabel(context);
        setKey(generateKey(mEntry));
        if (mCacheIcon != null) {
            setIcon(mCacheIcon);
        } else {
            setIcon(R.drawable.empty_icon);
        }
        updateState();
    }

    static String generateKey(AppEntry entry) {
        return entry.info.packageName + "|" + entry.info.uid;
    }

    public AppEntry getEntry() {
        return mEntry;
    }

    @Override
    public void onBindViewHolder(PreferenceViewHolder holder) {
        if (mCacheIcon == null) {
            ThreadUtils.postOnBackgroundThread(() -> {
                final Drawable icon = AppUtils.getIcon(getContext(), mEntry);
                ThreadUtils.postOnMainThread(() -> {
                    setIcon(icon);
                    mCacheIcon = icon;
                });
            });
        }
        super.onBindViewHolder(holder);
    }

    void updateState() {
        setTitle(mEntry.label);
        setChecked((boolean) mEntry.extraInfo);
        notifyChanged();
    }
}
