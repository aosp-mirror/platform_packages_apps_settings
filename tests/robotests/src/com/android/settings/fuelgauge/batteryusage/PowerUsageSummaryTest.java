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
package com.android.settings.fuelgauge.batteryusage;

import static com.android.settings.fuelgauge.batteryusage.PowerUsageSummary.KEY_BATTERY_ERROR;
import static com.android.settings.fuelgauge.batteryusage.PowerUsageSummary.KEY_BATTERY_USAGE;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.provider.Settings;

import androidx.loader.app.LoaderManager;
import androidx.loader.content.Loader;
import androidx.preference.Preference;
import androidx.preference.PreferenceScreen;

import com.android.settings.R;
import com.android.settings.SettingsActivity;
import com.android.settings.fuelgauge.BatteryBroadcastReceiver;
import com.android.settings.fuelgauge.BatteryInfo;
import com.android.settings.fuelgauge.BatteryUtils;
import com.android.settings.fuelgauge.batterytip.BatteryTipPreferenceController;
import com.android.settings.fuelgauge.batterytip.tips.BatteryTip;
import com.android.settings.testutils.FakeFeatureFactory;
import com.android.settings.testutils.XmlTestUtils;
import com.android.settings.testutils.shadow.ShadowUtils;
import com.android.settingslib.core.instrumentation.VisibilityLoggerMixin;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import org.robolectric.util.ReflectionHelpers;

import java.util.List;

// TODO: Improve this test class so that it starts up the real activity and fragment.
@RunWith(RobolectricTestRunner.class)
@Config(
        shadows = {
            com.android.settings.testutils.shadow.ShadowFragment.class,
        })
public class PowerUsageSummaryTest {
    private static Intent sAdditionalBatteryInfoIntent;

    @BeforeClass
    public static void beforeClass() {
        sAdditionalBatteryInfoIntent = new Intent("com.example.app.ADDITIONAL_BATTERY_INFO");
    }

    @Mock private SettingsActivity mSettingsActivity;
    @Mock private LoaderManager mLoaderManager;
    @Mock private Loader<BatteryTip> mBatteryTipLoader;
    @Mock private Loader<BatteryInfo> mBatteryInfoLoader;
    @Mock private ContentResolver mContentResolver;
    @Mock private BatteryBroadcastReceiver mBatteryBroadcastReceiver;
    @Mock private VisibilityLoggerMixin mVisibilityLoggerMixin;
    @Mock private PreferenceScreen mPreferenceScreen;
    @Mock private Preference mBatteryUsagePreference;
    @Mock private Preference mHelpPreference;

    private Context mRealContext;
    private TestFragment mFragment;
    private FakeFeatureFactory mFeatureFactory;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);

        mRealContext = spy(RuntimeEnvironment.application);
        mFeatureFactory = FakeFeatureFactory.setupForTest();
        mFragment = spy(new TestFragment(mRealContext, mLoaderManager));
        mFragment.initFeatureProvider();
        doNothing().when(mFragment).restartBatteryStatsLoader(anyInt());
        when(mFragment.getActivity()).thenReturn(mSettingsActivity);
        when(mFeatureFactory.powerUsageFeatureProvider.getAdditionalBatteryInfoIntent())
                .thenReturn(sAdditionalBatteryInfoIntent);
        mFragment.mBatteryUtils = Mockito.spy(new BatteryUtils(mRealContext));
        ReflectionHelpers.setField(mFragment, "mVisibilityLoggerMixin", mVisibilityLoggerMixin);
        ReflectionHelpers.setField(
                mFragment, "mBatteryBroadcastReceiver", mBatteryBroadcastReceiver);
        doReturn(mPreferenceScreen).when(mFragment).getPreferenceScreen();
        when(mFragment.getContentResolver()).thenReturn(mContentResolver);
        when(mFragment.findPreference(KEY_BATTERY_USAGE)).thenReturn(mBatteryUsagePreference);
        when(mFragment.findPreference(KEY_BATTERY_ERROR)).thenReturn(mHelpPreference);
    }

    @Test
    @Config(shadows = ShadowUtils.class)
    public void nonIndexableKeys_MatchPreferenceKeys() {
        final Context context = RuntimeEnvironment.application;
        final List<String> niks =
                PowerUsageSummary.SEARCH_INDEX_DATA_PROVIDER.getNonIndexableKeys(context);

        final List<String> keys =
                XmlTestUtils.getKeysFromPreferenceXml(context, R.xml.power_usage_summary);

        assertThat(keys).containsAtLeastElementsIn(niks);
    }

    @Test
    public void initPreference_hasCorrectSummary() {
        mFragment.initPreference();

        verify(mBatteryUsagePreference).setSummary("View usage since last full charge");
    }

    @Test
    public void initPreference_helpPreferenceInvisible() {
        mFragment.initPreference();

        verify(mHelpPreference).setVisible(false);
    }

    @Test
    public void restartBatteryTipLoader() {
        // TODO: add policy logic here when BatteryTipPolicy is implemented
        doReturn(mBatteryTipLoader)
                .when(mLoaderManager)
                .getLoader(PowerUsageBase.LoaderIndex.BATTERY_TIP_LOADER);
        doReturn(false).when(mBatteryTipLoader).isReset();

        mFragment.restartBatteryTipLoader();

        verify(mLoaderManager)
                .restartLoader(
                        eq(PowerUsageBase.LoaderIndex.BATTERY_TIP_LOADER), eq(Bundle.EMPTY), any());
    }

    @Test
    public void restartBatteryTipLoader_nullLoader_initLoader() {
        doReturn(null)
                .when(mLoaderManager)
                .getLoader(PowerUsageBase.LoaderIndex.BATTERY_TIP_LOADER);

        mFragment.restartBatteryTipLoader();

        verify(mLoaderManager)
                .initLoader(
                        eq(PowerUsageBase.LoaderIndex.BATTERY_TIP_LOADER), eq(Bundle.EMPTY), any());
    }

    @Test
    public void restartBatteryTipLoader_loaderReset_initLoader() {
        doReturn(mBatteryTipLoader)
                .when(mLoaderManager)
                .getLoader(PowerUsageBase.LoaderIndex.BATTERY_TIP_LOADER);
        doReturn(true).when(mBatteryTipLoader).isReset();

        mFragment.restartBatteryTipLoader();

        verify(mLoaderManager)
                .initLoader(
                        eq(PowerUsageBase.LoaderIndex.BATTERY_TIP_LOADER), eq(Bundle.EMPTY), any());
    }

    @Test
    public void refreshUi_contextNull_doNothing() {
        doReturn(null).when(mFragment).getContext();

        mFragment.refreshUi(BatteryBroadcastReceiver.BatteryUpdateType.MANUAL);

        verify(mFragment, never()).restartBatteryTipLoader();
        verify(mFragment, never()).restartBatteryInfoLoader();
    }

    @Test
    public void refreshUi_batteryNotPresent_doNothing() {
        mFragment.setIsBatteryPresent(false);
        mFragment.refreshUi(BatteryBroadcastReceiver.BatteryUpdateType.MANUAL);

        verify(mFragment, never()).restartBatteryTipLoader();
        verify(mFragment, never()).restartBatteryInfoLoader();
    }

    @Test
    public void refreshUi_deviceRotate_doNotUpdateBatteryTip() {
        mFragment.mBatteryTipPreferenceController = mock(BatteryTipPreferenceController.class);
        when(mFragment.mBatteryTipPreferenceController.needUpdate()).thenReturn(false);
        mFragment.updateBatteryTipFlag(new Bundle());
        doNothing().when(mFragment).restartBatteryInfoLoader();

        mFragment.refreshUi(BatteryBroadcastReceiver.BatteryUpdateType.MANUAL);

        verify(mFragment, never()).restartBatteryTipLoader();
        verify(mFragment).restartBatteryInfoLoader();
    }

    @Test
    public void refreshUi_batteryLevelChanged_doNotUpdateBatteryTip() {
        mFragment.mBatteryTipPreferenceController = mock(BatteryTipPreferenceController.class);
        when(mFragment.mBatteryTipPreferenceController.needUpdate()).thenReturn(true);
        mFragment.updateBatteryTipFlag(new Bundle());
        doNothing().when(mFragment).restartBatteryInfoLoader();

        mFragment.refreshUi(BatteryBroadcastReceiver.BatteryUpdateType.BATTERY_LEVEL);

        verify(mFragment, never()).restartBatteryTipLoader();
        verify(mFragment).restartBatteryInfoLoader();
    }

    @Test
    public void refreshUi_tipNeedUpdate_updateBatteryTip() {
        mFragment.mBatteryTipPreferenceController = mock(BatteryTipPreferenceController.class);
        when(mFragment.mBatteryTipPreferenceController.needUpdate()).thenReturn(true);
        mFragment.updateBatteryTipFlag(new Bundle());
        doNothing().when(mFragment).restartBatteryInfoLoader();
        doNothing().when(mFragment).restartBatteryTipLoader();

        mFragment.refreshUi(BatteryBroadcastReceiver.BatteryUpdateType.MANUAL);

        verify(mFragment).restartBatteryTipLoader();
        verify(mFragment).restartBatteryInfoLoader();
    }

    @Test
    public void onResume_registerContentObserver() {
        mFragment.onResume();

        verify(mContentResolver)
                .registerContentObserver(
                        Settings.Global.getUriFor(
                                Settings.Global.BATTERY_ESTIMATES_LAST_UPDATE_TIME),
                        false,
                        mFragment.mSettingsObserver);
    }

    @Test
    public void onPause_unregisterContentObserver() {
        mFragment.onPause();

        verify(mContentResolver).unregisterContentObserver(mFragment.mSettingsObserver);
    }

    @Test
    public void restartBatteryInfoLoader_contextNull_doNothing() {
        when(mFragment.getContext()).thenReturn(null);

        mFragment.restartBatteryInfoLoader();

        verify(mLoaderManager, never())
                .restartLoader(
                        PowerUsageBase.LoaderIndex.BATTERY_INFO_LOADER,
                        Bundle.EMPTY,
                        mFragment.mBatteryInfoLoaderCallbacks);
    }

    @Test
    public void restartBatteryInfoLoader_batteryIsNotPresent_doNothing() {
        mFragment.setIsBatteryPresent(false);

        mFragment.restartBatteryInfoLoader();

        verify(mLoaderManager, never())
                .restartLoader(
                        PowerUsageBase.LoaderIndex.BATTERY_INFO_LOADER,
                        Bundle.EMPTY,
                        mFragment.mBatteryInfoLoaderCallbacks);
    }

    @Test
    public void restartBatteryInfoLoader() {
        doReturn(mBatteryInfoLoader)
                .when(mLoaderManager)
                .getLoader(PowerUsageBase.LoaderIndex.BATTERY_INFO_LOADER);
        doReturn(false).when(mBatteryTipLoader).isReset();

        mFragment.restartBatteryInfoLoader();

        verify(mLoaderManager)
                .restartLoader(
                        eq(PowerUsageBase.LoaderIndex.BATTERY_INFO_LOADER),
                        eq(Bundle.EMPTY),
                        any());
    }

    @Test
    public void restartBatteryInfoLoader_nullLoader_initLoader() {
        doReturn(null)
                .when(mLoaderManager)
                .getLoader(PowerUsageBase.LoaderIndex.BATTERY_INFO_LOADER);

        mFragment.restartBatteryInfoLoader();

        verify(mLoaderManager)
                .initLoader(
                        eq(PowerUsageBase.LoaderIndex.BATTERY_INFO_LOADER),
                        eq(Bundle.EMPTY),
                        any());
    }

    @Test
    public void restartBatteryInfoLoader_loaderReset_initLoader() {
        mFragment.setIsBatteryPresent(true);
        doReturn(mBatteryInfoLoader)
                .when(mLoaderManager)
                .getLoader(PowerUsageBase.LoaderIndex.BATTERY_INFO_LOADER);
        doReturn(true).when(mBatteryInfoLoader).isReset();

        mFragment.restartBatteryInfoLoader();

        verify(mLoaderManager)
                .initLoader(
                        eq(PowerUsageBase.LoaderIndex.BATTERY_INFO_LOADER),
                        eq(Bundle.EMPTY),
                        any());
    }

    private static class TestFragment extends PowerUsageSummary {
        private Context mContext;
        private LoaderManager mLoaderManager;

        TestFragment(Context context, LoaderManager loaderManager) {
            mContext = context;
            mLoaderManager = loaderManager;
        }

        @Override
        public Context getContext() {
            return mContext;
        }

        @Override
        protected ContentResolver getContentResolver() {
            // Override it so we can access this method in test
            return super.getContentResolver();
        }

        public void setIsBatteryPresent(boolean isBatteryPresent) {
            mIsBatteryPresent = isBatteryPresent;
        }

        @Override
        protected LoaderManager getLoaderManagerForCurrentFragment() {
            return mLoaderManager;
        }
    }
}
