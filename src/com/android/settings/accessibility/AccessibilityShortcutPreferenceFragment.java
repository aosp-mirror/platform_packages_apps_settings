/*
 * Copyright (C) 2021 The Android Open Source Project
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

import static com.android.internal.accessibility.common.ShortcutConstants.UserShortcutType.DEFAULT;
import static com.android.settings.accessibility.AccessibilityDialogUtils.DialogEnums;
import static com.android.settings.accessibility.AccessibilityUtil.getShortcutSummaryList;
import static com.android.settings.accessibility.ToggleFeaturePreferenceFragment.KEY_GENERAL_CATEGORY;

import android.annotation.SuppressLint;
import android.app.Dialog;
import android.app.settings.SettingsEnums;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.os.Handler;
import android.provider.Settings;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.accessibility.AccessibilityManager;

import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;
import androidx.preference.PreferenceCategory;
import androidx.preference.PreferenceScreen;

import com.android.internal.accessibility.common.ShortcutConstants;
import com.android.internal.accessibility.util.ShortcutUtils;
import com.android.settings.R;
import com.android.settings.accessibility.shortcuts.EditShortcutsPreferenceFragment;
import com.android.settings.dashboard.RestrictedDashboardFragment;

import com.google.android.setupcompat.util.WizardManagerHelper;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Base class for accessibility fragments shortcut functions and dialog management.
 */
public abstract class AccessibilityShortcutPreferenceFragment extends RestrictedDashboardFragment
        implements ShortcutPreference.OnClickCallback {
    private static final String KEY_SHORTCUT_PREFERENCE = "shortcut_preference";

    protected ShortcutPreference mShortcutPreference;
    protected Dialog mDialog;
    private AccessibilityManager.TouchExplorationStateChangeListener
            mTouchExplorationStateChangeListener;
    private AccessibilitySettingsContentObserver mSettingsContentObserver;

    public AccessibilityShortcutPreferenceFragment(String restrictionKey) {
        super(restrictionKey);
    }

    /** Returns the accessibility component name. */
    protected abstract ComponentName getComponentName();

    /** Returns the accessibility feature name. */
    protected abstract CharSequence getLabelName();

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        final int resId = getPreferenceScreenResId();
        if (resId <= 0) {
            final PreferenceScreen preferenceScreen = getPreferenceManager().createPreferenceScreen(
                    getPrefContext());
            setPreferenceScreen(preferenceScreen);
        }

        if (showGeneralCategory()) {
            initGeneralCategory();
        }

        final List<String> shortcutFeatureKeys = new ArrayList<>();
        shortcutFeatureKeys.add(Settings.Secure.ACCESSIBILITY_BUTTON_TARGETS);
        shortcutFeatureKeys.add(Settings.Secure.ACCESSIBILITY_SHORTCUT_TARGET_SERVICE);
        shortcutFeatureKeys.add(Settings.Secure.ACCESSIBILITY_QS_TARGETS);
        mSettingsContentObserver = new AccessibilitySettingsContentObserver(new Handler());
        mSettingsContentObserver.registerKeysToObserverCallback(shortcutFeatureKeys, key -> {
            updateShortcutPreferenceData();
            updateShortcutPreference();
        });
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        mShortcutPreference = new ShortcutPreference(getPrefContext(), /* attrs= */ null);
        mShortcutPreference.setPersistent(false);
        mShortcutPreference.setKey(getShortcutPreferenceKey());
        mShortcutPreference.setOnClickCallback(this);
        mShortcutPreference.setTitle(getShortcutTitle());

        getPreferenceScreen().addPreference(mShortcutPreference);

        mTouchExplorationStateChangeListener = isTouchExplorationEnabled -> {
            mShortcutPreference.setSummary(getShortcutTypeSummary(getPrefContext()));
        };

        return super.onCreateView(inflater, container, savedInstanceState);
    }

    @Override
    public void onResume() {
        super.onResume();

        final AccessibilityManager am = getPrefContext().getSystemService(
                AccessibilityManager.class);
        am.addTouchExplorationStateChangeListener(mTouchExplorationStateChangeListener);
        mSettingsContentObserver.register(getContentResolver());
        updateShortcutPreferenceData();
        updateShortcutPreference();
    }

    @Override
    public void onPause() {
        final AccessibilityManager am = getPrefContext().getSystemService(
                AccessibilityManager.class);
        am.removeTouchExplorationStateChangeListener(mTouchExplorationStateChangeListener);
        mSettingsContentObserver.unregister(getContentResolver());
        super.onPause();
    }

    @Override
    public Dialog onCreateDialog(int dialogId) {
        switch (dialogId) {
            case DialogEnums.LAUNCH_ACCESSIBILITY_TUTORIAL:
                if (WizardManagerHelper.isAnySetupWizard(getIntent())) {
                    mDialog = AccessibilityShortcutsTutorial
                            .createAccessibilityTutorialDialogForSetupWizard(
                                    getPrefContext(), getUserPreferredShortcutTypes(),
                                    this::callOnTutorialDialogButtonClicked, getLabelName());
                } else {
                    mDialog = AccessibilityShortcutsTutorial
                            .createAccessibilityTutorialDialog(
                                    getPrefContext(), getUserPreferredShortcutTypes(),
                                    this::callOnTutorialDialogButtonClicked, getLabelName());
                }
                mDialog.setCanceledOnTouchOutside(false);
                return mDialog;
            default:
                throw new IllegalArgumentException("Unsupported dialogId " + dialogId);
        }
    }

    protected CharSequence getShortcutTitle() {
        return getString(R.string.accessibility_shortcut_title, getLabelName());
    }

    @Override
    public int getDialogMetricsCategory(int dialogId) {
        switch (dialogId) {
            case DialogEnums.LAUNCH_ACCESSIBILITY_TUTORIAL:
                return SettingsEnums.DIALOG_ACCESSIBILITY_TUTORIAL;
            default:
                return SettingsEnums.ACTION_UNKNOWN;
        }
    }

    @Override
    public void onSettingsClicked(ShortcutPreference preference) {
        EditShortcutsPreferenceFragment.showEditShortcutScreen(
                getContext(),
                getMetricsCategory(),
                getShortcutTitle(),
                getComponentName(),
                getIntent()
        );
    }

    @SuppressLint("MissingPermission")
    @Override
    public void onToggleClicked(ShortcutPreference preference) {
        if (getComponentName() == null) {
            return;
        }

        final int shortcutTypes = getUserPreferredShortcutTypes();
        final boolean isChecked = preference.isChecked();
        getPrefContext().getSystemService(AccessibilityManager.class).enableShortcutsForTargets(
                isChecked, shortcutTypes,
                Set.of(getComponentName().flattenToString()), getPrefContext().getUserId());
        if (isChecked) {
            showDialog(DialogEnums.LAUNCH_ACCESSIBILITY_TUTORIAL);
        }
        mShortcutPreference.setSummary(getShortcutTypeSummary(getPrefContext()));
    }

    /**
     * Overrides to return specific shortcut preference key
     *
     * @return String The specific shortcut preference key
     */
    protected String getShortcutPreferenceKey() {
        return KEY_SHORTCUT_PREFERENCE;
    }

    /**
     * Returns the shortcut type list which has been checked by user.
     */
    protected int getUserShortcutTypes() {
        return AccessibilityUtil.getUserShortcutTypesFromSettings(getPrefContext(),
                getComponentName());
    };

    private static CharSequence getSoftwareShortcutTypeSummary(Context context) {
        int resId;
        if (AccessibilityUtil.isFloatingMenuEnabled(context)) {
            resId = R.string.accessibility_shortcut_edit_summary_software;
        } else if (AccessibilityUtil.isGestureNavigateEnabled(context)) {
            resId = R.string.accessibility_shortcut_edit_summary_software_gesture;
        } else {
            resId = R.string.accessibility_shortcut_edit_summary_software;
        }
        return context.getText(resId);
    }

    /**
     * This method will be invoked when a button in the tutorial dialog is clicked.
     *
     * @param dialog The dialog that received the click
     * @param which  The button that was clicked
     */
    private void callOnTutorialDialogButtonClicked(DialogInterface dialog, int which) {
        dialog.dismiss();
    }

    @VisibleForTesting
    void initGeneralCategory() {
        final PreferenceCategory generalCategory = new PreferenceCategory(getPrefContext());
        generalCategory.setKey(KEY_GENERAL_CATEGORY);
        generalCategory.setTitle(getGeneralCategoryDescription(null));

        getPreferenceScreen().addPreference(generalCategory);
    }

    /**
     * Overrides to return customized description for general category above shortcut
     *
     * @return CharSequence The customized description for general category
     */
    protected CharSequence getGeneralCategoryDescription(@Nullable CharSequence title) {
        if (title == null || title.toString().isEmpty()) {
            // Return default 'Options' string for category
            return getContext().getString(R.string.accessibility_screen_option);
        }
        return title;
    }

    /**
     * Overrides to determinate if showing additional category description above shortcut
     *
     * @return boolean true to show category, false otherwise.
     */
    protected boolean showGeneralCategory() {
        return false;
    }

    protected CharSequence getShortcutTypeSummary(Context context) {
        if (!mShortcutPreference.isSettingsEditable()) {
            return context.getText(R.string.accessibility_shortcut_edit_dialog_title_hardware);
        }

        if (!mShortcutPreference.isChecked()) {
            return context.getText(R.string.accessibility_shortcut_state_off);
        }

        final int shortcutTypes = getUserPreferredShortcutTypes();
        return getShortcutSummaryList(context, shortcutTypes);
    }

    protected void updateShortcutPreferenceData() {
        if (getComponentName() == null) {
            return;
        }

        final int shortcutTypes = AccessibilityUtil.getUserShortcutTypesFromSettings(
                getPrefContext(), getComponentName());
        if (shortcutTypes != DEFAULT) {
            final PreferredShortcut shortcut = new PreferredShortcut(
                    getComponentName().flattenToString(), shortcutTypes);
            PreferredShortcuts.saveUserShortcutType(getPrefContext(), shortcut);
        }
    }

    protected void updateShortcutPreference() {
        if (getComponentName() == null) {
            return;
        }

        final int shortcutTypes = getUserPreferredShortcutTypes();
        mShortcutPreference.setChecked(
                ShortcutUtils.isShortcutContained(
                        getPrefContext(), shortcutTypes, getComponentName().flattenToString()));
        mShortcutPreference.setSummary(getShortcutTypeSummary(getPrefContext()));
    }

    /**
     * Returns the user preferred shortcut types or the default shortcut types if not set
     */
    @ShortcutConstants.UserShortcutType
    protected int getUserPreferredShortcutTypes() {
        return PreferredShortcuts.retrieveUserShortcutType(
                getPrefContext(),
                getComponentName().flattenToString());
    }
}
