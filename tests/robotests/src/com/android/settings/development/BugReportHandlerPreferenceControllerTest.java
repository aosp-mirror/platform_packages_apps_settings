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

package com.android.settings.development;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.content.Context;
import android.os.UserManager;

import androidx.preference.Preference;
import androidx.preference.PreferenceScreen;

import com.android.settings.R;
import com.android.settings.bugreporthandler.BugReportHandlerUtil;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.util.ReflectionHelpers;

@RunWith(RobolectricTestRunner.class)
public class BugReportHandlerPreferenceControllerTest {

    @Mock
    private PreferenceScreen mPreferenceScreen;
    @Mock
    private UserManager mUserManager;
    @Mock
    private BugReportHandlerUtil mBugReportHandlerUtil;
    @Mock
    private Preference mPreference;

    private BugReportHandlerPreferenceController mController;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mController = spy(new BugReportHandlerPreferenceController(RuntimeEnvironment.application));
        ReflectionHelpers.setField(mController, "mUserManager", mUserManager);
        ReflectionHelpers.setField(mController, "mBugReportHandlerUtil", mBugReportHandlerUtil);
    }

    @Test
    public void isAvailable_hasDebugRestriction_notAvailable() {
        doReturn(true).when(mUserManager).hasUserRestriction(anyString());
        doReturn(true).when(mBugReportHandlerUtil).isBugReportHandlerEnabled(any(Context.class));

        assertThat(mController.isAvailable()).isFalse();
    }

    @Test
    public void isAvailable_bugReportHandlerDisabled_notAvailable() {
        doReturn(false).when(mBugReportHandlerUtil).isBugReportHandlerEnabled(any(Context.class));
        doReturn(false).when(mUserManager).hasUserRestriction(anyString());

        assertThat(mController.isAvailable()).isFalse();
    }

    @Test
    public void isAvailable_noDebugRestrictionAndBugReportHandlerEnabled_available() {
        doReturn(false).when(mUserManager).hasUserRestriction(anyString());
        doReturn(true).when(mBugReportHandlerUtil).isBugReportHandlerEnabled(any(Context.class));

        assertThat(mController.isAvailable()).isTrue();
    }

    @Test
    public void updateState_hasCurrentBugReportHandlerAppLabel_setAppLabel() {
        when(mPreferenceScreen.findPreference(mController.getPreferenceKey()))
                .thenReturn(mPreference);
        mController.displayPreference(mPreferenceScreen);
        doReturn("SomeRandomAppLabel!!!").when(mController).getCurrentBugReportHandlerAppLabel();

        mController.updateState(mPreference);

        verify(mPreference).setSummary("SomeRandomAppLabel!!!");
    }

    @Test
    public void updateState_noCurrentBugReportHandlerAppLabel_setAppDefaultLabel() {
        when(mPreferenceScreen.findPreference(mController.getPreferenceKey()))
                .thenReturn(mPreference);
        mController.displayPreference(mPreferenceScreen);
        doReturn(null).when(mController).getCurrentBugReportHandlerAppLabel();

        mController.updateState(mPreference);

        verify(mPreference).setSummary(R.string.app_list_preference_none);
    }
}
