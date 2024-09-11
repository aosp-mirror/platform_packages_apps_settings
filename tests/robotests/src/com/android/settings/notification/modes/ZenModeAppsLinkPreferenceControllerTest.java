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

import static com.android.settings.notification.modes.ZenModeFragmentBase.MODE_ID;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.app.AutomaticZenRule;
import android.app.Flags;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.net.Uri;
import android.os.Bundle;
import android.platform.test.annotations.EnableFlags;
import android.platform.test.flag.junit.SetFlagsRule;
import android.service.notification.ZenPolicy;

import androidx.fragment.app.Fragment;
import androidx.preference.Preference;

import com.android.settings.SettingsActivity;
import com.android.settings.notification.NotificationBackend;
import com.android.settingslib.applications.ApplicationsState;
import com.android.settingslib.core.instrumentation.MetricsFeatureProvider;
import com.android.settingslib.widget.SelectorWithWidgetPreference;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.util.ReflectionHelpers;

import java.util.ArrayList;
import java.util.List;

@RunWith(RobolectricTestRunner.class)
@EnableFlags(Flags.FLAG_MODES_UI)
public final class ZenModeAppsLinkPreferenceControllerTest {

    private ZenModeAppsLinkPreferenceController mController;

    private Context mContext;
    @Mock
    private ZenModesBackend mZenModesBackend;

    @Mock
    private NotificationBackend mNotificationBackend;

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
                mZenModesBackend);
        ReflectionHelpers.setField(mController, "mNotificationBackend", mNotificationBackend);
    }

    private ApplicationsState.AppEntry createAppEntry(String packageName, String label) {
        ApplicationsState.AppEntry entry = mock(ApplicationsState.AppEntry.class);
        entry.info = new ApplicationInfo();
        entry.info.packageName = packageName;
        entry.label = label;
        entry.info.uid = 0;
        return entry;
    }

    private ZenMode createPriorityChannelsZenMode() {
        return new ZenMode("id", new AutomaticZenRule.Builder("Bedtime",
                Uri.parse("bed"))
                .setType(AutomaticZenRule.TYPE_BEDTIME)
                .setInterruptionFilter(INTERRUPTION_FILTER_PRIORITY)
                .setZenPolicy(new ZenPolicy.Builder()
                        .allowChannels(ZenPolicy.CHANNEL_POLICY_PRIORITY)
                        .build())
                .build(), true);
    }

    @Test
    public void testIsAvailable() {
        assertThat(mController.isAvailable()).isTrue();
    }

    @Test
    public void testUpdateSetsIntent() {
        // Creates the preference
        SelectorWithWidgetPreference preference = mock(SelectorWithWidgetPreference.class);
        // Create a zen mode that allows priority channels to breakthrough.
        ZenMode zenMode = createPriorityChannelsZenMode();

        // Capture the intent
        ArgumentCaptor<Intent> captor = ArgumentCaptor.forClass(Intent.class);
        mController.updateState((Preference) preference, zenMode);
        verify(preference).setIntent(captor.capture());
        Intent launcherIntent = captor.getValue();

        assertThat(launcherIntent.getStringExtra(SettingsActivity.EXTRA_SHOW_FRAGMENT))
                .isEqualTo("com.android.settings.notification.modes.ZenModeAppsFragment");
        assertThat(launcherIntent.getIntExtra(MetricsFeatureProvider.EXTRA_SOURCE_METRICS_CATEGORY,
                -1)).isEqualTo(0);

        Bundle bundle = launcherIntent.getBundleExtra(
                SettingsActivity.EXTRA_SHOW_FRAGMENT_ARGUMENTS);
        assertThat(bundle).isNotNull();
        assertThat(bundle.getString(MODE_ID)).isEqualTo("id");
    }

    @Test
    public void testGetAppsBypassingDnd() {
        ApplicationsState.AppEntry entry = createAppEntry("test", "testLabel");
        ApplicationsState.AppEntry entryConv = createAppEntry("test_conv", "test_convLabel");
        List<ApplicationsState.AppEntry> appEntries = List.of(entry, entryConv);

        when(mNotificationBackend.getPackagesBypassingDnd(mContext.getUserId(),
                false)).thenReturn(List.of("test"));

        assertThat(mController.getAppsBypassingDnd(appEntries)).containsExactly("testLabel");
    }

    @Test
    public void testUpdateTriggersRebuild() {
        // Creates the preference
        SelectorWithWidgetPreference preference = mock(SelectorWithWidgetPreference.class);
        // Create a zen mode that allows priority channels to breakthrough.
        ZenMode zenMode = createPriorityChannelsZenMode();

        // Create some applications.
        ArrayList<ApplicationsState.AppEntry> appEntries =
                new ArrayList<ApplicationsState.AppEntry>();
        appEntries.add(createAppEntry("test", "pkgLabel"));

        when(mNotificationBackend.getPackagesBypassingDnd(
                mContext.getUserId(), false))
                .thenReturn(List.of("test"));

        // Updates the preference with the zen mode. We expect that this causes the app session
        // to trigger a rebuild.
        mController.updateZenMode((Preference) preference, zenMode);
        verify(mSession).rebuild(any(), any(), eq(false));

        // Manually triggers the callback that will happen on rebuild.
        mController.mAppSessionCallbacks.onRebuildComplete(appEntries);
        verify(preference).setSummary("pkgLabel can interrupt");
    }

    @Test
    public void testOnPackageListChangedTriggersRebuild() {
        mController.mAppSessionCallbacks.onPackageListChanged();
        verify(mSession).rebuild(any(), any(), eq(false));
    }

    @Test
    public void testOnLoadEntriesCompletedTriggersRebuild() {
        mController.mAppSessionCallbacks.onLoadEntriesCompleted();
        verify(mSession).rebuild(any(), any(), eq(false));
    }
}
