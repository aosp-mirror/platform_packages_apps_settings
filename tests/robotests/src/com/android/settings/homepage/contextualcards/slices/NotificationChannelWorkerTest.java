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

import static com.google.common.truth.Truth.assertThat;

import static org.robolectric.Shadows.shadowOf;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.net.Uri;
import android.util.ArraySet;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.shadows.ShadowPackageManager;

import java.util.Set;

@RunWith(RobolectricTestRunner.class)
public class NotificationChannelWorkerTest {
    private static final Uri URI = Uri.parse("content://com.android.settings.slices/test");
    private static final String PACKAGE_NAME = "com.test.notification.channel.slice";

    private Context mContext;
    private NotificationChannelWorker mNotificationChannelWorker;
    private ShadowPackageManager mPackageManager;
    private SharedPreferences mSharedPreferences;


    @Before
    public void setUp() {
        mContext = RuntimeEnvironment.application;
        mNotificationChannelWorker = new NotificationChannelWorker(mContext, URI);

        // Shadow PackageManager to add mock package.
        mPackageManager = shadowOf(mContext.getPackageManager());

        mSharedPreferences = mContext.getSharedPreferences(PREFS, MODE_PRIVATE);
        addInteractedPackageToSharedPreference();
    }

    @After
    public void tearDown() {
        mPackageManager.removePackage(PACKAGE_NAME);
        removeInteractedPackageFromSharedPreference();
    }

    @Test
    public void onSliceUnpinned_interactedPackageIsUninstalled_shouldRemovePackage() {
        mNotificationChannelWorker.onSliceUnpinned();

        final Set<String> interactedPackages = mSharedPreferences.getStringSet(
                PREF_KEY_INTERACTED_PACKAGES, new ArraySet<>());
        assertThat(interactedPackages.contains(PACKAGE_NAME)).isFalse();
    }

    @Test
    public void onSliceUnpinned_interactedPackageIsInstalled_shouldKeepPackage() {
        mockInteractedPackageAsInstalled();

        mNotificationChannelWorker.onSliceUnpinned();

        final Set<String> interactedPackages = mSharedPreferences.getStringSet(
                PREF_KEY_INTERACTED_PACKAGES, new ArraySet<>());
        assertThat(interactedPackages.contains(PACKAGE_NAME)).isTrue();
    }

    private void mockInteractedPackageAsInstalled() {
        final PackageInfo packageInfo = new PackageInfo();
        packageInfo.packageName = PACKAGE_NAME;
        mPackageManager.addPackage(packageInfo);
    }

    private void addInteractedPackageToSharedPreference() {
        final Set<String> interactedPackages = new ArraySet<>();
        interactedPackages.add(PACKAGE_NAME);

        mSharedPreferences.edit().putStringSet(PREF_KEY_INTERACTED_PACKAGES,
                interactedPackages).apply();
    }

    private void removeInteractedPackageFromSharedPreference() {
        if (mSharedPreferences.contains(PREF_KEY_INTERACTED_PACKAGES)) {
            mSharedPreferences.edit().remove(PREF_KEY_INTERACTED_PACKAGES).apply();
        }
    }
}
