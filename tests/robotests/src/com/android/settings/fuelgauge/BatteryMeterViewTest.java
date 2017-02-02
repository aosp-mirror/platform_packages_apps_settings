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
package com.android.settings.fuelgauge;

import android.content.Context;
import com.android.settings.SettingsRobolectricTestRunner;
import com.android.settings.TestConfig;
import com.android.settings.testutils.shadow.SettingsShadowResources;
import com.android.settings.testutils.shadow.SettingsShadowResources.SettingsShadowTheme;
import com.android.settings.testutils.shadow.ShadowDynamicIndexableContentMonitor;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

import static org.mockito.Mockito.verify;

@RunWith(SettingsRobolectricTestRunner.class)
// TODO: Consider making the shadow class set global using a robolectric.properties file.
@Config(manifest = TestConfig.MANIFEST_PATH,
        sdk = TestConfig.SDK_VERSION,
        shadows = {
                SettingsShadowResources.class,
                SettingsShadowTheme.class,
                ShadowDynamicIndexableContentMonitor.class
        })
public class BatteryMeterViewTest {
    private static final int BATTERY_LEVEL = 100;
    @Mock
    private BatteryMeterView.BatteryMeterDrawable mDrawable;
    private Context mContext;
    private BatteryMeterView mBatteryMeterView;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);

        mContext = RuntimeEnvironment.application;
        mBatteryMeterView = new BatteryMeterView(mContext);
        mBatteryMeterView.setBatteryDrawable(mDrawable);
    }

    @Test
    public void testSetBatteryInfo_SetCorrectly() {
        mBatteryMeterView.setBatteryInfo(BATTERY_LEVEL);

        verify(mDrawable).setBatteryLevel(BATTERY_LEVEL);
    }
}
