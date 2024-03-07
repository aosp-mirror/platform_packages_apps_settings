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

package com.android.settings.remoteauth.enrolling

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlin.properties.Delegates

class RemoteAuthEnrollEnrollingViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(RemoteAuthEnrollEnrollingUiState())
    val uiState: StateFlow<RemoteAuthEnrollEnrollingUiState> = _uiState.asStateFlow()

    private var errorMessage: String? = null
        set(value) {
            field = value
            _uiState.update { currentState ->
                currentState.copy(
                    errorMsg = value,
                )
            }
        }

    // TODO(b/293906744): Change to RemoteAuthManager.DiscoveredDevice.
    private var selectedDevice: Any? by Delegates.observable(null) { _, _, _ -> discoverDevices() }


    /** Returns if a device has been selected */
    fun isDeviceSelected() = selectedDevice != null

    /**
     * Starts searching for nearby authenticators that are currently not enrolled. The devices
     * and the state of the searching are both returned in uiState.
     */
    fun discoverDevices() {
        _uiState.update { currentState ->
            currentState.copy(enrollmentUiState = EnrollmentUiState.FINDING_DEVICES)
        }

        // TODO(b/293906744): Map RemoteAuthManager discovered devices to
        // DiscoveredAuthenticatorUiState in viewModelScope.
        val discoveredDeviceUiStates = listOf<DiscoveredAuthenticatorUiState>()

        _uiState.update { currentState ->
            currentState.copy(
                discoveredDeviceUiStates = discoveredDeviceUiStates,
                enrollmentUiState = EnrollmentUiState.NONE
            )
        }
    }

    /** Registers the selected discovered device, if one is selected. */
    fun registerAuthenticator() {
        // TODO(b/293906744): Call RemoteAuthManager.register with selected device and update
        //  _uiState.
    }
}