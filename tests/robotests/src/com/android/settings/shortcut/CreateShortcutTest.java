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

import static com.google.common.truth.Truth.assertThat;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.pm.ShortcutInfo;
import android.content.pm.ShortcutManager;

import com.android.settings.Settings;
import com.android.settings.testutils.SettingsRobolectricTestRunner;
import com.android.settings.testutils.shadow.ShadowConnectivityManager;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.Robolectric;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import org.robolectric.shadow.api.Shadow;
import org.robolectric.shadows.ShadowPackageManager;

import java.util.Arrays;
import java.util.List;

/**
 * Tests for {@link CreateShortcutTest}
 */
@RunWith(SettingsRobolectricTestRunner.class)
@Config(shadows = ShadowConnectivityManager.class)
public class CreateShortcutTest {

    private static final String SHORTCUT_ID_PREFIX = CreateShortcut.SHORTCUT_ID_PREFIX;

    private Context mContext;
    private ShadowConnectivityManager mShadowConnectivityManager;
    private ShadowPackageManager mPackageManager;

    @Mock
    private ShortcutManager mShortcutManager;
    @Captor
    private ArgumentCaptor<List<ShortcutInfo>> mListCaptor;

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);
        mContext = RuntimeEnvironment.application;
        mPackageManager = Shadow.extract(mContext.getPackageManager());
        mShadowConnectivityManager = ShadowConnectivityManager.getShadow();
        mShadowConnectivityManager.setTetheringSupported(true);
    }

    @Test
    public void createResultIntent() {
        CreateShortcut orgActivity = Robolectric.setupActivity(CreateShortcut.class);
        CreateShortcut activity = spy(orgActivity);
        doReturn(mShortcutManager).when(activity).getSystemService(eq(Context.SHORTCUT_SERVICE));

        when(mShortcutManager.createShortcutResultIntent(any(ShortcutInfo.class)))
                .thenReturn(new Intent().putExtra("d1", "d2"));

        final Intent intent = CreateShortcut.getBaseIntent()
                .setClass(activity, Settings.ManageApplicationsActivity.class);
        final ResolveInfo ri = activity.getPackageManager().resolveActivity(intent, 0);
        final Intent result = activity.createResultIntent(intent, ri, "dummy");

        assertThat(result.getStringExtra("d1")).isEqualTo("d2");
        assertThat((Object) result.getParcelableExtra(Intent.EXTRA_SHORTCUT_INTENT)).isNotNull();

        ArgumentCaptor<ShortcutInfo> infoCaptor = ArgumentCaptor.forClass(ShortcutInfo.class);
        verify(mShortcutManager, times(1))
                .createShortcutResultIntent(infoCaptor.capture());
        assertThat(infoCaptor.getValue().getId())
                .isEqualTo(SHORTCUT_ID_PREFIX + intent.getComponent().flattenToShortString());
    }

    @Test
    public void shortcutsUpdateTask() {
        mContext = spy(RuntimeEnvironment.application);
        doReturn(mShortcutManager).when(mContext).getSystemService(eq(Context.SHORTCUT_SERVICE));
        final Intent shortcut1 = CreateShortcut.getBaseIntent().setComponent(
                new ComponentName(mContext, Settings.ManageApplicationsActivity.class));
        final ResolveInfo ri1 = mock(ResolveInfo.class);
        final Intent shortcut2 = CreateShortcut.getBaseIntent().setComponent(
                new ComponentName(mContext, Settings.SoundSettingsActivity.class));
        final ResolveInfo ri2 = mock(ResolveInfo.class);
        when(ri1.loadLabel(any(PackageManager.class))).thenReturn("label1");
        when(ri2.loadLabel(any(PackageManager.class))).thenReturn("label2");
        mPackageManager.addResolveInfoForIntent(shortcut1, ri1);
        mPackageManager.addResolveInfoForIntent(shortcut2, ri2);

        final List<ShortcutInfo> pinnedShortcuts = Arrays.asList(
                makeShortcut("d1"),
                makeShortcut("d2"),
                makeShortcut(Settings.ManageApplicationsActivity.class),
                makeShortcut("d3"),
                makeShortcut(Settings.SoundSettingsActivity.class));
        when(mShortcutManager.getPinnedShortcuts()).thenReturn(pinnedShortcuts);

        new CreateShortcut.ShortcutsUpdateTask(mContext).doInBackground();

        verify(mShortcutManager, times(1)).updateShortcuts(mListCaptor.capture());

        final List<ShortcutInfo> updates = mListCaptor.getValue();

        assertThat(updates).hasSize(2);
        assertThat(pinnedShortcuts.get(2).getId()).isEqualTo(updates.get(0).getId());
        assertThat(pinnedShortcuts.get(4).getId()).isEqualTo(updates.get(1).getId());
    }

    @Test
    public void queryActivities_shouldOnlyIncludeSystemApp() {
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

        mPackageManager.addResolveInfoForIntent(CreateShortcut.getBaseIntent(),
                Arrays.asList(ri1, ri2));

        TestClass orgActivity = Robolectric.setupActivity(TestClass.class);
        TestClass activity = spy(orgActivity);

        List<ResolveInfo> info = activity.onQueryPackageManager(CreateShortcut.getBaseIntent());
        assertThat(info).hasSize(1);
        assertThat(info.get(0)).isEqualTo(ri2);
    }

    private ShortcutInfo makeShortcut(Class<?> className) {
        ComponentName cn = new ComponentName(mContext, className);
        return makeShortcut(SHORTCUT_ID_PREFIX + cn.flattenToShortString());
    }

    private ShortcutInfo makeShortcut(String id) {
        return new ShortcutInfo.Builder(mContext, id).build();
    }

    private static class TestClass extends CreateShortcut {
    }
}
