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

import static com.android.settings.homepage.contextualcards.slices.ContextualNotificationChannelSlice.PREFS;
import static com.android.settings.homepage.contextualcards.slices.ContextualNotificationChannelSlice.PREF_KEY_INTERACTED_PACKAGES;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.net.Uri;
import android.util.ArraySet;

import com.android.settings.slices.SliceBackgroundWorker;

import java.io.IOException;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class NotificationChannelWorker extends SliceBackgroundWorker<Void> {

    public NotificationChannelWorker(Context context, Uri uri) {
        super(context, uri);
    }

    @Override
    protected void onSlicePinned() {
    }

    @Override
    protected void onSliceUnpinned() {
        removeUninstalledPackages();
    }

    @Override
    public void close() throws IOException {
    }

    private void removeUninstalledPackages() {
        final SharedPreferences prefs = getContext().getSharedPreferences(PREFS, MODE_PRIVATE);
        final Set<String> interactedPackages =
                prefs.getStringSet(PREF_KEY_INTERACTED_PACKAGES, new ArraySet());
        if (interactedPackages.isEmpty()) {
            return;
        }

        final List<PackageInfo> installedPackageInfos =
                getContext().getPackageManager().getInstalledPackages(0);
        final List<String> installedPackages = installedPackageInfos.stream()
                .map(packageInfo -> packageInfo.packageName)
                .collect(Collectors.toList());
        final Set<String> newInteractedPackages = new ArraySet<>();
        for (String packageName : interactedPackages) {
            if (installedPackages.contains(packageName)) {
                newInteractedPackages.add(packageName);
            }
        }
        prefs.edit().putStringSet(PREF_KEY_INTERACTED_PACKAGES, newInteractedPackages).apply();
    }
}
