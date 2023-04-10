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

package com.android.settings.biometrics2.ui.viewmodel;

import static com.google.common.truth.Truth.assertThat;

import android.app.Application;
import android.content.res.Configuration;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.android.settings.testutils.InstantTaskExecutorRule;
import com.android.systemui.unfold.compat.ScreenSizeFoldProvider;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public class DeviceFoldedViewModelTest {

    @Rule public final InstantTaskExecutorRule mTaskExecutorRule = new InstantTaskExecutorRule();

    private DeviceFoldedViewModel mViewModel;

    @Before
    public void setUp() {
        final Application application = ApplicationProvider.getApplicationContext();
        mViewModel = new DeviceFoldedViewModel(new ScreenSizeFoldProvider(application),
                application.getMainExecutor());
    }

    @Test
    public void testLiveData() {
        final Configuration config1 = new Configuration();
        config1.smallestScreenWidthDp = 601;
        mViewModel.onConfigurationChanged(config1);
        assertThat(mViewModel.getLiveData().getValue()).isFalse();

        final Configuration config2 = new Configuration();
        config2.smallestScreenWidthDp = 599;
        mViewModel.onConfigurationChanged(config2);
        assertThat(mViewModel.getLiveData().getValue()).isTrue();
    }
}
