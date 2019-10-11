/*
 * Copyright (C) 2018 The Android Open Source Project
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

package com.android.settings.security.trustagent;

import static com.android.settingslib.RestrictedLockUtils.EnforcedAdmin;

import android.app.admin.DevicePolicyManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.drawable.Drawable;
import android.os.UserHandle;
import android.service.trust.TrustAgentService;
import android.text.TextUtils;
import android.util.ArrayMap;
import android.util.ArraySet;
import android.util.IconDrawableFactory;

import androidx.preference.Preference;
import androidx.preference.PreferenceScreen;
import androidx.preference.SwitchPreference;

import com.android.internal.widget.LockPatternUtils;
import com.android.settings.core.BasePreferenceController;
import com.android.settings.overlay.FeatureFactory;
import com.android.settings.security.SecurityFeatureProvider;
import com.android.settingslib.RestrictedLockUtilsInternal;
import com.android.settingslib.RestrictedSwitchPreference;
import com.android.settingslib.core.lifecycle.LifecycleObserver;
import com.android.settingslib.core.lifecycle.events.OnStart;

import java.util.List;

public class TrustAgentsPreferenceController extends BasePreferenceController
        implements Preference.OnPreferenceChangeListener, LifecycleObserver, OnStart {

    private static final Intent TRUST_AGENT_INTENT =
            new Intent(TrustAgentService.SERVICE_INTERFACE);

    private final ArrayMap<ComponentName, TrustAgentInfo> mAvailableAgents;
    private final ArraySet<ComponentName> mActiveAgents;
    private final DevicePolicyManager mDevicePolicyManager;
    private final IconDrawableFactory mIconDrawableFactory;
    private final LockPatternUtils mLockPatternUtils;
    private final PackageManager mPackageManager;
    private final TrustAgentManager mTrustAgentManager;

    private PreferenceScreen mScreen;

    public TrustAgentsPreferenceController(Context context, String key) {
        super(context, key);
        mAvailableAgents = new ArrayMap<>();
        mActiveAgents = new ArraySet<>();
        mDevicePolicyManager = context.getSystemService(DevicePolicyManager.class);
        mIconDrawableFactory = IconDrawableFactory.newInstance(context);
        final SecurityFeatureProvider securityFeatureProvider =
                FeatureFactory.getFactory(context).getSecurityFeatureProvider();
        mTrustAgentManager = securityFeatureProvider.getTrustAgentManager();
        mLockPatternUtils = securityFeatureProvider.getLockPatternUtils(context);
        mPackageManager = context.getPackageManager();
    }

    @Override
    public void displayPreference(PreferenceScreen screen) {
        super.displayPreference(screen);
        mScreen = screen;
    }

    @Override
    public int getAvailabilityStatus() {
        return AVAILABLE;
    }

    @Override
    public void onStart() {
        updateAgents();
    }

    private void updateAgents() {
        findAvailableTrustAgents();
        loadActiveAgents();
        removeUselessExistingPreferences();

        final EnforcedAdmin admin = RestrictedLockUtilsInternal.checkIfKeyguardFeaturesDisabled(
                mContext, DevicePolicyManager.KEYGUARD_DISABLE_TRUST_AGENTS, UserHandle.myUserId());

        for (TrustAgentInfo agent : mAvailableAgents.values()) {
            final ComponentName componentName = agent.getComponentName();
            RestrictedSwitchPreference preference = (RestrictedSwitchPreference)
                    mScreen.findPreference(componentName.flattenToString());
            if (preference == null) {
                preference = new RestrictedSwitchPreference(mScreen.getContext());
            }
            preference.setKey(componentName.flattenToString());
            preference.useAdminDisabledSummary(true);
            preference.setTitle(agent.getLabel());
            preference.setIcon(agent.getIcon());
            preference.setOnPreferenceChangeListener(this);
            preference.setChecked(mActiveAgents.contains(componentName));
            if (admin != null && mDevicePolicyManager.getTrustAgentConfiguration(null /* admin */,
                    componentName) == null) {
                preference.setChecked(false);
                preference.setDisabledByAdmin(admin);
            }
            mScreen.addPreference(preference);
        }
    }

    private void loadActiveAgents() {
        final List<ComponentName> activeTrustAgents = mLockPatternUtils.getEnabledTrustAgents(
                UserHandle.myUserId());
        if (activeTrustAgents != null) {
            mActiveAgents.addAll(activeTrustAgents);
        }
    }

    private void saveActiveAgents() {
        mLockPatternUtils.setEnabledTrustAgents(mActiveAgents, UserHandle.myUserId());
    }

    private void findAvailableTrustAgents() {
        final List<ResolveInfo> resolveInfos = mPackageManager.queryIntentServices(
                TRUST_AGENT_INTENT, PackageManager.GET_META_DATA);
        mAvailableAgents.clear();
        for (ResolveInfo resolveInfo : resolveInfos) {
            if (resolveInfo.serviceInfo == null) {
                continue;
            }
            if (!mTrustAgentManager.shouldProvideTrust(resolveInfo, mPackageManager)) {
                continue;
            }
            final CharSequence label = resolveInfo.loadLabel(mPackageManager);
            final ComponentName componentName = mTrustAgentManager.getComponentName(resolveInfo);
            final Drawable icon = mIconDrawableFactory.getBadgedIcon(
                    resolveInfo.getComponentInfo().applicationInfo);
            final TrustAgentInfo agentInfo = new TrustAgentInfo(label, componentName, icon);
            mAvailableAgents.put(componentName, agentInfo);
        }
    }

    private void removeUselessExistingPreferences() {
        final int count = mScreen.getPreferenceCount();
        if (count <= 0) {
            return;
        }
        for (int i = count - 1; i >= 0; i--) {
            final Preference pref = mScreen.getPreference(i);
            final String[] names = TextUtils.split(pref.getKey(), "/");
            final ComponentName componentName = new ComponentName(names[0], names[1]);
            if (!mAvailableAgents.containsKey(componentName)) {
                mScreen.removePreference(pref);
                mActiveAgents.remove(componentName);
            }
        }
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        if (!(preference instanceof SwitchPreference)) {
            return false;
        }
        for (TrustAgentInfo agent : mAvailableAgents.values()) {
            final ComponentName componentName = agent.getComponentName();
            if (!TextUtils.equals(preference.getKey(), componentName.flattenToString())) {
                continue;
            }
            if ((Boolean) newValue && !mActiveAgents.contains(componentName)) {
                mActiveAgents.add(componentName);
            } else {
                mActiveAgents.remove(componentName);
            }
            saveActiveAgents();
            return true;
        }
        return false;
    }
}
