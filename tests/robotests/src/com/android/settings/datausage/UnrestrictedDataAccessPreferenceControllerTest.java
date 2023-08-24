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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.os.Process;

import androidx.preference.PreferenceManager;
import androidx.preference.PreferenceScreen;

import com.android.internal.logging.nano.MetricsProto;
import com.android.settings.applications.AppStateBaseBridge;
import com.android.settings.datausage.AppStateDataUsageBridge.DataUsageState;
import com.android.settings.testutils.FakeFeatureFactory;
import com.android.settings.testutils.shadow.ShadowRestrictedLockUtils;
import com.android.settings.testutils.shadow.ShadowRestrictedLockUtilsInternal;
import com.android.settingslib.applications.ApplicationsState;
import com.android.settingslib.applications.ApplicationsState.AppEntry;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.Implements;
import org.robolectric.util.ReflectionHelpers;

import java.util.ArrayList;

@RunWith(RobolectricTestRunner.class)
@Config(shadows = {
        ShadowRestrictedLockUtils.class,
        ShadowRestrictedLockUtilsInternal.class,
        UnrestrictedDataAccessPreferenceControllerTest.ShadowAppStateBaseBridge.class
})
public class UnrestrictedDataAccessPreferenceControllerTest {
    @Mock
    private ApplicationsState mState;
    @Mock
    private ApplicationsState.Session mSession;

    private Context mContext;
    private FakeFeatureFactory mFeatureFactory;
    private PreferenceManager mPreferenceManager;
    private PreferenceScreen mPreferenceScreen;
    private UnrestrictedDataAccess mFragment;
    private UnrestrictedDataAccessPreferenceController mController;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mContext = RuntimeEnvironment.application;
        mFeatureFactory = FakeFeatureFactory.setupForTest();
        ReflectionHelpers.setStaticField(ApplicationsState.class, "sInstance", mState);
        when(mState.newSession(any())).thenReturn(mSession);
        mController = spy(new UnrestrictedDataAccessPreferenceController(mContext, "pref_key"));
    }

    @Test
    public void shouldAddPreference_forApps_shouldBeTrue() {
        final int uid = Process.FIRST_APPLICATION_UID + 10;
        final AppEntry entry = createAppEntry(uid);
        assertThat(UnrestrictedDataAccessPreferenceController.shouldAddPreference(entry)).isTrue();
    }

    @Test
    public void shouldAddPreference_forNonApps_shouldBeFalse() {
        final int uid = Process.FIRST_APPLICATION_UID - 10;
        final AppEntry entry = createAppEntry(uid);
        assertThat(UnrestrictedDataAccessPreferenceController.shouldAddPreference(entry)).isFalse();
    }

    @Test
    public void logSpecialPermissionChange() {
        mController.logSpecialPermissionChange(true, "app");
        verify(mFeatureFactory.metricsFeatureProvider).action(nullable(Context.class),
                eq(MetricsProto.MetricsEvent.APP_SPECIAL_PERMISSION_UNL_DATA_ALLOW), eq("app"));

        mController.logSpecialPermissionChange(false, "app");
        verify(mFeatureFactory.metricsFeatureProvider).action(nullable(Context.class),
                eq(MetricsProto.MetricsEvent.APP_SPECIAL_PERMISSION_UNL_DATA_DENY), eq("app"));
    }

    @Test
    public void onRebuildComplete_restricted_shouldBeDisabled() {
        mFragment = spy(new UnrestrictedDataAccess());
        doNothing().when(mFragment).setLoading(anyBoolean(), anyBoolean());
        mController.setParentFragment(mFragment);
        mPreferenceManager = new PreferenceManager(mContext);
        mPreferenceScreen = spy(mPreferenceManager.createPreferenceScreen(mContext));
        doReturn(mPreferenceManager).when(mFragment).getPreferenceManager();
        doReturn(mPreferenceScreen).when(mFragment).getPreferenceScreen();
        doReturn(0).when(mPreferenceScreen).getPreferenceCount();
        final DataSaverBackend dataSaverBackend = mock(DataSaverBackend.class);
        ReflectionHelpers.setField(mController, "mDataSaverBackend", dataSaverBackend);
        ReflectionHelpers.setField(mController, "mScreen", mPreferenceScreen);

        final String testPkg1 = "com.example.one";
        final String testPkg2 = "com.example.two";
        ShadowRestrictedLockUtilsInternal.setRestrictedPkgs(testPkg2);

        doAnswer((invocation) -> {
            final UnrestrictedDataAccessPreference preference = invocation.getArgument(0);
            final AppEntry entry = preference.getEntry();
            // Verify preference is disabled by admin and the summary is changed accordingly.
            if (testPkg1.equals(entry.info.packageName)) {
                assertThat(preference.isDisabledByAdmin()).isFalse();
                assertThat(preference.getSummary()).isEqualTo("");
            } else if (testPkg2.equals(entry.info.packageName)) {
                assertThat(preference.isDisabledByAdmin()).isTrue();
                assertThat(preference.getSummary()).isEqualTo(
                        mContext.getString(
                                com.android.settingslib.widget.restricted.R.string.disabled_by_admin));
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
        }).when(mPreferenceScreen).addPreference(any(UnrestrictedDataAccessPreference.class));

        mController.onRebuildComplete(createAppEntries(testPkg1, testPkg2));
    }

    private ArrayList<AppEntry> createAppEntries(String... packageNames) {
        final ArrayList<AppEntry> appEntries = new ArrayList<>();
        for (int i = 0; i < packageNames.length; ++i) {
            final ApplicationInfo info = new ApplicationInfo();
            info.packageName = packageNames[i];
            info.uid = Process.FIRST_APPLICATION_UID + i;
            info.sourceDir = info.packageName;
            final AppEntry appEntry = spy(new AppEntry(mContext, info, i));
            appEntry.extraInfo = new DataUsageState(false, false);
            doNothing().when(appEntry).ensureLabel(any(Context.class));
            ReflectionHelpers.setField(appEntry, "info", info);
            appEntries.add(appEntry);
        }
        return appEntries;
    }

    private AppEntry createAppEntry(int uid) {
        final ApplicationInfo info = new ApplicationInfo();
        info.packageName = "com.example.three";
        info.uid = uid;
        info.sourceDir = info.packageName;
        return new AppEntry(mContext, info, uid);
    }

    @Implements(AppStateBaseBridge.class)
    public static class ShadowAppStateBaseBridge {

        public void __constructor__(ApplicationsState appState,
                AppStateBaseBridge.Callback callback) {
        }
    }
}
