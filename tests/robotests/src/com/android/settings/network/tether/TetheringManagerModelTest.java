/*
 * Copyright (C) 2023 The Android Open Source Project
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

package com.android.settings.network.tether;

import static android.net.TetheringManager.TETHERING_WIFI;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.app.Application;
import android.net.TetheringManager;

import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import java.util.List;
import java.util.concurrent.Executor;

@RunWith(AndroidJUnit4.class)
public class TetheringManagerModelTest {
    @Rule
    public final MockitoRule mMockitoRule = MockitoJUnit.rule();
    @Mock
    Application mApplication;
    @Mock
    Executor mExecutor;
    @Mock
    TetheringManager mTetheringManager;
    @Mock
    List<String> mInterfaces;

    TetheringManagerModel mModel;

    @Before
    public void setUp() {
        when(mApplication.getMainExecutor()).thenReturn(mExecutor);
        when(mApplication.getSystemService(TetheringManager.class)).thenReturn(mTetheringManager);

        mModel = new TetheringManagerModel(mApplication);
    }

    @Test
    public void constructor_registerCallback() {
        verify(mTetheringManager).registerTetheringEventCallback(any(), eq(mModel.mEventCallback));
    }

    @Test
    public void onCleared_unregisterCallback() {
        mModel.onCleared();

        verify(mTetheringManager).unregisterTetheringEventCallback(eq(mModel.mEventCallback));
    }

    @Test
    public void getTetheringManager_isNotNull() {
        assertThat(mModel.getTetheringManager()).isNotNull();
    }

    @Test
    public void getTetheredInterfaces_isNotNull() {
        assertThat(mModel.getTetheredInterfaces()).isNotNull();
    }

    @Test
    public void onTetheredInterfacesChanged_updateTetheredInterfaces() {
        mModel.mTetheredInterfaces.setValue(null);

        mModel.mEventCallback.onTetheredInterfacesChanged(mInterfaces);

        assertThat(mModel.mTetheredInterfaces.getValue()).isEqualTo(mInterfaces);
    }

    @Test
    public void startTethering_startTetheringToTetheringManager() {
        mModel.startTethering(TETHERING_WIFI);

        verify(mTetheringManager).startTethering(eq(TETHERING_WIFI), any(), any());
    }

    @Test
    public void stopTethering_stopTetheringToTetheringManager() {
        mModel.stopTethering(TETHERING_WIFI);

        verify(mTetheringManager).stopTethering(eq(TETHERING_WIFI));
    }
}
