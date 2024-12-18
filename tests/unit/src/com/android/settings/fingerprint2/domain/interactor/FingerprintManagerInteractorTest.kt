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

package com.android.settings.fingerprint2.domain.interactor

import android.content.Intent
import android.hardware.biometrics.ComponentInfoInternal
import android.hardware.biometrics.SensorLocationInternal
import android.hardware.biometrics.SensorProperties
import android.hardware.fingerprint.Fingerprint
import android.hardware.fingerprint.FingerprintEnrollOptions
import android.hardware.fingerprint.FingerprintManager
import android.hardware.fingerprint.FingerprintManager.CryptoObject
import android.hardware.fingerprint.FingerprintManager.FINGERPRINT_ERROR_LOCKOUT_PERMANENT
import android.hardware.fingerprint.FingerprintSensorProperties
import android.hardware.fingerprint.FingerprintSensorPropertiesInternal
import android.os.CancellationSignal
import android.os.Handler
import com.android.settings.biometrics.GatekeeperPasswordProvider
import com.android.settings.biometrics.fingerprint2.data.repository.FingerprintEnrollmentRepositoryImpl
import com.android.settings.biometrics.fingerprint2.data.repository.FingerprintSensorRepository
import com.android.settings.biometrics.fingerprint2.data.repository.FingerprintSettingsRepositoryImpl
import com.android.settings.biometrics.fingerprint2.data.repository.UserRepo
import com.android.settings.biometrics.fingerprint2.domain.interactor.AuthenticateInteractorImpl
import com.android.settings.biometrics.fingerprint2.domain.interactor.CanEnrollFingerprintsInteractorImpl
import com.android.settings.biometrics.fingerprint2.domain.interactor.EnrollFingerprintInteractorImpl
import com.android.settings.biometrics.fingerprint2.domain.interactor.EnrolledFingerprintsInteractorImpl
import com.android.settings.biometrics.fingerprint2.domain.interactor.GenerateChallengeInteractorImpl
import com.android.settings.biometrics.fingerprint2.domain.interactor.RemoveFingerprintsInteractorImpl
import com.android.settings.biometrics.fingerprint2.domain.interactor.RenameFingerprintsInteractorImpl
import com.android.settings.biometrics.fingerprint2.lib.domain.interactor.CanEnrollFingerprintsInteractor
import com.android.settings.biometrics.fingerprint2.lib.domain.interactor.EnrollFingerprintInteractor
import com.android.settings.biometrics.fingerprint2.lib.domain.interactor.EnrolledFingerprintsInteractor
import com.android.settings.biometrics.fingerprint2.lib.domain.interactor.GenerateChallengeInteractor
import com.android.settings.biometrics.fingerprint2.lib.domain.interactor.RemoveFingerprintInteractor
import com.android.settings.biometrics.fingerprint2.lib.domain.interactor.RenameFingerprintInteractor
import com.android.settings.biometrics.fingerprint2.lib.model.Default
import com.android.settings.biometrics.fingerprint2.lib.model.EnrollReason
import com.android.settings.biometrics.fingerprint2.lib.model.FingerEnrollState
import com.android.settings.biometrics.fingerprint2.lib.model.FingerprintAuthAttemptModel
import com.android.settings.biometrics.fingerprint2.lib.model.FingerprintData
import com.android.settings.biometrics.fingerprint2.lib.model.FingerprintFlow
import com.android.settings.password.ChooseLockSettingsHelper
import com.android.systemui.biometrics.shared.model.FingerprintSensor
import com.android.systemui.biometrics.shared.model.toFingerprintSensor
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.last
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers.anyInt
import org.mockito.ArgumentMatchers.anyLong
import org.mockito.ArgumentMatchers.eq
import org.mockito.ArgumentMatchers.nullable
import org.mockito.Mock
import org.mockito.Mockito
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.mockito.junit.MockitoJUnit
import org.mockito.junit.MockitoJUnitRunner
import org.mockito.stubbing.OngoingStubbing

@RunWith(MockitoJUnitRunner::class)
class FingerprintManagerInteractorTest {

  @JvmField @Rule var rule = MockitoJUnit.rule()
  private lateinit var enrolledFingerprintsInteractorUnderTest: EnrolledFingerprintsInteractor
  private lateinit var generateChallengeInteractorUnderTest: GenerateChallengeInteractor
  private lateinit var removeFingerprintsInteractorUnderTest: RemoveFingerprintInteractor
  private lateinit var renameFingerprintsInteractorUnderTest: RenameFingerprintInteractor
  private lateinit var authenticateInteractorImplUnderTest: AuthenticateInteractorImpl
  private lateinit var canEnrollFingerprintsInteractorUnderTest: CanEnrollFingerprintsInteractor
  private lateinit var enrollInteractorUnderTest: EnrollFingerprintInteractor

  private val userId = 0
  private var backgroundDispatcher = StandardTestDispatcher()
  @Mock private lateinit var fingerprintManager: FingerprintManager
  @Mock private lateinit var gateKeeperPasswordProvider: GatekeeperPasswordProvider

  private var testScope = TestScope(backgroundDispatcher)
  private var backgroundScope = testScope.backgroundScope
  private val flow: FingerprintFlow = Default
  private val maxFingerprints = 5
  private val currUser = MutableStateFlow(0)
  private val userRepo =
    object : UserRepo {
      override val currentUser: Flow<Int> = currUser
    }

  @Before
  fun setup() {
    val sensor =
      FingerprintSensorPropertiesInternal(
          0 /* sensorId */,
          SensorProperties.STRENGTH_STRONG,
          maxFingerprints,
          listOf<ComponentInfoInternal>(),
          FingerprintSensorProperties.TYPE_POWER_BUTTON,
          false /* halControlsIllumination */,
          true /* resetLockoutRequiresHardwareAuthToken */,
          listOf<SensorLocationInternal>(SensorLocationInternal.DEFAULT),
        )
        .toFingerprintSensor()

    val fingerprintSensorRepository =
      object : FingerprintSensorRepository {
        override val fingerprintSensor: Flow<FingerprintSensor> = flowOf(sensor)
        override val hasSideFps: Flow<Boolean> = flowOf(false)
      }

    val settingsRepository = FingerprintSettingsRepositoryImpl(maxFingerprints)
    val fingerprintEnrollmentRepository =
      FingerprintEnrollmentRepositoryImpl(
        fingerprintManager,
        userRepo,
        settingsRepository,
        backgroundDispatcher,
        backgroundScope,
      )

    enrolledFingerprintsInteractorUnderTest =
      EnrolledFingerprintsInteractorImpl(fingerprintManager, userId)
    generateChallengeInteractorUnderTest =
      GenerateChallengeInteractorImpl(fingerprintManager, userId, gateKeeperPasswordProvider)
    removeFingerprintsInteractorUnderTest =
      RemoveFingerprintsInteractorImpl(fingerprintManager, userId)
    renameFingerprintsInteractorUnderTest =
      RenameFingerprintsInteractorImpl(fingerprintManager, userId, backgroundDispatcher)
    authenticateInteractorImplUnderTest = AuthenticateInteractorImpl(fingerprintManager, userId)

    canEnrollFingerprintsInteractorUnderTest =
      CanEnrollFingerprintsInteractorImpl(fingerprintEnrollmentRepository)

    enrollInteractorUnderTest = EnrollFingerprintInteractorImpl(userId, fingerprintManager, flow)
  }

  @Test
  fun testEmptyFingerprints() =
    testScope.runTest {
      whenever(fingerprintManager.getEnrolledFingerprints(anyInt())).thenReturn(emptyList())

      val emptyFingerprintList: List<Fingerprint> = emptyList()
      assertThat(enrolledFingerprintsInteractorUnderTest.enrolledFingerprints.last())
        .isEqualTo(emptyFingerprintList)
    }

  @Test
  fun testOneFingerprint() =
    testScope.runTest {
      val expected = Fingerprint("Finger 1,", 2, 3L)
      val fingerprintList: List<Fingerprint> = listOf(expected)
      whenever(fingerprintManager.getEnrolledFingerprints(anyInt())).thenReturn(fingerprintList)

      val list = enrolledFingerprintsInteractorUnderTest.enrolledFingerprints.last()
      assertThat(list!!.size).isEqualTo(fingerprintList.size)
      val actual = list[0]
      assertThat(actual.name).isEqualTo(expected.name)
      assertThat(actual.fingerId).isEqualTo(expected.biometricId)
      assertThat(actual.deviceId).isEqualTo(expected.deviceId)
    }

  @Test
  fun testCanEnrollFingerprintSucceeds() =
    testScope.runTest {
      val fingerprintList: List<Fingerprint> =
        listOf(
          Fingerprint("Finger 1", 2, 3L),
          Fingerprint("Finger 2", 3, 3L),
          Fingerprint("Finger 3", 4, 3L),
        )
      whenever(fingerprintManager.getEnrolledFingerprints(anyInt())).thenReturn(fingerprintList)

      var result: Boolean? = null
      val job =
        testScope.launch {
          canEnrollFingerprintsInteractorUnderTest.canEnrollFingerprints.collect { result = it }
        }

      runCurrent()
      job.cancelAndJoin()

      assertThat(result).isTrue()
    }

  @Test
  fun testCanEnrollFingerprintFails() =
    testScope.runTest {
      val fingerprintList: List<Fingerprint> =
        listOf(
          Fingerprint("Finger 1", 2, 3L),
          Fingerprint("Finger 2", 3, 3L),
          Fingerprint("Finger 3", 4, 3L),
          Fingerprint("Finger 4", 5, 3L),
          Fingerprint("Finger 5", 6, 3L),
        )
      whenever(fingerprintManager.getEnrolledFingerprints(anyInt())).thenReturn(fingerprintList)

      var result: Boolean? = null
      val job =
        testScope.launch {
          canEnrollFingerprintsInteractorUnderTest.canEnrollFingerprints.collect { result = it }
        }

      runCurrent()
      job.cancelAndJoin()

      assertThat(result).isFalse()
    }

  @Test
  fun testGenerateChallenge() =
    testScope.runTest {
      val byteArray = byteArrayOf(5, 3, 2)
      val challenge = 100L
      val intent = Intent()
      intent.putExtra(ChooseLockSettingsHelper.EXTRA_KEY_GK_PW_HANDLE, challenge)
      whenever(
          gateKeeperPasswordProvider.requestGatekeeperHat(
            any(Intent::class.java),
            anyLong(),
            anyInt(),
          )
        )
        .thenReturn(byteArray)

      val generateChallengeCallback: ArgumentCaptor<FingerprintManager.GenerateChallengeCallback> =
        argumentCaptor()

      var result: Pair<Long, ByteArray?>? = null
      val job =
        testScope.launch { result = generateChallengeInteractorUnderTest.generateChallenge(1L) }
      runCurrent()

      verify(fingerprintManager).generateChallenge(anyInt(), capture(generateChallengeCallback))
      generateChallengeCallback.value.onChallengeGenerated(1, 2, challenge)

      runCurrent()
      job.cancelAndJoin()

      assertThat(result?.first).isEqualTo(challenge)
      assertThat(result?.second).isEqualTo(byteArray)
    }

  @Test
  fun testRemoveFingerprint_succeeds() =
    testScope.runTest {
      val fingerprintViewModelToRemove = FingerprintData("Finger 2", 1, 2L)
      val fingerprintToRemove = Fingerprint("Finger 2", 1, 2L)

      val removalCallback: ArgumentCaptor<FingerprintManager.RemovalCallback> = argumentCaptor()

      var result: Boolean? = null
      val job =
        testScope.launch {
          result =
            removeFingerprintsInteractorUnderTest.removeFingerprint(fingerprintViewModelToRemove)
        }
      runCurrent()

      verify(fingerprintManager)
        .remove(any(Fingerprint::class.java), anyInt(), capture(removalCallback))
      removalCallback.value.onRemovalSucceeded(fingerprintToRemove, 1)

      runCurrent()
      job.cancelAndJoin()

      assertThat(result).isTrue()
    }

  @Test
  fun testRemoveFingerprint_fails() =
    testScope.runTest {
      val fingerprintViewModelToRemove = FingerprintData("Finger 2", 1, 2L)
      val fingerprintToRemove = Fingerprint("Finger 2", 1, 2L)

      val removalCallback: ArgumentCaptor<FingerprintManager.RemovalCallback> = argumentCaptor()

      var result: Boolean? = null
      val job =
        testScope.launch {
          result =
            removeFingerprintsInteractorUnderTest.removeFingerprint(fingerprintViewModelToRemove)
        }
      runCurrent()

      verify(fingerprintManager)
        .remove(any(Fingerprint::class.java), anyInt(), capture(removalCallback))
      removalCallback.value.onRemovalError(
        fingerprintToRemove,
        100,
        "Oh no, we couldn't find that one",
      )

      runCurrent()
      job.cancelAndJoin()

      assertThat(result).isFalse()
    }

  @Test
  fun testRenameFingerprint_succeeds() =
    testScope.runTest {
      val fingerprintToRename = FingerprintData("Finger 2", 1, 2L)

      renameFingerprintsInteractorUnderTest.renameFingerprint(fingerprintToRename, "Woo")

      verify(fingerprintManager).rename(eq(fingerprintToRename.fingerId), anyInt(), safeEq("Woo"))
    }

  @Test
  fun testAuth_succeeds() =
    testScope.runTest {
      val fingerprint = Fingerprint("Woooo", 100, 101L)

      var result: FingerprintAuthAttemptModel? = null
      val job = launch { result = authenticateInteractorImplUnderTest.authenticate() }

      val authCallback: ArgumentCaptor<FingerprintManager.AuthenticationCallback> = argumentCaptor()

      runCurrent()

      verify(fingerprintManager)
        .authenticate(
          nullable(CryptoObject::class.java),
          any(CancellationSignal::class.java),
          capture(authCallback),
          nullable(Handler::class.java),
          anyInt(),
        )
      authCallback.value.onAuthenticationSucceeded(
        FingerprintManager.AuthenticationResult(null, fingerprint, 1, false)
      )

      runCurrent()
      job.cancelAndJoin()
      assertThat(result).isEqualTo(FingerprintAuthAttemptModel.Success(fingerprint.biometricId))
    }

  @Test
  fun testAuth_lockout() =
    testScope.runTest {
      var result: FingerprintAuthAttemptModel? = null
      val job = launch { result = authenticateInteractorImplUnderTest.authenticate() }

      val authCallback: ArgumentCaptor<FingerprintManager.AuthenticationCallback> = argumentCaptor()

      runCurrent()

      verify(fingerprintManager)
        .authenticate(
          nullable(CryptoObject::class.java),
          any(CancellationSignal::class.java),
          capture(authCallback),
          nullable(Handler::class.java),
          anyInt(),
        )
      authCallback.value.onAuthenticationError(FINGERPRINT_ERROR_LOCKOUT_PERMANENT, "Lockout!!")

      runCurrent()
      job.cancelAndJoin()
      assertThat(result)
        .isEqualTo(
          FingerprintAuthAttemptModel.Error(FINGERPRINT_ERROR_LOCKOUT_PERMANENT, "Lockout!!")
        )
    }

  @Test
  fun testEnroll_progress() =
    testScope.runTest {
      val token = byteArrayOf(5, 3, 2)
      var result: FingerEnrollState? = null
      val job = launch {
        enrollInteractorUnderTest
          .enroll(token, EnrollReason.FindSensor, FingerprintEnrollOptions.Builder().build())
          .collect { result = it }
      }
      val enrollCallback: ArgumentCaptor<FingerprintManager.EnrollmentCallback> = argumentCaptor()
      runCurrent()

      verify(fingerprintManager)
        .enroll(
          eq(token),
          any(CancellationSignal::class.java),
          anyInt(),
          capture(enrollCallback),
          eq(FingerprintManager.ENROLL_FIND_SENSOR),
          any(FingerprintEnrollOptions::class.java),
        )
      enrollCallback.value.onEnrollmentProgress(1)
      runCurrent()
      job.cancelAndJoin()

      assertThat(result).isEqualTo(FingerEnrollState.EnrollProgress(1, 2))
    }

  @Test
  fun testEnroll_help() =
    testScope.runTest {
      val token = byteArrayOf(5, 3, 2)
      var result: FingerEnrollState? = null
      val job = launch {
        enrollInteractorUnderTest
          .enroll(token, EnrollReason.FindSensor, FingerprintEnrollOptions.Builder().build())
          .collect { result = it }
      }
      val enrollCallback: ArgumentCaptor<FingerprintManager.EnrollmentCallback> = argumentCaptor()
      runCurrent()

      verify(fingerprintManager)
        .enroll(
          eq(token),
          any(CancellationSignal::class.java),
          anyInt(),
          capture(enrollCallback),
          eq(FingerprintManager.ENROLL_FIND_SENSOR),
          any(FingerprintEnrollOptions::class.java),
        )
      enrollCallback.value.onEnrollmentHelp(-1, "help")
      runCurrent()
      job.cancelAndJoin()

      assertThat(result).isEqualTo(FingerEnrollState.EnrollHelp(-1, "help"))
    }

  @Test
  fun testEnroll_error() =
    testScope.runTest {
      val token = byteArrayOf(5, 3, 2)
      var result: FingerEnrollState? = null
      val job = launch {
        enrollInteractorUnderTest
          .enroll(token, EnrollReason.FindSensor, FingerprintEnrollOptions.Builder().build())
          .collect { result = it }
      }
      val enrollCallback: ArgumentCaptor<FingerprintManager.EnrollmentCallback> = argumentCaptor()
      runCurrent()

      verify(fingerprintManager)
        .enroll(
          eq(token),
          any(CancellationSignal::class.java),
          anyInt(),
          capture(enrollCallback),
          eq(FingerprintManager.ENROLL_FIND_SENSOR),
          any(FingerprintEnrollOptions::class.java),
        )
      enrollCallback.value.onEnrollmentError(-1, "error")
      runCurrent()
      job.cancelAndJoin()
      assertThat(result).isInstanceOf(FingerEnrollState.EnrollError::class.java)
    }

  private fun <T : Any> safeEq(value: T): T = eq(value) ?: value

  private fun <T> capture(argumentCaptor: ArgumentCaptor<T>): T = argumentCaptor.capture()

  private fun <T> any(type: Class<T>): T = Mockito.any<T>(type)

  private fun <T> whenever(methodCall: T): OngoingStubbing<T> = `when`(methodCall)

  inline fun <reified T : Any> argumentCaptor(): ArgumentCaptor<T> =
    ArgumentCaptor.forClass(T::class.java)
}
