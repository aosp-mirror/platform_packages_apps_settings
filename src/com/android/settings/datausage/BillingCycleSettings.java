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

import static android.net.NetworkPolicy.LIMIT_DISABLED;
import static android.net.NetworkPolicy.WARNING_DISABLED;

import android.app.Dialog;
import android.app.settings.SettingsEnums;
import android.content.Context;
import android.content.DialogInterface;
import android.content.res.Resources;
import android.net.NetworkPolicy;
import android.net.NetworkTemplate;
import android.os.Bundle;
import android.text.method.NumberKeyListener;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.NumberPicker;
import android.widget.Spinner;

import androidx.annotation.VisibleForTesting;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.preference.Preference;
import androidx.preference.SwitchPreference;

import com.android.settings.R;
import com.android.settings.core.instrumentation.InstrumentedDialogFragment;
import com.android.settings.search.BaseSearchIndexProvider;
import com.android.settingslib.NetworkPolicyEditor;
import com.android.settingslib.net.DataUsageController;
import com.android.settingslib.search.SearchIndexable;

import java.text.NumberFormat;
import java.text.ParseException;
import java.util.TimeZone;

@SearchIndexable
public class BillingCycleSettings extends DataUsageBaseFragment implements
        Preference.OnPreferenceChangeListener, DataUsageEditController {

    private static final String TAG = "BillingCycleSettings";
    private static final boolean LOGD = false;
    public static final long MIB_IN_BYTES = 1024 * 1024;
    public static final long GIB_IN_BYTES = MIB_IN_BYTES * 1024;

    private static final long MAX_DATA_LIMIT_BYTES = 50000 * GIB_IN_BYTES;

    private static final String TAG_CONFIRM_LIMIT = "confirmLimit";
    private static final String TAG_CYCLE_EDITOR = "cycleEditor";
    private static final String TAG_WARNING_EDITOR = "warningEditor";

    private static final String KEY_BILLING_CYCLE = "billing_cycle";
    private static final String KEY_SET_DATA_WARNING = "set_data_warning";
    private static final String KEY_DATA_WARNING = "data_warning";
    @VisibleForTesting
    static final String KEY_SET_DATA_LIMIT = "set_data_limit";
    private static final String KEY_DATA_LIMIT = "data_limit";

    @VisibleForTesting
    NetworkTemplate mNetworkTemplate;
    private Preference mBillingCycle;
    private Preference mDataWarning;
    private SwitchPreference mEnableDataWarning;
    private SwitchPreference mEnableDataLimit;
    private Preference mDataLimit;
    private DataUsageController mDataUsageController;

    @VisibleForTesting
    void setUpForTest(NetworkPolicyEditor policyEditor,
            Preference billingCycle,
            Preference dataLimit,
            Preference dataWarning,
            SwitchPreference enableLimit,
            SwitchPreference enableWarning) {
        services.mPolicyEditor = policyEditor;
        mBillingCycle = billingCycle;
        mDataLimit = dataLimit;
        mDataWarning = dataWarning;
        mEnableDataLimit = enableLimit;
        mEnableDataWarning = enableWarning;
    }

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);

        final Context context = getContext();
        mDataUsageController = new DataUsageController(context);

        Bundle args = getArguments();
        mNetworkTemplate = args.getParcelable(DataUsageList.EXTRA_NETWORK_TEMPLATE);
        if (mNetworkTemplate == null) {
            mNetworkTemplate = DataUsageUtils.getDefaultTemplate(context,
                DataUsageUtils.getDefaultSubscriptionId(context));
        }

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

    @VisibleForTesting
    void updatePrefs() {
        mBillingCycle.setSummary(null);
        final long warningBytes = services.mPolicyEditor.getPolicyWarningBytes(mNetworkTemplate);
        if (warningBytes != WARNING_DISABLED) {
            mDataWarning.setSummary(DataUsageUtils.formatDataUsage(getContext(), warningBytes));
            mDataWarning.setEnabled(true);
            mEnableDataWarning.setChecked(true);
        } else {
            mDataWarning.setSummary(null);
            mDataWarning.setEnabled(false);
            mEnableDataWarning.setChecked(false);
        }
        final long limitBytes = services.mPolicyEditor.getPolicyLimitBytes(mNetworkTemplate);
        if (limitBytes != LIMIT_DISABLED) {
            mDataLimit.setSummary(DataUsageUtils.formatDataUsage(getContext(), limitBytes));
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
            writePreferenceClickMetric(preference);
            CycleEditorFragment.show(this);
            return true;
        } else if (preference == mDataWarning) {
            writePreferenceClickMetric(preference);
            BytesEditorFragment.show(this, false);
            return true;
        } else if (preference == mDataLimit) {
            writePreferenceClickMetric(preference);
            BytesEditorFragment.show(this, true);
            return true;
        }
        return super.onPreferenceTreeClick(preference);
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        if (mEnableDataLimit == preference) {
            boolean enabled = (Boolean) newValue;
            if (!enabled) {
                setPolicyLimitBytes(LIMIT_DISABLED);
                return true;
            }
            ConfirmLimitFragment.show(this);
            // This preference is enabled / disabled by ConfirmLimitFragment.
            return false;
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
    public int getMetricsCategory() {
        return SettingsEnums.BILLING_CYCLE;
    }

    @Override
    protected int getPreferenceScreenResId() {
        return R.xml.billing_cycle;
    }

    @Override
    protected String getLogTag() {
        return TAG;
    }

    @VisibleForTesting
    void setPolicyLimitBytes(long limitBytes) {
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
    public static class BytesEditorFragment extends InstrumentedDialogFragment
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

            bytesPicker.setKeyListener(new NumberKeyListener() {
                protected char[] getAcceptedChars() {
                    return new char [] {'0', '1', '2', '3', '4', '5', '6', '7', '8', '9',
                            ',', '.'};
                }
                public int getInputType() {
                    return EditorInfo.TYPE_CLASS_NUMBER | EditorInfo.TYPE_NUMBER_FLAG_DECIMAL;
                }
            });

            final NetworkTemplate template = getArguments().getParcelable(EXTRA_TEMPLATE);
            final boolean isLimit = getArguments().getBoolean(EXTRA_LIMIT);
            final long bytes = isLimit ? editor.getPolicyLimitBytes(template)
                    : editor.getPolicyWarningBytes(template);
            final long limitDisabled = isLimit ? LIMIT_DISABLED : WARNING_DISABLED;

            final boolean unitInGigaBytes = (bytes > 1.5f * GIB_IN_BYTES);
            final String bytesText = formatText(bytes,
                    unitInGigaBytes ? GIB_IN_BYTES : MIB_IN_BYTES);
            bytesPicker.setText(bytesText);
            bytesPicker.setSelection(0, bytesText.length());

            type.setSelection(unitInGigaBytes ? 1 : 0);
        }

        private String formatText(double v, double unitInBytes) {
            final NumberFormat formatter = NumberFormat.getNumberInstance();
            formatter.setMaximumFractionDigits(2);
            return formatter.format((double) (v / unitInBytes));
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
            final EditText bytesField = (EditText) mView.findViewById(R.id.bytes);
            final Spinner spinner = (Spinner) mView.findViewById(R.id.size_spinner);

            final String bytesString = bytesField.getText().toString();

            final NumberFormat formatter = NumberFormat.getNumberInstance();
            Number number = null;
            try {
                number = formatter.parse(bytesString);
            } catch (ParseException ex) {
            }
            long bytes = 0L;
            if (number != null) {
                bytes = (long) (number.floatValue()
                        * (spinner.getSelectedItemPosition() == 0 ? MIB_IN_BYTES : GIB_IN_BYTES));
            }

            // to fix the overflow problem
            final long correctedBytes = Math.min(MAX_DATA_LIMIT_BYTES, bytes);
            if (isLimit) {
                editor.setPolicyLimitBytes(template, correctedBytes);
            } else {
                editor.setPolicyWarningBytes(template, correctedBytes);
            }
            target.updateDataUsage();
        }

        @Override
        public int getMetricsCategory() {
            return SettingsEnums.DIALOG_BILLING_BYTE_LIMIT;
        }
    }

    /**
     * Dialog to edit {@link NetworkPolicy}.
     */
    public static class CycleEditorFragment extends InstrumentedDialogFragment implements
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
        public int getMetricsCategory() {
            return SettingsEnums.DIALOG_BILLING_CYCLE;
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
            final String cycleTimezone = TimeZone.getDefault().getID();
            editor.setPolicyCycleDay(template, cycleDay, cycleTimezone);
            target.updateDataUsage();
        }
    }

    /**
     * Dialog to request user confirmation before setting
     * {@link NetworkPolicy#limitBytes}.
     */
    public static class ConfirmLimitFragment extends InstrumentedDialogFragment implements
            DialogInterface.OnClickListener {
        @VisibleForTesting
        static final String EXTRA_LIMIT_BYTES = "limitBytes";
        public static final float FLOAT = 1.2f;

        public static void show(BillingCycleSettings parent) {
            if (!parent.isAdded()) return;

            final NetworkPolicy policy = parent.services.mPolicyEditor
                    .getPolicy(parent.mNetworkTemplate);
            if (policy == null) return;

            final Resources res = parent.getResources();
            final long minLimitBytes = (long) (policy.warningBytes * FLOAT);
            final long limitBytes;

            // TODO: customize default limits based on network template
            limitBytes = Math.max(5 * GIB_IN_BYTES, minLimitBytes);

            final Bundle args = new Bundle();
            args.putLong(EXTRA_LIMIT_BYTES, limitBytes);

            final ConfirmLimitFragment dialog = new ConfirmLimitFragment();
            dialog.setArguments(args);
            dialog.setTargetFragment(parent, 0);
            dialog.show(parent.getFragmentManager(), TAG_CONFIRM_LIMIT);
        }

        @Override
        public int getMetricsCategory() {
            return SettingsEnums.DIALOG_BILLING_CONFIRM_LIMIT;
        }

        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            final Context context = getActivity();

            return new AlertDialog.Builder(context)
                    .setTitle(R.string.data_usage_limit_dialog_title)
                    .setMessage(R.string.data_usage_limit_dialog_mobile)
                    .setPositiveButton(android.R.string.ok, this)
                    .setNegativeButton(android.R.string.cancel, null)
                    .create();
        }

        @Override
        public void onClick(DialogInterface dialog, int which) {
            final BillingCycleSettings target = (BillingCycleSettings) getTargetFragment();
            if (which != DialogInterface.BUTTON_POSITIVE) return;
            final long limitBytes = getArguments().getLong(EXTRA_LIMIT_BYTES);
            if (target != null) {
                target.setPolicyLimitBytes(limitBytes);
            }
            target.getPreferenceManager().getSharedPreferences().edit()
                    .putBoolean(KEY_SET_DATA_LIMIT, true).apply();
        }
    }

    public static final BaseSearchIndexProvider SEARCH_INDEX_DATA_PROVIDER =
            new BaseSearchIndexProvider(R.xml.billing_cycle) {

                @Override
                protected boolean isPageSearchEnabled(Context context) {
                    return DataUsageUtils.hasMobileData(context);
                }
            };

}
