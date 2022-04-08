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
 * limitations under the License.
 */

package com.android.settings.shortcut;

import static com.android.settings.shortcut.CreateShortcutPreferenceController.SHORTCUT_ID_PREFIX;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Mockito.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.ApplicationInfo;
import android.content.pm.ResolveInfo;
import android.content.pm.ShortcutInfo;
import android.content.pm.ShortcutManager;

import com.android.settings.Settings;
import com.android.settings.testutils.shadow.ShadowConnectivityManager;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import org.robolectric.shadow.api.Shadow;
import org.robolectric.shadows.ShadowPackageManager;

import java.util.Arrays;
import java.util.List;

/**
 * Tests for {@link CreateShortcutPreferenceController}
 */
@RunWith(RobolectricTestRunner.class)
@Config(shadows = ShadowConnectivityManager.class)
public class CreateShortcutPreferenceControllerTest {

    @Mock
    private ShortcutManager mShortcutManager;
    @Mock
    private Activity mHost;

    private Context mContext;
    private ShadowConnectivityManager mShadowConnectivityManager;
    private ShadowPackageManager mPackageManager;
    private CreateShortcutPreferenceController mController;

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);
        mContext = spy(RuntimeEnvironment.application);
        doReturn(mShortcutManager).when(mContext).getSystemService(eq(Context.SHORTCUT_SERVICE));
        mPackageManager = Shadow.extract(mContext.getPackageManager());
        mShadowConnectivityManager = ShadowConnectivityManager.getShadow();
        mShadowConnectivityManager.setTetheringSupported(true);

        mController = new CreateShortcutPreferenceController(mContext, "key");
        mController.setActivity(mHost);
    }

    @Test
    public void createResultIntent() {
        when(mShortcutManager.createShortcutResultIntent(any(ShortcutInfo.class)))
                .thenReturn(new Intent().putExtra("d1", "d2"));

        final Intent intent = new Intent(CreateShortcutPreferenceController.SHORTCUT_PROBE)
                .setClass(mContext, Settings.ManageApplicationsActivity.class);
        final ResolveInfo ri = mContext.getPackageManager().resolveActivity(intent, 0);
        final Intent result = mController.createResultIntent(intent, ri, "dummy");

        assertThat(result.getStringExtra("d1")).isEqualTo("d2");
        assertThat((Object) result.getParcelableExtra(Intent.EXTRA_SHORTCUT_INTENT)).isNotNull();

        ArgumentCaptor<ShortcutInfo> infoCaptor = ArgumentCaptor.forClass(ShortcutInfo.class);
        verify(mShortcutManager, times(1))
                .createShortcutResultIntent(infoCaptor.capture());
        assertThat(infoCaptor.getValue().getId())
                .isEqualTo(SHORTCUT_ID_PREFIX + intent.getComponent().flattenToShortString());
    }

    @Test
    public void queryShortcuts_shouldOnlyIncludeSystemApp() {
        final ResolveInfo ri1 = new ResolveInfo();
        ri1.activityInfo = new ActivityInfo();
        ri1.activityInfo.name = "activity1";
        ri1.activityInfo.applicationInfo = new ApplicationInfo();
        ri1.activityInfo.applicationInfo.flags = 0;
        final ResolveInfo ri2 = new ResolveInfo();
        ri2.activityInfo = new ActivityInfo();
        ri2.activityInfo.name = "activity2";
        ri2.activityInfo.applicationInfo = new ApplicationInfo();
        ri2.activityInfo.applicationInfo.flags = ApplicationInfo.FLAG_SYSTEM;

        mPackageManager.setResolveInfosForIntent(
                new Intent(CreateShortcutPreferenceController.SHORTCUT_PROBE),
                Arrays.asList(ri1, ri2));

        final List<ResolveInfo> info = mController.queryShortcuts();
        assertThat(info).hasSize(1);
        assertThat(info.get(0).activityInfo).isEqualTo(ri2.activityInfo);
    }

    @Test
    public void queryShortcuts_shouldSortBasedOnPriority() {
        final ResolveInfo ri1 = new ResolveInfo();
        ri1.priority = 100;
        ri1.activityInfo = new ActivityInfo();
        ri1.activityInfo.name = "activity1";
        ri1.activityInfo.applicationInfo = new ApplicationInfo();
        ri1.activityInfo.applicationInfo.flags = ApplicationInfo.FLAG_SYSTEM;

        final ResolveInfo ri2 = new ResolveInfo();
        ri1.priority = 50;
        ri2.activityInfo = new ActivityInfo();
        ri2.activityInfo.name = "activity2";
        ri2.activityInfo.applicationInfo = new ApplicationInfo();
        ri2.activityInfo.applicationInfo.flags = ApplicationInfo.FLAG_SYSTEM;

        mPackageManager.setResolveInfosForIntent(
                new Intent(CreateShortcutPreferenceController.SHORTCUT_PROBE),
                Arrays.asList(ri1, ri2));

        final List<ResolveInfo> info = mController.queryShortcuts();
        assertThat(info).hasSize(2);
        assertThat(info.get(0).activityInfo).isEqualTo(ri2.activityInfo);
        assertThat(info.get(1).activityInfo).isEqualTo(ri1.activityInfo);
    }
}
