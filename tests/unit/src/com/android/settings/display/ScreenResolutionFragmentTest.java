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
package com.android.settings.display;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

import android.content.Context;
import android.view.Display;

import androidx.test.annotation.UiThreadTest;
import androidx.test.core.app.ApplicationProvider;

import com.android.settingslib.widget.SelectorWithWidgetPreference;

import org.junit.Before;
import org.junit.Test;

public class ScreenResolutionFragmentTest {

    private Context mContext;
    private ScreenResolutionFragment mFragment;

    private static final int FHD_WIDTH = 1080;
    private static final int QHD_WIDTH = 1440;

    @Before
    @UiThreadTest
    public void setup() {
        mContext = spy(ApplicationProvider.getApplicationContext());
        mFragment = spy(new ScreenResolutionFragment());
    }

    @Test
    @UiThreadTest
    public void getDefaultKey_FHD() {
        Display.Mode mode = new Display.Mode(0, FHD_WIDTH, 0, 0);
        doReturn(mode).when(mFragment).getDisplayMode();
        doReturn(mContext).when(mFragment).getContext();

        mFragment.onAttach(mContext);
        assertThat(mFragment.getDefaultKey()).isEqualTo(mFragment.getKeyForResolution(FHD_WIDTH));
    }

    @Test
    @UiThreadTest
    public void getDefaultKey_QHD() {
        Display.Mode mode = new Display.Mode(0, QHD_WIDTH, 0, 0);
        doReturn(mode).when(mFragment).getDisplayMode();
        doReturn(mContext).when(mFragment).getContext();

        mFragment.onAttach(mContext);
        assertThat(mFragment.getDefaultKey()).isEqualTo(mFragment.getKeyForResolution(QHD_WIDTH));
    }

    @Test
    @UiThreadTest
    public void setDefaultKey_FHD() {
        doReturn(mContext).when(mFragment).getContext();
        mFragment.onAttach(mContext);

        mFragment.setDefaultKey(mFragment.getKeyForResolution(FHD_WIDTH));

        verify(mFragment).setDisplayMode(FHD_WIDTH);
    }

    @Test
    @UiThreadTest
    public void setDefaultKey_QHD() {
        doReturn(mContext).when(mFragment).getContext();
        mFragment.onAttach(mContext);

        mFragment.setDefaultKey(mFragment.getKeyForResolution(QHD_WIDTH));

        verify(mFragment).setDisplayMode(QHD_WIDTH);
    }

    @Test
    @UiThreadTest
    public void bindPreferenceExtra_setSummary() {
        doReturn(mContext).when(mFragment).getContext();
        mFragment.onAttach(mContext);
        SelectorWithWidgetPreference preference = new SelectorWithWidgetPreference(mContext);
        ScreenResolutionFragment.ScreenResolutionCandidateInfo candidates =
                mock(ScreenResolutionFragment.ScreenResolutionCandidateInfo.class);
        CharSequence summary = "test summary";
        doReturn(summary).when(candidates).loadSummary();

        mFragment.bindPreferenceExtra(preference, "com.example.test", candidates, null, null);

        assertThat(preference.getSummary().toString().contentEquals(summary)).isTrue();
    }
}
