/*
 * Copyright (C) 2022 The Android Open Source Project
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

package com.android.settings.users;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;

import android.content.Context;

import androidx.fragment.app.FragmentActivity;
import androidx.test.core.app.ApplicationProvider;

import com.android.settings.R;
import com.android.settings.testutils.shadow.ShadowFragment;
import com.android.settingslib.widget.CandidateInfo;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.util.List;

@RunWith(RobolectricTestRunner.class)
@Config(shadows = {ShadowFragment.class})
public class TimeoutToDockUserSettingsTest {
    @Mock
    private FragmentActivity mActivity;

    private TimeoutToDockUserSettings mSettings;

    private String[] mEntries;
    private String[] mValues;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);

        final Context context = spy(ApplicationProvider.getApplicationContext());
        mEntries = context.getResources().getStringArray(
                R.array.switch_to_dock_user_when_docked_timeout_entries);
        mValues = context.getResources().getStringArray(
                R.array.switch_to_dock_user_when_docked_timeout_values);
        mSettings = spy(new TimeoutToDockUserSettings());

        doReturn(context).when(mSettings).getContext();
        doReturn(mActivity).when(mSettings).getActivity();

        mSettings.onAttach(context);
    }

    @Test
    public void getCandidates_returnsACandidateForEachEntry() {
        final List<? extends CandidateInfo> candidates = mSettings.getCandidates();

        for (int i = 0; i < mEntries.length; i++) {
            assertThat(candidates.get(i).loadLabel().toString()).isEqualTo(mEntries[i]);
        }
    }

    @Test
    public void defaultKey_settingNotSet_shouldReturnSecondValueAsDefault() {
        assertThat(mSettings.getDefaultKey()).isEqualTo(
                mValues[TimeoutToDockUserSettings.DEFAULT_TIMEOUT_SETTING_VALUE_INDEX]);
    }

    @Test
    public void defaultKey_setToFirstValue_shouldSaveToSettings() {
        final String expectedKey = mValues[0];
        mSettings.setDefaultKey(expectedKey);
        assertThat(mSettings.getDefaultKey()).isEqualTo(expectedKey);
    }

    @Test
    public void defaultKey_setToSecondValue_shouldSaveToSettings() {
        final String expectedKey = mValues[1];
        mSettings.setDefaultKey(expectedKey);
        assertThat(mSettings.getDefaultKey()).isEqualTo(expectedKey);
    }

    @Test
    public void defaultKey_setToThirdValue_shouldSaveToSettings() {
        final String expectedKey = mValues[2];
        mSettings.setDefaultKey(expectedKey);
        assertThat(mSettings.getDefaultKey()).isEqualTo(expectedKey);
    }
}
