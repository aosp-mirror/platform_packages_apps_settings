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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

import android.content.Context;
import android.os.Bundle;

import androidx.loader.app.LoaderManager;

import com.android.internal.os.BatteryStatsHelper;
import com.android.settings.testutils.shadow.ShadowDashboardFragment;
import com.android.settingslib.core.AbstractPreferenceController;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.util.List;

@RunWith(RobolectricTestRunner.class)
@Config(shadows = ShadowDashboardFragment.class)
public class PowerUsageBaseTest {

    @Mock
    private BatteryStatsHelper mBatteryStatsHelper;
    @Mock
    private LoaderManager mLoaderManager;
    private TestFragment mFragment;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);

        mFragment = spy(new TestFragment());
        mFragment.setBatteryStatsHelper(mBatteryStatsHelper);
        doReturn(mLoaderManager).when(mFragment).getLoaderManager();
    }

    @Test
    public void testOnCreate_batteryStatsLoaderNotInvoked() {
        mFragment.onCreate(null);

        verify(mLoaderManager, never()).initLoader(anyInt(), any(Bundle.class), any());
    }

    public static class TestFragment extends PowerUsageBase {

        @Override
        public int getMetricsCategory() {
            return 0;
        }

        @Override
        protected void refreshUi(int refreshType) {
            // Do nothing
        }

        @Override
        protected String getLogTag() {
            return null;
        }

        @Override
        protected int getPreferenceScreenResId() {
            return 0;
        }

        @Override
        protected List<AbstractPreferenceController> createPreferenceControllers(Context context) {
            return null;
        }

        private void setBatteryStatsHelper(BatteryStatsHelper batteryStatsHelper) {
            mStatsHelper = batteryStatsHelper;
        }
    }
}
