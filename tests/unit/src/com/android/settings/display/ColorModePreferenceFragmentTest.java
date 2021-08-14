/*
 * Copyright (C) 2021 The Android Open Source Project
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

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.content.Context;
import android.content.res.Resources;
import android.hardware.display.ColorDisplayManager;

import androidx.test.annotation.UiThreadTest;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.android.internal.logging.nano.MetricsProto;
import com.android.settingslib.widget.CandidateInfo;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.ArrayList;
import java.util.List;

@RunWith(AndroidJUnit4.class)
public class ColorModePreferenceFragmentTest {

    private ColorModePreferenceFragment mFragment;

    private Context mContext;

    @Before
    @UiThreadTest
    public void setup() {
        mContext = spy(ApplicationProvider.getApplicationContext());
        mFragment = spy(new ColorModePreferenceFragment());
        doNothing().when(mFragment).setColorMode(anyInt());
    }

    @Test
    public void verifyMetricsConstant() {
        assertThat(mFragment.getMetricsCategory())
                .isEqualTo(MetricsProto.MetricsEvent.COLOR_MODE_SETTINGS);
    }

    @Test
    @UiThreadTest
    public void getCandidates_all() {
        final Resources res = spy(mContext.getResources());
        when(res.getIntArray(com.android.internal.R.array.config_availableColorModes)).thenReturn(
                new int[]{
                        ColorDisplayManager.COLOR_MODE_NATURAL,
                        ColorDisplayManager.COLOR_MODE_BOOSTED,
                        ColorDisplayManager.COLOR_MODE_SATURATED,
                        ColorDisplayManager.COLOR_MODE_AUTOMATIC
                });
        doReturn(res).when(mContext).getResources();
        mFragment.onAttach(mContext);

        final List<? extends CandidateInfo> candidates = mFragment.getCandidates();

        assertThat(candidates.size()).isEqualTo(4);
        assertThat(candidates.get(0).getKey())
                .isEqualTo(mFragment.getKeyForColorMode(ColorDisplayManager.COLOR_MODE_NATURAL));
        assertThat(candidates.get(1).getKey())
                .isEqualTo(mFragment.getKeyForColorMode(ColorDisplayManager.COLOR_MODE_BOOSTED));
        assertThat(candidates.get(2).getKey())
                .isEqualTo(mFragment.getKeyForColorMode(ColorDisplayManager.COLOR_MODE_SATURATED));
        assertThat(candidates.get(3).getKey())
                .isEqualTo(mFragment.getKeyForColorMode(ColorDisplayManager.COLOR_MODE_AUTOMATIC));
    }

    @Test
    @UiThreadTest
    public void getCandidates_none() {
        final Resources res = spy(mContext.getResources());
        when(res.getIntArray(com.android.internal.R.array.config_availableColorModes)).thenReturn(
                new int[]{
                });
        doReturn(res).when(mContext).getResources();
        mFragment.onAttach(mContext);

        List<? extends CandidateInfo> candidates = mFragment.getCandidates();

        assertThat(candidates.size()).isEqualTo(0);
    }

    @Test
    @UiThreadTest
    public void getCandidates_withAutomatic() {
        final Resources res = spy(mContext.getResources());
        when(res.getIntArray(com.android.internal.R.array.config_availableColorModes)).thenReturn(
                new int[]{
                        ColorDisplayManager.COLOR_MODE_NATURAL,
                        ColorDisplayManager.COLOR_MODE_AUTOMATIC
                });
        doReturn(res).when(mContext).getResources();
        mFragment.onAttach(mContext);

        List<? extends CandidateInfo> candidates = mFragment.getCandidates();

        assertThat(candidates.size()).isEqualTo(2);
        assertThat(candidates.get(0).getKey())
                .isEqualTo(mFragment.getKeyForColorMode(ColorDisplayManager.COLOR_MODE_NATURAL));
        assertThat(candidates.get(1).getKey())
                .isEqualTo(mFragment.getKeyForColorMode(ColorDisplayManager.COLOR_MODE_AUTOMATIC));
    }

    @Test
    @UiThreadTest
    public void getCandidates_withoutAutomatic() {
        final Resources res = spy(mContext.getResources());
        when(res.getIntArray(com.android.internal.R.array.config_availableColorModes)).thenReturn(
                new int[]{
                        ColorDisplayManager.COLOR_MODE_NATURAL,
                        ColorDisplayManager.COLOR_MODE_BOOSTED,
                        ColorDisplayManager.COLOR_MODE_SATURATED
                });
        doReturn(res).when(mContext).getResources();
        mFragment.onAttach(mContext);

        List<? extends CandidateInfo> candidates = mFragment.getCandidates();

        assertThat(candidates.size()).isEqualTo(3);
        assertThat(candidates.get(0).getKey())
                .isEqualTo(mFragment.getKeyForColorMode(ColorDisplayManager.COLOR_MODE_NATURAL));
        assertThat(candidates.get(1).getKey())
                .isEqualTo(mFragment.getKeyForColorMode(ColorDisplayManager.COLOR_MODE_BOOSTED));
        assertThat(candidates.get(2).getKey())
                .isEqualTo(mFragment.getKeyForColorMode(ColorDisplayManager.COLOR_MODE_SATURATED));
    }

    @Test
    @UiThreadTest
    public void getKey_natural() {
        doReturn(ColorDisplayManager.COLOR_MODE_NATURAL).when(mFragment).getColorMode();
        mFragment.onAttach(mContext);

        assertThat(mFragment.getDefaultKey())
                .isEqualTo(mFragment.getKeyForColorMode(ColorDisplayManager.COLOR_MODE_NATURAL));
    }

    @Test
    @UiThreadTest
    public void getKey_boosted() {
        doReturn(ColorDisplayManager.COLOR_MODE_BOOSTED).when(mFragment).getColorMode();
        mFragment.onAttach(mContext);

        assertThat(mFragment.getDefaultKey())
                .isEqualTo(mFragment.getKeyForColorMode(ColorDisplayManager.COLOR_MODE_BOOSTED));
    }

    @Test
    @UiThreadTest
    public void getKey_saturated() {
        doReturn(ColorDisplayManager.COLOR_MODE_SATURATED).when(mFragment).getColorMode();
        mFragment.onAttach(mContext);

        assertThat(mFragment.getDefaultKey())
                .isEqualTo(mFragment.getKeyForColorMode(ColorDisplayManager.COLOR_MODE_SATURATED));
    }

    @Test
    @UiThreadTest
    public void getKey_automatic() {
        doReturn(ColorDisplayManager.COLOR_MODE_AUTOMATIC).when(mFragment).getColorMode();
        mFragment.onAttach(mContext);

        assertThat(mFragment.getDefaultKey())
                .isEqualTo(mFragment.getKeyForColorMode(ColorDisplayManager.COLOR_MODE_AUTOMATIC));
    }

    @Test
    @UiThreadTest
    public void setKey_natural() {
        mFragment.onAttach(mContext);

        mFragment.setDefaultKey(
                mFragment.getKeyForColorMode(ColorDisplayManager.COLOR_MODE_NATURAL));

        verify(mFragment).setColorMode(ColorDisplayManager.COLOR_MODE_NATURAL);
    }

    @Test
    @UiThreadTest
    public void setKey_boosted() {
        mFragment.onAttach(mContext);

        mFragment.setDefaultKey(
                mFragment.getKeyForColorMode(ColorDisplayManager.COLOR_MODE_BOOSTED));

        verify(mFragment).setColorMode(ColorDisplayManager.COLOR_MODE_BOOSTED);
    }

    @Test
    @UiThreadTest
    public void setKey_saturated() {
        mFragment.onAttach(mContext);

        mFragment.setDefaultKey(
                mFragment.getKeyForColorMode(ColorDisplayManager.COLOR_MODE_SATURATED));

        verify(mFragment).setColorMode(ColorDisplayManager.COLOR_MODE_SATURATED);
    }

    @Test
    @UiThreadTest
    public void setKey_automatic() {
        mFragment.onAttach(mContext);

        mFragment.setDefaultKey(
                mFragment.getKeyForColorMode(ColorDisplayManager.COLOR_MODE_AUTOMATIC));

        verify(mFragment).setColorMode(ColorDisplayManager.COLOR_MODE_AUTOMATIC);
    }

    @Test
    @UiThreadTest
    public void checkViewPagerTotalCount() throws Throwable {
        final ArrayList<Integer> viewPagerResList = mFragment.getViewPagerResource();

        assertThat(viewPagerResList.size()).isEqualTo(3);
        for (int idx = 0; idx < viewPagerResList.size(); idx++) {
            assertThat(viewPagerResList.get(idx) > 0).isTrue();
        }
    }
}
