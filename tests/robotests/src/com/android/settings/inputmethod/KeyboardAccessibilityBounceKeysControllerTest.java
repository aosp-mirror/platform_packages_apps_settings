/*
 * Copyright 2024 The Android Open Source Project
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

package com.android.settings.inputmethod;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.content.Context;
import android.hardware.input.InputSettings;
import android.platform.test.annotations.DisableFlags;
import android.platform.test.annotations.EnableFlags;
import android.platform.test.flag.junit.SetFlagsRule;

import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import androidx.preference.Preference;

import com.android.settings.core.BasePreferenceController;
import com.android.settings.keyboard.Flags;
import com.android.settings.testutils.shadow.ShadowAlertDialogCompat;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.Implementation;
import org.robolectric.annotation.Implements;

@RunWith(RobolectricTestRunner.class)
@Config(shadows = {
        KeyboardAccessibilityBounceKeysControllerTest
                .ShadowKeyboardAccessibilityBounceKeysDialogFragment.class,
        com.android.settings.testutils.shadow.ShadowFragment.class,
        ShadowAlertDialogCompat.class,
})
public class KeyboardAccessibilityBounceKeysControllerTest {
    @Rule
    public final SetFlagsRule mSetFlagsRule = new SetFlagsRule();
    @Rule
    public MockitoRule mMockitoRule = MockitoJUnit.rule();
    private static final String PREFERENCE_KEY = "keyboard_a11y_page_bounce_keys";
    @Mock
    private Preference mPreference;
    @Mock
    private Fragment mFragment;
    @Mock
    private FragmentManager mFragmentManager;
    @Mock
    private FragmentTransaction mFragmentTransaction;
    @Mock
    private KeyboardAccessibilityBounceKeysDialogFragment
            mKeyboardAccessibilityBounceKeysDialogFragment;
    private Context mContext;
    private KeyboardAccessibilityBounceKeysController mKeyboardAccessibilityBounceKeysController;

    @Before
    public void setUp() {
        mContext = RuntimeEnvironment.application;
        mContext.setTheme(androidx.appcompat.R.style.Theme_AppCompat);
        mKeyboardAccessibilityBounceKeysController = new KeyboardAccessibilityBounceKeysController(
                mContext,
                PREFERENCE_KEY);
        when(mPreference.getKey()).thenReturn(PREFERENCE_KEY);
        when(mFragment.getParentFragmentManager()).thenReturn(mFragmentManager);
        when(mFragmentManager.beginTransaction()).thenReturn(mFragmentTransaction);
        mKeyboardAccessibilityBounceKeysController.setFragment(mFragment);
        ShadowKeyboardAccessibilityBounceKeysDialogFragment.setInstance(
                mKeyboardAccessibilityBounceKeysDialogFragment);
    }

    @Test
    @EnableFlags(Flags.FLAG_KEYBOARD_AND_TOUCHPAD_A11Y_NEW_PAGE_ENABLED)
    public void getAvailabilityStatus_flagIsEnabled_isAvailable() {
        assertThat(mKeyboardAccessibilityBounceKeysController.getAvailabilityStatus())
                .isEqualTo(BasePreferenceController.AVAILABLE);
    }

    @Test
    @DisableFlags(Flags.FLAG_KEYBOARD_AND_TOUCHPAD_A11Y_NEW_PAGE_ENABLED)
    public void getAvailabilityStatus_flagIsDisabled_notSupport() {
        assertThat(mKeyboardAccessibilityBounceKeysController.getAvailabilityStatus())
                .isEqualTo(BasePreferenceController.UNSUPPORTED_ON_DEVICE);
    }

    @Test
    public void setChecked_true_updateBounceKeyValue() {
        mKeyboardAccessibilityBounceKeysController.setChecked(true);
        boolean isEnabled = InputSettings.isAccessibilityBounceKeysEnabled(mContext);

        assertThat(isEnabled).isTrue();
    }

    @Test
    public void setChecked_false_updateBounceKeyValue() {
        mKeyboardAccessibilityBounceKeysController.setChecked(false);
        boolean isEnabled = InputSettings.isAccessibilityBounceKeysEnabled(mContext);

        assertThat(isEnabled).isFalse();
    }

    @Test
    public void handlePreferenceTreeClick_dialogShows() {
        mKeyboardAccessibilityBounceKeysController.handlePreferenceTreeClick(mPreference);

        verify(mKeyboardAccessibilityBounceKeysDialogFragment).show(any(FragmentManager.class),
                anyString());
    }

    /**
     * Note: Actually, shadow of KeyboardAccessibilitySlowKeysDialogFragment will not be used.
     * Instance that returned with {@link #getInstance} should be set with {@link #setInstance}
     */
    @Implements(KeyboardAccessibilityBounceKeysDialogFragment.class)
    public static class ShadowKeyboardAccessibilityBounceKeysDialogFragment {
        static KeyboardAccessibilityBounceKeysDialogFragment sInstance = null;

        @Implementation
        protected static KeyboardAccessibilityBounceKeysDialogFragment getInstance() {
            return sInstance;
        }

        public static void setInstance(KeyboardAccessibilityBounceKeysDialogFragment instance) {
            sInstance = instance;
        }
    }
}
