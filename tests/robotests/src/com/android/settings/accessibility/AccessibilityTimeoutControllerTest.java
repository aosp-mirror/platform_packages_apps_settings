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

import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.content.ContentResolver;
import android.content.Context;
import android.provider.Settings;

import androidx.preference.Preference;
import androidx.preference.PreferenceScreen;

import com.android.settings.R;
import com.android.settingslib.core.lifecycle.Lifecycle;
import com.android.settingslib.widget.RadioButtonPreference;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

@RunWith(RobolectricTestRunner.class)
public class AccessibilityTimeoutControllerTest
        implements AccessibilityTimeoutController.OnChangeListener {
    private static final String PREF_KEY = "accessibility_control_timeout_30secs";

    private AccessibilityTimeoutController mController;

    @Mock
    private RadioButtonPreference mMockPref;
    private Context mContext;
    private ContentResolver mContentResolver;

    @Mock
    private PreferenceScreen mScreen;

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);
        mContext = RuntimeEnvironment.application;
        mController = new AccessibilityTimeoutController(mContext, mock(Lifecycle.class), PREF_KEY);
        mController.setOnChangeListener(this);
        mContentResolver = mContext.getContentResolver();
        String prefTitle = mContext.getResources().getString(R.string.accessibility_timeout_30secs);

        when(mScreen.findPreference(mController.getPreferenceKey())).thenReturn(mMockPref);
        when(mMockPref.getKey()).thenReturn(PREF_KEY);
        when(mMockPref.getTitle()).thenReturn(prefTitle);
        mController.displayPreference(mScreen);
    }

    @Override
    public void onCheckedChanged(Preference preference) {
        mController.updateState(preference);
    }

    @Test
    public void isAvailable() {
        assertTrue(mController.isAvailable());
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
}
