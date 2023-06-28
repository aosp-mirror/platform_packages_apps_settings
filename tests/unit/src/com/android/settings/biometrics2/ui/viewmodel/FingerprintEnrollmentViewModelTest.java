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

import static android.hardware.fingerprint.FingerprintSensorProperties.TYPE_UDFPS_OPTICAL;

import static com.android.settings.biometrics2.ui.viewmodel.FingerprintEnrollmentViewModel.SAVED_STATE_IS_FIRST_FRAGMENT_ADDED;
import static com.android.settings.biometrics2.ui.viewmodel.FingerprintEnrollmentViewModel.SAVED_STATE_IS_NEW_FINGERPRINT_ADDED;
import static com.android.settings.biometrics2.utils.EnrollmentRequestUtils.newAllFalseRequest;
import static com.android.settings.biometrics2.utils.FingerprintRepositoryUtils.newFingerprintRepository;
import static com.android.settings.biometrics2.utils.FingerprintRepositoryUtils.setupFingerprintEnrolledFingerprints;

import static com.google.common.truth.Truth.assertThat;

import android.app.Application;
import android.content.Intent;
import android.hardware.fingerprint.FingerprintManager;
import android.os.Bundle;

import androidx.activity.result.ActivityResult;
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

    private FingerprintEnrollmentViewModel newViewModelInstance() {
        return new FingerprintEnrollmentViewModel(mApplication, mFingerprintRepository,
                newAllFalseRequest(mApplication));
    }

    @Before
    public void setUp() {
        mApplication = ApplicationProvider.getApplicationContext();
        mFingerprintRepository = newFingerprintRepository(mFingerprintManager, TYPE_UDFPS_OPTICAL,
                5);
        mViewModel = newViewModelInstance();
    }

    @Test
    public void testGetRequest() {
        assertThat(mViewModel.getRequest()).isNotNull();
    }

    @Test
    public void testIsWaitingActivityResult() {
        // Default false
        assertThat(mViewModel.isWaitingActivityResult().get()).isFalse();

        // false if null bundle
        mViewModel = newViewModelInstance();
        mViewModel.onRestoreInstanceState(null);
        assertThat(mViewModel.isWaitingActivityResult().get()).isFalse();

        // false if empty bundle
        mViewModel.onRestoreInstanceState(new Bundle());
        assertThat(mViewModel.isWaitingActivityResult().get()).isFalse();

        // False value can be saved during onSaveInstanceState() and restore after
        // onSaveInstanceState()
        final Bundle falseSavedInstance = new Bundle();
        mViewModel.onSaveInstanceState(falseSavedInstance);
        final FingerprintEnrollmentViewModel falseViewModel = newViewModelInstance();
        falseViewModel.onRestoreInstanceState(falseSavedInstance);
        assertThat(falseViewModel.isWaitingActivityResult().get()).isFalse();

        // True value can be saved during onSaveInstanceState() and restore after
        // onSaveInstanceState()
        final Bundle trueSavedInstance = new Bundle();
        mViewModel.isWaitingActivityResult().set(true);
        mViewModel.onSaveInstanceState(trueSavedInstance);
        final FingerprintEnrollmentViewModel trueViewModel = newViewModelInstance();
        trueViewModel.onRestoreInstanceState(trueSavedInstance);
        assertThat(trueViewModel.isWaitingActivityResult().get()).isTrue();
    }

    @Test
    public void testIsNewFingerprintAdded() {
        // Default false
        final Bundle outBundle = new Bundle();
        mViewModel.onSaveInstanceState(outBundle);
        assertThat(outBundle.containsKey(SAVED_STATE_IS_NEW_FINGERPRINT_ADDED)).isTrue();
        assertThat(outBundle.getBoolean(SAVED_STATE_IS_NEW_FINGERPRINT_ADDED)).isFalse();

        // false if null bundle
        mViewModel = newViewModelInstance();
        mViewModel.onRestoreInstanceState(null);
        outBundle.clear();
        mViewModel.onSaveInstanceState(outBundle);
        assertThat(outBundle.containsKey(SAVED_STATE_IS_NEW_FINGERPRINT_ADDED)).isTrue();
        assertThat(outBundle.getBoolean(SAVED_STATE_IS_NEW_FINGERPRINT_ADDED)).isFalse();

        // false if empty bundle
        mViewModel = newViewModelInstance();
        mViewModel.onRestoreInstanceState(new Bundle());
        outBundle.clear();
        mViewModel.onSaveInstanceState(outBundle);
        assertThat(outBundle.containsKey(SAVED_STATE_IS_NEW_FINGERPRINT_ADDED)).isTrue();
        assertThat(outBundle.getBoolean(SAVED_STATE_IS_NEW_FINGERPRINT_ADDED)).isFalse();

        // False value can be saved during onSaveInstanceState() and restore after
        // onSaveInstanceState()
        final Bundle falseSavedInstance = new Bundle();
        falseSavedInstance.putBoolean(SAVED_STATE_IS_NEW_FINGERPRINT_ADDED, false);
        mViewModel.onRestoreInstanceState(falseSavedInstance);
        outBundle.clear();
        mViewModel.onSaveInstanceState(outBundle);
        assertThat(outBundle.containsKey(SAVED_STATE_IS_NEW_FINGERPRINT_ADDED)).isTrue();
        assertThat(outBundle.getBoolean(SAVED_STATE_IS_NEW_FINGERPRINT_ADDED)).isFalse();

        // True value can be saved during onSaveInstanceState() and restore after
        // onSaveInstanceState()
        final Bundle trueSavedInstance = new Bundle();
        trueSavedInstance.putBoolean(SAVED_STATE_IS_NEW_FINGERPRINT_ADDED, true);
        mViewModel.onRestoreInstanceState(trueSavedInstance);
        outBundle.clear();
        mViewModel.onSaveInstanceState(outBundle);
        assertThat(outBundle.containsKey(SAVED_STATE_IS_NEW_FINGERPRINT_ADDED)).isTrue();
        assertThat(outBundle.getBoolean(SAVED_STATE_IS_NEW_FINGERPRINT_ADDED)).isTrue();

        // setIsFirstFragmentAdded() can be saved during onSaveInstanceState()
        mViewModel.setIsFirstFragmentAdded();
        mViewModel.onSaveInstanceState(trueSavedInstance);
        assertThat(trueSavedInstance.containsKey(SAVED_STATE_IS_NEW_FINGERPRINT_ADDED)).isTrue();
        assertThat(trueSavedInstance.getBoolean(SAVED_STATE_IS_NEW_FINGERPRINT_ADDED)).isTrue();
    }

    @Test
    public void testIsFirstFragmentAdded() {
        // Default false
        final Bundle outBundle = new Bundle();
        mViewModel.onSaveInstanceState(outBundle);
        assertThat(outBundle.containsKey(SAVED_STATE_IS_FIRST_FRAGMENT_ADDED)).isTrue();
        assertThat(outBundle.getBoolean(SAVED_STATE_IS_FIRST_FRAGMENT_ADDED)).isFalse();

        // false if null bundle
        mViewModel = newViewModelInstance();
        mViewModel.onRestoreInstanceState(null);
        outBundle.clear();
        mViewModel.onSaveInstanceState(outBundle);
        assertThat(outBundle.containsKey(SAVED_STATE_IS_FIRST_FRAGMENT_ADDED)).isTrue();
        assertThat(outBundle.getBoolean(SAVED_STATE_IS_FIRST_FRAGMENT_ADDED)).isFalse();

        // false if empty bundle
        mViewModel = newViewModelInstance();
        mViewModel.onRestoreInstanceState(new Bundle());
        outBundle.clear();
        mViewModel.onSaveInstanceState(outBundle);
        assertThat(outBundle.containsKey(SAVED_STATE_IS_FIRST_FRAGMENT_ADDED)).isTrue();
        assertThat(outBundle.getBoolean(SAVED_STATE_IS_FIRST_FRAGMENT_ADDED)).isFalse();

        // False value can be saved during onSaveInstanceState() and restore after
        // onSaveInstanceState()
        final Bundle falseSavedInstance = new Bundle();
        falseSavedInstance.putBoolean(SAVED_STATE_IS_FIRST_FRAGMENT_ADDED, false);
        mViewModel.onRestoreInstanceState(falseSavedInstance);
        outBundle.clear();
        mViewModel.onSaveInstanceState(outBundle);
        assertThat(outBundle.containsKey(SAVED_STATE_IS_FIRST_FRAGMENT_ADDED)).isTrue();
        assertThat(outBundle.getBoolean(SAVED_STATE_IS_FIRST_FRAGMENT_ADDED)).isFalse();

        // True value can be saved during onSaveInstanceState() and restore after
        // onSaveInstanceState()
        final Bundle trueSavedInstance = new Bundle();
        trueSavedInstance.putBoolean(SAVED_STATE_IS_FIRST_FRAGMENT_ADDED, true);
        mViewModel.onRestoreInstanceState(trueSavedInstance);
        outBundle.clear();
        mViewModel.onSaveInstanceState(outBundle);
        assertThat(outBundle.containsKey(SAVED_STATE_IS_FIRST_FRAGMENT_ADDED)).isTrue();
        assertThat(outBundle.getBoolean(SAVED_STATE_IS_FIRST_FRAGMENT_ADDED)).isTrue();

        // setIsFirstFragmentAdded() can be saved during onSaveInstanceState()
        mViewModel.setIsFirstFragmentAdded();
        mViewModel.onSaveInstanceState(trueSavedInstance);
        assertThat(trueSavedInstance.containsKey(SAVED_STATE_IS_FIRST_FRAGMENT_ADDED)).isTrue();
        assertThat(trueSavedInstance.getBoolean(SAVED_STATE_IS_FIRST_FRAGMENT_ADDED)).isTrue();
    }

    @Test
    public void testOverrideActivityResult_shallKeepNullIntent_woChallengeExtra() {
        final ActivityResult retResult = mViewModel.getOverrideActivityResult(
                new ActivityResult(22, null), null);

        assertThat(retResult).isNotNull();
        assertThat(retResult.getData()).isNull();
    }

    @Test
    public void testOverrideActivityResult_shallKeepNullIntent_noIntent_woChallengeExtra() {
        final Intent intent = new Intent();

        final ActivityResult retResult = mViewModel.getOverrideActivityResult(
                new ActivityResult(33, intent), null);

        assertThat(retResult).isNotNull();
        assertThat(retResult.getData()).isEqualTo(intent);
    }

    @Test
    public void testOverrideActivityResult_shallKeepNull_woAdded_woIntent_withChallenge() {
        final Bundle extra = new Bundle();
        extra.putString("test1", "test123");

        final ActivityResult retResult = mViewModel.getOverrideActivityResult(
                new ActivityResult(33, null), extra);

        assertThat(retResult).isNotNull();
        assertThat(retResult.getData()).isNull();
    }

    @Test
    public void testOverrideActivityResult_shallCreateNew_woIntent_withChallenge() {
        final String key1 = "test1";
        final String key2 = "test2";
        final Bundle extra = new Bundle();
        extra.putString(key1, "test123");
        extra.putInt(key2, 9999);

        mViewModel.setIsNewFingerprintAdded();
        final ActivityResult retResult = mViewModel.getOverrideActivityResult(
                new ActivityResult(33, null), extra);

        assertThat(retResult).isNotNull();
        final Intent retIntent = retResult.getData();
        assertThat(retIntent).isNotNull();
        final Bundle retExtra = retIntent.getExtras();
        assertThat(retExtra).isNotNull();
        assertThat(retExtra.getSize()).isEqualTo(extra.getSize());
        assertThat(retExtra.getString(key1)).isEqualTo(extra.getString(key1));
        assertThat(retExtra.getInt(key2)).isEqualTo(extra.getInt(key2));
    }

    @Test
    public void testOverrideActivityResult_shallNotMerge_nonAdded_woIntent_withChallenge() {
        final Bundle extra = new Bundle();
        extra.putString("test2", "test123");

        final Intent intent = new Intent();
        final String key2 = "test2";
        intent.putExtra(key2, 3456L);

        final ActivityResult retResult = mViewModel.getOverrideActivityResult(
                new ActivityResult(33, intent), extra);

        assertThat(retResult).isNotNull();
        final Intent retIntent = retResult.getData();
        assertThat(retIntent).isNotNull();
        final Bundle retExtra = retIntent.getExtras();
        assertThat(retExtra).isNotNull();
        assertThat(retExtra.getSize()).isEqualTo(intent.getExtras().getSize());
        assertThat(retExtra.getString(key2)).isEqualTo(intent.getExtras().getString(key2));
    }

    @Test
    public void testOverrideActivityResult_shallMerge_added_woIntent_withChallenge() {
        final String key1 = "test1";
        final String key2 = "test2";
        final Bundle extra = new Bundle();
        extra.putString(key1, "test123");
        extra.putInt(key2, 9999);

        final Intent intent = new Intent();
        final String key3 = "test3";
        intent.putExtra(key3, 3456L);

        mViewModel.setIsNewFingerprintAdded();
        final ActivityResult retResult = mViewModel.getOverrideActivityResult(
                new ActivityResult(33, intent), extra);

        assertThat(retResult).isNotNull();
        final Intent retIntent = retResult.getData();
        assertThat(retIntent).isNotNull();
        final Bundle retExtra = retIntent.getExtras();
        assertThat(retExtra).isNotNull();
        assertThat(retExtra.getSize()).isEqualTo(extra.getSize() + intent.getExtras().getSize());
        assertThat(retExtra.getString(key1)).isEqualTo(extra.getString(key1));
        assertThat(retExtra.getInt(key2)).isEqualTo(extra.getInt(key2));
        assertThat(retExtra.getLong(key3)).isEqualTo(intent.getExtras().getLong(key3));
    }

    @Test
    public void testIsMaxEnrolledReached() {
        final int uid = 100;
        mFingerprintRepository = newFingerprintRepository(mFingerprintManager, TYPE_UDFPS_OPTICAL,
                3);
        mViewModel = new FingerprintEnrollmentViewModel(mApplication, mFingerprintRepository,
                newAllFalseRequest(mApplication));

        setupFingerprintEnrolledFingerprints(mFingerprintManager, uid, 0);
        assertThat(mViewModel.isMaxEnrolledReached(uid)).isFalse();

        setupFingerprintEnrolledFingerprints(mFingerprintManager, uid, 1);
        assertThat(mViewModel.isMaxEnrolledReached(uid)).isFalse();

        setupFingerprintEnrolledFingerprints(mFingerprintManager, uid, 2);
        assertThat(mViewModel.isMaxEnrolledReached(uid)).isFalse();

        setupFingerprintEnrolledFingerprints(mFingerprintManager, uid, 3);
        assertThat(mViewModel.isMaxEnrolledReached(uid)).isTrue();

        setupFingerprintEnrolledFingerprints(mFingerprintManager, uid, 4);
        assertThat(mViewModel.isMaxEnrolledReached(uid)).isTrue();
    }
}
