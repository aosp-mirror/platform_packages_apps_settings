/*
 * Copyright (C) 2014 The Android Open Source Project
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

package com.android.settings;

import android.app.admin.DevicePolicyManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.UserHandle;
import android.service.trust.TrustAgentService;
import android.support.v14.preference.SwitchPreference;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceGroup;
import android.util.ArrayMap;
import android.util.ArraySet;

import com.android.internal.logging.nano.MetricsProto.MetricsEvent;
import com.android.internal.widget.LockPatternUtils;
import com.android.settings.overlay.FeatureFactory;
import com.android.settings.trustagent.TrustAgentManager;
import com.android.settingslib.RestrictedLockUtils;
import com.android.settingslib.RestrictedSwitchPreference;

import java.util.List;

import static com.android.settingslib.RestrictedLockUtils.EnforcedAdmin;

public class TrustAgentSettings extends SettingsPreferenceFragment implements
        Preference.OnPreferenceChangeListener {
    private static final String SERVICE_INTERFACE = TrustAgentService.SERVICE_INTERFACE;

    private ArrayMap<ComponentName, AgentInfo> mAvailableAgents;
    private final ArraySet<ComponentName> mActiveAgents = new ArraySet<ComponentName>();
    private LockPatternUtils mLockPatternUtils;
    private DevicePolicyManager mDpm;
    private TrustAgentManager mTrustAgentManager;

    public static final class AgentInfo {
        CharSequence label;
        ComponentName component; // service that implements ITrustAgent
        SwitchPreference preference;
        public Drawable icon;

        @Override
        public boolean equals(Object other) {
            if (other instanceof AgentInfo) {
                return component.equals(((AgentInfo)other).component);
            }
            return true;
        }

        public int compareTo(AgentInfo other) {
            return component.compareTo(other.component);
        }
    }

    @Override
    public int getMetricsCategory() {
        return MetricsEvent.TRUST_AGENT;
    }

    @Override
    protected int getHelpResource() {
        return R.string.help_url_trust_agent;
    }

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        mDpm = getActivity().getSystemService(DevicePolicyManager.class);
        mTrustAgentManager =
            FeatureFactory.getFactory(getActivity()).getSecurityFeatureProvider()
                .getTrustAgentManager();

        addPreferencesFromResource(R.xml.trust_agent_settings);
    }

    public void onResume() {
        super.onResume();
        removePreference("dummy_preference");
        updateAgents();
    };

    private void updateAgents() {
        final Context context = getActivity();
        if (mAvailableAgents == null) {
            mAvailableAgents = findAvailableTrustAgents();
        }
        if (mLockPatternUtils == null) {
            mLockPatternUtils = new LockPatternUtils(getActivity());
        }
        loadActiveAgents();
        PreferenceGroup category =
                (PreferenceGroup) getPreferenceScreen().findPreference("trust_agents");
        category.removeAll();

        final EnforcedAdmin admin = RestrictedLockUtils.checkIfKeyguardFeaturesDisabled(context,
                DevicePolicyManager.KEYGUARD_DISABLE_TRUST_AGENTS, UserHandle.myUserId());

        final int count = mAvailableAgents.size();
        for (int i = 0; i < count; i++) {
            AgentInfo agent = mAvailableAgents.valueAt(i);
            final RestrictedSwitchPreference preference =
                    new RestrictedSwitchPreference(getPrefContext());
            preference.useAdminDisabledSummary(true);
            agent.preference = preference;
            preference.setPersistent(false);
            preference.setTitle(agent.label);
            preference.setIcon(agent.icon);
            preference.setPersistent(false);
            preference.setOnPreferenceChangeListener(this);
            preference.setChecked(mActiveAgents.contains(agent.component));

            if (admin != null
                    && mDpm.getTrustAgentConfiguration(null, agent.component) == null) {
                preference.setChecked(false);
                preference.setDisabledByAdmin(admin);
            }

            category.addPreference(agent.preference);
        }
    }

    private void loadActiveAgents() {
        List<ComponentName> activeTrustAgents = mLockPatternUtils.getEnabledTrustAgents(
                UserHandle.myUserId());
        if (activeTrustAgents != null) {
            mActiveAgents.addAll(activeTrustAgents);
        }
    }

    private void saveActiveAgents() {
        mLockPatternUtils.setEnabledTrustAgents(mActiveAgents,
                UserHandle.myUserId());
    }

    ArrayMap<ComponentName, AgentInfo> findAvailableTrustAgents() {
        PackageManager pm = getActivity().getPackageManager();
        Intent trustAgentIntent = new Intent(SERVICE_INTERFACE);
        List<ResolveInfo> resolveInfos = pm.queryIntentServices(trustAgentIntent,
                PackageManager.GET_META_DATA);

        ArrayMap<ComponentName, AgentInfo> agents = new ArrayMap<ComponentName, AgentInfo>();
        final int count = resolveInfos.size();
        agents.ensureCapacity(count);
        for (int i = 0; i < count; i++ ) {
            ResolveInfo resolveInfo = resolveInfos.get(i);
            if (resolveInfo.serviceInfo == null) {
                continue;
            }
            if (!mTrustAgentManager.shouldProvideTrust(resolveInfo, pm)) {
                continue;
            }
            ComponentName name = TrustAgentUtils.getComponentName(resolveInfo);
            AgentInfo agentInfo = new AgentInfo();
            agentInfo.label = resolveInfo.loadLabel(pm);
            agentInfo.icon = resolveInfo.loadIcon(pm);
            agentInfo.component = name;
            agents.put(name, agentInfo);
        }
        return agents;
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        if (preference instanceof SwitchPreference) {
            final int count = mAvailableAgents.size();
            for (int i = 0; i < count; i++) {
                AgentInfo agent = mAvailableAgents.valueAt(i);
                if (agent.preference == preference) {
                    if ((Boolean) newValue) {
                        if (!mActiveAgents.contains(agent.component)) {
                            mActiveAgents.add(agent.component);
                        }
                    } else {
                        mActiveAgents.remove(agent.component);
                    }
                    saveActiveAgents();
                    return true;
                }
            }
        }
        return false;
    }

}
