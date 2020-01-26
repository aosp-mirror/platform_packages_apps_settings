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

package com.android.settings.datausage;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Mockito.when;

import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ProgressBar;

import androidx.preference.PreferenceViewHolder;

import com.android.settingslib.AppItem;
import com.android.settingslib.net.UidDetail;
import com.android.settingslib.net.UidDetailProvider;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

@RunWith(RobolectricTestRunner.class)
public class AppDataUsagePreferenceTest {
    private static final String FIFTY_PERCENT = "50%";

    @Mock
    private UidDetailProvider mUidDetailProvider;
    private AppItem mAppItem;
    private UidDetail mUidDetail;

    private AppDataUsagePreference mPreference;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mAppItem = new AppItem(123);
        mUidDetail = new UidDetail();
        mUidDetail.icon = new ColorDrawable(Color.BLUE);
        mUidDetail.label = "title";
    }

    @Test
    public void createPref_hasCachedUidDetail_shouldSetAppInfo() {
        when(mUidDetailProvider.getUidDetail(mAppItem.key, false /* blocking */))
                .thenReturn(mUidDetail);

        mPreference = new AppDataUsagePreference(RuntimeEnvironment.application, mAppItem,
                50 /* percent */, mUidDetailProvider);

        assertThat(mPreference.getTitle()).isEqualTo(mUidDetail.label);
        assertThat(mPreference.getIcon()).isEqualTo(mUidDetail.icon);
    }

    @Test
    public void createPref_noCachedUidDetail_shouldSetAppInfo() {
        when(mUidDetailProvider.getUidDetail(mAppItem.key, true /* blocking */))
                .thenReturn(mUidDetail);

        mPreference = new AppDataUsagePreference(RuntimeEnvironment.application, mAppItem,
                50 /* percent */, mUidDetailProvider);

        assertThat(mPreference.getTitle()).isEqualTo(mUidDetail.label);
        assertThat(mPreference.getIcon()).isEqualTo(mUidDetail.icon);
    }

    @Test
    public void createPref_progressBarShouldSetPercentageContentDescription() {
        when(mUidDetailProvider.getUidDetail(mAppItem.key, true /* blocking */))
                .thenReturn(mUidDetail);
        mPreference = new AppDataUsagePreference(RuntimeEnvironment.application, mAppItem,
                50 /* percent */, mUidDetailProvider);
        final View view = LayoutInflater.from(RuntimeEnvironment.application).inflate(
                com.android.settingslib.R.layout.preference_app, null);
        final PreferenceViewHolder preferenceViewHolder =
                PreferenceViewHolder.createInstanceForTests(view);
        final ProgressBar progressBar = (ProgressBar) preferenceViewHolder.findViewById(
                android.R.id.progress);

        mPreference.onBindViewHolder(preferenceViewHolder);

        assertThat(progressBar.getContentDescription()).isEqualTo(FIFTY_PERCENT);
    }
}
