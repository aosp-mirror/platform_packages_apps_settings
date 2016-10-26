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

import android.content.Context;

import com.android.settings.applications.ApplicationFeatureProvider;
import com.android.settings.core.instrumentation.MetricsFeatureProvider;
import com.android.settings.dashboard.DashboardFeatureProvider;
import com.android.settings.fuelgauge.PowerUsageFeatureProvider;
import com.android.settings.localepicker.LocaleFeatureProvider;
import com.android.settings.overlay.FeatureFactory;
import com.android.settings.overlay.SupportFeatureProvider;

import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Test util to provide fake FeatureFactory. To use this factory, call {@code setupForTest} in
 * {@code @Before} method of the test class.
 */
public class FakeFeatureFactory extends FeatureFactory {

    public final SupportFeatureProvider supportFeatureProvider;
    public final MetricsFeatureProvider metricsFeatureProvider;
    public final PowerUsageFeatureProvider powerUsageFeatureProvider;
    public final DashboardFeatureProvider dashboardFeatureProvider;
    public final LocaleFeatureProvider localeFeatureProvider;
    public final ApplicationFeatureProvider applicationFeatureProvider;

    /**
     * Call this in {@code @Before} method of the test class to use fake factory.
     *
     * @param context The context must be a deep mock.
     */
    public static void setupForTest(Context context) {
        sFactory = null;
        when(context.getString(com.android.settings.R.string.config_featureFactory))
                .thenReturn(FakeFeatureFactory.class.getName());
        try {
            Class c = FakeFeatureFactory.class;
            when(context.getClassLoader().loadClass(anyString())).thenReturn(c);
        } catch (ClassNotFoundException e) {
            // Ignore.
        }
    }

    /**
     * Used by reflection. Do not call directly.
     */
    public FakeFeatureFactory() {
        supportFeatureProvider = mock(SupportFeatureProvider.class);
        metricsFeatureProvider = mock(MetricsFeatureProvider.class);
        powerUsageFeatureProvider = mock(PowerUsageFeatureProvider.class);
        dashboardFeatureProvider = mock(DashboardFeatureProvider.class);
        localeFeatureProvider = mock(LocaleFeatureProvider.class);
        applicationFeatureProvider = mock(ApplicationFeatureProvider.class);
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
    public PowerUsageFeatureProvider getPowerUsageFeatureProvider() {
        return powerUsageFeatureProvider;
    }

    @Override
    public DashboardFeatureProvider getDashboardFeatureProvider(Context context) {
        return dashboardFeatureProvider;
    }

    @Override
    public ApplicationFeatureProvider getApplicationFeatureProvider(Context context) {
        return applicationFeatureProvider;
    }

    @Override
    public LocaleFeatureProvider getLocaleFeatureProvider() {
        return localeFeatureProvider;
    }
}
