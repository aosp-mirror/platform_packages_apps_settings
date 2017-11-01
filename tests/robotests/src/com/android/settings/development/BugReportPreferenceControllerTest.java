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

package com.android.settings.development;

import android.content.Context;
import android.os.UserManager;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceScreen;

import com.android.settings.testutils.SettingsRobolectricTestRunner;
import com.android.settings.TestConfig;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.annotation.Config;

import static org.mockito.Answers.RETURNS_DEEP_STUBS;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(SettingsRobolectricTestRunner.class)
@Config(manifest = TestConfig.MANIFEST_PATH, sdk = TestConfig.SDK_VERSION)
public class BugReportPreferenceControllerTest {

    @Mock
    private Context mContext;
    @Mock
    private Preference mPreference;
    @Mock(answer = RETURNS_DEEP_STUBS)
    private PreferenceScreen mScreen;
    @Mock
    private UserManager mUserManager;

    private BugReportPreferenceController mController;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        when(mContext.getSystemService(Context.USER_SERVICE)).thenReturn(mUserManager);
        when(mScreen.findPreference(anyString())).thenReturn(mPreference);
        mController = new BugReportPreferenceController(mContext);
    }

    @Test
    public void displayPreference_hasDebugRestriction_shouldRemovePreference() {
        when(mUserManager.hasUserRestriction(anyString())).thenReturn(true);
        when(mScreen.getPreferenceCount()).thenReturn(1);
        when(mScreen.getPreference(0)).thenReturn(mPreference);
        when(mPreference.getKey()).thenReturn(mController.getPreferenceKey());

        mController.displayPreference(mScreen);

        verify(mScreen).removePreference(any(Preference.class));
    }

    @Test
    public void displayPreference_noDebugRestriction_shouldNotRemovePreference() {
        when(mUserManager.hasUserRestriction(anyString())).thenReturn(false);

        mController.displayPreference(mScreen);

        verify(mScreen, never()).removePreference(any(Preference.class));
    }

    @Test
    public void enablePreference_hasDebugRestriction_shouldNotEnable() {
        when(mUserManager.hasUserRestriction(anyString())).thenReturn(true);
        mController.displayPreference(mScreen);

        mController.enablePreference(true);

        verify(mPreference, never()).setEnabled(anyBoolean());
    }

    @Test
    public void enablePreference_noDebugRestriction_shouldEnable() {
        when(mUserManager.hasUserRestriction(anyString())).thenReturn(false);
        mController.displayPreference(mScreen);

        mController.enablePreference(true);

        verify(mPreference).setEnabled(anyBoolean());
    }

}
