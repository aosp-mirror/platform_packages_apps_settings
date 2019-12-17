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
 * limitations under the License
 */
package com.android.settings.testutils;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import android.content.Context;

import com.android.settings.accounts.AccountFeatureProvider;
import com.android.settings.applications.ApplicationFeatureProvider;
import com.android.settings.aware.AwareFeatureProvider;
import com.android.settings.biometrics.face.FaceFeatureProvider;
import com.android.settings.bluetooth.BluetoothFeatureProvider;
import com.android.settings.dashboard.DashboardFeatureProvider;
import com.android.settings.dashboard.suggestions.SuggestionFeatureProvider;
import com.android.settings.enterprise.EnterprisePrivacyFeatureProvider;
import com.android.settings.fuelgauge.PowerUsageFeatureProvider;
import com.android.settings.gestures.AssistGestureFeatureProvider;
import com.android.settings.homepage.contextualcards.ContextualCardFeatureProvider;
import com.android.settings.localepicker.LocaleFeatureProvider;
import com.android.settings.overlay.DockUpdaterFeatureProvider;
import com.android.settings.overlay.FeatureFactory;
import com.android.settings.overlay.SupportFeatureProvider;
import com.android.settings.overlay.SurveyFeatureProvider;
import com.android.settings.panel.PanelFeatureProvider;
import com.android.settings.search.SearchFeatureProvider;
import com.android.settings.security.SecurityFeatureProvider;
import com.android.settings.slices.SlicesFeatureProvider;
import com.android.settings.users.UserFeatureProvider;
import com.android.settingslib.core.instrumentation.MetricsFeatureProvider;

import org.mockito.Answers;

/**
 * Test util to provide fake FeatureFactory. To use this factory, call {@code setupForTest} in
 * {@code @Before} method of the test class.
 */
public class FakeFeatureFactory extends FeatureFactory {

    public final SupportFeatureProvider supportFeatureProvider;
    public final MetricsFeatureProvider metricsFeatureProvider;
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
    public final AssistGestureFeatureProvider assistGestureFeatureProvider;
    public final AccountFeatureProvider mAccountFeatureProvider;
    public final ContextualCardFeatureProvider mContextualCardFeatureProvider;
    public final BluetoothFeatureProvider mBluetoothFeatureProvider;
    public final AwareFeatureProvider mAwareFeatureProvider;
    public final FaceFeatureProvider mFaceFeatureProvider;

    public PanelFeatureProvider panelFeatureProvider;
    public SlicesFeatureProvider slicesFeatureProvider;
    public SearchFeatureProvider searchFeatureProvider;

    /**
     * Call this in {@code @Before} method of the test class to use fake factory.
     */
    public static FakeFeatureFactory setupForTest() {
        final Context context = mock(Context.class, Answers.RETURNS_DEEP_STUBS);
        sFactory = null;
        when(context.getString(com.android.settings.R.string.config_featureFactory))
                .thenReturn(FakeFeatureFactory.class.getName());
        try {
            Class c = FakeFeatureFactory.class;
            when(context.getClassLoader().loadClass(anyString())).thenReturn(c);
        } catch (ClassNotFoundException e) {
            // Ignore.
        }
        return (FakeFeatureFactory) FakeFeatureFactory.getFactory(context);
    }

    /**
     * Used by reflection. Do not call directly.
     */
    public FakeFeatureFactory() {
        supportFeatureProvider = mock(SupportFeatureProvider.class);
        metricsFeatureProvider = mock(MetricsFeatureProvider.class);
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
        assistGestureFeatureProvider = mock(AssistGestureFeatureProvider.class);
        slicesFeatureProvider = mock(SlicesFeatureProvider.class);
        mAccountFeatureProvider = mock(AccountFeatureProvider.class);
        mContextualCardFeatureProvider = mock(ContextualCardFeatureProvider.class);
        panelFeatureProvider = mock(PanelFeatureProvider.class);
        mBluetoothFeatureProvider = mock(BluetoothFeatureProvider.class);
        mAwareFeatureProvider = mock(AwareFeatureProvider.class);
        mFaceFeatureProvider = mock(FaceFeatureProvider.class);
    }

    @Override
    public SuggestionFeatureProvider getSuggestionFeatureProvider(Context context) {
        return suggestionsFeatureProvider;
    }

    @Override
    public SupportFeatureProvider getSupportFeatureProvider(Context context) {
        return supportFeatureProvider;
    }

    @Override
    public MetricsFeatureProvider getMetricsFeatureProvider() {
        return metricsFeatureProvider;
    }

    @Override
    public PowerUsageFeatureProvider getPowerUsageFeatureProvider(Context context) {
        return powerUsageFeatureProvider;
    }

    @Override
    public DashboardFeatureProvider getDashboardFeatureProvider(Context context) {
        return dashboardFeatureProvider;
    }

    @Override
    public DockUpdaterFeatureProvider getDockUpdaterFeatureProvider() {
        return dockUpdaterFeatureProvider;
    }

    @Override
    public ApplicationFeatureProvider getApplicationFeatureProvider(Context context) {
        return applicationFeatureProvider;
    }

    @Override
    public LocaleFeatureProvider getLocaleFeatureProvider() {
        return localeFeatureProvider;
    }

    @Override
    public EnterprisePrivacyFeatureProvider getEnterprisePrivacyFeatureProvider(Context context) {
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

    @Override
    public UserFeatureProvider getUserFeatureProvider(Context context) {
        return userFeatureProvider;
    }

    @Override
    public AssistGestureFeatureProvider getAssistGestureFeatureProvider() {
        return assistGestureFeatureProvider;
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
    public BluetoothFeatureProvider getBluetoothFeatureProvider(Context context) {
        return mBluetoothFeatureProvider;
    }

    @Override
    public AwareFeatureProvider getAwareFeatureProvider() {
        return mAwareFeatureProvider;
    }

    @Override
    public FaceFeatureProvider getFaceFeatureProvider() {
        return mFaceFeatureProvider;
    }
}
