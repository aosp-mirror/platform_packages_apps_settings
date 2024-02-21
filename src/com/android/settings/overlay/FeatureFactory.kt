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

import android.content.Context
import com.android.settings.accessibility.AccessibilityMetricsFeatureProvider
import com.android.settings.accessibility.AccessibilitySearchFeatureProvider
import com.android.settings.accounts.AccountFeatureProvider
import com.android.settings.applications.ApplicationFeatureProvider
import com.android.settings.biometrics.face.FaceFeatureProvider
import com.android.settings.biometrics.fingerprint.FingerprintFeatureProvider
import com.android.settings.biometrics2.factory.BiometricsRepositoryProvider
import com.android.settings.bluetooth.BluetoothFeatureProvider
import com.android.settings.connecteddevice.audiosharing.AudioSharingFeatureProvider
import com.android.settings.connecteddevice.fastpair.FastPairFeatureProvider
import com.android.settings.connecteddevice.stylus.StylusFeatureProvider
import com.android.settings.dashboard.DashboardFeatureProvider
import com.android.settings.dashboard.suggestions.SuggestionFeatureProvider
import com.android.settings.deviceinfo.hardwareinfo.HardwareInfoFeatureProvider
import com.android.settings.display.DisplayFeatureProvider
import com.android.settings.enterprise.EnterprisePrivacyFeatureProvider
import com.android.settings.fuelgauge.BatterySettingsFeatureProvider
import com.android.settings.fuelgauge.BatteryStatusFeatureProvider
import com.android.settings.fuelgauge.PowerUsageFeatureProvider
import com.android.settings.homepage.contextualcards.ContextualCardFeatureProvider
import com.android.settings.inputmethod.KeyboardSettingsFeatureProvider
import com.android.settings.localepicker.LocaleFeatureProvider
import com.android.settings.onboarding.OnboardingFeatureProvider
import com.android.settings.overlay.FeatureFactory.Companion.setFactory
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

/**
 * Abstract class for creating feature controllers.
 *
 * Allows OEM implementations to define their own factories with their own controllers containing
 * whatever code is needed to implement the features.
 * To provide a factory implementation, implementors should call [setFactory] in their Application.
 */
abstract class FeatureFactory {
    /**
     * Gets implementation for the Suggestion Feature provider.
     */
    abstract val suggestionFeatureProvider: SuggestionFeatureProvider

    /**
     * Retrieves implementation for Hardware Info feature.
     */
    abstract val hardwareInfoFeatureProvider: HardwareInfoFeatureProvider

    /** Implementation for [SupportFeatureProvider]. */
    open val supportFeatureProvider: SupportFeatureProvider? = null

    abstract val metricsFeatureProvider: MetricsFeatureProvider

    abstract val powerUsageFeatureProvider: PowerUsageFeatureProvider

    /**
     * Retrieves implementation for Battery Status feature.
     */
    abstract val batteryStatusFeatureProvider: BatteryStatusFeatureProvider

    /**
     * Gets implementation for Battery Settings provider.
     */
    abstract val batterySettingsFeatureProvider: BatterySettingsFeatureProvider

    abstract val dashboardFeatureProvider: DashboardFeatureProvider
    abstract val dockUpdaterFeatureProvider: DockUpdaterFeatureProvider
    abstract val applicationFeatureProvider: ApplicationFeatureProvider
    abstract val localeFeatureProvider: LocaleFeatureProvider

    abstract val enterprisePrivacyFeatureProvider: EnterprisePrivacyFeatureProvider

    abstract val searchFeatureProvider: SearchFeatureProvider
    abstract fun getSurveyFeatureProvider(context: Context): SurveyFeatureProvider?
    abstract val securityFeatureProvider: SecurityFeatureProvider
    abstract val userFeatureProvider: UserFeatureProvider
    abstract val slicesFeatureProvider: SlicesFeatureProvider
    abstract val accountFeatureProvider: AccountFeatureProvider
    abstract val panelFeatureProvider: PanelFeatureProvider
    abstract fun getContextualCardFeatureProvider(context: Context): ContextualCardFeatureProvider

    /**
     * Retrieves implementation for Bluetooth feature.
     */
    abstract val bluetoothFeatureProvider: BluetoothFeatureProvider

    /**
     * Retrieves implementation for Face feature.
     */
    abstract val faceFeatureProvider: FaceFeatureProvider

    /**
     * Retrieves implementation for Fingerprint feature.
     */
    abstract val fingerprintFeatureProvider: FingerprintFeatureProvider

    /**
     * Gets implementation for Biometrics repository provider.
     */
    abstract val biometricsRepositoryProvider: BiometricsRepositoryProvider

    /**
     * Gets implementation for the WifiTrackerLib.
     */
    abstract val wifiTrackerLibProvider: WifiTrackerLibProvider

    /**
     * Retrieves implementation for SecuritySettings feature.
     */
    abstract val securitySettingsFeatureProvider: SecuritySettingsFeatureProvider

    /**
     * Retrieves implementation for Accessibility search index feature.
     */
    abstract val accessibilitySearchFeatureProvider: AccessibilitySearchFeatureProvider

    /**
     * Retrieves implementation for Accessibility metrics category feature.
     */
    abstract val accessibilityMetricsFeatureProvider: AccessibilityMetricsFeatureProvider

    /**
     * Retrieves implementation for advanced vpn feature.
     */
    abstract val advancedVpnFeatureProvider: AdvancedVpnFeatureProvider

    /**
     * Retrieves implementation for Wi-Fi feature.
     */
    abstract val wifiFeatureProvider: WifiFeatureProvider

    /**
     * Retrieves implementation for keyboard settings feature.
     */
    abstract val keyboardSettingsFeatureProvider: KeyboardSettingsFeatureProvider

    /**
     * Retrieves implementation for stylus feature.
     */
    abstract val stylusFeatureProvider: StylusFeatureProvider

    /**
     * Retrieves implementation for Onboarding related feature.
     */
    open val onboardingFeatureProvider: OnboardingFeatureProvider? = null

    /**
     * Gets implementation for Fast Pair device updater provider.
     */
    abstract val fastPairFeatureProvider: FastPairFeatureProvider

    /**
     * Gets implementation for Private Space account login feature.
     */
    abstract val privateSpaceLoginFeatureProvider: PrivateSpaceLoginFeatureProvider

    /**
     * Gets implementation for Display feature.
     */
    abstract val displayFeatureProvider: DisplayFeatureProvider

    /**
     * Gets implementation for audio sharing related feature.
     */
    abstract val audioSharingFeatureProvider: AudioSharingFeatureProvider

    companion object {
        private var _factory: FeatureFactory? = null

        /** Returns a factory for creating feature controllers. */
        @JvmStatic
        val featureFactory: FeatureFactory
            get() = _factory ?: throw UnsupportedOperationException("No feature factory configured")

        private var _appContext: Context? = null

        /** Returns an application [Context] used to create this [FeatureFactory]. */
        @JvmStatic
        val appContext: Context
            get() = _appContext
                ?: throw UnsupportedOperationException("No feature factory configured")

        @JvmStatic
        fun setFactory(appContext: Context, factory: FeatureFactory) {
            _appContext = appContext
            _factory = factory
        }
    }
}
