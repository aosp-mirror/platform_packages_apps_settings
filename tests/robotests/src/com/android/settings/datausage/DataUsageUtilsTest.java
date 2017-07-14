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
 * limitations under the License
 */

package com.android.settings.datausage;

import static com.google.common.truth.Truth.assertThat;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Mockito.when;

import android.content.Context;
import android.net.ConnectivityManager;
import com.android.settings.TestConfig;
import com.android.settings.testutils.SettingsRobolectricTestRunner;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowApplication;

@RunWith(SettingsRobolectricTestRunner.class)
@Config(manifest = TestConfig.MANIFEST_PATH, sdk = TestConfig.SDK_VERSION)
public final class DataUsageUtilsTest {
    @Mock private ConnectivityManager mManager;
    private Context mContext;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        ShadowApplication shadowContext = ShadowApplication.getInstance();
        mContext = shadowContext.getApplicationContext();
        shadowContext.setSystemService(Context.CONNECTIVITY_SERVICE, mManager);
    }

    @Test
    public void mobileDataStatus_whenNetworkIsSupported() {
        when(mManager.isNetworkSupported(anyInt())).thenReturn(true);
        boolean hasMobileData = DataUsageUtils.hasMobileData(mContext);
        assertThat(hasMobileData).isTrue();
    }

    @Test
    public void mobileDataStatus_whenNetworkIsNotSupported() {
        when(mManager.isNetworkSupported(anyInt())).thenReturn(false);
        boolean hasMobileData = DataUsageUtils.hasMobileData(mContext);
        assertThat(hasMobileData).isFalse();
    }
}
