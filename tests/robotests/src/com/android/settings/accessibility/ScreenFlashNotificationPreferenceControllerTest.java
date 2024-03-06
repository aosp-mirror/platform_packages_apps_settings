/*
 * Copyright (C) 2023 The Android Open Source Project
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

import static com.android.settings.accessibility.AccessibilityUtil.State.OFF;
import static com.android.settings.accessibility.AccessibilityUtil.State.ON;
import static com.android.settings.accessibility.FlashNotificationsUtil.DEFAULT_SCREEN_FLASH_COLOR;
import static com.android.settings.core.BasePreferenceController.AVAILABLE;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.content.ContentResolver;
import android.content.Context;
import android.graphics.Color;
import android.provider.Settings;

import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;
import androidx.preference.Preference;
import androidx.preference.PreferenceScreen;

import com.android.settings.R;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.Implementation;
import org.robolectric.annotation.Implements;
import org.robolectric.annotation.Resetter;

import java.util.function.Consumer;

@RunWith(RobolectricTestRunner.class)
@Config(shadows = {
        ScreenFlashNotificationPreferenceControllerTest
                .ShadowScreenFlashNotificationColorDialogFragment.class,
        ShadowFlashNotificationsUtils.class,
        com.android.settings.testutils.shadow.ShadowFragment.class,
})
public class ScreenFlashNotificationPreferenceControllerTest {
    private static final String PREFERENCE_KEY = "preference_key";
    private static final String COLOR_DESCRIPTION_TEXT = "Colorful";

    @Rule
    public MockitoRule mMockitoRule = MockitoJUnit.rule();

    @Mock
    private PreferenceScreen mPreferenceScreen;
    @Mock
    private Preference mPreference;
    @Mock
    private Fragment mParentFragment;
    @Mock
    private FragmentManager mFragmentManager;
    @Mock
    private ScreenFlashNotificationColorDialogFragment mDialogFragment;

    private ScreenFlashNotificationPreferenceController mController;
    private ContentResolver mContentResolver;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        FragmentActivity fragmentActivity = Robolectric.setupActivity(FragmentActivity.class);
        Context context = fragmentActivity.getApplicationContext();
        ShadowScreenFlashNotificationColorDialogFragment.setInstance(mDialogFragment);
        ShadowFlashNotificationsUtils.setColorDescriptionText(COLOR_DESCRIPTION_TEXT);

        mContentResolver = context.getContentResolver();
        mController = new ScreenFlashNotificationPreferenceController(context, PREFERENCE_KEY);
        when(mPreferenceScreen.findPreference(PREFERENCE_KEY)).thenReturn(mPreference);
        when(mPreference.getKey()).thenReturn(PREFERENCE_KEY);
        mController.setParentFragment(mParentFragment);
        when(mParentFragment.getParentFragmentManager()).thenReturn(mFragmentManager);
    }

    @After
    public void tearDown() {
        ShadowScreenFlashNotificationColorDialogFragment.reset();
    }

    @Test
    public void getAvailabilityStatus() {
        assertThat(mController.getAvailabilityStatus()).isEqualTo(AVAILABLE);
    }

    @Test
    public void isChecked_setOff_assertFalse() {
        Settings.System.putInt(mContentResolver, Settings.System.SCREEN_FLASH_NOTIFICATION, OFF);
        assertThat(mController.isChecked()).isFalse();
    }

    @Test
    public void isChecked_setOn_assertTrue() {
        Settings.System.putInt(mContentResolver, Settings.System.SCREEN_FLASH_NOTIFICATION, ON);
        assertThat(mController.isChecked()).isTrue();
    }

    @Test
    public void setChecked_whenTransparentColor_setTrue_assertNotTransparentColor() {
        Settings.System.putInt(mContentResolver,
                Settings.System.SCREEN_FLASH_NOTIFICATION_COLOR, Color.TRANSPARENT);
        mController.setChecked(true);
        assertThat(Settings.System.getInt(mContentResolver,
                Settings.System.SCREEN_FLASH_NOTIFICATION_COLOR, 0)).isEqualTo(
                DEFAULT_SCREEN_FLASH_COLOR);
    }

    @Test
    public void setChecked_whenNotTransparent_setTrue_assertSameColor() {
        Settings.System.putInt(mContentResolver,
                Settings.System.SCREEN_FLASH_NOTIFICATION_COLOR, 0x4D0000FF);
        mController.setChecked(true);
        assertThat(Settings.System.getInt(mContentResolver,
                Settings.System.SCREEN_FLASH_NOTIFICATION_COLOR, 0))
                .isEqualTo(0x4D0000FF);
    }

    @Test
    public void setChecked_setTrue_assertOn() {
        mController.setChecked(true);
        assertThat(
                Settings.System.getInt(mContentResolver, Settings.System.SCREEN_FLASH_NOTIFICATION,
                        OFF)).isEqualTo(ON);
    }

    @Test
    public void setChecked_setFalse_assertOff() {
        mController.setChecked(false);
        assertThat(
                Settings.System.getInt(mContentResolver, Settings.System.SCREEN_FLASH_NOTIFICATION,
                        OFF)).isEqualTo(OFF);
    }

    @Test
    public void getSliceHighlightMenuRes() {
        assertThat(mController.getSliceHighlightMenuRes())
                .isEqualTo(R.string.menu_key_accessibility);
    }

    @Test
    public void getSummary() {
        assertThat(mController.getSummary()).isEqualTo(COLOR_DESCRIPTION_TEXT);
    }

    @Test
    public void displayPreference() {
        mController.displayPreference(mPreferenceScreen);
        verify(mPreference).setSummary(COLOR_DESCRIPTION_TEXT);
    }

    @Test
    public void handlePreferenceTreeClick() {
        mController.handlePreferenceTreeClick(mPreference);
        verify(mDialogFragment).show(any(FragmentManager.class), anyString());
    }

    /**
     * Note: Actually, shadow of ScreenFlashNotificationColorDialogFragment will not be used.
     * Instance that returned with {@link #getInstance} should be set with {@link #setInstance}
     */
    @Implements(ScreenFlashNotificationColorDialogFragment.class)
    public static class ShadowScreenFlashNotificationColorDialogFragment {
        static ScreenFlashNotificationColorDialogFragment sInstance = null;

        @Implementation
        protected static ScreenFlashNotificationColorDialogFragment getInstance(
                int initialColor, Consumer<Integer> colorConsumer) {
            return sInstance;
        }

        public static void setInstance(ScreenFlashNotificationColorDialogFragment instance) {
            sInstance = instance;
        }

        @Resetter
        public static void reset() {
            sInstance = null;
        }
    }
}
