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

package com.android.settings.applications.appinfo;

import static com.android.settings.SettingsActivity.EXTRA_FRAGMENT_ARG_KEY;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.app.usage.IUsageStatsManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;

import androidx.fragment.app.FragmentActivity;
import androidx.preference.Preference;
import androidx.preference.PreferenceScreen;

import com.android.settings.notification.NotificationBackend;
import com.android.settings.notification.app.AppNotificationSettings;
import com.android.settingslib.applications.ApplicationsState;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import org.robolectric.util.ReflectionHelpers;

@RunWith(RobolectricTestRunner.class)
@Config(shadows = {
        com.android.settings.testutils.shadow.ShadowFragment.class,
})
public class AppNotificationPreferenceControllerTest {

    @Mock
    private AppInfoDashboardFragment mFragment;
    @Mock
    private PreferenceScreen mScreen;
    @Mock
    private Preference mPreference;

    private Context mContext;
    private AppNotificationPreferenceController mController;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mContext = RuntimeEnvironment.application;
        mController = spy(new AppNotificationPreferenceController(mContext, "test_key"));
        mController.setParentFragment(mFragment);
        when(mScreen.findPreference(mController.getPreferenceKey())).thenReturn(mPreference);
        final String key = mController.getPreferenceKey();
        when(mPreference.getKey()).thenReturn(key);
    }

    @Test
    public void getDetailFragmentClass_shouldReturnAppNotificationSettings() {
        assertThat(mController.getDetailFragmentClass()).isEqualTo(AppNotificationSettings.class);
    }

    @Test
    public void updateState_shouldSetSummary() {
        final ApplicationsState.AppEntry appEntry = mock(ApplicationsState.AppEntry.class);
        appEntry.info = new ApplicationInfo();
        when(mFragment.getAppEntry()).thenReturn(appEntry);
        NotificationBackend backend = new NotificationBackend();
        ReflectionHelpers.setField(backend, "sUsageStatsManager", mock(IUsageStatsManager.class));
        ReflectionHelpers.setField(mController, "mBackend", new NotificationBackend());
        mController.displayPreference(mScreen);

        mController.updateState(mPreference);

        verify(mPreference).setSummary(any());
    }

    @Test
    public void getArguments_nullIfChannelIsNull() {
        assertThat(mController.getArguments()).isNull();
    }

    @Test
    public void getArguments_containsChannelId() {
        FragmentActivity activity = mock(FragmentActivity.class);
        Intent intent = new Intent();
        intent.putExtra(EXTRA_FRAGMENT_ARG_KEY, "test");
        when(mFragment.getActivity()).thenReturn(activity);
        when(activity.getIntent()).thenReturn(intent);
        AppNotificationPreferenceController controller =
            new AppNotificationPreferenceController(mContext, "test");
        controller.setParentFragment(mFragment);

        assertThat(controller.getArguments().containsKey(EXTRA_FRAGMENT_ARG_KEY)).isTrue();
        assertThat(controller.getArguments().getString(EXTRA_FRAGMENT_ARG_KEY)).isEqualTo("test");
    }

    @Test
    public void getNotificationSummary_noCrashOnNull() {
        AppNotificationPreferenceController.getNotificationSummary(null, mContext);
    }

    @Test
    public void getNotificationSummary_appBlocked() {
        NotificationBackend.AppRow appRow = new NotificationBackend.AppRow();
        appRow.banned = true;
        appRow.blockedChannelCount = 30;
        assertThat(
            AppNotificationPreferenceController.getNotificationSummary(appRow, mContext).toString())
            .isEqualTo("Off");
    }

    @Test
    public void getNotificationSummary_appNotBlockedAllChannelsBlocked() {
        NotificationBackend.AppRow appRow = new NotificationBackend.AppRow();
        appRow.banned = false;
        appRow.blockedChannelCount = 30;
        appRow.channelCount = 30;
        assertThat(
            AppNotificationPreferenceController.getNotificationSummary(appRow, mContext).toString())
            .isEqualTo("Off");
    }

    @Test
    public void getNotificationSummary_appNotBlocked() {
        NotificationBackend.AppRow appRow = new NotificationBackend.AppRow();
        appRow.banned = false;
        appRow.blockedChannelCount = 30;
        appRow.channelCount = 60;
        appRow.sentByApp = new NotificationBackend.NotificationsSentState();
        appRow.sentByApp.avgSentWeekly = 4;
        assertThat(AppNotificationPreferenceController.getNotificationSummary(
                appRow, mContext).toString().contains("30")).isTrue();
        assertThat(
            AppNotificationPreferenceController.getNotificationSummary(appRow, mContext).toString()
                .contains(
                    NotificationBackend.getSentSummary(mContext, appRow.sentByApp, false)))
            .isTrue();
    }

    @Test
    public void getNotificationSummary_channelsNotBlocked() {
        NotificationBackend.AppRow appRow = new NotificationBackend.AppRow();
        appRow.banned = false;
        appRow.blockedChannelCount = 0;
        appRow.channelCount = 10;
        appRow.sentByApp = new NotificationBackend.NotificationsSentState();
        appRow.sentByApp.avgSentDaily = 4;
        assertThat(
            AppNotificationPreferenceController.getNotificationSummary(appRow, mContext).toString())
            .isEqualTo(
                NotificationBackend.getSentSummary(mContext, appRow.sentByApp, false));
    }

    @Test
    public void getNotificationSummary_noChannels() {
        NotificationBackend.AppRow appRow = new NotificationBackend.AppRow();
        appRow.banned = false;
        appRow.blockedChannelCount = 0;
        appRow.channelCount = 0;
        appRow.sentByApp = new NotificationBackend.NotificationsSentState();
        appRow.sentByApp.avgSentDaily = 7;
        assertThat(
            AppNotificationPreferenceController.getNotificationSummary(appRow, mContext).toString())
            .isEqualTo(
                NotificationBackend.getSentSummary(mContext, appRow.sentByApp, false));
    }
}
