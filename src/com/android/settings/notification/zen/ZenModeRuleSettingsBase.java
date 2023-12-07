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

import static android.app.NotificationManager.EXTRA_AUTOMATIC_RULE_ID;

import android.app.AutomaticZenRule;
import android.app.Flags;
import android.app.NotificationManager;
import android.app.settings.SettingsEnums;
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

    protected boolean mDisableListeners;
    protected AutomaticZenRule mRule;
    protected String mId;
    private boolean mRuleRemoved;

    protected ZenAutomaticRuleHeaderPreferenceController mHeader;
    protected ZenRuleButtonsPreferenceController mActionButtons;
    protected ZenAutomaticRuleSwitchPreferenceController mSwitch;
    protected Preference mCustomBehaviorPreference;

    abstract protected void onCreateInternal();
    abstract protected boolean setRule(AutomaticZenRule rule);
    abstract protected void updateControlsInternal();

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);

        final Intent intent = getActivity().getIntent();
        if (DEBUG) Log.d(TAG, "onCreate getIntent()=" + intent);
        if (intent == null) {
            Log.w(TAG, "No intent");
            toastAndFinish();
            return;
        }

        mId = intent.getStringExtra(ConditionProviderService.EXTRA_RULE_ID);
        if (mId == null) {
            mId = intent.getStringExtra(EXTRA_AUTOMATIC_RULE_ID);
            if (mId == null) {
                Log.w(TAG, "rule id is null");
                toastAndFinish();
                return;
            }
        }

        if (DEBUG) Log.d(TAG, "mId=" + mId);
        refreshRuleOrFinish();
    }

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);

        if (isFinishingOrDestroyed()) {
            return;
        }

        mCustomBehaviorPreference = getPreferenceScreen().findPreference(CUSTOM_BEHAVIOR_KEY);
        mCustomBehaviorPreference.setOnPreferenceClickListener(
                new Preference.OnPreferenceClickListener() {
                    @Override
                    public boolean onPreferenceClick(Preference preference) {
                        Bundle bundle = new Bundle();
                        bundle.putString(ZenCustomRuleSettings.RULE_ID, mId);

                        // When modes_api flag is on, we skip the radio button screen distinguishing
                        // between "default" and "custom" and take users directly to the custom
                        // settings screen.
                        String destination = ZenCustomRuleSettings.class.getName();
                        int sourceMetricsCategory = 0;
                        if (Flags.modesApi()) {
                            // From ZenRuleCustomPolicyPreferenceController#launchCustomSettings
                            destination = ZenCustomRuleConfigSettings.class.getName();
                            sourceMetricsCategory = SettingsEnums.ZEN_CUSTOM_RULE_SOUND_SETTINGS;
                        }
                        new SubSettingLauncher(mContext)
                                .setDestination(destination)
                                .setArguments(bundle)
                                .setSourceMetricsCategory(sourceMetricsCategory)
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

        mSwitch.displayPreference(screen);
        updatePreference(mSwitch);

        mHeader.displayPreference(screen);
        updatePreference(mHeader);

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
        if (mRuleRemoved && getActivity() != null) {
            getActivity().finish();
            return true;
        }
        mRule = getZenRule();
        if (DEBUG) Log.d(TAG, "mRule=" + mRule);
        mHeader.setRule(mRule);
        mSwitch.setIdAndRule(mId, mRule);
        mActionButtons.setIdAndRule(mId, mRule);
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

    void onRuleRemoved() {
        mRuleRemoved = true;
    }
}
