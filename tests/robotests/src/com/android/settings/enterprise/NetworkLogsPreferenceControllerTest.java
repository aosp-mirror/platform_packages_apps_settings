/*
 * Copyright (C) 2016 The Android Open Source Project
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

package com.android.settings.enterprise;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Mockito.when;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Date;
import org.robolectric.RobolectricTestRunner;

@RunWith(RobolectricTestRunner.class)
public class NetworkLogsPreferenceControllerTest extends AdminActionPreferenceControllerTestBase {

    @Override
    public void setUp() {
        super.setUp();
        mController = new NetworkLogsPreferenceController(mContext);
    }

    @Override
    public void setDate(Date date) {
        when(mFeatureFactory.enterprisePrivacyFeatureProvider.getLastNetworkLogRetrievalTime())
                .thenReturn(date);
    }

    @Test
    public void testIsAvailable() {
        setDate(null);
        when(mFeatureFactory.enterprisePrivacyFeatureProvider.isNetworkLoggingEnabled())
                .thenReturn(false);
        assertThat(mController.isAvailable()).isFalse();

        setDate(new Date());
        assertThat(mController.isAvailable()).isTrue();

        setDate(null);
        when(mFeatureFactory.enterprisePrivacyFeatureProvider.isNetworkLoggingEnabled())
                .thenReturn(true);
        assertThat(mController.isAvailable()).isTrue();
    }

    @Override
    public String getPreferenceKey() {
        return "network_logs";
    }
}
