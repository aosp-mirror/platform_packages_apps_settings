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

import android.content.Context;
import android.os.PersistableBundle;
import android.telephony.CarrierConfigManager;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.Shadows;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowCarrierConfigManager;

@RunWith(RobolectricTestRunner.class)
@Config(shadows = ShadowCarrierConfigManager.class)
public class CarrierSettingsVersionPreferenceControllerTest {

    private ShadowCarrierConfigManager mCarrierConfigManager;
    private CarrierSettingsVersionPreferenceController mController;
    private int mSubscriptionId = 1234;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        final Context context = RuntimeEnvironment.application;
        mController = new CarrierSettingsVersionPreferenceController(context, "dummy_key");
        mController.init(mSubscriptionId);
        mCarrierConfigManager = Shadows.shadowOf(
                context.getSystemService(CarrierConfigManager.class));
    }

    @Test
    public void getSummary_nullConfig_noCrash() {
        mCarrierConfigManager.setConfigForSubId(mSubscriptionId, null);
        assertThat(mController.getSummary()).isNull();
    }

    @Test
    public void getSummary_nullVersionString_noCrash() {
        mCarrierConfigManager.setConfigForSubId(mSubscriptionId, new PersistableBundle());
        assertThat(mController.getSummary()).isNull();
    }

    @Test
    public void getSummary_hasVersionString_correctSummary() {
        final PersistableBundle bundle = new PersistableBundle();
        bundle.putString(CarrierConfigManager.KEY_CARRIER_CONFIG_VERSION_STRING,
                "test_version_123");
        mCarrierConfigManager.setConfigForSubId(mSubscriptionId, bundle);
        assertThat(mController.getSummary()).isEqualTo("test_version_123");
    }
}
