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

import android.os.Bundle
import android.view.View
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.NavHostFragment.Companion.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.android.settings.R
import kotlinx.coroutines.launch

class RemoteAuthSettings : Fragment(R.layout.remote_auth_settings) {

    val viewModel: RemoteAuthSettingsViewModel by viewModels()
    private val adapter = RemoteAuthSettingsRecyclerViewAdapter()
    private val recyclerView by lazy {
        view!!.requireViewById<RecyclerView>(R.id.registered_authenticator_list)
    }

    private val addAuthenticatorLayout by lazy {
        view!!.requireViewById<ConstraintLayout>(R.id.add_authenticator_layout)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        recyclerView.layoutManager = LinearLayoutManager(context)
        recyclerView.adapter = adapter

        // Add new remote authenticator click listener
        addAuthenticatorLayout.setOnClickListener {
            findNavController(this).navigate(R.id.action_settings_to_introduction)
        }

        // Collect UIState and update UI on changes.
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect {
                    updateUi(it)
                }
            }
        }

    }

    private fun updateUi(uiState: RemoteAuthSettingsUiState) {
        adapter.uiStates = uiState.registeredAuthenticatorUiStates
    }

}
