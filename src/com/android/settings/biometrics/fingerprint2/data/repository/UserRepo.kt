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

package com.android.settings.biometrics.fingerprint2.data.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.update

/**
 * A repository responsible for indicating the current user.
 */
interface UserRepo {
    /**
     * This flow indicates the current user.
     */
    val currentUser: Flow<Int>

    /**
     * Updates the current user.
     */
    fun updateUser(user: Int)
}

class UserRepoImpl(currUser: Int): UserRepo {
    private val _currentUser = MutableStateFlow(currUser)
    override val currentUser = _currentUser.asStateFlow()

    override fun updateUser(user: Int) {
        _currentUser.update { user }
    }
}
