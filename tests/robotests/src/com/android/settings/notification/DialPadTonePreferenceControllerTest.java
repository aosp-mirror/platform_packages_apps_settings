/*
 * Copyright (C) 2017 The Android Open Source Project
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

package com.android.settings.notification;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.when;

import android.content.ContentResolver;
import android.content.Context;
import android.provider.Settings.System;
import android.telephony.TelephonyManager;

import androidx.fragment.app.FragmentActivity;
import androidx.preference.PreferenceScreen;
import androidx.preference.SwitchPreference;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

@RunWith(RobolectricTestRunner.class)
public class DialPadTonePreferenceControllerTest {

    @Mock
    private TelephonyManager mTelephonyManager;
    @Mock
    private PreferenceScreen mScreen;
    @Mock
    private FragmentActivity mActivity;
    @Mock
    private ContentResolver mContentResolver;
    @Mock
    private SoundSettings mSetting;
    @Mock
    private Context mContext;

    private DialPadTonePreferenceController mController;
    private SwitchPreference mPreference;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        when(mContext.getSystemService(Context.TELEPHONY_SERVICE)).thenReturn(mTelephonyManager);
        when(mTelephonyManager.isVoiceCapable()).thenReturn(true);
        when(mSetting.getActivity()).thenReturn(mActivity);
        when(mActivity.getSystemService(Context.TELEPHONY_SERVICE)).thenReturn(mTelephonyManager);
        when(mActivity.getContentResolver()).thenReturn(mContentResolver);
        mPreference = new SwitchPreference(RuntimeEnvironment.application);
        mController = new DialPadTonePreferenceController(mContext, mSetting, null);
        when(mScreen.findPreference(mController.getPreferenceKey())).thenReturn(mPreference);
        doReturn(mScreen).when(mSetting).getPreferenceScreen();
    }

    @Test
    public void isAvailable_voiceCapable_shouldReturnTrue() {
        assertThat(mController.isAvailable()).isTrue();
    }

    @Test
    public void isAvailable_notVoiceCapable_shouldReturnFalse() {
        when(mTelephonyManager.isVoiceCapable()).thenReturn(false);

        assertThat(mController.isAvailable()).isFalse();
    }

    @Test
    public void displayPreference_dialToneEnabled_shouldCheckedPreference() {
        System.putInt(mContentResolver, System.DTMF_TONE_WHEN_DIALING, 1);

        mController.displayPreference(mScreen);

        assertThat(mPreference.isChecked()).isTrue();
    }

    @Test
    public void displayPreference_dialToneDisabled_shouldUncheckedPreference() {
        System.putInt(mContentResolver, System.DTMF_TONE_WHEN_DIALING, 0);

        mController.displayPreference(mScreen);

        assertThat(mPreference.isChecked()).isFalse();
    }

    @Test
    public void onPreferenceChanged_preferenceChecked_shouldEnabledDialTone() {
        mController.displayPreference(mScreen);

        mPreference.getOnPreferenceChangeListener().onPreferenceChange(mPreference, true);

        assertThat(System.getInt(mContentResolver, System.DTMF_TONE_WHEN_DIALING, 1)).isEqualTo(1);
    }

    @Test
    public void onPreferenceChanged_preferenceUnchecked_shouldDisabledDialTone() {
        mController.displayPreference(mScreen);

        mPreference.getOnPreferenceChangeListener().onPreferenceChange(mPreference, false);

        assertThat(System.getInt(mContentResolver, System.DTMF_TONE_WHEN_DIALING, 1)).isEqualTo(0);
    }
}
