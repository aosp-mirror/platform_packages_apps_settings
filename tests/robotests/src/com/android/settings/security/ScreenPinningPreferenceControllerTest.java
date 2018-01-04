/*
 * Copyright (C) 2018 The Android Open Source Project
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

package com.android.settings.security;

import static com.android.settings.core.BasePreferenceController.AVAILABLE;
import static com.google.common.truth.Truth.assertThat;
import static org.mockito.Mockito.when;

import android.content.Context;
import android.provider.Settings;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceScreen;

import com.android.settings.R;
import com.android.settings.TestConfig;
import com.android.settings.testutils.FakeFeatureFactory;
import com.android.settings.testutils.SettingsRobolectricTestRunner;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

@RunWith(SettingsRobolectricTestRunner.class)
@Config(manifest = TestConfig.MANIFEST_PATH, sdk = TestConfig.SDK_VERSION)
public class ScreenPinningPreferenceControllerTest {

    @Mock
    private PreferenceScreen mScreen;

    private FakeFeatureFactory mFeatureFactory;
    private Context mContext;
    private ScreenPinningPreferenceController mController;
    private Preference mPreference;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mContext = RuntimeEnvironment.application;
        mFeatureFactory = FakeFeatureFactory.setupForTest();
        mController = new ScreenPinningPreferenceController(mContext);
        mPreference = new Preference(mContext);
        mPreference.setKey(mController.getPreferenceKey());
        when(mScreen.findPreference(mController.getPreferenceKey()))
                .thenReturn(mPreference);
    }

    @After
    public void tearDown() {
        Settings.System.putInt(mContext.getContentResolver(),
                Settings.System.LOCK_TO_APP_ENABLED, 0);
    }

    @Test
    public void isAlwaysAvailable() {
        assertThat(mController.getAvailabilityStatus()).isEqualTo(AVAILABLE);
    }

    @Test
    public void displayPreference_isOff_shouldDisableOffSummary() {
        Settings.System.putInt(mContext.getContentResolver(),
                Settings.System.LOCK_TO_APP_ENABLED, 0);

        mController.displayPreference(mScreen);

        assertThat(mPreference.getSummary())
                .isEqualTo(mContext.getString(R.string.switch_off_text));
    }

    @Test
    public void displayPreference_isOn_shouldDisableOnSummary() {
        Settings.System.putInt(mContext.getContentResolver(),
                Settings.System.LOCK_TO_APP_ENABLED, 1);

        mController.displayPreference(mScreen);

        assertThat(mPreference.getSummary())
                .isEqualTo(mContext.getString(R.string.switch_on_text));
    }

}
