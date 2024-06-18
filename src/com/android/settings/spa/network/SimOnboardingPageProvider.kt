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

package com.android.settings.spa.network


import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.os.Bundle
import android.util.Log
import androidx.annotation.VisibleForTesting
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.android.settings.R
import com.android.settings.network.SimOnboardingActivity
import com.android.settings.network.SimOnboardingActivity.Companion.CallbackType
import com.android.settings.network.SimOnboardingService
import com.android.settingslib.spa.framework.common.SettingsEntryBuilder
import com.android.settingslib.spa.framework.common.SettingsPageProvider
import com.android.settingslib.spa.framework.common.createSettingsPage
import com.android.settingslib.spa.framework.compose.navigator
import com.android.settingslib.spa.widget.preference.Preference
import com.android.settingslib.spa.widget.preference.PreferenceModel

const val SUB_ID = "subId"

enum class SimOnboardingScreen(val stringResId: Int) {
    LabelSim(R.string.sim_onboarding_label_sim_title),
    SelectSim(R.string.sim_onboarding_select_sim_title),
    PrimarySim(R.string.sim_onboarding_primary_sim_title)
}

/**
 * Showing the sim onboarding which is the process flow of sim switching on.
 */
object SimOnboardingPageProvider : SettingsPageProvider {
    override val name = "SimOnboardingPageProvider"
    override val parameter = listOf(
        navArgument(SUB_ID) { type = NavType.IntType },
    )

    private val owner = createSettingsPage()
    @VisibleForTesting
    var onboardingService: SimOnboardingService = SimOnboardingActivity.onboardingService

    fun buildInjectEntry() = SettingsEntryBuilder.createInject(owner = owner)
        .setUiLayoutFn {
            // never using
            Preference(object : PreferenceModel {
                override val title = name
                override val onClick = navigator(getRoute(-1))
            })
        }

    @Composable
    override fun Page(arguments: Bundle?) {
        PageImpl(onboardingService,rememberNavController())
    }

    fun getRoute(
        subId: Int
    ): String = "${name}/$subId"
}

private fun Context.getActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.getActivity()
    else -> null
}

@Composable
fun PageImpl(onboardingService:SimOnboardingService,navHostController: NavHostController) {
    val context = LocalContext.current
    var finishOnboarding: () -> Unit = {
        context.getActivity()?.finish()
        onboardingService.callback(CallbackType.CALLBACK_FINISH)
    }

    NavHost(
        navController = navHostController,
        startDestination = SimOnboardingScreen.LabelSim.name
    ) {
        composable(route = SimOnboardingScreen.LabelSim.name) {
            val nextPage =
                if (onboardingService.isMultipleEnabledProfilesSupported
                            && onboardingService.isAllOfSlotAssigned) {
                    SimOnboardingScreen.SelectSim.name
                } else {
                    LaunchedEffect(Unit) {
                        onboardingService.addCurrentItemForSelectedSim()
                    }
                    SimOnboardingScreen.PrimarySim.name
                }
            SimOnboardingLabelSimImpl(
                nextAction = { navHostController.navigate(nextPage) },
                cancelAction = finishOnboarding,
                onboardingService = onboardingService
            )
        }
        composable(route = SimOnboardingScreen.PrimarySim.name) {
            SimOnboardingPrimarySimImpl(
                nextAction = {
                    onboardingService.callback(CallbackType.CALLBACK_ONBOARDING_COMPLETE)
                    context.getActivity()?.finish()
                },
                cancelAction = finishOnboarding,
                onboardingService = onboardingService
            )
        }
        composable(route = SimOnboardingScreen.SelectSim.name) {
            SimOnboardingSelectSimImpl(
                nextAction = { navHostController.navigate(SimOnboardingScreen.PrimarySim.name) },
                cancelAction = finishOnboarding,
                onboardingService = onboardingService
            )
        }
    }
}