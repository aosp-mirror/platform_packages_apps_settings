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

import static android.view.WindowManagerPolicyConstants.NAV_BAR_MODE_2BUTTON;
import static android.view.WindowManagerPolicyConstants.NAV_BAR_MODE_GESTURAL;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Mockito.when;

import android.content.Context;
import android.content.res.Resources;
import android.icu.text.MessageFormat;
import android.text.Html;

import androidx.preference.PreferenceScreen;
import androidx.test.core.app.ApplicationProvider;

import com.android.settings.R;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import org.robolectric.RobolectricTestRunner;

/** Tests for {@link AccessibilityButtonFooterPreferenceController}. */
@RunWith(RobolectricTestRunner.class)
public class AccessibilityButtonFooterPreferenceControllerTest {

    @Rule
    public final MockitoRule mockito = MockitoJUnit.rule();
    @Spy
    private final Context mContext = ApplicationProvider.getApplicationContext();
    @Spy
    private final Resources mResources = mContext.getResources();
    @Mock
    private PreferenceScreen mScreen;
    private AccessibilityButtonFooterPreferenceController mController;
    private AccessibilityFooterPreference mPreference;

    @Before
    public void setUp() {
        mController = new AccessibilityButtonFooterPreferenceController(mContext, "test_key");
        mPreference = new AccessibilityFooterPreference(mContext);
        mPreference.setKey("test_key");

        when(mScreen.findPreference(mController.getPreferenceKey())).thenReturn(mPreference);
        when(mContext.getResources()).thenReturn(mResources);
    }

    @Test
    public void displayPreference_navigationGestureEnabled_setCorrectTitle() {
        when(mResources.getInteger(com.android.internal.R.integer.config_navBarInteractionMode))
                .thenReturn(NAV_BAR_MODE_GESTURAL);

        mController.displayPreference(mScreen);

        assertThat(mPreference.getTitle().toString()).isEqualTo(
                Html.fromHtml(
                        MessageFormat.format(mContext.getString(
                                R.string.accessibility_button_gesture_description), 1, 2, 3),
                        Html.FROM_HTML_MODE_COMPACT).toString());
    }

    @Test
    public void displayPreference_navigationGestureDisabled_setCorrectTitle() {
        when(mResources.getInteger(
                com.android.internal.R.integer.config_navBarInteractionMode)).thenReturn(
                NAV_BAR_MODE_2BUTTON);

        mController.displayPreference(mScreen);

        assertThat(mPreference.getTitle().toString()).isEqualTo(
                Html.fromHtml(
                        MessageFormat.format(
                                mContext.getString(
                                        R.string.accessibility_button_description), 1, 2, 3),
                        Html.FROM_HTML_MODE_COMPACT).toString());
    }
}
