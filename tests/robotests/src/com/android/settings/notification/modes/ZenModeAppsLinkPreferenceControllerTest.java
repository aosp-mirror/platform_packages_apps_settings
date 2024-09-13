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

import static android.app.NotificationManager.INTERRUPTION_FILTER_NONE;
import static android.app.NotificationManager.INTERRUPTION_FILTER_PRIORITY;
import static android.provider.Settings.EXTRA_AUTOMATIC_ZEN_RULE_ID;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.truth.Truth.assertThat;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static org.robolectric.Shadows.shadowOf;

import android.app.Flags;
import android.app.settings.SettingsEnums;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.UserInfo;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.os.UserHandle;
import android.os.UserManager;
import android.platform.test.annotations.EnableFlags;
import android.platform.test.flag.junit.SetFlagsRule;
import android.service.notification.ZenPolicy;
import android.view.LayoutInflater;
import android.view.View;

import androidx.fragment.app.Fragment;
import androidx.preference.PreferenceViewHolder;

import com.android.settings.R;
import com.android.settings.SettingsActivity;
import com.android.settingslib.applications.ApplicationsState;
import com.android.settingslib.applications.ApplicationsState.AppEntry;
import com.android.settingslib.core.instrumentation.MetricsFeatureProvider;
import com.android.settingslib.notification.modes.TestModeBuilder;
import com.android.settingslib.notification.modes.ZenMode;
import com.android.settingslib.notification.modes.ZenModesBackend;

import com.google.common.collect.ImmutableList;
import com.google.common.util.concurrent.MoreExecutors;

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
    private CircularIconsPreference mPreference;
    private CircularIconsView mIconsView;

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
        CircularIconSet.sExecutorService = MoreExecutors.newDirectExecutorService();
        mPreference = new TestableCircularIconsPreference(mContext);
        when(mApplicationsState.newSession(any(), any())).thenReturn(mSession);

        mController = new ZenModeAppsLinkPreferenceController(
                mContext, "controller_key", mock(Fragment.class), mApplicationsState,
                mZenModesBackend, mHelperBackend,
                /* appIconRetriever= */ appInfo -> new ColorDrawable());

        // Ensure the preference view is bound & measured (needed to add child ImageViews).
        View preferenceView = LayoutInflater.from(mContext).inflate(mPreference.getLayoutResource(),
                null);
        mIconsView = checkNotNull(preferenceView.findViewById(R.id.circles_container));
        mIconsView.setUiExecutor(MoreExecutors.directExecutor());
        preferenceView.measure(View.MeasureSpec.makeMeasureSpec(1000, View.MeasureSpec.EXACTLY),
                View.MeasureSpec.makeMeasureSpec(1000, View.MeasureSpec.EXACTLY));
        PreferenceViewHolder holder = PreferenceViewHolder.createInstanceForTests(preferenceView);
        mPreference.onBindViewHolder(holder);
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
    public void updateState_dnd_enabled() {
        ZenMode dnd = TestModeBuilder.MANUAL_DND_ACTIVE;
        mController.updateState(mPreference, dnd);
        assertThat(mPreference.isEnabled()).isTrue();
    }

    @Test
    public void updateState_specialDnd_disabled() {
        ZenMode specialDnd = TestModeBuilder.manualDnd(INTERRUPTION_FILTER_NONE, true);
        mController.updateState(mPreference, specialDnd);
        assertThat(mPreference.isEnabled()).isFalse();
    }

    @Test
    public void testUpdateState_disabled() {
        ZenMode zenMode = new TestModeBuilder()
                .setEnabled(false)
                .build();

        mController.updateState(mPreference, zenMode);

        assertThat(mPreference.isEnabled()).isFalse();
    }

    @Test
    public void testUpdateSetsIntent() {
        // Create a zen mode that allows priority channels to breakthrough.
        ZenMode zenMode = createPriorityChannelsZenMode();

        mController.updateState(mPreference, zenMode);
        Intent launcherIntent = mPreference.getIntent();

        assertThat(launcherIntent.getStringExtra(SettingsActivity.EXTRA_SHOW_FRAGMENT))
                .isEqualTo("com.android.settings.notification.modes.ZenModeAppsFragment");
        assertThat(launcherIntent.getIntExtra(MetricsFeatureProvider.EXTRA_SOURCE_METRICS_CATEGORY,
                -1)).isEqualTo(SettingsEnums.ZEN_PRIORITY_MODE);

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
    public void updateState_withPolicyAllowingNoChannels_doesNotLoadPriorityApps() {
        ZenMode zenMode = new TestModeBuilder()
                .setZenPolicy(new ZenPolicy.Builder().allowPriorityChannels(false).build())
                .build();

        mController.updateState(mPreference, zenMode);

        verifyNoMoreInteractions(mSession);
        verify(mHelperBackend, never()).getPackagesBypassingDnd(anyInt(), anyBoolean());
        assertThat(String.valueOf(mPreference.getSummary())).isEqualTo("None");
    }

    @Test
    public void updateState_withPolicyAllowingPriorityChannels_triggersRebuild() {
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
        mController.updateZenMode(mPreference, zenMode);
        verify(mSession).rebuild(any(), any(), eq(false));
        assertThat(String.valueOf(mPreference.getSummary())).isEqualTo("Calculating…");

        // Manually triggers the callback that will happen on rebuild.
        mController.mAppSessionCallbacks.onRebuildComplete(appEntries);
        assertThat(String.valueOf(mPreference.getSummary())).isEqualTo("test can interrupt");
    }

    @Test
    public void updateState_withPolicyAllowingPriorityChannels_loadsIcons() {
        ZenMode zenMode = createPriorityChannelsZenMode();

        mController.updateState(mPreference, zenMode);
        when(mHelperBackend.getPackagesBypassingDnd(anyInt(), anyBoolean()))
                .thenReturn(ImmutableList.of("test1", "test2"));
        ArrayList<ApplicationsState.AppEntry> appEntries = new ArrayList<>();
        appEntries.add(createAppEntry("test1", mContext.getUserId()));
        appEntries.add(createAppEntry("test2", mContext.getUserId()));
        mController.mAppSessionCallbacks.onRebuildComplete(appEntries);

        assertThat(mIconsView.getDisplayedIcons().icons()).hasSize(2);
    }

    @Test
    public void testOnPackageListChangedTriggersRebuild() {
        // Create a zen mode that allows priority channels to breakthrough.
        ZenMode zenMode = createPriorityChannelsZenMode();
        mController.updateState(mPreference, zenMode);
        verify(mSession).rebuild(any(), any(), eq(false));

        mController.mAppSessionCallbacks.onPackageListChanged();
        verify(mSession, times(2)).rebuild(any(), any(), eq(false));
    }

    @Test
    public void testOnLoadEntriesCompletedTriggersRebuild() {
        // Create a zen mode that allows priority channels to breakthrough.
        ZenMode zenMode = createPriorityChannelsZenMode();
        mController.updateState(mPreference, zenMode);
        verify(mSession).rebuild(any(), any(), eq(false));

        mController.mAppSessionCallbacks.onLoadEntriesCompleted();
        verify(mSession, times(2)).rebuild(any(), any(), eq(false));
    }

    @Test
    public void updateState_noneToPriority_loadsBypassingAppsAndListensForChanges() {
        ZenMode zenModeWithNone = new TestModeBuilder()
                .setZenPolicy(new ZenPolicy.Builder().allowPriorityChannels(false).build())
                .build();
        ZenMode zenModeWithPriority = new TestModeBuilder()
                .setZenPolicy(new ZenPolicy.Builder().allowPriorityChannels(true).build())
                .build();
        ArrayList<ApplicationsState.AppEntry> appEntries = new ArrayList<>();
        appEntries.add(createAppEntry("test", mContext.getUserId()));
        when(mHelperBackend.getPackagesBypassingDnd(mContext.getUserId(), false))
                .thenReturn(List.of("test"));

        mController.updateState(mPreference, zenModeWithNone);

        assertThat(mIconsView.getDisplayedIcons().icons()).hasSize(0);
        verifyNoMoreInteractions(mApplicationsState);
        verifyNoMoreInteractions(mSession);

        mController.updateState(mPreference, zenModeWithPriority);

        verify(mApplicationsState).newSession(any(), any());
        verify(mSession).rebuild(any(), any(), anyBoolean());
        mController.mAppSessionCallbacks.onRebuildComplete(appEntries);
        assertThat(mIconsView.getDisplayedIcons().icons()).hasSize(1);
    }

    @Test
    public void updateState_priorityToNone_clearsBypassingAppsAndStopsListening() {
        ZenMode zenModeWithNone = new TestModeBuilder()
                .setZenPolicy(new ZenPolicy.Builder().allowPriorityChannels(false).build())
                .build();
        ZenMode zenModeWithPriority = new TestModeBuilder()
                .setZenPolicy(new ZenPolicy.Builder().allowPriorityChannels(true).build())
                .build();
        ArrayList<ApplicationsState.AppEntry> appEntries = new ArrayList<>();
        appEntries.add(createAppEntry("test", mContext.getUserId()));
        when(mHelperBackend.getPackagesBypassingDnd(mContext.getUserId(), false))
                .thenReturn(List.of("test"));

        mController.updateState(mPreference, zenModeWithPriority);

        verify(mApplicationsState).newSession(any(), any());
        verify(mSession).rebuild(any(), any(), anyBoolean());
        mController.mAppSessionCallbacks.onRebuildComplete(appEntries);
        assertThat(mIconsView.getDisplayedIcons().icons()).hasSize(1);

        mController.updateState(mPreference, zenModeWithNone);

        assertThat(mIconsView.getDisplayedIcons().icons()).hasSize(0);
        verify(mSession).deactivateSession();
        verifyNoMoreInteractions(mSession);
        verifyNoMoreInteractions(mApplicationsState);

        // An errant callback (triggered by onResume and received asynchronously after
        // updateState()) is ignored.
        mController.mAppSessionCallbacks.onRebuildComplete(appEntries);

        assertThat(mIconsView.getDisplayedIcons().icons()).hasSize(0);
    }

    @Test
    public void updateState_priorityToNoneToPriority_restartsListening() {
        ZenMode zenModeWithNone = new TestModeBuilder()
                .setZenPolicy(new ZenPolicy.Builder().allowPriorityChannels(false).build())
                .build();
        ZenMode zenModeWithPriority = new TestModeBuilder()
                .setZenPolicy(new ZenPolicy.Builder().allowPriorityChannels(true).build())
                .build();

        mController.updateState(mPreference, zenModeWithPriority);
        verify(mApplicationsState).newSession(any(), any());
        verify(mSession).rebuild(any(), any(), anyBoolean());

        mController.updateState(mPreference, zenModeWithNone);
        verifyNoMoreInteractions(mApplicationsState);
        verify(mSession).deactivateSession();

        mController.updateState(mPreference, zenModeWithPriority);
        verifyNoMoreInteractions(mApplicationsState);
        verify(mSession).activateSession();
    }

    @Test
    public void testNoCrashIfAppsReadyBeforeRuleAvailable() {
        mController.mAppSessionCallbacks.onLoadEntriesCompleted();
    }
}
