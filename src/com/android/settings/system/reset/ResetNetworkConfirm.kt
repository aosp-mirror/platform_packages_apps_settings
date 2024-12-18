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

package com.android.settings.system.reset

import android.app.ProgressDialog
import android.app.settings.SettingsEnums
import android.content.DialogInterface
import android.os.Bundle
import android.os.Looper
import android.telephony.SubscriptionManager
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.VisibleForTesting
import androidx.appcompat.app.AlertDialog
import androidx.lifecycle.lifecycleScope
import com.android.settings.R
import com.android.settings.ResetNetworkRequest
import com.android.settings.Utils
import com.android.settings.core.InstrumentedFragment
import com.android.settings.network.ResetNetworkRestrictionViewBuilder
import com.android.settings.network.telephony.SubscriptionRepository
import com.android.settingslib.spa.framework.util.collectLatestWithLifecycle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Confirm and execute a reset of the network settings to a clean "just out of the box" state.
 * Multiple confirmations are required: first, a general "are you sure you want to do this?" prompt,
 * followed by a keyguard pattern trace if the user has defined one, followed by a final
 * strongly-worded "THIS WILL RESET EVERYTHING" prompt. If at any time the phone is allowed to go to
 * sleep, is locked, et cetera, then the confirmation sequence is abandoned.
 *
 * This is the confirmation screen.
 */
class ResetNetworkConfirm : InstrumentedFragment() {
    @VisibleForTesting
    lateinit var resetNetworkRequest: ResetNetworkRequest
    private var progressDialog: ProgressDialog? = null
    private var alertDialog: AlertDialog? = null
    private var resetStarted = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        Log.d(TAG, "onCreate: $arguments")
        resetNetworkRequest = ResetNetworkRequest(arguments)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = ResetNetworkRestrictionViewBuilder(requireActivity()).build()
        if (view != null) {
            Log.w(TAG, "Access deny.")
            return view
        }
        return inflater.inflate(R.layout.reset_network_confirm, null).apply {
            establishFinalConfirmationState()
            setSubtitle()
        }
    }

    /** Configure the UI for the final confirmation interaction */
    private fun View.establishFinalConfirmationState() {
        requireViewById<View>(R.id.execute_reset_network).setOnClickListener {
            showResetInternetDialog();
        }
    }

    private fun View.setSubtitle() {
        if (resetNetworkRequest.resetEsimPackageName != null) {
            requireViewById<TextView>(R.id.reset_network_confirm)
                .setText(R.string.reset_network_final_desc_esim)
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        invalidSubIdFlow().collectLatestWithLifecycle(viewLifecycleOwner) { invalidSubId ->
            // Reset process could triage this callback, so if reset has started, ignore the event.
            if (!resetStarted) {
                Log.w(TAG, "subId $invalidSubId no longer active.")
                requireActivity().finish()
            }
        }
    }

    /**
     * Monitor the sub ids in the request, if any sub id becomes inactive, the request is abandoned.
     */
    private fun invalidSubIdFlow(): Flow<Int> {
        val subIdsInRequest =
            listOf(
                resetNetworkRequest.resetTelephonyAndNetworkPolicyManager,
                resetNetworkRequest.resetApnSubId,
                resetNetworkRequest.resetImsSubId,
            )
                .distinct()
                .filter(SubscriptionManager::isUsableSubscriptionId)

        if (subIdsInRequest.isEmpty()) return emptyFlow()

        return SubscriptionRepository(requireContext())
            .activeSubscriptionIdListFlow()
            .mapNotNull { activeSubIds -> subIdsInRequest.firstOrNull { it !in activeSubIds } }
            .conflate()
            .flowOn(Dispatchers.Default)
    }

    /**
     * The user has gone through the multiple confirmation, so now we go ahead and reset the network
     * settings to its factory-default state.
     */
    @VisibleForTesting
    suspend fun onResetClicked() {
        showProgressDialog()
        resetNetwork()
    }

    private fun showProgressDialog() {
        progressDialog =
            ProgressDialog(requireContext()).apply {
                isIndeterminate = true
                setCancelable(false)
                setMessage(requireContext().getString(R.string.main_clear_progress_text))
                show()
            }
    }

    private fun dismissProgressDialog() {
        progressDialog?.let { progressDialog ->
            if (progressDialog.isShowing) {
                progressDialog.dismiss()
            }
        }
    }

    private fun showResetInternetDialog() {
        val builder = AlertDialog.Builder(requireContext())
        val resetInternetClickListener =
            DialogInterface.OnClickListener { dialog, which ->
                if (!Utils.isMonkeyRunning() && !resetStarted) {
                    resetStarted = true
                    viewLifecycleOwner.lifecycleScope.launch { onResetClicked() }
                }
            }

        builder.setTitle(R.string.reset_your_internet_title)
            .setMessage(R.string.reset_internet_text)
            .setPositiveButton(R.string.tts_reset, resetInternetClickListener)
            .setNegativeButton(android.R.string.cancel, null)
            .create()
            .show()
    }

    /**
     * Do all reset task.
     *
     * If error happens during erasing eSIM profiles or timeout, an error msg is shown.
     */
    private suspend fun resetNetwork() {
        var resetEsimSuccess = true

        withContext(Dispatchers.Default) {
            val builder =
                resetNetworkRequest.toResetNetworkOperationBuilder(
                    requireContext(), Looper.getMainLooper()
                )
            resetNetworkRequest.resetEsimPackageName?.let {
                builder.resetEsimResultCallback { resetEsimSuccess = it }
            }
            builder.build().run()
        }

        Log.d(TAG, "network factoryReset complete. succeeded: $resetEsimSuccess")
        onResetFinished(resetEsimSuccess)
    }

    private fun onResetFinished(resetEsimSuccess: Boolean) {
        dismissProgressDialog()
        val activity = requireActivity()

        if (!resetEsimSuccess) {
            alertDialog =
                AlertDialog.Builder(activity)
                    .setTitle(R.string.reset_esim_error_title)
                    .setMessage(R.string.reset_esim_error_msg)
                    .setPositiveButton(android.R.string.ok, /* listener= */ null)
                    .show()
        } else {
            Toast.makeText(activity, R.string.reset_network_complete_toast, Toast.LENGTH_SHORT)
                .show()
            activity.finish()
        }
    }

    override fun onDestroy() {
        progressDialog?.dismiss()
        alertDialog?.dismiss()
        super.onDestroy()
    }

    override fun getMetricsCategory() = SettingsEnums.RESET_NETWORK_CONFIRM

    private companion object {
        const val TAG = "ResetNetworkConfirm"
    }
}
