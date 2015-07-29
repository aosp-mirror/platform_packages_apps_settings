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

package com.android.settings.notification;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceScreen;
import android.provider.Settings.Global;
import android.service.notification.ZenModeConfig;
import android.service.notification.ZenModeConfig.ZenRule;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import com.android.internal.logging.MetricsLogger;
import com.android.settings.DropDownPreference;
import com.android.settings.R;
import com.android.settings.SettingsActivity;
import com.android.settings.widget.SwitchBar;

public abstract class ZenModeRuleSettingsBase extends ZenModeSettingsBase
        implements SwitchBar.OnSwitchChangeListener {
    protected static final String TAG = ZenModeSettingsBase.TAG;
    protected static final boolean DEBUG = ZenModeSettingsBase.DEBUG;

    public static final String EXTRA_RULE_ID = "rule_id";
    private static final String KEY_RULE_NAME = "rule_name";
    private static final String KEY_ZEN_MODE = "zen_mode";

    protected Context mContext;
    protected boolean mDisableListeners;
    protected ZenRule mRule;

    private String mRuleId;
    private boolean mDeleting;
    private Preference mRuleName;
    private SwitchBar mSwitchBar;
    private DropDownPreference mZenMode;
    private Toast mEnabledToast;

    abstract protected void onCreateInternal();
    abstract protected boolean setRule(ZenRule rule);
    abstract protected String getZenModeDependency();
    abstract protected void updateControlsInternal();
    abstract protected int getEnabledToastText();

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);

        mContext = getActivity();

        final Intent intent = getActivity().getIntent();
        if (DEBUG) Log.d(TAG, "onCreate getIntent()=" + intent);
        if (intent == null) {
            Log.w(TAG, "No intent");
            toastAndFinish();
            return;
        }

        mRuleId = intent.getStringExtra(EXTRA_RULE_ID);
        if (DEBUG) Log.d(TAG, "mRuleId=" + mRuleId);
        if (refreshRuleOrFinish()) {
            return;
        }

        setHasOptionsMenu(true);

        onCreateInternal();

        final PreferenceScreen root = getPreferenceScreen();
        mRuleName = root.findPreference(KEY_RULE_NAME);
        mRuleName.setOnPreferenceClickListener(new OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                showRuleNameDialog();
                return true;
            }
        });

        mZenMode = (DropDownPreference) root.findPreference(KEY_ZEN_MODE);
        mZenMode.addItem(R.string.zen_mode_option_important_interruptions,
                Global.ZEN_MODE_IMPORTANT_INTERRUPTIONS);
        mZenMode.addItem(R.string.zen_mode_option_alarms, Global.ZEN_MODE_ALARMS);
        mZenMode.addItem(R.string.zen_mode_option_no_interruptions,
                Global.ZEN_MODE_NO_INTERRUPTIONS);
        mZenMode.setCallback(new DropDownPreference.Callback() {
            @Override
            public boolean onItemSelected(int pos, Object value) {
                if (mDisableListeners) return true;
                final int zenMode = (Integer) value;
                if (zenMode == mRule.zenMode) return true;
                if (DEBUG) Log.d(TAG, "onPrefChange zenMode=" + zenMode);
                mRule.zenMode = zenMode;
                setZenModeConfig(mConfig);
                return true;
            }
        });
        mZenMode.setOrder(10);  // sort at the bottom of the category
        mZenMode.setDependency(getZenModeDependency());
    }

    @Override
    public void onResume() {
        super.onResume();
        updateControls();
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        final SettingsActivity activity = (SettingsActivity) getActivity();
        mSwitchBar = activity.getSwitchBar();
        mSwitchBar.addOnSwitchChangeListener(this);
        mSwitchBar.show();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        mSwitchBar.removeOnSwitchChangeListener(this);
        mSwitchBar.hide();
    }

    @Override
    public void onSwitchChanged(Switch switchView, boolean isChecked) {
        if (DEBUG) Log.d(TAG, "onSwitchChanged " + isChecked);
        if (mDisableListeners) return;
        final boolean enabled = isChecked;
        if (enabled == mRule.enabled) return;
        MetricsLogger.action(mContext, MetricsLogger.ACTION_ZEN_ENABLE_RULE, enabled);
        if (DEBUG) Log.d(TAG, "onSwitchChanged enabled=" + enabled);
        mRule.enabled = enabled;
        mRule.snoozing = false;
        setZenModeConfig(mConfig);
        if (enabled) {
            final int toastText = getEnabledToastText();
            if (toastText != 0) {
                mEnabledToast = Toast.makeText(mContext, toastText, Toast.LENGTH_SHORT);
                mEnabledToast.show();
            }
        } else {
            if (mEnabledToast != null) {
                mEnabledToast.cancel();
            }
        }
    }

    protected void updateRule(Uri newConditionId) {
        mRule.conditionId = newConditionId;
        mRule.condition = null;
        mRule.snoozing = false;
        setZenModeConfig(mConfig);
    }

    @Override
    protected void onZenModeChanged() {
        // noop
    }

    @Override
    protected void onZenModeConfigChanged() {
        if (!refreshRuleOrFinish()) {
            updateControls();
        }
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        if (DEBUG) Log.d(TAG, "onCreateOptionsMenu");
        inflater.inflate(R.menu.zen_mode_rule, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (DEBUG) Log.d(TAG, "onOptionsItemSelected " + item.getItemId());
        if (item.getItemId() == R.id.delete) {
            MetricsLogger.action(mContext, MetricsLogger.ACTION_ZEN_DELETE_RULE);
            showDeleteRuleDialog();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void showRuleNameDialog() {
        new ZenRuleNameDialog(mContext, null, mRule.name, mConfig.getAutomaticRuleNames()) {
            @Override
            public void onOk(String ruleName, RuleInfo type) {
                final ZenModeConfig newConfig = mConfig.copy();
                final ZenRule rule = newConfig.automaticRules.get(mRuleId);
                if (rule == null) return;
                rule.name = ruleName;
                setZenModeConfig(newConfig);
            }
        }.show();
    }

    private boolean refreshRuleOrFinish() {
        mRule = mConfig.automaticRules.get(mRuleId);
        if (DEBUG) Log.d(TAG, "mRule=" + mRule);
        if (!setRule(mRule)) {
            toastAndFinish();
            return true;
        }
        return false;
    }

    private void showDeleteRuleDialog() {
        final AlertDialog dialog = new AlertDialog.Builder(mContext)
                .setMessage(getString(R.string.zen_mode_delete_rule_confirmation, mRule.name))
                .setNegativeButton(R.string.cancel, null)
                .setPositiveButton(R.string.zen_mode_delete_rule_button, new OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        MetricsLogger.action(mContext, MetricsLogger.ACTION_ZEN_DELETE_RULE_OK);
                        mDeleting = true;
                        mConfig.automaticRules.remove(mRuleId);
                        setZenModeConfig(mConfig);
                    }
                })
                .show();
        final View messageView = dialog.findViewById(android.R.id.message);
        if (messageView != null) {
            messageView.setTextDirection(View.TEXT_DIRECTION_LOCALE);
        }
    }

    private void toastAndFinish() {
        if (!mDeleting) {
            Toast.makeText(mContext, R.string.zen_mode_rule_not_found_text, Toast.LENGTH_SHORT)
                    .show();
        }
        getActivity().finish();
    }

    private void updateRuleName() {
        getActivity().setTitle(mRule.name);
        mRuleName.setSummary(mRule.name);
    }

    private void updateControls() {
        mDisableListeners = true;
        updateRuleName();
        updateControlsInternal();
        mZenMode.setSelectedValue(mRule.zenMode);
        if (mSwitchBar != null) {
            mSwitchBar.setChecked(mRule.enabled);
        }
        mDisableListeners = false;
    }

}
