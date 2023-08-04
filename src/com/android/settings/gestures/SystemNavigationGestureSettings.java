/*
 * Copyright (C) 2018 The Android Open Source Project
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

package com.android.settings.gestures;

import static android.os.UserHandle.USER_CURRENT;
import static android.provider.Settings.Secure.ACCESSIBILITY_BUTTON_MODE_FLOATING_MENU;
import static android.view.WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY;
import static android.view.WindowManagerPolicyConstants.NAV_BAR_MODE_2BUTTON_OVERLAY;
import static android.view.WindowManagerPolicyConstants.NAV_BAR_MODE_3BUTTON_OVERLAY;
import static android.view.WindowManagerPolicyConstants.NAV_BAR_MODE_GESTURAL_OVERLAY;

import android.app.settings.SettingsEnums;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.om.IOverlayManager;
import android.content.om.OverlayInfo;
import android.content.res.Resources;
import android.os.Bundle;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.provider.Settings;
import android.text.TextUtils;
import android.view.accessibility.AccessibilityManager;

import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;
import androidx.preference.PreferenceScreen;

import com.android.settings.R;
import com.android.settings.accessibility.AccessibilityGestureNavigationTutorial;
import com.android.settings.core.SubSettingLauncher;
import com.android.settings.dashboard.suggestions.SuggestionFeatureProvider;
import com.android.settings.overlay.FeatureFactory;
import com.android.settings.search.BaseSearchIndexProvider;
import com.android.settings.support.actionbar.HelpResourceProvider;
import com.android.settings.utils.CandidateInfoExtra;
import com.android.settings.widget.RadioButtonPickerFragment;
import com.android.settingslib.search.SearchIndexable;
import com.android.settingslib.search.SearchIndexableRaw;
import com.android.settingslib.widget.CandidateInfo;
import com.android.settingslib.widget.IllustrationPreference;
import com.android.settingslib.widget.SelectorWithWidgetPreference;

import java.util.ArrayList;
import java.util.List;

@SearchIndexable
public class SystemNavigationGestureSettings extends RadioButtonPickerFragment implements
        HelpResourceProvider {

    @VisibleForTesting
    static final String KEY_SYSTEM_NAV_3BUTTONS = "system_nav_3buttons";
    @VisibleForTesting
    static final String KEY_SYSTEM_NAV_2BUTTONS = "system_nav_2buttons";
    @VisibleForTesting
    static final String KEY_SYSTEM_NAV_GESTURAL = "system_nav_gestural";

    public static final String PREF_KEY_SUGGESTION_COMPLETE =
            "pref_system_navigation_suggestion_complete";

    private static final String KEY_SHOW_A11Y_TUTORIAL_DIALOG = "show_a11y_tutorial_dialog_bool";

    static final String LAUNCHER_PACKAGE_NAME = "com.google.android.apps.nexuslauncher";

    static final String ACTION_GESTURE_SANDBOX = "com.android.quickstep.action.GESTURE_SANDBOX";

    final Intent mLaunchSandboxIntent = new Intent(ACTION_GESTURE_SANDBOX)
            .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            .putExtra("use_tutorial_menu", true)
            .setPackage(LAUNCHER_PACKAGE_NAME);

    private static final int MIN_LARGESCREEN_WIDTH_DP = 600;

    private boolean mA11yTutorialDialogShown = false;

    private IOverlayManager mOverlayManager;

    private IllustrationPreference mVideoPreference;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (savedInstanceState != null) {
            mA11yTutorialDialogShown =
                    savedInstanceState.getBoolean(KEY_SHOW_A11Y_TUTORIAL_DIALOG, false);
            if (mA11yTutorialDialogShown) {
                AccessibilityGestureNavigationTutorial.showGestureNavigationTutorialDialog(
                        getContext(), dialog -> mA11yTutorialDialogShown = false);
            }
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        outState.putBoolean(KEY_SHOW_A11Y_TUTORIAL_DIALOG, mA11yTutorialDialogShown);
        super.onSaveInstanceState(outState);
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);

        SuggestionFeatureProvider suggestionFeatureProvider =
                FeatureFactory.getFeatureFactory().getSuggestionFeatureProvider();
        SharedPreferences prefs = suggestionFeatureProvider.getSharedPrefs(context);
        prefs.edit().putBoolean(PREF_KEY_SUGGESTION_COMPLETE, true).apply();

        mOverlayManager = IOverlayManager.Stub.asInterface(
                ServiceManager.getService(Context.OVERLAY_SERVICE));

        mVideoPreference = new IllustrationPreference(context);
        Context windowContext = context.createWindowContext(TYPE_APPLICATION_OVERLAY, null);
        if (windowContext.getResources()
                .getConfiguration().smallestScreenWidthDp >= MIN_LARGESCREEN_WIDTH_DP) {
            mVideoPreference.applyDynamicColor();
        }
        setIllustrationVideo(mVideoPreference, getDefaultKey());
        setIllustrationClickListener(mVideoPreference, getDefaultKey());

        migrateOverlaySensitivityToSettings(context, mOverlayManager);
    }

    @Override
    public int getMetricsCategory() {
        return SettingsEnums.SETTINGS_GESTURE_SWIPE_UP;
    }

    @Override
    public void updateCandidates() {
        final String defaultKey = getDefaultKey();
        final String systemDefaultKey = getSystemDefaultKey();
        final PreferenceScreen screen = getPreferenceScreen();
        screen.removeAll();
        screen.addPreference(mVideoPreference);

        final List<? extends CandidateInfo> candidateList = getCandidates();
        if (candidateList == null) {
            return;
        }
        for (CandidateInfo info : candidateList) {
            SelectorWithWidgetPreference pref =
                    new SelectorWithWidgetPreference(getPrefContext());
            bindPreference(pref, info.getKey(), info, defaultKey);
            bindPreferenceExtra(pref, info.getKey(), info, defaultKey, systemDefaultKey);
            screen.addPreference(pref);
        }
        mayCheckOnlyRadioButton();
    }

    @Override
    public void bindPreferenceExtra(SelectorWithWidgetPreference pref,
            String key, CandidateInfo info, String defaultKey, String systemDefaultKey) {
        if (!(info instanceof CandidateInfoExtra)) {
            return;
        }

        pref.setSummary(((CandidateInfoExtra) info).loadSummary());

        if (KEY_SYSTEM_NAV_GESTURAL.equals(info.getKey())) {
            pref.setExtraWidgetOnClickListener((v) -> startActivity(new Intent(
                    GestureNavigationSettingsFragment.GESTURE_NAVIGATION_SETTINGS)));
        }

        if (KEY_SYSTEM_NAV_2BUTTONS.equals(info.getKey()) || KEY_SYSTEM_NAV_3BUTTONS.equals(
                info.getKey())) {
            pref.setExtraWidgetOnClickListener((v) ->
                    new SubSettingLauncher(getContext())
                            .setDestination(ButtonNavigationSettingsFragment.class.getName())
                            .setSourceMetricsCategory(SettingsEnums.SETTINGS_GESTURE_SWIPE_UP)
                            .launch());
        }
    }

    @Override
    protected int getPreferenceScreenResId() {
        return R.xml.system_navigation_gesture_settings;
    }

    @Override
    protected List<? extends CandidateInfo> getCandidates() {
        final Context c = getContext();
        List<CandidateInfoExtra> candidates = new ArrayList<>();

        if (SystemNavigationPreferenceController.isOverlayPackageAvailable(c,
                NAV_BAR_MODE_GESTURAL_OVERLAY)) {
            candidates.add(new CandidateInfoExtra(
                    c.getText(R.string.edge_to_edge_navigation_title),
                    c.getText(R.string.edge_to_edge_navigation_summary),
                    KEY_SYSTEM_NAV_GESTURAL, true /* enabled */));
        }
        if (SystemNavigationPreferenceController.isOverlayPackageAvailable(c,
                NAV_BAR_MODE_2BUTTON_OVERLAY)) {
            candidates.add(new CandidateInfoExtra(
                    c.getText(R.string.swipe_up_to_switch_apps_title),
                    c.getText(R.string.swipe_up_to_switch_apps_summary),
                    KEY_SYSTEM_NAV_2BUTTONS, true /* enabled */));
        }
        if (SystemNavigationPreferenceController.isOverlayPackageAvailable(c,
                NAV_BAR_MODE_3BUTTON_OVERLAY)) {
            candidates.add(new CandidateInfoExtra(
                    c.getText(R.string.legacy_navigation_title),
                    c.getText(R.string.legacy_navigation_summary),
                    KEY_SYSTEM_NAV_3BUTTONS, true /* enabled */));
        }

        return candidates;
    }

    @Override
    protected String getDefaultKey() {
        return getCurrentSystemNavigationMode(getContext());
    }

    @Override
    protected boolean setDefaultKey(String key) {
        setCurrentSystemNavigationMode(mOverlayManager, key);
        setIllustrationVideo(mVideoPreference, key);
        setGestureNavigationTutorialDialog(key);
        setIllustrationClickListener(mVideoPreference, key);
        return true;
    }

    private boolean isGestureTutorialAvailable() {
        Context context = getContext();
        return context != null
                && mLaunchSandboxIntent.resolveActivity(context.getPackageManager()) != null;
    }

    private void setIllustrationClickListener(IllustrationPreference videoPref,
            String systemNavKey) {

        switch (systemNavKey) {
            case KEY_SYSTEM_NAV_GESTURAL:
                if (isGestureTutorialAvailable()){
                    videoPref.setOnPreferenceClickListener(preference -> {
                        startActivity(mLaunchSandboxIntent);
                        return true;
                    });
                } else {
                    videoPref.setOnPreferenceClickListener(null);
                }

                break;
            case KEY_SYSTEM_NAV_2BUTTONS:
            case KEY_SYSTEM_NAV_3BUTTONS:
            default:
                videoPref.setOnPreferenceClickListener(null);
                break;
        }
    }

    static void migrateOverlaySensitivityToSettings(Context context,
            IOverlayManager overlayManager) {
        if (!SystemNavigationPreferenceController.isGestureNavigationEnabled(context)) {
            return;
        }

        OverlayInfo info = null;
        try {
            info = overlayManager.getOverlayInfo(NAV_BAR_MODE_GESTURAL_OVERLAY, USER_CURRENT);
        } catch (RemoteException e) { /* Do nothing */ }
        if (info != null && !info.isEnabled()) {
            // Enable the default gesture nav overlay. Back sensitivity for left and right are
            // stored as separate settings values, and other gesture nav overlays are deprecated.
            setCurrentSystemNavigationMode(overlayManager, KEY_SYSTEM_NAV_GESTURAL);
            Settings.Secure.putFloat(context.getContentResolver(),
                    Settings.Secure.BACK_GESTURE_INSET_SCALE_LEFT, 1.0f);
            Settings.Secure.putFloat(context.getContentResolver(),
                    Settings.Secure.BACK_GESTURE_INSET_SCALE_RIGHT, 1.0f);
        }
    }

    @VisibleForTesting
    static String getCurrentSystemNavigationMode(Context context) {
        if (SystemNavigationPreferenceController.isGestureNavigationEnabled(context)) {
            return KEY_SYSTEM_NAV_GESTURAL;
        } else if (SystemNavigationPreferenceController.is2ButtonNavigationEnabled(context)) {
            return KEY_SYSTEM_NAV_2BUTTONS;
        } else {
            return KEY_SYSTEM_NAV_3BUTTONS;
        }
    }

    @VisibleForTesting
    static void setCurrentSystemNavigationMode(IOverlayManager overlayManager, String key) {
        String overlayPackage = NAV_BAR_MODE_GESTURAL_OVERLAY;
        switch (key) {
            case KEY_SYSTEM_NAV_GESTURAL:
                overlayPackage = NAV_BAR_MODE_GESTURAL_OVERLAY;
                break;
            case KEY_SYSTEM_NAV_2BUTTONS:
                overlayPackage = NAV_BAR_MODE_2BUTTON_OVERLAY;
                break;
            case KEY_SYSTEM_NAV_3BUTTONS:
                overlayPackage = NAV_BAR_MODE_3BUTTON_OVERLAY;
                break;
        }

        try {
            overlayManager.setEnabledExclusiveInCategory(overlayPackage, USER_CURRENT);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    private void setIllustrationVideo(IllustrationPreference videoPref,
            String systemNavKey) {
        switch (systemNavKey) {
            case KEY_SYSTEM_NAV_GESTURAL:
                if (isGestureTutorialAvailable()) {
                    videoPref.setLottieAnimationResId(
                            R.raw.lottie_system_nav_fully_gestural_with_nav);
                } else {
                    videoPref.setLottieAnimationResId(R.raw.lottie_system_nav_fully_gestural);
                }
                break;
            case KEY_SYSTEM_NAV_2BUTTONS:
                videoPref.setLottieAnimationResId(R.raw.lottie_system_nav_2_button);
                break;
            case KEY_SYSTEM_NAV_3BUTTONS:
                videoPref.setLottieAnimationResId(R.raw.lottie_system_nav_3_button);
                break;
        }
    }

    private void setGestureNavigationTutorialDialog(String systemNavKey) {
        if (TextUtils.equals(KEY_SYSTEM_NAV_GESTURAL, systemNavKey)
                && !isAccessibilityFloatingMenuEnabled()
                && (isAnyServiceSupportAccessibilityButton() || isNavBarMagnificationEnabled())) {
            mA11yTutorialDialogShown = true;
            AccessibilityGestureNavigationTutorial.showGestureNavigationTutorialDialog(getContext(),
                    dialog -> mA11yTutorialDialogShown = false);
        } else {
            mA11yTutorialDialogShown = false;
        }
    }

    private boolean isAnyServiceSupportAccessibilityButton() {
        final AccessibilityManager ams = getContext().getSystemService(AccessibilityManager.class);
        final List<String> targets = ams.getAccessibilityShortcutTargets(
                AccessibilityManager.ACCESSIBILITY_BUTTON);
        return !targets.isEmpty();
    }

    private boolean isNavBarMagnificationEnabled() {
        return Settings.Secure.getInt(getContext().getContentResolver(),
                Settings.Secure.ACCESSIBILITY_DISPLAY_MAGNIFICATION_NAVBAR_ENABLED, 0) == 1;
    }

    private boolean isAccessibilityFloatingMenuEnabled() {
        return Settings.Secure.getInt(getContext().getContentResolver(),
                Settings.Secure.ACCESSIBILITY_BUTTON_MODE, /* def= */ -1)
                == ACCESSIBILITY_BUTTON_MODE_FLOATING_MENU;
    }

    public static final BaseSearchIndexProvider SEARCH_INDEX_DATA_PROVIDER =
            new BaseSearchIndexProvider(R.xml.system_navigation_gesture_settings) {

                @Override
                protected boolean isPageSearchEnabled(Context context) {
                    return SystemNavigationPreferenceController.isGestureAvailable(context);
                }

                @Override
                public List<SearchIndexableRaw> getRawDataToIndex(Context context,
                        boolean enabled) {
                    final Resources res = context.getResources();
                    final List<SearchIndexableRaw> result = new ArrayList<>();

                    if (SystemNavigationPreferenceController.isOverlayPackageAvailable(context,
                            NAV_BAR_MODE_GESTURAL_OVERLAY)) {
                        SearchIndexableRaw data = new SearchIndexableRaw(context);
                        data.title = res.getString(R.string.edge_to_edge_navigation_title);
                        data.key = KEY_SYSTEM_NAV_GESTURAL;
                        result.add(data);
                    }

                    if (SystemNavigationPreferenceController.isOverlayPackageAvailable(context,
                            NAV_BAR_MODE_2BUTTON_OVERLAY)) {
                        SearchIndexableRaw data = new SearchIndexableRaw(context);
                        data.title = res.getString(R.string.swipe_up_to_switch_apps_title);
                        data.key = KEY_SYSTEM_NAV_2BUTTONS;
                        result.add(data);
                    }

                    if (SystemNavigationPreferenceController.isOverlayPackageAvailable(context,
                            NAV_BAR_MODE_3BUTTON_OVERLAY)) {
                        SearchIndexableRaw data = new SearchIndexableRaw(context);
                        data.title = res.getString(R.string.legacy_navigation_title);
                        data.key = KEY_SYSTEM_NAV_3BUTTONS;
                        data.keywords = res.getString(R.string.keywords_3_button_navigation);
                        result.add(data);
                    }

                    return result;
                }
            };

    // From HelpResourceProvider
    @Override
    public int getHelpResource() {
        // TODO(b/146001201): Replace with system navigation help page when ready.
        return R.string.help_uri_default;
    }
}
