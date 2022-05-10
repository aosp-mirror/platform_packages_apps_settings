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

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.content.Context;
import android.os.Looper;
import android.telephony.SubscriptionInfo;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;

import androidx.preference.Preference;
import androidx.preference.PreferenceCategory;
import androidx.preference.PreferenceManager;
import androidx.preference.PreferenceScreen;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.android.settings.core.BasePreferenceController;
import com.android.settings.testutils.ResourcesUtils;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.ArrayList;
import java.util.List;

@RunWith(AndroidJUnit4.class)
public class PhoneNumberPreferenceControllerTest {

    private Preference mPreference;
    @Mock
    private Preference mSecondPreference;
    @Mock
    private TelephonyManager mTelephonyManager;
    @Mock
    private SubscriptionInfo mSubscriptionInfo;
    @Mock
    private SubscriptionManager mSubscriptionManager;
    private PreferenceCategory mCategory;
    private PreferenceScreen mScreen;

    private Context mContext;
    private PhoneNumberPreferenceController mController;

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);
        mContext = spy(ApplicationProvider.getApplicationContext());
        when(mContext.getSystemService(SubscriptionManager.class)).thenReturn(mSubscriptionManager);
        when(mContext.getSystemService(TelephonyManager.class)).thenReturn(mTelephonyManager);
        mController = spy(new PhoneNumberPreferenceController(mContext, "phone_number"));

        if (Looper.myLooper() == null) {
            Looper.prepare();
        }
        final PreferenceManager preferenceManager = new PreferenceManager(mContext);
        mScreen = preferenceManager.createPreferenceScreen(mContext);
        mPreference = spy(new Preference(mContext));
        mPreference.setKey(mController.getPreferenceKey());
        mPreference.setVisible(true);
        mScreen.addPreference(mPreference);
        final String categoryKey = "basic_info_category";
        mCategory = new PreferenceCategory(mContext);
        mCategory.setKey(categoryKey);
        mScreen.addPreference(mCategory);

        doReturn(mSubscriptionInfo).when(mController).getSubscriptionInfo(anyInt());
        doReturn(mSecondPreference).when(mController).createNewPreference(mContext);
    }

    @Test
    public void getAvailabilityStatus_isVoiceCapable_shouldBeAVAILABLE() {
        when(mTelephonyManager.isVoiceCapable()).thenReturn(true);

        assertThat(mController.getAvailabilityStatus()).isEqualTo(
                BasePreferenceController.AVAILABLE);
    }

    @Test
    public void getAvailabilityStatus_isNotVoiceCapable_shouldBeUNSUPPORTED_ON_DEVICE() {
        when(mTelephonyManager.isVoiceCapable()).thenReturn(false);

        assertThat(mController.getAvailabilityStatus()).isEqualTo(
                BasePreferenceController.UNSUPPORTED_ON_DEVICE);
    }

    @Test
    public void displayPreference_multiSim_shouldAddSecondPreference() {
        when(mTelephonyManager.getPhoneCount()).thenReturn(2);

        final Preference sim1Preference = new Preference(mContext);
        mCategory.addItemFromInflater(sim1Preference);
        mController.displayPreference(mScreen);

        assertThat(mCategory.getPreferenceCount()).isEqualTo(2);
    }

    @Test
    public void updateState_singleSim_shouldUpdateTitleAndPhoneNumber() {
        final String phoneNumber = "1111111111";
        doReturn(phoneNumber).when(mController).getFormattedPhoneNumber(mSubscriptionInfo);
        when(mTelephonyManager.getPhoneCount()).thenReturn(1);
        mController.displayPreference(mScreen);

        mController.updateState(mPreference);

        verify(mPreference).setTitle(ResourcesUtils.getResourcesString(mContext, "status_number"));
        verify(mPreference).setSummary(phoneNumber);
    }

    @Test
    public void updateState_multiSim_shouldUpdateTitleAndPhoneNumberOfMultiplePreferences() {
        final String phoneNumber = "1111111111";
        doReturn(phoneNumber).when(mController).getFormattedPhoneNumber(mSubscriptionInfo);
        when(mTelephonyManager.getPhoneCount()).thenReturn(2);
        mController.displayPreference(mScreen);

        mController.updateState(mPreference);

        verify(mPreference).setTitle(ResourcesUtils.getResourcesString(
                mContext, "status_number_sim_slot", 1 /* sim slot */));
        verify(mPreference).setSummary(phoneNumber);
        verify(mSecondPreference).setTitle(ResourcesUtils.getResourcesString(
                mContext, "status_number_sim_slot", 2 /* sim slot */));
        verify(mSecondPreference).setSummary(phoneNumber);
    }

    @Test
    public void getSummary_cannotGetActiveSubscriptionInfo_shouldShowUnknown() {
        when(mSubscriptionManager.getActiveSubscriptionInfoList()).thenReturn(null);

        CharSequence primaryNumber = mController.getSummary();

        assertThat(primaryNumber).isNotNull();
        assertThat(primaryNumber).isEqualTo(ResourcesUtils.getResourcesString(
                mContext, "device_info_default"));
    }

    @Test
    public void getSummary_getEmptySubscriptionInfo_shouldShowUnknown() {
        List<SubscriptionInfo> infos = new ArrayList<>();
        when(mSubscriptionManager.getActiveSubscriptionInfoList()).thenReturn(infos);

        CharSequence primaryNumber = mController.getSummary();

        assertThat(primaryNumber).isEqualTo(ResourcesUtils.getResourcesString(
                mContext, "device_info_default"));
    }
}
