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

package com.android.settings.deviceinfo.imei;

import static android.telephony.TelephonyManager.PHONE_TYPE_CDMA;
import static android.telephony.TelephonyManager.PHONE_TYPE_GSM;
import static android.telephony.TelephonyManager.PHONE_TYPE_NONE;

import static com.android.settings.core.BasePreferenceController.AVAILABLE;

import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.anyInt;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.content.Context;
import android.content.res.Resources;
import android.os.UserManager;
import android.telephony.TelephonyManager;

import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.preference.Preference;
import androidx.preference.PreferenceCategory;
import androidx.preference.PreferenceScreen;

import com.android.settings.R;
import com.android.settings.deviceinfo.simstatus.SlotSimStatus;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

@RunWith(RobolectricTestRunner.class)
@Config(shadows = {
        com.android.settings.testutils.shadow.ShadowFragment.class,
})
public class ImeiInfoPreferenceControllerTest {

    @Mock
    private Preference mPreference;
    @Mock
    private Preference mSecondSimPreference;
    @Mock
    private PreferenceScreen mScreen;
    @Mock
    private TelephonyManager mTelephonyManager;
    @Mock
    private UserManager mUserManager;
    @Mock
    private Fragment mFragment;
    @Mock
    private PreferenceCategory mCategory;

    private Context mContext;
    private Resources mResources;
    private ImeiInfoPreferenceController mDefaultController;
    private ImeiInfoPreferenceController mController;
    private ImeiInfoPreferenceController mSecondController;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mContext = spy(RuntimeEnvironment.application);

        mResources = spy(mContext.getResources());
        when(mContext.getResources()).thenReturn(mResources);
        when(mResources.getBoolean(R.bool.config_show_sim_info)).thenReturn(true);

        mockService(Context.TELEPHONY_SERVICE, TelephonyManager.class, mTelephonyManager);
        mockService(Context.USER_SERVICE, UserManager.class, mUserManager);

        when(mScreen.getContext()).thenReturn(mContext);
        final String categoryKey = "device_detail_category";
        when(mScreen.findPreference(categoryKey)).thenReturn(mCategory);
    }

    private ImeiInfoPreferenceController createPreferenceController(SlotSimStatus slotSimStatus,
        String key, Preference preference, int phoneType) {
        ImeiInfoPreferenceController controller =
                spy(new ImeiInfoPreferenceController(mContext, key) {
                    public int getPhoneType(int slotId) {
                        return phoneType;
                    }
                });
        controller.init(mFragment, slotSimStatus);
        doReturn(AVAILABLE).when(controller).getAvailabilityStatus();
        doReturn(preference).when(controller).createNewPreference(mContext);

        when(mScreen.findPreference(key)).thenReturn(preference);
        when(preference.getKey()).thenReturn(key);
        when(preference.isVisible()).thenReturn(true);
        return controller;
    }

    private void setupPhoneCount(int count, int phoneType1, int phoneType2) {
        when(mTelephonyManager.getPhoneCount()).thenReturn(count);

        SlotSimStatus slotSimStatus = new SlotSimStatus(mContext);
        mController = createPreferenceController(slotSimStatus,
                "imei_info1", mPreference, phoneType1);
        mSecondController = createPreferenceController(slotSimStatus,
                "imei_info2", mSecondSimPreference, phoneType2);
    }

    @Test
    public void updatePreference_simSlotWithoutSim_notSetEnabled() {
        mSecondController = createPreferenceController(null,
                "imei_info2", mSecondSimPreference, PHONE_TYPE_NONE);

        mSecondController.updatePreference(mSecondSimPreference, -1);

        verify(mSecondSimPreference, never()).setEnabled(anyBoolean());
    }

    @Ignore
    @Test
    public void displayPreference_multiSimGsm_shouldAddSecondPreference() {
        setupPhoneCount(2, PHONE_TYPE_GSM, PHONE_TYPE_GSM);

        mDefaultController.displayPreference(mScreen);

        verify(mCategory).addPreference(mSecondSimPreference);
    }

    @Test
    public void displayPreference_singleSimCdmaPhone_shouldSetSingleSimCdmaTitleAndMeid() {
        setupPhoneCount(1, PHONE_TYPE_CDMA, PHONE_TYPE_NONE);

        final String meid = "Tap to show info";
        when(mTelephonyManager.getMeid(anyInt())).thenReturn(meid);

        mController.displayPreference(mScreen);
        mController.updateState(mPreference);

        verify(mPreference).setTitle(mContext.getString(R.string.status_meid_number));
        verify(mPreference).setSummary(meid);
    }

    @Test
    public void displayPreference_multiSimCdmaPhone_shouldSetMultiSimCdmaTitleAndMeid() {
        setupPhoneCount(2, PHONE_TYPE_CDMA, PHONE_TYPE_CDMA);

        final String meid = "Tap to show info";
        when(mTelephonyManager.getMeid(anyInt())).thenReturn(meid);

        mController.displayPreference(mScreen);
        mController.updateState(mPreference);
        mSecondController.displayPreference(mScreen);
        mSecondController.updateState(mSecondSimPreference);

        verify(mPreference).setTitle(mContext.getString(R.string.meid_multi_sim, 1 /* sim slot */));
        verify(mSecondSimPreference).setTitle(
                mContext.getString(R.string.meid_multi_sim, 2 /* sim slot */));
        verify(mPreference).setSummary(meid);
        verify(mSecondSimPreference).setSummary(meid);
    }

    @Test
    public void displayPreference_singleSimGsmPhone_shouldSetSingleSimGsmTitleAndImei() {
        setupPhoneCount(1, PHONE_TYPE_GSM, PHONE_TYPE_NONE);

        final String imei = "Tap to show info";
        when(mTelephonyManager.getImei(anyInt())).thenReturn(imei);

        mController.displayPreference(mScreen);
        mController.updateState(mPreference);

        verify(mPreference).setTitle(mContext.getString(R.string.status_imei));
        verify(mPreference).setSummary(imei);
    }

    @Test
    public void displayPreference_multiSimGsmPhone_shouldSetMultiSimGsmTitleAndImei() {
        setupPhoneCount(2, PHONE_TYPE_GSM, PHONE_TYPE_GSM);

        final String imei = "Tap to show info";
        when(mTelephonyManager.getImei(anyInt())).thenReturn(imei);

        mController.displayPreference(mScreen);
        mController.updateState(mPreference);
        mSecondController.displayPreference(mScreen);
        mSecondController.updateState(mSecondSimPreference);

        verify(mPreference).setTitle(mContext.getString(R.string.imei_multi_sim, 1 /* sim slot */));
        verify(mSecondSimPreference).setTitle(
                mContext.getString(R.string.imei_multi_sim, 2 /* sim slot */));
        verify(mPreference).setSummary(imei);
        verify(mSecondSimPreference).setSummary(imei);
    }

    @Test
    public void handlePreferenceTreeClick_shouldStartDialogFragment() {
        setupPhoneCount(1, PHONE_TYPE_GSM, PHONE_TYPE_NONE);

        when(mFragment.getChildFragmentManager())
                .thenReturn(mock(FragmentManager.class, Answers.RETURNS_DEEP_STUBS));
        when(mPreference.getTitle()).thenReturn("SomeTitle");
        mController.displayPreference(mScreen);

        mController.handlePreferenceTreeClick(mPreference);

        verify(mFragment).getChildFragmentManager();
    }

    private <T> void mockService(String serviceName, Class<T> serviceClass, T service) {
        when(mContext.getSystemServiceName(serviceClass)).thenReturn(serviceName);
        when(mContext.getSystemService(serviceName)).thenReturn(service);
    }
}
