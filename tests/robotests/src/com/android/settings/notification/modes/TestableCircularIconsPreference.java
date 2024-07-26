/*
 * Copyright (C) 2024 The Android Open Source Project
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

package com.android.settings.notification.modes;

import android.content.Context;

import androidx.preference.PreferenceViewHolder;

import com.google.common.util.concurrent.MoreExecutors;

class TestableCircularIconsPreference extends CircularIconsPreference {

    private PreferenceViewHolder mLastViewHolder;

    TestableCircularIconsPreference(Context context) {
        super(context, MoreExecutors.directExecutor());
    }

    @Override
    public void onBindViewHolder(PreferenceViewHolder holder) {
        super.onBindViewHolder(holder);
        mLastViewHolder = holder;
    }

    @Override
    protected void notifyChanged() {
        // Calling androidx.preference.Preference.notifyChanged() will, through an internal
        // listener added by PreferenceGroupAdapter, eventually rebind the Preference to its
        // corresponding view in the RecyclerView. This will not happen to a Preference that is
        // created without a proper PreferencesScreen/RecyclerView/etc, so we simulate it here.
        if (mLastViewHolder != null) {
            onBindViewHolder(mLastViewHolder);
        }
    }
}
