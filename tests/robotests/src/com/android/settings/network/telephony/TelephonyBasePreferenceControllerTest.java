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

import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

import android.content.Context;
import android.telephony.SubscriptionInfo;
import android.telephony.SubscriptionManager;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

import java.util.Arrays;

@RunWith(RobolectricTestRunner.class)
public class TelephonyBasePreferenceControllerTest {
    private static final int VALID_SUB_ID = 1;

    @Mock
    private SubscriptionManager mSubscriptionManager;
    @Mock
    private SubscriptionInfo mSubscriptionInfo;

    private TestPreferenceController mPreferenceController;
    private Context mContext;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);

        mContext = spy(RuntimeEnvironment.application);
        when(mContext.getSystemService(SubscriptionManager.class))
                .thenReturn(mSubscriptionManager);
        when(mSubscriptionInfo.getSubscriptionId()).thenReturn(VALID_SUB_ID);
        mPreferenceController = new TestPreferenceController(mContext, "prefKey");
    }

    @Test
    public void isAvailable_validSubIdSet_returnTrue() {
        mPreferenceController.init(VALID_SUB_ID);

        assertThat(mPreferenceController.isAvailable()).isTrue();
    }

    @Test
    public void isAvailable_noIdSetHoweverHasDefaultOne_returnTrue() {
        when(mSubscriptionManager.getActiveSubscriptionInfoList()).thenReturn(
                Arrays.asList(mSubscriptionInfo));

        assertThat(mPreferenceController.isAvailable()).isTrue();
    }

    @Test
    public void isAvailable_noDefaultAndNoSet_returnFalse() {
        assertThat(mPreferenceController.isAvailable()).isFalse();
    }

    /**
     * Test preference controller for {@link TelephonyBasePreferenceController}
     */
    public class TestPreferenceController extends TelephonyBasePreferenceController {
        public TestPreferenceController(Context context, String prefKey) {
            super(context, prefKey);
        }

        public void init(int subId) {
            mSubId = subId;
        }

        @Override
        public int getAvailabilityStatus(int subId) {
            return subId == VALID_SUB_ID ? AVAILABLE : UNSUPPORTED_ON_DEVICE;
        }
    }
}
