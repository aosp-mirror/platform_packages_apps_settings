/*
 * Copyright (C) 2020 The Android Open Source Project
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
package com.android.settings.testutils;

import static org.mockito.Mockito.mock;

import android.content.Context;

import com.android.settings.accessibility.AccessibilityMetricsFeatureProvider;
import com.android.settings.accessibility.AccessibilitySearchFeatureProvider;
import com.android.settings.accounts.AccountFeatureProvider;
import com.android.settings.applications.ApplicationFeatureProvider;
import com.android.settings.biometrics.face.FaceFeatureProvider;
import com.android.settings.biometrics.fingerprint.FingerprintFeatureProvider;
import com.android.settings.biometrics2.factory.BiometricsRepositoryProvider;
import com.android.settings.bluetooth.BluetoothFeatureProvider;
import com.android.settings.connecteddevice.fastpair.FastPairFeatureProvider;
import com.android.settings.connecteddevice.stylus.StylusFeatureProvider;
import com.android.settings.dashboard.DashboardFeatureProvider;
import com.android.settings.dashboard.suggestions.SuggestionFeatureProvider;
import com.android.settings.deviceinfo.hardwareinfo.HardwareInfoFeatureProvider;
import com.android.settings.deviceinfo.hardwareinfo.HardwareInfoFeatureProviderImpl;
import com.android.settings.enterprise.EnterprisePrivacyFeatureProvider;
import com.android.settings.fuelgauge.BatterySettingsFeatureProvider;
import com.android.settings.fuelgauge.BatteryStatusFeatureProvider;
import com.android.settings.fuelgauge.PowerUsageFeatureProvider;
import com.android.settings.homepage.contextualcards.ContextualCardFeatureProvider;
import com.android.settings.inputmethod.KeyboardSettingsFeatureProvider;
import com.android.settings.localepicker.LocaleFeatureProvider;
import com.android.settings.onboarding.OnboardingFeatureProvider;
import com.android.settings.overlay.DockUpdaterFeatureProvider;
import com.android.settings.overlay.FeatureFactory;
import com.android.settings.overlay.SupportFeatureProvider;
import com.android.settings.overlay.SurveyFeatureProvider;
import com.android.settings.panel.PanelFeatureProvider;
import com.android.settings.privatespace.PrivateSpaceLoginFeatureProvider;
import com.android.settings.search.SearchFeatureProvider;
import com.android.settings.security.SecurityFeatureProvider;
import com.android.settings.security.SecuritySettingsFeatureProvider;
import com.android.settings.slices.SlicesFeatureProvider;
import com.android.settings.users.UserFeatureProvider;
import com.android.settings.vpn2.AdvancedVpnFeatureProvider;
import com.android.settings.wifi.WifiTrackerLibProvider;
import com.android.settings.wifi.factory.WifiFeatureProvider;
import com.android.settingslib.core.instrumentation.MetricsFeatureProvider;

import org.jetbrains.annotations.NotNull;

/**
 * Test util to provide fake FeatureFactory. To use this factory, call {@code setupForTest} in
 * {@code @Before} method of the test class.
 */
public class FakeFeatureFactory extends FeatureFactory {
    public final SupportFeatureProvider supportFeatureProvider;
    public final MetricsFeatureProvider metricsFeatureProvider;
    public final BatteryStatusFeatureProvider batteryStatusFeatureProvider;
    public final BatterySettingsFeatureProvider batterySettingsFeatureProvider;
    public final PowerUsageFeatureProvider powerUsageFeatureProvider;
    public final DashboardFeatureProvider dashboardFeatureProvider;
    public final DockUpdaterFeatureProvider dockUpdaterFeatureProvider;
    public final LocaleFeatureProvider localeFeatureProvider;
    public final ApplicationFeatureProvider applicationFeatureProvider;
    public final EnterprisePrivacyFeatureProvider enterprisePrivacyFeatureProvider;
    public final SurveyFeatureProvider surveyFeatureProvider;
    public final SecurityFeatureProvider securityFeatureProvider;
    public final SuggestionFeatureProvider suggestionsFeatureProvider;
    public final UserFeatureProvider userFeatureProvider;
    public final AccountFeatureProvider mAccountFeatureProvider;
    public final BluetoothFeatureProvider mBluetoothFeatureProvider;
    public final FaceFeatureProvider mFaceFeatureProvider;
    public final FingerprintFeatureProvider mFingerprintFeatureProvider;
    public final BiometricsRepositoryProvider mBiometricsRepositoryProvider;

    public PanelFeatureProvider panelFeatureProvider;
    public SlicesFeatureProvider slicesFeatureProvider;
    public SearchFeatureProvider searchFeatureProvider;
    public ContextualCardFeatureProvider mContextualCardFeatureProvider;

    public WifiTrackerLibProvider wifiTrackerLibProvider;
    public SecuritySettingsFeatureProvider securitySettingsFeatureProvider;
    public AccessibilitySearchFeatureProvider mAccessibilitySearchFeatureProvider;
    public AccessibilityMetricsFeatureProvider mAccessibilityMetricsFeatureProvider;
    public AdvancedVpnFeatureProvider mAdvancedVpnFeatureProvider;
    public WifiFeatureProvider mWifiFeatureProvider;
    public KeyboardSettingsFeatureProvider mKeyboardSettingsFeatureProvider;
    public StylusFeatureProvider mStylusFeatureProvider;
    public OnboardingFeatureProvider mOnboardingFeatureProvider;
    public FastPairFeatureProvider mFastPairFeatureProvider;
    public PrivateSpaceLoginFeatureProvider mPrivateSpaceLoginFeatureProvider;

    /**
     * Call this in {@code @Before} method of the test class to use fake factory.
     */
    public static FakeFeatureFactory setupForTest() {
        FakeFeatureFactory factory = new FakeFeatureFactory();
        setFactory(getAppContext(), factory);
        return factory;
    }

    /**
     * FeatureFactory constructor.
     */
    public FakeFeatureFactory() {
        supportFeatureProvider = mock(SupportFeatureProvider.class);
        metricsFeatureProvider = mock(MetricsFeatureProvider.class);
        batteryStatusFeatureProvider = mock(BatteryStatusFeatureProvider.class);
        batterySettingsFeatureProvider = mock(BatterySettingsFeatureProvider.class);
        powerUsageFeatureProvider = mock(PowerUsageFeatureProvider.class);
        dashboardFeatureProvider = mock(DashboardFeatureProvider.class);
        dockUpdaterFeatureProvider = mock(DockUpdaterFeatureProvider.class);
        localeFeatureProvider = mock(LocaleFeatureProvider.class);
        applicationFeatureProvider = mock(ApplicationFeatureProvider.class);
        enterprisePrivacyFeatureProvider = mock(EnterprisePrivacyFeatureProvider.class);
        searchFeatureProvider = mock(SearchFeatureProvider.class);
        surveyFeatureProvider = mock(SurveyFeatureProvider.class);
        securityFeatureProvider = mock(SecurityFeatureProvider.class);
        suggestionsFeatureProvider = mock(SuggestionFeatureProvider.class);
        userFeatureProvider = mock(UserFeatureProvider.class);
        slicesFeatureProvider = mock(SlicesFeatureProvider.class);
        mAccountFeatureProvider = mock(AccountFeatureProvider.class);
        mContextualCardFeatureProvider = mock(ContextualCardFeatureProvider.class);
        panelFeatureProvider = mock(PanelFeatureProvider.class);
        mBluetoothFeatureProvider = mock(BluetoothFeatureProvider.class);
        mFaceFeatureProvider = mock(FaceFeatureProvider.class);
        mFingerprintFeatureProvider = mock(FingerprintFeatureProvider.class);
        mBiometricsRepositoryProvider = mock(BiometricsRepositoryProvider.class);
        wifiTrackerLibProvider = mock(WifiTrackerLibProvider.class);
        securitySettingsFeatureProvider = mock(SecuritySettingsFeatureProvider.class);
        mAccessibilitySearchFeatureProvider = mock(AccessibilitySearchFeatureProvider.class);
        mAccessibilityMetricsFeatureProvider = mock(AccessibilityMetricsFeatureProvider.class);
        mAdvancedVpnFeatureProvider = mock(AdvancedVpnFeatureProvider.class);
        mWifiFeatureProvider = mock(WifiFeatureProvider.class);
        mKeyboardSettingsFeatureProvider = mock(KeyboardSettingsFeatureProvider.class);
        mStylusFeatureProvider = mock(StylusFeatureProvider.class);
        mOnboardingFeatureProvider = mock(OnboardingFeatureProvider.class);
        mFastPairFeatureProvider = mock(FastPairFeatureProvider.class);
        mPrivateSpaceLoginFeatureProvider = mock(PrivateSpaceLoginFeatureProvider.class);
    }

    @Override
    public SuggestionFeatureProvider getSuggestionFeatureProvider() {
        return suggestionsFeatureProvider;
    }

    @Override
    public SupportFeatureProvider getSupportFeatureProvider() {
        return supportFeatureProvider;
    }

    @Override
    public MetricsFeatureProvider getMetricsFeatureProvider() {
        return metricsFeatureProvider;
    }

    @NotNull
    @Override
    public BatteryStatusFeatureProvider getBatteryStatusFeatureProvider() {
        return batteryStatusFeatureProvider;
    }

    @Override
    public BatterySettingsFeatureProvider getBatterySettingsFeatureProvider() {
        return batterySettingsFeatureProvider;
    }

    @NotNull
    @Override
    public PowerUsageFeatureProvider getPowerUsageFeatureProvider() {
        return powerUsageFeatureProvider;
    }

    @NotNull
    @Override
    public DashboardFeatureProvider getDashboardFeatureProvider() {
        return dashboardFeatureProvider;
    }

    @Override
    public DockUpdaterFeatureProvider getDockUpdaterFeatureProvider() {
        return dockUpdaterFeatureProvider;
    }

    @NotNull
    @Override
    public ApplicationFeatureProvider getApplicationFeatureProvider() {
        return applicationFeatureProvider;
    }

    @Override
    public LocaleFeatureProvider getLocaleFeatureProvider() {
        return localeFeatureProvider;
    }

    @NotNull
    @Override
    public EnterprisePrivacyFeatureProvider getEnterprisePrivacyFeatureProvider() {
        return enterprisePrivacyFeatureProvider;
    }

    @Override
    public SearchFeatureProvider getSearchFeatureProvider() {
        return searchFeatureProvider;
    }

    @Override
    public SurveyFeatureProvider getSurveyFeatureProvider(Context context) {
        return surveyFeatureProvider;
    }

    @Override
    public SecurityFeatureProvider getSecurityFeatureProvider() {
        return securityFeatureProvider;
    }

    @NotNull
    @Override
    public UserFeatureProvider getUserFeatureProvider() {
        return userFeatureProvider;
    }

    @Override
    public SlicesFeatureProvider getSlicesFeatureProvider() {
        return slicesFeatureProvider;
    }

    @Override
    public AccountFeatureProvider getAccountFeatureProvider() {
        return mAccountFeatureProvider;
    }

    @Override
    public PanelFeatureProvider getPanelFeatureProvider() {
        return panelFeatureProvider;
    }

    @Override
    public ContextualCardFeatureProvider getContextualCardFeatureProvider(Context context) {
        return mContextualCardFeatureProvider;
    }

    @Override
    public BluetoothFeatureProvider getBluetoothFeatureProvider() {
        return mBluetoothFeatureProvider;
    }

    @Override
    public FaceFeatureProvider getFaceFeatureProvider() {
        return mFaceFeatureProvider;
    }

    @Override
    public FingerprintFeatureProvider getFingerprintFeatureProvider() {
        return mFingerprintFeatureProvider;
    }

    @Override
    public BiometricsRepositoryProvider getBiometricsRepositoryProvider() {
        return mBiometricsRepositoryProvider;
    }

    @Override
    public WifiTrackerLibProvider getWifiTrackerLibProvider() {
        return wifiTrackerLibProvider;
    }

    @Override
    public SecuritySettingsFeatureProvider getSecuritySettingsFeatureProvider() {
        return securitySettingsFeatureProvider;
    }

    @Override
    public AccessibilitySearchFeatureProvider getAccessibilitySearchFeatureProvider() {
        return mAccessibilitySearchFeatureProvider;
    }

    @Override
    public AccessibilityMetricsFeatureProvider getAccessibilityMetricsFeatureProvider() {
        return mAccessibilityMetricsFeatureProvider;
    }

    @Override
    public HardwareInfoFeatureProvider getHardwareInfoFeatureProvider() {
        return HardwareInfoFeatureProviderImpl.INSTANCE;
    }

    @Override
    public AdvancedVpnFeatureProvider getAdvancedVpnFeatureProvider() {
        return mAdvancedVpnFeatureProvider;
    }

    @Override
    public WifiFeatureProvider getWifiFeatureProvider() {
        return mWifiFeatureProvider;
    }

    @Override
    public KeyboardSettingsFeatureProvider getKeyboardSettingsFeatureProvider() {
        return mKeyboardSettingsFeatureProvider;
    }

    @Override
    public StylusFeatureProvider getStylusFeatureProvider() {
        return mStylusFeatureProvider;
    }

    @Override
    public OnboardingFeatureProvider getOnboardingFeatureProvider() {
        return mOnboardingFeatureProvider;
    }

    @Override
    public FastPairFeatureProvider getFastPairFeatureProvider() {
        return mFastPairFeatureProvider;
    }

    @Override
    public PrivateSpaceLoginFeatureProvider getPrivateSpaceLoginFeatureProvider() {
        return mPrivateSpaceLoginFeatureProvider;
    }
}
