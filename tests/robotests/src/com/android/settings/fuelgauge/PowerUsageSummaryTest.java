/*
 * Copyright (C) 2016 The Android Open Source Project
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

import android.content.Context;
import android.content.Intent;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import com.android.internal.os.BatterySipper;
import com.android.internal.os.BatteryStatsImpl;
import com.android.settings.R;
import com.android.settings.TestConfig;
import com.android.settings.testutils.FakeFeatureFactory;
import org.junit.Before;
import org.junit.Test;

import org.junit.runner.RunWith;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import static com.android.settings.fuelgauge.PowerUsageBase.MENU_STATS_REFRESH;
import static com.android.settings.fuelgauge.PowerUsageSummary.MENU_ADDITIONAL_BATTERY_INFO;
import static com.google.common.truth.Truth.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link PowerUsageSummary}.
 */
// TODO: Improve this test class so that it starts up the real activity and fragment.
@RunWith(RobolectricTestRunner.class)
@Config(manifest = TestConfig.MANIFEST_PATH, sdk = TestConfig.SDK_VERSION)
public class PowerUsageSummaryTest {
    private static final String[] PACKAGE_NAMES = {"com.app1", "com.app2"};
    private static final int UID = 123;

    private static final Intent ADDITIONAL_BATTERY_INFO_INTENT =
            new Intent("com.example.app.ADDITIONAL_BATTERY_INFO");

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private Context mContext;
    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private Menu mMenu;
    @Mock
    private MenuItem mRefreshMenu;
    @Mock
    private MenuItem mAdditionalBatteryInfoMenu;
    @Mock
    private MenuInflater mMenuInflater;
    @Mock
    private BatterySipper mBatterySipper;

    private TestFragment mFragment;
    private FakeFeatureFactory mFeatureFactory;
    private PowerUsageSummary mPowerUsageSummary;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);

        FakeFeatureFactory.setupForTest(mContext);
        mFeatureFactory = (FakeFeatureFactory) FakeFeatureFactory.getFactory(mContext);

        mFragment = new TestFragment(mContext);

        when(mMenu.add(Menu.NONE, MENU_STATS_REFRESH, Menu.NONE,
                R.string.menu_stats_refresh)
                .setIcon(com.android.internal.R.drawable.ic_menu_refresh)
                .setAlphabeticShortcut('r'))
                .thenReturn(mRefreshMenu);
        when(mAdditionalBatteryInfoMenu.getItemId())
                .thenReturn(MENU_ADDITIONAL_BATTERY_INFO);
        when(mFeatureFactory.powerUsageFeatureProvider.getAdditionalBatteryInfoIntent())
                .thenReturn(ADDITIONAL_BATTERY_INFO_INTENT);

        mPowerUsageSummary = new PowerUsageSummary();

        when(mBatterySipper.getPackages()).thenReturn(PACKAGE_NAMES);
        when(mBatterySipper.getUid()).thenReturn(UID);
    }

    @Test
    public void testOptionsMenu_additionalBatteryInfoEnabled() {
        when(mFeatureFactory.powerUsageFeatureProvider.isAdditionalBatteryInfoEnabled())
                .thenReturn(true);

        mFragment.onCreateOptionsMenu(mMenu, mMenuInflater);

        verify(mMenu).add(Menu.NONE, MENU_ADDITIONAL_BATTERY_INFO,
                Menu.NONE, R.string.additional_battery_info);

        mFragment.onOptionsItemSelected(mAdditionalBatteryInfoMenu);

        assertThat(mFragment.mStartActivityCalled).isTrue();
        assertThat(mFragment.mStartActivityIntent).isEqualTo(ADDITIONAL_BATTERY_INFO_INTENT);
    }

    @Test
    public void testOptionsMenu_additionalBatteryInfoDisabled() {
        when(mFeatureFactory.powerUsageFeatureProvider.isAdditionalBatteryInfoEnabled())
                .thenReturn(false);

        mFragment.onCreateOptionsMenu(mMenu, mMenuInflater);

        verify(mMenu, never()).add(Menu.NONE, MENU_ADDITIONAL_BATTERY_INFO,
                Menu.NONE, R.string.additional_battery_info);
    }

    @Test
    public void testExtractKeyFromSipper_TypeAPPUidObjectNull_ReturnPackageNames() {
        mBatterySipper.uidObj = null;
        mBatterySipper.drainType = BatterySipper.DrainType.APP;

        final String key = mPowerUsageSummary.extractKeyFromSipper(mBatterySipper);
        assertThat(key).isEqualTo(TextUtils.concat(mBatterySipper.getPackages()).toString());
    }

    @Test
    public void testExtractKeyFromSipper_TypeOther_ReturnDrainType() {
        mBatterySipper.uidObj = null;
        mBatterySipper.drainType = BatterySipper.DrainType.BLUETOOTH;

        final String key = mPowerUsageSummary.extractKeyFromSipper(mBatterySipper);
        assertThat(key).isEqualTo(mBatterySipper.drainType.toString());
    }

    @Test
    public void testExtractKeyFromSipper_TypeAPPUidObjectNotNull_ReturnUid() {
        mBatterySipper.uidObj = new BatteryStatsImpl.Uid(new BatteryStatsImpl(), UID);
        mBatterySipper.drainType = BatterySipper.DrainType.APP;

        final String key = mPowerUsageSummary.extractKeyFromSipper(mBatterySipper);
        assertThat(key).isEqualTo(Integer.toString(mBatterySipper.getUid()));
    }

    public static class TestFragment extends PowerUsageSummary {

        private Context mContext;
        private boolean mStartActivityCalled;
        private Intent mStartActivityIntent;

        public TestFragment(Context context) {
            mContext = context;
        }

        @Override
        public Context getContext() {
            return mContext;
        }

        @Override
        public void startActivity(Intent intent) {
            mStartActivityCalled = true;
            mStartActivityIntent = intent;
        }
    }
}
