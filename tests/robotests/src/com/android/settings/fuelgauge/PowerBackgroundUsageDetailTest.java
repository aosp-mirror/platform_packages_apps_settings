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
import static com.android.settings.fuelgauge.BatteryOptimizeHistoricalLogEntry.Action;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.doAnswer;
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
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.UserHandle;
import android.widget.CompoundButton;

import androidx.fragment.app.FragmentActivity;
import androidx.loader.app.LoaderManager;
import androidx.test.core.app.ApplicationProvider;

import com.android.settings.SettingsActivity;
import com.android.settings.fuelgauge.batteryusage.BatteryEntry;
import com.android.settings.testutils.FakeFeatureFactory;
import com.android.settings.testutils.shadow.ShadowEntityHeaderController;
import com.android.settings.widget.EntityHeaderController;
import com.android.settingslib.applications.AppUtils;
import com.android.settingslib.applications.ApplicationsState;
import com.android.settingslib.applications.instantapps.InstantAppDataProvider;
import com.android.settingslib.core.instrumentation.MetricsFeatureProvider;
import com.android.settingslib.widget.FooterPreference;
import com.android.settingslib.widget.LayoutPreference;
import com.android.settingslib.widget.MainSwitchPreference;
import com.android.settingslib.widget.SelectorWithWidgetPreference;

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
import org.robolectric.util.ReflectionHelpers;

import java.util.concurrent.TimeUnit;

@RunWith(RobolectricTestRunner.class)
@Config(
        shadows = {
            ShadowEntityHeaderController.class,
            com.android.settings.testutils.shadow.ShadowFragment.class,
        })
public class PowerBackgroundUsageDetailTest {

    @Rule
    public final MockitoRule mMockitoRule = MockitoJUnit.rule();

    private static final String APP_LABEL = "app label";
    private static final String SUMMARY = "summary";
    private static final int ICON_ID = 123;
    private static final int UID = 1;
    private static final String KEY_PREF_UNRESTRICTED = "unrestricted_preference";
    private static final String KEY_PREF_OPTIMIZED = "optimized_preference";
    private static final String KEY_ALLOW_BACKGROUND_USAGE = "allow_background_usage";

    private Context mContext;
    private PowerBackgroundUsageDetail mFragment;
    private FooterPreference mFooterPreference;
    private MainSwitchPreference mMainSwitchPreference;
    private MetricsFeatureProvider mMetricsFeatureProvider;
    private SelectorWithWidgetPreference mOptimizePreference;
    private SelectorWithWidgetPreference mUnrestrictedPreference;
    private SettingsActivity mTestActivity;

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private FragmentActivity mActivity;

    @Mock private EntityHeaderController mEntityHeaderController;
    @Mock private BatteryOptimizeUtils mBatteryOptimizeUtils;
    @Mock private LayoutPreference mHeaderPreference;
    @Mock private ApplicationsState mState;
    @Mock private Bundle mBundle;
    @Mock private LoaderManager mLoaderManager;
    @Mock private ApplicationsState.AppEntry mAppEntry;
    @Mock private BatteryEntry mBatteryEntry;
    @Mock private PackageManager mPackageManager;
    @Mock private AppOpsManager mAppOpsManager;
    @Mock private CompoundButton mMockSwitch;
    @Mock private InstallSourceInfo mInstallSourceInfo;

    @Before
    public void setUp() throws Exception {
        mContext = spy(ApplicationProvider.getApplicationContext());
        when(mContext.getPackageName()).thenReturn("foo");
        when(mContext.getPackageManager()).thenReturn(mPackageManager);
        when(mPackageManager.getInstallSourceInfo(anyString())).thenReturn(mInstallSourceInfo);

        final FakeFeatureFactory fakeFeatureFactory = FakeFeatureFactory.setupForTest();
        mMetricsFeatureProvider = fakeFeatureFactory.metricsFeatureProvider;

        mFragment = spy(new PowerBackgroundUsageDetail());
        mFragment.mLogStringBuilder = new StringBuilder();
        doReturn(mContext).when(mFragment).getContext();
        doReturn(mActivity).when(mFragment).getActivity();
        doReturn(SUMMARY).when(mFragment).getString(anyInt());
        doReturn(APP_LABEL).when(mBundle).getString(nullable(String.class));
        when(mFragment.getArguments()).thenReturn(mBundle);
        doReturn(mLoaderManager).when(mFragment).getLoaderManager();

        ShadowEntityHeaderController.setUseMock(mEntityHeaderController);
        doReturn(mEntityHeaderController)
                .when(mEntityHeaderController)
                .setButtonActions(anyInt(), anyInt());
        doReturn(mEntityHeaderController)
                .when(mEntityHeaderController)
                .setIcon(nullable(Drawable.class));
        doReturn(mEntityHeaderController)
                .when(mEntityHeaderController)
                .setIcon(nullable(ApplicationsState.AppEntry.class));
        doReturn(mEntityHeaderController)
                .when(mEntityHeaderController)
                .setLabel(nullable(String.class));
        doReturn(mEntityHeaderController)
                .when(mEntityHeaderController)
                .setLabel(nullable(String.class));
        doReturn(mEntityHeaderController)
                .when(mEntityHeaderController)
                .setLabel(nullable(ApplicationsState.AppEntry.class));
        doReturn(mEntityHeaderController)
                .when(mEntityHeaderController)
                .setSummary(nullable(String.class));

        when(mBatteryEntry.getUid()).thenReturn(UID);
        when(mBatteryEntry.getLabel()).thenReturn(APP_LABEL);
        mBatteryEntry.mIconId = ICON_ID;

        mFragment.mHeaderPreference = mHeaderPreference;
        mFragment.mState = mState;
        mFragment.mBatteryOptimizeUtils = mBatteryOptimizeUtils;
        mAppEntry.info = mock(ApplicationInfo.class);

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

        mFooterPreference = spy(new FooterPreference(mContext));
        mMainSwitchPreference = spy(new MainSwitchPreference(mContext));
        mMainSwitchPreference.setKey(KEY_ALLOW_BACKGROUND_USAGE);
        mOptimizePreference = spy(new SelectorWithWidgetPreference(mContext));
        mOptimizePreference.setKey(KEY_PREF_OPTIMIZED);
        mUnrestrictedPreference = spy(new SelectorWithWidgetPreference(mContext));
        mUnrestrictedPreference.setKey(KEY_PREF_UNRESTRICTED);
        mFragment.mFooterPreference = mFooterPreference;
        mFragment.mMainSwitchPreference = mMainSwitchPreference;
        mFragment.mOptimizePreference = mOptimizePreference;
        mFragment.mUnrestrictedPreference = mUnrestrictedPreference;
    }

    @After
    public void reset() {
        ShadowEntityHeaderController.reset();
    }

    @Test
    public void initHeader_NoAppEntry_BuildByBundle() {
        mFragment.mAppEntry = null;
        mFragment.initHeader();

        verify(mEntityHeaderController).setIcon(nullable(Drawable.class));
        verify(mEntityHeaderController).setLabel(APP_LABEL);
    }

    @Test
    public void initHeader_HasAppEntry_BuildByAppEntry() {
        ReflectionHelpers.setStaticField(
                AppUtils.class,
                "sInstantAppDataProvider",
                new InstantAppDataProvider() {
                    @Override
                    public boolean isInstantApp(ApplicationInfo info) {
                        return false;
                    }
                });
        mFragment.mAppEntry = mAppEntry;
        mFragment.initHeader();

        verify(mEntityHeaderController).setIcon(mAppEntry);
        verify(mEntityHeaderController).setLabel(mAppEntry);
        verify(mEntityHeaderController).setIsInstantApp(false);
    }

    @Test
    public void initHeader_HasAppEntry_InstantApp() {
        ReflectionHelpers.setStaticField(
                AppUtils.class,
                "sInstantAppDataProvider",
                new InstantAppDataProvider() {
                    @Override
                    public boolean isInstantApp(ApplicationInfo info) {
                        return true;
                    }
                });
        mFragment.mAppEntry = mAppEntry;
        mFragment.initHeader();

        verify(mEntityHeaderController).setIcon(mAppEntry);
        verify(mEntityHeaderController).setLabel(mAppEntry);
        verify(mEntityHeaderController).setIsInstantApp(true);
    }

    @Test
    public void initFooter_hasCorrectString() {
        when(mBatteryOptimizeUtils.isDisabledForOptimizeModeOnly()).thenReturn(false);
        when(mBatteryOptimizeUtils.isSystemOrDefaultApp()).thenReturn(false);

        mFragment.initFooter();

        assertThat(mFooterPreference.getTitle().toString())
                .isEqualTo("Changing how an app uses your battery can affect its performance.");
    }

    @Test
    public void onSwitchChanged_fromUnrestrictedModeSetDisabled_becomeRestrictedMode() {
        final int restrictedMode = BatteryOptimizeUtils.MODE_RESTRICTED;
        final int optimizedMode = BatteryOptimizeUtils.MODE_OPTIMIZED;
        mFragment.mOptimizationMode = optimizedMode;

        mFragment.onCheckedChanged(mMockSwitch, /* isChecked= */ false);

        verify(mOptimizePreference).setEnabled(false);
        verify(mUnrestrictedPreference).setEnabled(false);
        verify(mFragment).onRadioButtonClicked(null);
        verify(mMainSwitchPreference).setChecked(false);
        assertThat(mFragment.getSelectedPreference()).isEqualTo(restrictedMode);
        verify(mBatteryOptimizeUtils).setAppUsageState(restrictedMode, Action.APPLY);
    }

    @Test
    public void onSwitchChanged_fromRestrictedModeSetEnabled_becomeOptimizedMode() {
        final int restrictedMode = BatteryOptimizeUtils.MODE_RESTRICTED;
        final int optimizedMode = BatteryOptimizeUtils.MODE_OPTIMIZED;
        mFragment.mOptimizationMode = restrictedMode;

        mFragment.onCheckedChanged(mMockSwitch, /* isChecked= */ true);

        verify(mOptimizePreference).setEnabled(true);
        verify(mUnrestrictedPreference).setEnabled(true);
        verify(mFragment).onRadioButtonClicked(mOptimizePreference);
        verify(mMainSwitchPreference).setChecked(true);
        verify(mOptimizePreference).setChecked(true);
        assertThat(mFragment.getSelectedPreference()).isEqualTo(optimizedMode);
        verify(mBatteryOptimizeUtils).setAppUsageState(optimizedMode, Action.APPLY);
    }

    @Test
    public void onPause_optimizationModeChanged_logPreference() throws Exception {
        final String packageName = "testPackageName";
        final int restrictedMode = BatteryOptimizeUtils.MODE_RESTRICTED;
        final int optimizedMode = BatteryOptimizeUtils.MODE_OPTIMIZED;
        mFragment.mOptimizationMode = restrictedMode;
        when(mBatteryOptimizeUtils.getPackageName()).thenReturn(packageName);
        when(mInstallSourceInfo.getInitiatingPackageName()).thenReturn("com.android.vending");

        mFragment.onCheckedChanged(mMockSwitch, /* isChecked= */ true);
        verify(mBatteryOptimizeUtils).setAppUsageState(optimizedMode, Action.APPLY);
        when(mBatteryOptimizeUtils.getAppOptimizationMode()).thenReturn(optimizedMode);
        mFragment.onPause();

        TimeUnit.SECONDS.sleep(1);
        verify(mMetricsFeatureProvider)
                .action(
                        SettingsEnums.LEAVE_POWER_USAGE_MANAGE_BACKGROUND,
                        SettingsEnums.ACTION_APP_BATTERY_USAGE_OPTIMIZED,
                        SettingsEnums.FUELGAUGE_POWER_USAGE_MANAGE_BACKGROUND,
                        packageName,
                        /* consumed battery */ 0);
    }

    @Test
    public void onPause_optimizationModeIsNotChanged_notInvokeLogging() throws Exception {
        final String packageName = "testPackageName";
        final int restrictedMode = BatteryOptimizeUtils.MODE_RESTRICTED;
        final int optimizedMode = BatteryOptimizeUtils.MODE_OPTIMIZED;
        mFragment.mOptimizationMode = restrictedMode;
        when(mBatteryOptimizeUtils.getPackageName()).thenReturn(packageName);
        when(mInstallSourceInfo.getInitiatingPackageName()).thenReturn("com.android.vending");

        mFragment.onCheckedChanged(mMockSwitch, /* isChecked= */ true);
        verify(mBatteryOptimizeUtils).setAppUsageState(optimizedMode, Action.APPLY);
        when(mBatteryOptimizeUtils.getAppOptimizationMode()).thenReturn(optimizedMode);
        mFragment.onCheckedChanged(mMockSwitch, /* isChecked= */ false);
        verify(mBatteryOptimizeUtils).setAppUsageState(restrictedMode, Action.APPLY);
        when(mBatteryOptimizeUtils.getAppOptimizationMode()).thenReturn(restrictedMode);
        mFragment.onPause();

        TimeUnit.SECONDS.sleep(1);
        verifyNoInteractions(mMetricsFeatureProvider);
    }
}
