/*
 * Copyright (C) 2021 The Android Open Source Project
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

package com.android.settings.accessibility;

import static com.google.common.truth.Truth.assertThat;

import android.content.Context;

import androidx.preference.PreferenceManager;
import androidx.preference.PreferenceScreen;
import androidx.test.core.app.ApplicationProvider;

import com.android.settings.R;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

/** Tests for {@link CaptionFooterPreferenceController}. */
@RunWith(RobolectricTestRunner.class)
public class CaptionFooterPreferenceControllerTest {

    private static final String TEST_KEY = "test_key";
    private final Context mContext = ApplicationProvider.getApplicationContext();
    private PreferenceScreen mScreen;
    private CaptionFooterPreferenceController mController;

    @Before
    public void setUp() {
        final PreferenceManager preferenceManager = new PreferenceManager(mContext);
        mScreen = preferenceManager.createPreferenceScreen(mContext);
        final AccessibilityFooterPreference footerPreference =
                new AccessibilityFooterPreference(mContext);
        footerPreference.setKey(TEST_KEY);
        mScreen.addPreference(footerPreference);
        mController = new CaptionFooterPreferenceController(mContext, TEST_KEY);
    }

    @Test
    public void onPreferenceChange_shouldSetCorrectIconContentDescription() {
        mController.displayPreference(mScreen);

        final AccessibilityFooterPreference footerPreference = mScreen.findPreference(TEST_KEY);
        final String packageName = mContext.getString(R.string.accessibility_captioning_title);
        final String iconContentDescription = mContext.getString(
                R.string.accessibility_introduction_title,
                packageName);
        assertThat(footerPreference.getIconContentDescription()).isEqualTo(iconContentDescription);
    }
}
