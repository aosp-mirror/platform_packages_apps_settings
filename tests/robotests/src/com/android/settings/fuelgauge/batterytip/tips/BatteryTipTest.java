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
package com.android.settings.fuelgauge.batterytip.tips;

import static com.google.common.truth.Truth.assertThat;

import android.app.Dialog;
import android.content.Context;
import android.support.annotation.IdRes;
import android.support.v7.preference.Preference;

import com.android.settings.R;
import com.android.settings.TestConfig;
import com.android.settings.testutils.SettingsRobolectricTestRunner;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

@RunWith(SettingsRobolectricTestRunner.class)
@Config(manifest = TestConfig.MANIFEST_PATH, sdk = TestConfig.SDK_VERSION)
public class BatteryTipTest {
    private static final String TITLE = "title";
    private static final String SUMMARY = "summary";
    @IdRes
    private static final int ICON_ID = R.drawable.ic_fingerprint;

    private Context mContext;
    private TestBatteryTip mBatteryTip;

    @Before
    public void setUp() {
        mContext = RuntimeEnvironment.application;
        mBatteryTip = new TestBatteryTip();
    }

    @Test
    public void testBuildPreference() {
        final Preference preference = mBatteryTip.buildPreference(mContext);

        assertThat(preference.getTitle()).isEqualTo(TITLE);
        assertThat(preference.getSummary()).isEqualTo(SUMMARY);
        assertThat(preference.getIcon()).isEqualTo(mContext.getDrawable(ICON_ID));
    }

    /**
     * Used to test the non abstract methods in {@link TestBatteryTip}
     */
    public class TestBatteryTip extends BatteryTip {

        @Override
        public String getTitle(Context context) {
            return TITLE;
        }

        @Override
        public String getSummary(Context context) {
            return SUMMARY;
        }

        @Override
        public int getIconId() {
            return ICON_ID;
        }

        @Override
        public void updateState(BatteryTip tip) {
            // do nothing
        }

        @Override
        public void action() {
            // do nothing
        }

        @Override
        public Dialog buildDialog() {
            return null;
        }
    }

}
