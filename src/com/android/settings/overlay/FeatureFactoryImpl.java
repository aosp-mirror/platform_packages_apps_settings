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
import com.android.settings.applications.IPackageManagerWrapperImpl;
import com.android.settings.applications.PackageManagerWrapperImpl;
import com.android.settings.bluetooth.BluetoothFeatureProvider;
import com.android.settings.bluetooth.BluetoothFeatureProviderImpl;
import com.android.settings.connecteddevice.SmsMirroringFeatureProvider;
import com.android.settings.connecteddevice.SmsMirroringFeatureProviderImpl;
import com.android.settings.core.instrumentation.MetricsFeatureProvider;
import com.android.settings.dashboard.DashboardFeatureProvider;
import com.android.settings.dashboard.DashboardFeatureProviderImpl;
import com.android.settings.dashboard.suggestions.SuggestionFeatureProvider;
import com.android.settings.dashboard.suggestions.SuggestionFeatureProviderImpl;
import com.android.settings.datausage.DataPlanFeatureProvider;
import com.android.settings.datausage.DataPlanFeatureProviderImpl;
import com.android.settings.enterprise.DevicePolicyManagerWrapperImpl;
import com.android.settings.enterprise.EnterprisePrivacyFeatureProvider;
import com.android.settings.enterprise.EnterprisePrivacyFeatureProviderImpl;
import com.android.settings.fuelgauge.PowerUsageFeatureProvider;
import com.android.settings.fuelgauge.PowerUsageFeatureProviderImpl;
import com.android.settings.gestures.AssistGestureFeatureProvider;
import com.android.settings.gestures.AssistGestureFeatureProviderImpl;
import com.android.settings.localepicker.LocaleFeatureProvider;
import com.android.settings.localepicker.LocaleFeatureProviderImpl;
import com.android.settings.search.SearchFeatureProvider;
import com.android.settings.search.SearchFeatureProviderImpl;
import com.android.settings.security.SecurityFeatureProvider;
import com.android.settings.security.SecurityFeatureProviderImpl;
import com.android.settings.users.UserFeatureProvider;
import com.android.settings.users.UserFeatureProviderImpl;
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
    private PowerUsageFeatureProvider mPowerUsageFeatureProvider;
    private AssistGestureFeatureProvider mAssistGestureFeatureProvider;
    private UserFeatureProvider mUserFeatureProvider;
    private BluetoothFeatureProvider mBluetoothFeatureProvider;
    private DataPlanFeatureProvider mDataPlanFeatureProvider;
    private SmsMirroringFeatureProvider mSmsMirroringFeatureProvider;

    @Override
    public SupportFeatureProvider getSupportFeatureProvider(Context context) {
        return null;
    }

    @Override
    public MetricsFeatureProvider getMetricsFeatureProvider() {
        if (mMetricsFeatureProvider == null) {
            mMetricsFeatureProvider = new MetricsFeatureProvider();
        }
        return mMetricsFeatureProvider;
    }

    @Override
    public PowerUsageFeatureProvider getPowerUsageFeatureProvider(Context context) {
        if (mPowerUsageFeatureProvider == null) {
            mPowerUsageFeatureProvider = new PowerUsageFeatureProviderImpl(context);
        }
        return mPowerUsageFeatureProvider;
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
                    new IPackageManagerWrapperImpl(AppGlobals.getPackageManager()),
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
            mEnterprisePrivacyFeatureProvider = new EnterprisePrivacyFeatureProviderImpl(context,
                    new DevicePolicyManagerWrapperImpl((DevicePolicyManager) context
                            .getSystemService(Context.DEVICE_POLICY_SERVICE)),
                    new PackageManagerWrapperImpl(context.getPackageManager()),
                    UserManager.get(context),
                    new ConnectivityManagerWrapperImpl((ConnectivityManager) context
                            .getSystemService(Context.CONNECTIVITY_SERVICE)),
                    context.getResources());
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
    public SuggestionFeatureProvider getSuggestionFeatureProvider(Context context) {
        if (mSuggestionFeatureProvider == null) {
            mSuggestionFeatureProvider = new SuggestionFeatureProviderImpl(context);
        }
        return mSuggestionFeatureProvider;
    }

    @Override
    public UserFeatureProvider getUserFeatureProvider(Context context) {
        if (mUserFeatureProvider == null) {
            mUserFeatureProvider = new UserFeatureProviderImpl(context);
        }
        return mUserFeatureProvider;
    }

    @Override
    public BluetoothFeatureProvider getBluetoothFeatureProvider(Context context) {
        if (mBluetoothFeatureProvider == null) {
            mBluetoothFeatureProvider = new BluetoothFeatureProviderImpl();
        }
        return mBluetoothFeatureProvider;
    }

    @Override
    public DataPlanFeatureProvider getDataPlanFeatureProvider() {
        if (mDataPlanFeatureProvider == null) {
            mDataPlanFeatureProvider = new DataPlanFeatureProviderImpl();
        }
        return mDataPlanFeatureProvider;
    }

    @Override
    public AssistGestureFeatureProvider getAssistGestureFeatureProvider() {
        if (mAssistGestureFeatureProvider == null) {
            mAssistGestureFeatureProvider = new AssistGestureFeatureProviderImpl();
        }
        return mAssistGestureFeatureProvider;
    }

    @Override
    public SmsMirroringFeatureProvider getSmsMirroringFeatureProvider() {
        if (mSmsMirroringFeatureProvider == null) {
            mSmsMirroringFeatureProvider = new SmsMirroringFeatureProviderImpl();
        }
        return mSmsMirroringFeatureProvider;
    }
}
