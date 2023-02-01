/*
 * Copyright (C) 2020 The Android Open Source Project
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
import static com.android.settings.accessibility.AutoclickUtils.KEY_DELAY_MODE;
import static com.android.settings.core.BasePreferenceController.AVAILABLE;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.content.Context;
import android.content.SharedPreferences;
import android.provider.Settings.Secure;

import androidx.preference.PreferenceScreen;
import androidx.test.core.app.ApplicationProvider;

import com.android.settingslib.widget.LayoutPreference;
import com.android.settingslib.widget.SelectorWithWidgetPreference;
import com.android.settingslib.widget.SelectorWithWidgetPreference.OnClickListener;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import org.robolectric.RobolectricTestRunner;

/** Tests for {@link ToggleAutoclickPreferenceController}. */
@RunWith(RobolectricTestRunner.class)
public class ToggleAutoclickPreferenceControllerTest {

    private static final String KEY_PREF_DEFAULT = "accessibility_control_autoclick_default";
    private static final String KEY_PREF_CUSTOM = "accessibility_control_autoclick_custom";
    private static final String KEY_CUSTOM_SEEKBAR = "autoclick_custom_seekbar";

    @Rule
    public final MockitoRule mMockitoRule = MockitoJUnit.rule();
    @Mock
    private PreferenceScreen mScreen;
    @Mock
    private SelectorWithWidgetPreference mDelayModePref;
    @Mock
    private LayoutPreference mSeekBarPref;
    @Mock
    private SharedPreferences mSharedPreferences;
    @Spy
    private Context mContext = ApplicationProvider.getApplicationContext();
    private ToggleAutoclickPreferenceController mController;

    @Test
    public void getAvailabilityStatus_available() {
        setUpController(KEY_PREF_DEFAULT);

        assertThat(mController.getAvailabilityStatus()).isEqualTo(AVAILABLE);
    }

    @Test
    public void setClickListenerOnDelayModePref_whenDisplay_success() {
        setUpController(KEY_PREF_DEFAULT);

        mController.displayPreference(mScreen);

        verify(mDelayModePref).setOnClickListener(any(OnClickListener.class));
    }

    @Test
    public void onStart_registerOnSharedPreferenceChangeListener() {
        doReturn(mSharedPreferences).when(mContext).getSharedPreferences(anyString(), anyInt());
        setUpController(KEY_PREF_DEFAULT);

        mController.onStart();

        verify(mSharedPreferences).registerOnSharedPreferenceChangeListener(mController);
    }

    @Test
    public void onStop_unregisterOnSharedPreferenceChangeListener() {
        doReturn(mSharedPreferences).when(mContext).getSharedPreferences(anyString(), anyInt());
        setUpController(KEY_PREF_DEFAULT);

        mController.onStop();

        verify(mSharedPreferences).unregisterOnSharedPreferenceChangeListener(mController);
    }

    @Test
    public void onRadioButtonClicked_offMode_disableAutoClick() {
        setUpController(KEY_PREF_DEFAULT);
        mController.displayPreference(mScreen);

        mController.onRadioButtonClicked(mDelayModePref);

        final boolean isEnabled = Secure.getInt(mContext.getContentResolver(),
                Secure.ACCESSIBILITY_AUTOCLICK_ENABLED, OFF) == ON;
        final int delayMs = Secure.getInt(mContext.getContentResolver(),
                Secure.ACCESSIBILITY_AUTOCLICK_DELAY, 0);
        assertThat(delayMs).isEqualTo(0);
        assertThat(isEnabled).isFalse();
    }


    @Test
    public void onRadioButtonClicked_customMode_enableAutoClick() {
        setUpController(KEY_PREF_CUSTOM);
        mController.displayPreference(mScreen);

        mController.onRadioButtonClicked(mDelayModePref);

        final boolean isEnabled = Secure.getInt(mContext.getContentResolver(),
                Secure.ACCESSIBILITY_AUTOCLICK_ENABLED, OFF) == ON;
        assertThat(isEnabled).isTrue();
    }

    @Test
    public void onSharedPreferenceChanged_customMode_shouldShowCustomSeekbar() {
        setUpController(KEY_PREF_CUSTOM);
        mController.displayPreference(mScreen);
        mController.onRadioButtonClicked(mDelayModePref);
        when(mDelayModePref.isChecked()).thenReturn(true);
        reset(mSeekBarPref);

        mController.onSharedPreferenceChanged(mSharedPreferences, KEY_DELAY_MODE);

        verify(mSeekBarPref).setVisible(true);
    }

    @Test
    public void onSharedPreferenceChanged_offMode_shouldNotShowCustomSeekbar() {
        setUpController(KEY_PREF_DEFAULT);
        mController.displayPreference(mScreen);
        mController.onRadioButtonClicked(mDelayModePref);
        reset(mSeekBarPref);

        mController.onSharedPreferenceChanged(mSharedPreferences, KEY_DELAY_MODE);

        verify(mSeekBarPref, never()).setVisible(true);
    }

    private void setUpController(String preferenceKey) {
        mController = new ToggleAutoclickPreferenceController(mContext, preferenceKey);
        when(mScreen.findPreference(preferenceKey)).thenReturn(mDelayModePref);
        when(mDelayModePref.getKey()).thenReturn(preferenceKey);
        when(mScreen.findPreference(KEY_CUSTOM_SEEKBAR)).thenReturn(mSeekBarPref);
    }
}
