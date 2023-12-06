/**
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

package com.android.settings.network.telephony;

import static androidx.lifecycle.Lifecycle.Event.ON_START;
import static androidx.lifecycle.Lifecycle.Event.ON_STOP;

import android.Manifest;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.ComponentInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.pm.ServiceInfo;
import android.service.euicc.EuiccService;
import android.telephony.SubscriptionInfo;
import android.telephony.SubscriptionManager;
import android.telephony.euicc.EuiccManager;
import android.text.TextUtils;

import androidx.annotation.VisibleForTesting;
import androidx.lifecycle.LifecycleObserver;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.OnLifecycleEvent;
import androidx.preference.Preference;
import androidx.preference.PreferenceScreen;

import com.android.internal.telephony.util.TelephonyUtils;
import com.android.settings.network.MobileNetworkRepository;
import com.android.settingslib.core.lifecycle.Lifecycle;
import com.android.settingslib.mobile.dataservice.SubscriptionInfoEntity;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public class ConvertToEsimPreferenceController extends TelephonyBasePreferenceController implements
        LifecycleObserver, MobileNetworkRepository.MobileNetworkCallback {

    private Preference mPreference;
    private LifecycleOwner mLifecycleOwner;
    private MobileNetworkRepository mMobileNetworkRepository;
    private List<SubscriptionInfoEntity> mSubscriptionInfoEntityList = new ArrayList<>();
    private SubscriptionInfoEntity mSubscriptionInfoEntity;
    private static int sQueryFlag =
            PackageManager.MATCH_SYSTEM_ONLY | PackageManager.MATCH_DIRECT_BOOT_AUTO
                    | PackageManager.GET_RESOLVED_FILTER;

    public ConvertToEsimPreferenceController(Context context, String key, Lifecycle lifecycle,
            LifecycleOwner lifecycleOwner, int subId) {
        super(context, key);
        mSubId = subId;
        mMobileNetworkRepository = MobileNetworkRepository.getInstance(context);
        mLifecycleOwner = lifecycleOwner;
        if (lifecycle != null) {
            lifecycle.addObserver(this);
        }
    }

    public void init(int subId, SubscriptionInfoEntity subInfoEntity) {
        mSubId = subId;
        mSubscriptionInfoEntity = subInfoEntity;
    }

    @OnLifecycleEvent(ON_START)
    public void onStart() {
        mMobileNetworkRepository.addRegister(mLifecycleOwner, this, mSubId);
        mMobileNetworkRepository.updateEntity();
    }

    @OnLifecycleEvent(ON_STOP)
    public void onStop() {
        mMobileNetworkRepository.removeRegister(this);
    }

    @Override
    public void displayPreference(PreferenceScreen screen) {
        super.displayPreference(screen);
        mPreference = screen.findPreference(getPreferenceKey());
    }

    @Override
    public int getAvailabilityStatus(int subId) {
        // TODO(b/262195754): Need the intent to enabled the feature.
        if (findConversionSupportComponent()) {
            return mSubscriptionInfoEntity != null && mSubscriptionInfoEntity.isActiveSubscriptionId
                    && !mSubscriptionInfoEntity.isEmbedded && isActiveSubscription(subId)
                    ? AVAILABLE
                    : CONDITIONALLY_UNAVAILABLE;
        }
        return CONDITIONALLY_UNAVAILABLE;
    }

    @VisibleForTesting
    void update() {
        if (mPreference == null) {
            return;
        }
        mPreference.setVisible(getAvailabilityStatus(mSubId) == AVAILABLE);
    }

    @Override
    public boolean handlePreferenceTreeClick(Preference preference) {
        if (!TextUtils.equals(preference.getKey(), getPreferenceKey())) {
            return false;
        }
        // Send intent to launch LPA
        Intent intent = new Intent(EuiccManager.ACTION_CONVERT_TO_EMBEDDED_SUBSCRIPTION);
        intent.putExtra("subId", mSubId);
        mContext.startActivity(intent);
        return true;
    }

    @VisibleForTesting
    public void setSubscriptionInfoEntity(SubscriptionInfoEntity subscriptionInfoEntity) {
        mSubscriptionInfoEntity = subscriptionInfoEntity;
    }

    @Override
    public void onActiveSubInfoChanged(List<SubscriptionInfoEntity> subInfoEntityList) {
        // TODO(b/262195754): Need the intent to enabled the feature.
        mSubscriptionInfoEntityList = subInfoEntityList;
        mSubscriptionInfoEntityList.forEach(entity -> {
            if (Integer.parseInt(entity.subId) == mSubId) {
                mSubscriptionInfoEntity = entity;
                update();
            }
        });
    }

    private boolean isActiveSubscription(int subId) {
        SubscriptionManager subscriptionManager = mContext.getSystemService(
                SubscriptionManager.class);
        SubscriptionInfo subInfo = subscriptionManager.getActiveSubscriptionInfo(subId);
        if (subInfo == null) {
            return false;
        }
        return true;
    }

    private boolean findConversionSupportComponent() {
        Intent intent = new Intent(EuiccService.ACTION_CONVERT_TO_EMBEDDED_SUBSCRIPTION);
        PackageManager packageManager = mContext.getPackageManager();
        List<ResolveInfo> resolveInfoList = packageManager
                .queryIntentActivities(intent, sQueryFlag);
        if (resolveInfoList == null || resolveInfoList.isEmpty()) {
            return false;
        }
        for (ResolveInfo resolveInfo : resolveInfoList) {
            if (!isValidEuiccComponent(packageManager, resolveInfo)) {
                continue;
            } else {
                return true;
            }
        }
        return true;
    }

    private boolean isValidEuiccComponent(
            PackageManager packageManager, @NotNull ResolveInfo resolveInfo) {
        ComponentInfo componentInfo = TelephonyUtils.getComponentInfo(resolveInfo);
        String packageName = new ComponentName(componentInfo.packageName, componentInfo.name)
                .getPackageName();

        // Verify that the app is privileged (via granting of a privileged permission).
        if (packageManager.checkPermission(
                Manifest.permission.WRITE_EMBEDDED_SUBSCRIPTIONS, packageName)
                != PackageManager.PERMISSION_GRANTED) {
            return false;
        }

        // Verify that only the system can access the component.
        final String permission;
        if (componentInfo instanceof ServiceInfo) {
            permission = ((ServiceInfo) componentInfo).permission;
        } else if (componentInfo instanceof ActivityInfo) {
            permission = ((ActivityInfo) componentInfo).permission;
        } else {
            return false;
        }
        if (!TextUtils.equals(permission, Manifest.permission.BIND_EUICC_SERVICE)) {
            return false;
        }

        // Verify that the component declares a priority.
        if (resolveInfo.filter == null || resolveInfo.filter.getPriority() == 0) {
            return false;
        }
        return true;
    }
}
