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

package com.android.settings.biometrics.fingerprint2.shared.model

/**
 * The [FingerprintFlow] for fingerprint enrollment indicates information on how the flow should behave.
 */
sealed class FingerprintFlow

/** The default enrollment experience, typically called from Settings */
data object Default : FingerprintFlow()

/** SetupWizard/Out of box experience (OOBE) enrollment type. */
data object SetupWizard : FingerprintFlow()

/** Unicorn enrollment type */
data object Unicorn : FingerprintFlow()

/** Flow to specify settings type */
data object Settings : FingerprintFlow()
