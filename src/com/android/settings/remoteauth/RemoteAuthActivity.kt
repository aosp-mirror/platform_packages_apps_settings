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

package com.android.settings.remoteauth

import android.os.Bundle

import androidx.annotation.IdRes
import androidx.navigation.fragment.NavHostFragment

import com.android.settings.R
import com.android.settings.SetupWizardUtils
import com.android.settings.core.InstrumentedActivity
import com.google.android.setupdesign.util.ThemeHelper

open class RemoteAuthActivity : InstrumentedActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setTheme(SetupWizardUtils.getTheme(this, getIntent()))
        ThemeHelper.trySetDynamicColor(this)
        setContentView(R.layout.remote_auth_root)
        // TODO(b/290768873): Change to remote_auth_enroll_introduction_fragment if no device is
        // enrolled.
        initializeNavigation(R.id.remote_auth_settings_fragment)
    }

    override fun getMetricsCategory(): Int {
        // TODO() Update frameworks/proto_logging/stats/enums/app/settings_enums.proto
        return 0
    }

    private fun initializeNavigation(@IdRes startDestinationId: Int) {
        val navHostFragment =
            supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        val navController = navHostFragment.navController
        val navGraph = navController.navInflater.inflate(R.navigation.remote_auth_navigation)
        navGraph.setStartDestination(startDestinationId)
        navController.graph = navGraph
    }
}