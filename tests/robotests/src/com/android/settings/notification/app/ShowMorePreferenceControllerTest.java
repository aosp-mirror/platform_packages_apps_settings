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

package com.android.settings.notification.app;

import static com.google.common.truth.Truth.assertThat;

import static org.junit.Assert.assertFalse;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import android.app.NotificationChannelGroup;
import android.app.NotificationManager;
import android.content.Context;
import android.os.UserManager;
import android.platform.test.annotations.EnableFlags;
import android.platform.test.flag.junit.SetFlagsRule;

import androidx.preference.Preference;

import com.android.server.notification.Flags;
import com.android.settings.notification.NotificationBackend;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.shadows.ShadowApplication;

@RunWith(RobolectricTestRunner.class)
@EnableFlags(Flags.FLAG_NOTIFICATION_HIDE_UNUSED_CHANNELS)
public class ShowMorePreferenceControllerTest {

    private Context mContext;
    @Mock
    private NotificationBackend mBackend;
    @Mock
    private NotificationManager mNm;
    @Mock
    private UserManager mUm;
    @Mock
    private NotificationSettings.DependentFieldListener mDependentFieldListener;

    private ShowMorePreferenceController mController;
    @Rule
    public final SetFlagsRule mSetFlagsRule = new SetFlagsRule();

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        ShadowApplication shadowApplication = ShadowApplication.getInstance();
        shadowApplication.setSystemService(Context.NOTIFICATION_SERVICE, mNm);
        shadowApplication.setSystemService(Context.USER_SERVICE, mUm);
        mContext = RuntimeEnvironment.application;
        mController = new ShowMorePreferenceController(mContext, mDependentFieldListener, mBackend);
    }

    @Test
    public void noCrashIfNoOnResume() {
        mController.isAvailable();
        mController.updateState(mock(Preference.class));
    }

    @Test
    public void isAvailable_notIfAppBlocked() {
        NotificationBackend.AppRow appRow = new NotificationBackend.AppRow();
        appRow.banned = true;
        appRow.showAllChannels = false;
        mController.onResume(appRow, null, null, null, null, null, null);
        assertFalse(mController.isAvailable());
    }

    @Test
    public void isAvailable_notIfShowingAll() {
        NotificationBackend.AppRow appRow = new NotificationBackend.AppRow();
        mController.onResume(appRow, null, mock(NotificationChannelGroup.class), null, null, null,
                null);
        assertFalse(mController.isAvailable());
    }

    @Test
    public void updateState() {
        NotificationBackend.AppRow appRow = new NotificationBackend.AppRow();
        appRow.banned = false;
        appRow.showAllChannels = false;
        mController.onResume(appRow, null, null, null, null, null, null);

        Preference pref = new Preference(mContext);
        mController.updateState(pref);

        pref.performClick();

        verify(mDependentFieldListener).onFieldValueChanged();
        assertThat(appRow.showAllChannels).isTrue();
    }
}
