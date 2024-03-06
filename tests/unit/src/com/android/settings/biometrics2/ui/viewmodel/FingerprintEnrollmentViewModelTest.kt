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
package com.android.settings.biometrics2.ui.viewmodel

import android.app.Application
import android.content.Intent
import android.hardware.fingerprint.FingerprintManager
import android.hardware.fingerprint.FingerprintSensorProperties
import android.os.Bundle
import androidx.activity.result.ActivityResult
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.settings.biometrics.BiometricEnrollBase
import com.android.settings.biometrics2.data.repository.FingerprintRepository
import com.android.settings.biometrics2.utils.EnrollmentRequestUtils.newAllFalseRequest
import com.android.settings.biometrics2.utils.EnrollmentRequestUtils.newIsSuwRequest
import com.android.settings.biometrics2.utils.FingerprintRepositoryUtils.newFingerprintRepository
import com.android.settings.biometrics2.utils.FingerprintRepositoryUtils.setupFingerprintEnrolledFingerprints
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.junit.MockitoJUnit
import org.mockito.junit.MockitoRule

@RunWith(AndroidJUnit4::class)
class FingerprintEnrollmentViewModelTest {

    @get:Rule val mockito: MockitoRule = MockitoJUnit.rule()

    private val application: Application
        get() = ApplicationProvider.getApplicationContext()

    @Mock
    private lateinit var fingerprintManager: FingerprintManager

    private lateinit var fingerprintRepository: FingerprintRepository
    private lateinit var viewModel: FingerprintEnrollmentViewModel

    @Before
    fun setUp() {
        fingerprintRepository = newFingerprintRepository(
            fingerprintManager,
            FingerprintSensorProperties.TYPE_UDFPS_OPTICAL,
            5
        )
        viewModel = FingerprintEnrollmentViewModel(
            application,
            fingerprintRepository,
            newAllFalseRequest(application)
        )
    }

    @Test
    fun testGetRequest() {
        assertThat(viewModel.request).isNotNull()
    }

    @Test
    fun testIsWaitingActivityResultDefaultFalse() {
        assertThat(viewModel.isWaitingActivityResult.value).isFalse()
    }


    @Test
    fun testOverrideActivityResult_shallKeepNullIntent_woChallengeExtra() {
        val retResult = viewModel.getOverrideActivityResult(
            ActivityResult(22, null), null
        )
        assertThat(retResult).isNotNull()
        assertThat(retResult.data).isNull()
    }

    @Test
    fun testOverrideActivityResult_shallKeepNullIntent_noIntent_woChallengeExtra() {
        val intent = Intent()
        val retResult = viewModel.getOverrideActivityResult(
            ActivityResult(33, intent), null
        )
        assertThat(retResult).isNotNull()
        assertThat(retResult.data).isEqualTo(intent)
    }

    @Test
    fun testOverrideActivityResult_shallKeepNull_woAdded_woIntent_withChallenge() {
        val extra = Bundle()
        extra.putString("test1", "test123")

        val retResult = viewModel.getOverrideActivityResult(
            ActivityResult(33, null), extra
        )

        assertThat(retResult).isNotNull()
        assertThat(retResult.data).isNull()
    }

    @Test
    fun testOverrideActivityResult_shallCreateNew_woIntent_withChallenge() {
        val key1 = "test1"
        val key2 = "test2"
        val extra = Bundle().apply {
            putString(key1, "test123")
            putInt(key2, 9999)
        }

        viewModel.isNewFingerprintAdded = true

        val retResult = viewModel.getOverrideActivityResult(
            ActivityResult(33, null), extra
        )
        assertThat(retResult).isNotNull()

        val retIntent = retResult.data
        assertThat(retIntent).isNotNull()

        val retExtra = retIntent!!.extras
        assertThat(retExtra).isNotNull()
        assertThat(retExtra!!.size).isEqualTo(extra.size)
        assertThat(retExtra.getString(key1)).isEqualTo(extra.getString(key1))
        assertThat(retExtra.getInt(key2)).isEqualTo(extra.getInt(key2))
    }

    @Test
    fun testOverrideActivityResult_shallNotMerge_nonAdded_woIntent_withChallenge() {
        val extra = Bundle().apply {
            putString("test2", "test123")
        }

        val key2 = "test2"
        val intent = Intent().apply {
            putExtra(key2, 3456L)
        }

        val retResult = viewModel.getOverrideActivityResult(ActivityResult(33, intent), extra)

        assertThat(retResult).isNotNull()

        val retIntent = retResult.data
        assertThat(retIntent).isNotNull()

        val retExtra = retIntent!!.extras
        assertThat(retExtra).isNotNull()
        assertThat(retExtra!!.size).isEqualTo(intent.extras!!.size)
        assertThat(retExtra.getString(key2)).isEqualTo(intent.extras!!.getString(key2))
    }

    @Test
    fun testOverrideActivityResult_shallMerge_added_woIntent_withChallenge() {
        val key1 = "test1"
        val key2 = "test2"
        val extra = Bundle().apply {
            putString(key1, "test123")
            putInt(key2, 9999)
        }

        val key3 = "test3"
        val intent = Intent().apply {
            putExtra(key3, 3456L)
        }

        viewModel.isNewFingerprintAdded = true

        val retResult = viewModel.getOverrideActivityResult(ActivityResult(33, intent), extra)
        assertThat(retResult).isNotNull()

        val retIntent = retResult.data
        assertThat(retIntent).isNotNull()

        val retExtra = retIntent!!.extras
        assertThat(retExtra).isNotNull()
        assertThat(retExtra!!.size).isEqualTo(extra.size + intent.extras!!.size)
        assertThat(retExtra.getString(key1)).isEqualTo(extra.getString(key1))
        assertThat(retExtra.getInt(key2)).isEqualTo(extra.getInt(key2))
        assertThat(retExtra.getLong(key3)).isEqualTo(intent.extras!!.getLong(key3))
    }

    @Test
    fun testIsMaxEnrolledReached() {
        val uid = 100
        fingerprintRepository = newFingerprintRepository(
            fingerprintManager,
            FingerprintSensorProperties.TYPE_UDFPS_OPTICAL,
            3
        )
        viewModel = FingerprintEnrollmentViewModel(
            application,
            fingerprintRepository,
            newAllFalseRequest(application)
        )

        setupFingerprintEnrolledFingerprints(fingerprintManager, uid, 0)
        assertThat(viewModel.isMaxEnrolledReached(uid)).isFalse()

        setupFingerprintEnrolledFingerprints(fingerprintManager, uid, 1)
        assertThat(viewModel.isMaxEnrolledReached(uid)).isFalse()

        setupFingerprintEnrolledFingerprints(fingerprintManager, uid, 2)
        assertThat(viewModel.isMaxEnrolledReached(uid)).isFalse()

        setupFingerprintEnrolledFingerprints(fingerprintManager, uid, 3)
        assertThat(viewModel.isMaxEnrolledReached(uid)).isTrue()

        setupFingerprintEnrolledFingerprints(fingerprintManager, uid, 4)
        assertThat(viewModel.isMaxEnrolledReached(uid)).isTrue()
    }

    @Test
    fun testSetResultFlow_defaultEmpty() = runTest {
        val activityResults = listOfSetResultFlow()

        runCurrent()

        assertThat(activityResults.size).isEqualTo(0)
    }

    @Test
    fun testCheckFinishActivityDuringOnPause_doNothingIfIsSuw() = runTest {
        viewModel = FingerprintEnrollmentViewModel(
            application,
            fingerprintRepository,
            newIsSuwRequest(application)
        )

        val activityResults = listOfSetResultFlow()

        viewModel.checkFinishActivityDuringOnPause(
            isActivityFinishing = false,
            isChangingConfigurations = false,
            scope = this
        )
        runCurrent()

        assertThat(activityResults.size).isEqualTo(0)
    }

    @Test
    fun testCheckFinishActivityDuringOnPause_doNothingIfIsWaitingActivity() = runTest {
        val activityResults = listOfSetResultFlow()

        viewModel.isWaitingActivityResult.value = true
        viewModel.checkFinishActivityDuringOnPause(
            isActivityFinishing = false,
            isChangingConfigurations = false,
            scope = this
        )
        runCurrent()

        assertThat(activityResults.size).isEqualTo(0)
    }

    @Test
    fun testCheckFinishActivityDuringOnPause_doNothingIfIsActivityFinishing() = runTest {
        val activityResults = listOfSetResultFlow()

        viewModel.checkFinishActivityDuringOnPause(
            isActivityFinishing = true,
            isChangingConfigurations = false,
            scope = this
        )
        runCurrent()

        assertThat(activityResults.size).isEqualTo(0)
    }

    @Test
    fun testCheckFinishActivityDuringOnPause_doNothingIfIsChangingConfigurations() = runTest {
        val activityResults = listOfSetResultFlow()

        viewModel.checkFinishActivityDuringOnPause(
            isActivityFinishing = false,
            isChangingConfigurations = true,
            scope = this
        )
        runCurrent()

        assertThat(activityResults.size).isEqualTo(0)
    }

    @Test
    fun testCheckFinishActivityDuringOnPause_defaultFinishSelf() = runTest {
        val activityResults = listOfSetResultFlow()

        viewModel.checkFinishActivityDuringOnPause(
            isActivityFinishing = false,
            isChangingConfigurations = false,
            scope = backgroundScope
        )
        runCurrent()

        assertThat(activityResults.size).isEqualTo(1)
        assertThat(activityResults[0].resultCode).isEqualTo(BiometricEnrollBase.RESULT_TIMEOUT)
        assertThat(activityResults[0].data).isEqualTo(null)
    }

    private fun TestScope.listOfSetResultFlow(): List<ActivityResult> =
        mutableListOf<ActivityResult>().also {
            backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
                viewModel.setResultFlow.toList(it)
            }
        }
}
