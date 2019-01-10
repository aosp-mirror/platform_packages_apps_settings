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

import static android.content.Context.MODE_PRIVATE;

import static com.android.settings.homepage.contextualcards.slices.BatteryFixSlice.KEY_CURRENT_TIPS_TYPE;
import static com.android.settings.homepage.contextualcards.slices.BatteryFixSlice.PREFS;

import static com.google.common.truth.Truth.assertThat;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.slice.Slice;
import androidx.slice.SliceMetadata;
import androidx.slice.SliceProvider;
import androidx.slice.widget.SliceLiveData;

import com.android.internal.os.BatteryStatsHelper;
import com.android.settings.fuelgauge.BatteryStatsHelperLoader;
import com.android.settings.fuelgauge.batterytip.BatteryTipLoader;
import com.android.settings.fuelgauge.batterytip.tips.BatteryTip;
import com.android.settings.fuelgauge.batterytip.tips.EarlyWarningTip;
import com.android.settings.fuelgauge.batterytip.tips.LowBatteryTip;

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
    }

    @Test
    public void readBatteryTipFromPref_readCorrectValue() {
        int target = 111;
        final SharedPreferences.Editor editor = mContext.getSharedPreferences(PREFS,
                MODE_PRIVATE).edit();
        editor.putInt(KEY_CURRENT_TIPS_TYPE, target);

        editor.commit();

        assertThat(BatteryFixSlice.readBatteryTipAvailabilityCache(mContext)).isEqualTo(target);
    }

    @Test
    @Config(shadows = {
            ShadowBatteryStatsHelperLoader.class,
            ShadowBatteryTipLoader.class
    })
    public void updateBatteryTipAvailabilityCache_writeCorrectValue() {
        final List<BatteryTip> tips = new ArrayList<>();
        tips.add(new LowBatteryTip(BatteryTip.StateType.INVISIBLE, false, ""));
        tips.add(new EarlyWarningTip(BatteryTip.StateType.NEW, false));
        ShadowBatteryTipLoader.setBatteryTips(tips);

        BatteryFixSlice.updateBatteryTipAvailabilityCache(mContext);

        assertThat(BatteryFixSlice.readBatteryTipAvailabilityCache(mContext)).isEqualTo(
                BatteryTip.TipType.BATTERY_SAVER);
    }

    @Test
    @Config(shadows = {
            ShadowBatteryStatsHelperLoader.class,
            ShadowBatteryTipLoader.class
    })
    public void getSlice_unimportantSlice_shouldSkip() {
        final List<BatteryTip> tips = new ArrayList<>();
        tips.add(new LowBatteryTip(BatteryTip.StateType.INVISIBLE, false, ""));
        tips.add(new EarlyWarningTip(BatteryTip.StateType.NEW, false));
        ShadowBatteryTipLoader.setBatteryTips(tips);

        BatteryFixSlice.updateBatteryTipAvailabilityCache(mContext);
        final Slice slice = mSlice.getSlice();

        assertThat(SliceMetadata.from(mContext, slice).isErrorSlice()).isTrue();
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
}
