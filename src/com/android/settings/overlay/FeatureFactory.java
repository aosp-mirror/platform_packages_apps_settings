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

package com.android.settings.overlay;

import android.content.Context;
import android.text.TextUtils;
import android.util.Log;

import androidx.annotation.Nullable;

import com.android.settings.R;
import com.android.settings.accounts.AccountFeatureProvider;
import com.android.settings.applications.ApplicationFeatureProvider;
import com.android.settings.aware.AwareFeatureProvider;
import com.android.settings.biometrics.face.FaceFeatureProvider;
import com.android.settings.bluetooth.BluetoothFeatureProvider;
import com.android.settings.dashboard.DashboardFeatureProvider;
import com.android.settings.dashboard.suggestions.SuggestionFeatureProvider;
import com.android.settings.enterprise.EnterprisePrivacyFeatureProvider;
import com.android.settings.fuelgauge.BatteryStatusFeatureProvider;
import com.android.settings.fuelgauge.PowerUsageFeatureProvider;
import com.android.settings.gestures.AssistGestureFeatureProvider;
import com.android.settings.homepage.contextualcards.ContextualCardFeatureProvider;
import com.android.settings.localepicker.LocaleFeatureProvider;
import com.android.settings.panel.PanelFeatureProvider;
import com.android.settings.search.SearchFeatureProvider;
import com.android.settings.security.SecurityFeatureProvider;
import com.android.settings.slices.SlicesFeatureProvider;
import com.android.settings.users.UserFeatureProvider;
import com.android.settingslib.core.instrumentation.MetricsFeatureProvider;

/**
 * Abstract class for creating feature controllers. Allows OEM implementations to define their own
 * factories with their own controllers containing whatever code is needed to implement
 * the features. To provide a factory implementation, implementors should override
 * {@link R.string#config_featureFactory} in their override.
 */
public abstract class FeatureFactory {
    private static final String LOG_TAG = "FeatureFactory";
    private static final boolean DEBUG = false;

    protected static FeatureFactory sFactory;
    protected static Context sAppContext;

    /**
     * Returns a factory for creating feature controllers. Creates the factory if it does not
     * already exist. Uses the value of {@link R.string#config_featureFactory} to instantiate
     * a factory implementation.
     */
    public static FeatureFactory getFactory(Context context) {
        if (sFactory != null) {
            return sFactory;
        }
        if (sAppContext == null) {
            sAppContext = context.getApplicationContext();
        }

        if (DEBUG) Log.d(LOG_TAG, "getFactory");
        final String clsName = context.getString(R.string.config_featureFactory);
        if (TextUtils.isEmpty(clsName)) {
            throw new UnsupportedOperationException("No feature factory configured");
        }
        try {
            sFactory = (FeatureFactory) context.getClassLoader().loadClass(clsName).newInstance();
        } catch (InstantiationException | IllegalAccessException | ClassNotFoundException e) {
            throw new FactoryNotFoundException(e);
        }

        if (DEBUG) Log.d(LOG_TAG, "started " + sFactory.getClass().getSimpleName());
        return sFactory;
    }

    /**
     * Returns an application {@link Context} used to create this {@link FeatureFactory}. If the
     * factory has not been properly created yet (aka {@link #getFactory} has not been called), this
     * will return null.
     */
    @Nullable
    public static Context getAppContext() {
        return sAppContext;
    }

    public abstract AssistGestureFeatureProvider getAssistGestureFeatureProvider();

    public abstract SuggestionFeatureProvider getSuggestionFeatureProvider(Context context);

    public abstract SupportFeatureProvider getSupportFeatureProvider(Context context);

    public abstract MetricsFeatureProvider getMetricsFeatureProvider();

    public abstract PowerUsageFeatureProvider getPowerUsageFeatureProvider(Context context);

    /**
     * Retrieve implementation for Battery Status feature.
     */
    public abstract BatteryStatusFeatureProvider getBatteryStatusFeatureProvider(
            Context context);

    public abstract DashboardFeatureProvider getDashboardFeatureProvider(Context context);

    public abstract DockUpdaterFeatureProvider getDockUpdaterFeatureProvider();

    public abstract ApplicationFeatureProvider getApplicationFeatureProvider(Context context);

    public abstract LocaleFeatureProvider getLocaleFeatureProvider();

    public abstract EnterprisePrivacyFeatureProvider getEnterprisePrivacyFeatureProvider(
            Context context);

    public abstract SearchFeatureProvider getSearchFeatureProvider();

    public abstract SurveyFeatureProvider getSurveyFeatureProvider(Context context);

    public abstract SecurityFeatureProvider getSecurityFeatureProvider();

    public abstract UserFeatureProvider getUserFeatureProvider(Context context);

    public abstract SlicesFeatureProvider getSlicesFeatureProvider();

    public abstract AccountFeatureProvider getAccountFeatureProvider();

    public abstract PanelFeatureProvider getPanelFeatureProvider();

    public abstract ContextualCardFeatureProvider getContextualCardFeatureProvider(Context context);

    public abstract BluetoothFeatureProvider getBluetoothFeatureProvider(Context context);

    public abstract AwareFeatureProvider getAwareFeatureProvider();

    public abstract FaceFeatureProvider getFaceFeatureProvider();

    public static final class FactoryNotFoundException extends RuntimeException {
        public FactoryNotFoundException(Throwable throwable) {
            super("Unable to create factory. Did you misconfigure Proguard?", throwable);
        }
    }
}
