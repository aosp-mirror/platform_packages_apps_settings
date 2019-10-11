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

import static com.android.settings.security.SecuritySettings.CHANGE_TRUST_AGENT_SETTINGS;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.UserHandle;
import android.text.TextUtils;

import androidx.annotation.VisibleForTesting;
import androidx.preference.Preference;
import androidx.preference.PreferenceCategory;
import androidx.preference.PreferenceScreen;

import com.android.internal.widget.LockPatternUtils;
import com.android.settings.R;
import com.android.settings.core.PreferenceControllerMixin;
import com.android.settings.overlay.FeatureFactory;
import com.android.settings.password.ChooseLockSettingsHelper;
import com.android.settings.security.SecurityFeatureProvider;
import com.android.settings.security.SecuritySettings;
import com.android.settingslib.RestrictedPreference;
import com.android.settingslib.core.AbstractPreferenceController;
import com.android.settingslib.core.lifecycle.Lifecycle;
import com.android.settingslib.core.lifecycle.LifecycleObserver;
import com.android.settingslib.core.lifecycle.events.OnCreate;
import com.android.settingslib.core.lifecycle.events.OnResume;
import com.android.settingslib.core.lifecycle.events.OnSaveInstanceState;

import java.util.List;

public class TrustAgentListPreferenceController extends AbstractPreferenceController
        implements PreferenceControllerMixin, LifecycleObserver, OnSaveInstanceState,
        OnCreate, OnResume {

    private static final String TRUST_AGENT_CLICK_INTENT = "trust_agent_click_intent";
    @VisibleForTesting
    static final String PREF_KEY_TRUST_AGENT = "trust_agent";
    @VisibleForTesting
    static final String PREF_KEY_SECURITY_CATEGORY = "security_category";
    private static final int MY_USER_ID = UserHandle.myUserId();

    private final LockPatternUtils mLockPatternUtils;
    private final TrustAgentManager mTrustAgentManager;
    private final SecuritySettings mHost;

    private Intent mTrustAgentClickIntent;
    private PreferenceCategory mSecurityCategory;

    public TrustAgentListPreferenceController(Context context, SecuritySettings host,
            Lifecycle lifecycle) {
        super(context);
        final SecurityFeatureProvider provider = FeatureFactory.getFactory(context)
                .getSecurityFeatureProvider();
        mHost = host;
        mLockPatternUtils = provider.getLockPatternUtils(context);
        mTrustAgentManager = provider.getTrustAgentManager();
        if (lifecycle != null) {
            lifecycle.addObserver(this);
        }
    }

    @Override
    public boolean isAvailable() {
        return mContext.getResources().getBoolean(R.bool.config_show_trust_agent_click_intent);
    }

    @Override
    public String getPreferenceKey() {
        return PREF_KEY_TRUST_AGENT;
    }

    @Override
    public void displayPreference(PreferenceScreen screen) {
        super.displayPreference(screen);
        mSecurityCategory = screen.findPreference(PREF_KEY_SECURITY_CATEGORY);
        updateTrustAgents();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        if (savedInstanceState != null
                && savedInstanceState.containsKey(TRUST_AGENT_CLICK_INTENT)) {
            mTrustAgentClickIntent = savedInstanceState.getParcelable(TRUST_AGENT_CLICK_INTENT);
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        if (mTrustAgentClickIntent != null) {
            outState.putParcelable(TRUST_AGENT_CLICK_INTENT, mTrustAgentClickIntent);
        }
    }

    @Override
    public boolean handlePreferenceTreeClick(Preference preference) {
        if (!TextUtils.equals(preference.getKey(), getPreferenceKey())) {
            return super.handlePreferenceTreeClick(preference);
        }
        final ChooseLockSettingsHelper helper = new ChooseLockSettingsHelper(
                mHost.getActivity(), mHost);
        mTrustAgentClickIntent = preference.getIntent();
        boolean confirmationLaunched = helper.launchConfirmationActivity(
                CHANGE_TRUST_AGENT_SETTINGS, preference.getTitle());

        if (!confirmationLaunched && mTrustAgentClickIntent != null) {
            // If this returns false, it means no password confirmation is required.
            mHost.startActivity(mTrustAgentClickIntent);
            mTrustAgentClickIntent = null;
        }
        return true;
    }

    @Override
    public void onResume() {
        updateTrustAgents();
    }

    private void updateTrustAgents() {
        if (mSecurityCategory == null) {
            return;
        }
        // First remove all old trust agents.
        while (true) {
            final Preference oldAgent = mSecurityCategory.findPreference(PREF_KEY_TRUST_AGENT);
            if (oldAgent == null) {
                break;
            } else {
                mSecurityCategory.removePreference(oldAgent);
            }
        }
        // If for some reason the preference is no longer available, don't proceed to add.
        if (!isAvailable()) {
            return;
        }
        // Then add new ones.
        final boolean hasSecurity = mLockPatternUtils.isSecure(MY_USER_ID);
        final List<TrustAgentManager.TrustAgentComponentInfo> agents =
                mTrustAgentManager.getActiveTrustAgents(mContext, mLockPatternUtils);
        if (agents == null) {
            return;
        }
        for (TrustAgentManager.TrustAgentComponentInfo agent : agents) {
            final RestrictedPreference trustAgentPreference =
                    new RestrictedPreference(mSecurityCategory.getContext());
            trustAgentPreference.setKey(PREF_KEY_TRUST_AGENT);
            trustAgentPreference.setTitle(agent.title);
            trustAgentPreference.setSummary(agent.summary);
            // Create intent for this preference.
            trustAgentPreference.setIntent(new Intent(Intent.ACTION_MAIN)
                    .setComponent(agent.componentName));
            trustAgentPreference.setDisabledByAdmin(agent.admin);
            if (!trustAgentPreference.isDisabledByAdmin() && !hasSecurity) {
                trustAgentPreference.setEnabled(false);
                trustAgentPreference.setSummary(R.string.disabled_because_no_backup_security);
            }
            // Add preference to the settings menu.
            mSecurityCategory.addPreference(trustAgentPreference);
        }
    }

    public boolean handleActivityResult(int requestCode, int resultCode) {
        if (requestCode == CHANGE_TRUST_AGENT_SETTINGS && resultCode == Activity.RESULT_OK) {
            if (mTrustAgentClickIntent != null) {
                mHost.startActivity(mTrustAgentClickIntent);
                mTrustAgentClickIntent = null;
            }
            return true;
        }
        return false;
    }
}
