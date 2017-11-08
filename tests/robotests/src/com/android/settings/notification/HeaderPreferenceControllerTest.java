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

package com.android.settings.notification;

import static android.app.NotificationManager.IMPORTANCE_NONE;

import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertTrue;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.app.Activity;
import android.app.NotificationChannel;
import android.app.NotificationChannelGroup;
import android.app.NotificationManager;
import android.content.Context;
import android.os.Build;
import android.os.UserManager;
import android.support.v14.preference.PreferenceFragment;
import android.view.View;

import com.android.settings.R;
import com.android.settings.TestConfig;
import com.android.settings.applications.LayoutPreference;
import com.android.settings.testutils.SettingsRobolectricTestRunner;
import com.android.settings.wrapper.NotificationChannelGroupWrapper;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowApplication;

@RunWith(SettingsRobolectricTestRunner.class)
@Config(manifest = TestConfig.MANIFEST_PATH, sdk = Build.VERSION_CODES.O)
public class HeaderPreferenceControllerTest {

    private Context mContext;
    @Mock
    private NotificationManager mNm;
    @Mock
    private UserManager mUm;

    private HeaderPreferenceController mController;
    @Mock
    private LayoutPreference mPreference;
    @Mock
    private View mView;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        ShadowApplication shadowApplication = ShadowApplication.getInstance();
        shadowApplication.setSystemService(Context.NOTIFICATION_SERVICE, mNm);
        shadowApplication.setSystemService(Context.USER_SERVICE, mUm);
        mContext = shadowApplication.getApplicationContext();
        PreferenceFragment fragment = mock(PreferenceFragment.class);
        when(fragment.getContext()).thenReturn(mContext);
        Activity activity = mock(Activity.class);
        when(activity.getApplicationContext()).thenReturn(mContext);
        when(fragment.getActivity()).thenReturn(activity);
        mController = spy(new HeaderPreferenceController(mContext, fragment));
        when(mPreference.findViewById(anyInt())).thenReturn(mView);
    }

    @Test
    public void testNoCrashIfNoOnResume() throws Exception {
        mController.isAvailable();
        mController.updateState(mock(LayoutPreference.class));
    }

    @Test
    public void testIsAvailable_notIfNull() throws Exception {
        mController.onResume(null, null, null, null);
        assertFalse(mController.isAvailable());
    }

    @Test
    public void testIsAvailable() throws Exception {
        NotificationBackend.AppRow appRow = new NotificationBackend.AppRow();
        appRow.banned = true;
        mController.onResume(appRow, null, null, null);
        assertTrue(mController.isAvailable());
    }

    @Test
    public void testGetLabel() throws Exception {
        NotificationBackend.AppRow appRow = new NotificationBackend.AppRow();
        appRow.label = "bananas";
        mController.onResume(appRow, null, null, null);
        assertEquals(appRow.label, mController.getLabel());

        NotificationChannelGroup group = new NotificationChannelGroup("id", "name");
        NotificationChannelGroupWrapper gWrapper = new NotificationChannelGroupWrapper(group);
        mController.onResume(appRow, null, gWrapper, null);
        assertEquals(group.getName(), mController.getLabel());

        NotificationChannel channel = new NotificationChannel("cid", "cname", IMPORTANCE_NONE);
        mController.onResume(appRow, channel, gWrapper, null);
        assertEquals(channel.getName(), mController.getLabel());
    }

    @Test
    public void testGetSummary() throws Exception {
        NotificationBackend.AppRow appRow = new NotificationBackend.AppRow();
        appRow.label = "bananas";
        mController.onResume(appRow, null, null, null);
        assertEquals("", mController.getSummary());

        NotificationChannelGroup group = new NotificationChannelGroup("id", "name");
        NotificationChannelGroupWrapper gWrapper = new NotificationChannelGroupWrapper(group);
        mController.onResume(appRow, null, gWrapper, null);
        assertEquals(appRow.label, mController.getSummary());

        NotificationChannel channel = new NotificationChannel("cid", "cname", IMPORTANCE_NONE);
        mController.onResume(appRow, channel, gWrapper, null);
        assertTrue(mController.getSummary().toString().contains(group.getName()));
        assertTrue(mController.getSummary().toString().contains(appRow.label));

        mController.onResume(appRow, channel, null, null);
        assertFalse(mController.getSummary().toString().contains(group.getName()));
        assertTrue(mController.getSummary().toString().contains(appRow.label));
    }
}
