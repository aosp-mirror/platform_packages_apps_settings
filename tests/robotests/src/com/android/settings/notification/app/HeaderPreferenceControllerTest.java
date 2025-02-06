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

package com.android.settings.notification.app;

import static android.app.NotificationManager.IMPORTANCE_NONE;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

import android.app.NotificationChannel;
import android.app.NotificationChannelGroup;
import android.app.NotificationManager;
import android.content.Context;
import android.os.UserManager;
import android.view.View;

import androidx.fragment.app.FragmentActivity;

import com.android.settings.dashboard.DashboardFragment;
import com.android.settings.notification.NotificationBackend;
import com.android.settingslib.widget.LayoutPreference;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowApplication;

import java.util.ArrayList;

@RunWith(RobolectricTestRunner.class)
@Config(shadows = {
        com.android.settings.testutils.shadow.ShadowFragment.class,
})
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
        mContext = RuntimeEnvironment.application;
        DashboardFragment fragment = mock(DashboardFragment.class);
        when(fragment.getContext()).thenReturn(mContext);
        FragmentActivity activity = mock(FragmentActivity.class);
        when(activity.getApplicationContext()).thenReturn(mContext);
        when(fragment.getActivity()).thenReturn(activity);
        mController = spy(new HeaderPreferenceController(mContext, fragment));
        when(mPreference.findViewById(anyInt())).thenReturn(mView);
    }

    @Test
    public void testNoCrashIfNoOnResume() {
        mController.isAvailable();
        mController.updateState(mock(LayoutPreference.class));
    }

    @Test
    public void testIsAvailable_notIfNull() {
        mController.onResume(null, null, null, null, null, null, null);
        assertFalse(mController.isAvailable());
    }

    @Test
    public void testIsAvailable() {
        NotificationBackend.AppRow appRow = new NotificationBackend.AppRow();
        appRow.banned = true;
        mController.onResume(appRow, null, null, null, null, null, null);
        assertTrue(mController.isAvailable());
    }

    @Test
    public void testIsAvailable_ignoredFilter() {
        NotificationBackend.AppRow appRow = new NotificationBackend.AppRow();
        appRow.banned = true;
        mController.onResume(appRow, null, null, null, null, null, new ArrayList<>());
        assertTrue(mController.isAvailable());
    }

    @Test
    public void testGetSummary() {
        NotificationBackend.AppRow appRow = new NotificationBackend.AppRow();
        appRow.label = "bananas";
        mController.onResume(appRow, null, null, null, null, null, null);
        assertEquals("", mController.getSummary());

        NotificationChannelGroup group = new NotificationChannelGroup("id", "name");

        NotificationChannel channel = new NotificationChannel("cid", "cname", IMPORTANCE_NONE);
        mController.onResume(appRow, channel, group, null, null, null, null);
        assertTrue(mController.getSummary().toString().contains(group.getName()));

        mController.onResume(appRow, channel, null, null, null, null, null);
        assertFalse(mController.getSummary().toString().contains(group.getName()));

        NotificationChannel defaultChannel = new NotificationChannel(
                NotificationChannel.DEFAULT_CHANNEL_ID, "", IMPORTANCE_NONE);
        mController.onResume(appRow, defaultChannel, null, null, null, null, null);
        assertEquals("", mController.getSummary());
    }

    @Test
    public void testGetSecondSummary() {
        NotificationBackend.AppRow appRow = new NotificationBackend.AppRow();
        appRow.label = "bananas";
        mController.onResume(appRow, null, null, null, null, null, null);
        assertEquals(null, mController.getSecondSummary());

        NotificationChannelGroup group = new NotificationChannelGroup("id", "name");

        NotificationChannel channel = new NotificationChannel("cid", "cname", IMPORTANCE_NONE);
        mController.onResume(appRow, channel, group, null, null, null, null);
        assertEquals(null, mController.getSecondSummary());

        channel.setDescription("description");
        mController.onResume(appRow, channel, group, null, null, null, null);
        assertEquals("description", mController.getSecondSummary());
    }
}
