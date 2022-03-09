/*
 * Copyright (C) 2016 The Android Open Source Project
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

import static com.android.settings.accessibility.AccessibilityScreenSizeForSetupWizardActivity.VISION_FRAGMENT_NO;

import android.content.ComponentName;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.accessibility.AccessibilityEvent;

import androidx.annotation.VisibleForTesting;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;

import com.android.settings.R;
import com.android.settings.SettingsActivity;
import com.android.settings.SetupWizardUtils;
import com.android.settings.accessibility.AccessibilityScreenSizeForSetupWizardActivity.FragmentType;
import com.android.settings.core.SubSettingLauncher;
import com.android.settings.search.actionbar.SearchMenuController;
import com.android.settings.support.actionbar.HelpResourceProvider;
import com.android.settingslib.core.instrumentation.Instrumentable;
import com.android.settingslib.transition.SettingsTransitionHelper;

import com.google.android.setupcompat.util.WizardManagerHelper;
import com.google.android.setupdesign.util.ThemeHelper;

public class AccessibilitySettingsForSetupWizardActivity extends SettingsActivity {

    private static final String LOG_TAG = "A11ySettingsForSUW";
    private static final String SAVE_KEY_TITLE = "activity_title";

    @VisibleForTesting
    static final String CLASS_NAME_FONT_SIZE_SETTINGS_FOR_SUW =
            "com.android.settings.FontSizeSettingsForSetupWizardActivity";

    @Override
    protected void onSaveInstanceState(Bundle savedState) {
        savedState.putCharSequence(SAVE_KEY_TITLE, getTitle());
        super.onSaveInstanceState(savedState);
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedState) {
        super.onRestoreInstanceState(savedState);
        setTitle(savedState.getCharSequence(SAVE_KEY_TITLE));
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Return true, so we get notified when items in the menu are clicked.
        return true;
    }

    @Override
    public boolean onNavigateUp() {
        onBackPressed();

        // Clear accessibility focus and let the screen reader announce the new title.
        getWindow().getDecorView()
                .sendAccessibilityEvent(AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED);

        return true;
    }

    @Override
    public boolean onPreferenceStartFragment(PreferenceFragmentCompat caller, Preference pref) {
        Bundle args = pref.getExtras();
        if (args == null) {
            args = new Bundle();
        }
        args.putInt(HelpResourceProvider.HELP_URI_RESOURCE_KEY, 0);
        args.putBoolean(SearchMenuController.NEED_SEARCH_ICON_IN_ACTION_BAR, false);
        new SubSettingLauncher(this)
                .setDestination(pref.getFragment())
                .setArguments(args)
                .setSourceMetricsCategory(caller instanceof Instrumentable
                        ? ((Instrumentable) caller).getMetricsCategory()
                        : Instrumentable.METRICS_CATEGORY_UNKNOWN)
                .setExtras(SetupWizardUtils.copyLifecycleExtra(getIntent().getExtras(),
                        new Bundle()))
                .setTransitionType(SettingsTransitionHelper.TransitionType.TRANSITION_FADE)
                .launch();
        return true;
    }

    @Override
    protected void onCreate(Bundle savedState) {
        super.onCreate(savedState);
        applyTheme();
        tryLaunchFontSizeSettings();
        findViewById(R.id.content_parent).setFitsSystemWindows(false);
    }

    private void applyTheme() {
        if (ThemeHelper.trySetDynamicColor(this)) {
            final int appliedTheme = ThemeHelper.isSetupWizardDayNightEnabled(this)
                    ? R.style.SudDynamicColorThemeSettings_SetupWizard_DayNight
                    : R.style.SudDynamicColorThemeSettings_SetupWizard;
            setTheme(appliedTheme);
        } else {
            setTheme(SetupWizardUtils.getTheme(this, getIntent()));
        }
    }

    @VisibleForTesting
    void tryLaunchFontSizeSettings() {
        if (WizardManagerHelper.isAnySetupWizard(getIntent())
                && new ComponentName(getPackageName(),
                CLASS_NAME_FONT_SIZE_SETTINGS_FOR_SUW).equals(
                getIntent().getComponent())) {
            final Intent intent = new Intent(this,
                    AccessibilityScreenSizeForSetupWizardActivity.class);
            intent.putExtra(VISION_FRAGMENT_NO, FragmentType.FONT_SIZE);
            startActivity(intent);
            Log.d(LOG_TAG, "Launch font size settings");
            finish();
        }
    }
}
