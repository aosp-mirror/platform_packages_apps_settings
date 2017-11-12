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

import static com.google.common.truth.Truth.assertThat;
import static org.mockito.Answers.RETURNS_DEEP_STUBS;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.when;

import android.content.Context;
import android.os.UserManager;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceScreen;

import com.android.settings.TestConfig;
import com.android.settings.testutils.SettingsRobolectricTestRunner;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

/**
 * deprecated in favor of {@link BugReportPreferenceControllerV2}
 */
@Deprecated
@RunWith(SettingsRobolectricTestRunner.class)
@Config(manifest = TestConfig.MANIFEST_PATH, sdk = TestConfig.SDK_VERSION_O)
public class BugReportPreferenceControllerTest {

    @Mock
    private Context mContext;
    @Mock(answer = RETURNS_DEEP_STUBS)
    private PreferenceScreen mScreen;
    @Mock
    private UserManager mUserManager;

    private BugReportPreferenceController mController;
    private Preference mPreference;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        when(mContext.getSystemService(Context.USER_SERVICE)).thenReturn(mUserManager);
        mController = new BugReportPreferenceController(mContext);
        mPreference = new Preference(RuntimeEnvironment.application);
        mPreference.setKey(mController.getPreferenceKey());
        when(mScreen.findPreference(mPreference.getKey())).thenReturn(mPreference);
    }

    @Test
    public void displayPreference_hasDebugRestriction_shouldRemovePreference() {
        when(mUserManager.hasUserRestriction(anyString())).thenReturn(true);

        mController.displayPreference(mScreen);

        assertThat(mPreference.isVisible()).isFalse();
    }

    @Test
    public void displayPreference_noDebugRestriction_shouldNotRemovePreference() {
        when(mUserManager.hasUserRestriction(anyString())).thenReturn(false);

        mController.displayPreference(mScreen);

        assertThat(mPreference.isVisible()).isTrue();
    }

    @Test
    public void enablePreference_hasDebugRestriction_shouldNotEnable() {
        when(mUserManager.hasUserRestriction(anyString())).thenReturn(true);
        mController.displayPreference(mScreen);

        mPreference.setEnabled(false);
        mController.enablePreference(true);

        assertThat(mPreference.isEnabled()).isFalse();
    }

    @Test
    public void enablePreference_noDebugRestriction_shouldEnable() {
        when(mUserManager.hasUserRestriction(anyString())).thenReturn(false);
        mController.displayPreference(mScreen);

        mPreference.setEnabled(false);
        mController.enablePreference(true);

        assertThat(mPreference.isEnabled()).isTrue();
    }

}
