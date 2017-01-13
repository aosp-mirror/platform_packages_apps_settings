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

import android.app.AppGlobals;
import android.app.admin.DevicePolicyManager;
import android.content.Context;
import android.net.ConnectivityManager;
import android.os.UserManager;
import android.support.annotation.Keep;

import com.android.settings.applications.ApplicationFeatureProvider;
import com.android.settings.applications.ApplicationFeatureProviderImpl;
import com.android.settings.applications.PackageManagerWrapperImpl;
import com.android.settings.core.instrumentation.MetricsFeatureProvider;
import com.android.settings.core.instrumentation.MetricsFeatureProviderImpl;
import com.android.settings.dashboard.DashboardFeatureProvider;
import com.android.settings.dashboard.DashboardFeatureProviderImpl;
import com.android.settings.dashboard.SuggestionFeatureProvider;
import com.android.settings.dashboard.SuggestionFeatureProviderImpl;
import com.android.settings.enterprise.DevicePolicyManagerWrapperImpl;
import com.android.settings.enterprise.EnterprisePrivacyFeatureProvider;
import com.android.settings.enterprise.EnterprisePrivacyFeatureProviderImpl;
import com.android.settings.fuelgauge.PowerUsageFeatureProvider;
import com.android.settings.localepicker.LocaleFeatureProvider;
import com.android.settings.localepicker.LocaleFeatureProviderImpl;
import com.android.settings.search2.SearchFeatureProvider;
import com.android.settings.search2.SearchFeatureProviderImpl;
import com.android.settings.security.SecurityFeatureProvider;
import com.android.settings.security.SecurityFeatureProviderImpl;
import com.android.settings.vpn2.ConnectivityManagerWrapperImpl;

/**
 * {@link FeatureFactory} implementation for AOSP Settings.
 */
@Keep
public class FeatureFactoryImpl extends FeatureFactory {

    private ApplicationFeatureProvider mApplicationFeatureProvider;
    private MetricsFeatureProvider mMetricsFeatureProvider;
    private DashboardFeatureProviderImpl mDashboardFeatureProvider;
    private LocaleFeatureProvider mLocaleFeatureProvider;
    private EnterprisePrivacyFeatureProvider mEnterprisePrivacyFeatureProvider;
    private SearchFeatureProvider mSearchFeatureProvider;
    private SecurityFeatureProvider mSecurityFeatureProvider;
    private SuggestionFeatureProvider mSuggestionFeatureProvider;

    @Override
    public SupportFeatureProvider getSupportFeatureProvider(Context context) {
        return null;
    }

    @Override
    public MetricsFeatureProvider getMetricsFeatureProvider() {
        if (mMetricsFeatureProvider == null) {
            mMetricsFeatureProvider = new MetricsFeatureProviderImpl();
        }
        return mMetricsFeatureProvider;
    }

    @Override
    public PowerUsageFeatureProvider getPowerUsageFeatureProvider(Context context) {
        return null;
    }

    @Override
    public DashboardFeatureProvider getDashboardFeatureProvider(Context context) {
        if (mDashboardFeatureProvider == null) {
            mDashboardFeatureProvider = new DashboardFeatureProviderImpl(context);
        }
        return mDashboardFeatureProvider;
    }

    @Override
    public ApplicationFeatureProvider getApplicationFeatureProvider(Context context) {
        if (mApplicationFeatureProvider == null) {
            mApplicationFeatureProvider = new ApplicationFeatureProviderImpl(context,
                    new PackageManagerWrapperImpl(context.getPackageManager()),
                    AppGlobals.getPackageManager(),
                    new DevicePolicyManagerWrapperImpl((DevicePolicyManager) context
                            .getSystemService(Context.DEVICE_POLICY_SERVICE)));
        }
        return mApplicationFeatureProvider;
    }

    @Override
    public LocaleFeatureProvider getLocaleFeatureProvider() {
        if (mLocaleFeatureProvider == null) {
            mLocaleFeatureProvider = new LocaleFeatureProviderImpl();
        }
        return mLocaleFeatureProvider;
    }

    @Override
    public EnterprisePrivacyFeatureProvider getEnterprisePrivacyFeatureProvider(Context context) {
        if (mEnterprisePrivacyFeatureProvider == null) {
            mEnterprisePrivacyFeatureProvider = new EnterprisePrivacyFeatureProviderImpl(
                    new DevicePolicyManagerWrapperImpl((DevicePolicyManager) context
                            .getSystemService(Context.DEVICE_POLICY_SERVICE)),
                    new PackageManagerWrapperImpl(context.getPackageManager()),
                    UserManager.get(context),
                    new ConnectivityManagerWrapperImpl((ConnectivityManager) context
                            .getSystemService(Context.CONNECTIVITY_SERVICE)));
        }
        return mEnterprisePrivacyFeatureProvider;
    }

    @Override
    public SearchFeatureProvider getSearchFeatureProvider() {
        if (mSearchFeatureProvider == null) {
            mSearchFeatureProvider = new SearchFeatureProviderImpl();
        }
        return mSearchFeatureProvider;
    }

    @Override
    public SurveyFeatureProvider getSurveyFeatureProvider(Context context) {
        return null;
    }

    @Override
    public SecurityFeatureProvider getSecurityFeatureProvider() {
        if (mSecurityFeatureProvider == null) {
            mSecurityFeatureProvider = new SecurityFeatureProviderImpl();
        }
        return mSecurityFeatureProvider;
    }

    @Override
    public SuggestionFeatureProvider getSuggestionFeatureProvider() {
        if (mSuggestionFeatureProvider == null) {
            mSuggestionFeatureProvider = new SuggestionFeatureProviderImpl();
        }
        return mSuggestionFeatureProvider;
    }
}
