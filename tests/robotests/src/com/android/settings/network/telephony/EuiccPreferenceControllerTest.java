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

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.spy;
import static org.robolectric.Shadows.shadowOf;

import android.content.Context;
import android.content.Intent;
import android.telephony.TelephonyManager;
import android.telephony.euicc.EuiccManager;

import androidx.preference.Preference;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.MockitoAnnotations;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.shadows.ShadowTelephonyManager;

@RunWith(AndroidJUnit4.class)
public class EuiccPreferenceControllerTest {
    private static final int SUB_ID = 2;

    private TelephonyManager mTelephonyManager;
    private ShadowTelephonyManager mShadowTelephonyManager;

    private EuiccPreferenceController mController;
    private Preference mPreference;
    private Context mContext;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);

        mContext = spy(RuntimeEnvironment.application.getBaseContext());

        mTelephonyManager = mContext.getSystemService(TelephonyManager.class);
        mShadowTelephonyManager = shadowOf(mTelephonyManager);
        mShadowTelephonyManager.setTelephonyManagerForSubscriptionId(SUB_ID, mTelephonyManager);

        mPreference = new Preference(mContext);
        mController = new EuiccPreferenceController(mContext, "euicc");
        mController.init(SUB_ID);
        mPreference.setKey(mController.getPreferenceKey());
    }

    @Test
    public void handlePreferenceTreeClick_startActivity() {
        ArgumentCaptor<Intent> captor = ArgumentCaptor.forClass(Intent.class);
        doNothing().when(mContext).startActivity(captor.capture());

        mController.handlePreferenceTreeClick(mPreference);

        assertThat(captor.getValue().getAction()).isEqualTo(
                EuiccManager.ACTION_MANAGE_EMBEDDED_SUBSCRIPTIONS);
    }
}
