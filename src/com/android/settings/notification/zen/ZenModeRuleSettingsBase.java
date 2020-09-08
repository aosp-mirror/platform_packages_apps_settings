/*
 * Copyright (C) 2015 The Android Open Source Project
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

package com.android.settings.notification.zen;

import android.app.AutomaticZenRule;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.service.notification.ConditionProviderService;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.preference.Preference;
import androidx.preference.PreferenceScreen;

import com.android.settings.R;
import com.android.settings.Utils;
import com.android.settings.core.SubSettingLauncher;

public abstract class ZenModeRuleSettingsBase extends ZenModeSettingsBase {

    protected static final String TAG = ZenModeSettingsBase.TAG;
    protected static final boolean DEBUG = ZenModeSettingsBase.DEBUG;

    private final String CUSTOM_BEHAVIOR_KEY = "zen_custom_setting";

    protected Context mContext;
    protected boolean mDisableListeners;
    protected AutomaticZenRule mRule;
    protected String mId;

    protected ZenAutomaticRuleHeaderPreferenceController mHeader;
    protected ZenRuleButtonsPreferenceController mActionButtons;
    protected ZenAutomaticRuleSwitchPreferenceController mSwitch;
    protected Preference mCustomBehaviorPreference;

    abstract protected void onCreateInternal();
    abstract protected boolean setRule(AutomaticZenRule rule);
    abstract protected void updateControlsInternal();

    @Override
    public void onCreate(Bundle icicle) {
        mContext = getActivity();

        final Intent intent = getActivity().getIntent();
        if (DEBUG) Log.d(TAG, "onCreate getIntent()=" + intent);
        if (intent == null) {
            Log.w(TAG, "No intent");
            toastAndFinish();
            return;
        }

        mId = intent.getStringExtra(ConditionProviderService.EXTRA_RULE_ID);
        if (mId == null) {
            Log.w(TAG, "rule id is null");
            toastAndFinish();
            return;
        }

        if (DEBUG) Log.d(TAG, "mId=" + mId);
        if (refreshRuleOrFinish()) {
            return;
        }

        super.onCreate(icicle);
        mCustomBehaviorPreference = getPreferenceScreen().findPreference(CUSTOM_BEHAVIOR_KEY);
        mCustomBehaviorPreference.setOnPreferenceClickListener(
                new Preference.OnPreferenceClickListener() {
                    @Override
                    public boolean onPreferenceClick(Preference preference) {
                        Bundle bundle = new Bundle();
                        bundle.putString(ZenCustomRuleSettings.RULE_ID, mId);
                        new SubSettingLauncher(mContext)
                                .setDestination(ZenCustomRuleSettings.class.getName())
                                .setArguments(bundle)
                                .setSourceMetricsCategory(0) // TODO
                                .launch();
                        return true;
                    }
                });
        onCreateInternal();
    }

    @Override
    public void onResume() {
        super.onResume();
        if (isUiRestricted()) {
            return;
        }
        if (!refreshRuleOrFinish()) {
            updateControls();
        }
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        Utils.setActionBarShadowAnimation(getActivity(), getSettingsLifecycle(), getListView());
    }

    @Override
    public int getHelpResource() {
        return R.string.help_uri_interruptions;
    }

    /**
     * Update state of header preference managed by PreferenceController.
     */
    protected void updateHeader() {
        final PreferenceScreen screen = getPreferenceScreen();

        mSwitch.onResume(mRule, mId);
        mSwitch.displayPreference(screen);
        updatePreference(mSwitch);

        mHeader.onResume(mRule, mId);
        mHeader.displayPreference(screen);
        updatePreference(mHeader);

        mActionButtons.onResume(mRule, mId);
        mActionButtons.displayPreference(screen);
        updatePreference(mActionButtons);
    }

    protected void updateRule(Uri newConditionId) {
        mRule.setConditionId(newConditionId);
        mBackend.updateZenRule(mId, mRule);
    }

    @Override
    protected void onZenModeConfigChanged() {
        super.onZenModeConfigChanged();
        if (!refreshRuleOrFinish()) {
            updateControls();
        }
    }

    private boolean refreshRuleOrFinish() {
        mRule = getZenRule();
        if (DEBUG) Log.d(TAG, "mRule=" + mRule);
        if (!setRule(mRule)) {
            toastAndFinish();
            return true;
        }
        return false;
    }

    private void toastAndFinish() {
        Toast.makeText(mContext, R.string.zen_mode_rule_not_found_text, Toast.LENGTH_SHORT)
                .show();

        getActivity().finish();
    }

    private AutomaticZenRule getZenRule() {
        return NotificationManager.from(mContext).getAutomaticZenRule(mId);
    }

    private void updateControls() {
        mDisableListeners = true;
        updateControlsInternal();
        updateHeader();
        if (mRule.getZenPolicy() == null) {
            mCustomBehaviorPreference.setSummary(R.string.zen_mode_custom_behavior_summary_default);
        } else {
            mCustomBehaviorPreference.setSummary(R.string.zen_mode_custom_behavior_summary);
        }
        mDisableListeners = false;
    }
}
