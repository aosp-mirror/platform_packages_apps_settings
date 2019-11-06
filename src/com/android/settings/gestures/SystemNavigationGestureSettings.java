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
import static android.view.WindowManagerPolicyConstants.NAV_BAR_MODE_2BUTTON_OVERLAY;
import static android.view.WindowManagerPolicyConstants.NAV_BAR_MODE_3BUTTON_OVERLAY;
import static android.view.WindowManagerPolicyConstants.NAV_BAR_MODE_GESTURAL_OVERLAY;

import static com.android.settings.widget.RadioButtonPreferenceWithExtraWidget.EXTRA_WIDGET_VISIBILITY_GONE;
import static com.android.settings.widget.RadioButtonPreferenceWithExtraWidget.EXTRA_WIDGET_VISIBILITY_SETTING;

import android.accessibilityservice.AccessibilityServiceInfo;
import android.app.settings.SettingsEnums;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.om.IOverlayManager;
import android.content.om.OverlayInfo;
import android.graphics.drawable.Drawable;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.provider.SearchIndexableResource;
import android.provider.Settings;
import android.text.TextUtils;
import android.view.accessibility.AccessibilityManager;

import androidx.annotation.VisibleForTesting;
import androidx.preference.PreferenceScreen;

import com.android.settings.SettingsTutorialDialogWrapperActivity;
import com.android.settings.R;
import com.android.settings.dashboard.suggestions.SuggestionFeatureProvider;
import com.android.settings.overlay.FeatureFactory;
import com.android.settings.search.BaseSearchIndexProvider;
import com.android.settings.search.Indexable;
import com.android.settings.widget.RadioButtonPickerFragment;
import com.android.settings.widget.RadioButtonPreference;
import com.android.settings.widget.RadioButtonPreferenceWithExtraWidget;
import com.android.settings.widget.VideoPreference;
import com.android.settingslib.search.SearchIndexable;
import com.android.settingslib.widget.CandidateInfo;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@SearchIndexable
public class SystemNavigationGestureSettings extends RadioButtonPickerFragment {

    private static final String TAG = "SystemNavigationGesture";

    @VisibleForTesting
    static final String SHARED_PREFERENCES_NAME = "system_navigation_settings_preferences";
    @VisibleForTesting
    static final String PREFS_BACK_SENSITIVITY_KEY = "system_navigation_back_sensitivity";

    @VisibleForTesting
    static final String KEY_SYSTEM_NAV_3BUTTONS = "system_nav_3buttons";
    @VisibleForTesting
    static final String KEY_SYSTEM_NAV_2BUTTONS = "system_nav_2buttons";
    @VisibleForTesting
    static final String KEY_SYSTEM_NAV_GESTURAL = "system_nav_gestural";

    public static final String PREF_KEY_SUGGESTION_COMPLETE =
            "pref_system_navigation_suggestion_complete";

    @VisibleForTesting
    static final String NAV_BAR_MODE_GESTURAL_OVERLAY_NARROW_BACK
            = "com.android.internal.systemui.navbar.gestural_narrow_back";
    @VisibleForTesting
    static final String NAV_BAR_MODE_GESTURAL_OVERLAY_WIDE_BACK
            = "com.android.internal.systemui.navbar.gestural_wide_back";
    @VisibleForTesting
    static final String NAV_BAR_MODE_GESTURAL_OVERLAY_EXTRA_WIDE_BACK
            = "com.android.internal.systemui.navbar.gestural_extra_wide_back";
    @VisibleForTesting
    static final String[] BACK_GESTURE_INSET_OVERLAYS = {
            NAV_BAR_MODE_GESTURAL_OVERLAY_NARROW_BACK,
            NAV_BAR_MODE_GESTURAL_OVERLAY,
            NAV_BAR_MODE_GESTURAL_OVERLAY_WIDE_BACK,
            NAV_BAR_MODE_GESTURAL_OVERLAY_EXTRA_WIDE_BACK
    };
    @VisibleForTesting
    static int BACK_GESTURE_INSET_DEFAULT_OVERLAY = 1;

    private IOverlayManager mOverlayManager;

    private VideoPreference mVideoPreference;

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        SuggestionFeatureProvider suggestionFeatureProvider = FeatureFactory.getFactory(context)
                .getSuggestionFeatureProvider(context);
        SharedPreferences prefs = suggestionFeatureProvider.getSharedPrefs(context);
        prefs.edit().putBoolean(PREF_KEY_SUGGESTION_COMPLETE, true).apply();

        mOverlayManager = IOverlayManager.Stub.asInterface(
                ServiceManager.getService(Context.OVERLAY_SERVICE));

        mVideoPreference = new VideoPreference(context);
        setIllustrationVideo(mVideoPreference, getDefaultKey());
        mVideoPreference.setHeight( /* Illustration height in dp */
                getResources().getDimension(R.dimen.system_navigation_illustration_height)
                        / getResources().getDisplayMetrics().density);
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
            RadioButtonPreferenceWithExtraWidget pref =
                    new RadioButtonPreferenceWithExtraWidget(getPrefContext());
            bindPreference(pref, info.getKey(), info, defaultKey);
            bindPreferenceExtra(pref, info.getKey(), info, defaultKey, systemDefaultKey);
            screen.addPreference(pref);
        }
        mayCheckOnlyRadioButton();
    }

    @Override
    public void bindPreferenceExtra(RadioButtonPreference pref,
            String key, CandidateInfo info, String defaultKey, String systemDefaultKey) {
        if (!(info instanceof NavModeCandidateInfo)
                || !(pref instanceof RadioButtonPreferenceWithExtraWidget)) {
            return;
        }

        pref.setSummary(((NavModeCandidateInfo) info).loadSummary());

        RadioButtonPreferenceWithExtraWidget p = (RadioButtonPreferenceWithExtraWidget) pref;
        if (info.getKey() == KEY_SYSTEM_NAV_GESTURAL) {
            p.setExtraWidgetVisibility(EXTRA_WIDGET_VISIBILITY_SETTING);
            p.setExtraWidgetOnClickListener((v) -> GestureNavigationBackSensitivityDialog
                    .show(this, getBackSensitivity(getContext(), mOverlayManager)));
        } else {
            p.setExtraWidgetVisibility(EXTRA_WIDGET_VISIBILITY_GONE);
        }
    }

    @Override
    protected int getPreferenceScreenResId() {
        return R.xml.system_navigation_gesture_settings;
    }

    @Override
    protected List<? extends CandidateInfo> getCandidates() {
        final Context c = getContext();
        List<NavModeCandidateInfo> candidates = new ArrayList<>();

        if (SystemNavigationPreferenceController.isOverlayPackageAvailable(c,
                NAV_BAR_MODE_GESTURAL_OVERLAY)) {
            candidates.add(new NavModeCandidateInfo(
                    c.getText(R.string.edge_to_edge_navigation_title),
                    c.getText(R.string.edge_to_edge_navigation_summary),
                    KEY_SYSTEM_NAV_GESTURAL, true /* enabled */));
        }
        if (SystemNavigationPreferenceController.isOverlayPackageAvailable(c,
                NAV_BAR_MODE_2BUTTON_OVERLAY)) {
            candidates.add(new NavModeCandidateInfo(
                    c.getText(R.string.swipe_up_to_switch_apps_title),
                    c.getText(R.string.swipe_up_to_switch_apps_summary),
                    KEY_SYSTEM_NAV_2BUTTONS, true /* enabled */));
        }
        if (SystemNavigationPreferenceController.isOverlayPackageAvailable(c,
                NAV_BAR_MODE_3BUTTON_OVERLAY)) {
            candidates.add(new NavModeCandidateInfo(
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
        final Context c = getContext();
        setCurrentSystemNavigationMode(c, mOverlayManager, key);
        setIllustrationVideo(mVideoPreference, key);
        if (TextUtils.equals(KEY_SYSTEM_NAV_GESTURAL, key) && (
                isAnyServiceSupportAccessibilityButton() || isNavBarMagnificationEnabled())) {
            Intent intent = new Intent(getActivity(), SettingsTutorialDialogWrapperActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
        }
        return true;
    }

    @VisibleForTesting
    static void setBackSensitivity(Context context, IOverlayManager overlayManager,
            int sensitivity) {
        if (sensitivity < 0 || sensitivity >= BACK_GESTURE_INSET_OVERLAYS.length) {
            throw new IllegalArgumentException("Sensitivity out of range.");
        }

        // Store the sensitivity level, to be able to restore when user returns to Gesture Nav mode
        context.getSharedPreferences(SHARED_PREFERENCES_NAME, Context.MODE_PRIVATE).edit()
                .putInt(PREFS_BACK_SENSITIVITY_KEY, sensitivity).apply();
        if (getCurrentSystemNavigationMode(context) == KEY_SYSTEM_NAV_GESTURAL) {
            setNavBarInteractionMode(overlayManager, BACK_GESTURE_INSET_OVERLAYS[sensitivity]);
        }
    }

    @VisibleForTesting
    static int getBackSensitivity(Context context, IOverlayManager overlayManager) {
        for (int i = 0; i < BACK_GESTURE_INSET_OVERLAYS.length; i++) {
            OverlayInfo info = null;
            try {
                info = overlayManager.getOverlayInfo(BACK_GESTURE_INSET_OVERLAYS[i], USER_CURRENT);
            } catch (RemoteException e) { /* Do nothing */ }
            if (info != null && info.isEnabled()) {
                return i;
            }
        }
        // If Gesture nav is not selected, read the value from shared preferences.
        return context.getSharedPreferences(SHARED_PREFERENCES_NAME, Context.MODE_PRIVATE)
                .getInt(PREFS_BACK_SENSITIVITY_KEY, BACK_GESTURE_INSET_DEFAULT_OVERLAY);
    }

    @VisibleForTesting
    static String getCurrentSystemNavigationMode(Context context) {
        if (SystemNavigationPreferenceController.isEdgeToEdgeEnabled(context)) {
            return KEY_SYSTEM_NAV_GESTURAL;
        } else if (SystemNavigationPreferenceController.isSwipeUpEnabled(context)) {
            return KEY_SYSTEM_NAV_2BUTTONS;
        } else {
            return KEY_SYSTEM_NAV_3BUTTONS;
        }
    }

    @VisibleForTesting
    static void setCurrentSystemNavigationMode(Context context, IOverlayManager overlayManager,
            String key) {
        switch (key) {
            case KEY_SYSTEM_NAV_GESTURAL:
                int sensitivity = getBackSensitivity(context, overlayManager);
                setNavBarInteractionMode(overlayManager, BACK_GESTURE_INSET_OVERLAYS[sensitivity]);
                break;
            case KEY_SYSTEM_NAV_2BUTTONS:
                setNavBarInteractionMode(overlayManager, NAV_BAR_MODE_2BUTTON_OVERLAY);
                break;
            case KEY_SYSTEM_NAV_3BUTTONS:
                setNavBarInteractionMode(overlayManager, NAV_BAR_MODE_3BUTTON_OVERLAY);
                break;
        }
    }

    private static void setNavBarInteractionMode(IOverlayManager overlayManager,
            String overlayPackage) {
        try {
            overlayManager.setEnabledExclusiveInCategory(overlayPackage, USER_CURRENT);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    private static void setIllustrationVideo(VideoPreference videoPref, String systemNavKey) {
        videoPref.setVideo(0, 0);
        switch (systemNavKey) {
            case KEY_SYSTEM_NAV_GESTURAL:
                videoPref.setVideo(R.raw.system_nav_fully_gestural,
                        R.drawable.system_nav_fully_gestural);
                break;
            case KEY_SYSTEM_NAV_2BUTTONS:
                videoPref.setVideo(R.raw.system_nav_2_button, R.drawable.system_nav_2_button);
                break;
            case KEY_SYSTEM_NAV_3BUTTONS:
                videoPref.setVideo(R.raw.system_nav_3_button, R.drawable.system_nav_3_button);
                break;
        }
    }

    private boolean isAnyServiceSupportAccessibilityButton() {
        final AccessibilityManager ams = (AccessibilityManager) getContext().getSystemService(
                Context.ACCESSIBILITY_SERVICE);
        final List<AccessibilityServiceInfo> services = ams.getEnabledAccessibilityServiceList(
                AccessibilityServiceInfo.FEEDBACK_ALL_MASK);

        for (AccessibilityServiceInfo info : services) {
            if ((info.flags & AccessibilityServiceInfo.FLAG_REQUEST_ACCESSIBILITY_BUTTON) != 0) {
                return true;
            }
        }

        return false;
    }

    private boolean isNavBarMagnificationEnabled() {
        return Settings.Secure.getInt(getContext().getContentResolver(),
                Settings.Secure.ACCESSIBILITY_DISPLAY_MAGNIFICATION_NAVBAR_ENABLED, 0) == 1;
    }

    static class NavModeCandidateInfo extends CandidateInfo {
        private final CharSequence mLabel;
        private final CharSequence mSummary;
        private final String mKey;

        NavModeCandidateInfo(CharSequence label, CharSequence summary, String key,
                boolean enabled) {
            super(enabled);
            mLabel = label;
            mSummary = summary;
            mKey = key;
        }

        @Override
        public CharSequence loadLabel() {
            return mLabel;
        }

        public CharSequence loadSummary() {
            return mSummary;
        }

        @Override
        public Drawable loadIcon() {
            return null;
        }

        @Override
        public String getKey() {
            return mKey;
        }
    }

    public static final Indexable.SearchIndexProvider SEARCH_INDEX_DATA_PROVIDER =
            new BaseSearchIndexProvider() {
                @Override
                public List<SearchIndexableResource> getXmlResourcesToIndex(
                        Context context, boolean enabled) {
                    final SearchIndexableResource sir = new SearchIndexableResource(context);
                    sir.xmlResId = R.xml.system_navigation_gesture_settings;
                    return Arrays.asList(sir);
                }

                @Override
                protected boolean isPageSearchEnabled(Context context) {
                    return SystemNavigationPreferenceController.isGestureAvailable(context);
                }
            };
}
