/*
 * Copyright (C) 2016 The Android Open Source Project
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
package com.android.settings.overlay

import android.app.AppGlobals
import android.content.Context
import android.net.ConnectivityManager
import android.net.VpnManager
import android.os.UserManager
import com.android.settings.accessibility.AccessibilityMetricsFeatureProvider
import com.android.settings.accessibility.AccessibilityMetricsFeatureProviderImpl
import com.android.settings.accessibility.AccessibilitySearchFeatureProvider
import com.android.settings.accessibility.AccessibilitySearchFeatureProviderImpl
import com.android.settings.accounts.AccountFeatureProvider
import com.android.settings.accounts.AccountFeatureProviderImpl
import com.android.settings.applications.ApplicationFeatureProviderImpl
import com.android.settings.biometrics.face.FaceFeatureProvider
import com.android.settings.biometrics.face.FaceFeatureProviderImpl
import com.android.settings.biometrics.fingerprint.FingerprintFeatureProvider
import com.android.settings.biometrics.fingerprint.FingerprintFeatureProviderImpl
import com.android.settings.biometrics2.factory.BiometricsRepositoryProviderImpl
import com.android.settings.bluetooth.BluetoothFeatureProvider
import com.android.settings.bluetooth.BluetoothFeatureProviderImpl
import com.android.settings.connecteddevice.dock.DockUpdaterFeatureProviderImpl
import com.android.settings.connecteddevice.fastpair.FastPairFeatureProvider
import com.android.settings.connecteddevice.fastpair.FastPairFeatureProviderImpl
import com.android.settings.connecteddevice.stylus.StylusFeatureProvider
import com.android.settings.connecteddevice.stylus.StylusFeatureProviderImpl
import com.android.settings.core.instrumentation.SettingsMetricsFeatureProvider
import com.android.settings.dashboard.DashboardFeatureProviderImpl
import com.android.settings.dashboard.suggestions.SuggestionFeatureProvider
import com.android.settings.dashboard.suggestions.SuggestionFeatureProviderImpl
import com.android.settings.deviceinfo.hardwareinfo.HardwareInfoFeatureProvider
import com.android.settings.deviceinfo.hardwareinfo.HardwareInfoFeatureProviderImpl
import com.android.settings.enterprise.EnterprisePrivacyFeatureProviderImpl
import com.android.settings.fuelgauge.BatterySettingsFeatureProviderImpl
import com.android.settings.fuelgauge.BatteryStatusFeatureProviderImpl
import com.android.settings.fuelgauge.PowerUsageFeatureProviderImpl
import com.android.settings.homepage.contextualcards.ContextualCardFeatureProviderImpl
import com.android.settings.inputmethod.KeyboardSettingsFeatureProvider
import com.android.settings.inputmethod.KeyboardSettingsFeatureProviderImpl
import com.android.settings.localepicker.LocaleFeatureProviderImpl
import com.android.settings.panel.PanelFeatureProviderImpl
import com.android.settings.search.SearchFeatureProvider
import com.android.settings.search.SearchFeatureProviderImpl
import com.android.settings.security.SecurityFeatureProviderImpl
import com.android.settings.security.SecuritySettingsFeatureProvider
import com.android.settings.security.SecuritySettingsFeatureProviderImpl
import com.android.settings.privatespace.PrivateSpaceLoginFeatureProvider
import com.android.settings.privatespace.PrivateSpaceLoginFeatureProviderImpl
import com.android.settings.slices.SlicesFeatureProviderImpl
import com.android.settings.users.UserFeatureProviderImpl
import com.android.settings.vpn2.AdvancedVpnFeatureProviderImpl
import com.android.settings.wifi.WifiTrackerLibProvider
import com.android.settings.wifi.WifiTrackerLibProviderImpl
import com.android.settings.wifi.factory.WifiFeatureProvider
import com.android.settingslib.spaprivileged.framework.common.devicePolicyManager

/**
 * [FeatureFactory] implementation for AOSP Settings.
 */
open class FeatureFactoryImpl : FeatureFactory() {
    private val contextualCardFeatureProvider by lazy {
        ContextualCardFeatureProviderImpl(appContext)
    }

    override val hardwareInfoFeatureProvider: HardwareInfoFeatureProvider =
        HardwareInfoFeatureProviderImpl

    override val metricsFeatureProvider by lazy { SettingsMetricsFeatureProvider() }

    override val powerUsageFeatureProvider by lazy { PowerUsageFeatureProviderImpl(appContext) }

    override val batteryStatusFeatureProvider by lazy {
        BatteryStatusFeatureProviderImpl(appContext)
    }

    override val batterySettingsFeatureProvider by lazy { BatterySettingsFeatureProviderImpl() }

    override val dashboardFeatureProvider by lazy { DashboardFeatureProviderImpl(appContext) }

    override val dockUpdaterFeatureProvider: DockUpdaterFeatureProvider by lazy {
        DockUpdaterFeatureProviderImpl()
    }

    override val applicationFeatureProvider by lazy {
        ApplicationFeatureProviderImpl(
            appContext,
            appContext.packageManager,
            AppGlobals.getPackageManager(),
            appContext.devicePolicyManager,
        )
    }

    override val localeFeatureProvider by lazy { LocaleFeatureProviderImpl() }

    override val enterprisePrivacyFeatureProvider by lazy {
        EnterprisePrivacyFeatureProviderImpl(
            appContext,
            appContext.devicePolicyManager,
            appContext.packageManager,
            UserManager.get(appContext),
            appContext.getSystemService(ConnectivityManager::class.java),
            appContext.getSystemService(VpnManager::class.java),
            appContext.resources,
        )
    }

    override val searchFeatureProvider: SearchFeatureProvider by lazy {
        SearchFeatureProviderImpl()
    }

    override fun getSurveyFeatureProvider(context: Context): SurveyFeatureProvider? = null

    override val securityFeatureProvider by lazy { SecurityFeatureProviderImpl() }

    override val suggestionFeatureProvider: SuggestionFeatureProvider by lazy {
        SuggestionFeatureProviderImpl()
    }

    override val userFeatureProvider by lazy { UserFeatureProviderImpl(appContext) }

    override val slicesFeatureProvider by lazy { SlicesFeatureProviderImpl() }

    override val accountFeatureProvider: AccountFeatureProvider by lazy {
        AccountFeatureProviderImpl()
    }

    override val panelFeatureProvider by lazy { PanelFeatureProviderImpl() }

    override fun getContextualCardFeatureProvider(context: Context) = contextualCardFeatureProvider

    override val bluetoothFeatureProvider: BluetoothFeatureProvider by lazy {
        BluetoothFeatureProviderImpl()
    }

    override val faceFeatureProvider: FaceFeatureProvider by lazy { FaceFeatureProviderImpl() }

    override val fingerprintFeatureProvider: FingerprintFeatureProvider by lazy {
        FingerprintFeatureProviderImpl()
    }

    override val biometricsRepositoryProvider by lazy { BiometricsRepositoryProviderImpl() }

    override val wifiTrackerLibProvider: WifiTrackerLibProvider by lazy {
        WifiTrackerLibProviderImpl()
    }

    override val securitySettingsFeatureProvider: SecuritySettingsFeatureProvider by lazy {
        SecuritySettingsFeatureProviderImpl()
    }

    override val accessibilitySearchFeatureProvider: AccessibilitySearchFeatureProvider by lazy {
        AccessibilitySearchFeatureProviderImpl()
    }

    override val accessibilityMetricsFeatureProvider: AccessibilityMetricsFeatureProvider by lazy {
        AccessibilityMetricsFeatureProviderImpl()
    }

    override val advancedVpnFeatureProvider by lazy { AdvancedVpnFeatureProviderImpl() }

    override val wifiFeatureProvider by lazy { WifiFeatureProvider(appContext) }

    override val keyboardSettingsFeatureProvider: KeyboardSettingsFeatureProvider by lazy {
        KeyboardSettingsFeatureProviderImpl()
    }

    override val stylusFeatureProvider: StylusFeatureProvider by lazy {
        StylusFeatureProviderImpl()
    }

    override val fastPairFeatureProvider: FastPairFeatureProvider by lazy {
        FastPairFeatureProviderImpl()
    }

    override val privateSpaceLoginFeatureProvider: PrivateSpaceLoginFeatureProvider by lazy {
        PrivateSpaceLoginFeatureProviderImpl()
    }
}
