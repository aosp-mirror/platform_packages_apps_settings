/*
 * Copyright (C) 2022 The Android Open Source Project
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

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.content.ContentResolver;
import android.content.Context;
import android.provider.Settings;
import android.view.accessibility.CaptioningManager.CaptionStyle;

import androidx.preference.Preference;
import androidx.preference.PreferenceScreen;
import androidx.test.core.app.ApplicationProvider;

import com.android.settings.core.BasePreferenceController;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import org.robolectric.RobolectricTestRunner;

/** Tests for {@link CaptioningCustomController}. */
@RunWith(RobolectricTestRunner.class)
public class CaptioningCustomControllerTest {

    private static final String PREF_KEY = "custom";

    @Rule
    public final MockitoRule mMockitoRule = MockitoJUnit.rule();
    @Mock
    private PreferenceScreen mScreen;
    @Mock
    private AccessibilitySettingsContentObserver mAccessibilitySettingsContentObserver;
    private final Context mContext = ApplicationProvider.getApplicationContext();
    private ContentResolver mContentResolver;
    private CaptioningCustomController mController;
    private Preference mPreference;

    @Before
    public void setUp() {
        mContentResolver = mContext.getContentResolver();
        mController = new CaptioningCustomController(mContext, PREF_KEY,
                mAccessibilitySettingsContentObserver);
        mPreference = new Preference(mContext);
        when(mScreen.findPreference(mController.getPreferenceKey())).thenReturn(mPreference);
    }

    @Test
    public void getAvailabilityStatus_shouldReturnAvailable() {
        assertThat(mController.getAvailabilityStatus())
                .isEqualTo(BasePreferenceController.AVAILABLE);
    }


    @Test
    public void displayPreference_byDefault_shouldIsInvisible() {
        mController.displayPreference(mScreen);

        assertThat(mPreference.isVisible()).isFalse();
    }

    @Test
    public void displayPreference_customValue_shouldIsVisible() {
        Settings.Secure.putInt(mContext.getContentResolver(),
                Settings.Secure.ACCESSIBILITY_CAPTIONING_PRESET, CaptionStyle.PRESET_CUSTOM);

        mController.displayPreference(mScreen);

        assertThat(mPreference.isVisible()).isTrue();
    }

    @Test
    public void onStart_registerSpecificContentObserverForSpecificKeys() {
        mController.onStart();

        verify(mAccessibilitySettingsContentObserver).register(mContentResolver);
    }

    @Test
    public void onStop_unregisterContentObserver() {
        mController.onStop();

        verify(mAccessibilitySettingsContentObserver).unregister(mContentResolver);
    }
}
