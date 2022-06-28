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

import static android.content.Context.MODE_PRIVATE;

import static com.android.settings.accessibility.ToggleAutoclickPreferenceController.AUTOCLICK_CUSTOM_MODE;
import static com.android.settings.accessibility.ToggleAutoclickPreferenceController.AUTOCLICK_OFF_MODE;
import static com.android.settings.accessibility.ToggleAutoclickPreferenceController.KEY_AUTOCLICK_CUSTOM_SEEKBAR;
import static com.android.settings.accessibility.ToggleAutoclickPreferenceController.KEY_DELAY_MODE;
import static com.android.settings.core.BasePreferenceController.AVAILABLE;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.content.Context;
import android.content.SharedPreferences;
import android.provider.Settings.Secure;

import androidx.lifecycle.LifecycleObserver;
import androidx.preference.PreferenceScreen;
import androidx.test.core.app.ApplicationProvider;

import com.android.settings.accessibility.ToggleAutoclickPreferenceController.OnChangeListener;
import com.android.settingslib.core.lifecycle.Lifecycle;
import com.android.settingslib.widget.LayoutPreference;
import com.android.settingslib.widget.SelectorWithWidgetPreference;
import com.android.settingslib.widget.SelectorWithWidgetPreference.OnClickListener;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;

import java.util.Map;

/** Tests for {@link ToggleAutoclickPreferenceController}. */
@RunWith(RobolectricTestRunner.class)
public class ToggleAutoclickPreferenceControllerTest {

    @Mock
    private PreferenceScreen mScreen;

    @Mock
    private SelectorWithWidgetPreference mDelayModePref;

    @Mock
    private OnChangeListener mOnChangeListener;

    @Mock
    private LayoutPreference mSeekBarPref;

    @Mock
    private Map<String, Integer> mAccessibilityAutoclickKeyToValueMap;

    private ToggleAutoclickPreferenceController mController;
    private SharedPreferences mSharedPreferences;
    private final String mPrefKey = "prefKey";
    private final Context mContext = ApplicationProvider.getApplicationContext();

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);

        mController = new ToggleAutoclickPreferenceController(mContext, mPrefKey);
        mController.mAccessibilityAutoclickKeyToValueMap = mAccessibilityAutoclickKeyToValueMap;
        mSharedPreferences =
                mContext.getSharedPreferences(mContext.getPackageName(), MODE_PRIVATE);

        when(mScreen.findPreference(mPrefKey)).thenReturn(mDelayModePref);
        when(mScreen.findPreference(KEY_AUTOCLICK_CUSTOM_SEEKBAR)).thenReturn(mSeekBarPref);
        when(mAccessibilityAutoclickKeyToValueMap.get(mDelayModePref.getKey())).thenReturn(
                AUTOCLICK_OFF_MODE);
    }

    @Test
    public void getAvailabilityStatus_available() {
        assertThat(mController.getAvailabilityStatus()).isEqualTo(AVAILABLE);
    }

    @Test
    public void setClickListenerOnDelayModePref_whenDisplay_success() {
        mController.displayPreference(mScreen);

        verify(mDelayModePref).setOnClickListener(any(OnClickListener.class));
    }

    @Test
    public void constructor_hasLifecycle_addObserver() {
        final Lifecycle lifecycle = mock(Lifecycle.class);
        mController = new ToggleAutoclickPreferenceController(mContext, lifecycle, mPrefKey);

        verify(lifecycle).addObserver(any(LifecycleObserver.class));
    }

    @Test
    public void onRadioButtonClicked_offMode_disableAutoClick() {
        when(mAccessibilityAutoclickKeyToValueMap.get(mPrefKey)).thenReturn(AUTOCLICK_OFF_MODE);

        mController.displayPreference(mScreen);
        mController.onRadioButtonClicked(any(SelectorWithWidgetPreference.class));
        final boolean isEnabled = Secure.getInt(mContext.getContentResolver(),
                Secure.ACCESSIBILITY_AUTOCLICK_ENABLED, /* def= */ 0) == 1;
        final int delayMs = Secure.getInt(mContext.getContentResolver(),
                Secure.ACCESSIBILITY_AUTOCLICK_DELAY, /* def= */ 0);
        final int keyDelayMode = mSharedPreferences.getInt(KEY_DELAY_MODE, AUTOCLICK_CUSTOM_MODE);

        assertThat(keyDelayMode).isEqualTo(AUTOCLICK_OFF_MODE);
        assertThat(delayMs).isEqualTo(/* expected= */ 0);
        assertThat(isEnabled).isFalse();
    }

    @Test
    public void onRadioButtonClicked_customMode_enableAutoClick() {
        when(mAccessibilityAutoclickKeyToValueMap.get(mDelayModePref.getKey())).thenReturn(
                AUTOCLICK_CUSTOM_MODE);
        when(mAccessibilityAutoclickKeyToValueMap.get(mPrefKey)).thenReturn(AUTOCLICK_CUSTOM_MODE);

        mController.displayPreference(mScreen);
        mController.onRadioButtonClicked(any(SelectorWithWidgetPreference.class));
        final boolean isEnabled = Secure.getInt(mContext.getContentResolver(),
                Secure.ACCESSIBILITY_AUTOCLICK_ENABLED, /* def= */ 0) == 1;
        final int keyDelayMode = mSharedPreferences.getInt(KEY_DELAY_MODE, AUTOCLICK_CUSTOM_MODE);

        assertThat(keyDelayMode).isEqualTo(AUTOCLICK_CUSTOM_MODE);
        assertThat(isEnabled).isTrue();
    }

    @Test
    public void onRadioButtonClicked_hasListener_runOnCheckedChanged() {
        when(mAccessibilityAutoclickKeyToValueMap.get(mDelayModePref.getKey())).thenReturn(
                AUTOCLICK_CUSTOM_MODE);
        when(mAccessibilityAutoclickKeyToValueMap.get(mPrefKey)).thenReturn(AUTOCLICK_CUSTOM_MODE);

        mController.setOnChangeListener(mOnChangeListener);
        mController.displayPreference(mScreen);
        mController.onRadioButtonClicked(any(SelectorWithWidgetPreference.class));

        verify(mOnChangeListener).onCheckedChanged(mDelayModePref);
    }
}
