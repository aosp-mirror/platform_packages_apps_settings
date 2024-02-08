/*
 * Copyright (C) 2023 The Android Open Source Project
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

package com.android.settings.accessibility.shortcuts;

import static android.app.Activity.RESULT_CANCELED;
import static android.provider.Settings.Secure.ACCESSIBILITY_BUTTON_MODE;
import static android.provider.Settings.Secure.ACCESSIBILITY_BUTTON_TARGETS;
import static android.provider.Settings.Secure.ACCESSIBILITY_DISPLAY_MAGNIFICATION_ENABLED;
import static android.provider.Settings.Secure.ACCESSIBILITY_MAGNIFICATION_TWO_FINGER_TRIPLE_TAP_ENABLED;
import static android.provider.Settings.Secure.ACCESSIBILITY_SHORTCUT_TARGET_SERVICE;

import static com.android.internal.accessibility.AccessibilityShortcutController.MAGNIFICATION_COMPONENT_NAME;
import static com.android.internal.accessibility.AccessibilityShortcutController.MAGNIFICATION_CONTROLLER_NAME;
import static com.android.settings.SettingsActivity.EXTRA_SHOW_FRAGMENT_TITLE;

import android.app.Activity;
import android.app.settings.SettingsEnums;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.database.ContentObserver;
import android.icu.text.ListFormatter;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.ArrayMap;
import android.util.Pair;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.accessibility.AccessibilityManager;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;
import androidx.preference.Preference;
import androidx.recyclerview.widget.RecyclerView;

import com.android.internal.accessibility.dialog.AccessibilityTarget;
import com.android.internal.accessibility.dialog.AccessibilityTargetHelper;
import com.android.settings.R;
import com.android.settings.SetupWizardUtils;
import com.android.settings.accessibility.AccessibilitySetupWizardUtils;
import com.android.settings.accessibility.Flags;
import com.android.settings.accessibility.PreferredShortcuts;
import com.android.settings.core.SubSettingLauncher;
import com.android.settings.dashboard.DashboardFragment;
import com.android.settingslib.core.AbstractPreferenceController;

import com.google.android.setupcompat.template.FooterBarMixin;
import com.google.android.setupcompat.util.WizardManagerHelper;
import com.google.android.setupdesign.GlifPreferenceLayout;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * A screen show various accessibility shortcut options for the given a11y feature
 */
public class EditShortcutsPreferenceFragment extends DashboardFragment {
    private static final String TAG = "EditShortcutsPreferenceFragment";

    @VisibleForTesting
    static final String ARG_KEY_SHORTCUT_TARGETS = "targets";
    @VisibleForTesting
    static final String SAVED_STATE_IS_EXPANDED = "isExpanded";
    private ContentObserver mSettingsObserver;

    private static final Uri VOLUME_KEYS_SHORTCUT_SETTING =
            Settings.Secure.getUriFor(ACCESSIBILITY_SHORTCUT_TARGET_SERVICE);
    private static final Uri BUTTON_SHORTCUT_MODE_SETTING =
            Settings.Secure.getUriFor(ACCESSIBILITY_BUTTON_MODE);
    private static final Uri BUTTON_SHORTCUT_SETTING =
            Settings.Secure.getUriFor(ACCESSIBILITY_BUTTON_TARGETS);

    private static final Uri TRIPLE_TAP_SHORTCUT_SETTING =
            Settings.Secure.getUriFor(ACCESSIBILITY_DISPLAY_MAGNIFICATION_ENABLED);
    private static final Uri TWO_FINGERS_DOUBLE_TAP_SHORTCUT_SETTING =
            Settings.Secure.getUriFor(ACCESSIBILITY_MAGNIFICATION_TWO_FINGER_TRIPLE_TAP_ENABLED);

    @VisibleForTesting
    static final Uri[] SHORTCUT_SETTINGS = {
            VOLUME_KEYS_SHORTCUT_SETTING,
            BUTTON_SHORTCUT_MODE_SETTING,
            BUTTON_SHORTCUT_SETTING,
            TRIPLE_TAP_SHORTCUT_SETTING,
            TWO_FINGERS_DOUBLE_TAP_SHORTCUT_SETTING,
    };

    private Set<String> mShortcutTargets;

    @Nullable
    private AccessibilityManager.TouchExplorationStateChangeListener
            mTouchExplorationStateChangeListener;


    /**
     * Helper method to show the edit shortcut screen
     */
    public static void showEditShortcutScreen(
            Context context, int metricsCategory, CharSequence screenTitle,
            ComponentName target, Intent fromIntent) {
        Bundle args = new Bundle();

        if (MAGNIFICATION_COMPONENT_NAME.equals(target)) {
            // We can remove this branch once b/147990389 is completed
            args.putStringArray(
                    ARG_KEY_SHORTCUT_TARGETS, new String[]{MAGNIFICATION_CONTROLLER_NAME});
        } else {
            args.putStringArray(
                    ARG_KEY_SHORTCUT_TARGETS, new String[]{target.flattenToString()});
        }
        Intent toIntent = new Intent();
        if (fromIntent != null) {
            SetupWizardUtils.copySetupExtras(fromIntent, toIntent);
        }

        new SubSettingLauncher(context)
                .setDestination(EditShortcutsPreferenceFragment.class.getName())
                .setExtras(toIntent.getExtras())
                .setArguments(args)
                .setSourceMetricsCategory(metricsCategory)
                .setTitleText(screenTitle)
                .launch();
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        initializeArguments();
        initializePreferenceControllerArguments();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (savedInstanceState != null) {
            boolean isExpanded = savedInstanceState.getBoolean(SAVED_STATE_IS_EXPANDED);
            if (isExpanded) {
                onExpanded();
            }
        }
        mSettingsObserver = new ContentObserver(new Handler()) {
            @Override
            public void onChange(boolean selfChange, Uri uri) {
                if (VOLUME_KEYS_SHORTCUT_SETTING.equals(uri)) {
                    refreshPreferenceController(VolumeKeysShortcutOptionController.class);
                } else if (BUTTON_SHORTCUT_MODE_SETTING.equals(uri)
                        || BUTTON_SHORTCUT_SETTING.equals(uri)) {
                    refreshSoftwareShortcutControllers();
                } else if (TRIPLE_TAP_SHORTCUT_SETTING.equals(uri)) {
                    refreshPreferenceController(TripleTapShortcutOptionController.class);
                } else if (TWO_FINGERS_DOUBLE_TAP_SHORTCUT_SETTING.equals(uri)) {
                    refreshPreferenceController(TwoFingersDoubleTapShortcutOptionController.class);
                }

                PreferredShortcuts.updatePreferredShortcutsFromSettings(
                        getContext(), mShortcutTargets);
            }
        };

        registerSettingsObserver();
    }

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        super.onCreatePreferences(savedInstanceState, rootKey);

        Activity activity = getActivity();

        if (!activity.getIntent().getAction().equals(
                Settings.ACTION_ACCESSIBILITY_SHORTCUT_SETTINGS)
                || !Flags.editShortcutsInFullScreen()) {
            return;
        }

        // TODO(b/325664350): Implement shortcut type for "all shortcuts"
        List<AccessibilityTarget> accessibilityTargets =
                AccessibilityTargetHelper.getInstalledTargets(
                        activity.getBaseContext(), AccessibilityManager.ACCESSIBILITY_SHORTCUT_KEY);

        Pair<String, String> titles = getTitlesFromAccessibilityTargetList(
                mShortcutTargets,
                accessibilityTargets,
                activity.getResources()
        );

        activity.setTitle(titles.first);

        String categoryKey = activity.getResources().getString(
                R.string.accessibility_shortcut_description_pref);
        findPreference(categoryKey).setTitle(titles.second);
    }

    @NonNull
    @Override
    public RecyclerView onCreateRecyclerView(
            @NonNull LayoutInflater inflater, @NonNull ViewGroup parent,
            @Nullable Bundle savedInstanceState) {
        if (parent instanceof GlifPreferenceLayout layout) {
            // Usually for setup wizard
            return layout.onCreateRecyclerView(inflater, parent, savedInstanceState);
        } else {
            return super.onCreateRecyclerView(inflater, parent, savedInstanceState);
        }
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        if (view instanceof GlifPreferenceLayout layout) {
            // Usually for setup wizard
            String title = null;
            Intent intent = getIntent();
            if (intent != null) {
                title = intent.getStringExtra(EXTRA_SHOW_FRAGMENT_TITLE);
            }
            AccessibilitySetupWizardUtils.updateGlifPreferenceLayout(getContext(), layout, title,
                    /* description= */ null, /* icon= */ null);

            FooterBarMixin mixin = layout.getMixin(FooterBarMixin.class);
            AccessibilitySetupWizardUtils.setPrimaryButton(getContext(), mixin, R.string.done,
                    () -> {
                        setResult(RESULT_CANCELED);
                        finish();
                    });
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        mTouchExplorationStateChangeListener = isTouchExplorationEnabled ->
                refreshPreferenceController(GestureShortcutOptionController.class);

        final AccessibilityManager am = getSystemService(
                AccessibilityManager.class);
        am.addTouchExplorationStateChangeListener(mTouchExplorationStateChangeListener);
        PreferredShortcuts.updatePreferredShortcutsFromSettings(getContext(), mShortcutTargets);
    }

    @Override
    public void onPause() {
        super.onPause();

        if (mTouchExplorationStateChangeListener != null) {
            final AccessibilityManager am = getSystemService(
                    AccessibilityManager.class);
            am.removeTouchExplorationStateChangeListener(mTouchExplorationStateChangeListener);
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBoolean(
                SAVED_STATE_IS_EXPANDED,
                use(AdvancedShortcutsPreferenceController.class).isExpanded());
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        unregisterSettingsObserver();
    }

    private void registerSettingsObserver() {
        if (mSettingsObserver != null) {
            for (Uri uri : SHORTCUT_SETTINGS) {
                getContentResolver().registerContentObserver(
                        uri, /* notifyForDescendants= */ false, mSettingsObserver);
            }
        }
    }

    private void unregisterSettingsObserver() {
        if (mSettingsObserver != null) {
            getContentResolver().unregisterContentObserver(mSettingsObserver);
        }
    }

    private void initializeArguments() {
        Bundle args = getArguments();
        if (args == null || args.isEmpty()) {
            throw new IllegalArgumentException(
                    EditShortcutsPreferenceFragment.class.getSimpleName()
                            + " requires non-empty shortcut targets");
        }

        String[] targets = args.getStringArray(ARG_KEY_SHORTCUT_TARGETS);
        if (targets == null) {
            throw new IllegalArgumentException(
                    EditShortcutsPreferenceFragment.class.getSimpleName()
                            + " requires non-empty shortcut targets");
        }

        mShortcutTargets = Set.of(targets);
    }

    @Override
    public int getMetricsCategory() {
        return SettingsEnums.DIALOG_ACCESSIBILITY_SERVICE_EDIT_SHORTCUT;
    }

    @Override
    protected int getPreferenceScreenResId() {
        return R.xml.accessibility_edit_shortcuts;
    }

    @Override
    protected String getLogTag() {
        return TAG;
    }

    @Override
    public boolean onPreferenceTreeClick(Preference preference) {
        if (getString(R.string.accessibility_shortcuts_advanced_collapsed)
                .equals(preference.getKey())) {
            onExpanded();
            // log here since calling super.onPreferenceTreeClick will be skipped
            writePreferenceClickMetric(preference);
            return true;
        }
        return super.onPreferenceTreeClick(preference);
    }

    @VisibleForTesting
    void initializePreferenceControllerArguments() {
        boolean isInSuw = WizardManagerHelper.isAnySetupWizard(getIntent());

        getPreferenceControllers()
                .stream()
                .flatMap(Collection::stream)
                .filter(
                        controller -> controller instanceof ShortcutOptionPreferenceController)
                .forEach(controller -> {
                    ShortcutOptionPreferenceController shortcutOptionPreferenceController =
                            (ShortcutOptionPreferenceController) controller;
                    shortcutOptionPreferenceController.setShortcutTargets(mShortcutTargets);
                    shortcutOptionPreferenceController.setInSetupWizard(isInSuw);
                });
    }

    private void onExpanded() {
        AdvancedShortcutsPreferenceController advanced =
                use(AdvancedShortcutsPreferenceController.class);
        advanced.setExpanded(true);

        TripleTapShortcutOptionController tripleTapShortcutOptionController =
                use(TripleTapShortcutOptionController.class);
        tripleTapShortcutOptionController.setExpanded(true);

        refreshPreferenceController(AdvancedShortcutsPreferenceController.class);
        refreshPreferenceController(TripleTapShortcutOptionController.class);
    }

    private void refreshPreferenceController(
            Class<? extends AbstractPreferenceController> controllerClass) {
        AbstractPreferenceController controller = use(controllerClass);
        if (controller != null) {
            controller.displayPreference(getPreferenceScreen());
            if (!TextUtils.isEmpty(controller.getPreferenceKey())) {
                controller.updateState(findPreference(controller.getPreferenceKey()));
            }
        }
    }

    private void refreshSoftwareShortcutControllers() {
        // Gesture
        refreshPreferenceController(GestureShortcutOptionController.class);

        // FAB
        refreshPreferenceController(FloatingButtonShortcutOptionController.class);

        // A11y Nav Button
        refreshPreferenceController(NavButtonShortcutOptionController.class);
    }

    /**
     * Generates a title & subtitle pair describing the features whose shortcuts are being edited.
     *
     * @param shortcutTargets string list of component names corresponding to
     *                        the relevant shortcut targets.
     * @param accessibilityTargets list of accessibility targets
     *                             to try and find corresponding labels in.
     * @return pair of strings to be used as page title and subtitle.
     * If there is only one shortcut label, It is displayed in the title and the subtitle is null.
     * Otherwise, the title is a generic prompt and the subtitle lists all shortcut labels.
     */
    @VisibleForTesting
    static Pair<String, String> getTitlesFromAccessibilityTargetList(
            Set<String> shortcutTargets,
            List<AccessibilityTarget> accessibilityTargets,
            Resources resources) {
        ArrayList<CharSequence> featureLabels = new ArrayList<>();

        Map<String, CharSequence> accessibilityTargetLabels = new ArrayMap<>();
        accessibilityTargets.forEach((target) -> accessibilityTargetLabels.put(
                target.getId(), target.getLabel()));

        for (String target: shortcutTargets) {
            if (accessibilityTargetLabels.containsKey(target)) {
                featureLabels.add(accessibilityTargetLabels.get(target));
            } else {
                throw new IllegalStateException("Shortcut target does not have a label: " + target);
            }
        }

        if (featureLabels.size() == 1) {
            return new Pair<>(
                    resources.getString(
                            R.string.accessibility_shortcut_title, featureLabels.get(0)),
                    null
            );
        } else if (featureLabels.size() == 0) {
            throw new IllegalStateException("Found no labels for any shortcut targets.");
        } else {
            return new Pair<>(
                    resources.getString(R.string.accessibility_shortcut_edit_screen_title),
                    resources.getString(
                            R.string.accessibility_shortcut_edit_screen_prompt,
                            ListFormatter.getInstance().format(featureLabels))
            );
        }
    }
}
