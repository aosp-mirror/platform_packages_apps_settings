/*
 * Copyright (C) 2017 The Android Open Source Project
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

package com.android.settings.applications.specialaccess.premiumsms;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

import android.app.settings.SettingsEnums;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.os.Process;
import android.telephony.SmsManager;
import android.view.LayoutInflater;
import android.view.View;

import androidx.preference.PreferenceManager;
import androidx.preference.PreferenceScreen;
import androidx.preference.PreferenceViewHolder;
import androidx.preference.R;

import com.android.internal.logging.nano.MetricsProto;
import com.android.settings.datausage.AppStateDataUsageBridge;
import com.android.settings.testutils.FakeFeatureFactory;
import com.android.settings.testutils.shadow.ShadowRestrictedLockUtilsInternal;
import com.android.settingslib.RestrictedDropDownPreference;
import com.android.settingslib.applications.ApplicationsState;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import org.robolectric.util.ReflectionHelpers;

import java.util.ArrayList;

@RunWith(RobolectricTestRunner.class)
@Config(shadows = {
        ShadowRestrictedLockUtilsInternal.class
})
public class PremiumSmsAccessTest {

    private FakeFeatureFactory mFeatureFactory;
    private PremiumSmsAccess mFragment;
    private Context mContext;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mFeatureFactory = FakeFeatureFactory.setupForTest();
        mFragment = new PremiumSmsAccess();
        mContext = RuntimeEnvironment.application;
        mFragment.onAttach(mContext);
    }

    @Test
    public void logSpecialPermissionChange() {
        mFragment.logSpecialPermissionChange(SmsManager.PREMIUM_SMS_CONSENT_ASK_USER,
                "app");
        verify(mFeatureFactory.metricsFeatureProvider).action(
                SettingsEnums.PAGE_UNKNOWN,
                MetricsProto.MetricsEvent.APP_SPECIAL_PERMISSION_PREMIUM_SMS_ASK,
                mFragment.getMetricsCategory(),
                "app",
                SmsManager.PREMIUM_SMS_CONSENT_ASK_USER);

        mFragment.logSpecialPermissionChange(SmsManager.PREMIUM_SMS_CONSENT_NEVER_ALLOW,
                "app");
        verify(mFeatureFactory.metricsFeatureProvider).action(
                SettingsEnums.PAGE_UNKNOWN,
                MetricsProto.MetricsEvent.APP_SPECIAL_PERMISSION_PREMIUM_SMS_DENY,
                mFragment.getMetricsCategory(),
                "app",
                SmsManager.PREMIUM_SMS_CONSENT_NEVER_ALLOW);

        mFragment.logSpecialPermissionChange(SmsManager.PREMIUM_SMS_CONSENT_ALWAYS_ALLOW,
                "app");
        verify(mFeatureFactory.metricsFeatureProvider).action(
                SettingsEnums.PAGE_UNKNOWN,
                MetricsProto.MetricsEvent.APP_SPECIAL_PERMISSION_PREMIUM_SMS_ALWAYS_ALLOW,
                mFragment.getMetricsCategory(),
                "app",
                SmsManager.PREMIUM_SMS_CONSENT_ALWAYS_ALLOW);
    }

    @Test
    public void onRebuildComplete_ecmRestricted_shouldBeDisabled() {
        mFragment = spy(mFragment);
        mContext = spy(mContext);
        LayoutInflater inflater = LayoutInflater.from(mContext);
        View view = inflater.inflate(R.layout.preference_dropdown, null);
        PreferenceViewHolder holder = PreferenceViewHolder.createInstanceForTests(view);

        PreferenceManager preferenceManager = new PreferenceManager(mContext);
        PreferenceScreen preferenceScreen = spy(preferenceManager.createPreferenceScreen(mContext));
        doReturn(preferenceManager).when(mFragment).getPreferenceManager();
        doReturn(preferenceScreen).when(mFragment).getPreferenceScreen();
        final String testPkg = "com.example.disabled";
        doNothing().when(mContext).startActivity(any());
        ShadowRestrictedLockUtilsInternal.setEcmRestrictedPkgs(testPkg);

        doAnswer((invocation) -> {
            final RestrictedDropDownPreference preference = invocation.getArgument(0);
            // Verify preference is disabled by ecm and the summary is changed accordingly.
            assertThat(preference.isDisabledByEcm()).isTrue();
            assertThat(preference.getSummary().toString()).isEqualTo(
                    mContext.getString(
                            com.android.settingslib.R.string.disabled_by_app_ops_text));
            preference.onBindViewHolder(holder);
            preference.performClick();
            // Verify that when the preference is clicked, ecm details intent is launched
            verify(mContext).startActivity(any());

            return null;
        }).when(preferenceScreen).addPreference(any(RestrictedDropDownPreference.class));

        mFragment.onRebuildComplete(createAppEntries(testPkg));
        verify(preferenceScreen).addPreference(any(RestrictedDropDownPreference.class));
    }

    @Test
    public void onRebuildComplete_ecmNotRestricted_notDisabled() {
        mFragment = spy(mFragment);
        mContext = spy(mContext);
        LayoutInflater inflater = LayoutInflater.from(mContext);
        View view = inflater.inflate(R.layout.preference_dropdown, null);
        PreferenceViewHolder holder = PreferenceViewHolder.createInstanceForTests(view);

        PreferenceManager preferenceManager = new PreferenceManager(mContext);
        PreferenceScreen preferenceScreen = spy(preferenceManager.createPreferenceScreen(mContext));
        doReturn(preferenceManager).when(mFragment).getPreferenceManager();
        doReturn(preferenceScreen).when(mFragment).getPreferenceScreen();
        final String testPkg = "com.example.enabled";
        ShadowRestrictedLockUtilsInternal.setEcmRestrictedPkgs();


        doAnswer((invocation) -> {
            final RestrictedDropDownPreference preference = invocation.getArgument(0);
            assertThat(preference.isDisabledByEcm()).isFalse();
            assertThat(preference.getSummary().toString()).isEqualTo("");
            preference.onBindViewHolder(holder);
            preference.performClick();
            // Verify that when the preference is clicked, ecm details intent is not launched
            verify(mContext, never()).startActivity(any());

            return null;
        }).when(preferenceScreen).addPreference(any(RestrictedDropDownPreference.class));

        mFragment.onRebuildComplete(createAppEntries(testPkg));
        verify(preferenceScreen).addPreference(any(RestrictedDropDownPreference.class));
    }

    private ArrayList<ApplicationsState.AppEntry> createAppEntries(String... packageNames) {
        final ArrayList<ApplicationsState.AppEntry> appEntries = new ArrayList<>();
        for (int i = 0; i < packageNames.length; ++i) {
            final ApplicationInfo info = new ApplicationInfo();
            info.packageName = packageNames[i];
            info.uid = Process.FIRST_APPLICATION_UID + i;
            info.sourceDir = info.packageName;
            final ApplicationsState.AppEntry appEntry =
                    spy(new ApplicationsState.AppEntry(mContext, info, i));
            appEntry.extraInfo = new AppStateDataUsageBridge
                    .DataUsageState(false, false);
            doNothing().when(appEntry).ensureLabel(any(Context.class));
            ReflectionHelpers.setField(appEntry, "info", info);
            appEntries.add(appEntry);
        }
        return appEntries;
    }
}
