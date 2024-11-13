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

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.app.Flags;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.platform.test.annotations.EnableFlags;
import android.platform.test.flag.junit.SetFlagsRule;

import androidx.fragment.app.Fragment;
import androidx.preference.Preference;
import androidx.preference.PreferenceCategory;

import com.android.settingslib.applications.ApplicationsState;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@RunWith(RobolectricTestRunner.class)
@EnableFlags(Flags.FLAG_MODES_UI)
public class ZenModeAllBypassingAppsPreferenceControllerTest {

    @Rule
    public final SetFlagsRule mSetFlagsRule = new SetFlagsRule();

    private ZenModeAllBypassingAppsPreferenceController mController;

    private Context mContext;
    @Mock
    private ZenHelperBackend mBackend;
    @Mock
    private PreferenceCategory mPreferenceCategory;
    @Mock
    private ApplicationsState mApplicationState;

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);
        mContext = RuntimeEnvironment.application;

        mController = new ZenModeAllBypassingAppsPreferenceController(
                mContext, null, mock(Fragment.class), mBackend);
        mController.mPreferenceCategory = mPreferenceCategory;
        mController.mApplicationsState = mApplicationState;
        mController.mPrefContext = mContext;
    }

    @Test
    public void testIsAvailable() {
        assertThat(mController.isAvailable()).isTrue();
    }

    @Test
    public void testUpdateAppList() {
        // WHEN there's two apps with notification channels that bypass DND
        ApplicationsState.AppEntry entry1 = mock(ApplicationsState.AppEntry.class);
        entry1.info = new ApplicationInfo();
        entry1.info.packageName = "test";
        entry1.info.uid = 0;

        ApplicationsState.AppEntry entry2 = mock(ApplicationsState.AppEntry.class);
        entry2.info = new ApplicationInfo();
        entry2.info.packageName = "test2";
        entry2.info.uid = 0;

        ApplicationsState.AppEntry entry3= mock(ApplicationsState.AppEntry.class);
        entry3.info = new ApplicationInfo();
        entry3.info.packageName = "test3";
        entry3.info.uid = 0;

        List<ApplicationsState.AppEntry> appEntries = new ArrayList<>();
        appEntries.add(entry1);
        appEntries.add(entry2);
        appEntries.add(entry3);
        when(mBackend.getPackagesBypassingDnd(anyInt())).thenReturn(
                Map.of("test", true, "test2", false));

        // THEN there's are two preferences
        mController.updateAppList(appEntries);
        ArgumentCaptor<Preference> captor = ArgumentCaptor.forClass(Preference.class);
        verify(mPreferenceCategory, times(2)).addPreference(captor.capture());
        List<Preference> prefs = captor.getAllValues();
        assertThat(prefs.get(0).getSummary().toString()).isEqualTo("All notifications");
        assertThat(prefs.get(1).getSummary().toString()).isEqualTo("Some notifications");
    }

    @Test
    public void testUpdateAppList_nullApps() {
        mController.updateAppList(null);
        verify(mPreferenceCategory, never()).addPreference(any());
    }

    @Test
    public void testUpdateAppList_emptyAppList() {
        // WHEN there are no apps
        mController.updateAppList(new ArrayList<>());

        // THEN only the appWithChannelsNoneBypassing makes it to the app list
        ArgumentCaptor<Preference> prefCaptor = ArgumentCaptor.forClass(Preference.class);
        verify(mPreferenceCategory).addPreference(prefCaptor.capture());

        Preference pref = prefCaptor.getValue();
        assertThat(pref.getKey()).isEqualTo(
                ZenModeAllBypassingAppsPreferenceController.KEY_NO_APPS);
    }
}
