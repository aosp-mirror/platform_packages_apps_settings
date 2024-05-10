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

package com.android.settings.biometrics.fingerprint2.ui.settings.viewmodel

/**
 * A class to represent a high level step for FingerprintSettings. This is typically to perform an
 * action like launching an activity.
 */
sealed class NextStepViewModel

data class EnrollFirstFingerprint(
  val userId: Int,
  val gateKeeperPasswordHandle: Long?,
  val challenge: Long?,
  val challengeToken: ByteArray?,
) : NextStepViewModel()

data class EnrollAdditionalFingerprint(
  val userId: Int,
  val challengeToken: ByteArray?,
) : NextStepViewModel()

data class FinishSettings(val reason: String) : NextStepViewModel()

data class FinishSettingsWithResult(val result: Int, val reason: String) : NextStepViewModel()

object ShowSettings : NextStepViewModel()

object LaunchedActivity : NextStepViewModel()

data class LaunchConfirmDeviceCredential(val userId: Int) : NextStepViewModel()
