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
package com.android.settings.bluetooth;

import static com.google.common.truth.Truth.assertThat;

import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.graphics.drawable.Drawable;

import com.android.internal.logging.nano.MetricsProto.MetricsEvent;
import com.android.settings.R;
import com.android.settings.testutils.SettingsRobolectricTestRunner;
import com.android.settings.TestConfig;
import com.android.settings.core.instrumentation.MetricsFeatureProvider;
import com.android.settings.testutils.FakeFeatureFactory;
import com.android.settings.testutils.shadow.SettingsShadowResources;
import com.android.settingslib.bluetooth.LocalBluetoothManager;
import com.android.settingslib.graph.BluetoothDeviceLayerDrawable;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(SettingsRobolectricTestRunner.class)
@Config(manifest = TestConfig.MANIFEST_PATH, sdk = TestConfig.SDK_VERSION,
        shadows = SettingsShadowResources.class)
public class UtilsTest {

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private Context mContext;
    @Mock
    private LocalBluetoothManager mLocalBluetoothManager;

    private FakeFeatureFactory mFakeFeatureFactory;
    private MetricsFeatureProvider mMetricsFeatureProvider;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        FakeFeatureFactory.setupForTest(mContext);
        mFakeFeatureFactory = (FakeFeatureFactory) FakeFeatureFactory.getFactory(mContext);
        mMetricsFeatureProvider = mFakeFeatureFactory.getMetricsFeatureProvider();
    }

    @Test
    public void testShowConnectingError_shouldLogBluetoothConnectError() {
        when(mContext.getString(anyInt(), anyString())).thenReturn("testMessage");
        Utils.showConnectingError(mContext, "testName", mock(LocalBluetoothManager.class));

        verify(mMetricsFeatureProvider).visible(eq(mContext), anyInt(),
                eq(MetricsEvent.ACTION_SETTINGS_BLUETOOTH_CONNECT_ERROR));
    }

    @Test
    public void testGetBluetoothDrawable_noBatteryLevel_returnSimpleDrawable() {
        final Drawable drawable = Utils.getBluetoothDrawable(RuntimeEnvironment.application,
                R.drawable.ic_bt_laptop, BluetoothDevice.BATTERY_LEVEL_UNKNOWN, 1 /* iconScale */);

        assertThat(drawable).isNotInstanceOf(BluetoothDeviceLayerDrawable.class);
    }

    @Test
    public void testGetBluetoothDrawable_hasBatteryLevel_returnLayerDrawable() {
        final Drawable drawable = Utils.getBluetoothDrawable(RuntimeEnvironment.application,
                R.drawable.ic_bt_laptop, 10 /* batteryLevel */, 1 /* iconScale */);

        assertThat(drawable).isInstanceOf(BluetoothDeviceLayerDrawable.class);
    }
}
