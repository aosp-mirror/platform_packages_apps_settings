/*
 * Copyright (C) 2020 The Android Open Source Project
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

package com.android.settings.datausage;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Typeface;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.MeasureSpec;
import android.widget.TextView;

import androidx.preference.PreferenceViewHolder;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.android.settings.testutils.ResourcesUtils;
import com.android.settingslib.Utils;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.MockitoAnnotations;

import java.util.concurrent.TimeUnit;

@RunWith(AndroidJUnit4.class)
public class DataUsageSummaryPreferenceTest {

    private static final long CYCLE_DURATION_MILLIS = 1000000000L;
    private static final long UPDATE_LAG_MILLIS = 10000000L;
    private static final String FAKE_CARRIER = "z-mobile";

    private Context mContext;
    private Resources mResources;
    private PreferenceViewHolder mHolder;
    private DataUsageSummaryPreference mSummaryPreference;

    private long mCycleEnd;
    private long mUpdateTime;


    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mContext = spy(ApplicationProvider.getApplicationContext());
        mResources = spy(mContext.getResources());
        when(mContext.getResources()).thenReturn(mResources);
        mSummaryPreference = spy(new DataUsageSummaryPreference(mContext, null /* attrs */));
        LayoutInflater inflater = mContext.getSystemService(LayoutInflater.class);
        View view = inflater.inflate(
                mSummaryPreference.getLayoutResource(),
                null /* root */, false /* attachToRoot */);

        mHolder = spy(PreferenceViewHolder.createInstanceForTests(view));
        assertThat(mSummaryPreference.getDataUsed(mHolder)).isNotNull();

        final long now = System.currentTimeMillis();
        mCycleEnd = now + CYCLE_DURATION_MILLIS;
        mUpdateTime = now - UPDATE_LAG_MILLIS;
    }

    @Test
    public void testSetUsageInfo_withDataPlans_carrierInfoShown() {
        mSummaryPreference.setUsageInfo(mCycleEnd, mUpdateTime, FAKE_CARRIER, 1 /* numPlans */);

        mSummaryPreference.onBindViewHolder(mHolder);
        assertThat(mSummaryPreference.getCarrierInfo(mHolder).getVisibility())
                .isEqualTo(View.VISIBLE);
    }

    @Test
    public void testSetUsageInfo_withNoDataPlans_carrierInfoNotShown() {
        mSummaryPreference.setUsageInfo(mCycleEnd, -1, FAKE_CARRIER, 0 /* numPlans */);

        mSummaryPreference.onBindViewHolder(mHolder);
        assertThat(mSummaryPreference.getCarrierInfo(mHolder).getVisibility())
                .isEqualTo(View.GONE);
    }

    @Test
    public void testCarrierUpdateTime_shouldFormatDaysCorrectly() {
        int baseUnit = 2;
        int smudge = 6;
        final long updateTime = System.currentTimeMillis()
                - TimeUnit.DAYS.toMillis(baseUnit) - TimeUnit.HOURS.toMillis(smudge);
        mSummaryPreference.setUsageInfo(mCycleEnd, updateTime, FAKE_CARRIER, 1 /* numPlans */);

        mSummaryPreference.onBindViewHolder(mHolder);
        assertThat(mSummaryPreference.getCarrierInfo(mHolder).getText().toString())
                .isEqualTo("Updated by " + FAKE_CARRIER + " " + baseUnit + " days ago");
    }

    @Test
    public void testCarrierUpdateTime_shouldFormatHoursCorrectly() {
        int baseUnit = 2;
        int smudge = 6;
        final long updateTime = System.currentTimeMillis()
                - TimeUnit.HOURS.toMillis(baseUnit) - TimeUnit.MINUTES.toMillis(smudge);
        mSummaryPreference.setUsageInfo(mCycleEnd, updateTime, FAKE_CARRIER, 1 /* numPlans */);

        mSummaryPreference.onBindViewHolder(mHolder);
        assertThat(mSummaryPreference.getCarrierInfo(mHolder).getText().toString())
                .isEqualTo("Updated by " + FAKE_CARRIER + " " + baseUnit + " hr ago");
    }

    @Test
    public void testCarrierUpdateTime_shouldFormatMinutesCorrectly() {
        int baseUnit = 2;
        int smudge = 6;
        final long updateTime = System.currentTimeMillis()
                - TimeUnit.MINUTES.toMillis(baseUnit) - TimeUnit.SECONDS.toMillis(smudge);
        mSummaryPreference.setUsageInfo(mCycleEnd, updateTime, FAKE_CARRIER, 1 /* numPlans */);

        mSummaryPreference.onBindViewHolder(mHolder);
        assertThat(mSummaryPreference.getCarrierInfo(mHolder).getText().toString())
                .isEqualTo("Updated by " + FAKE_CARRIER + " " + baseUnit + " min ago");
    }

    @Test
    public void testCarrierUpdateTime_shouldFormatLessThanMinuteCorrectly() {
        final long updateTime = System.currentTimeMillis() - TimeUnit.SECONDS.toMillis(45);
        mSummaryPreference.setUsageInfo(mCycleEnd, updateTime, FAKE_CARRIER, 1 /* numPlans */);

        mSummaryPreference.onBindViewHolder(mHolder);
        assertThat(mSummaryPreference.getCarrierInfo(mHolder).getText().toString())
                .isEqualTo("Updated by " + FAKE_CARRIER + " just now");
    }

    @Test
    public void testCarrierUpdateTimeWithNoCarrier_shouldSayJustNow() {
        final long updateTime = System.currentTimeMillis() - TimeUnit.SECONDS.toMillis(45);
        mSummaryPreference.setUsageInfo(mCycleEnd, updateTime, null /* carrier */,
                1 /* numPlans */);

        mSummaryPreference.onBindViewHolder(mHolder);
        assertThat(mSummaryPreference.getCarrierInfo(mHolder).getText().toString())
                .isEqualTo("Updated just now");
    }

    @Test
    public void testCarrierUpdateTimeWithNoCarrier_shouldFormatTime() {
        final long updateTime = System.currentTimeMillis() - TimeUnit.MINUTES.toMillis(2);
        mSummaryPreference.setUsageInfo(mCycleEnd, updateTime, null /* carrier */,
                1 /* numPlans */);

        mSummaryPreference.onBindViewHolder(mHolder);
        assertThat(mSummaryPreference.getCarrierInfo(mHolder).getText().toString())
                .isEqualTo("Updated 2 min ago");
    }

    @Test
    public void setUsageInfo_withRecentCarrierUpdate_doesNotSetCarrierInfoWarningColorAndFont() {
        final long updateTime = System.currentTimeMillis() - TimeUnit.HOURS.toMillis(1);
        mSummaryPreference.setUsageInfo(mCycleEnd, updateTime, FAKE_CARRIER, 1 /* numPlans */);

        mSummaryPreference.onBindViewHolder(mHolder);
        TextView carrierInfo = mSummaryPreference.getCarrierInfo(mHolder);
        assertThat(carrierInfo.getVisibility()).isEqualTo(View.VISIBLE);
        assertThat(carrierInfo.getCurrentTextColor()).isEqualTo(
                Utils.getColorAttrDefaultColor(mContext, android.R.attr.textColorSecondary));
        assertThat(carrierInfo.getTypeface()).isEqualTo(Typeface.SANS_SERIF);
    }

    @Test
    public void testSetUsageInfo_withStaleCarrierUpdate_setsCarrierInfoWarningColorAndFont() {
        final long updateTime = System.currentTimeMillis() - TimeUnit.HOURS.toMillis(7);
        mSummaryPreference.setUsageInfo(mCycleEnd, updateTime, FAKE_CARRIER, 1 /* numPlans */);

        mSummaryPreference.onBindViewHolder(mHolder);
        TextView carrierInfo = mSummaryPreference.getCarrierInfo(mHolder);
        assertThat(carrierInfo.getVisibility()).isEqualTo(View.VISIBLE);
        assertThat(carrierInfo.getCurrentTextColor()).isEqualTo(
                Utils.getColorAttrDefaultColor(mContext, android.R.attr.colorError));
        assertThat(carrierInfo.getTypeface()).isEqualTo(
                DataUsageSummaryPreference.SANS_SERIF_MEDIUM);
    }

    @Test
    public void testSetUsageInfo_withNoDataPlans_usageTitleNotShown() {
        mSummaryPreference.setUsageInfo(mCycleEnd, -1, FAKE_CARRIER, 0 /* numPlans */);

        mSummaryPreference.onBindViewHolder(mHolder);
        assertThat(mSummaryPreference.getUsageTitle(mHolder).getVisibility()).isEqualTo(View.GONE);
    }

    @Test
    public void testSetUsageInfo_withMultipleDataPlans_usageTitleShown() {
        mSummaryPreference.setUsageInfo(mCycleEnd, mUpdateTime, FAKE_CARRIER, 2 /* numPlans */);

        mSummaryPreference.onBindViewHolder(mHolder);
        assertThat(mSummaryPreference.getUsageTitle(mHolder).getVisibility())
                .isEqualTo(View.VISIBLE);
    }

    @Test
    public void testSetUsageInfo_cycleRemainingTimeIsLessOneDay() {
        // just under one day
        final long cycleEnd = System.currentTimeMillis() + TimeUnit.HOURS.toMillis(23);
        mSummaryPreference.setUsageInfo(cycleEnd, -1, FAKE_CARRIER, 0 /* numPlans */);

        mSummaryPreference.onBindViewHolder(mHolder);
        assertThat(mSummaryPreference.getCycleTime(mHolder).getVisibility())
                .isEqualTo(View.VISIBLE);
        assertThat(mSummaryPreference.getCycleTime(mHolder).getText()).isEqualTo(
                ResourcesUtils.getResourcesString(
                        mContext, "billing_cycle_less_than_one_day_left"));
    }

    @Test
    public void testSetUsageInfo_cycleRemainingTimeNegativeDaysLeft_shouldDisplayNoneLeft() {
        final long cycleEnd = System.currentTimeMillis() - 1L;
        mSummaryPreference.setUsageInfo(cycleEnd, -1, FAKE_CARRIER, 0 /* numPlans */);

        mSummaryPreference.onBindViewHolder(mHolder);
        assertThat(mSummaryPreference.getCycleTime(mHolder).getVisibility())
                .isEqualTo(View.VISIBLE);
        assertThat(mSummaryPreference.getCycleTime(mHolder).getText()).isEqualTo(
                ResourcesUtils.getResourcesString(mContext, "billing_cycle_none_left"));
    }

    @Test
    public void testSetUsageInfo_cycleRemainingTimeDaysLeft_shouldUsePlurals() {
        final int daysLeft = 3;
        final long cycleEnd = System.currentTimeMillis() + TimeUnit.DAYS.toMillis(daysLeft)
                + TimeUnit.HOURS.toMillis(1);
        mSummaryPreference.setUsageInfo(cycleEnd, -1, FAKE_CARRIER, 0 /* numPlans */);

        mSummaryPreference.onBindViewHolder(mHolder);
        assertThat(mSummaryPreference.getCycleTime(mHolder).getVisibility())
                .isEqualTo(View.VISIBLE);
        assertThat(mSummaryPreference.getCycleTime(mHolder).getText())
                .isEqualTo(daysLeft + " days left");
    }

    @Test
    public void testSetLimitInfo_withLimitInfo_dataLimitsShown() {
        final String limitText = "test limit text";
        mSummaryPreference.setLimitInfo(limitText);

        mSummaryPreference.onBindViewHolder(mHolder);
        assertThat(mSummaryPreference.getDataLimits(mHolder).getVisibility())
                .isEqualTo(View.VISIBLE);
        assertThat(mSummaryPreference.getDataLimits(mHolder).getText()).isEqualTo(limitText);
    }

    @Test
    public void testSetLimitInfo_withNullLimitInfo_dataLimitsNotShown() {
        mSummaryPreference.setLimitInfo(null);

        mSummaryPreference.onBindViewHolder(mHolder);
        assertThat(mSummaryPreference.getDataLimits(mHolder).getVisibility()).isEqualTo(View.GONE);
    }

    @Test
    public void testSetLimitInfo_withEmptyLimitInfo_dataLimitsNotShown() {
        final String emptyLimitText = "";
        mSummaryPreference.setLimitInfo(emptyLimitText);

        mSummaryPreference.onBindViewHolder(mHolder);
        assertThat(mSummaryPreference.getDataLimits(mHolder).getVisibility()).isEqualTo(View.GONE);
    }

    @Test
    public void testSetChartEnabledFalse_hidesLabelBar() {
        setValidLabels();
        mSummaryPreference.setChartEnabled(false);

        mSummaryPreference.onBindViewHolder(mHolder);
        assertThat(mSummaryPreference.getLabelBar(mHolder).getVisibility()).isEqualTo(View.GONE);
        assertThat(mSummaryPreference.getProgressBar(mHolder).getVisibility()).isEqualTo(View.GONE);
    }

    @Test
    public void testSetEmptyLabels_hidesLabelBar() {
        mSummaryPreference.setLabels("", "");

        mSummaryPreference.onBindViewHolder(mHolder);
        assertThat(mSummaryPreference.getLabelBar(mHolder).getVisibility()).isEqualTo(View.GONE);
        assertThat(mSummaryPreference.getProgressBar(mHolder).getVisibility()).isEqualTo(View.GONE);
    }

    @Test
    public void testLabelBar_isVisible_whenLabelsSet() {
        setValidLabels();
        //mChartEnabled defaults to true

        mSummaryPreference.onBindViewHolder(mHolder);
        assertThat(mSummaryPreference.getLabelBar(mHolder).getVisibility())
                .isEqualTo(View.VISIBLE);
        assertThat(mSummaryPreference.getProgressBar(mHolder).getVisibility())
                .isEqualTo(View.VISIBLE);
    }

    @Test
    public void testSetProgress_updatesProgressBar() {
        setValidLabels();
        mSummaryPreference.setProgress(.5f);

        mSummaryPreference.onBindViewHolder(mHolder);
        assertThat(mSummaryPreference.getProgressBar(mHolder).getProgress()).isEqualTo(50);
    }

    private void setValidLabels() {
        mSummaryPreference.setLabels("0.0 GB", "5.0 GB");
    }

    @Test
    public void testSetUsageAndRemainingInfo_withUsageInfo_dataUsageAndRemainingShown() {
        mSummaryPreference.setUsageInfo(mCycleEnd, mUpdateTime, FAKE_CARRIER, 1 /* numPlans */);
        mSummaryPreference.setUsageNumbers(
                BillingCycleSettings.MIB_IN_BYTES,
                10 * BillingCycleSettings.MIB_IN_BYTES);

        mSummaryPreference.onBindViewHolder(mHolder);
        assertThat(mSummaryPreference.getDataUsed(mHolder).getText().toString())
                .isEqualTo("1.00 MB used");
        assertThat(mSummaryPreference.getDataRemaining(mHolder).getText().toString())
                .isEqualTo("9.00 MB left");
        assertThat(mSummaryPreference.getDataRemaining(mHolder).getVisibility())
                .isEqualTo(View.VISIBLE);
        final int colorId = Utils.getColorAttrDefaultColor(mContext, android.R.attr.colorAccent);
        assertThat(mSummaryPreference.getDataRemaining(mHolder).getCurrentTextColor())
                .isEqualTo(colorId);
    }

    @Test
    public void testSetUsageInfo_withDataOverusage() {
        mSummaryPreference.setUsageInfo(mCycleEnd, mUpdateTime, FAKE_CARRIER, 1 /* numPlans */);
        mSummaryPreference.setUsageNumbers(
                11 * BillingCycleSettings.MIB_IN_BYTES,
                10 * BillingCycleSettings.MIB_IN_BYTES);

        mSummaryPreference.onBindViewHolder(mHolder);
        assertThat(mSummaryPreference.getDataUsed(mHolder).getText().toString())
                .isEqualTo("11.00 MB used");
        assertThat(mSummaryPreference.getDataRemaining(mHolder).getText().toString())
                .isEqualTo("1.00 MB over");
        final int colorId = Utils.getColorAttrDefaultColor(mContext, android.R.attr.colorError);
        assertThat(mSummaryPreference.getDataRemaining(mHolder).getCurrentTextColor())
                .isEqualTo(colorId);
    }

    @Test
    public void testSetUsageInfo_withUsageInfo_dataUsageShown() {
        mSummaryPreference.setUsageInfo(mCycleEnd, -1, FAKE_CARRIER, 0 /* numPlans */);
        mSummaryPreference.setUsageNumbers(
                BillingCycleSettings.MIB_IN_BYTES, -1L);

        mSummaryPreference.onBindViewHolder(mHolder);
        assertThat(mSummaryPreference.getDataUsed(mHolder).getText().toString())
                .isEqualTo("1.00 MB used");
        assertThat(mSummaryPreference.getDataRemaining(mHolder).getText()).isEqualTo("");
    }

    @Test
    public void testSetUsageInfo_withOverflowStrings_dataRemainingNotShown() {
        LayoutInflater inflater = LayoutInflater.from(mContext);
        View view = inflater.inflate(mSummaryPreference.getLayoutResource(), null /* root */,
                false /* attachToRoot */);

        mSummaryPreference.setUsageInfo(mCycleEnd, mUpdateTime, FAKE_CARRIER, 1 /* numPlans */);
        mSummaryPreference.setUsageNumbers(
                BillingCycleSettings.MIB_IN_BYTES,
                10 * BillingCycleSettings.MIB_IN_BYTES);

        int data_used_formatted_id = ResourcesUtils.getResourcesId(
                mContext, "string", "data_used_formatted");
        int data_remaining_id = ResourcesUtils.getResourcesId(
                mContext, "string", "data_remaining");
        CharSequence data_used_formatted_cs = "^1 ^2 used with long trailing text";
        CharSequence data_remaining_cs = "^1 left";
        doReturn(data_used_formatted_cs).when(mResources).getText(data_used_formatted_id);
        doReturn(data_remaining_cs).when(mResources).getText(data_remaining_id);

        mSummaryPreference.onBindViewHolder(mHolder);

        TextView dataUsed = mSummaryPreference.getDataUsed(mHolder);
        TextView dataRemaining = mSummaryPreference.getDataRemaining(mHolder);
        int width = MeasureSpec.makeMeasureSpec(500, MeasureSpec.EXACTLY);
        dataUsed.measure(width, MeasureSpec.UNSPECIFIED);
        dataRemaining.measure(width, MeasureSpec.UNSPECIFIED);

        assertThat(dataRemaining.getVisibility()).isEqualTo(View.VISIBLE);

        MeasurableLinearLayout layout = mSummaryPreference.getLayout(mHolder);
        layout.measure(
                MeasureSpec.makeMeasureSpec(800, View.MeasureSpec.EXACTLY),
                MeasureSpec.makeMeasureSpec(1000, View.MeasureSpec.EXACTLY));

        assertThat(dataUsed.getText().toString()).isEqualTo("1.00 MB used with long trailing text");
        // TODO(b/175389659): re-enable this line once cuttlefish device specs are verified.
        // assertThat(dataRemaining.getVisibility()).isEqualTo(View.GONE);
    }
}
