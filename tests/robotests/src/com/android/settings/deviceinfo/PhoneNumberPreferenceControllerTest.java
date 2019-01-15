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

package com.android.settings.deviceinfo;

import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.content.Context;
import androidx.preference.Preference;
import androidx.preference.PreferenceScreen;
import android.telephony.SubscriptionInfo;
import android.telephony.TelephonyManager;

import com.android.settings.R;
import com.android.settings.testutils.SettingsRobolectricTestRunner;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.util.ReflectionHelpers;

@RunWith(SettingsRobolectricTestRunner.class)
public class PhoneNumberPreferenceControllerTest {

    @Mock
    private Preference mPreference;
    @Mock
    private Preference mSecondPreference;
    @Mock
    private TelephonyManager mTelephonyManager;
    @Mock
    private SubscriptionInfo mSubscriptionInfo;
    @Mock
    private PreferenceScreen mScreen;

    private Context mContext;
    private PhoneNumberPreferenceController mController;

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);
        mContext = RuntimeEnvironment.application;
        mController = spy(new PhoneNumberPreferenceController(mContext));
        ReflectionHelpers.setField(mController, "mTelephonyManager", mTelephonyManager);
        final String prefKey = mController.getPreferenceKey();
        when(mScreen.findPreference(prefKey)).thenReturn(mPreference);
        when(mScreen.getContext()).thenReturn(mContext);
        doReturn(mSubscriptionInfo).when(mController).getSubscriptionInfo(anyInt());
        doReturn(mSecondPreference).when(mController).createNewPreference(mContext);
        when(mPreference.isVisible()).thenReturn(true);
    }

    @Test
    public void isAvailable_shouldBeTrueIfCallCapable() {
        when(mTelephonyManager.isVoiceCapable()).thenReturn(true);

        assertTrue(mController.isAvailable());
    }

    @Test
    public void isAvailable_shouldBeFalseIfNotCallCapable() {
        when(mTelephonyManager.isVoiceCapable()).thenReturn(false);

        assertFalse(mController.isAvailable());
    }

    @Test
    public void displayPreference_multiSim_shouldAddSecondPreference() {
        when(mTelephonyManager.getPhoneCount()).thenReturn(2);

        mController.displayPreference(mScreen);

        verify(mScreen).addPreference(mSecondPreference);
    }

    @Test
    public void updateState_singleSim_shouldUpdateTitleAndPhoneNumber() {
        final String phoneNumber = "1111111111";
        doReturn(phoneNumber).when(mController).getFormattedPhoneNumber(mSubscriptionInfo);
        when(mTelephonyManager.getPhoneCount()).thenReturn(1);
        mController.displayPreference(mScreen);

        mController.updateState(mPreference);

        verify(mPreference).setTitle(mContext.getString(R.string.status_number));
        verify(mPreference).setSummary(phoneNumber);
    }

    @Test
    public void updateState_multiSim_shouldUpdateTitleAndPhoneNumberOfMultiplePreferences() {
        final String phoneNumber = "1111111111";
        doReturn(phoneNumber).when(mController).getFormattedPhoneNumber(mSubscriptionInfo);
        when(mTelephonyManager.getPhoneCount()).thenReturn(2);
        mController.displayPreference(mScreen);

        mController.updateState(mPreference);

        verify(mPreference).setTitle(
                mContext.getString(R.string.status_number_sim_slot, 1 /* sim slot */));
        verify(mPreference).setSummary(phoneNumber);
        verify(mSecondPreference).setTitle(
                mContext.getString(R.string.status_number_sim_slot, 2 /* sim slot */));
        verify(mSecondPreference).setSummary(phoneNumber);
    }
}
