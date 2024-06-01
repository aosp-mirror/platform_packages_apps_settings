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

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.robolectric.Shadows.shadowOf;

import android.content.ContentResolver;
import android.content.Context;
import android.graphics.drawable.Drawable;
import android.provider.Settings;

import androidx.preference.PreferenceScreen;
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
import org.robolectric.shadows.ShadowDrawable;

/** Tests for {@link AccessibilityButtonPreviewPreferenceController}. */
@RunWith(RobolectricTestRunner.class)
public class AccessibilityButtonPreviewPreferenceControllerTest {

    @Rule
    public MockitoRule mocks = MockitoJUnit.rule();
    private static final String PREF_KEY = "test_key";
    @Spy
    private final Context mContext = ApplicationProvider.getApplicationContext();
    @Mock
    private ContentResolver mContentResolver;
    @Mock
    private PreferenceScreen mPreferenceScreen;
    private AccessibilityButtonPreviewPreferenceController mController;

    @Before
    public void setUp() {
        when(mContext.getContentResolver()).thenReturn(mContentResolver);
        mController = new AccessibilityButtonPreviewPreferenceController(mContext, PREF_KEY);
        mController.mIllustrationPreference = new IllustrationPreference(mContext);
        when(mPreferenceScreen.findPreference(PREF_KEY))
                .thenReturn(mController.mIllustrationPreference);
    }

    @Test
    public void onChange_a11yBtnModeNavigationBar_getNavigationBarDrawable() {
        Settings.Secure.putInt(mContentResolver, Settings.Secure.ACCESSIBILITY_BUTTON_MODE,
                ACCESSIBILITY_BUTTON_MODE_NAVIGATION_BAR);

        mController.mContentObserver.onChange(false);

        ShadowDrawable drawable = shadowOf(mController.mIllustrationPreference.getImageDrawable());
        assertThat(drawable.getCreatedFromResId())
                .isEqualTo(R.drawable.accessibility_shortcut_type_navbar);
    }

    @Test
    public void onChange_updateFloatingMenuSize_expectedPreviewDrawable() {
        Settings.Secure.putInt(mContentResolver,
                Settings.Secure.ACCESSIBILITY_BUTTON_MODE, ACCESSIBILITY_BUTTON_MODE_FLOATING_MENU);
        Settings.Secure.putInt(mContentResolver,
                Settings.Secure.ACCESSIBILITY_FLOATING_MENU_SIZE, /* small size */ 0);
        mController.displayPreference(mPreferenceScreen);
        Drawable actualDrawable = mController.mIllustrationPreference.getImageDrawable();
        ShadowDrawable shadowDrawable = shadowOf(actualDrawable);
        assertThat(shadowDrawable.getCreatedFromResId())
                .isEqualTo(R.drawable.accessibility_shortcut_type_fab_size_small_preview);

        Settings.Secure.putInt(mContentResolver,
                Settings.Secure.ACCESSIBILITY_FLOATING_MENU_SIZE, /* large size */ 1);
        mController.mContentObserver.onChange(false);

        actualDrawable = mController.mIllustrationPreference.getImageDrawable();
        shadowDrawable = shadowOf(actualDrawable);
        assertThat(shadowDrawable.getCreatedFromResId())
                .isEqualTo(R.drawable.accessibility_shortcut_type_fab_size_large_preview);
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
