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

package com.android.settings.applications.specialaccess.premiumsms;

import static com.android.settings.core.BasePreferenceController.AVAILABLE;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

import android.content.Context;
import android.content.res.Resources;

import com.android.settings.R;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

@RunWith(RobolectricTestRunner.class)
public class PremiumSmsControllerTest {

    private Context mContext;
    private Resources mResources;
    private PremiumSmsController mController;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mContext = spy(RuntimeEnvironment.application.getApplicationContext());

        mResources = spy(mContext.getResources());
        when(mContext.getResources()).thenReturn(mResources);

        mController = new PremiumSmsController(mContext, "key");
    }

    @Test
    public void getAvailability_byDefault_shouldBeShown() {
        when(mResources.getBoolean(R.bool.config_show_premium_sms)).thenReturn(true);
        assertThat(mController.getAvailabilityStatus()).isEqualTo(AVAILABLE);
    }

    @Test
    @Config(qualifiers = "mcc999")
    public void getAvailability_disabledByCarrier_returnUnavailable() {
        assertThat(mController.isAvailable()).isFalse();
    }

    @Test
    public void getAvailability_disabled_returnUnavailable() {
        when(mResources.getBoolean(R.bool.config_show_premium_sms)).thenReturn(false);
        assertThat(mController.isAvailable()).isFalse();
    }
}
