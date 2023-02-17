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

import static android.provider.Settings.Secure.ACCESSIBILITY_BUTTON_MODE_FLOATING_MENU;
import static android.provider.Settings.Secure.ACCESSIBILITY_BUTTON_MODE_NAVIGATION_BAR;

import static com.android.settings.testutils.ImageTestUtils.drawableToBitmap;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.content.ContentResolver;
import android.content.Context;
import android.graphics.drawable.Drawable;
import android.provider.Settings;

import androidx.test.core.app.ApplicationProvider;

import com.android.settings.R;
import com.android.settingslib.widget.IllustrationPreference;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import org.robolectric.RobolectricTestRunner;

/** Tests for {@link AccessibilityButtonPreviewPreferenceController}. */
@RunWith(RobolectricTestRunner.class)
public class AccessibilityButtonPreviewPreferenceControllerTest {

    @Rule
    public MockitoRule mocks = MockitoJUnit.rule();

    @Spy
    private final Context mContext = ApplicationProvider.getApplicationContext();
    @Mock
    private ContentResolver mContentResolver;
    private AccessibilityButtonPreviewPreferenceController mController;

    @Before
    public void setUp() {
        when(mContext.getContentResolver()).thenReturn(mContentResolver);
        mController = new AccessibilityButtonPreviewPreferenceController(mContext, "test_key");
        mController.mIllustrationPreference = new IllustrationPreference(mContext);
    }

    @Test
    public void onChange_a11yBtnModeNavigationBar_getNavigationBarDrawable() {
        Settings.Secure.putInt(mContentResolver, Settings.Secure.ACCESSIBILITY_BUTTON_MODE,
                ACCESSIBILITY_BUTTON_MODE_NAVIGATION_BAR);

        mController.mContentObserver.onChange(false);

        final Drawable navigationBarDrawable = mContext.getDrawable(
                R.drawable.accessibility_button_navigation);
        assertThat(drawableToBitmap(mController.mIllustrationPreference.getImageDrawable()).sameAs(
                drawableToBitmap(navigationBarDrawable))).isTrue();
    }

    @Test
    public void onChange_updatePreviewPreferenceWithConfig_expectedPreviewDrawable() {
        Settings.Secure.putInt(mContentResolver,
                Settings.Secure.ACCESSIBILITY_BUTTON_MODE, ACCESSIBILITY_BUTTON_MODE_FLOATING_MENU);
        Settings.Secure.putInt(mContentResolver,
                Settings.Secure.ACCESSIBILITY_FLOATING_MENU_SIZE, /* small size */ 0);
        Settings.Secure.putFloat(mContentResolver,
                Settings.Secure.ACCESSIBILITY_FLOATING_MENU_OPACITY, 0.1f);

        mController.mContentObserver.onChange(false);

        final Drawable smallFloatingMenuWithTenOpacityDrawable =
                AccessibilityLayerDrawable.createLayerDrawable(mContext,
                        R.drawable.a11y_button_preview_small_floating_menu, 10);
        assertThat(
                mController.mIllustrationPreference.getImageDrawable().getConstantState())
                .isEqualTo(smallFloatingMenuWithTenOpacityDrawable.getConstantState());
    }

    @Test
    public void onResume_registerSpecificContentObserver() {
        mController.onResume();

        verify(mContentResolver).registerContentObserver(
                Settings.Secure.getUriFor(Settings.Secure.ACCESSIBILITY_BUTTON_MODE), false,
                mController.mContentObserver);
        verify(mContentResolver).registerContentObserver(
                Settings.Secure.getUriFor(Settings.Secure.ACCESSIBILITY_FLOATING_MENU_SIZE), false,
                mController.mContentObserver);
        verify(mContentResolver).registerContentObserver(
                Settings.Secure.getUriFor(Settings.Secure.ACCESSIBILITY_FLOATING_MENU_OPACITY),
                false,
                mController.mContentObserver);
    }

    @Test
    public void onPause_unregisterContentObserver() {
        mController.onPause();

        verify(mContentResolver).unregisterContentObserver(mController.mContentObserver);
    }
}
