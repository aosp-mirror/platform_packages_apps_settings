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
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.content.ContentResolver;
import android.content.Context;
import android.os.PowerManager;
import android.support.v7.preference.Preference;

import com.android.settings.R;
import com.android.settings.testutils.SettingsRobolectricTestRunner;
import com.android.settingslib.core.lifecycle.Lifecycle;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.util.ReflectionHelpers;

@RunWith(SettingsRobolectricTestRunner.class)
public class BatterySaverControllerTest {

    @Mock
    private Preference mBatterySaverPref;
    @Mock
    private PowerManager mPowerManager;
    @Mock
    private Context mContext;
    @Mock
    private ContentResolver mContentResolver;

    private BatterySaverController mBatterySaverController;

    private static final String SAVER_ON_SUMMARY = "saver-on";
    private static final String SAVER_OFF_SUMMARY = "saver-off";

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);

        mBatterySaverController = spy(new BatterySaverController(mContext));
        ReflectionHelpers.setField(mBatterySaverController, "mPowerManager", mPowerManager);
        ReflectionHelpers.setField(mBatterySaverController, "mBatterySaverPref", mBatterySaverPref);
        doNothing().when(mBatterySaverController).refreshConditionManager();

        when(mContext.getContentResolver()).thenReturn(mContentResolver);

        when(mContext.getString(anyInt(), any(Object.class)))
                .thenAnswer((inv) -> "str-" + inv.getArgument(0));

        when(mContext.getString(eq(R.string.battery_saver_on_summary), any(Object.class)))
                .thenReturn(SAVER_ON_SUMMARY);
        when(mContext.getString(eq(R.string.battery_saver_off_summary), any(Object.class)))
                .thenReturn(SAVER_OFF_SUMMARY);
    }

    @Test
    public void testOnPreferenceChange_onStart() {
        mBatterySaverController.onStart();
        verify(mBatterySaverPref).setSummary(eq(SAVER_OFF_SUMMARY));
    }

    @Test
    public void testOnPreferenceChange_onPowerSaveModeChanged() {
        mBatterySaverController.onPowerSaveModeChanged();
        verify(mBatterySaverPref).setSummary(eq(SAVER_OFF_SUMMARY));
    }
}
