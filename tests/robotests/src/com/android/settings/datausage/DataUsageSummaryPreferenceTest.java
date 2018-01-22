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
 * limitations under the License
 */

package com.android.settings.datausage;

import android.content.Context;
import android.content.Intent;
import android.support.v7.preference.PreferenceViewHolder;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.android.settings.R;
import com.android.settings.TestConfig;
import com.android.settings.testutils.SettingsRobolectricTestRunner;
import com.android.settings.testutils.shadow.SettingsShadowResourcesImpl;
import com.android.settingslib.utils.StringUtil;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.MockitoAnnotations;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

import static com.google.common.truth.Truth.assertThat;
import static org.mockito.Mockito.spy;

@RunWith(SettingsRobolectricTestRunner.class)
@Config(manifest = TestConfig.MANIFEST_PATH, sdk = TestConfig.SDK_VERSION, shadows =
        SettingsShadowResourcesImpl.class)
public class DataUsageSummaryPreferenceTest {
    private static final long CYCLE_DURATION_MILLIS = 1000000000L;
    private static final long UPDATE_LAG_MILLIS = 10000000L;
    private static final String DUMMY_CARRIER = "z-mobile";

    private Context mContext;
    private PreferenceViewHolder mHolder;
    private DataUsageSummaryPreference mSummaryPreference;
    private TextView mUsageTitle;
    private TextView mCycleTime;
    private TextView mCarrierInfo;
    private TextView mDataLimits;
    private Button mLaunchButton;

    private long mCycleEnd;
    private long mUpdateTime;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mContext = spy(RuntimeEnvironment.application);
        mSummaryPreference = new DataUsageSummaryPreference(mContext, null /* attrs */);
        LayoutInflater inflater = LayoutInflater.from(mContext);
        View view = inflater.inflate(mSummaryPreference.getLayoutResource(), null /* root */,
                false /* attachToRoot */);

        mHolder = PreferenceViewHolder.createInstanceForTests(view);

        final long now = System.currentTimeMillis();
        mCycleEnd = now + CYCLE_DURATION_MILLIS;
        mUpdateTime = now - UPDATE_LAG_MILLIS;
    }

    @Test
    public void testSetUsageInfo_withLaunchIntent_launchButtonShown() {
        mSummaryPreference.setUsageInfo(mCycleEnd, mUpdateTime, DUMMY_CARRIER, 0 /* numPlans */,
                new Intent());

        bindViewHolder();
        assertThat(mLaunchButton.getVisibility()).isEqualTo(View.VISIBLE);
    }

    @Test
    public void testSetUsageInfo_withoutLaunchIntent_launchButtonNotShown() {
        mSummaryPreference.setUsageInfo(mCycleEnd, mUpdateTime, DUMMY_CARRIER, 0 /* numPlans */,
                null /* launchIntent */);

        bindViewHolder();
        assertThat(mLaunchButton.getVisibility()).isEqualTo(View.GONE);
    }

    @Test
    public void testSetUsageInfo_withDataPlans_carrierInfoShown() {
        mSummaryPreference.setUsageInfo(mCycleEnd, mUpdateTime, DUMMY_CARRIER, 1 /* numPlans */,
                new Intent());

        bindViewHolder();
        assertThat(mCarrierInfo.getVisibility()).isEqualTo(View.VISIBLE);
    }

    @Test
    public void testSetUsageInfo_withNoDataPlans_carrierInfoNotShown() {
        mSummaryPreference.setUsageInfo(mCycleEnd, mUpdateTime, DUMMY_CARRIER, 0 /* numPlans */,
                new Intent());

        bindViewHolder();
        assertThat(mCarrierInfo.getVisibility()).isEqualTo(View.GONE);
    }

    @Test
    public void testSetUsageInfo_withNoDataPlans_usageTitleNotShown() {
        mSummaryPreference.setUsageInfo(mCycleEnd, mUpdateTime, DUMMY_CARRIER, 0 /* numPlans */,
                new Intent());

        bindViewHolder();
        assertThat(mUsageTitle.getVisibility()).isEqualTo(View.GONE);
    }

    @Test
    public void testSetUsageInfo_withMultipleDataPlans_usageTitleShown() {
        mSummaryPreference.setUsageInfo(mCycleEnd, mUpdateTime, DUMMY_CARRIER, 2 /* numPlans */,
                new Intent());

        bindViewHolder();
        assertThat(mUsageTitle.getVisibility()).isEqualTo(View.VISIBLE);
    }

    @Test
    public void testSetUsageInfo_cycleRemainingTimeShown() {
        mSummaryPreference.setUsageInfo(mCycleEnd, mUpdateTime, DUMMY_CARRIER, 0 /* numPlans */,
                new Intent());
        String cyclePrefix = StringUtil.formatElapsedTime(mContext, CYCLE_DURATION_MILLIS,
                false /* withSeconds */).toString();
        String text = mContext.getString(R.string.cycle_left_time_text, cyclePrefix);

        bindViewHolder();
        assertThat(mCycleTime.getVisibility()).isEqualTo(View.VISIBLE);
        assertThat(mCycleTime.getText()).isEqualTo(text);
    }

    @Test
    public void testSetLimitInfo_withLimitInfo_dataLimitsShown() {
        final String limitText = "test limit text";
        mSummaryPreference.setLimitInfo(limitText);

        bindViewHolder();
        assertThat(mDataLimits.getVisibility()).isEqualTo(View.VISIBLE);
        assertThat(mDataLimits.getText()).isEqualTo(limitText);
    }

    @Test
    public void testSetLimitInfo_withNullLimitInfo_dataLimitsNotShown() {
        mSummaryPreference.setLimitInfo(null);

        bindViewHolder();
        assertThat(mDataLimits.getVisibility()).isEqualTo(View.GONE);
    }

    private void bindViewHolder() {
        mSummaryPreference.onBindViewHolder(mHolder);
        mUsageTitle = (TextView) mHolder.findViewById(R.id.usage_title);
        mCycleTime = (TextView) mHolder.findViewById(R.id.cycle_left_time);
        mCarrierInfo = (TextView) mHolder.findViewById(R.id.carrier_and_update);
        mDataLimits = (TextView) mHolder.findViewById(R.id.data_limits);
        mLaunchButton = (Button) mHolder.findViewById(R.id.launch_mdp_app_button);
    }
}