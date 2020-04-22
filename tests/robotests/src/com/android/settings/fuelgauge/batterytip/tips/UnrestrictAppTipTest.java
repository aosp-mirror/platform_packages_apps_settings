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
package com.android.settings.fuelgauge.batterytip.tips;

import static com.google.common.truth.Truth.assertThat;

import android.os.Parcel;

import com.android.settings.fuelgauge.batterytip.AppInfo;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;

@RunWith(RobolectricTestRunner.class)
public class UnrestrictAppTipTest {

    private static final String PACKAGE_NAME = "com.android.app";

    private UnrestrictAppTip mBatteryTip;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);

        AppInfo appInfo = new AppInfo.Builder().setPackageName(PACKAGE_NAME).build();
        mBatteryTip = new UnrestrictAppTip(BatteryTip.StateType.NEW, appInfo);
    }

    @Test
    public void testParcelable() {
        Parcel parcel = Parcel.obtain();
        mBatteryTip.writeToParcel(parcel, mBatteryTip.describeContents());
        parcel.setDataPosition(0);

        final UnrestrictAppTip parcelTip = new UnrestrictAppTip(parcel);

        assertThat(parcelTip.getType()).isEqualTo(BatteryTip.TipType.REMOVE_APP_RESTRICTION);
        assertThat(parcelTip.getState()).isEqualTo(BatteryTip.StateType.NEW);
        assertThat(parcelTip.getPackageName()).isEqualTo(PACKAGE_NAME);
    }
}
