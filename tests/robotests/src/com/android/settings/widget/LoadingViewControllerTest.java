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

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import android.content.Context;
import android.os.Handler;
import android.view.View;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.util.ReflectionHelpers;

@RunWith(RobolectricTestRunner.class)
public class LoadingViewControllerTest {

    private Context mContext;
    private View mLoadingView;
    private View mContentView;

    private LoadingViewController mController;

    @Before
    public void setUp() {
        mContext = RuntimeEnvironment.application;
        mLoadingView = new View(mContext);
        mContentView = new View(mContext);

        mController = new LoadingViewController(mLoadingView, mContentView);
    }

    @Test
    public void showContent_shouldSetContentVisible() {
        mController.showContent(false /* animate */);

        assertThat(mContentView.getVisibility()).isEqualTo(View.VISIBLE);
    }

    @Test
    public void showLoadingViewDelayed_shouldPostRunnable() {
        final Handler handler = mock(Handler.class);
        ReflectionHelpers.setField(mController, "mFgHandler", handler);
        mController.showLoadingViewDelayed();

        verify(handler).postDelayed(any(Runnable.class), anyLong());
    }
}
