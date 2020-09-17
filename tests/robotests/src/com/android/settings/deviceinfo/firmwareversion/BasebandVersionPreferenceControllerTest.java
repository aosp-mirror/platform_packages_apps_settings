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

package com.android.settings.deviceinfo.firmwareversion;

import static com.android.settings.core.BasePreferenceController.AVAILABLE;
import static com.android.settings.core.BasePreferenceController.UNSUPPORTED_ON_DEVICE;

import static com.google.common.truth.Truth.assertThat;

import static org.robolectric.shadow.api.Shadow.extract;

import android.content.Context;
import android.net.ConnectivityManager;
import android.sysprop.TelephonyProperties;

import com.android.settings.testutils.shadow.ShadowConnectivityManager;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

import java.util.Arrays;


@RunWith(RobolectricTestRunner.class)
@Config(shadows = ShadowConnectivityManager.class)
public class BasebandVersionPreferenceControllerTest {

    private Context mContext;
    private BasebandVersionPreferenceController mController;

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);
        mContext = RuntimeEnvironment.application;
        mController = new BasebandVersionPreferenceController(mContext, "key");
    }

    @Test
    public void getAvailability_wifiOnly_unavailable() {
        final ShadowConnectivityManager connectivityManager =
                extract(mContext.getSystemService(ConnectivityManager.class));
        connectivityManager.setNetworkSupported(ConnectivityManager.TYPE_MOBILE, false);

        assertThat(mController.getAvailabilityStatus()).isEqualTo(UNSUPPORTED_ON_DEVICE);
    }

    @Test
    public void getAvailability_hasMobile_available() {
        final String text = "test";
        TelephonyProperties.baseband_version(Arrays.asList(new String[]{text}));
        ShadowConnectivityManager connectivityManager =
                extract(mContext.getSystemService(ConnectivityManager.class));
        connectivityManager.setNetworkSupported(ConnectivityManager.TYPE_MOBILE, true);

        assertThat(mController.getAvailabilityStatus()).isEqualTo(AVAILABLE);
    }
}
