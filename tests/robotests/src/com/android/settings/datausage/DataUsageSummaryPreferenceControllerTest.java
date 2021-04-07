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

package com.android.settings.datausage;

import static android.net.ConnectivityManager.TYPE_WIFI;

import static com.android.settings.core.BasePreferenceController.AVAILABLE;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkTemplate;
import android.telephony.SubscriptionInfo;
import android.telephony.SubscriptionManager;
import android.telephony.SubscriptionPlan;
import android.telephony.TelephonyManager;
import android.util.RecurrenceRule;

import androidx.fragment.app.FragmentActivity;
import androidx.preference.PreferenceFragmentCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.android.internal.logging.nano.MetricsProto;
import com.android.settings.R;
import com.android.settings.testutils.FakeFeatureFactory;
import com.android.settings.testutils.shadow.ShadowEntityHeaderController;
import com.android.settings.widget.EntityHeaderController;
import com.android.settingslib.NetworkPolicyEditor;
import com.android.settingslib.core.lifecycle.Lifecycle;
import com.android.settingslib.net.DataUsageController;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Answers;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

import java.time.Instant;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

@RunWith(RobolectricTestRunner.class)
@Config(shadows = ShadowEntityHeaderController.class)
public class DataUsageSummaryPreferenceControllerTest {

    private static final long UPDATE_BACKOFF_MS = TimeUnit.MINUTES.toMillis(13);
    private static final long CYCLE_BACKOFF_MS = TimeUnit.DAYS.toMillis(6);
    private static final long CYCLE_LENGTH_MS = TimeUnit.DAYS.toMillis(30);
    private static final long USAGE1 =  373 * BillingCycleSettings.MIB_IN_BYTES;
    private static final long LIMIT1 = BillingCycleSettings.GIB_IN_BYTES;
    private static final String CARRIER_NAME = "z-mobile";
    private static final String PERIOD = "Feb";

    @Mock
    private DataUsageController mDataUsageController;
    @Mock
    private DataUsageSummaryPreference mSummaryPreference;
    @Mock
    private NetworkPolicyEditor mPolicyEditor;
    @Mock
    private NetworkTemplate mNetworkTemplate;
    @Mock
    private SubscriptionInfo mSubscriptionInfo;
    @Mock
    private SubscriptionPlan mSubscriptionPlan;
    @Mock
    private Lifecycle mLifecycle;
    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private EntityHeaderController mHeaderController;
    @Mock
    private PreferenceFragmentCompat mPreferenceFragment;
    @Mock
    private TelephonyManager mTelephonyManager;
    @Mock
    private ConnectivityManager mConnectivityManager;

    private DataUsageInfoController mDataInfoController;

    private FakeFeatureFactory mFactory;
    private FragmentActivity mActivity;
    private Context mContext;
    private DataUsageSummaryPreferenceController mController;
    private int mDefaultSubscriptionId;
    private List<SubscriptionPlan> mSubscriptionPlans;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mContext = spy(RuntimeEnvironment.application);

        doReturn("%1$s %2%s").when(mContext)
                .getString(com.android.internal.R.string.fileSizeSuffix);

        mDefaultSubscriptionId = 1234;
        mSubscriptionPlans = new ArrayList<SubscriptionPlan>();

        mFactory = FakeFeatureFactory.setupForTest();
        when(mFactory.metricsFeatureProvider.getMetricsCategory(any(Object.class)))
                .thenReturn(MetricsProto.MetricsEvent.SETTINGS_APP_NOTIF_CATEGORY);
        ShadowEntityHeaderController.setUseMock(mHeaderController);

        mDataInfoController = spy(new DataUsageInfoController());
        doReturn(-1L).when(mDataInfoController).getSummaryLimit(any());

        mActivity = spy(Robolectric.buildActivity(FragmentActivity.class).get());
        doReturn(mTelephonyManager).when(mActivity).getSystemService(TelephonyManager.class);
        doReturn(mTelephonyManager).when(mTelephonyManager)
                .createForSubscriptionId(mDefaultSubscriptionId);
        when(mActivity.getSystemService(Context.CONNECTIVITY_SERVICE))
                .thenReturn(mConnectivityManager);
        doReturn(TelephonyManager.SIM_STATE_READY).when(mTelephonyManager).getSimState();
        when(mConnectivityManager.isNetworkSupported(TYPE_WIFI)).thenReturn(false);

        mController = spy(new DataUsageSummaryPreferenceController(
                mDataUsageController,
                mDataInfoController,
                mNetworkTemplate,
                mPolicyEditor,
                R.string.cell_data_template,
                mActivity, null, null, null, mDefaultSubscriptionId));
        doReturn(null).when(mController).getSubscriptionInfo(
                SubscriptionManager.INVALID_SUBSCRIPTION_ID);
        doReturn(null).when(mController).getSubscriptionPlans(
                SubscriptionManager.INVALID_SUBSCRIPTION_ID);

        doReturn(CARRIER_NAME).when(mSubscriptionInfo).getCarrierName();
        doReturn(mSubscriptionInfo).when(mController).getSubscriptionInfo(mDefaultSubscriptionId);
        doReturn(mSubscriptionPlans).when(mController).getSubscriptionPlans(mDefaultSubscriptionId);
    }

    @After
    public void tearDown() {
        ShadowEntityHeaderController.reset();
    }

    @Test
    public void testSummaryUpdate_onePlan_basic() {
        final long now = System.currentTimeMillis();
        final DataUsageController.DataUsageInfo info = createTestDataUsageInfo(now);

        final Intent intent = new Intent();

        doReturn(info).when(mDataUsageController).getDataUsageInfo(any());
        setupTestDataUsage(LIMIT1, USAGE1, now - UPDATE_BACKOFF_MS);
        createTestDataPlan(info.cycleStart, info.cycleEnd);
        doReturn(intent).when(mController).createManageSubscriptionIntent(mDefaultSubscriptionId);

        mController.updateState(mSummaryPreference);

        ArgumentCaptor<CharSequence> captor = ArgumentCaptor.forClass(CharSequence.class);
        verify(mSummaryPreference).setLimitInfo(captor.capture());
        CharSequence value = captor.getValue();
        assertThat(value.toString()).isEqualTo("512 MB data warning / 1.00 GB data limit");

        // TODO (b/170330084): return intent instead of null for mSummaryPreference
        verify(mSummaryPreference).setUsageInfo((info.cycleEnd / 1000) * 1000,
                now - UPDATE_BACKOFF_MS,
                CARRIER_NAME, 1 /* numPlans */, null /* launchIntent */);
        verify(mSummaryPreference).setChartEnabled(true);
        verify(mSummaryPreference).setWifiMode(false /* isWifiMode */, null /* usagePeriod */,
                false /* isSingleWifi */);
    }

    @Test
    public void testSummaryUpdate_noPlan_basic() {
        final long now = System.currentTimeMillis();
        final DataUsageController.DataUsageInfo info = createTestDataUsageInfo(now);

        final Intent intent = new Intent();

        doReturn(info).when(mDataUsageController).getDataUsageInfo(any());
        setupTestDataUsage(LIMIT1, USAGE1, now - UPDATE_BACKOFF_MS);

        mController.updateState(mSummaryPreference);

        ArgumentCaptor<CharSequence> captor = ArgumentCaptor.forClass(CharSequence.class);
        verify(mSummaryPreference).setLimitInfo(captor.capture());
        CharSequence value = captor.getValue();
        assertThat(value.toString()).isEqualTo("512 MB data warning / 1.00 GB data limit");

        verify(mSummaryPreference).setUsageInfo(
                info.cycleEnd,
                -1L /* snapshotTime */,
                CARRIER_NAME,
                0 /* numPlans */,
                null /* launchIntent */);
        verify(mSummaryPreference).setChartEnabled(true);
        verify(mSummaryPreference).setWifiMode(false /* isWifiMode */, null /* usagePeriod */,
                false /* isSingleWifi */);
    }

    @Test
    public void testSummaryUpdate_noCarrier_basic() {
        final long now = System.currentTimeMillis();
        final DataUsageController.DataUsageInfo info = createTestDataUsageInfo(now);

        doReturn(info).when(mDataUsageController).getDataUsageInfo(any());
        doReturn(null).when(mSubscriptionInfo).getCarrierName();
        setupTestDataUsage(LIMIT1, USAGE1, -1L /* snapshotTime */);

        mController.updateState(mSummaryPreference);

        ArgumentCaptor<CharSequence> captor = ArgumentCaptor.forClass(CharSequence.class);
        verify(mSummaryPreference).setLimitInfo(captor.capture());
        CharSequence value = captor.getValue();
        assertThat(value.toString()).isEqualTo("512 MB data warning / 1.00 GB data limit");

        verify(mSummaryPreference).setUsageInfo(
                info.cycleEnd,
                -1L /* snapshotTime */,
                null /* carrierName */,
                0 /* numPlans */,
                null /* launchIntent */);
        verify(mSummaryPreference).setChartEnabled(true);
        verify(mSummaryPreference).setWifiMode(false /* isWifiMode */, null /* usagePeriod */,
                false /* isSingleWifi */);
    }

    @Test
    public void testSummaryUpdate_noPlanData_basic() {
        final long now = System.currentTimeMillis();

        final DataUsageController.DataUsageInfo info = createTestDataUsageInfo(now);

        doReturn(info).when(mDataUsageController).getDataUsageInfo(any());
        doReturn(null).when(mSubscriptionInfo).getCarrierName();
        setupTestDataUsage(-1L /* dataPlanSize */, USAGE1, -1L /* snapshotTime */);

        mController.updateState(mSummaryPreference);

        ArgumentCaptor<CharSequence> captor = ArgumentCaptor.forClass(CharSequence.class);
        verify(mSummaryPreference).setLimitInfo(captor.capture());
        CharSequence value = captor.getValue();
        assertThat(value.toString()).isEqualTo("512 MB data warning / 1.00 GB data limit");
        verify(mSummaryPreference).setUsageInfo(
                info.cycleEnd,
                -1L /* snapshotTime */,
                null /* carrierName */,
                0 /* numPlans */,
                null /* launchIntent */);
        verify(mSummaryPreference).setChartEnabled(false);
        verify(mSummaryPreference).setWifiMode(false /* isWifiMode */, null /* usagePeriod */,
                false /* isSingleWifi */);
    }

    @Test
    public void testSummaryUpdate_noLimitNoWarning() {
        final long now = System.currentTimeMillis();
        final DataUsageController.DataUsageInfo info = createTestDataUsageInfo(now);
        info.warningLevel = 0L;
        info.limitLevel = 0L;

        final Intent intent = new Intent();

        doReturn(info).when(mDataUsageController).getDataUsageInfo(any());
        setupTestDataUsage(LIMIT1, USAGE1, now - UPDATE_BACKOFF_MS);

        mController.updateState(mSummaryPreference);
        verify(mSummaryPreference).setLimitInfo(null);
    }

    @Test
    public void testSummaryUpdate_warningOnly() {
        final long now = System.currentTimeMillis();
        final DataUsageController.DataUsageInfo info = createTestDataUsageInfo(now);
        info.warningLevel = BillingCycleSettings.MIB_IN_BYTES;
        info.limitLevel = 0L;

        final Intent intent = new Intent();

        doReturn(info).when(mDataUsageController).getDataUsageInfo(any());
        setupTestDataUsage(LIMIT1, USAGE1, now - UPDATE_BACKOFF_MS);

        mController.updateState(mSummaryPreference);

        ArgumentCaptor<CharSequence> captor = ArgumentCaptor.forClass(CharSequence.class);
        verify(mSummaryPreference).setLimitInfo(captor.capture());
        CharSequence value = captor.getValue();
        assertThat(value.toString()).isEqualTo("1.00 MB data warning");
    }

    @Test
    public void testSummaryUpdate_limitOnly() {
        final long now = System.currentTimeMillis();
        final DataUsageController.DataUsageInfo info = createTestDataUsageInfo(now);
        info.warningLevel = 0L;
        info.limitLevel = BillingCycleSettings.MIB_IN_BYTES;

        final Intent intent = new Intent();

        doReturn(info).when(mDataUsageController).getDataUsageInfo(any());
        setupTestDataUsage(LIMIT1, USAGE1, now - UPDATE_BACKOFF_MS);

        mController.updateState(mSummaryPreference);

        ArgumentCaptor<CharSequence> captor = ArgumentCaptor.forClass(CharSequence.class);
        verify(mSummaryPreference).setLimitInfo(captor.capture());
        CharSequence value = captor.getValue();
        assertThat(value.toString()).isEqualTo("1.00 MB data limit");
    }

    @Test
    public void testSummaryUpdate_limitAndWarning() {
        final long now = System.currentTimeMillis();
        final DataUsageController.DataUsageInfo info = createTestDataUsageInfo(now);
        info.warningLevel = BillingCycleSettings.MIB_IN_BYTES;
        info.limitLevel = BillingCycleSettings.MIB_IN_BYTES;

        final Intent intent = new Intent();

        doReturn(info).when(mDataUsageController).getDataUsageInfo(any());
        setupTestDataUsage(LIMIT1, USAGE1, now - UPDATE_BACKOFF_MS);

        mController.updateState(mSummaryPreference);

        ArgumentCaptor<CharSequence> captor = ArgumentCaptor.forClass(CharSequence.class);
        verify(mSummaryPreference).setLimitInfo(captor.capture());
        CharSequence value = captor.getValue();
        assertThat(value.toString()).isEqualTo("1.00 MB data warning / 1.00 MB data limit");
        verify(mSummaryPreference).setWifiMode(false /* isWifiMode */, null /* usagePeriod */,
                false /* isSingleWifi */);
    }

    @Test
    public void testMobileData_preferenceAvailable() {
        assertThat(mController.getAvailabilityStatus()).isEqualTo(AVAILABLE);
    }

    @Test
    public void testMobileData_noSimWifi_preferenceDisabled() {
        final int subscriptionId = SubscriptionManager.INVALID_SUBSCRIPTION_ID;
        mController.init(subscriptionId);
        mController.mDataUsageController = mDataUsageController;
        when(mConnectivityManager.isNetworkSupported(TYPE_WIFI)).thenReturn(true);
        assertThat(mController.getAvailabilityStatus()).isEqualTo(AVAILABLE);
    }

    @Test
    public void testMobileData_entityHeaderSet() {
        final RecyclerView recyclerView = new RecyclerView(mActivity);

        mController = spy(new DataUsageSummaryPreferenceController(
                mDataUsageController,
                mDataInfoController,
                mNetworkTemplate,
                mPolicyEditor,
                R.string.cell_data_template,
                mActivity, mLifecycle, mHeaderController, mPreferenceFragment,
                mDefaultSubscriptionId));

        when(mPreferenceFragment.getListView()).thenReturn(recyclerView);

        mController.onStart();

        verify(mHeaderController)
                .setRecyclerView(any(RecyclerView.class), any(Lifecycle.class));
        verify(mHeaderController).styleActionBar(any(Activity.class));
    }

    private DataUsageController.DataUsageInfo createTestDataUsageInfo(long now) {
        DataUsageController.DataUsageInfo info = new DataUsageController.DataUsageInfo();
        info.carrier = CARRIER_NAME;
        info.period = PERIOD;
        info.startDate = now;
        info.limitLevel = LIMIT1;
        info.warningLevel = LIMIT1 >> 1;
        info.usageLevel = USAGE1;
        info.cycleStart = now - CYCLE_BACKOFF_MS;
        info.cycleEnd = info.cycleStart + CYCLE_LENGTH_MS;
        return info;
    }

    private void setupTestDataUsage(long dataPlanSize, long dataUsageSize, long snapshotTime) {
        doReturn(dataPlanSize).when(mSubscriptionPlan).getDataLimitBytes();
        doReturn(dataUsageSize).when(mSubscriptionPlan).getDataUsageBytes();
        doReturn(snapshotTime).when(mSubscriptionPlan).getDataUsageTime();

        doReturn(dataPlanSize).when(mDataInfoController).getSummaryLimit(any());
    }

    private void createTestDataPlan(long startTime, long endTime) {
        final RecurrenceRule recurrenceRule = new RecurrenceRule(
                Instant.ofEpochMilli(startTime).atZone(ZoneId.systemDefault()),
                Instant.ofEpochMilli(endTime).atZone(ZoneId.systemDefault()),
                null);
        doReturn(recurrenceRule).when(mSubscriptionPlan).getCycleRule();
        mSubscriptionPlans.add(mSubscriptionPlan);
    }
}
