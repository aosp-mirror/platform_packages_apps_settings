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

package com.android.settings.remoteauth.settings

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class RemoteAuthSettingsViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(RemoteAuthSettingsUiState())
    val uiState: StateFlow<RemoteAuthSettingsUiState> = _uiState.asStateFlow()

    private var errorMessage: String? = null
        set(value) {
            field = value
            _uiState.update { currentState ->
                currentState.copy(
                    errorMsg = value,
                )
            }
        }

    fun refreshAuthenticatorList() {
        // TODO(b/290768873): Pull from RemoteAuthenticationManager and map to UIState
        val authenticatorUiStates = listOf<RemoteAuthAuthenticatorItemUiState>()

        _uiState.update { currentState ->
            currentState.copy(
                registeredAuthenticatorUiStates = authenticatorUiStates,
            )
        }
    }

    /** Called by UI when user has acknowledged they seen the error dialog, via ok button. */
    fun resetErrorMessage() {
        errorMessage = null
    }
}