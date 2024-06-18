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

package com.android.settings.network

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.telephony.SubscriptionManager
import android.util.Log
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.SignalCellularAlt
import androidx.compose.material3.AlertDialogDefaults
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import com.android.settings.R
import com.android.settings.SidecarFragment
import com.android.settings.network.telephony.SubscriptionActionDialogActivity
import com.android.settings.network.telephony.ToggleSubscriptionDialogActivity
import com.android.settings.spa.SpaActivity.Companion.startSpaActivity
import com.android.settings.spa.network.SimOnboardingPageProvider.getRoute
import com.android.settingslib.spa.SpaBaseDialogActivity
import com.android.settingslib.spa.framework.theme.SettingsDimension
import com.android.settingslib.spa.framework.util.collectLatestWithLifecycle
import com.android.settingslib.spa.widget.dialog.AlertDialogButton
import com.android.settingslib.spa.widget.dialog.getDialogWidth
import com.android.settingslib.spa.widget.dialog.rememberAlertDialogPresenter
import com.android.settingslib.spa.widget.editor.SettingsOutlinedTextField
import com.android.settingslib.spa.widget.ui.SettingsTitle
import com.android.settingslib.spaprivileged.framework.common.userManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.launch

class SimOnboardingActivity : SpaBaseDialogActivity() {
    lateinit var scope: CoroutineScope
    lateinit var showBottomSheet: MutableState<Boolean>
    lateinit var showError: MutableState<ErrorType>
    lateinit var showProgressDialog: MutableState<Boolean>

    private var switchToEuiccSubscriptionSidecar: SwitchToEuiccSubscriptionSidecar? = null
    private var switchToRemovableSlotSidecar: SwitchToRemovableSlotSidecar? = null
    private var enableMultiSimSidecar: EnableMultiSimSidecar? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (!this.userManager.isAdminUser) {
            Log.e(TAG, "It is not the admin user. Unable to toggle subscription.")
            finish()
            return
        }

        var targetSubId = intent.getIntExtra(SUB_ID,SubscriptionManager.INVALID_SUBSCRIPTION_ID)
        initServiceData(this, targetSubId, callbackListener)
        if (!onboardingService.isUsableTargetSubscriptionId) {
            Log.e(TAG, "The subscription id is not usable.")
            finish()
            return
        }

        if (onboardingService.activeSubInfoList.isEmpty()) {
            // TODO: refactor and replace the ToggleSubscriptionDialogActivity
            Log.d(TAG, "onboardingService.activeSubInfoList is empty" +
                    ", start ToggleSubscriptionDialogActivity")
            this.startActivity(ToggleSubscriptionDialogActivity
                    .getIntent(this.applicationContext, targetSubId, true))
            finish()
            return
        }

        switchToEuiccSubscriptionSidecar = SwitchToEuiccSubscriptionSidecar.get(fragmentManager)
        switchToRemovableSlotSidecar = SwitchToRemovableSlotSidecar.get(fragmentManager)
        enableMultiSimSidecar = EnableMultiSimSidecar.get(fragmentManager)
    }

    override fun finish() {
        setProgressDialog(false)
        onboardingService.clear()
        super.finish()
    }

    var callbackListener: (CallbackType) -> Unit = {
        Log.d(TAG, "Receive the CALLBACK: $it")
        when (it) {
            CallbackType.CALLBACK_ERROR -> {
                setProgressDialog(false)
            }

            CallbackType.CALLBACK_ONBOARDING_COMPLETE -> {
                showBottomSheet.value = false
                setProgressDialog(true)
                scope.launch {
                    // TODO: refactor the Sidecar
                    // start to activate the sim
                    startSimSwitching()
                }
            }

            CallbackType.CALLBACK_SETUP_NAME -> {
                scope.launch {
                    onboardingService.startSetupName()
                }
            }

            CallbackType.CALLBACK_SETUP_PRIMARY_SIM -> {
                scope.launch {
                    onboardingService.startSetupPrimarySim(this@SimOnboardingActivity)
                }
            }

            CallbackType.CALLBACK_FINISH -> {
                finish()
            }
        }
    }

    fun setProgressDialog(enable: Boolean) {
        if (!this::showProgressDialog.isInitialized) {
            return
        }
        showProgressDialog.value = enable
        val progressState = if (enable) {
            SubscriptionActionDialogActivity.PROGRESS_IS_SHOWING
        } else {
            SubscriptionActionDialogActivity.PROGRESS_IS_NOT_SHOWING
        }
        setProgressState(progressState)
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content() {
        showBottomSheet = remember { mutableStateOf(false) }
        showError = remember { mutableStateOf(ErrorType.ERROR_NONE) }
        showProgressDialog = remember { mutableStateOf(false) }
        scope = rememberCoroutineScope()

        registerSidecarReceiverFlow()

        ErrorDialogImpl()

        LaunchedEffect(Unit) {
            if (onboardingService.activeSubInfoList.isNotEmpty()) {
                showBottomSheet.value = true
            }
        }

        if (showBottomSheet.value) {
            var sheetState = rememberModalBottomSheetState()
            BottomSheetImpl(
                sheetState = sheetState,
                nextAction = {
                    // TODO: if the phone is SS mode and the isDsdsConditionSatisfied is true, then
                    //  enable the DSDS mode.
                    //  case#1: the device need the reboot after enabling DSDS. Showing the confirm
                    //          dialog to user whether reboot device or not.
                    //  case#2: The device don't need the reboot. Enabling DSDS and then showing
                    //          the SIM onboarding UI.

                    // case#2
                    val route = getRoute(onboardingService.targetSubId)
                    startSpaActivity(route)
                },
                cancelAction = { finish() },
            )
        }

        if(showProgressDialog.value) {
            ProgressDialogImpl()
        }
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    fun ProgressDialogImpl() {
        // TODO: Create the SPA's ProgressDialog and using SPA's widget
        BasicAlertDialog(
            onDismissRequest = {},
            modifier = Modifier.width(
                getDialogWidth()
            ),
        ) {
            Surface(
                color = AlertDialogDefaults.containerColor,
                shape = AlertDialogDefaults.shape
            ) {
                Row(
                    modifier = Modifier
                            .fillMaxWidth()
                            .padding(SettingsDimension.itemPaddingStart),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CircularProgressIndicator()
                    Column(modifier = Modifier
                            .padding(start = SettingsDimension.itemPaddingStart)) {
                        SettingsTitle(
                            stringResource(
                                R.string.sim_onboarding_progressbar_turning_sim_on,
                                onboardingService.targetSubInfo?.displayName ?: ""
                            )
                        )
                    }
                }
            }
        }
    }

    @Composable
    fun ErrorDialogImpl(){
        // EuiccSlotSidecar showErrorDialog
        val errorDialogPresenterForEuiccSlotSidecar = rememberAlertDialogPresenter(
                confirmButton = AlertDialogButton(
                        stringResource(android.R.string.ok)
                ) {
                    finish()
                },
                title = stringResource(R.string.privileged_action_disable_fail_title),
                text = {
                    Text(stringResource(R.string.privileged_action_disable_fail_text))
                },
        )

        // RemovableSlotSidecar showErrorDialog
        val errorDialogPresenterForRemovableSlotSidecar = rememberAlertDialogPresenter(
                confirmButton = AlertDialogButton(
                        stringResource(android.R.string.ok)
                ) {
                    finish()
                },
                title = stringResource(R.string.sim_action_enable_sim_fail_title),
                text = {
                    Text(stringResource(R.string.sim_action_enable_sim_fail_text))
                },
        )

        // enableDSDS showErrorDialog
        val errorDialogPresenterForMultiSimSidecar = rememberAlertDialogPresenter(
                confirmButton = AlertDialogButton(
                        stringResource(android.R.string.ok)
                ) {
                    finish()
                },
                title = stringResource(R.string.dsds_activation_failure_title),
                text = {
                    Text(stringResource(R.string.dsds_activation_failure_body_msg2))
                },
        )

        // show error
        when (showError.value) {
            ErrorType.ERROR_EUICC_SLOT -> errorDialogPresenterForEuiccSlotSidecar.open()
            ErrorType.ERROR_REMOVABLE_SLOT -> errorDialogPresenterForRemovableSlotSidecar.open()
            ErrorType.ERROR_ENABLE_DSDS -> errorDialogPresenterForMultiSimSidecar.open()
            else -> {}
        }
    }

    @Composable
    fun registerSidecarReceiverFlow(){
        switchToEuiccSubscriptionSidecar?.sidecarReceiverFlow()
            ?.collectLatestWithLifecycle(LocalLifecycleOwner.current) {
                onStateChange(it)
            }
        switchToRemovableSlotSidecar?.sidecarReceiverFlow()
            ?.collectLatestWithLifecycle(LocalLifecycleOwner.current) {
                onStateChange(it)
            }
        enableMultiSimSidecar?.sidecarReceiverFlow()
            ?.collectLatestWithLifecycle(LocalLifecycleOwner.current) {
                onStateChange(it)
            }
    }

    fun SidecarFragment.sidecarReceiverFlow(): Flow<SidecarFragment> = callbackFlow {
        val broadcastReceiver = SidecarFragment.Listener {
            Log.d(TAG, "onReceive: $it")
            trySend(it)
        }
        addListener(broadcastReceiver)

        awaitClose { removeListener(broadcastReceiver) }
    }.catch { e ->
        Log.e(TAG, "Error while sidecarReceiverFlow", e)
    }.conflate()

    fun startSimSwitching(){
        Log.d(TAG, "startSimSwitching:")

        var targetSubInfo = onboardingService.targetSubInfo
        targetSubInfo?.let {
            var removedSubInfo = onboardingService.getRemovedSim()
            if (targetSubInfo.isEmbedded) {
                switchToEuiccSubscriptionSidecar!!.run(
                    targetSubInfo.subscriptionId,
                    UiccSlotUtil.INVALID_PORT_ID,
                    removedSubInfo
                )
                return@let
            }
            switchToRemovableSlotSidecar!!.run(
                UiccSlotUtil.INVALID_PHYSICAL_SLOT_ID,
                removedSubInfo
            )
        } ?: run {
            Log.e(TAG, "no target subInfo in onboardingService")
            finish()
        }
    }

    fun onStateChange(fragment: SidecarFragment?) {
        if (fragment === switchToEuiccSubscriptionSidecar) {
            handleSwitchToEuiccSubscriptionSidecarStateChange()
        } else if (fragment === switchToRemovableSlotSidecar) {
            handleSwitchToRemovableSlotSidecarStateChange()
        } else if (fragment === enableMultiSimSidecar) {
            handleEnableMultiSimSidecarStateChange()
        }
    }

    fun handleSwitchToEuiccSubscriptionSidecarStateChange() {
        when (switchToEuiccSubscriptionSidecar!!.state) {
            SidecarFragment.State.SUCCESS -> {
                Log.i(TAG, "Successfully enable the eSIM profile.")
                switchToEuiccSubscriptionSidecar!!.reset()
                callbackListener(CallbackType.CALLBACK_SETUP_NAME)
            }

            SidecarFragment.State.ERROR -> {
                Log.i(TAG, "Failed to enable the eSIM profile.")
                switchToEuiccSubscriptionSidecar!!.reset()
                showError.value = ErrorType.ERROR_EUICC_SLOT
                callbackListener(CallbackType.CALLBACK_ERROR)
                // TODO: showErrorDialog and using privileged_action_disable_fail_title and
                //       privileged_action_disable_fail_text
            }
        }
    }

    fun handleSwitchToRemovableSlotSidecarStateChange() {
        when (switchToRemovableSlotSidecar!!.state) {
            SidecarFragment.State.SUCCESS -> {
                Log.i(TAG, "Successfully switched to removable slot.")
                switchToRemovableSlotSidecar!!.reset()
                onboardingService.handleTogglePsimAction()
                callbackListener(CallbackType.CALLBACK_SETUP_NAME)
            }

            SidecarFragment.State.ERROR -> {
                Log.e(TAG, "Failed switching to removable slot.")
                switchToRemovableSlotSidecar!!.reset()
                showError.value = ErrorType.ERROR_REMOVABLE_SLOT
                callbackListener(CallbackType.CALLBACK_ERROR)
                // TODO: showErrorDialog and using sim_action_enable_sim_fail_title and
                //       sim_action_enable_sim_fail_text
            }
        }
    }

    fun handleEnableMultiSimSidecarStateChange() {
        when (enableMultiSimSidecar!!.state) {
            SidecarFragment.State.SUCCESS -> {
                enableMultiSimSidecar!!.reset()
                Log.i(TAG, "Successfully switched to DSDS without reboot.")
                handleEnableSubscriptionAfterEnablingDsds()
            }

            SidecarFragment.State.ERROR -> {
                enableMultiSimSidecar!!.reset()
                Log.i(TAG, "Failed to switch to DSDS without rebooting.")
                showError.value = ErrorType.ERROR_ENABLE_DSDS
                callbackListener(CallbackType.CALLBACK_ERROR)
                // TODO: showErrorDialog and using dsds_activation_failure_title and
                //       dsds_activation_failure_body_msg2
            }
        }
    }

    fun handleEnableSubscriptionAfterEnablingDsds() {
        var targetSubInfo = onboardingService.targetSubInfo
        if (targetSubInfo?.isEmbedded == true) {
            Log.i(TAG,
                    "DSDS enabled, start to enable profile: " + targetSubInfo.getSubscriptionId()
            )
            // For eSIM operations, we simply switch to the selected eSIM profile.
            switchToEuiccSubscriptionSidecar!!.run(
                targetSubInfo.subscriptionId,
                UiccSlotUtil.INVALID_PORT_ID,
                null
            )
            return
        }
        Log.i(TAG, "DSDS enabled, start to enable pSIM profile.")
        onboardingService.handleTogglePsimAction()
        callbackListener(CallbackType.CALLBACK_FINISH)
    }

    @Composable
    fun BottomSheetBody(nextAction: () -> Unit) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(bottom = SettingsDimension.itemPaddingVertical)) {
            Icon(
                imageVector = Icons.Outlined.SignalCellularAlt,
                contentDescription = null,
                modifier = Modifier
                    .size(SettingsDimension.iconLarge),
                tint = MaterialTheme.colorScheme.primary,
            )
            SettingsTitle(stringResource(R.string.sim_onboarding_bottomsheets_title))
            Column(Modifier.padding(SettingsDimension.itemPadding)) {
                Text(
                    text = stringResource(R.string.sim_onboarding_bottomsheets_msg),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodyMedium,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center
                )
            }
            Button(onClick = nextAction) {
                Text(stringResource(R.string.sim_onboarding_setup))
            }
        }
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    fun BottomSheetImpl(
        sheetState: SheetState,
        nextAction: () -> Unit,
        cancelAction: () -> Unit,
    ) {
        ModalBottomSheet(
            onDismissRequest = cancelAction,
            sheetState = sheetState,
        ) {
            BottomSheetBody(nextAction = nextAction)
        }
        LaunchedEffect(Unit) {
            sheetState.show()
        }
    }

    fun setProgressState(state: Int) {
        val prefs = getSharedPreferences(
            SubscriptionActionDialogActivity.SIM_ACTION_DIALOG_PREFS,
            MODE_PRIVATE
        )
        prefs.edit().putInt(SubscriptionActionDialogActivity.KEY_PROGRESS_STATE, state).apply()
        Log.i(TAG, "setProgressState:$state")
    }

    fun initServiceData(context: Context,targetSubId: Int, callback:(CallbackType)->Unit) {
        onboardingService.initData(targetSubId, context,callback)
    }

    companion object {
        @JvmStatic
        fun startSimOnboardingActivity(
            context: Context,
            subId: Int,
        ) {
            val intent = Intent(context, SimOnboardingActivity::class.java).apply {
                putExtra(SUB_ID, subId)
            }
            context.startActivity(intent)
        }

        var onboardingService:SimOnboardingService = SimOnboardingService()
        const val TAG = "SimOnboardingActivity"
        const val SUB_ID = "sub_id"

        enum class ErrorType(val value:Int){
            ERROR_NONE(-1),
            ERROR_EUICC_SLOT(1),
            ERROR_REMOVABLE_SLOT(2),
            ERROR_ENABLE_DSDS(3)
        }

        enum class CallbackType(val value:Int){
            CALLBACK_ERROR(-1),
            CALLBACK_ONBOARDING_COMPLETE(1),
            CALLBACK_SETUP_NAME(2),
            CALLBACK_SETUP_PRIMARY_SIM(3),
            CALLBACK_FINISH(4)
        }
    }
}