/*
 * Copyright (C) 2019 The Android Open Source Project
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

package com.android.settings.privacy;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.app.ActionBar;
import android.content.Context;
import android.os.Bundle;
import android.view.View;

import androidx.fragment.app.FragmentActivity;
import androidx.recyclerview.widget.RecyclerView;

import com.android.settingslib.core.lifecycle.Lifecycle;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

@RunWith(RobolectricTestRunner.class)
public class PrivacyDashboardFragmentTest {

    private Context mContext;
    private PrivacyDashboardFragment mFragment;

    @Before
    public void setUp() {
        mContext = RuntimeEnvironment.application;
        mFragment = spy(new PrivacyDashboardFragment());
    }

    @Test
    public void onViewCreated_shouldCallStyleActionBar() {
        final FragmentActivity activity = spy(
                Robolectric.buildActivity(FragmentActivity.class).get());
        final ActionBar actionBar = mock(ActionBar.class);

        when(mFragment.getActivity()).thenReturn(activity);
        when(mFragment.getSettingsLifecycle()).thenReturn(mock(Lifecycle.class));
        when(mFragment.getListView()).thenReturn(mock(RecyclerView.class));
        when(activity.getActionBar()).thenReturn(actionBar);

        mFragment.onViewCreated(new View(mContext), new Bundle());

        verify(mFragment).styleActionBar();
    }
}
