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

package com.android.settings.widget;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;

import androidx.preference.PreferenceViewHolder;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.android.settings.testutils.ResourcesUtils;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

/** Unittest for GearPreference */
@RunWith(AndroidJUnit4.class)
public class GearPreferenceTest {
    @Mock
    private GearPreference.OnGearClickListener mOnGearClickListener;

    private Context mContext = ApplicationProvider.getApplicationContext();
    private GearPreference mGearPreference;
    private PreferenceViewHolder mViewHolder;
    private View mGearView;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mGearPreference =
                new GearPreference(mContext, null);
        int layoutId = ResourcesUtils.getResourcesId(mContext, "layout", "preference_widget_gear");
        PreferenceViewHolder holder =
                PreferenceViewHolder.createInstanceForTests(
                        LayoutInflater.from(ApplicationProvider.getApplicationContext())
                                .inflate(layoutId, null));
        mViewHolder = spy(holder);
        mGearView = new View(mContext, null);
        int gearId = ResourcesUtils.getResourcesId(mContext, "id", "settings_button");
        when(mViewHolder.findViewById(gearId)).thenReturn(mGearView);
    }

    @Test
    public void onBindViewHolder_gearIsVisible() {
        mGearPreference.setOnGearClickListener(mOnGearClickListener);

        mGearPreference.onBindViewHolder(mViewHolder);

        assertThat(mGearView.getVisibility()).isEqualTo(View.VISIBLE);
    }

    @Test
    public void onBindViewHolder_gearIsGone() {
        mGearPreference.setOnGearClickListener(null);

        mGearPreference.onBindViewHolder(mViewHolder);

        assertThat(mGearView.getVisibility()).isEqualTo(View.GONE);
    }
}
