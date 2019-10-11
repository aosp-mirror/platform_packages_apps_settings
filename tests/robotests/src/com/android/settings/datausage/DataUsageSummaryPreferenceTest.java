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

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Intent;
import android.graphics.Typeface;
import android.net.ConnectivityManager;
import android.net.NetworkTemplate;
import android.os.Bundle;
import android.telephony.SubscriptionManager;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.fragment.app.FragmentActivity;
import androidx.preference.PreferenceViewHolder;

import com.android.settings.R;
import com.android.settings.SettingsActivity;
import com.android.settings.SubSettings;
import com.android.settingslib.Utils;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.MockitoAnnotations;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.Shadows;
import org.robolectric.shadows.ShadowActivity;

import java.util.concurrent.TimeUnit;

@RunWith(RobolectricTestRunner.class)
public class DataUsageSummaryPreferenceTest {

    private static final long CYCLE_DURATION_MILLIS = 1000000000L;
    private static final long UPDATE_LAG_MILLIS = 10000000L;
    private static final String DUMMY_CARRIER = "z-mobile";

    private Activity mActivity;
    private PreferenceViewHolder mHolder;
    private DataUsageSummaryPreference mSummaryPreference;
    private TextView mUsageTitle;
    private TextView mCycleTime;
    private TextView mCarrierInfo;
    private TextView mDataLimits;
    private TextView mDataUsed;
    private TextView mDataRemaining;
    private Button mLaunchButton;
    private LinearLayout mLabelBar;
    private TextView mLabel1;
    private TextView mLabel2;
    private ProgressBar mProgressBar;

    private long mCycleEnd;
    private long mUpdateTime;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mActivity = spy(Robolectric.setupActivity(Activity.class));
        mSummaryPreference = new DataUsageSummaryPreference(mActivity, null /* attrs */);
        LayoutInflater inflater = LayoutInflater.from(mActivity);
        View view = inflater.inflate(mSummaryPreference.getLayoutResource(), null /* root */,
                false /* attachToRoot */);

        mHolder = spy(PreferenceViewHolder.createInstanceForTests(view));

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
    public void testCarrierUpdateTime_shouldFormatDaysCorrectly() {
        int baseUnit = 2;
        int smudge = 6;
        final long updateTime = System.currentTimeMillis()
                - TimeUnit.DAYS.toMillis(baseUnit) - TimeUnit.HOURS.toMillis(smudge);
        mCarrierInfo = (TextView) mHolder.findViewById(R.id.carrier_and_update);
        mSummaryPreference.setUsageInfo(mCycleEnd, updateTime, DUMMY_CARRIER, 1 /* numPlans */,
                new Intent());

        bindViewHolder();
        assertThat(mCarrierInfo.getText().toString())
                .isEqualTo("Updated by " + DUMMY_CARRIER + " " + baseUnit + " days ago");
    }

    @Test
    public void testCarrierUpdateTime_shouldFormatHoursCorrectly() {
        int baseUnit = 2;
        int smudge = 6;
        final long updateTime = System.currentTimeMillis()
                - TimeUnit.HOURS.toMillis(baseUnit) - TimeUnit.MINUTES.toMillis(smudge);
        mCarrierInfo = (TextView) mHolder.findViewById(R.id.carrier_and_update);
        mSummaryPreference.setUsageInfo(mCycleEnd, updateTime, DUMMY_CARRIER, 1 /* numPlans */,
                new Intent());

        bindViewHolder();
        assertThat(mCarrierInfo.getText().toString())
                .isEqualTo("Updated by " + DUMMY_CARRIER + " " + baseUnit + " hr ago");
    }

    @Test
    public void testCarrierUpdateTime_shouldFormatMinutesCorrectly() {
        int baseUnit = 2;
        int smudge = 6;
        final long updateTime = System.currentTimeMillis()
                - TimeUnit.MINUTES.toMillis(baseUnit) - TimeUnit.SECONDS.toMillis(smudge);
        mCarrierInfo = (TextView) mHolder.findViewById(R.id.carrier_and_update);
        mSummaryPreference.setUsageInfo(mCycleEnd, updateTime, DUMMY_CARRIER, 1 /* numPlans */,
                new Intent());

        bindViewHolder();
        assertThat(mCarrierInfo.getText().toString())
                .isEqualTo("Updated by " + DUMMY_CARRIER + " " + baseUnit + " min ago");
    }

    @Test
    public void testCarrierUpdateTime_shouldFormatLessThanMinuteCorrectly() {
        final long updateTime = System.currentTimeMillis() - TimeUnit.SECONDS.toMillis(45);
        mCarrierInfo = (TextView) mHolder.findViewById(R.id.carrier_and_update);
        mSummaryPreference.setUsageInfo(mCycleEnd, updateTime, DUMMY_CARRIER, 1 /* numPlans */,
                new Intent());

        bindViewHolder();
        assertThat(mCarrierInfo.getText().toString())
                .isEqualTo("Updated by " + DUMMY_CARRIER + " just now");
    }

    @Test
    public void testCarrierUpdateTimeWithNoCarrier_shouldSayJustNow() {
        final long updateTime = System.currentTimeMillis() - TimeUnit.SECONDS.toMillis(45);
        mCarrierInfo = (TextView) mHolder.findViewById(R.id.carrier_and_update);
        mSummaryPreference.setUsageInfo(mCycleEnd, updateTime, null /* carrier */,
                1 /* numPlans */, new Intent());

        bindViewHolder();
        assertThat(mCarrierInfo.getText().toString())
                .isEqualTo("Updated just now");
    }

    @Test
    public void testCarrierUpdateTimeWithNoCarrier_shouldFormatTime() {
        final long updateTime = System.currentTimeMillis() - TimeUnit.MINUTES.toMillis(2);
        mCarrierInfo = (TextView) mHolder.findViewById(R.id.carrier_and_update);
        mSummaryPreference.setUsageInfo(mCycleEnd, updateTime, null /* carrier */,
                1 /* numPlans */, new Intent());

        bindViewHolder();
        assertThat(mCarrierInfo.getText().toString())
                .isEqualTo("Updated 2 min ago");
    }

    @Test
    public void setUsageInfo_withRecentCarrierUpdate_doesNotSetCarrierInfoWarningColorAndFont() {
        final long updateTime = System.currentTimeMillis() - TimeUnit.HOURS.toMillis(1);
        mCarrierInfo = (TextView) mHolder.findViewById(R.id.carrier_and_update);
        mSummaryPreference.setUsageInfo(mCycleEnd, updateTime, DUMMY_CARRIER, 1 /* numPlans */,
                new Intent());

        bindViewHolder();
        assertThat(mCarrierInfo.getVisibility()).isEqualTo(View.VISIBLE);
        assertThat(mCarrierInfo.getCurrentTextColor()).isEqualTo(
                Utils.getColorAttrDefaultColor(mActivity, android.R.attr.textColorSecondary));
        assertThat(mCarrierInfo.getTypeface()).isEqualTo(Typeface.SANS_SERIF);
    }

    @Test
    public void testSetUsageInfo_withStaleCarrierUpdate_setsCarrierInfoWarningColorAndFont() {
        final long updateTime = System.currentTimeMillis() - TimeUnit.HOURS.toMillis(7);
        mSummaryPreference.setUsageInfo(mCycleEnd, updateTime, DUMMY_CARRIER, 1 /* numPlans */,
                new Intent());

        bindViewHolder();
        assertThat(mCarrierInfo.getVisibility()).isEqualTo(View.VISIBLE);
        assertThat(mCarrierInfo.getCurrentTextColor()).isEqualTo(
                Utils.getColorAttrDefaultColor(mActivity, android.R.attr.colorError));
        assertThat(mCarrierInfo.getTypeface()).isEqualTo(
                DataUsageSummaryPreference.SANS_SERIF_MEDIUM);
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
    public void testSetUsageInfo_cycleRemainingTimeIsLessOneDay() {
        // just under one day
        final long cycleEnd = System.currentTimeMillis() + TimeUnit.HOURS.toMillis(23);
        mSummaryPreference.setUsageInfo(cycleEnd, mUpdateTime, DUMMY_CARRIER, 0 /* numPlans */,
                new Intent());

        bindViewHolder();
        assertThat(mCycleTime.getVisibility()).isEqualTo(View.VISIBLE);
        assertThat(mCycleTime.getText()).isEqualTo(
                mActivity.getString(R.string.billing_cycle_less_than_one_day_left));
    }

    @Test
    public void testSetUsageInfo_cycleRemainingTimeNegativeDaysLeft_shouldDisplayNoneLeft() {
        final long cycleEnd = System.currentTimeMillis() - 1L;
        mSummaryPreference.setUsageInfo(cycleEnd, mUpdateTime, DUMMY_CARRIER, 0 /* numPlans */,
                new Intent());

        bindViewHolder();
        assertThat(mCycleTime.getVisibility()).isEqualTo(View.VISIBLE);
        assertThat(mCycleTime.getText()).isEqualTo(
                mActivity.getString(R.string.billing_cycle_none_left));
    }

    @Test
    public void testSetUsageInfo_cycleRemainingTimeDaysLeft_shouldUsePlurals() {
        final int daysLeft = 3;
        final long cycleEnd = System.currentTimeMillis() + TimeUnit.DAYS.toMillis(daysLeft)
                + TimeUnit.HOURS.toMillis(1);
        mSummaryPreference.setUsageInfo(cycleEnd, mUpdateTime, DUMMY_CARRIER, 0 /* numPlans */,
                new Intent());

        bindViewHolder();
        assertThat(mCycleTime.getVisibility()).isEqualTo(View.VISIBLE);
        assertThat(mCycleTime.getText()).isEqualTo(daysLeft + " days left");
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

    @Test
    public void testSetLimitInfo_withEmptyLimitInfo_dataLimitsNotShown() {
        final String emptyLimitText = "";
        mSummaryPreference.setLimitInfo(emptyLimitText);

        bindViewHolder();
        assertThat(mDataLimits.getVisibility()).isEqualTo(View.GONE);
    }

    @Test
    public void testSetChartEnabledFalse_hidesLabelBar() {
        setValidLabels();
        mSummaryPreference.setChartEnabled(false);

        bindViewHolder();
        assertThat(mLabelBar.getVisibility()).isEqualTo(View.GONE);
        assertThat(mProgressBar.getVisibility()).isEqualTo(View.GONE);
    }

    @Test
    public void testSetEmptyLabels_hidesLabelBar() {
        mSummaryPreference.setLabels("", "");

        bindViewHolder();
        assertThat(mLabelBar.getVisibility()).isEqualTo(View.GONE);
        assertThat(mProgressBar.getVisibility()).isEqualTo(View.GONE);
    }

    @Test
    public void testLabelBar_isVisible_whenLabelsSet() {
        setValidLabels();
        //mChartEnabled defaults to true

        bindViewHolder();
        assertThat(mLabelBar.getVisibility()).isEqualTo(View.VISIBLE);
        assertThat(mProgressBar.getVisibility()).isEqualTo(View.VISIBLE);
    }

    @Test
    public void testSetProgress_updatesProgressBar() {
        setValidLabels();
        mSummaryPreference.setProgress(.5f);

        bindViewHolder();
        assertThat(mProgressBar.getProgress()).isEqualTo(50);
    }

    private void setValidLabels() {
        mSummaryPreference.setLabels("0.0 GB", "5.0 GB");
    }

    @Test
    public void testSetUsageAndRemainingInfo_withUsageInfo_dataUsageAndRemainingShown() {
        mSummaryPreference.setUsageInfo(mCycleEnd, mUpdateTime, DUMMY_CARRIER, 1 /* numPlans */,
                new Intent());
        mSummaryPreference.setUsageNumbers(
                BillingCycleSettings.MIB_IN_BYTES,
                10 * BillingCycleSettings.MIB_IN_BYTES,
                true /* hasMobileData */);

        bindViewHolder();
        assertThat(mDataUsed.getText().toString()).isEqualTo("1.00 MB used");
        assertThat(mDataRemaining.getText().toString()).isEqualTo("9.00 MB left");
        assertThat(mDataRemaining.getVisibility()).isEqualTo(View.VISIBLE);
        final int colorId = Utils.getColorAttrDefaultColor(mActivity, android.R.attr.colorAccent);
        assertThat(mDataRemaining.getCurrentTextColor()).isEqualTo(colorId);
    }

    @Test
    public void testSetUsageInfo_withDataOverusage() {
        mSummaryPreference.setUsageInfo(mCycleEnd, mUpdateTime, DUMMY_CARRIER, 1 /* numPlans */,
                new Intent());
        mSummaryPreference.setUsageNumbers(
                11 * BillingCycleSettings.MIB_IN_BYTES,
                10 * BillingCycleSettings.MIB_IN_BYTES,
                true /* hasMobileData */);

        bindViewHolder();
        assertThat(mDataUsed.getText().toString()).isEqualTo("11.00 MB used");
        assertThat(mDataRemaining.getText().toString()).isEqualTo("1.00 MB over");
        final int colorId = Utils.getColorAttrDefaultColor(mActivity, android.R.attr.colorError);
        assertThat(mDataRemaining.getCurrentTextColor()).isEqualTo(colorId);
    }

    @Test
    public void testSetUsageInfo_withUsageInfo_dataUsageShown() {
        mSummaryPreference.setUsageInfo(mCycleEnd, mUpdateTime, DUMMY_CARRIER, 0 /* numPlans */,
                new Intent());
        mSummaryPreference.setUsageNumbers(
                BillingCycleSettings.MIB_IN_BYTES, -1L, true /* hasMobileData */);

        bindViewHolder();
        assertThat(mDataUsed.getText().toString()).isEqualTo("1.00 MB used");
        assertThat(mDataRemaining.getText()).isEqualTo("");
    }

    @Test
    public void testSetAppIntent_toMdpApp_intentCorrect() {
        final FragmentActivity activity = Robolectric.setupActivity(FragmentActivity.class);
        final Intent intent = new Intent(SubscriptionManager.ACTION_MANAGE_SUBSCRIPTION_PLANS);
        intent.setPackage("test-owner.example.com");
        intent.putExtra(SubscriptionManager.EXTRA_SUBSCRIPTION_INDEX, 42);

        mSummaryPreference.setUsageInfo(mCycleEnd, mUpdateTime, DUMMY_CARRIER, 0 /* numPlans */,
                intent);

        bindViewHolder();
        assertThat(mLaunchButton.getVisibility()).isEqualTo(View.VISIBLE);
        assertThat(mLaunchButton.getText())
                .isEqualTo(mActivity.getString(R.string.launch_mdp_app_text));

        mLaunchButton.callOnClick();
        ShadowActivity shadowActivity = Shadows.shadowOf(activity);
        Intent startedIntent = shadowActivity.getNextStartedActivity();
        assertThat(startedIntent.getAction())
                .isEqualTo(SubscriptionManager.ACTION_MANAGE_SUBSCRIPTION_PLANS);
        assertThat(startedIntent.getPackage()).isEqualTo("test-owner.example.com");
        assertThat(startedIntent.getIntExtra(SubscriptionManager.EXTRA_SUBSCRIPTION_INDEX, -1))
                .isEqualTo(42);
    }

    @Test
    public void testSetUsageInfo_withOverflowStrings_dataRemainingNotShown() {
        LayoutInflater inflater = LayoutInflater.from(mActivity);
        View view = inflater.inflate(mSummaryPreference.getLayoutResource(), null /* root */,
                false /* attachToRoot */);

        TextView dataUsed = spy(new TextView(mActivity));
        TextView dataRemaining = spy(new TextView(mActivity));
        doReturn(dataUsed).when(mHolder).findViewById(R.id.data_usage_view);
        doReturn(dataRemaining).when(mHolder).findViewById(R.id.data_remaining_view);

        mSummaryPreference.setUsageInfo(mCycleEnd, mUpdateTime, DUMMY_CARRIER, 1 /* numPlans */,
                new Intent());
        mSummaryPreference.setUsageNumbers(
                BillingCycleSettings.MIB_IN_BYTES,
                10 * BillingCycleSettings.MIB_IN_BYTES,
                true /* hasMobileData */);

        when(mActivity.getResources()).thenCallRealMethod();
        when(mActivity.getText(R.string.data_used_formatted))
                .thenReturn("^1 ^2 used with long trailing text");
        when(mActivity.getText(R.string.data_remaining)).thenReturn("^1 left");

        bindViewHolder();

        doReturn(500).when(dataUsed).getMeasuredWidth();
        doReturn(500).when(dataRemaining).getMeasuredWidth();

        assertThat(dataRemaining.getVisibility()).isEqualTo(View.VISIBLE);

        MeasurableLinearLayout layout =
                (MeasurableLinearLayout) mHolder.findViewById(R.id.usage_layout);
        layout.measure(
                View.MeasureSpec.makeMeasureSpec(800, View.MeasureSpec.EXACTLY),
                View.MeasureSpec.makeMeasureSpec(1000, View.MeasureSpec.EXACTLY));

        assertThat(dataUsed.getText().toString()).isEqualTo("1.00 MB used with long trailing text");
        assertThat(dataRemaining.getVisibility()).isEqualTo(View.GONE);
    }

    @Test
    public void testSetWifiMode_withUsageInfo_dataUsageShown() {
        final int daysLeft = 3;
        final long cycleEnd = System.currentTimeMillis() + TimeUnit.DAYS.toMillis(daysLeft)
                + TimeUnit.HOURS.toMillis(1);
        final FragmentActivity activity = Robolectric.setupActivity(FragmentActivity.class);
        mSummaryPreference = spy(mSummaryPreference);
        mSummaryPreference.setUsageInfo(cycleEnd, mUpdateTime, DUMMY_CARRIER, 0 /* numPlans */,
                new Intent());
        mSummaryPreference.setUsageNumbers(1000000L, -1L, true);
        final String cycleText = "The quick fox";
        mSummaryPreference.setWifiMode(true /* isWifiMode */, cycleText, false /* isSingleWifi */);
        doReturn(200L).when(mSummaryPreference).getHistoricalUsageLevel();

        bindViewHolder();
        assertThat(mUsageTitle.getText().toString())
                .isEqualTo(mActivity.getString(R.string.data_usage_wifi_title));
        assertThat(mUsageTitle.getVisibility()).isEqualTo(View.VISIBLE);
        assertThat(mCycleTime.getVisibility()).isEqualTo(View.VISIBLE);
        assertThat(mCycleTime.getText()).isEqualTo(cycleText);
        assertThat(mCarrierInfo.getVisibility()).isEqualTo(View.GONE);
        assertThat(mDataLimits.getVisibility()).isEqualTo(View.GONE);
        assertThat(mLaunchButton.getVisibility()).isEqualTo(View.VISIBLE);
        assertThat(mLaunchButton.getText())
                .isEqualTo(mActivity.getString(R.string.launch_wifi_text));

        mLaunchButton.callOnClick();
        ShadowActivity shadowActivity = Shadows.shadowOf(activity);
        Intent startedIntent = shadowActivity.getNextStartedActivity();
        assertThat(startedIntent.getComponent()).isEqualTo(new ComponentName("com.android.settings",
                SubSettings.class.getName()));

        final Bundle expect = new Bundle(1);
        expect.putParcelable(DataUsageList.EXTRA_NETWORK_TEMPLATE,
                NetworkTemplate.buildTemplateWifiWildcard());
        final Bundle actual = startedIntent
                .getBundleExtra(SettingsActivity.EXTRA_SHOW_FRAGMENT_ARGUMENTS);
        assertThat((NetworkTemplate) actual.getParcelable(DataUsageList.EXTRA_NETWORK_TEMPLATE))
                .isEqualTo(NetworkTemplate.buildTemplateWifiWildcard());

        assertThat(startedIntent.getIntExtra(SettingsActivity.EXTRA_SHOW_FRAGMENT_TITLE_RESID, 0))
                .isEqualTo(R.string.wifi_data_usage);
    }

    @Test
    public void testSetWifiMode_noUsageInfo_shouldDisableLaunchButton() {
        mSummaryPreference = spy(mSummaryPreference);
        mSummaryPreference.setWifiMode(true /* isWifiMode */, "Test cycle text",
                false /* isSingleWifi */);
        doReturn(0L).when(mSummaryPreference).getHistoricalUsageLevel();

        bindViewHolder();

        assertThat(mLaunchButton.isEnabled()).isFalse();
    }

    @Test
    public void launchWifiDataUsage_shouldSetWifiNetworkTypeInIntentExtra() {
        mSummaryPreference.launchWifiDataUsage(mActivity);

        final Intent launchIntent = Shadows.shadowOf(mActivity).getNextStartedActivity();
        final Bundle args =
            launchIntent.getBundleExtra(SettingsActivity.EXTRA_SHOW_FRAGMENT_ARGUMENTS);

        assertThat(args.getInt(DataUsageList.EXTRA_NETWORK_TYPE))
            .isEqualTo(ConnectivityManager.TYPE_WIFI);
    }

    private void bindViewHolder() {
        mSummaryPreference.onBindViewHolder(mHolder);
        mUsageTitle = (TextView) mHolder.findViewById(R.id.usage_title);
        mCycleTime = (TextView) mHolder.findViewById(R.id.cycle_left_time);
        mCarrierInfo = (TextView) mHolder.findViewById(R.id.carrier_and_update);
        mDataLimits = (TextView) mHolder.findViewById(R.id.data_limits);
        mDataUsed = spy((TextView) mHolder.findViewById(R.id.data_usage_view));
        mDataRemaining = spy((TextView) mHolder.findViewById(R.id.data_remaining_view));
        mLaunchButton = (Button) mHolder.findViewById(R.id.launch_mdp_app_button);
        mLabelBar = (LinearLayout) mHolder.findViewById(R.id.label_bar);
        mLabel1 = (TextView) mHolder.findViewById(R.id.text1);
        mLabel2 = (TextView) mHolder.findViewById(R.id.text2);
        mProgressBar = (ProgressBar) mHolder.findViewById(R.id.determinateBar);
    }
}
