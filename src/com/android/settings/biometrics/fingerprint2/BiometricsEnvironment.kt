/*
 * Copyright (C) 2024 The Android Open Source Project
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

package com.android.settings.biometrics.fingerprint2

import android.hardware.fingerprint.FingerprintManager
import android.view.MotionEvent
import android.view.accessibility.AccessibilityManager
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import com.android.internal.widget.LockPatternUtils
import com.android.settings.SettingsApplication
import com.android.settings.biometrics.GatekeeperPasswordProvider
import com.android.settings.biometrics.fingerprint2.data.repository.DebuggingRepository
import com.android.settings.biometrics.fingerprint2.data.repository.DebuggingRepositoryImpl
import com.android.settings.biometrics.fingerprint2.data.repository.FingerprintEnrollmentRepositoryImpl
import com.android.settings.biometrics.fingerprint2.data.repository.FingerprintSensorRepository
import com.android.settings.biometrics.fingerprint2.data.repository.FingerprintSensorRepositoryImpl
import com.android.settings.biometrics.fingerprint2.data.repository.FingerprintSettingsRepositoryImpl
import com.android.settings.biometrics.fingerprint2.data.repository.UserRepoImpl
import com.android.settings.biometrics.fingerprint2.debug.data.repository.UdfpsEnrollDebugRepositoryImpl
import com.android.settings.biometrics.fingerprint2.debug.domain.interactor.DebugTouchEventInteractorImpl
import com.android.settings.biometrics.fingerprint2.domain.interactor.AccessibilityInteractor
import com.android.settings.biometrics.fingerprint2.domain.interactor.AccessibilityInteractorImpl
import com.android.settings.biometrics.fingerprint2.domain.interactor.AuthenticateInteractorImpl
import com.android.settings.biometrics.fingerprint2.domain.interactor.CanEnrollFingerprintsInteractorImpl
import com.android.settings.biometrics.fingerprint2.domain.interactor.DebuggingInteractor
import com.android.settings.biometrics.fingerprint2.domain.interactor.DebuggingInteractorImpl
import com.android.settings.biometrics.fingerprint2.domain.interactor.DisplayDensityInteractor
import com.android.settings.biometrics.fingerprint2.domain.interactor.DisplayDensityInteractorImpl
import com.android.settings.biometrics.fingerprint2.domain.interactor.EnrollFingerprintInteractorImpl
import com.android.settings.biometrics.fingerprint2.domain.interactor.EnrollStageInteractor
import com.android.settings.biometrics.fingerprint2.domain.interactor.EnrollStageInteractorImpl
import com.android.settings.biometrics.fingerprint2.domain.interactor.EnrolledFingerprintsInteractorImpl
import com.android.settings.biometrics.fingerprint2.domain.interactor.FingerprintSensorInteractor
import com.android.settings.biometrics.fingerprint2.domain.interactor.FingerprintSensorInteractorImpl
import com.android.settings.biometrics.fingerprint2.domain.interactor.FoldStateInteractor
import com.android.settings.biometrics.fingerprint2.domain.interactor.FoldStateInteractorImpl
import com.android.settings.biometrics.fingerprint2.domain.interactor.GenerateChallengeInteractorImpl
import com.android.settings.biometrics.fingerprint2.domain.interactor.OrientationInteractor
import com.android.settings.biometrics.fingerprint2.domain.interactor.OrientationInteractorImpl
import com.android.settings.biometrics.fingerprint2.domain.interactor.RemoveFingerprintsInteractorImpl
import com.android.settings.biometrics.fingerprint2.domain.interactor.RenameFingerprintsInteractorImpl
import com.android.settings.biometrics.fingerprint2.domain.interactor.SensorInteractorImpl
import com.android.settings.biometrics.fingerprint2.domain.interactor.TouchEventInteractor
import com.android.settings.biometrics.fingerprint2.domain.interactor.UdfpsEnrollInteractor
import com.android.settings.biometrics.fingerprint2.domain.interactor.UdfpsEnrollInteractorImpl
import com.android.settings.biometrics.fingerprint2.domain.interactor.VibrationInteractor
import com.android.settings.biometrics.fingerprint2.domain.interactor.VibrationInteractorImpl
import com.android.settings.biometrics.fingerprint2.lib.domain.interactor.AuthenitcateInteractor
import com.android.settings.biometrics.fingerprint2.lib.domain.interactor.CanEnrollFingerprintsInteractor
import com.android.settings.biometrics.fingerprint2.lib.domain.interactor.EnrollFingerprintInteractor
import com.android.settings.biometrics.fingerprint2.lib.domain.interactor.GenerateChallengeInteractor
import com.android.settings.biometrics.fingerprint2.lib.domain.interactor.RemoveFingerprintInteractor
import com.android.settings.biometrics.fingerprint2.lib.domain.interactor.RenameFingerprintInteractor
import com.android.settings.biometrics.fingerprint2.lib.domain.interactor.SensorInteractor
import com.android.settings.biometrics.fingerprint2.lib.model.Settings
import java.util.concurrent.Executors
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

/**
 * This class should handle all repo & interactor creation needed by the ViewModels for the
 * biometrics code.
 *
 * This code is instantiated within the [SettingsApplication], all repos should be private &
 * immutable and all interactors should public and immutable
 */
class BiometricsEnvironment(
  val context: SettingsApplication,
  private val fingerprintManager: FingerprintManager,
) : ViewModelStoreOwner {
  private val executorService = Executors.newSingleThreadExecutor()
  private val backgroundDispatcher = executorService.asCoroutineDispatcher()
  private val applicationScope = MainScope()
  private val gateKeeperPasswordProvider = GatekeeperPasswordProvider(LockPatternUtils(context))

  private val userRepo = UserRepoImpl(context.userId)
  private val fingerprintSettingsRepository =
    FingerprintSettingsRepositoryImpl(
      context.resources.getInteger(
        com.android.internal.R.integer.config_fingerprintMaxTemplatesPerUser
      )
    )
  private val fingerprintEnrollmentRepository =
    FingerprintEnrollmentRepositoryImpl(fingerprintManager, userRepo, fingerprintSettingsRepository,
      backgroundDispatcher, applicationScope)
  private val fingerprintSensorRepository: FingerprintSensorRepository =
    FingerprintSensorRepositoryImpl(fingerprintManager, backgroundDispatcher, applicationScope)
  private val debuggingRepository: DebuggingRepository = DebuggingRepositoryImpl()
  private val udfpsDebugRepo = UdfpsEnrollDebugRepositoryImpl()

  fun createSensorPropertiesInteractor(): SensorInteractor =
    SensorInteractorImpl(fingerprintSensorRepository)

  fun createCanEnrollFingerprintsInteractor(): CanEnrollFingerprintsInteractor =
    CanEnrollFingerprintsInteractorImpl(fingerprintEnrollmentRepository)

  fun createGenerateChallengeInteractor(): GenerateChallengeInteractor =
    GenerateChallengeInteractorImpl(fingerprintManager, context.userId, gateKeeperPasswordProvider)

  fun createFingerprintEnrollInteractor(): EnrollFingerprintInteractor =
    EnrollFingerprintInteractorImpl(context.userId, fingerprintManager, Settings)

  fun createFingerprintsEnrolledInteractor(): EnrolledFingerprintsInteractorImpl =
    EnrolledFingerprintsInteractorImpl(fingerprintManager, context.userId)

  fun createAuthenticateInteractor(): AuthenitcateInteractor =
    AuthenticateInteractorImpl(fingerprintManager, context.userId)

  fun createRemoveFingerprintInteractor(): RemoveFingerprintInteractor =
    RemoveFingerprintsInteractorImpl(fingerprintManager, context.userId)

  fun createRenameFingerprintInteractor(): RenameFingerprintInteractor =
    RenameFingerprintsInteractorImpl(fingerprintManager, context.userId, backgroundDispatcher)

  val accessibilityInteractor: AccessibilityInteractor by lazy {
    AccessibilityInteractorImpl(
      context.getSystemService(AccessibilityManager::class.java)!!,
      applicationScope,
    )
  }

  val foldStateInteractor: FoldStateInteractor by lazy { FoldStateInteractorImpl(context) }

  val orientationInteractor: OrientationInteractor by lazy { OrientationInteractorImpl(context) }

  val vibrationInteractor: VibrationInteractor by lazy { VibrationInteractorImpl(context) }

  val displayDensityInteractor: DisplayDensityInteractor by lazy {
    DisplayDensityInteractorImpl(context, applicationScope)
  }

  val debuggingInteractor: DebuggingInteractor by lazy {
    DebuggingInteractorImpl(debuggingRepository)
  }

  val enrollStageInteractor: EnrollStageInteractor by lazy { EnrollStageInteractorImpl() }

  val udfpsEnrollInteractor: UdfpsEnrollInteractor by lazy {
    UdfpsEnrollInteractorImpl(context, accessibilityInteractor)
  }

  val sensorInteractor: FingerprintSensorInteractor by lazy {
    FingerprintSensorInteractorImpl(fingerprintSensorRepository)
  }

  val touchEventInteractor: TouchEventInteractor by lazy {
    if (debuggingRepository.isDebuggingEnabled()) {
      DebugTouchEventInteractorImpl(udfpsDebugRepo)
    } else {
      object : TouchEventInteractor {
        override val touchEvent: Flow<MotionEvent> = flowOf()
      }
    }
  }

  override val viewModelStore: ViewModelStore = ViewModelStore()
}
