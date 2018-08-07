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
 * limitations under the License
 */
package com.android.settings.datausage;

import static com.google.common.truth.Truth.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.os.Process;
import android.support.v7.preference.PreferenceManager;
import android.support.v7.preference.PreferenceScreen;

import com.android.internal.logging.nano.MetricsProto;
import com.android.settings.R;
import com.android.settings.datausage.AppStateDataUsageBridge.DataUsageState;
import com.android.settings.datausage.UnrestrictedDataAccess.AccessPreference;
import com.android.settings.testutils.FakeFeatureFactory;
import com.android.settings.testutils.SettingsRobolectricTestRunner;
import com.android.settings.testutils.shadow.ShadowRestrictedLockUtils;
import com.android.settingslib.applications.ApplicationsState.AppEntry;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import org.robolectric.util.ReflectionHelpers;

import java.util.ArrayList;

@RunWith(SettingsRobolectricTestRunner.class)
@Config(shadows = ShadowRestrictedLockUtils.class)
public class UnrestrictedDataAccessTest {

    @Mock
    private AppEntry mAppEntry;
    private UnrestrictedDataAccess mFragment;
    private FakeFeatureFactory mFeatureFactory;
    @Mock
    private PreferenceScreen mPreferenceScreen;
    @Mock
    private PreferenceManager mPreferenceManager;
    @Mock
    private DataSaverBackend mDataSaverBackend;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mFeatureFactory = FakeFeatureFactory.setupForTest();
        mFragment = spy(new UnrestrictedDataAccess());
    }

    @Test
    public void testShouldAddPreferenceForApps() {
        mAppEntry.info = new ApplicationInfo();
        mAppEntry.info.uid = Process.FIRST_APPLICATION_UID + 10;

        assertThat(mFragment.shouldAddPreference(mAppEntry)).isTrue();
    }

    @Test
    public void testShouldNotAddPreferenceForNonApps() {
        mAppEntry.info = new ApplicationInfo();
        mAppEntry.info.uid = Process.FIRST_APPLICATION_UID - 10;

        assertThat(mFragment.shouldAddPreference(mAppEntry)).isFalse();
    }

    @Test
    public void logSpecialPermissionChange() {
        mFragment.logSpecialPermissionChange(true, "app");
        verify(mFeatureFactory.metricsFeatureProvider).action(nullable(Context.class),
                eq(MetricsProto.MetricsEvent.APP_SPECIAL_PERMISSION_UNL_DATA_ALLOW), eq("app"));

        mFragment.logSpecialPermissionChange(false, "app");
        verify(mFeatureFactory.metricsFeatureProvider).action(nullable(Context.class),
                eq(MetricsProto.MetricsEvent.APP_SPECIAL_PERMISSION_UNL_DATA_DENY), eq("app"));
    }

    @Test
    public void testOnRebuildComplete_restricted_shouldBeDisabled() {
        final Context context = RuntimeEnvironment.application;
        doReturn(context).when(mFragment).getContext();
        when(mPreferenceManager.getContext()).thenReturn(context);
        doReturn(true).when(mFragment).shouldAddPreference(any(AppEntry.class));
        doNothing().when(mFragment).setLoading(anyBoolean(), anyBoolean());
        doReturn(mPreferenceScreen).when(mFragment).getPreferenceScreen();
        when(mFragment.getPreferenceManager()).thenReturn(mPreferenceManager);
        ReflectionHelpers.setField(mFragment, "mDataSaverBackend", mDataSaverBackend);

        final String testPkg1 = "com.example.one";
        final String testPkg2 = "com.example.two";
        ShadowRestrictedLockUtils.setRestrictedPkgs(testPkg2);

        doAnswer((invocation) -> {
            final AccessPreference preference = invocation.getArgument(0);
            final AppEntry entry = preference.getEntryForTest();
            // Verify preference is disabled by admin and the summary is changed accordingly.
            if (testPkg1.equals(entry.info.packageName)) {
                assertThat(preference.isDisabledByAdmin()).isFalse();
                assertThat(preference.getSummary()).isEqualTo("");
            } else if (testPkg2.equals(entry.info.packageName)) {
                assertThat(preference.isDisabledByAdmin()).isTrue();
                assertThat(preference.getSummary()).isEqualTo(
                        context.getString(R.string.disabled_by_admin));
            }
            assertThat(preference.isChecked()).isFalse();
            preference.performClick();
            // Verify that when the preference is clicked, support details intent is launched
            // if the preference is disabled by admin, otherwise the switch is toggled.
            if (testPkg1.equals(entry.info.packageName)) {
                assertThat(preference.isChecked()).isTrue();
                assertThat(ShadowRestrictedLockUtils.hasAdminSupportDetailsIntentLaunched())
                        .isFalse();
            } else if (testPkg2.equals(entry.info.packageName)) {
                assertThat(preference.isChecked()).isFalse();
                assertThat(ShadowRestrictedLockUtils.hasAdminSupportDetailsIntentLaunched())
                        .isTrue();
            }
            ShadowRestrictedLockUtils.clearAdminSupportDetailsIntentLaunch();
            return null;
        }).when(mPreferenceScreen).addPreference(any(AccessPreference.class));
        mFragment.onRebuildComplete(createAppEntries(testPkg1, testPkg2));
    }

    private ArrayList<AppEntry> createAppEntries(String... packageNames) {
        final ArrayList<AppEntry> appEntries = new ArrayList<>();
        for (int i = 0; i < packageNames.length; ++i) {
            final ApplicationInfo info = new ApplicationInfo();
            info.packageName = packageNames[i];
            info.uid = Process.FIRST_APPLICATION_UID + i;
            info.sourceDir = info.packageName;
            final AppEntry appEntry = spy(new AppEntry(RuntimeEnvironment.application, info, i));
            appEntry.extraInfo = new DataUsageState(false, false);
            doNothing().when(appEntry).ensureLabel(any(Context.class));
            ReflectionHelpers.setField(appEntry, "info", info);
            appEntries.add(appEntry);
        }
        return appEntries;
    }
}
