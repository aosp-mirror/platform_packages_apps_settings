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
package com.android.settings.deviceinfo;

import android.content.Context;
import android.os.Build;
import android.os.UserManager;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceScreen;

import com.android.settings.R;
import com.android.settings.testutils.SettingsRobolectricTestRunner;
import com.android.settings.TestConfig;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

import java.util.ArrayList;
import java.util.List;

import static com.google.common.truth.Truth.assertThat;
import static org.mockito.Answers.RETURNS_DEEP_STUBS;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(SettingsRobolectricTestRunner.class)
@Config(manifest = TestConfig.MANIFEST_PATH, sdk = TestConfig.SDK_VERSION)
public class SystemUpdatePreferenceControllerTest {

    @Mock(answer = RETURNS_DEEP_STUBS)
    private Context mContext;
    @Mock
    private UserManager mUserManager;
    @Mock(answer = RETURNS_DEEP_STUBS)
    private PreferenceScreen mScreen;

    private SystemUpdatePreferenceController mController;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mController = new SystemUpdatePreferenceController(mContext, mUserManager);
    }

    @Test
    public void updateNonIndexable_bothAvailable_shouldNotUpdate() {
        final List<String> keys = new ArrayList<>();
        when(mUserManager.isAdminUser()).thenReturn(true);
        when(mContext.getResources().getBoolean(
                R.bool.config_additional_system_update_setting_enable))
                .thenReturn(true);

        mController.updateNonIndexableKeys(keys);

        assertThat(keys).isEmpty();
    }

    @Test
    public void updateNonIndexable_nothingAvailable_shouldUpdateWith2Prefs() {
        final List<String> keys = new ArrayList<>();

        mController.updateNonIndexableKeys(keys);

        assertThat(keys.size()).isEqualTo(1);
    }

    @Test
    public void displayPrefs_nothingAvailable_shouldNotDisplay() {
        final Preference preference = mock(Preference.class);
        when(mScreen.getPreferenceCount()).thenReturn(1);
        when(mScreen.getPreference(0)).thenReturn(preference);
        when(preference.getKey()).thenReturn(mController.getPreferenceKey());

        mController.displayPreference(mScreen);

        verify(mScreen).removePreference(any(Preference.class));
    }

    @Test
    public void updateState_shouldSetToAndroidVersion() {
        final Preference preference = new Preference(RuntimeEnvironment.application);
        mController = new SystemUpdatePreferenceController(
                RuntimeEnvironment.application, mUserManager);
        mController.updateState(preference);

        assertThat(preference.getSummary())
                .isEqualTo(RuntimeEnvironment.application.getString(R.string.about_summary,
                        Build.VERSION.RELEASE));
    }

    @Test
    public void displayPrefs_oneAvailable_shouldDisplayOne() {
        final Preference preference = mock(Preference.class);
        when(mScreen.getPreferenceCount()).thenReturn(1);
        when(mScreen.getPreference(0)).thenReturn(preference);
        when(preference.getKey()).thenReturn(mController.getPreferenceKey());

        when(mContext.getResources().getBoolean(
                R.bool.config_additional_system_update_setting_enable))
                .thenReturn(true);

        mController.displayPreference(mScreen);

        verify(mScreen).removePreference(any(Preference.class));
    }
}
