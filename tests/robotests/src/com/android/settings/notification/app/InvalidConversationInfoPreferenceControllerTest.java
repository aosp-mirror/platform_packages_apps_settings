/*
 * Copyright (C) 2020 The Android Open Source Project
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

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

import android.app.NotificationManager;
import android.content.Context;
import android.os.UserManager;

import androidx.preference.Preference;
import androidx.preference.PreferenceScreen;

import com.android.settings.notification.NotificationBackend;
import com.android.settings.testutils.shadow.SettingsShadowResources;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowApplication;

@RunWith(RobolectricTestRunner.class)
@Config(shadows = SettingsShadowResources.class)
public class InvalidConversationInfoPreferenceControllerTest {

    private Context mContext;
    @Mock
    private NotificationBackend mBackend;
    @Mock
    private NotificationManager mNm;
    @Mock
    private UserManager mUm;
    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private PreferenceScreen mScreen;

    private InvalidConversationInfoPreferenceController mController;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        ShadowApplication shadowApplication = ShadowApplication.getInstance();
        shadowApplication.setSystemService(Context.NOTIFICATION_SERVICE, mNm);
        shadowApplication.setSystemService(Context.USER_SERVICE, mUm);
        mContext = RuntimeEnvironment.application;
        mController = spy(new InvalidConversationInfoPreferenceController(mContext, mBackend));
    }

    @After
    public void tearDown() {
        SettingsShadowResources.reset();
    }

    @Test
    public void testNoCrashIfNoOnResume() {
        mController.isAvailable();
        mController.updateState(mock(Preference.class));
    }

    @Test
    public void testIsAvailable_notIfAppBlocked() {
        when(mBackend.isInInvalidMsgState(anyString(), anyInt())).thenReturn(true);
        NotificationBackend.AppRow appRow = new NotificationBackend.AppRow();
        appRow.pkg = "hi";
        appRow.uid = 0;
        appRow.banned = true;
        mController.onResume(appRow, null, null, null, null, null);
        assertFalse(mController.isAvailable());
    }

    @Test
    public void testIsAvailable_notIfInValidMsgState() {
        when(mBackend.isInInvalidMsgState(anyString(), anyInt())).thenReturn(false);
        NotificationBackend.AppRow appRow = new NotificationBackend.AppRow();
        appRow.pkg = "hi";
        appRow.uid = 0;
        mController.onResume(appRow, null, null, null, null, null);
        assertFalse(mController.isAvailable());
    }

    @Test
    public void testIsAvailable() {
        when(mBackend.isInInvalidMsgState(anyString(), anyInt())).thenReturn(true);
        NotificationBackend.AppRow appRow = new NotificationBackend.AppRow();
        appRow.pkg = "hi";
        appRow.uid = 0;
        mController.onResume(appRow, null, null, null, null, null);
        assertTrue(mController.isAvailable());
    }

    @Test
    public void testUpdateState() {
        when(mBackend.isInInvalidMsgState(anyString(), anyInt())).thenReturn(true);
        NotificationBackend.AppRow appRow = new NotificationBackend.AppRow();
        appRow.pkg = "hi";
        appRow.uid = 0;
        appRow.label = "plum";
        mController.onResume(appRow, null, null, null, null, null);
        Preference pref = new Preference(mContext);
        mController.updateState(pref);
        assertTrue(pref.getSummary().toString().contains(appRow.label));
    }
}
