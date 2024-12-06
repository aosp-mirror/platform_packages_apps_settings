/*
 * Copyright (C) 2023 The Android Open Source Project
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

import static com.android.settings.SettingsActivity.EXTRA_SHOW_FRAGMENT_ARGUMENTS;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import android.app.AppOpsManager;
import android.app.settings.SettingsEnums;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.InstallSourceInfo;
import android.content.pm.PackageManager;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.UserHandle;

import androidx.fragment.app.FragmentActivity;
import androidx.loader.app.LoaderManager;
import androidx.test.core.app.ApplicationProvider;

import com.android.settings.R;
import com.android.settings.SettingsActivity;
import com.android.settings.fuelgauge.batteryusage.BatteryEntry;
import com.android.settings.testutils.FakeFeatureFactory;
import com.android.settings.testutils.shadow.ShadowHelpUtils;
import com.android.settingslib.Utils;
import com.android.settingslib.applications.ApplicationsState;
import com.android.settingslib.core.instrumentation.MetricsFeatureProvider;
import com.android.settingslib.widget.FooterPreference;
import com.android.settingslib.widget.IntroPreference;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Answers;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import org.mockito.stubbing.Answer;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.Implementation;
import org.robolectric.annotation.Implements;

import java.util.concurrent.TimeUnit;

@RunWith(RobolectricTestRunner.class)
@Config(
        shadows = {
            ShadowHelpUtils.class,
            com.android.settings.testutils.shadow.ShadowFragment.class,
        })
public class PowerBackgroundUsageDetailTest {

    @Rule public final MockitoRule mMockitoRule = MockitoJUnit.rule();

    private static final String APP_LABEL = "app label";
    private static final String APP_ENTRY_LABEL = "app entry label";
    private static final String SUMMARY = "summary";
    private static final int ICON_ID = 123;
    private static final int UID = 1;
    private static final String PACKAGE_NAME = "com.android.app";
    private static final String KEY_PREF_HEADER = "header_view";
    private static final String KEY_FOOTER_PREFERENCE = "app_usage_footer_preference";
    private static final String INITIATING_PACKAGE_NAME = "com.android.vending";
    private static final Drawable TEST_DRAWABLE = new ColorDrawable(0);

    private int mTestMode;
    private Context mContext;
    private PowerBackgroundUsageDetail mFragment;
    private MetricsFeatureProvider mMetricsFeatureProvider;
    private SettingsActivity mTestActivity;
    private BatteryOptimizeUtils mBatteryOptimizeUtils;
    private IntroPreference mIntroPreference;

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private FragmentActivity mActivity;

    @Mock private ApplicationsState mState;
    @Mock private Bundle mBundle;
    @Mock private LoaderManager mLoaderManager;
    @Mock private ApplicationsState.AppEntry mAppEntry;
    @Mock private BatteryEntry mBatteryEntry;
    @Mock private PackageManager mPackageManager;
    @Mock private AppOpsManager mAppOpsManager;
    @Mock private InstallSourceInfo mInstallSourceInfo;
    @Mock private FooterPreference mFooterPreference;

    @Implements(Utils.class)
    private static class ShadowUtils {
        @Implementation
        public static Drawable getBadgedIcon(Context context, ApplicationInfo appInfo) {
            return PowerBackgroundUsageDetailTest.TEST_DRAWABLE;
        }
    }

    @Before
    public void setUp() throws Exception {
        mContext = spy(ApplicationProvider.getApplicationContext());
        when(mContext.getPackageName()).thenReturn(PACKAGE_NAME);
        when(mContext.getPackageManager()).thenReturn(mPackageManager);
        when(mPackageManager.getInstallSourceInfo(anyString())).thenReturn(mInstallSourceInfo);

        final FakeFeatureFactory fakeFeatureFactory = FakeFeatureFactory.setupForTest();
        mMetricsFeatureProvider = fakeFeatureFactory.metricsFeatureProvider;

        prepareTestBatteryOptimizationUtils();
        mFragment = spy(new PowerBackgroundUsageDetail());
        mFragment.mBatteryOptimizeUtils = mBatteryOptimizeUtils;
        mFragment.mLogStringBuilder = new StringBuilder();
        mIntroPreference = new IntroPreference(mContext);
        doReturn(mIntroPreference).when(mFragment).findPreference(KEY_PREF_HEADER);
        doReturn(mFooterPreference).when(mFragment).findPreference(KEY_FOOTER_PREFERENCE);
        doReturn(mContext).when(mFragment).getContext();
        doReturn(mActivity).when(mFragment).getActivity();
        doReturn(SUMMARY).when(mFragment).getString(anyInt());
        doReturn(APP_LABEL).when(mBundle).getString(nullable(String.class));
        when(mFragment.getArguments()).thenReturn(mBundle);
        doReturn(mLoaderManager).when(mFragment).getLoaderManager();

        when(mBatteryEntry.getUid()).thenReturn(UID);
        when(mBatteryEntry.getLabel()).thenReturn(APP_LABEL);
        mBatteryEntry.mIconId = ICON_ID;

        mFragment.mState = mState;
        doNothing().when(mState).ensureIcon(mAppEntry);
        mAppEntry.info = mock(ApplicationInfo.class);
        mAppEntry.label = APP_ENTRY_LABEL;

        mTestActivity = spy(new SettingsActivity());
        doReturn(mPackageManager).when(mTestActivity).getPackageManager();
        doReturn(mPackageManager).when(mActivity).getPackageManager();
        doReturn(mAppOpsManager).when(mTestActivity).getSystemService(Context.APP_OPS_SERVICE);

        final ArgumentCaptor<Intent> captor = ArgumentCaptor.forClass(Intent.class);

        Answer<Void> callable =
                invocation -> {
                    mBundle = captor.getValue().getBundleExtra(EXTRA_SHOW_FRAGMENT_ARGUMENTS);
                    System.out.println("mBundle = " + mBundle);
                    return null;
                };
        doAnswer(callable)
                .when(mActivity)
                .startActivityAsUser(captor.capture(), nullable(UserHandle.class));
        doAnswer(callable).when(mActivity).startActivity(captor.capture());
    }

    @After
    public void reset() {
        ShadowHelpUtils.reset();
    }

    @Test
    @Config(shadows = ShadowUtils.class)
    public void initHeader_NoAppEntry_BuildByBundle() {
        mFragment.mAppEntry = null;
        mFragment.initHeader();

        assertThat(mIntroPreference.getIcon()).isNotEqualTo(TEST_DRAWABLE);
        assertThat(mIntroPreference.getTitle()).isEqualTo(APP_LABEL);
    }

    @Test
    @Config(shadows = ShadowUtils.class)
    public void initHeader_HasAppEntry_BuildByAppEntry() {
        mFragment.mAppEntry = mAppEntry;
        mFragment.initHeader();

        assertThat(mIntroPreference.getIcon()).isEqualTo(TEST_DRAWABLE);
        assertThat(mIntroPreference.getTitle()).isEqualTo(mAppEntry.label);
    }

    @Test
    @Config(shadows = ShadowUtils.class)
    public void initHeader_HasAppEntry_InstantApp() {
        mFragment.mAppEntry = mAppEntry;
        mFragment.initHeader();

        assertThat(mIntroPreference.getIcon()).isEqualTo(TEST_DRAWABLE);
        assertThat(mIntroPreference.getTitle()).isEqualTo(mAppEntry.label);
    }

    @Test
    public void initFooter_setExpectedFooterContent() {
        mFragment.initFooter();

        verify(mFooterPreference)
                .setTitle(mContext.getString(R.string.manager_battery_usage_footer));
        verify(mFooterPreference).setLearnMoreAction(any());
        verify(mFooterPreference)
                .setLearnMoreText(mContext.getString(R.string.manager_battery_usage_link_a11y));
    }

    @Test
    public void onPause_optimizationModeIsChanged_logPreference() throws Exception {
        mFragment.mOptimizationMode = BatteryOptimizeUtils.MODE_OPTIMIZED;
        when(mBatteryOptimizeUtils.getPackageName()).thenReturn(PACKAGE_NAME);
        when(mInstallSourceInfo.getInitiatingPackageName()).thenReturn(INITIATING_PACKAGE_NAME);

        mTestMode = BatteryOptimizeUtils.MODE_UNRESTRICTED;
        assertThat(mBatteryOptimizeUtils.getAppOptimizationMode())
                .isEqualTo(BatteryOptimizeUtils.MODE_UNRESTRICTED);
        mFragment.onPause();

        TimeUnit.SECONDS.sleep(1);
        verify(mMetricsFeatureProvider)
                .action(
                        SettingsEnums.LEAVE_POWER_USAGE_MANAGE_BACKGROUND,
                        SettingsEnums.ACTION_APP_BATTERY_USAGE_UNRESTRICTED,
                        SettingsEnums.FUELGAUGE_POWER_USAGE_MANAGE_BACKGROUND,
                        PACKAGE_NAME,
                        /* consumed battery */ 0);
    }

    @Test
    public void onPause_optimizationModeIsNotChanged_notInvokeLogging() throws Exception {
        mFragment.mOptimizationMode = BatteryOptimizeUtils.MODE_OPTIMIZED;
        when(mBatteryOptimizeUtils.getPackageName()).thenReturn(PACKAGE_NAME);
        when(mInstallSourceInfo.getInitiatingPackageName()).thenReturn(INITIATING_PACKAGE_NAME);

        mTestMode = BatteryOptimizeUtils.MODE_UNRESTRICTED;
        assertThat(mBatteryOptimizeUtils.getAppOptimizationMode())
                .isEqualTo(BatteryOptimizeUtils.MODE_UNRESTRICTED);
        mTestMode = BatteryOptimizeUtils.MODE_OPTIMIZED;
        assertThat(mBatteryOptimizeUtils.getAppOptimizationMode())
                .isEqualTo(BatteryOptimizeUtils.MODE_OPTIMIZED);
        mFragment.onPause();

        TimeUnit.SECONDS.sleep(1);
        verifyNoInteractions(mMetricsFeatureProvider);
    }

    private void prepareTestBatteryOptimizationUtils() {
        mBatteryOptimizeUtils = spy(new BatteryOptimizeUtils(mContext, UID, PACKAGE_NAME));
        Answer<Void> setTestMode =
                invocation -> {
                    mTestMode = invocation.getArgument(0);
                    return null;
                };
        doAnswer(setTestMode).when(mBatteryOptimizeUtils).setAppUsageState(anyInt(), any());
        Answer<Integer> getTestMode = invocation -> mTestMode;
        doAnswer(getTestMode).when(mBatteryOptimizeUtils).getAppOptimizationMode();
    }
}
