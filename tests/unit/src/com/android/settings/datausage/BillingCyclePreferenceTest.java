/*
 * Copyright (C) 2022 The Android Open Source Project
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

package com.android.settings.datausage;

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

import android.content.Context;
import android.os.INetworkManagementService;
import android.os.RemoteException;
import android.os.UserManager;
import android.telephony.TelephonyManager;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

@RunWith(AndroidJUnit4.class)
public class BillingCyclePreferenceTest {

    private Context mContext;
    private BillingCyclePreference mPreference;
    private TemplatePreference.NetworkServices mServices;
    @Mock
    private INetworkManagementService mNetManageSerice;
    @Mock
    private TelephonyManager mTelephonyManager;
    @Mock
    private UserManager mUserManager;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mContext = spy(ApplicationProvider.getApplicationContext());

        mServices = new TemplatePreference.NetworkServices();
        mServices.mNetworkService = mNetManageSerice;
        mServices.mTelephonyManager = mTelephonyManager;
        mServices.mUserManager = mUserManager;

        doReturn(mTelephonyManager).when(mTelephonyManager)
                .createForSubscriptionId(anyInt());

        mPreference = spy(new BillingCyclePreference(mContext, null /* attrs */));
        mPreference.setTemplate(null, 0, mServices);
    }

    @Test
    public void testPreferenceUpdate_onMobileDataEnabledChange_accessDataEnabledApi() {
        try {
            doReturn(true).when(mNetManageSerice).isBandwidthControlEnabled();
        } catch (RemoteException exception) {}
        doReturn(true).when(mUserManager).isAdminUser();
        mPreference.onMobileDataEnabledChange();

        verify(mTelephonyManager)
                .isDataEnabledForReason(TelephonyManager.DATA_ENABLED_REASON_USER);
    }
}