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

import android.content.Context;
import android.content.SharedPreferences;
import android.net.Uri;
import android.util.ArraySet;

import com.android.settings.R;
import com.android.settings.slices.CustomSliceRegistry;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

import java.util.Set;

@RunWith(RobolectricTestRunner.class)
public class ContextualNotificationChannelSliceTest {

    private static final String PACKAGE_NAME = "package_name";

    private Context mContext;
    private ContextualNotificationChannelSlice mNotificationChannelSlice;
    private SharedPreferences mSharedPreferences;

    @Before
    public void setUp() {
        mContext = RuntimeEnvironment.application;
        mNotificationChannelSlice = new ContextualNotificationChannelSlice(mContext);
        mSharedPreferences = mContext.getSharedPreferences(PREFS, MODE_PRIVATE);
    }

    @After
    public void tearDown() {
        removeInteractedPackageFromSharedPreference();
    }

    @Test
    public void getUri_shouldBeContextualNotificationChannelSliceUri() {
        final Uri uri = mNotificationChannelSlice.getUri();

        assertThat(uri).isEqualTo(CustomSliceRegistry.CONTEXTUAL_NOTIFICATION_CHANNEL_SLICE_URI);
    }

    @Test
    public void getSubTitle_shouldBeRecentlyInstalledApp() {
        final CharSequence subTitle = mNotificationChannelSlice.getSubTitle("com.test.package", 0);

        assertThat(subTitle).isEqualTo(mContext.getText(R.string.recently_installed_app));
    }

    @Test
    public void isUserInteracted_hasInteractedPackage_shouldBeTrue() {
        addInteractedPackageToSharedPreference();

        final boolean isInteracted = mNotificationChannelSlice.isUserInteracted(PACKAGE_NAME);

        assertThat(isInteracted).isTrue();
    }

    @Test
    public void isUserInteracted_noInteractedPackage_shouldBeFalse() {
        final boolean isInteracted = mNotificationChannelSlice.isUserInteracted(PACKAGE_NAME);

        assertThat(isInteracted).isFalse();
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
