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

package com.android.settings.network.telephony;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;

import android.content.Context;
import android.os.PersistableBundle;
import android.telephony.CarrierConfigManager;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.android.settings.network.CarrierConfigCache;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

@RunWith(AndroidJUnit4.class)
public class CarrierSettingsVersionPreferenceControllerTest {
    @Mock
    private CarrierConfigCache mCarrierConfigCache;

    private CarrierSettingsVersionPreferenceController mController;
    private int mSubscriptionId = 1234;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        Context context = spy(ApplicationProvider.getApplicationContext());
        CarrierConfigCache.setTestInstance(context, mCarrierConfigCache);
        mController = new CarrierSettingsVersionPreferenceController(context, "mock_key");
        mController.init(mSubscriptionId);
    }

    @Test
    public void getSummary_nullConfig_noCrash() {
        doReturn(null).when(mCarrierConfigCache).getConfigForSubId(mSubscriptionId);

        assertThat(mController.getSummary()).isNull();
    }

    @Test
    public void getSummary_nullVersionString_noCrash() {
        doReturn(new PersistableBundle()).when(mCarrierConfigCache)
                .getConfigForSubId(mSubscriptionId);
        assertThat(mController.getSummary()).isNull();
    }

    @Test
    public void getSummary_hasVersionString_correctSummary() {
        final PersistableBundle bundle = new PersistableBundle();
        bundle.putString(CarrierConfigManager.KEY_CARRIER_CONFIG_VERSION_STRING,
                "test_version_123");
        doReturn(bundle).when(mCarrierConfigCache).getConfigForSubId(mSubscriptionId);

        assertThat(mController.getSummary()).isEqualTo("test_version_123");
    }
}
