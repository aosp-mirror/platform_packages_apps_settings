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

import static com.android.internal.accessibility.AccessibilityShortcutController.MAGNIFICATION_CONTROLLER_NAME;
import static com.android.settings.accessibility.AccessibilityUtil.State.OFF;
import static com.android.settings.accessibility.AccessibilityUtil.State.ON;

import android.app.Dialog;
import android.app.settings.SettingsEnums;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.provider.Settings;
import android.text.TextUtils;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;

import androidx.preference.Preference;

import com.android.settings.R;
import com.android.settings.dashboard.DashboardFragment;
import com.android.settings.search.BaseSearchIndexProvider;
import com.android.settingslib.search.SearchIndexable;

import com.google.common.primitives.Ints;

import java.util.StringJoiner;

/** Settings page for magnification. */
@SearchIndexable(forTarget = SearchIndexable.ALL & ~SearchIndexable.ARC)
public class MagnificationSettingsFragment extends DashboardFragment {

    private static final String TAG = "MagnificationSettingsFragment";
    private static final String PREF_KEY_MODE = "magnification_mode";
    private static final String KEY_CAPABILITY =
            Settings.Secure.ACCESSIBILITY_MAGNIFICATION_CAPABILITY;
    private static final int DIALOG_MAGNIFICATION_CAPABILITY = 1;
    private static final int DIALOG_MAGNIFICATION_SWITCH_SHORTCUT = 2;
    private static final String EXTRA_CAPABILITY = "capability";
    private static final int NONE = 0;
    private static final char COMPONENT_NAME_SEPARATOR = ':';
    private Preference mModePreference;
    private int mCapabilities = NONE;
    private CheckBox mMagnifyFullScreenCheckBox;
    private CheckBox mMagnifyWindowCheckBox;
    private Dialog mDialog;

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
        return getSecureIntValue(context, KEY_CAPABILITY,
                Settings.Secure.ACCESSIBILITY_MAGNIFICATION_MODE_FULLSCREEN);
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
            mCapabilities = savedInstanceState.getInt(EXTRA_CAPABILITY, NONE);
        }
        if (mCapabilities == NONE) {
            mCapabilities = getMagnificationCapabilities(getPrefContext());
        }
    }

    @Override
    public int getDialogMetricsCategory(int dialogId) {
        switch (dialogId) {
            case DIALOG_MAGNIFICATION_CAPABILITY:
                return SettingsEnums.DIALOG_MAGNIFICATION_CAPABILITY;
            case DIALOG_MAGNIFICATION_SWITCH_SHORTCUT:
                return SettingsEnums.DIALOG_MAGNIFICATION_SWITCH_SHORTCUT;
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
        final CharSequence title;
        switch (dialogId) {
            case DIALOG_MAGNIFICATION_CAPABILITY:
                title = getPrefContext().getString(
                        R.string.accessibility_magnification_mode_title);
                mDialog = AccessibilityEditDialogUtils.showMagnificationModeDialog(getPrefContext(),
                        title, this::callOnAlertDialogCheckboxClicked);
                initializeDialogCheckBox(mDialog);
                return mDialog;
            case DIALOG_MAGNIFICATION_SWITCH_SHORTCUT:
                title = getPrefContext().getString(
                        R.string.accessibility_magnification_switch_shortcut_title);
                mDialog = AccessibilityEditDialogUtils.showMagnificationSwitchShortcutDialog(
                        getPrefContext(), title, this::onSwitchShortcutDialogPositiveButtonClicked);
                return mDialog;
        }

        throw new IllegalArgumentException("Unsupported dialogId " + dialogId);
    }

    private void callOnAlertDialogCheckboxClicked(DialogInterface dialog, int which) {
        updateCapabilities(true);
        mModePreference.setSummary(
                getMagnificationCapabilitiesSummary(getPrefContext()));
    }

    private void onSwitchShortcutDialogPositiveButtonClicked(View view) {
        //TODO(b/147990389): Merge this function into util until magnification change format to
        // Component.
        optOutMagnificationFromTripleTap();
        optInMagnificationToAccessibilityButton();

        mDialog.dismiss();
    }

    private void optOutMagnificationFromTripleTap() {
        Settings.Secure.putInt(getPrefContext().getContentResolver(),
                Settings.Secure.ACCESSIBILITY_DISPLAY_MAGNIFICATION_ENABLED, OFF);
    }

    private void optInMagnificationToAccessibilityButton() {
        final String targetKey = Settings.Secure.ACCESSIBILITY_BUTTON_TARGETS;
        final String targetString = Settings.Secure.getString(getPrefContext().getContentResolver(),
                targetKey);
        if (targetString.contains(MAGNIFICATION_CONTROLLER_NAME)) {
            return;
        }

        final StringJoiner joiner = new StringJoiner(String.valueOf(COMPONENT_NAME_SEPARATOR));

        if (!TextUtils.isEmpty(targetString)) {
            joiner.add(targetString);
        }
        joiner.add(MAGNIFICATION_CONTROLLER_NAME);

        Settings.Secure.putString(getPrefContext().getContentResolver(), targetKey,
                joiner.toString());
    }

    private void initializeDialogCheckBox(Dialog dialog) {
        final View dialogFullScreenView = dialog.findViewById(R.id.magnify_full_screen);
        final View dialogFullScreenTextArea = dialogFullScreenView.findViewById(R.id.container);
        mMagnifyFullScreenCheckBox = dialogFullScreenView.findViewById(R.id.checkbox);

        final View dialogWidowView = dialog.findViewById(R.id.magnify_window_screen);
        final View dialogWindowTextArea = dialogWidowView.findViewById(R.id.container);
        mMagnifyWindowCheckBox = dialogWidowView.findViewById(R.id.checkbox);

        setTextAreasClickListener(dialogFullScreenTextArea, mMagnifyFullScreenCheckBox,
                dialogWindowTextArea, mMagnifyWindowCheckBox);

        updateAlertDialogCheckState();
        updateAlertDialogEnableState(dialogFullScreenTextArea, dialogWindowTextArea);
    }

    private void setTextAreasClickListener(View fullScreenTextArea, CheckBox fullScreenCheckBox,
            View windowTextArea, CheckBox windowCheckBox) {
        fullScreenTextArea.setOnClickListener(v -> {
            fullScreenCheckBox.toggle();
            updateCapabilities(false);
            updateAlertDialogEnableState(fullScreenTextArea, windowTextArea);
        });

        windowTextArea.setOnClickListener(v -> {
            windowCheckBox.toggle();
            updateCapabilities(false);
            updateAlertDialogEnableState(fullScreenTextArea, windowTextArea);

            if (isTripleTapEnabled() && windowCheckBox.isChecked()) {
                showDialog(DIALOG_MAGNIFICATION_SWITCH_SHORTCUT);
            }
        });
    }

    private void updateAlertDialogCheckState() {
        updateCheckStatus(mMagnifyWindowCheckBox,
                Settings.Secure.ACCESSIBILITY_MAGNIFICATION_MODE_WINDOW);
        updateCheckStatus(mMagnifyFullScreenCheckBox,
                Settings.Secure.ACCESSIBILITY_MAGNIFICATION_MODE_FULLSCREEN);

    }

    private void updateCheckStatus(CheckBox checkBox, int mode) {
        checkBox.setChecked((mode & mCapabilities) != 0);
    }

    private void updateAlertDialogEnableState(View fullScreenTextArea, View windowTextArea) {
        switch (mCapabilities) {
            case Settings.Secure.ACCESSIBILITY_MAGNIFICATION_MODE_FULLSCREEN:
                setViewAndChildrenEnabled(fullScreenTextArea, false);
                break;
            case Settings.Secure.ACCESSIBILITY_MAGNIFICATION_MODE_WINDOW:
                setViewAndChildrenEnabled(windowTextArea, false);
                break;
            case Settings.Secure.ACCESSIBILITY_MAGNIFICATION_MODE_ALL:
                setViewAndChildrenEnabled(fullScreenTextArea, true);
                setViewAndChildrenEnabled(windowTextArea, true);
                break;
            default:
                throw new IllegalArgumentException(
                        "Unsupported ACCESSIBILITY_MAGNIFICATION_CAPABILITY " + mCapabilities);
        }
    }

    private void setViewAndChildrenEnabled(View view, boolean enabled) {
        view.setEnabled(enabled);
        if (view instanceof ViewGroup) {
            final ViewGroup viewGroup = (ViewGroup) view;
            for (int i = 0; i < viewGroup.getChildCount(); i++) {
                View child = viewGroup.getChildAt(i);
                setViewAndChildrenEnabled(child, enabled);
            }
        }
    }

    private void updateCapabilities(boolean saveToDB) {
        int capabilities = 0;
        capabilities |=
                mMagnifyFullScreenCheckBox.isChecked()
                        ? Settings.Secure.ACCESSIBILITY_MAGNIFICATION_MODE_FULLSCREEN : 0;
        capabilities |= mMagnifyWindowCheckBox.isChecked()
                ? Settings.Secure.ACCESSIBILITY_MAGNIFICATION_MODE_WINDOW : 0;
        mCapabilities = capabilities;
        if (saveToDB) {
            setMagnificationCapabilities(capabilities);
        }
    }

    private boolean isTripleTapEnabled() {
        return Settings.Secure.getInt(getPrefContext().getContentResolver(),
                Settings.Secure.ACCESSIBILITY_DISPLAY_MAGNIFICATION_ENABLED, OFF) == ON;
    }

    private void setSecureIntValue(String key, int value) {
        Settings.Secure.putIntForUser(getPrefContext().getContentResolver(),
                key, value, getPrefContext().getContentResolver().getUserId());
    }

    private void setMagnificationCapabilities(int capabilities) {
        setSecureIntValue(KEY_CAPABILITY, capabilities);
    }

    public static final BaseSearchIndexProvider SEARCH_INDEX_DATA_PROVIDER =
            new BaseSearchIndexProvider(R.xml.accessibility_magnification_service_settings);
}
