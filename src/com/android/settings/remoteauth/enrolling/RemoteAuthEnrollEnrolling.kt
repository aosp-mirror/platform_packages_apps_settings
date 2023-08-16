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

import android.os.Bundle
import android.view.View
import android.widget.ProgressBar
import android.widget.TextView
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle


import androidx.navigation.fragment.NavHostFragment.Companion.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

import com.android.settings.R
import com.android.settings.remoteauth.RemoteAuthEnrollBase

import com.google.android.setupcompat.template.FooterButton
import kotlinx.coroutines.launch

class RemoteAuthEnrollEnrolling :
    RemoteAuthEnrollBase(
        layoutResId = R.layout.remote_auth_enroll_enrolling,
        glifLayoutId = R.id.setup_wizard_layout,
    ) {
    private val viewModel: RemoteAuthEnrollEnrollingViewModel by viewModels()
    private val navController by lazy { findNavController(this) }
    private val adapter = RemoteAuthEnrollEnrollingRecyclerViewAdapter()
    private val progressBar by lazy {
        view!!.requireViewById<ProgressBar>(R.id.enrolling_list_progress_bar)
    }
    private val errorText by lazy { view!!.requireViewById<TextView>(R.id.error_text) }
    private val recyclerView by lazy {
        view!!.requireViewById<RecyclerView>(R.id.discovered_authenticator_list)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        // Set up adapter
        recyclerView.layoutManager = LinearLayoutManager(context)
        recyclerView.adapter = adapter

        // Collect UIState and update UI on changes.
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect {
                    updateUi(it)
                }
            }
        }
    }

    override fun onStart() {
        super.onStart()
        // Get list of discovered devices from viewModel.
        viewModel.discoverDevices()
    }

    override fun initializePrimaryFooterButton(): FooterButton {
        return FooterButton.Builder(requireContext())
            .setText(R.string.security_settings_remoteauth_enroll_enrolling_agree)
            .setListener(this::onPrimaryFooterButtonClick)
            .setButtonType(FooterButton.ButtonType.NEXT)
            .setTheme(com.google.android.setupdesign.R.style.SudGlifButton_Primary)
            .build()
    }

    override fun initializeSecondaryFooterButton(): FooterButton? {
        return FooterButton.Builder(requireContext())
            .setText(R.string.security_settings_remoteauth_enroll_enrolling_disagree)
            .setListener(this::onSecondaryFooterButtonClick)
            .setButtonType(FooterButton.ButtonType.SKIP)
            .setTheme(com.google.android.setupdesign.R.style.SudGlifButton_Secondary)
            .build()
    }

    private fun onPrimaryFooterButtonClick(view: View) {
        viewModel.registerAuthenticator()
    }

    private fun onSecondaryFooterButtonClick(view: View) {
        navController.navigateUp()
    }

    private fun updateUi(uiState: RemoteAuthEnrollEnrollingUiState) {
        progressBar.visibility = View.INVISIBLE
        primaryFooterButton.isEnabled = false
        // TODO(b/290769765): Add unit tests for all this states.
        when (uiState.enrollmentUiState) {
            EnrollmentUiState.NONE -> {
                adapter.uiStates = uiState.discoveredDeviceUiStates
                primaryFooterButton.isEnabled = viewModel.isDeviceSelected()
            }

            EnrollmentUiState.FINDING_DEVICES -> {
                progressBar.visibility = View.VISIBLE
            }

            EnrollmentUiState.ENROLLING -> {}
            EnrollmentUiState.SUCCESS -> {
                navController.navigate(R.id.action_enrolling_to_finish)
            }
        }
        if (uiState.errorMsg != null) {
            errorText.visibility = View.VISIBLE
            errorText.text = uiState.errorMsg
        } else {
            errorText.visibility = View.INVISIBLE
            errorText.text = ""
        }
    }
}
