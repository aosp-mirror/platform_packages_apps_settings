/*
 * Copyright (C) 2019 The Android Open Source Project
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

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.content.Context;
import android.content.Intent;
import android.provider.Settings;
import android.telephony.SubscriptionInfo;
import android.telephony.SubscriptionManager;
import android.telephony.euicc.EuiccManager;

import androidx.fragment.app.Fragment;
import androidx.preference.Preference;
import androidx.preference.PreferenceScreen;

import com.android.settings.network.SubscriptionUtil;
import com.android.settings.security.ConfirmSimDeletionPreferenceController;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

import java.util.ArrayList;
import java.util.Arrays;

@RunWith(RobolectricTestRunner.class)
public class DeleteSimProfilePreferenceControllerTest {
    private static final String PREF_KEY = "delete_profile_key";
    private static final int REQUEST_CODE = 4321;
    private static final int SUB_ID = 1234;
    private static final int OTHER_ID = 5678;

    @Mock
    private Fragment mFragment;
    @Mock
    private SubscriptionInfo mSubscriptionInfo;
    @Mock
    private PreferenceScreen mScreen;

    private Context mContext;
    private Preference mPreference;
    private DeleteSimProfilePreferenceController mController;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mContext = spy(RuntimeEnvironment.application);

        SubscriptionUtil.setAvailableSubscriptionsForTesting(Arrays.asList(mSubscriptionInfo));
        when(mSubscriptionInfo.getSubscriptionId()).thenReturn(SUB_ID);
        when(mSubscriptionInfo.isEmbedded()).thenReturn(true);

        mPreference = new Preference(mContext);
        mPreference.setKey(PREF_KEY);
        when(mScreen.findPreference(PREF_KEY)).thenReturn(mPreference);

        mController = new DeleteSimProfilePreferenceController(mContext, PREF_KEY);
    }

    @After
    public void tearDown() {
        SubscriptionUtil.setAvailableSubscriptionsForTesting(null);
    }

    @Test
    public void getAvailabilityStatus_noSubs_notAvailable() {
        SubscriptionUtil.setAvailableSubscriptionsForTesting(new ArrayList<>());
        mController.init(SUB_ID, mFragment, REQUEST_CODE);
        assertThat(mController.isAvailable()).isFalse();
    }

    @Test
    public void getAvailabilityStatus_physicalSim_notAvailable() {
        when(mSubscriptionInfo.isEmbedded()).thenReturn(false);
        mController.init(SUB_ID, mFragment, REQUEST_CODE);
        assertThat(mController.isAvailable()).isFalse();
    }

    @Test
    public void getAvailabilityStatus_unknownSim_notAvailable() {
        when(mSubscriptionInfo.getSubscriptionId()).thenReturn(OTHER_ID);
        mController.init(SUB_ID, mFragment, REQUEST_CODE);
        assertThat(mController.isAvailable()).isFalse();
    }

    @Test
    public void getAvailabilityStatus_knownEsim_isAvailable() {
        mController.init(SUB_ID, mFragment, REQUEST_CODE);
        assertThat(mController.isAvailable()).isTrue();
    }

    @Test
    public void onPreferenceClick_startsIntent() {
        mController.init(SUB_ID, mFragment, REQUEST_CODE);
        mController.displayPreference(mScreen);
        // turn off confirmation before click
        Settings.Global.putInt(mContext.getContentResolver(),
                ConfirmSimDeletionPreferenceController.KEY_CONFIRM_SIM_DELETION, 0);

        mController.handlePreferenceTreeClick(mPreference);

        final ArgumentCaptor<Intent> intentCaptor = ArgumentCaptor.forClass(Intent.class);
        verify(mFragment).startActivityForResult(intentCaptor.capture(), eq(REQUEST_CODE));
        final Intent intent = intentCaptor.getValue();
        assertThat(intent.getAction()).isEqualTo(
                EuiccManager.ACTION_DELETE_SUBSCRIPTION_PRIVILEGED);
        assertThat(intent.getIntExtra(EuiccManager.EXTRA_SUBSCRIPTION_ID,
                SubscriptionManager.INVALID_SUBSCRIPTION_ID)).isEqualTo(SUB_ID);
    }
}
