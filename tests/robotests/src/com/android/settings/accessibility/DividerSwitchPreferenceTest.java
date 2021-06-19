/*
 * Copyright (C) 2020 The Android Open Source Project
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

package com.android.settings.accessibility;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.internal.verification.VerificationModeFactory.times;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;

import androidx.preference.PreferenceViewHolder;

import com.android.settings.R;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

/** Tests for {@link DividerSwitchPreference}. */
@RunWith(RobolectricTestRunner.class)
public class DividerSwitchPreferenceTest {
    private final Context mContext = RuntimeEnvironment.application;
    private View mRootView;
    private PreferenceViewHolder mViewHolder;
    private DividerSwitchPreference mDividerSwitchPreference;

    @Before
    public void setUp() {
        initRootView();
        initPreference();
    }

    @Test
    public void setDividerAllowedAbove_allowed_success() {
        mDividerSwitchPreference.setDividerAllowedAbove(true);
        mDividerSwitchPreference.onBindViewHolder(mViewHolder);

        // One time was in parent, the other time was in child.
        verify(mViewHolder, times(2)).setDividerAllowedAbove(true);
    }

    @Test
    public void setDividerAllowedBelow_allowed_success() {
        mDividerSwitchPreference.setDividerAllowedBelow(true);
        mDividerSwitchPreference.onBindViewHolder(mViewHolder);

        // One time was in parent, the other time was in child.
        verify(mViewHolder, times(2)).setDividerAllowedBelow(true);
    }

    @Test
    public void setSwitchVisibility_visible_success() {
        final View view = spy(new View(mContext));
        doReturn(view).when(mRootView).findViewById(android.R.id.widget_frame);

        mDividerSwitchPreference.setSwitchVisibility(View.VISIBLE);
        mDividerSwitchPreference.onBindViewHolder(mViewHolder);

        assertThat(view.getVisibility()).isEqualTo(View.VISIBLE);
    }

    private void initRootView() {
        final LayoutInflater inflater = LayoutInflater.from(mContext);
        mRootView = spy(inflater.inflate(R.layout.preference_widget_switch, /* root= */ null));
        mViewHolder = spy(PreferenceViewHolder.createInstanceForTests(mRootView));
    }

    private void initPreference() {
        mDividerSwitchPreference = new DividerSwitchPreference(mContext);
        mDividerSwitchPreference.setDividerAllowedAbove(false);
        mDividerSwitchPreference.setDividerAllowedBelow(false);
        mDividerSwitchPreference.setSwitchVisibility(View.INVISIBLE);
    }
}
