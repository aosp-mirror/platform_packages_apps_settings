/*
 * Copyright (C) 2019 The Android Open Source Project
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

package com.android.settings.homepage.contextualcards.slices;

import static android.content.Context.MODE_PRIVATE;

import android.content.Context;
import android.net.Uri;
import android.util.ArraySet;

import com.android.settings.R;
import com.android.settings.slices.CustomSliceRegistry;
import com.android.settings.slices.SliceBackgroundWorker;

import java.util.Set;

public class ContextualNotificationChannelSlice extends NotificationChannelSlice {

    public static final String PREFS = "notification_channel_slice_prefs";
    public static final String PREF_KEY_INTERACTED_PACKAGES = "interacted_packages";

    public ContextualNotificationChannelSlice(Context context) {
        super(context);
    }

    @Override
    public Uri getUri() {
        return CustomSliceRegistry.CONTEXTUAL_NOTIFICATION_CHANNEL_SLICE_URI;
    }

    @Override
    protected CharSequence getSubTitle(String packageName, int uid) {
        return mContext.getText(R.string.recently_installed_app);
    }

    @Override
    protected boolean isUserInteracted(String packageName) {
        // Check the package has been interacted on current slice or not.
        final Set<String> interactedPackages =
                mContext.getSharedPreferences(PREFS, MODE_PRIVATE)
                        .getStringSet(PREF_KEY_INTERACTED_PACKAGES, new ArraySet<>());
        return interactedPackages.contains(packageName);
    }

    @Override
    public Class<? extends SliceBackgroundWorker> getBackgroundWorkerClass() {
        return NotificationChannelWorker.class;
    }
}
