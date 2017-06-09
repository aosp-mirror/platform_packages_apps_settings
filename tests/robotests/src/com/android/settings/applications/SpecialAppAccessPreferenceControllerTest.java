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

package com.android.settings.applications;

import static com.google.common.truth.Truth.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.content.Context;
import android.support.v7.preference.Preference;
import com.android.settings.R;
import com.android.settings.testutils.SettingsRobolectricTestRunner;
import com.android.settings.TestConfig;
import com.android.settings.datausage.DataSaverBackend;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import org.robolectric.util.ReflectionHelpers;

@RunWith(SettingsRobolectricTestRunner.class)
@Config(manifest = TestConfig.MANIFEST_PATH, sdk = TestConfig.SDK_VERSION)
public class SpecialAppAccessPreferenceControllerTest {
    private Context mContext;
    @Mock
    private DataSaverBackend mBackend;
    @Mock
    private Preference mPreference;

    private SpecialAppAccessPreferenceController mController;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mContext = RuntimeEnvironment.application;
        mController = new SpecialAppAccessPreferenceController(mContext);
        ReflectionHelpers.setField(mController, "mDataSaverBackend", mBackend);
    }

    @Test
    public void isAvailable_shouldAlwaysReturnTrue() {
        assertThat(mController.isAvailable()).isTrue();
    }

    @Test
    public void updateState_shouldSetSummary() {
        when(mBackend.getWhitelistedCount()).thenReturn(0);

        mController.updateState(mPreference);

        verify(mPreference).setSummary(mContext.getResources().getQuantityString(
            R.plurals.special_access_summary, 0, 0));

        when(mBackend.getWhitelistedCount()).thenReturn(1);

        mController.updateState(mPreference);

        verify(mPreference).setSummary(mContext.getResources().getQuantityString(
            R.plurals.special_access_summary, 1, 1));
    }
}
