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
 * limitations under the License
 */

package com.android.settings.homepage.contextualcards;

import static android.content.Context.MODE_PRIVATE;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.ArraySet;

import androidx.slice.Slice;
import androidx.slice.SliceMetadata;
import androidx.slice.core.SliceAction;

import com.android.settings.SettingsActivity;
import com.android.settings.applications.AppInfoBase;
import com.android.settings.homepage.contextualcards.slices.ContextualNotificationChannelSlice;
import com.android.settings.slices.CustomSliceRegistry;

import java.util.Set;

public class ContextualCardFeatureProviderImpl implements ContextualCardFeatureProvider {
    private final Context mContext;

    public ContextualCardFeatureProviderImpl(Context context) {
        mContext = context;
    }

    @Override
    public void logNotificationPackage(Slice slice) {
        if (slice == null || !slice.getUri().equals(
                CustomSliceRegistry.CONTEXTUAL_NOTIFICATION_CHANNEL_SLICE_URI)) {
            return;
        }

        final SliceAction primaryAction = SliceMetadata.from(mContext, slice).getPrimaryAction();
        final String currentPackage = primaryAction.getAction().getIntent()
                .getBundleExtra(SettingsActivity.EXTRA_SHOW_FRAGMENT_ARGUMENTS)
                .getString(AppInfoBase.ARG_PACKAGE_NAME);

        final SharedPreferences prefs = mContext.getSharedPreferences(
                ContextualNotificationChannelSlice.PREFS, MODE_PRIVATE);
        final Set<String> interactedPackages = prefs.getStringSet(
                ContextualNotificationChannelSlice.PREF_KEY_INTERACTED_PACKAGES, new ArraySet<>());

        final Set<String> newInteractedPackages = new ArraySet<>(interactedPackages);
        newInteractedPackages.add(currentPackage);
        prefs.edit().putStringSet(ContextualNotificationChannelSlice.PREF_KEY_INTERACTED_PACKAGES,
                newInteractedPackages).apply();
    }
}
