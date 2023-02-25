/*
 * Copyright (C) 2022 The Android Open Source Project
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

package com.android.settings.testutils

import android.content.Context
import com.android.settings.accessibility.AccessibilityMetricsFeatureProvider
import com.android.settings.accessibility.AccessibilitySearchFeatureProvider
import com.android.settings.accounts.AccountFeatureProvider
import com.android.settings.applications.ApplicationFeatureProvider
import com.android.settings.aware.AwareFeatureProvider
import com.android.settings.biometrics.face.FaceFeatureProvider
import com.android.settings.biometrics2.factory.BiometricsRepositoryProvider
import com.android.settings.bluetooth.BluetoothFeatureProvider
import com.android.settings.dashboard.DashboardFeatureProvider
import com.android.settings.dashboard.suggestions.SuggestionFeatureProvider
import com.android.settings.enterprise.EnterprisePrivacyFeatureProvider
import com.android.settings.fuelgauge.BatterySettingsFeatureProvider
import com.android.settings.fuelgauge.BatteryStatusFeatureProvider
import com.android.settings.fuelgauge.PowerUsageFeatureProvider
import com.android.settings.gestures.AssistGestureFeatureProvider
import com.android.settings.homepage.contextualcards.ContextualCardFeatureProvider
import com.android.settings.localepicker.LocaleFeatureProvider
import com.android.settings.overlay.DockUpdaterFeatureProvider
import com.android.settings.overlay.FeatureFactory
import com.android.settings.overlay.SupportFeatureProvider
import com.android.settings.overlay.SurveyFeatureProvider
import com.android.settings.panel.PanelFeatureProvider
import com.android.settings.search.SearchFeatureProvider
import com.android.settings.security.SecurityFeatureProvider
import com.android.settings.security.SecuritySettingsFeatureProvider
import com.android.settings.slices.SlicesFeatureProvider
import com.android.settings.users.UserFeatureProvider
import com.android.settings.vpn2.AdvancedVpnFeatureProvider
import com.android.settings.wifi.WifiTrackerLibProvider
import com.android.settings.wifi.factory.WifiFeatureProvider;
import com.android.settingslib.core.instrumentation.MetricsFeatureProvider
import org.mockito.Mockito.mock

class FakeFeatureFactory : FeatureFactory() {

    private val mockMetricsFeatureProvider: MetricsFeatureProvider =
        mock(MetricsFeatureProvider::class.java)
    val mockApplicationFeatureProvider: ApplicationFeatureProvider =
        mock(ApplicationFeatureProvider::class.java)

    init {
        sFactory = this
    }

    override fun getAssistGestureFeatureProvider(): AssistGestureFeatureProvider {
        TODO("Not yet implemented")
    }

    override fun getSuggestionFeatureProvider(): SuggestionFeatureProvider {
        TODO("Not yet implemented")
    }

    override fun getSupportFeatureProvider(context: Context?): SupportFeatureProvider {
        TODO("Not yet implemented")
    }

    override fun getMetricsFeatureProvider(): MetricsFeatureProvider = mockMetricsFeatureProvider

    override fun getPowerUsageFeatureProvider(context: Context?): PowerUsageFeatureProvider {
        TODO("Not yet implemented")
    }

    override fun getBatteryStatusFeatureProvider(context: Context?): BatteryStatusFeatureProvider {
        TODO("Not yet implemented")
    }

    override fun getBatterySettingsFeatureProvider(
        context: Context?,
    ): BatterySettingsFeatureProvider {
        TODO("Not yet implemented")
    }

    override fun getDashboardFeatureProvider(context: Context?): DashboardFeatureProvider {
        TODO("Not yet implemented")
    }

    override fun getDockUpdaterFeatureProvider(): DockUpdaterFeatureProvider {
        TODO("Not yet implemented")
    }

    override fun getApplicationFeatureProvider(context: Context?) = mockApplicationFeatureProvider

    override fun getLocaleFeatureProvider(): LocaleFeatureProvider {
        TODO("Not yet implemented")
    }

    override fun getEnterprisePrivacyFeatureProvider(
        context: Context?,
    ): EnterprisePrivacyFeatureProvider {
        TODO("Not yet implemented")
    }

    override fun getSearchFeatureProvider(): SearchFeatureProvider {
        TODO("Not yet implemented")
    }

    override fun getSurveyFeatureProvider(context: Context?): SurveyFeatureProvider {
        TODO("Not yet implemented")
    }

    override fun getSecurityFeatureProvider(): SecurityFeatureProvider {
        TODO("Not yet implemented")
    }

    override fun getUserFeatureProvider(context: Context?): UserFeatureProvider {
        TODO("Not yet implemented")
    }

    override fun getSlicesFeatureProvider(): SlicesFeatureProvider {
        TODO("Not yet implemented")
    }

    override fun getAccountFeatureProvider(): AccountFeatureProvider {
        TODO("Not yet implemented")
    }

    override fun getPanelFeatureProvider(): PanelFeatureProvider {
        TODO("Not yet implemented")
    }

    override fun getContextualCardFeatureProvider(
        context: Context?,
    ): ContextualCardFeatureProvider {
        TODO("Not yet implemented")
    }

    override fun getBluetoothFeatureProvider(): BluetoothFeatureProvider {
        TODO("Not yet implemented")
    }

    override fun getAwareFeatureProvider(): AwareFeatureProvider {
        TODO("Not yet implemented")
    }

    override fun getFaceFeatureProvider(): FaceFeatureProvider {
        TODO("Not yet implemented")
    }

    override fun getBiometricsRepositoryProvider(): BiometricsRepositoryProvider {
        TODO("Not yet implemented")
    }

    override fun getWifiTrackerLibProvider(): WifiTrackerLibProvider {
        TODO("Not yet implemented")
    }

    override fun getSecuritySettingsFeatureProvider(): SecuritySettingsFeatureProvider {
        TODO("Not yet implemented")
    }

    override fun getAccessibilitySearchFeatureProvider(): AccessibilitySearchFeatureProvider {
        TODO("Not yet implemented")
    }

    override fun getAccessibilityMetricsFeatureProvider(): AccessibilityMetricsFeatureProvider {
        TODO("Not yet implemented")
    }

    override fun getAdvancedVpnFeatureProvider(): AdvancedVpnFeatureProvider {
        TODO("Not yet implemented")
    }

    override fun getWifiFeatureProvider(): WifiFeatureProvider {
        TODO("Not yet implemented")
    }
}
