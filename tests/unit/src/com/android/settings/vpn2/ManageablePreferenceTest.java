/*
 * Copyright (C) 2021 The Android Open Source Project
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

package com.android.settings.vpn2;

import static com.google.common.truth.Truth.assertThat;

import android.content.Context;
import android.util.AttributeSet;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.android.settings.testutils.ResourcesUtils;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

/** Unittest for ManageablePreference */
@RunWith(AndroidJUnit4.class)
public class ManageablePreferenceTest {
    private static final int STATE_DISCONNECTED = 0;

    private Context mContext = ApplicationProvider.getApplicationContext();
    private TestManageablePreference mManageablePreference;

    public class TestManageablePreference extends ManageablePreference {
        public TestManageablePreference(Context context, AttributeSet attrs) {
            super(context, attrs);
        }
    }

    @Before
    public void setUp() {
        mManageablePreference = new TestManageablePreference(mContext, null);
    }

    @Test
    public void setAlwaysOnVpn_summaryUpdatedCorrectly() {
        mManageablePreference.setAlwaysOn(true);
        final String alwaysOnString = ResourcesUtils.getResourcesString(
                mContext, "vpn_always_on_summary_active");
        // Setting always-on should automatically call UpdateSummary
        assertThat(mManageablePreference.getSummary()).isEqualTo(alwaysOnString);
    }

    @Test
    public void setInsecure_summaryUpdatedCorrectly() {
        mManageablePreference.setInsecureVpn(true);
        final String insecureString = ResourcesUtils.getResourcesString(
                mContext, "vpn_insecure_summary");
        // Setting isInsecure should automatically call UpdateSummary
        assertThat(mManageablePreference.getSummary().toString()).isEqualTo(insecureString);
    }

    @Test
    public void setState_accuratelySet() {
        mManageablePreference.setState(STATE_DISCONNECTED);
        assertThat(mManageablePreference.getState()).isEqualTo(STATE_DISCONNECTED);
    }

    @Test
    public void setState_summaryUpdatedCorrectly() {
        mManageablePreference.setState(STATE_DISCONNECTED);
        // Setting the state should automatically call UpdateSummary
        // Fetch the appropriate string from the array resource
        int id = ResourcesUtils.getResourcesId(mContext, "array", "vpn_states");
        String[] states = mContext.getResources().getStringArray(id);
        String disconnectedStr = states[STATE_DISCONNECTED];
        assertThat(mManageablePreference.getSummary()).isEqualTo(disconnectedStr);
    }
}
