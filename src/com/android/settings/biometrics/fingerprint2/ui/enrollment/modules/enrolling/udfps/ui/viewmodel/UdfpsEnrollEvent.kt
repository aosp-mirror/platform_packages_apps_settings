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

package com.android.settings.biometrics.fingerprint2.ui.enrollment.modules.enrolling.udfps.ui.viewmodel

/** A class indicating a udfps enroll event occurred. */
sealed class UdfpsEnrollEvent

/** Describes how many [remainingSteps] and how many [totalSteps] are left in udfps enrollment. */
data class UdfpsProgress(val remainingSteps: Int, val totalSteps: Int) : UdfpsEnrollEvent()

/** Indicates a help event has been sent by enrollment */
data class UdfpsHelp(val helpMsgId: Int, val helpString: String) : UdfpsEnrollEvent()

/** Indicates a error event has been sent by enrollment */
data class UdfpsError(val errMsgId: Int, val errString: String) : UdfpsEnrollEvent()

/** Indicates an acquired event has occurred */
data class Acquired(val acquiredGood: Boolean) : UdfpsEnrollEvent()

/** Indicates a pointer down event has occurred */
data object PointerDown : UdfpsEnrollEvent()

/** Indicates a pointer up event has occurred */
data object PointerUp : UdfpsEnrollEvent()

/** Indicates the overlay has shown */
data object OverlayShown : UdfpsEnrollEvent()
