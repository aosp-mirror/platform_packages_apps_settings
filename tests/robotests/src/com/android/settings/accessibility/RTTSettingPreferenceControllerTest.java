/*
 * Copyright (C) 2020 The Android Open Source Project
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

package com.android.settings.accessibility;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;
import static org.robolectric.Shadows.shadowOf;

import android.content.Context;
import android.content.Intent;
import android.content.pm.ResolveInfo;

import com.android.settings.core.BasePreferenceController;
import com.android.settings.testutils.ResolveInfoBuilder;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.shadows.ShadowPackageManager;

@RunWith(RobolectricTestRunner.class)
public class RTTSettingPreferenceControllerTest {

    private Context mContext;
    private ShadowPackageManager mShadowPackageManager;
    private RTTSettingPreferenceController mController;

    @Before
    public void setUp() {
        mContext = RuntimeEnvironment.application;
        mShadowPackageManager = shadowOf(mContext.getPackageManager());
        mController = spy(new RTTSettingPreferenceController(mContext, "rtt_setting"));
        mController.mRTTIntent = new Intent("com.android.test.action.example");
    }

    @Test
    public void getAvailabilityStatus_defaultDialerIsExpected_intentCanBeHandled_returnAVAILABLE() {
        // Default dialer is expected.
        doReturn(true).when(mController).isDialerSupportRTTSetting();
        // Intent can be handled.
        final ResolveInfo info = new ResolveInfoBuilder("pkg")
                .setActivity("pkg", "class").build();
        final Intent intent = new Intent("com.android.test.action.example");
        mShadowPackageManager.addResolveInfoForIntent(intent, info);

        assertThat(mController.getAvailabilityStatus())
                .isEqualTo(BasePreferenceController.AVAILABLE);
    }

    @Test
    public void getAvailabilityStatus_intentCanNotBeHandled_shouldReturnUNSUPPORTED_ON_DEVICE() {
        // Default dialer is expected.
        doReturn(true).when(mController).isDialerSupportRTTSetting();
        // Intent can not be handled.

        assertThat(mController.getAvailabilityStatus())
                .isEqualTo(BasePreferenceController.UNSUPPORTED_ON_DEVICE);
    }

    @Test
    public void getAvailabilityStatus_defaultDialerIsNotExpected_returnUNSUPPORTED_ON_DEVICE() {
        // Default dialer is not expected.
        doReturn(false).when(mController).isDialerSupportRTTSetting();
        // Intent can be handled.
        final ResolveInfo info = new ResolveInfoBuilder("pkg")
                .setActivity("pkg", "class").build();
        final Intent intent = new Intent("com.android.test.action.example");
        mShadowPackageManager.addResolveInfoForIntent(intent, info);

        assertThat(mController.getAvailabilityStatus())
                .isEqualTo(BasePreferenceController.UNSUPPORTED_ON_DEVICE);
    }
}
