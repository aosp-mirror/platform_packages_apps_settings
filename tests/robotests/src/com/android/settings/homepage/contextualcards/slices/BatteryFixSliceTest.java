/*
 * Copyright (C) 2018 The Android Open Source Project
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

package com.android.settings.homepage.contextualcards.slices;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import android.content.Context;
import android.net.Uri;

import androidx.slice.Slice;
import androidx.slice.SliceMetadata;
import androidx.slice.SliceProvider;
import androidx.slice.widget.SliceLiveData;

import com.android.internal.os.BatteryStatsHelper;
import com.android.settings.R;
import com.android.settings.fuelgauge.BatteryStatsHelperLoader;
import com.android.settings.fuelgauge.batterytip.AppInfo;
import com.android.settings.fuelgauge.batterytip.BatteryTipLoader;
import com.android.settings.fuelgauge.batterytip.tips.BatteryTip;
import com.android.settings.fuelgauge.batterytip.tips.EarlyWarningTip;
import com.android.settings.fuelgauge.batterytip.tips.HighUsageTip;
import com.android.settings.fuelgauge.batterytip.tips.LowBatteryTip;
import com.android.settings.slices.SliceBackgroundWorker;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.Implementation;
import org.robolectric.annotation.Implements;
import org.robolectric.annotation.Resetter;

import java.util.ArrayList;
import java.util.List;

@RunWith(RobolectricTestRunner.class)
@Config(shadows = {
        BatteryFixSliceTest.ShadowBatteryStatsHelperLoader.class,
        BatteryFixSliceTest.ShadowBatteryTipLoader.class
})
public class BatteryFixSliceTest {

    private Context mContext;
    private BatteryFixSlice mSlice;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mContext = RuntimeEnvironment.application;
        mSlice = new BatteryFixSlice(mContext);

        // Set-up specs for SliceMetadata.
        SliceProvider.setSpecs(SliceLiveData.SUPPORTED_SPECS);
    }

    @After
    public void tearDown() {
        ShadowBatteryTipLoader.reset();
        ShadowSliceBackgroundWorker.reset();
        ShadowEarlyWarningTip.reset();
    }

    @Test
    public void refreshBatteryTips_hasImportantTip_shouldReturnTrue() {
        final List<BatteryTip> tips = new ArrayList<>();
        tips.add(new LowBatteryTip(BatteryTip.StateType.INVISIBLE, false, ""));
        tips.add(new EarlyWarningTip(BatteryTip.StateType.NEW, false));
        ShadowBatteryTipLoader.setBatteryTips(tips);

        BatteryFixSlice.refreshBatteryTips(mContext);

        assertThat(BatteryFixSlice.isBatteryTipAvailableFromCache(mContext)).isTrue();
    }

    @Test
    public void getSlice_unimportantSlice_shouldSkip() {
        final List<BatteryTip> tips = new ArrayList<>();
        final List<AppInfo> appList = new ArrayList<>();
        appList.add(new AppInfo.Builder()
                .setPackageName("com.android.settings")
                .setScreenOnTimeMs(10000L)
                .build());
        tips.add(new LowBatteryTip(BatteryTip.StateType.INVISIBLE, false, ""));
        tips.add(new EarlyWarningTip(BatteryTip.StateType.HANDLED, false));
        tips.add(new HighUsageTip(1000L, appList));
        ShadowBatteryTipLoader.setBatteryTips(tips);

        BatteryFixSlice.refreshBatteryTips(mContext);
        final Slice slice = mSlice.getSlice();

        assertThat(SliceMetadata.from(mContext, slice).isErrorSlice()).isTrue();
    }

    @Test
    @Config(shadows = {
            BatteryFixSliceTest.ShadowEarlyWarningTip.class,
            BatteryFixSliceTest.ShadowSliceBackgroundWorker.class
    })
    public void getSlice_hasImportantTip_shouldTintIcon() {
        final List<BatteryTip> tips = new ArrayList<>();
        tips.add(new EarlyWarningTip(BatteryTip.StateType.NEW, false));
        // Create fake cache data
        ShadowBatteryTipLoader.setBatteryTips(tips);
        BatteryFixSlice.refreshBatteryTips(mContext);
        // Create fake background worker data
        BatteryFixSlice.BatteryTipWorker batteryTipWorker = mock(
                BatteryFixSlice.BatteryTipWorker.class);
        when(batteryTipWorker.getResults()).thenReturn(tips);
        ShadowSliceBackgroundWorker.setBatteryTipWorkerWorker(batteryTipWorker);

        final Slice slice = mSlice.getSlice();

        assertThat(ShadowEarlyWarningTip.isIconTintColorIdCalled()).isTrue();
    }

    @Implements(BatteryStatsHelperLoader.class)
    public static class ShadowBatteryStatsHelperLoader {

        @Implementation
        protected BatteryStatsHelper loadInBackground() {
            return null;
        }
    }

    @Implements(BatteryTipLoader.class)
    public static class ShadowBatteryTipLoader {

        private static List<BatteryTip> sBatteryTips = new ArrayList<>();

        @Resetter
        public static void reset() {
            sBatteryTips = new ArrayList<>();
        }

        @Implementation
        protected List<BatteryTip> loadInBackground() {
            return sBatteryTips;
        }

        private static void setBatteryTips(List<BatteryTip> tips) {
            sBatteryTips = tips;
        }
    }

    @Implements(SliceBackgroundWorker.class)
    public static class ShadowSliceBackgroundWorker {

        private static BatteryFixSlice.BatteryTipWorker sBatteryTipWorkerWorker;

        @Resetter
        public static void reset() {
            sBatteryTipWorkerWorker = null;
        }

        @Implementation
        protected static <T extends SliceBackgroundWorker> T getInstance(Uri uri) {
            return (T) sBatteryTipWorkerWorker;
        }

        public static void setBatteryTipWorkerWorker(BatteryFixSlice.BatteryTipWorker worker) {
            sBatteryTipWorkerWorker = worker;
        }
    }

    @Implements(EarlyWarningTip.class)
    public static class ShadowEarlyWarningTip {

        private static boolean mIsGetIconTintColorIdCalled;

        @Resetter
        public static void reset() {
            mIsGetIconTintColorIdCalled = false;
        }

        @Implementation
        protected int getIconTintColorId() {
            mIsGetIconTintColorIdCalled = true;
            return R.color.battery_bad_color_light;
        }

        public static boolean isIconTintColorIdCalled() {
            return mIsGetIconTintColorIdCalled;
        }
    }
}
