/*
 * Copyright (C) 2022 The Android Open Source Project
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

package com.android.settings.development.snooplogger;

import android.content.Context;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

@RunWith(RobolectricTestRunner.class)
public class SnoopLoggerFiltersPreferenceTest {

    private static String sKEY;
    private static String sENTRY;

    private Context mContext;
    private SnoopLoggerFiltersPreference mPreference;

    @Before
    public void setUp() {
        mContext = RuntimeEnvironment.application;
        sKEY = mContext.getResources().getStringArray(
                com.android.settingslib.R.array.bt_hci_snoop_log_filters_values)[0];
        sENTRY =
                mContext.getResources().getStringArray(
                        com.android.settingslib.R.array.bt_hci_snoop_log_filters_entries)[0];
        mPreference = new SnoopLoggerFiltersPreference(mContext, sKEY, sENTRY);
    }

    @Test
    public void constructor_shouldSetTitle() {
        Assert.assertEquals(mPreference.getTitle(), sENTRY);
        Assert.assertFalse(mPreference.isChecked());
    }

    @Test
    public void setChecked_shouldSetChecked() {
        mPreference.setChecked(true);
        Assert.assertTrue(mPreference.isChecked());
    }
}
