/*
 * Copyright (C) 2016 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the
 * License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the specific language governing
 * permissions and limitations under the License.
 */

package com.android.settings.datausage;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.app.Fragment;
import android.content.Context;
import android.content.DialogInterface;
import android.content.res.Resources;
import android.net.NetworkPolicy;
import android.net.NetworkTemplate;
import android.os.Bundle;
import android.support.v14.preference.SwitchPreference;
import android.support.v7.preference.Preference;
import android.text.format.Formatter;
import android.text.format.Time;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.NumberPicker;
import android.widget.Spinner;

import com.android.internal.logging.MetricsProto.MetricsEvent;
import com.android.settings.R;
import com.android.settingslib.NetworkPolicyEditor;
import com.android.settingslib.net.DataUsageController;

import static android.net.NetworkPolicy.LIMIT_DISABLED;
import static android.net.NetworkPolicy.WARNING_DISABLED;
import static android.net.TrafficStats.GB_IN_BYTES;
import static android.net.TrafficStats.MB_IN_BYTES;

public class BillingCycleSettings extends DataUsageBase implements
        Preference.OnPreferenceChangeListener, DataUsageEditController {

    private static final String TAG = "BillingCycleSettings";
    private static final boolean LOGD = false;

    private static final String TAG_CONFIRM_LIMIT = "confirmLimit";
    private static final String TAG_CYCLE_EDITOR = "cycleEditor";
    private static final String TAG_WARNING_EDITOR = "warningEditor";

    private static final String KEY_BILLING_CYCLE = "billing_cycle";
    private static final String KEY_SET_DATA_WARNING = "set_data_warning";
    private static final String KEY_DATA_WARNING = "data_warning";
    private static final String KEY_SET_DATA_LIMIT = "set_data_limit";
    private static final String KEY_DATA_LIMIT = "data_limit";

    private NetworkTemplate mNetworkTemplate;
    private Preference mBillingCycle;
    private Preference mDataWarning;
    private SwitchPreference mEnableDataWarning;
    private SwitchPreference mEnableDataLimit;
    private Preference mDataLimit;
    private DataUsageController mDataUsageController;

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);

        mDataUsageController = new DataUsageController(getContext());

        Bundle args = getArguments();
        mNetworkTemplate = args.getParcelable(DataUsageList.EXTRA_NETWORK_TEMPLATE);

        addPreferencesFromResource(R.xml.billing_cycle);
        mBillingCycle = findPreference(KEY_BILLING_CYCLE);
        mEnableDataWarning = (SwitchPreference) findPreference(KEY_SET_DATA_WARNING);
        mEnableDataWarning.setOnPreferenceChangeListener(this);
        mDataWarning = findPreference(KEY_DATA_WARNING);
        mEnableDataLimit = (SwitchPreference) findPreference(KEY_SET_DATA_LIMIT);
        mEnableDataLimit.setOnPreferenceChangeListener(this);
        mDataLimit = findPreference(KEY_DATA_LIMIT);
    }

    @Override
    public void onResume() {
        super.onResume();
        updatePrefs();
    }

    private void updatePrefs() {
        NetworkPolicy policy = services.mPolicyEditor.getPolicy(mNetworkTemplate);
        mBillingCycle.setSummary(getString(R.string.billing_cycle_summary, policy != null ?
                policy.cycleDay : 1));
        if (policy != null && policy.warningBytes != WARNING_DISABLED) {
            mDataWarning.setSummary(Formatter.formatFileSize(getContext(), policy.warningBytes));
            mDataWarning.setEnabled(true);
            mEnableDataWarning.setChecked(true);
        } else {
            mDataWarning.setSummary(null);
            mDataWarning.setEnabled(false);
            mEnableDataWarning.setChecked(false);
        }
        if (policy != null && policy.limitBytes != LIMIT_DISABLED) {
            mDataLimit.setSummary(Formatter.formatFileSize(getContext(), policy.limitBytes));
            mDataLimit.setEnabled(true);
            mEnableDataLimit.setChecked(true);
        } else {
            mDataLimit.setSummary(null);
            mDataLimit.setEnabled(false);
            mEnableDataLimit.setChecked(false);
        }
    }

    @Override
    public boolean onPreferenceTreeClick(Preference preference) {
        if (preference == mBillingCycle) {
            CycleEditorFragment.show(this);
            return true;
        } else if (preference == mDataWarning) {
            BytesEditorFragment.show(this, false);
            return true;
        } else if (preference == mDataLimit) {
            BytesEditorFragment.show(this, true);
            return true;
        }
        return super.onPreferenceTreeClick(preference);
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        if (mEnableDataLimit == preference) {
            boolean enabled = (Boolean) newValue;
            if (enabled) {
                ConfirmLimitFragment.show(this);
            } else {
                setPolicyLimitBytes(LIMIT_DISABLED);
            }
            return true;
        } else if (mEnableDataWarning == preference) {
            boolean enabled = (Boolean) newValue;
            if (enabled) {
                setPolicyWarningBytes(mDataUsageController.getDefaultWarningLevel());
            } else {
                setPolicyWarningBytes(WARNING_DISABLED);
            }
            return true;
        }
        return false;
    }

    @Override
    protected int getMetricsCategory() {
        return MetricsEvent.BILLING_CYCLE;
    }

    private void setPolicyLimitBytes(long limitBytes) {
        if (LOGD) Log.d(TAG, "setPolicyLimitBytes()");
        services.mPolicyEditor.setPolicyLimitBytes(mNetworkTemplate, limitBytes);
        updatePrefs();
    }

    private void setPolicyWarningBytes(long warningBytes) {
        if (LOGD) Log.d(TAG, "setPolicyWarningBytes()");
        services.mPolicyEditor.setPolicyWarningBytes(mNetworkTemplate, warningBytes);
        updatePrefs();
    }

    @Override
    public NetworkPolicyEditor getNetworkPolicyEditor() {
        return services.mPolicyEditor;
    }

    @Override
    public NetworkTemplate getNetworkTemplate() {
        return mNetworkTemplate;
    }

    @Override
    public void updateDataUsage() {
        updatePrefs();
    }

    /**
     * Dialog to edit {@link NetworkPolicy#warningBytes}.
     */
    public static class BytesEditorFragment extends DialogFragment
            implements DialogInterface.OnClickListener {
        private static final String EXTRA_TEMPLATE = "template";
        private static final String EXTRA_LIMIT = "limit";
        private View mView;

        public static void show(DataUsageEditController parent, boolean isLimit) {
            if (!(parent instanceof Fragment)) {
                return;
            }
            Fragment targetFragment = (Fragment) parent;
            if (!targetFragment.isAdded()) {
                return;
            }

            final Bundle args = new Bundle();
            args.putParcelable(EXTRA_TEMPLATE, parent.getNetworkTemplate());
            args.putBoolean(EXTRA_LIMIT, isLimit);

            final BytesEditorFragment dialog = new BytesEditorFragment();
            dialog.setArguments(args);
            dialog.setTargetFragment(targetFragment, 0);
            dialog.show(targetFragment.getFragmentManager(), TAG_WARNING_EDITOR);
        }

        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            final Context context = getActivity();
            final LayoutInflater dialogInflater = LayoutInflater.from(context);
            final boolean isLimit = getArguments().getBoolean(EXTRA_LIMIT);
            mView = dialogInflater.inflate(R.layout.data_usage_bytes_editor, null, false);
            setupPicker((EditText) mView.findViewById(R.id.bytes),
                    (Spinner) mView.findViewById(R.id.size_spinner));
            return new AlertDialog.Builder(context)
                    .setTitle(isLimit ? R.string.data_usage_limit_editor_title
                            : R.string.data_usage_warning_editor_title)
                    .setView(mView)
                    .setPositiveButton(R.string.data_usage_cycle_editor_positive, this)
                    .create();
        }

        private void setupPicker(EditText bytesPicker, Spinner type) {
            final DataUsageEditController target = (DataUsageEditController) getTargetFragment();
            final NetworkPolicyEditor editor = target.getNetworkPolicyEditor();

            final NetworkTemplate template = getArguments().getParcelable(EXTRA_TEMPLATE);
            final boolean isLimit = getArguments().getBoolean(EXTRA_LIMIT);
            final long bytes = isLimit ? editor.getPolicyLimitBytes(template)
                    : editor.getPolicyWarningBytes(template);
            final long limitDisabled = isLimit ? LIMIT_DISABLED : WARNING_DISABLED;

            if (bytes > 1.5f * GB_IN_BYTES) {
                final String bytesText = formatText(bytes / (float) GB_IN_BYTES);
                bytesPicker.setText(bytesText);
                bytesPicker.setSelection(0, bytesText.length());

                type.setSelection(1);
            } else {
                final String bytesText = formatText(bytes / (float) MB_IN_BYTES);
                bytesPicker.setText(bytesText);
                bytesPicker.setSelection(0, bytesText.length());

                type.setSelection(0);
            }
        }

        private String formatText(float v) {
            v = Math.round(v * 100) / 100f;
            return String.valueOf(v);
        }

        @Override
        public void onClick(DialogInterface dialog, int which) {
            if (which != DialogInterface.BUTTON_POSITIVE) {
                return;
            }
            final DataUsageEditController target = (DataUsageEditController) getTargetFragment();
            final NetworkPolicyEditor editor = target.getNetworkPolicyEditor();

            final NetworkTemplate template = getArguments().getParcelable(EXTRA_TEMPLATE);
            final boolean isLimit = getArguments().getBoolean(EXTRA_LIMIT);
            EditText bytesField = (EditText) mView.findViewById(R.id.bytes);
            Spinner spinner = (Spinner) mView.findViewById(R.id.size_spinner);

            String bytesString = bytesField.getText().toString();
            if (bytesString.isEmpty()) {
                bytesString = "0";
            }
            final long bytes = (long) (Float.valueOf(bytesString)
                    * (spinner.getSelectedItemPosition() == 0 ? MB_IN_BYTES : GB_IN_BYTES));
            if (isLimit) {
                editor.setPolicyLimitBytes(template, bytes);
            } else {
                editor.setPolicyWarningBytes(template, bytes);
            }
            target.updateDataUsage();
        }
    }

    /**
     * Dialog to edit {@link NetworkPolicy#cycleDay}.
     */
    public static class CycleEditorFragment extends DialogFragment implements
            DialogInterface.OnClickListener {
        private static final String EXTRA_TEMPLATE = "template";
        private NumberPicker mCycleDayPicker;

        public static void show(BillingCycleSettings parent) {
            if (!parent.isAdded()) return;

            final Bundle args = new Bundle();
            args.putParcelable(EXTRA_TEMPLATE, parent.mNetworkTemplate);

            final CycleEditorFragment dialog = new CycleEditorFragment();
            dialog.setArguments(args);
            dialog.setTargetFragment(parent, 0);
            dialog.show(parent.getFragmentManager(), TAG_CYCLE_EDITOR);
        }

        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            final Context context = getActivity();
            final DataUsageEditController target = (DataUsageEditController) getTargetFragment();
            final NetworkPolicyEditor editor = target.getNetworkPolicyEditor();

            final AlertDialog.Builder builder = new AlertDialog.Builder(context);
            final LayoutInflater dialogInflater = LayoutInflater.from(builder.getContext());

            final View view = dialogInflater.inflate(R.layout.data_usage_cycle_editor, null, false);
            mCycleDayPicker = (NumberPicker) view.findViewById(R.id.cycle_day);

            final NetworkTemplate template = getArguments().getParcelable(EXTRA_TEMPLATE);
            final int cycleDay = editor.getPolicyCycleDay(template);

            mCycleDayPicker.setMinValue(1);
            mCycleDayPicker.setMaxValue(31);
            mCycleDayPicker.setValue(cycleDay);
            mCycleDayPicker.setWrapSelectorWheel(true);

            return builder.setTitle(R.string.data_usage_cycle_editor_title)
                    .setView(view)
                    .setPositiveButton(R.string.data_usage_cycle_editor_positive, this)
                    .create();
        }

        @Override
        public void onClick(DialogInterface dialog, int which) {
            final NetworkTemplate template = getArguments().getParcelable(EXTRA_TEMPLATE);
            final DataUsageEditController target = (DataUsageEditController) getTargetFragment();
            final NetworkPolicyEditor editor = target.getNetworkPolicyEditor();

            // clear focus to finish pending text edits
            mCycleDayPicker.clearFocus();

            final int cycleDay = mCycleDayPicker.getValue();
            final String cycleTimezone = new Time().timezone;
            editor.setPolicyCycleDay(template, cycleDay, cycleTimezone);
            target.updateDataUsage();
        }
    }

    /**
     * Dialog to request user confirmation before setting
     * {@link NetworkPolicy#limitBytes}.
     */
    public static class ConfirmLimitFragment extends DialogFragment implements
            DialogInterface.OnClickListener {
        private static final String EXTRA_MESSAGE = "message";
        private static final String EXTRA_LIMIT_BYTES = "limitBytes";
        public static final float FLOAT = 1.2f;

        public static void show(BillingCycleSettings parent) {
            if (!parent.isAdded()) return;

            final NetworkPolicy policy = parent.services.mPolicyEditor
                    .getPolicy(parent.mNetworkTemplate);
            if (policy == null) return;

            final Resources res = parent.getResources();
            final CharSequence message;
            final long minLimitBytes = (long) (policy.warningBytes * FLOAT);
            final long limitBytes;

            // TODO: customize default limits based on network template
            message = res.getString(R.string.data_usage_limit_dialog_mobile);
            limitBytes = Math.max(5 * GB_IN_BYTES, minLimitBytes);

            final Bundle args = new Bundle();
            args.putCharSequence(EXTRA_MESSAGE, message);
            args.putLong(EXTRA_LIMIT_BYTES, limitBytes);

            final ConfirmLimitFragment dialog = new ConfirmLimitFragment();
            dialog.setArguments(args);
            dialog.setTargetFragment(parent, 0);
            dialog.show(parent.getFragmentManager(), TAG_CONFIRM_LIMIT);
        }

        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            final Context context = getActivity();

            final CharSequence message = getArguments().getCharSequence(EXTRA_MESSAGE);

            return new AlertDialog.Builder(context)
                    .setTitle(R.string.data_usage_limit_dialog_title)
                    .setMessage(message)
                    .setPositiveButton(android.R.string.ok, this)
                    .setNegativeButton(android.R.string.cancel, null)
                    .create();
        }

        @Override
        public void onClick(DialogInterface dialog, int which) {
            if (which != DialogInterface.BUTTON_POSITIVE) return;
            final long limitBytes = getArguments().getLong(EXTRA_LIMIT_BYTES);
            final BillingCycleSettings target = (BillingCycleSettings) getTargetFragment();
            if (target != null) {
                target.setPolicyLimitBytes(limitBytes);
            }
        }
    }
}
