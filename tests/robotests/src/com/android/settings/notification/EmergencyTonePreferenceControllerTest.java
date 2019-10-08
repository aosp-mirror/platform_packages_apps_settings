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
import android.provider.Settings.Global;
import android.telephony.TelephonyManager;

import androidx.fragment.app.FragmentActivity;
import androidx.preference.DropDownPreference;
import androidx.preference.PreferenceScreen;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

@RunWith(RobolectricTestRunner.class)
public class EmergencyTonePreferenceControllerTest {

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

    private EmergencyTonePreferenceController mController;
    private DropDownPreference mPreference;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        final Context appContext = RuntimeEnvironment.application;
        when(mContext.getSystemService(Context.TELEPHONY_SERVICE)).thenReturn(mTelephonyManager);
        when(mTelephonyManager.getCurrentPhoneType()).thenReturn(TelephonyManager.PHONE_TYPE_CDMA);
        when(mSetting.getActivity()).thenReturn(mActivity);
        when(mActivity.getSystemService(Context.TELEPHONY_SERVICE)).thenReturn(mTelephonyManager);
        when(mActivity.getContentResolver()).thenReturn(mContentResolver);
        when(mActivity.getResources()).thenReturn(appContext.getResources());
        mPreference = new DropDownPreference(appContext);
        mController = new EmergencyTonePreferenceController(mContext, mSetting, null);
        when(mScreen.findPreference(mController.getPreferenceKey())).thenReturn(mPreference);
        doReturn(mScreen).when(mSetting).getPreferenceScreen();
    }

    @Test
    public void isAvailable_cdma_shouldReturnTrue() {
        assertThat(mController.isAvailable()).isTrue();
    }

    @Test
    public void isAvailable_notCdma_shouldReturnFalse() {
        when(mTelephonyManager.getCurrentPhoneType()).thenReturn(TelephonyManager.PHONE_TYPE_GSM);

        assertThat(mController.isAvailable()).isFalse();
    }

    @Test
    public void displayPreference_emergencyToneOff_shouldSelectFirstItem() {
        Global.putInt(mContentResolver, Global.EMERGENCY_TONE, 0);

        mController.displayPreference(mScreen);

        assertThat(mPreference.getValue()).isEqualTo("0");
    }

    @Test
    public void displayPreference_emergencyToneAlert_shouldSelectSecondItem() {
        Global.putInt(mContentResolver, Global.EMERGENCY_TONE, 1);

        mController.displayPreference(mScreen);

        assertThat(mPreference.getValue()).isEqualTo("1");
    }

    @Test
    public void displayPreference_emergencyToneVibrate_shouldSelectThirdItem() {
        Global.putInt(mContentResolver, Global.EMERGENCY_TONE, 2);

        mController.displayPreference(mScreen);

        assertThat(mPreference.getValue()).isEqualTo("2");
    }

    @Test
    public void onPreferenceChanged_firstItemSelected_shouldSetEmergencyToneToOff() {
        mController.displayPreference(mScreen);

        mPreference.getOnPreferenceChangeListener().onPreferenceChange(mPreference, "0");

        assertThat(Global.getInt(mContentResolver, Global.EMERGENCY_TONE, 0)).isEqualTo(0);
    }

    @Test
    public void onPreferenceChanged_secondItemSelected_shouldSetEmergencyToneToAlert() {
        mController.displayPreference(mScreen);

        mPreference.getOnPreferenceChangeListener().onPreferenceChange(mPreference, "1");

        assertThat(Global.getInt(mContentResolver, Global.EMERGENCY_TONE, 0)).isEqualTo(1);
    }

    @Test
    public void onPreferenceChanged_thirdItemSelected_shouldSetEmergencyToneToVibrate() {
        mController.displayPreference(mScreen);

        mPreference.getOnPreferenceChangeListener().onPreferenceChange(mPreference, "2");

        assertThat(Global.getInt(mContentResolver, Global.EMERGENCY_TONE, 0)).isEqualTo(2);
    }
}
