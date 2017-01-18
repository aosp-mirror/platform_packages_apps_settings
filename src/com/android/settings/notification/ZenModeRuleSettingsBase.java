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
import android.app.AutomaticZenRule;
import android.app.NotificationManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.service.notification.ConditionProviderService;
import android.support.v7.preference.DropDownPreference;
import android.support.v7.preference.Preference;
import android.support.v7.preference.Preference.OnPreferenceChangeListener;
import android.support.v7.preference.Preference.OnPreferenceClickListener;
import android.support.v7.preference.PreferenceScreen;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Switch;
import android.widget.Toast;

import com.android.internal.logging.MetricsLogger;
import com.android.internal.logging.MetricsProto.MetricsEvent;
import com.android.settings.R;
import com.android.settings.SettingsActivity;
import com.android.settings.widget.SwitchBar;

public abstract class ZenModeRuleSettingsBase extends ZenModeSettingsBase
        implements SwitchBar.OnSwitchChangeListener {
    protected static final String TAG = ZenModeSettingsBase.TAG;
    protected static final boolean DEBUG = ZenModeSettingsBase.DEBUG;

    private static final String KEY_RULE_NAME = "rule_name";
    private static final String KEY_ZEN_MODE = "zen_mode";

    protected Context mContext;
    protected boolean mDisableListeners;
    protected AutomaticZenRule mRule;
    protected String mId;

    private boolean mDeleting;
    private Preference mRuleName;
    private SwitchBar mSwitchBar;
    private DropDownPreference mZenMode;
    private Toast mEnabledToast;

    abstract protected void onCreateInternal();
    abstract protected boolean setRule(AutomaticZenRule rule);
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

        mId = intent.getStringExtra(ConditionProviderService.EXTRA_RULE_ID);
        if (DEBUG) Log.d(TAG, "mId=" + mId);
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
        mZenMode.setEntries(new CharSequence[] {
                getString(R.string.zen_mode_option_important_interruptions),
                getString(R.string.zen_mode_option_alarms),
                getString(R.string.zen_mode_option_no_interruptions),
        });
        mZenMode.setEntryValues(new CharSequence[] {
                Integer.toString(NotificationManager.INTERRUPTION_FILTER_PRIORITY),
                Integer.toString(NotificationManager.INTERRUPTION_FILTER_ALARMS),
                Integer.toString(NotificationManager.INTERRUPTION_FILTER_NONE),
        });
        mZenMode.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                if (mDisableListeners) return false;
                final int zenMode = Integer.parseInt((String) newValue);
                if (zenMode == mRule.getInterruptionFilter()) return false;
                if (DEBUG) Log.d(TAG, "onPrefChange zenMode=" + zenMode);
                mRule.setInterruptionFilter(zenMode);
                setZenRule(mId, mRule);
                return true;
            }
        });
        mZenMode.setOrder(10);  // sort at the bottom of the category
        mZenMode.setDependency(getZenModeDependency());
    }

    @Override
    public void onResume() {
        super.onResume();
        if (isUiRestricted()) {
            return;
        }
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
        if (enabled == mRule.isEnabled()) return;
        MetricsLogger.action(mContext, MetricsEvent.ACTION_ZEN_ENABLE_RULE, enabled);
        if (DEBUG) Log.d(TAG, "onSwitchChanged enabled=" + enabled);
        mRule.setEnabled(enabled);
        setZenRule(mId, mRule);
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
        mRule.setConditionId(newConditionId);
        setZenRule(mId, mRule);
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
            MetricsLogger.action(mContext, MetricsEvent.ACTION_ZEN_DELETE_RULE);
            showDeleteRuleDialog();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void showRuleNameDialog() {
        new ZenRuleNameDialog(mContext, mRule.getName()) {
            @Override
            public void onOk(String ruleName) {
                mRule.setName(ruleName);
                setZenRule(mId, mRule);
            }
        }.show();
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

    private void showDeleteRuleDialog() {
        final AlertDialog dialog = new AlertDialog.Builder(mContext)
                .setMessage(getString(R.string.zen_mode_delete_rule_confirmation, mRule.getName()))
                .setNegativeButton(R.string.cancel, null)
                .setPositiveButton(R.string.zen_mode_delete_rule_button, new OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        MetricsLogger.action(mContext, MetricsEvent.ACTION_ZEN_DELETE_RULE_OK);
                        mDeleting = true;
                        removeZenRule(mId);
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
        getActivity().setTitle(mRule.getName());
        mRuleName.setSummary(mRule.getName());
    }

    private AutomaticZenRule getZenRule() {
        return NotificationManager.from(mContext).getAutomaticZenRule(mId);
    }

    private void updateControls() {
        mDisableListeners = true;
        updateRuleName();
        updateControlsInternal();
        mZenMode.setValue(Integer.toString(mRule.getInterruptionFilter()));
        if (mSwitchBar != null) {
            mSwitchBar.setChecked(mRule.isEnabled());
        }
        mDisableListeners = false;
    }

}
