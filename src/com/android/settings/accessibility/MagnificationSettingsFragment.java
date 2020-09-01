/*
 * Copyright (C) 2019 The Android Open Source Project
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

package com.android.settings.accessibility;

import android.app.Dialog;
import android.app.settings.SettingsEnums;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.provider.Settings;
import android.view.View;
import android.widget.CheckBox;

import androidx.annotation.IntDef;
import androidx.appcompat.app.AlertDialog;
import androidx.preference.Preference;

import com.android.settings.R;
import com.android.settings.dashboard.DashboardFragment;
import com.android.settings.search.BaseSearchIndexProvider;
import com.android.settingslib.search.SearchIndexable;

import com.google.common.primitives.Ints;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/** Settings page for magnification. */
@SearchIndexable(forTarget = SearchIndexable.ALL & ~SearchIndexable.ARC)
public class MagnificationSettingsFragment extends DashboardFragment {

    private static final String TAG = "MagnificationSettingsFragment";
    private static final String PREF_KEY_MODE = "magnification_mode";
    // TODO(b/146019459): Use magnification_capability.
    private static final String KEY_CAPABILITY = Settings.System.MASTER_MONO;
    private static final int DIALOG_MAGNIFICATION_CAPABILITY = 1;
    private static final String EXTRA_CAPABILITY = "capability";
    private Preference mModePreference;
    private int mCapabilities = MagnifyMode.NONE;
    private CheckBox mMagnifyFullScreenCheckBox;
    private CheckBox mMagnifyWindowCheckBox;

    static String getMagnificationCapabilitiesSummary(Context context) {
        final String[] magnificationModeSummaries = context.getResources().getStringArray(
                R.array.magnification_mode_summaries);
        final int[] magnificationModeValues = context.getResources().getIntArray(
                R.array.magnification_mode_values);
        final int capabilities = MagnificationSettingsFragment.getMagnificationCapabilities(
                context);

        final int idx = Ints.indexOf(magnificationModeValues, capabilities);
        return magnificationModeSummaries[idx == -1 ? 0 : idx];
    }

    private static int getMagnificationCapabilities(Context context) {
        return getSecureIntValue(context, KEY_CAPABILITY, MagnifyMode.FULLSCREEN);
    }

    private static int getSecureIntValue(Context context, String key, int defaultValue) {
        return Settings.Secure.getIntForUser(
                context.getContentResolver(),
                key, defaultValue, context.getContentResolver().getUserId());
    }

    @Override
    public int getMetricsCategory() {
        return SettingsEnums.ACCESSIBILITY_MAGNIFICATION_SETTINGS;
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        outState.putInt(EXTRA_CAPABILITY, mCapabilities);
        super.onSaveInstanceState(outState);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        if (savedInstanceState != null) {
            mCapabilities = savedInstanceState.getInt(EXTRA_CAPABILITY, MagnifyMode.NONE);
        }
        if (mCapabilities == MagnifyMode.NONE) {
            mCapabilities = getMagnificationCapabilities(getPrefContext());
        }
    }

    @Override
    public int getDialogMetricsCategory(int dialogId) {
        switch (dialogId) {
            case DIALOG_MAGNIFICATION_CAPABILITY:
                return SettingsEnums.DIALOG_MAGNIFICATION_CAPABILITY;
            default:
                return 0;
        }
    }

    @Override
    protected String getLogTag() {
        return TAG;
    }

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        mModePreference = findPreference(PREF_KEY_MODE);
        mModePreference.setOnPreferenceClickListener(preference -> {
            mCapabilities = getMagnificationCapabilities(getPrefContext());
            showDialog(DIALOG_MAGNIFICATION_CAPABILITY);
            return true;
        });
    }

    @Override
    protected int getPreferenceScreenResId() {
        return R.xml.accessibility_magnification_service_settings;
    }

    @Override
    public Dialog onCreateDialog(int dialogId) {
        if (dialogId == DIALOG_MAGNIFICATION_CAPABILITY) {
            final String title = getPrefContext().getString(
                    R.string.accessibility_magnification_mode_title);
            AlertDialog alertDialog = AccessibilityEditDialogUtils
                    .showMagnificationModeDialog(getPrefContext(), title,
                            this::callOnAlertDialogCheckboxClicked);
            initializeDialogCheckBox(alertDialog);
            return alertDialog;
        }
        throw new IllegalArgumentException("Unsupported dialogId " + dialogId);
    }

    private void callOnAlertDialogCheckboxClicked(DialogInterface dialog, int which) {
        updateCapabilities(true);
        mModePreference.setSummary(
                getMagnificationCapabilitiesSummary(getPrefContext()));
    }

    private void initializeDialogCheckBox(AlertDialog dialog) {
        final View dialogFullScreenView = dialog.findViewById(R.id.magnify_full_screen);
        mMagnifyFullScreenCheckBox = dialogFullScreenView.findViewById(R.id.checkbox);

        final View dialogWidowView = dialog.findViewById(R.id.magnify_window_screen);
        mMagnifyWindowCheckBox = dialogWidowView.findViewById(R.id.checkbox);

        updateAlertDialogCheckState();
        updateAlertDialogEnableState();
    }

    private void updateAlertDialogCheckState() {
        updateCheckStatus(mMagnifyWindowCheckBox, MagnifyMode.WINDOW);
        updateCheckStatus(mMagnifyFullScreenCheckBox, MagnifyMode.FULLSCREEN);

    }

    private void updateCheckStatus(CheckBox checkBox, int mode) {
        checkBox.setChecked((mode & mCapabilities) != 0);
        checkBox.setOnClickListener(v -> {
            updateCapabilities(false);
            updateAlertDialogEnableState();
        });
    }

    private void updateAlertDialogEnableState() {
        if (mCapabilities != MagnifyMode.ALL) {
            disableEnabledMagnificationModePreference();
        } else {
            enableAllPreference();
        }
    }

    private void enableAllPreference() {
        mMagnifyFullScreenCheckBox.setEnabled(true);
        mMagnifyWindowCheckBox.setEnabled(true);
    }

    private void disableEnabledMagnificationModePreference() {
        if (!mMagnifyFullScreenCheckBox.isChecked()) {
            mMagnifyWindowCheckBox.setEnabled(false);
        } else if (!mMagnifyWindowCheckBox.isChecked()) {
            mMagnifyFullScreenCheckBox.setEnabled(false);
        }
    }

    private void updateCapabilities(boolean saveToDB) {
        int capabilities = 0;
        capabilities |=
                mMagnifyFullScreenCheckBox.isChecked() ? MagnifyMode.FULLSCREEN : 0;
        capabilities |= mMagnifyWindowCheckBox.isChecked() ? MagnifyMode.WINDOW : 0;
        mCapabilities = capabilities;
        if (saveToDB) {
            setMagnificationCapabilities(capabilities);
        }
    }

    private void setSecureIntValue(String key, int value) {
        Settings.Secure.putIntForUser(getPrefContext().getContentResolver(),
                key, value, getPrefContext().getContentResolver().getUserId());
    }

    private void setMagnificationCapabilities(int capabilities) {
        setSecureIntValue(KEY_CAPABILITY, capabilities);
    }

    @Retention(RetentionPolicy.SOURCE)
    @IntDef({
            MagnifyMode.NONE,
            MagnifyMode.FULLSCREEN,
            MagnifyMode.WINDOW,
            MagnifyMode.ALL,
    })
    private @interface MagnifyMode {
        int NONE = 0;
        int FULLSCREEN = 1;
        int WINDOW = 2;
        int ALL = 3;
    }

    public static final BaseSearchIndexProvider SEARCH_INDEX_DATA_PROVIDER =
            new BaseSearchIndexProvider(R.xml.accessibility_magnification_service_settings);
}
