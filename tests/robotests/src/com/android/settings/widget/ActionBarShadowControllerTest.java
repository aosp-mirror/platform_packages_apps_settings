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

package com.android.settings.widget;


import android.app.ActionBar;
import android.app.Activity;
import android.support.v7.widget.RecyclerView;

import com.android.settings.testutils.SettingsRobolectricTestRunner;
import com.android.settings.TestConfig;
import com.android.settingslib.core.lifecycle.Lifecycle;
import com.android.settingslib.core.lifecycle.LifecycleObserver;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.annotation.Config;
import org.robolectric.util.ReflectionHelpers;

import java.util.List;

import static com.google.common.truth.Truth.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(SettingsRobolectricTestRunner.class)
@Config(manifest = TestConfig.MANIFEST_PATH, sdk = TestConfig.SDK_VERSION)
public class ActionBarShadowControllerTest {

    @Mock
    private RecyclerView mRecyclerView;
    @Mock
    private Activity mActivity;
    @Mock
    private ActionBar mActionBar;
    private Lifecycle mLifecycle;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        when(mActivity.getActionBar()).thenReturn(mActionBar);
        mLifecycle = new Lifecycle();
    }

    @Test
    public void attachToRecyclerView_shouldAddScrollWatcherAndUpdateActionBar() {
        when(mRecyclerView.canScrollVertically(-1)).thenReturn(false);

        ActionBarShadowController.attachToRecyclerView(mActivity, mLifecycle, mRecyclerView);

        verify(mActionBar).setElevation(0);
    }


    @Test
    public void attachToRecyclerView_lifecycleChange_shouldAttachDetach() {
        ActionBarShadowController.attachToRecyclerView(mActivity, mLifecycle, mRecyclerView);

        List<LifecycleObserver> observers = ReflectionHelpers.getField(mLifecycle, "mObservers");
        assertThat(observers).hasSize(1);
        verify(mRecyclerView).addOnScrollListener(any());

        mLifecycle.onStop();
        verify(mRecyclerView).removeOnScrollListener(any());

        mLifecycle.onStart();
        verify(mRecyclerView, times(2)).addOnScrollListener(any());
    }

}
