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

package com.android.settings.biometrics2.ui.viewmodel;

import static com.android.settings.biometrics2.ui.viewmodel.FingerprintEnrollmentViewModel.SAVED_STATE_IS_NEW_FINGERPRINT_ADDED;
import static com.android.settings.biometrics2.ui.viewmodel.FingerprintEnrollmentViewModel.SAVED_STATE_IS_WAITING_ACTIVITY_RESULT;
import static com.android.settings.biometrics2.utils.EnrollmentRequestUtils.newAllFalseRequest;

import static com.google.common.truth.Truth.assertThat;

import android.app.Application;
import android.hardware.fingerprint.FingerprintManager;
import android.os.Bundle;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.android.settings.biometrics2.data.repository.FingerprintRepository;
import com.android.settings.testutils.InstantTaskExecutorRule;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

@RunWith(AndroidJUnit4.class)
public class FingerprintEnrollmentViewModelTest {

    @Rule public final MockitoRule mockito = MockitoJUnit.rule();
    @Rule public final InstantTaskExecutorRule mTaskExecutorRule = new InstantTaskExecutorRule();

    @Mock private FingerprintManager mFingerprintManager;

    private Application mApplication;
    private FingerprintRepository mFingerprintRepository;
    private FingerprintEnrollmentViewModel mViewModel;

    @Before
    public void setUp() {
        mApplication = ApplicationProvider.getApplicationContext();
        mFingerprintRepository = new FingerprintRepository(mFingerprintManager);
        mViewModel = new FingerprintEnrollmentViewModel(mApplication, mFingerprintRepository,
                newAllFalseRequest(mApplication));
    }

    @Test
    public void testGetRequest() {
        assertThat(mViewModel.getRequest()).isNotNull();
    }

    @Test
    public void testSetSavedInstanceState() {
        // setSavedInstanceState() as false
        final Bundle bundle = new Bundle();
        bundle.putBoolean(SAVED_STATE_IS_WAITING_ACTIVITY_RESULT, false);
        mViewModel.setSavedInstanceState(bundle);
        assertThat(mViewModel.isWaitingActivityResult().get()).isFalse();

        // setSavedInstanceState() as false
        bundle.clear();
        bundle.putBoolean(SAVED_STATE_IS_WAITING_ACTIVITY_RESULT, true);
        mViewModel.setSavedInstanceState(bundle);
        assertThat(mViewModel.isWaitingActivityResult().get()).isTrue();

        // setSavedInstanceState() as false
        bundle.clear();
        bundle.putBoolean(SAVED_STATE_IS_NEW_FINGERPRINT_ADDED, false);
        mViewModel.setSavedInstanceState(bundle);
        assertThat(mViewModel.isNewFingerprintAdded()).isFalse();

        // setSavedInstanceState() as false
        bundle.clear();
        bundle.putBoolean(SAVED_STATE_IS_NEW_FINGERPRINT_ADDED, true);
        mViewModel.setSavedInstanceState(bundle);
        assertThat(mViewModel.isNewFingerprintAdded()).isTrue();
    }

    @Test
    public void testOnSaveInstanceState() {
        // Test isWaitingActivityResult false
        mViewModel.isWaitingActivityResult().set(false);
        final Bundle bundle = new Bundle();
        mViewModel.onSaveInstanceState(bundle);
        assertThat(bundle.getBoolean(SAVED_STATE_IS_WAITING_ACTIVITY_RESULT)).isFalse();

        // Test isWaitingActivityResult true
        mViewModel.isWaitingActivityResult().set(true);
        bundle.clear();
        mViewModel.onSaveInstanceState(bundle);
        assertThat(bundle.getBoolean(SAVED_STATE_IS_WAITING_ACTIVITY_RESULT)).isTrue();

        // Test isNewFingerprintAdded default false
        bundle.clear();
        mViewModel.onSaveInstanceState(bundle);
        assertThat(bundle.getBoolean(SAVED_STATE_IS_NEW_FINGERPRINT_ADDED)).isFalse();

        // Test isNewFingerprintAdded true
        mViewModel.setIsNewFingerprintAdded();
        bundle.clear();
        mViewModel.onSaveInstanceState(bundle);
        assertThat(bundle.getBoolean(SAVED_STATE_IS_NEW_FINGERPRINT_ADDED)).isTrue();
    }
}
