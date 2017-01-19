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
 * limitations under the License.
 */

package com.android.settings;

import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.assertion.ViewAssertions.doesNotExist;
import static android.support.test.espresso.matcher.ViewMatchers.withText;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.app.Instrumentation;
import android.content.ComponentName;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.Intent;
import android.content.pm.ResolveInfo;
import android.content.pm.ShortcutInfo;
import android.content.pm.ShortcutManager;
import android.support.test.InstrumentationRegistry;
import android.support.test.filters.SmallTest;
import android.support.test.runner.AndroidJUnit4;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.Arrays;
import java.util.List;

/**
 * Tests for {@link CreateShortcutTest}
 *
 m SettingsTests &&
 adb install \
 -r -g  ${ANDROID_PRODUCT_OUT}/data/app/SettingsTests/SettingsTests.apk &&
 adb shell am instrument -e class com.android.settings.CreateShortcutTest \
 -w com.android.settings.tests/android.support.test.runner.AndroidJUnitRunner
 */
@RunWith(AndroidJUnit4.class)
@SmallTest
public class CreateShortcutTest {

    private static final String SHORTCUT_ID_PREFIX = CreateShortcut.SHORTCUT_ID_PREFIX;

    private Instrumentation mInstrumentation;
    private Context mContext;

    @Mock ShortcutManager mShortcutManager;
    @Captor ArgumentCaptor<List<ShortcutInfo>> mListCaptor;

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);
        mInstrumentation = InstrumentationRegistry.getInstrumentation();
        mContext = mInstrumentation.getTargetContext();
    }

    @Test
    public void test_layoutDoesNotHaveCancelButton() {
        mInstrumentation.startActivitySync(new Intent(Intent.ACTION_CREATE_SHORTCUT)
                .setClassName(mContext, CreateShortcut.class.getName()));
        onView(withText(R.string.cancel)).check(doesNotExist());
    }

    @Test
    public void createResultIntent() {
        CreateShortcut orgActivity = (CreateShortcut) mInstrumentation.startActivitySync(
                new Intent(Intent.ACTION_CREATE_SHORTCUT)
                        .setClassName(mContext, CreateShortcut.class.getName()));
        CreateShortcut activity = spy(orgActivity);
        doReturn(mShortcutManager).when(activity).getSystemService(eq(Context.SHORTCUT_SERVICE));

        when(mShortcutManager.createShortcutResultIntent(any(ShortcutInfo.class)))
                .thenReturn(new Intent().putExtra("d1", "d2"));

        Intent intent = CreateShortcut.getBaseIntent()
                .setClass(activity, Settings.ManageApplicationsActivity.class);
        ResolveInfo ri = activity.getPackageManager().resolveActivity(intent, 0);
        Intent result = activity.createResultIntent(intent, ri, "dummy");
        assertEquals("d2", result.getStringExtra("d1"));
        assertNotNull(result.getParcelableExtra(Intent.EXTRA_SHORTCUT_INTENT));

        ArgumentCaptor<ShortcutInfo> infoCaptor = ArgumentCaptor.forClass(ShortcutInfo.class);
        verify(mShortcutManager, times(1))
                .createShortcutResultIntent(infoCaptor.capture());
        String expectedId = SHORTCUT_ID_PREFIX + intent.getComponent().flattenToShortString();
        assertEquals(expectedId, infoCaptor.getValue().getId());
    }

    @Test
    public void shortcutsUpdateTask() {
        mContext = spy(new ContextWrapper(mInstrumentation.getTargetContext()));
        doReturn(mShortcutManager).when(mContext).getSystemService(eq(Context.SHORTCUT_SERVICE));

        List<ShortcutInfo> pinnedShortcuts = Arrays.asList(
                makeShortcut("d1"), makeShortcut("d2"),
                makeShortcut(Settings.ManageApplicationsActivity.class),
                makeShortcut("d3"),
                makeShortcut(Settings.SoundSettingsActivity.class));
        when(mShortcutManager.getPinnedShortcuts()).thenReturn(pinnedShortcuts);
        new CreateShortcut.ShortcutsUpdateTask(mContext).doInBackground();

        verify(mShortcutManager, times(1)).updateShortcuts(mListCaptor.capture());

        List<ShortcutInfo> updates = mListCaptor.getValue();
        assertEquals(2, updates.size());
        assertEquals(pinnedShortcuts.get(2).getId(), updates.get(0).getId());
        assertEquals(pinnedShortcuts.get(4).getId(), updates.get(1).getId());
    }

    private ShortcutInfo makeShortcut(Class<?> className) {
        ComponentName cn = new ComponentName(mContext, className);
        return makeShortcut(SHORTCUT_ID_PREFIX + cn.flattenToShortString());
    }

    private ShortcutInfo makeShortcut(String id) {
        return new ShortcutInfo.Builder(mContext, id).build();
    }
}
