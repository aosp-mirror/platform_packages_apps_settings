/*
 * Copyright (C) 2018 The Android Open Source Project
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

package com.android.settings.slices;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

import android.content.Context;
import android.net.Uri;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.Observer;
import androidx.slice.Slice;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

@RunWith(RobolectricTestRunner.class)
public class SlicePreferenceControllerTest {
    private static final String KEY = "slice_preference_key";

    @Mock
    private LiveData<Slice> mLiveData;
    @Mock
    private SlicePreference mSlicePreference;

    private Context mContext;
    private SlicePreferenceController mController;
    private Uri mUri;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);

        mContext = spy(RuntimeEnvironment.application);
        mController = new SlicePreferenceController(mContext, KEY);
        mController.mLiveData = mLiveData;
        mController.mSlicePreference = mSlicePreference;
        mUri = Uri.EMPTY;
    }

    @Test
    public void isAvailable_uriNull_returnFalse() {
        assertThat(mController.isAvailable()).isFalse();
    }

    @Test
    public void isAvailable_uriNotNull_returnTrue() {
        mController.setSliceUri(mUri);
        assertThat(mController.isAvailable()).isTrue();
    }

    @Test
    public void onStart_registerObserver() {
        mController.onStart();
        verify(mLiveData).observeForever(mController);
    }

    @Test
    public void onStop_unregisterObserver() {
        mController.onStart();

        mController.onStop();
        verify(mLiveData).removeObserver(mController);
    }

    @Test
    public void onStop_unregisterObserverAndHasSecurityException_noCrash() {
        LiveData<Slice> liveData = new LiveData<Slice>() {
            @Override
            public void removeObserver(@NonNull Observer<? super Slice> observer) {
                super.removeObserver(observer);
                throw new SecurityException("SecurityException Test");
            }
        };
        mController.mLiveData = liveData;
        mController.onStart();

        mController.onStop();
    }

    @Test
    public void onChanged_nullSlice_updateSlice() {
        mController.onChanged(null);

        verify(mController.mSlicePreference).onSliceUpdated(null);
    }
}