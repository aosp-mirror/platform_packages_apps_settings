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

package com.android.settings.network.telephony;

import static com.android.settings.core.BasePreferenceController.AVAILABLE;
import static com.android.settings.core.BasePreferenceController.CONDITIONALLY_UNAVAILABLE;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.os.PersistableBundle;
import android.telephony.CarrierConfigManager;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;

import com.android.settingslib.RestrictedPreference;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

@RunWith(RobolectricTestRunner.class)
public class CarrierPreferenceControllerTest {
    private static final int SUB_ID = 2;
    private static final String CARRIER_SETTINGS_COMPONENT = "packageName/className";

    @Mock
    private TelephonyManager mTelephonyManager;
    @Mock
    private TelephonyManager mInvalidTelephonyManager;
    @Mock
    private SubscriptionManager mSubscriptionManager;
    @Mock
    private CarrierConfigManager mCarrierConfigManager;

    private CarrierPreferenceController mController;
    private RestrictedPreference mPreference;
    private Context mContext;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);

        mContext = spy(RuntimeEnvironment.application);
        doReturn(mTelephonyManager).when(mContext).getSystemService(Context.TELEPHONY_SERVICE);
        doReturn(mSubscriptionManager).when(mContext).getSystemService(SubscriptionManager.class);
        doReturn(mTelephonyManager).when(mTelephonyManager).createForSubscriptionId(SUB_ID);
        doReturn(mInvalidTelephonyManager).when(mTelephonyManager).createForSubscriptionId(
                SubscriptionManager.INVALID_SUBSCRIPTION_ID);
        doReturn(mCarrierConfigManager).when(mContext).getSystemService(CarrierConfigManager.class);

        mPreference = new RestrictedPreference(mContext);
        mController = new CarrierPreferenceController(mContext, "mobile_data");
        mController.init(SUB_ID);
        mController.mCarrierConfigManager = mCarrierConfigManager;
        mPreference.setKey(mController.getPreferenceKey());
    }

    @Test
    public void getAvailabilityStatus_cdmaWithFlagOff_returnUnavailable() {
        doReturn(TelephonyManager.PHONE_TYPE_CDMA).when(mTelephonyManager).getPhoneType();
        final PersistableBundle bundle = new PersistableBundle();
        bundle.putBoolean(CarrierConfigManager.KEY_CARRIER_SETTINGS_ENABLE_BOOL, false);
        doReturn(bundle).when(mCarrierConfigManager).getConfigForSubId(SUB_ID);

        assertThat(mController.getAvailabilityStatus()).isEqualTo(CONDITIONALLY_UNAVAILABLE);
    }

    @Test
    public void getAvailabilityStatus_cdmaWithFlagOnreturnAvailable() {
        doReturn(TelephonyManager.PHONE_TYPE_CDMA).when(mTelephonyManager).getPhoneType();
        final PersistableBundle bundle = new PersistableBundle();
        bundle.putBoolean(CarrierConfigManager.KEY_CARRIER_SETTINGS_ENABLE_BOOL, true);
        doReturn(bundle).when(mCarrierConfigManager).getConfigForSubId(SUB_ID);

        assertThat(mController.getAvailabilityStatus()).isEqualTo(AVAILABLE);
    }

    @Test
    public void getAvailabilityStatus_gsmWithFlagOnreturnAvailable() {
        doReturn(TelephonyManager.PHONE_TYPE_GSM).when(mTelephonyManager).getPhoneType();
        final PersistableBundle bundle = new PersistableBundle();
        bundle.putBoolean(CarrierConfigManager.KEY_CARRIER_SETTINGS_ENABLE_BOOL, true);
        doReturn(bundle).when(mCarrierConfigManager).getConfigForSubId(SUB_ID);

        assertThat(mController.getAvailabilityStatus()).isEqualTo(AVAILABLE);
    }

    @Test
    public void handlePreferenceClick_activityFound_openCarrierSettingActivity() {
        final PersistableBundle bundle = new PersistableBundle();
        bundle.putString(
                CarrierConfigManager.KEY_CARRIER_SETTINGS_ACTIVITY_COMPONENT_NAME_STRING,
                CARRIER_SETTINGS_COMPONENT);
        doReturn(bundle).when(mCarrierConfigManager).getConfigForSubId(SUB_ID);
        PackageManager pm = Mockito.mock(PackageManager.class);
        doReturn(pm).when(mContext).getPackageManager();
        doReturn(new ResolveInfo()).when(pm).resolveActivity(any(Intent.class), anyInt());

        mController.handlePreferenceTreeClick(mPreference);

        final ArgumentCaptor<Intent> captor = ArgumentCaptor.forClass(Intent.class);
        verify(mContext).startActivity(captor.capture());
        final Intent intent = captor.getValue();
        assertThat(intent.getComponent()).isEqualTo(
                ComponentName.unflattenFromString(CARRIER_SETTINGS_COMPONENT));
    }

    @Test
    public void handlePreferenceClick_activityNotFound_DoNothing() {
        final PersistableBundle bundle = new PersistableBundle();
        bundle.putString(
                CarrierConfigManager.KEY_CARRIER_SETTINGS_ACTIVITY_COMPONENT_NAME_STRING,
                CARRIER_SETTINGS_COMPONENT);
        doReturn(bundle).when(mCarrierConfigManager).getConfigForSubId(SUB_ID);
        PackageManager pm = Mockito.mock(PackageManager.class);
        doReturn(pm).when(mContext).getPackageManager();
        doReturn(null).when(pm).resolveActivity(any(Intent.class), anyInt());

        mController.handlePreferenceTreeClick(mPreference);

        final ArgumentCaptor<Intent> captor = ArgumentCaptor.forClass(Intent.class);
        verify(mContext, never()).startActivity(captor.capture());
    }

    @Test
    public void handlePreferenceClick_activityNotConfigured_DoNothing() {
        final PersistableBundle bundle = new PersistableBundle();
        doReturn(bundle).when(mCarrierConfigManager).getConfigForSubId(SUB_ID);
        PackageManager pm = Mockito.mock(PackageManager.class);
        doReturn(pm).when(mContext).getPackageManager();
        doReturn(new ResolveInfo()).when(pm).resolveActivity(any(Intent.class), anyInt());

        mController.handlePreferenceTreeClick(mPreference);

        final ArgumentCaptor<Intent> captor = ArgumentCaptor.forClass(Intent.class);
        verify(mContext, never()).startActivity(captor.capture());
    }
}
