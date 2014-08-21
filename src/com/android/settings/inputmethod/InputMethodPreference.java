/*
 * Copyright (C) 2011 The Android Open Source Project
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

package com.android.settings.inputmethod;

import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.SwitchPreference;
import android.text.TextUtils;
import android.util.Log;
import android.view.inputmethod.InputMethodInfo;
import android.view.inputmethod.InputMethodManager;
import android.view.inputmethod.InputMethodSubtype;
import android.widget.Toast;

import com.android.internal.inputmethod.InputMethodUtils;
import com.android.settings.R;

import java.text.Collator;
import java.util.ArrayList;
import java.util.List;

/**
 * Input method preference.
 *
 * This preference represents an IME. It is used for two purposes. 1) An instance with a switch
 * is used to enable or disable the IME. 2) An instance without a switch is used to invoke the
 * setting activity of the IME.
 */
class InputMethodPreference extends SwitchPreference implements OnPreferenceClickListener,
        OnPreferenceChangeListener {
    private static final String TAG = InputMethodPreference.class.getSimpleName();
    private static final String EMPTY_TEXT = "";

    interface OnSavePreferenceListener {
        /**
         * Called when this preference needs to be saved its state.
         *
         * Note that this preference is non-persistent and needs explicitly to be saved its state.
         * Because changing one IME state may change other IMEs' state, this is a place to update
         * other IMEs' state as well.
         *
         * @param pref This preference.
         */
        public void onSaveInputMethodPreference(InputMethodPreference pref);
    }

    private final InputMethodInfo mImi;
    private final boolean mHasPriorityInSorting;
    private final OnSavePreferenceListener mOnSaveListener;
    private final InputMethodSettingValuesWrapper mInputMethodSettingValues;
    private final boolean mIsAllowedByOrganization;

    private AlertDialog mDialog = null;

    /**
     * A preference entry of an input method.
     *
     * @param context The Context this is associated with.
     * @param imi The {@link InputMethodInfo} of this preference.
     * @param isImeEnabler true if this preference is the IME enabler that has enable/disable
     *     switches for all available IMEs, not the list of enabled IMEs.
     * @param isAllowedByOrganization false if the IME has been disabled by a device or profile
           owner.
     * @param onSaveListener The listener called when this preference has been changed and needs
     *     to save the state to shared preference.
     */
    InputMethodPreference(final Context context, final InputMethodInfo imi,
            final boolean isImeEnabler, final boolean isAllowedByOrganization,
            final OnSavePreferenceListener onSaveListener) {
        super(context);
        setPersistent(false);
        mImi = imi;
        mIsAllowedByOrganization = isAllowedByOrganization;
        mOnSaveListener = onSaveListener;
        if (!isImeEnabler) {
            // Hide switch widget.
            setWidgetLayoutResource(0 /* widgetLayoutResId */);
        }
        // Disable on/off switch texts.
        setSwitchTextOn(EMPTY_TEXT);
        setSwitchTextOff(EMPTY_TEXT);
        setKey(imi.getId());
        setTitle(imi.loadLabel(context.getPackageManager()));
        final String settingsActivity = imi.getSettingsActivity();
        if (TextUtils.isEmpty(settingsActivity)) {
            setIntent(null);
        } else {
            // Set an intent to invoke settings activity of an input method.
            final Intent intent = new Intent(Intent.ACTION_MAIN);
            intent.setClassName(imi.getPackageName(), settingsActivity);
            setIntent(intent);
        }
        mInputMethodSettingValues = InputMethodSettingValuesWrapper.getInstance(context);
        mHasPriorityInSorting = InputMethodUtils.isSystemIme(imi)
                && mInputMethodSettingValues.isValidSystemNonAuxAsciiCapableIme(imi, context);
        setOnPreferenceClickListener(this);
        setOnPreferenceChangeListener(this);
    }

    public InputMethodInfo getInputMethodInfo() {
        return mImi;
    }

    private boolean isImeEnabler() {
        // If this {@link SwitchPreference} doesn't have a widget layout, we explicitly hide the
        // switch widget at constructor.
        return getWidgetLayoutResource() != 0;
    }

    @Override
    public boolean onPreferenceChange(final Preference preference, final Object newValue) {
        // Always returns false to prevent default behavior.
        // See {@link TwoStatePreference#onClick()}.
        if (!isImeEnabler()) {
            // Prevent disabling an IME because this preference is for invoking a settings activity.
            return false;
        }
        if (isChecked()) {
            // Disable this IME.
            setChecked(false);
            mOnSaveListener.onSaveInputMethodPreference(this);
            return false;
        }
        if (InputMethodUtils.isSystemIme(mImi)) {
            // Enable a system IME. No need to show a security warning dialog.
            setChecked(true);
            mOnSaveListener.onSaveInputMethodPreference(this);
            return false;
        }
        // Enable a 3rd party IME.
        showSecurityWarnDialog(mImi);
        return false;
    }

    @Override
    public boolean onPreferenceClick(final Preference preference) {
        // Always returns true to prevent invoking an intent without catching exceptions.
        // See {@link Preference#performClick(PreferenceScreen)}/
        if (isImeEnabler()) {
            // Prevent invoking a settings activity because this preference is for enabling and
            // disabling an input method.
            return true;
        }
        final Context context = getContext();
        try {
            final Intent intent = getIntent();
            if (intent != null) {
                // Invoke a settings activity of an input method.
                context.startActivity(intent);
            }
        } catch (final ActivityNotFoundException e) {
            Log.d(TAG, "IME's Settings Activity Not Found", e);
            final String message = context.getString(
                    R.string.failed_to_open_app_settings_toast,
                    mImi.loadLabel(context.getPackageManager()));
            Toast.makeText(context, message, Toast.LENGTH_LONG).show();
        }
        return true;
    }

    void updatePreferenceViews() {
        final boolean isAlwaysChecked = mInputMethodSettingValues.isAlwaysCheckedIme(
                mImi, getContext());
        // Only when this preference has a switch and an input method should be always enabled,
        // this preference should be disabled to prevent accidentally disabling an input method.
        setEnabled(!((isAlwaysChecked && isImeEnabler()) || (!mIsAllowedByOrganization)));
        setChecked(mInputMethodSettingValues.isEnabledImi(mImi));
        setSummary(getSummaryString());
    }

    private InputMethodManager getInputMethodManager() {
        return (InputMethodManager)getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
    }

    private String getSummaryString() {
        final Context context = getContext();
        if (!mIsAllowedByOrganization) {
            return context.getString(R.string.accessibility_feature_or_input_method_not_allowed);
        }
        final InputMethodManager imm = getInputMethodManager();
        final List<InputMethodSubtype> subtypes = imm.getEnabledInputMethodSubtypeList(mImi, true);
        final ArrayList<CharSequence> subtypeLabels = new ArrayList<>();
        for (final InputMethodSubtype subtype : subtypes) {
            final CharSequence label = subtype.getDisplayName(
                  context, mImi.getPackageName(), mImi.getServiceInfo().applicationInfo);
            subtypeLabels.add(label);
        }
        // TODO: A delimiter of subtype labels should be localized.
        return TextUtils.join(", ", subtypeLabels);
    }

    private void showSecurityWarnDialog(final InputMethodInfo imi) {
        if (mDialog != null && mDialog.isShowing()) {
            mDialog.dismiss();
        }
        final Context context = getContext();
        final AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setCancelable(true /* cancelable */);
        builder.setTitle(android.R.string.dialog_alert_title);
        final CharSequence label = imi.getServiceInfo().applicationInfo.loadLabel(
                context.getPackageManager());
        builder.setMessage(context.getString(R.string.ime_security_warning, label));
        builder.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(final DialogInterface dialog, final int which) {
                // The user confirmed to enable a 3rd party IME.
                setChecked(true);
                mOnSaveListener.onSaveInputMethodPreference(InputMethodPreference.this);
                notifyChanged();
            }
        });
        builder.setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(final DialogInterface dialog, final int which) {
                // The user canceled to enable a 3rd party IME.
                setChecked(false);
                mOnSaveListener.onSaveInputMethodPreference(InputMethodPreference.this);
                notifyChanged();
            }
        });
        mDialog = builder.create();
        mDialog.show();
    }

    int compareTo(final InputMethodPreference rhs, final Collator collator) {
        if (this == rhs) {
            return 0;
        }
        if (mHasPriorityInSorting == rhs.mHasPriorityInSorting) {
            final CharSequence t0 = getTitle();
            final CharSequence t1 = rhs.getTitle();
            if (TextUtils.isEmpty(t0)) {
                return 1;
            }
            if (TextUtils.isEmpty(t1)) {
                return -1;
            }
            return collator.compare(t0.toString(), t1.toString());
        }
        // Prefer always checked system IMEs
        return mHasPriorityInSorting ? -1 : 1;
    }
}
