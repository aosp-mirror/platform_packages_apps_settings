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
import android.view.Surface;

import androidx.annotation.NonNull;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.android.settings.testutils.InstantTaskExecutorRule;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

@RunWith(AndroidJUnit4.class)
public class DeviceRotationViewModelTest {

    @Rule public final MockitoRule mockito = MockitoJUnit.rule();
    @Rule public final InstantTaskExecutorRule mTaskExecutorRule = new InstantTaskExecutorRule();

    private TestDeviceRotationViewModel mViewModel;

    @Before
    public void setUp() {
        TestDeviceRotationViewModel.sTestRotation = 3;
        mViewModel = new TestDeviceRotationViewModel(ApplicationProvider.getApplicationContext());
    }

    @Test
    public void testDefaultLiveDataNotNull() {
        assertThat(mViewModel.getLiveData().getValue()).isEqualTo(mViewModel.sTestRotation);
    }

    @Test
    public void testOnDisplayChange() {
        mViewModel.sTestRotation = 3;
        mViewModel.triggerOnDisplayChanged();
        assertThat(mViewModel.getLiveData().getValue()).isEqualTo(mViewModel.sTestRotation);
    }

    public static class TestDeviceRotationViewModel extends DeviceRotationViewModel {

        @Surface.Rotation static int sTestRotation = 0;

        public TestDeviceRotationViewModel(@NonNull Application application) {
            super(application);
        }

        void triggerOnDisplayChanged() {
            mDisplayListener.onDisplayChanged(0);
        }

        @Override
        protected int getRotation() {
            return sTestRotation;
        }
    }
}
