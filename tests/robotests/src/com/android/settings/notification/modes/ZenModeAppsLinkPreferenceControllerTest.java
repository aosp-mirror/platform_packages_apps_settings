/*
 * Copyright (C) 2024 The Android Open Source Project
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

package com.android.settings.notification.modes;

import static android.app.NotificationManager.INTERRUPTION_FILTER_PRIORITY;
import static android.provider.Settings.EXTRA_AUTOMATIC_ZEN_RULE_ID;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.robolectric.Shadows.shadowOf;

import android.app.Flags;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.UserInfo;
import android.os.Bundle;
import android.os.UserHandle;
import android.os.UserManager;
import android.platform.test.annotations.EnableFlags;
import android.platform.test.flag.junit.SetFlagsRule;
import android.service.notification.ZenPolicy;

import androidx.fragment.app.Fragment;

import com.android.settings.SettingsActivity;
import com.android.settingslib.applications.ApplicationsState;
import com.android.settingslib.applications.ApplicationsState.AppEntry;
import com.android.settingslib.core.instrumentation.MetricsFeatureProvider;
import com.android.settingslib.notification.modes.TestModeBuilder;
import com.android.settingslib.notification.modes.ZenMode;
import com.android.settingslib.notification.modes.ZenModesBackend;
import com.android.settingslib.widget.SelectorWithWidgetPreference;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

@RunWith(RobolectricTestRunner.class)
@EnableFlags(Flags.FLAG_MODES_UI)
public final class ZenModeAppsLinkPreferenceControllerTest {

    private ZenModeAppsLinkPreferenceController mController;

    private Context mContext;
    @Mock
    private ZenModesBackend mZenModesBackend;

    @Mock
    private ZenHelperBackend mHelperBackend;

    @Mock
    private ApplicationsState mApplicationsState;
    @Mock
    private ApplicationsState.Session mSession;

    @Rule
    public final SetFlagsRule mSetFlagsRule = new SetFlagsRule();

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);
        mContext = RuntimeEnvironment.application;
        when(mApplicationsState.newSession(any(), any())).thenReturn(mSession);
        mController = new ZenModeAppsLinkPreferenceController(
                mContext, "controller_key", mock(Fragment.class), mApplicationsState,
                mZenModesBackend, mHelperBackend);
    }

    private AppEntry createAppEntry(String packageName, int userId) {
        ApplicationInfo applicationInfo = new ApplicationInfo();
        applicationInfo.packageName = packageName;
        applicationInfo.uid = UserHandle.getUid(userId, new Random().nextInt(100));
        AppEntry appEntry = new AppEntry(mContext, applicationInfo, 1);
        appEntry.label = packageName;
        return appEntry;
    }

    private ZenMode createPriorityChannelsZenMode() {
        return new TestModeBuilder()
                .setId("id")
                .setInterruptionFilter(INTERRUPTION_FILTER_PRIORITY)
                .setZenPolicy(new ZenPolicy.Builder()
                        .allowChannels(ZenPolicy.CHANNEL_POLICY_PRIORITY)
                        .build())
                .build();
    }

    @Test
    public void testIsAvailable() {
        assertThat(mController.isAvailable()).isTrue();
    }

    @Test
    public void testUpdateSetsIntent() {
        // Creates the preference
        SelectorWithWidgetPreference preference = new SelectorWithWidgetPreference(mContext);
        // Create a zen mode that allows priority channels to breakthrough.
        ZenMode zenMode = createPriorityChannelsZenMode();

        mController.updateState(preference, zenMode);
        Intent launcherIntent = preference.getIntent();

        assertThat(launcherIntent.getStringExtra(SettingsActivity.EXTRA_SHOW_FRAGMENT))
                .isEqualTo("com.android.settings.notification.modes.ZenModeAppsFragment");
        assertThat(launcherIntent.getIntExtra(MetricsFeatureProvider.EXTRA_SOURCE_METRICS_CATEGORY,
                -1)).isEqualTo(0);

        Bundle bundle = launcherIntent.getBundleExtra(
                SettingsActivity.EXTRA_SHOW_FRAGMENT_ARGUMENTS);
        assertThat(bundle).isNotNull();
        assertThat(bundle.getString(EXTRA_AUTOMATIC_ZEN_RULE_ID)).isEqualTo("id");
    }

    @Test
    public void testGetAppsBypassingDnd() {
        ApplicationsState.AppEntry app1 = createAppEntry("app1", mContext.getUserId());
        ApplicationsState.AppEntry app2 = createAppEntry("app2", mContext.getUserId());
        List<ApplicationsState.AppEntry> allApps = List.of(app1, app2);

        when(mHelperBackend.getPackagesBypassingDnd(mContext.getUserId(),
                false)).thenReturn(List.of("app1"));

        assertThat(mController.getAppsBypassingDndSortedByName(allApps)).containsExactly(app1);
    }

    @Test
    public void testGetAppsBypassingDnd_sortsByName() {
        ApplicationsState.AppEntry appC = createAppEntry("C", mContext.getUserId());
        ApplicationsState.AppEntry appA = createAppEntry("A", mContext.getUserId());
        ApplicationsState.AppEntry appB = createAppEntry("B", mContext.getUserId());
        List<ApplicationsState.AppEntry> allApps = List.of(appC, appA, appB);

        when(mHelperBackend.getPackagesBypassingDnd(eq(mContext.getUserId()), anyBoolean()))
                .thenReturn(List.of("B", "C", "A"));

        assertThat(mController.getAppsBypassingDndSortedByName(allApps))
                .containsExactly(appA, appB, appC).inOrder();
    }

    @Test
    public void testGetAppsBypassingDnd_withWorkProfile_includesProfileAndSorts() {
        UserInfo workProfile = new UserInfo(10, "Work Profile", 0);
        workProfile.userType = UserManager.USER_TYPE_PROFILE_MANAGED;
        UserManager userManager = mContext.getSystemService(UserManager.class);
        shadowOf(userManager).addProfile(mContext.getUserId(), 10, workProfile);

        ApplicationsState.AppEntry personalCopy = createAppEntry("app", mContext.getUserId());
        ApplicationsState.AppEntry workCopy = createAppEntry("app", 10);
        ApplicationsState.AppEntry otherPersonal = createAppEntry("p2", mContext.getUserId());
        ApplicationsState.AppEntry otherWork = createAppEntry("w2", 10);
        List<ApplicationsState.AppEntry> allApps = List.of(workCopy, personalCopy, otherPersonal,
                otherWork);

        when(mHelperBackend.getPackagesBypassingDnd(eq(mContext.getUserId()), anyBoolean()))
                .thenReturn(List.of("app", "p2"));
        when(mHelperBackend.getPackagesBypassingDnd(eq(10), anyBoolean()))
                .thenReturn(List.of("app"));

        // Personal copy before work copy (names match).
        assertThat(mController.getAppsBypassingDndSortedByName(allApps))
                .containsExactly(personalCopy, workCopy, otherPersonal).inOrder();
    }

    @Test
    public void testUpdateTriggersRebuild() {
        // Creates the preference
        SelectorWithWidgetPreference preference = new SelectorWithWidgetPreference(mContext);
        // Create a zen mode that allows priority channels to breakthrough.
        ZenMode zenMode = createPriorityChannelsZenMode();

        // Create some applications.
        ArrayList<ApplicationsState.AppEntry> appEntries = new ArrayList<>();
        appEntries.add(createAppEntry("test", mContext.getUserId()));

        when(mHelperBackend.getPackagesBypassingDnd(
                mContext.getUserId(), false))
                .thenReturn(List.of("test"));

        // Updates the preference with the zen mode. We expect that this causes the app session
        // to trigger a rebuild (and display a temporary text in the meantime).
        mController.updateZenMode(preference, zenMode);
        verify(mSession).rebuild(any(), any(), eq(false));
        assertThat(String.valueOf(preference.getSummary())).isEqualTo("Calculating…");

        // Manually triggers the callback that will happen on rebuild.
        mController.mAppSessionCallbacks.onRebuildComplete(appEntries);
        assertThat(String.valueOf(preference.getSummary())).isEqualTo("test can interrupt");
    }

    @Test
    public void testOnPackageListChangedTriggersRebuild() {
        SelectorWithWidgetPreference preference = new SelectorWithWidgetPreference(mContext);
        // Create a zen mode that allows priority channels to breakthrough.
        ZenMode zenMode = createPriorityChannelsZenMode();
        mController.updateState(preference, zenMode);
        verify(mSession).rebuild(any(), any(), eq(false));

        mController.mAppSessionCallbacks.onPackageListChanged();
        verify(mSession, times(2)).rebuild(any(), any(), eq(false));
    }

    @Test
    public void testOnLoadEntriesCompletedTriggersRebuild() {
        SelectorWithWidgetPreference preference = new SelectorWithWidgetPreference(mContext);
        // Create a zen mode that allows priority channels to breakthrough.
        ZenMode zenMode = createPriorityChannelsZenMode();
        mController.updateState(preference, zenMode);
        verify(mSession).rebuild(any(), any(), eq(false));

        mController.mAppSessionCallbacks.onLoadEntriesCompleted();
        verify(mSession, times(2)).rebuild(any(), any(), eq(false));
    }

    @Test
    public void testNoCrashIfAppsReadyBeforeRuleAvailable() {
        mController.mAppSessionCallbacks.onLoadEntriesCompleted();
    }
}
