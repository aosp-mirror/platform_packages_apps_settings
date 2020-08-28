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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import android.content.Context;
import android.content.pm.ShortcutInfo;

import androidx.preference.Preference;

import com.android.settings.notification.NotificationBackend;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

@RunWith(RobolectricTestRunner.class)
public class AddToHomeScreenPreferenceControllerTest {

    private Context mContext;
    @Mock
    private NotificationBackend mBackend;

    private AddToHomeScreenPreferenceController mController;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mContext = RuntimeEnvironment.application;
        mController = new AddToHomeScreenPreferenceController(mContext, mBackend);
    }

    @Test
    public void testNoCrashIfNoOnResume() {
        mController.isAvailable();
        mController.updateState(mock(Preference.class));
    }

    @Test
    public void testIsAvailable_notIfNull() {
        mController.onResume(null, null, null, null, null, null);
        assertFalse(mController.isAvailable());
    }

    @Test
    public void testIsAvailable() {
        ShortcutInfo si = mock(ShortcutInfo.class);
        mController.onResume(mock(NotificationBackend.AppRow.class), null, null, null, si, null);
        assertTrue(mController.isAvailable());
    }

    @Test
    public void testHandlePreferenceTreeClick() {
        ShortcutInfo si = mock(ShortcutInfo.class);
        mController.onResume(mock(NotificationBackend.AppRow.class), null, null, null, si, null);

        Preference pref = new Preference(RuntimeEnvironment.application);
        pref.setKey("add_to_home");
        mController.handlePreferenceTreeClick(pref);

        verify(mBackend).requestPinShortcut(any(), eq(si));
    }
}
