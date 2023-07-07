/*
 * Copyright (C) 2018 The Android Open Source Project
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

import androidx.preference.PreferenceScreen;
import androidx.test.core.app.ApplicationProvider;

import com.android.settings.R;
import com.android.settings.core.BasePreferenceController;
import com.android.settingslib.widget.SelectorWithWidgetPreference;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import org.robolectric.RobolectricTestRunner;

/** Tests for {@link AccessibilityTimeoutController}. */
@RunWith(RobolectricTestRunner.class)
public class AccessibilityTimeoutControllerTest {

    private static final String PREF_KEY = "accessibility_control_timeout_30secs";

    @Rule
    public final MockitoRule mMockitoRule = MockitoJUnit.rule();
    @Mock
    private SelectorWithWidgetPreference mMockPref;
    @Mock
    private PreferenceScreen mScreen;
    @Mock
    private AccessibilitySettingsContentObserver mAccessibilitySettingsContentObserver;
    private Context mContext = ApplicationProvider.getApplicationContext();
    private ContentResolver mContentResolver;
    private AccessibilityTimeoutController mController;

    @Before
    public void setup() {
        mContentResolver = mContext.getContentResolver();
        mController = new AccessibilityTimeoutController(mContext, PREF_KEY,
                mAccessibilitySettingsContentObserver);
        when(mScreen.findPreference(mController.getPreferenceKey())).thenReturn(mMockPref);
        when(mMockPref.getKey()).thenReturn(PREF_KEY);
        final String prefTitle =
                mContext.getResources().getString(R.string.accessibility_timeout_30secs);
        when(mMockPref.getTitle()).thenReturn(prefTitle);
        mController.displayPreference(mScreen);
    }

    @Test
    public void getAvailabilityStatus_shouldReturnAvailable() {
        assertThat(mController.getAvailabilityStatus()).isEqualTo(
                BasePreferenceController.AVAILABLE);
    }

    @Test
    public void updateState_notChecked() {
        Settings.Secure.putString(mContentResolver,
                Settings.Secure.ACCESSIBILITY_INTERACTIVE_UI_TIMEOUT_MS, "0");

        mController.updateState(mMockPref);

        // the first checked state is set to false by control
        verify(mMockPref).setChecked(false);
        verify(mMockPref).setChecked(false);
    }

    @Test
    public void updateState_checked() {
        Settings.Secure.putString(mContentResolver,
                Settings.Secure.ACCESSIBILITY_INTERACTIVE_UI_TIMEOUT_MS, "30000");

        mController.updateState(mMockPref);

        // the first checked state is set to false by control
        verify(mMockPref).setChecked(false);
        verify(mMockPref).setChecked(true);
    }

    @Test
    public void onRadioButtonClick() {
        mController.onRadioButtonClicked(mMockPref);

        String accessibilityUiTimeoutValue = Settings.Secure.getString(mContentResolver,
                Settings.Secure.ACCESSIBILITY_INTERACTIVE_UI_TIMEOUT_MS);

        assertThat(accessibilityUiTimeoutValue).isEqualTo("30000");
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
