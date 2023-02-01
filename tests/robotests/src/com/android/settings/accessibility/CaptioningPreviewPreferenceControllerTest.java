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
import android.view.View;

import androidx.preference.PreferenceScreen;
import androidx.test.core.app.ApplicationProvider;

import com.android.settings.core.BasePreferenceController;
import com.android.settingslib.widget.LayoutPreference;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import org.robolectric.RobolectricTestRunner;

/** Tests for {@link CaptioningPreviewPreferenceController}. */
@RunWith(RobolectricTestRunner.class)
public class CaptioningPreviewPreferenceControllerTest {

    @Rule
    public final MockitoRule mMockitoRule = MockitoJUnit.rule();
    @Mock
    private PreferenceScreen mScreen;
    @Mock
    private ContentResolver mContentResolver;
    @Spy
    private final Context mContext = ApplicationProvider.getApplicationContext();
    private CaptioningPreviewPreferenceController mController;
    private LayoutPreference mLayoutPreference;

    @Before
    public void setUp() {
        when(mContext.getContentResolver()).thenReturn(mContentResolver);
        mController = new CaptioningPreviewPreferenceController(mContext,
                "captioning_preference_switch");
        final View view = new View(mContext);
        mLayoutPreference = new LayoutPreference(mContext, view);
        when(mScreen.findPreference(mController.getPreferenceKey())).thenReturn(mLayoutPreference);
    }

    @Test
    public void getAvailabilityStatus_shouldReturnAvailable() {
        assertThat(mController.getAvailabilityStatus())
                .isEqualTo(BasePreferenceController.AVAILABLE);
    }

    @Test
    public void onStart_registerSpecificContentObserverForSpecificKeys() {
        mController.onStart();

        for (String key : mController.CAPTIONING_FEATURE_KEYS) {
            verify(mContentResolver).registerContentObserver(Settings.Secure.getUriFor(key),
                    /* notifyForDescendants= */ false, mController.mSettingsContentObserver);
        }
    }

    @Test
    public void onStop_unregisterContentObserver() {
        mController.onStop();

        verify(mContentResolver).unregisterContentObserver(mController.mSettingsContentObserver);
    }
}
