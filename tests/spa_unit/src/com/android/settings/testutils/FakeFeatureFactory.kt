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
import com.android.settings.biometrics.face.FaceFeatureProvider
import com.android.settings.biometrics.fingerprint.FingerprintFeatureProvider
import com.android.settings.biometrics2.factory.BiometricsRepositoryProvider
import com.android.settings.bluetooth.BluetoothFeatureProvider
import com.android.settings.connecteddevice.fastpair.FastPairFeatureProvider
import com.android.settings.connecteddevice.stylus.StylusFeatureProvider
import com.android.settings.dashboard.DashboardFeatureProvider
import com.android.settings.dashboard.suggestions.SuggestionFeatureProvider
import com.android.settings.deviceinfo.hardwareinfo.HardwareInfoFeatureProvider
import com.android.settings.enterprise.EnterprisePrivacyFeatureProvider
import com.android.settings.fuelgauge.BatterySettingsFeatureProvider
import com.android.settings.fuelgauge.BatteryStatusFeatureProvider
import com.android.settings.fuelgauge.PowerUsageFeatureProvider
import com.android.settings.homepage.contextualcards.ContextualCardFeatureProvider
import com.android.settings.inputmethod.KeyboardSettingsFeatureProvider
import com.android.settings.localepicker.LocaleFeatureProvider
import com.android.settings.overlay.DockUpdaterFeatureProvider
import com.android.settings.overlay.FeatureFactory
import com.android.settings.overlay.SurveyFeatureProvider
import com.android.settings.panel.PanelFeatureProvider
import com.android.settings.privatespace.PrivateSpaceLoginFeatureProvider
import com.android.settings.search.SearchFeatureProvider
import com.android.settings.security.SecurityFeatureProvider
import com.android.settings.security.SecuritySettingsFeatureProvider
import com.android.settings.slices.SlicesFeatureProvider
import com.android.settings.users.UserFeatureProvider
import com.android.settings.vpn2.AdvancedVpnFeatureProvider
import com.android.settings.wifi.WifiTrackerLibProvider
import com.android.settings.wifi.factory.WifiFeatureProvider
import com.android.settingslib.core.instrumentation.MetricsFeatureProvider
import org.mockito.Mockito.mock

class FakeFeatureFactory : FeatureFactory() {

    private val mockMetricsFeatureProvider: MetricsFeatureProvider =
        mock(MetricsFeatureProvider::class.java)
    val mockApplicationFeatureProvider: ApplicationFeatureProvider =
        mock(ApplicationFeatureProvider::class.java)

    init {
        setFactory(appContext, this)
    }

    override val suggestionFeatureProvider: SuggestionFeatureProvider
        get() = TODO("Not yet implemented")
    override val hardwareInfoFeatureProvider: HardwareInfoFeatureProvider
        get() = TODO("Not yet implemented")

    override val metricsFeatureProvider = mockMetricsFeatureProvider

    override val powerUsageFeatureProvider: PowerUsageFeatureProvider
        get() = TODO("Not yet implemented")

    override val batteryStatusFeatureProvider: BatteryStatusFeatureProvider
        get() = TODO("Not yet implemented")

    override val batterySettingsFeatureProvider: BatterySettingsFeatureProvider
        get() = TODO("Not yet implemented")

    override val dashboardFeatureProvider: DashboardFeatureProvider
        get() = TODO("Not yet implemented")

    override val dockUpdaterFeatureProvider: DockUpdaterFeatureProvider
        get() = TODO("Not yet implemented")

    override val applicationFeatureProvider = mockApplicationFeatureProvider

    override val localeFeatureProvider: LocaleFeatureProvider
        get() = TODO("Not yet implemented")

    override val enterprisePrivacyFeatureProvider: EnterprisePrivacyFeatureProvider
        get() = TODO("Not yet implemented")

    override val searchFeatureProvider: SearchFeatureProvider
        get() = TODO("Not yet implemented")

    override fun getSurveyFeatureProvider(context: Context): SurveyFeatureProvider? {
        TODO("Not yet implemented")
    }

    override val securityFeatureProvider: SecurityFeatureProvider
        get() = TODO("Not yet implemented")

    override val userFeatureProvider: UserFeatureProvider
        get() = TODO("Not yet implemented")

    override val slicesFeatureProvider: SlicesFeatureProvider
        get() = TODO("Not yet implemented")
    override val accountFeatureProvider: AccountFeatureProvider
        get() = TODO("Not yet implemented")
    override val panelFeatureProvider: PanelFeatureProvider
        get() = TODO("Not yet implemented")

    override fun getContextualCardFeatureProvider(context: Context): ContextualCardFeatureProvider {
        TODO("Not yet implemented")
    }

    override val bluetoothFeatureProvider: BluetoothFeatureProvider
        get() = TODO("Not yet implemented")
    override val faceFeatureProvider: FaceFeatureProvider
        get() = TODO("Not yet implemented")
    override val fingerprintFeatureProvider: FingerprintFeatureProvider
        get() = TODO("Not yet implemented")
    override val biometricsRepositoryProvider: BiometricsRepositoryProvider
        get() = TODO("Not yet implemented")
    override val wifiTrackerLibProvider: WifiTrackerLibProvider
        get() = TODO("Not yet implemented")
    override val securitySettingsFeatureProvider: SecuritySettingsFeatureProvider
        get() = TODO("Not yet implemented")
    override val accessibilitySearchFeatureProvider: AccessibilitySearchFeatureProvider
        get() = TODO("Not yet implemented")
    override val accessibilityMetricsFeatureProvider: AccessibilityMetricsFeatureProvider
        get() = TODO("Not yet implemented")
    override val advancedVpnFeatureProvider: AdvancedVpnFeatureProvider
        get() = TODO("Not yet implemented")
    override val wifiFeatureProvider: WifiFeatureProvider
        get() = TODO("Not yet implemented")
    override val keyboardSettingsFeatureProvider: KeyboardSettingsFeatureProvider
        get() = TODO("Not yet implemented")
    override val stylusFeatureProvider: StylusFeatureProvider
        get() = TODO("Not yet implemented")
    override val fastPairFeatureProvider: FastPairFeatureProvider
        get() = TODO("Not yet implemented")
    override val privateSpaceLoginFeatureProvider: PrivateSpaceLoginFeatureProvider
        get() = TODO("Not yet implemented")
}
