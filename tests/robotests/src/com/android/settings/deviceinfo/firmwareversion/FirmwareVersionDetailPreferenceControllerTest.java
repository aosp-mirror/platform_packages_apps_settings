/*
 * Copyright (C) 2019 The Android Open Source Project
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

package com.android.settings.deviceinfo.firmwareversion;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.content.Context;
import android.os.Build;
import android.os.UserManager;

import androidx.preference.Preference;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.util.ReflectionHelpers;

@RunWith(RobolectricTestRunner.class)
public class FirmwareVersionDetailPreferenceControllerTest {

    @Mock
    private UserManager mUserManager;

    private Preference mPreference;
    private Context mContext;
    private FirmwareVersionDetailPreferenceController mController;

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);
        mContext = spy(RuntimeEnvironment.application);
        mController = spy(new TestController(mContext, "key"));

        ReflectionHelpers.setField(mController, "mUserManager", mUserManager);

        mPreference = new Preference(mContext);
        mPreference.setKey(mController.getPreferenceKey());
    }

    @Test
    public void getSummary_shouldGetBuildVersion() {
        assertThat(mController.getSummary()).isEqualTo(Build.VERSION.RELEASE_OR_CODENAME);
    }

    @Test
    public void handleSettingClicked_userRestricted_shouldDoNothing() {
        final long[] hits = ReflectionHelpers.getField(mController, "mHits");
        hits[0] = Long.MAX_VALUE;
        when(mUserManager.hasUserRestriction(UserManager.DISALLOW_FUN)).thenReturn(true);

        mController.handlePreferenceTreeClick(mPreference);

        verify(mContext, never()).startActivity(any());
    }

    @Test
    public void handleSettingClicked_userNotRestricted_shouldStartActivity() {
        final long[] hits = ReflectionHelpers.getField(mController, "mHits");
        hits[0] = Long.MAX_VALUE;
        when(mUserManager.hasUserRestriction(UserManager.DISALLOW_FUN)).thenReturn(false);

        mController.handlePreferenceTreeClick(mPreference);

        verify(mContext).startActivity(any());
    }

    @Test
    public void isPublicSlice_returnTrue() {
        assertThat(mController.isPublicSlice()).isTrue();
    }

    private static class TestController extends FirmwareVersionDetailPreferenceController {

        public TestController(Context context, String key) {
            super(context, key);
        }

        @Override
        void initializeAdminPermissions() {
        }
    }
}
