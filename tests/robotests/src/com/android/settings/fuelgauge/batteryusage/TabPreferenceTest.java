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

package com.android.settings.fuelgauge.batteryusage;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;

import android.content.Context;

import androidx.fragment.app.Fragment;
import androidx.preference.Preference;

import com.android.settings.R;

import com.google.android.material.tabs.TabLayout;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

@RunWith(RobolectricTestRunner.class)
public final class TabPreferenceTest {

    private Context mContext;
    private TabPreference mTabPreference;

    @Mock
    private Fragment mMockFragment;
    @Mock
    private TabLayout mMockTabLayout;

    private final String[] mTabTitles = new String[]{"tab1", "tab2"};

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mContext = spy(RuntimeEnvironment.application);
        mTabPreference = new TabPreference(mContext, /*attrs=*/ null);
    }

    @Test
    public void constructor_returnExpectedResult() {
        assertThat(mTabPreference.getLayoutResource()).isEqualTo(R.layout.preference_tab);
    }

    @Test
    public void initializeTabs_returnExpectedResult() {
        mTabPreference.initializeTabs(mMockFragment, mTabTitles);
        assertThat(mTabPreference.mTabTitles).isEqualTo(mTabTitles);
    }

    @Test
    public void onSaveInstanceState_returnExpectedResult() {
        doReturn(1).when(mMockTabLayout).getSelectedTabPosition();
        mTabPreference.mTabLayout = mMockTabLayout;
        TabPreference.SavedState savedState =
                (TabPreference.SavedState) mTabPreference.onSaveInstanceState();
        assertThat(savedState.getTabPosition()).isEqualTo(1);
    }

    @Test
    public void onRestoreInstanceState_returnExpectedResult() {
        TabPreference.SavedState savedState =
                new TabPreference.SavedState(Preference.BaseSavedState.EMPTY_STATE, 2);
        mTabPreference.onRestoreInstanceState(savedState);
        assertThat(mTabPreference.mSavedTabPosition).isEqualTo(2);
    }
}
