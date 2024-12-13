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
import android.provider.Settings
import android.telephony.SubscriptionManager
import android.util.Log
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
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.lifecycle.LifecycleRegistry
import com.android.settings.R
import com.android.settings.SidecarFragment
import com.android.settings.network.telephony.SimRepository
import com.android.settings.network.telephony.SubscriptionActionDialogActivity
import com.android.settings.network.telephony.ToggleSubscriptionDialogActivity
import com.android.settings.network.telephony.requireSubscriptionManager
import com.android.settings.spa.SpaActivity.Companion.startSpaActivity
import com.android.settings.spa.network.SimOnboardingPageProvider.getRoute
import com.android.settings.wifi.WifiPickerTrackerHelper
import com.android.settingslib.spa.SpaBaseDialogActivity
import com.android.settingslib.spa.framework.theme.SettingsDimension
import com.android.settingslib.spa.framework.util.collectLatestWithLifecycle
import com.android.settingslib.spa.widget.dialog.AlertDialogButton
import com.android.settingslib.spa.widget.dialog.SettingsAlertDialogWithIcon
import com.android.settingslib.spa.widget.dialog.getDialogWidth
import com.android.settingslib.spa.widget.dialog.rememberAlertDialogPresenter
import com.android.settingslib.spa.widget.ui.SettingsTitle
import com.android.settingslib.spaprivileged.framework.common.userManager
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class SimOnboardingActivity : SpaBaseDialogActivity() {
    lateinit var scope: CoroutineScope
    lateinit var wifiPickerTrackerHelper: WifiPickerTrackerHelper
    lateinit var context: Context
    lateinit var showStartingDialog: MutableState<Boolean>
    lateinit var showError: MutableState<ErrorType>
    lateinit var showProgressDialog: MutableState<Boolean>
    lateinit var showDsdsProgressDialog: MutableState<Boolean>
    lateinit var showRestartDialog: MutableState<Boolean>

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

            CallbackType.CALLBACK_ENABLE_DSDS-> {
                scope.launch {
                    onboardingService.startEnableDsds(this@SimOnboardingActivity)
                }
            }

            CallbackType.CALLBACK_ONBOARDING_COMPLETE -> {
                showStartingDialog.value = false
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
                    onboardingService.startSetupPrimarySim(
                        this@SimOnboardingActivity,
                        wifiPickerTrackerHelper
                    )
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
        showStartingDialog = rememberSaveable { mutableStateOf(false) }
        showError = rememberSaveable { mutableStateOf(ErrorType.ERROR_NONE) }
        showProgressDialog = rememberSaveable { mutableStateOf(false) }
        showDsdsProgressDialog = rememberSaveable { mutableStateOf(false) }
        showRestartDialog = rememberSaveable { mutableStateOf(false) }
        scope = rememberCoroutineScope()
        context = LocalContext.current
        val lifecycleOwner = LocalLifecycleOwner.current
        wifiPickerTrackerHelper = WifiPickerTrackerHelper(
            LifecycleRegistry(lifecycleOwner), context,
            null /* WifiPickerTrackerCallback */
        )

        registerSidecarReceiverFlow()

        ErrorDialogImpl()
        RestartDialogImpl()
        LaunchedEffect(Unit) {
            if (showError.value != ErrorType.ERROR_NONE
                || showProgressDialog.value
                || showDsdsProgressDialog.value
                || showRestartDialog.value) {
                Log.d(TAG, "status: showError:${showError.value}, " +
                        "showProgressDialog:${showProgressDialog.value}, " +
                        "showDsdsProgressDialog:${showDsdsProgressDialog.value}, " +
                        "showRestartDialog:${showRestartDialog.value}")
                showStartingDialog.value = false
            } else if (onboardingService.activeSubInfoList.isNotEmpty()) {
                Log.d(TAG, "status: showStartingDialog.value:${showStartingDialog.value}")
                showStartingDialog.value = true
            }
        }

        if (showStartingDialog.value) {
            StartingDialogImpl(
                nextAction = {
                    if (onboardingService.isDsdsConditionSatisfied()) {
                        // TODO: if the phone is SS mode and the isDsdsConditionSatisfied is true,
                        //  then enable the DSDS mode.
                        //  case#1: the device need the reboot after enabling DSDS. Showing the
                        //          confirm dialog to user whether reboot device or not.
                        //  case#2: The device don't need the reboot. Enabling DSDS and then showing
                        //          the SIM onboarding UI.
                        if (onboardingService.doesSwitchMultiSimConfigTriggerReboot) {
                            // case#1
                            Log.d(TAG, "Device does not support reboot free DSDS.")
                            showRestartDialog.value = true
                        } else {
                            // case#2
                            Log.d(TAG, "Enable DSDS mode")
                            showDsdsProgressDialog.value = true
                            enableMultiSimSidecar?.run(SimOnboardingService.NUM_OF_SIMS_FOR_DSDS)
                        }
                    } else {
                        startSimOnboardingProvider()
                    }
                },
                cancelAction = { finish() },
            )
        }

        if (showProgressDialog.value) {
            ProgressDialogImpl(
                stringResource(
                    R.string.sim_onboarding_progressbar_turning_sim_on,
                    onboardingService.targetSubInfo?.displayName ?: ""
                )
            )
        }
        if (showDsdsProgressDialog.value) {
            ProgressDialogImpl(
                stringResource(R.string.sim_action_enabling_sim_without_carrier_name)
            )
        }
    }
    @Composable
    private fun RestartDialogImpl() {
        val restartDialogPresenter = rememberAlertDialogPresenter(
            confirmButton = AlertDialogButton(
                stringResource(R.string.sim_action_reboot)
            ) {
                callbackListener(CallbackType.CALLBACK_ENABLE_DSDS)
            },
            dismissButton = AlertDialogButton(
                stringResource(
                    R.string.sim_action_restart_dialog_cancel,
                    onboardingService.targetSubInfo?.displayName ?: "")
            ) {
                callbackListener(CallbackType.CALLBACK_ONBOARDING_COMPLETE)
            },
            title = stringResource(R.string.sim_action_restart_dialog_title),
            text = {
                Text(stringResource(R.string.sim_action_restart_dialog_msg))
            },
        )

        if(showRestartDialog.value){
            LaunchedEffect(Unit) {
                restartDialogPresenter.open()
            }
        }
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    fun ProgressDialogImpl(title: String) {
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
                        SettingsTitle(title)
                    }
                }
            }
        }
    }

    @Composable
    fun ErrorDialogImpl(){
        // EuiccSlotSidecar showErrorDialog
        val errorDialogPresenterForSimSwitching = rememberAlertDialogPresenter(
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
            ErrorType.ERROR_SIM_SWITCHING -> errorDialogPresenterForSimSwitching.open()
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

    fun startSimSwitching() {
        Log.d(TAG, "startSimSwitching:")

        var targetSubInfo = onboardingService.targetSubInfo
        if(onboardingService.doesTargetSimActive) {
            Log.d(TAG, "target subInfo is already active")
            callbackListener(CallbackType.CALLBACK_SETUP_NAME)
            return
        }
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
                scope.launch {
                    checkSimIsReadyAndGoNext()
                }
            }

            SidecarFragment.State.ERROR -> {
                Log.i(TAG, "Failed to enable the eSIM profile.")
                switchToEuiccSubscriptionSidecar!!.reset()
                showError.value = ErrorType.ERROR_SIM_SWITCHING
                callbackListener(CallbackType.CALLBACK_ERROR)
            }
        }
    }

    fun handleSwitchToRemovableSlotSidecarStateChange() {
        when (switchToRemovableSlotSidecar!!.state) {
            SidecarFragment.State.SUCCESS -> {
                Log.i(TAG, "Successfully switched to removable slot.")
                switchToRemovableSlotSidecar!!.reset()
                onboardingService.handleTogglePsimAction()
                scope.launch {
                    checkSimIsReadyAndGoNext()
                }
            }

            SidecarFragment.State.ERROR -> {
                Log.e(TAG, "Failed switching to removable slot.")
                switchToRemovableSlotSidecar!!.reset()
                showError.value = ErrorType.ERROR_SIM_SWITCHING
                callbackListener(CallbackType.CALLBACK_ERROR)
            }
        }
    }

    fun handleEnableMultiSimSidecarStateChange() {
        when (enableMultiSimSidecar!!.state) {
            SidecarFragment.State.SUCCESS -> {
                enableMultiSimSidecar!!.reset()
                Log.i(TAG, "Successfully switched to DSDS without reboot.")
                showDsdsProgressDialog.value = false
                // refresh data
                initServiceData(this, onboardingService.targetSubId, callbackListener)
                startSimOnboardingProvider()
            }

            SidecarFragment.State.ERROR -> {
                enableMultiSimSidecar!!.reset()
                showDsdsProgressDialog.value = false
                Log.i(TAG, "Failed to switch to DSDS without rebooting.")
                showError.value = ErrorType.ERROR_ENABLE_DSDS
                callbackListener(CallbackType.CALLBACK_ERROR)
            }
        }
    }

    suspend fun checkSimIsReadyAndGoNext() {
        withContext(Dispatchers.Default) {
            val isEnabled = context.requireSubscriptionManager()
                .isSubscriptionEnabled(onboardingService.targetSubId)
            if (!isEnabled) {
                val latch = CountDownLatch(1)
                val receiver = CarrierConfigChangedReceiver(latch)
                try {
                    val waitingTimeMillis =
                        Settings.Global.getLong(
                            context.contentResolver,
                            Settings.Global.EUICC_SWITCH_SLOT_TIMEOUT_MILLIS,
                            UiccSlotUtil.DEFAULT_WAIT_AFTER_SWITCH_TIMEOUT_MILLIS
                        )
                    receiver.registerOn(context)
                    Log.d(TAG, "Start waiting, waitingTime is $waitingTimeMillis")
                    latch.await(waitingTimeMillis, TimeUnit.MILLISECONDS)
                } catch (e: InterruptedException) {
                    Thread.currentThread().interrupt()
                    Log.e(TAG, "Failed switching to physical slot.", e)
                } finally {
                    context.unregisterReceiver(receiver)
                }
            }
            Log.d(TAG, "Sim is ready then go to next")
            callbackListener(CallbackType.CALLBACK_SETUP_NAME)
        }
    }

    @Composable
    fun StartingDialogImpl(
        nextAction: () -> Unit,
        cancelAction: () -> Unit,
    ) {
        SettingsAlertDialogWithIcon(
            onDismissRequest = cancelAction,
            confirmButton = AlertDialogButton(
                text = getString(R.string.sim_onboarding_setup),
                onClick = nextAction,
            ),
            dismissButton = AlertDialogButton(
                text = getString(R.string.sim_onboarding_close),
                onClick = cancelAction,
            ),
            title = stringResource(R.string.sim_onboarding_dialog_starting_title),
            icon = {
                Icon(
                    imageVector = Icons.Outlined.SignalCellularAlt,
                    contentDescription = null,
                    modifier = Modifier
                        .size(SettingsDimension.iconLarge),
                    tint = MaterialTheme.colorScheme.primary,
                )
            },
            text = {
                Text(
                    stringResource(R.string.sim_onboarding_dialog_starting_msg),
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center
                )
            })

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

    private fun startSimOnboardingProvider() {
        val route = getRoute(onboardingService.targetSubId)
        startSpaActivity(route)
    }

    companion object {
        @JvmStatic
        fun startSimOnboardingActivity(
            context: Context,
            subId: Int,
            isNewTask: Boolean = false,
        ) {
            if (!SimRepository(context).canEnterMobileNetworkPage()) {
                Log.i(TAG, "Unable to start SimOnboardingActivity due to missing permissions")
                return
            }
            val intent = Intent(context, SimOnboardingActivity::class.java).apply {
                putExtra(SUB_ID, subId)
                if(isNewTask) {
                    setFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
            }
            context.startActivity(intent)
        }

        var onboardingService:SimOnboardingService = SimOnboardingService()
        const val TAG = "SimOnboardingActivity"
        const val SUB_ID = "sub_id"

        enum class ErrorType(val value:Int){
            ERROR_NONE(-1),
            ERROR_SIM_SWITCHING(1),
            ERROR_ENABLE_DSDS(2)
        }

        enum class CallbackType(val value:Int){
            CALLBACK_ERROR(-1),
            CALLBACK_ONBOARDING_COMPLETE(1),
            CALLBACK_ENABLE_DSDS(2),
            CALLBACK_SETUP_NAME(3),
            CALLBACK_SETUP_PRIMARY_SIM(4),
            CALLBACK_FINISH(5)
        }
    }
}