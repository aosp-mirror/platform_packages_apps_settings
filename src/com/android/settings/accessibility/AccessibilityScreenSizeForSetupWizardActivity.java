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

import static com.android.settings.core.SettingsBaseActivity.EXTRA_PAGE_TRANSITION_TYPE;

import android.app.settings.SettingsEnums;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.annotation.IntDef;
import androidx.annotation.VisibleForTesting;
import androidx.preference.PreferenceFragmentCompat;

import com.android.settings.R;
import com.android.settings.core.InstrumentedActivity;
import com.android.settings.display.FontSizePreferenceFragmentForSetupWizard;
import com.android.settings.display.ScreenZoomPreferenceFragmentForSetupWizard;
import com.android.settingslib.transition.SettingsTransitionHelper.TransitionType;

import com.google.android.setupcompat.template.FooterBarMixin;
import com.google.android.setupcompat.template.FooterButton;
import com.google.android.setupdesign.GlifLayout;
import com.google.android.setupdesign.util.ThemeHelper;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/** Settings font/display size activity for SUW. */
public class AccessibilityScreenSizeForSetupWizardActivity extends InstrumentedActivity {
    private static final String TAG = "ScreenSizeForSetup";

    // A parameter decides which fragment ({@link FontSizePreferenceFragmentForSetupWizard} or
    // {@link ScreenZoomPreferenceFragmentForSetupWizard}) will be visioned.
    static final String VISION_FRAGMENT_NO = "vision_fragment_no";
    /**
     * Flags indicating the type of the fragment.
     */
    @IntDef({
        FragmentType.FONT_SIZE,
        FragmentType.SCREEN_SIZE,
    })
    @Retention(RetentionPolicy.SOURCE)
    public @interface FragmentType {
        int FONT_SIZE = 1;
        int SCREEN_SIZE = 2;
    }

    // Keep the last height of the scroll view in the {@link GlifLayout}
    private int mLastScrollViewHeight;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        final int appliedTheme = ThemeHelper.trySetDynamicColor(this)
                ? R.style.SudDynamicColorThemeGlifV3_DayNight : R.style.SudThemeGlifV3_DayNight;
        setTheme(appliedTheme);
        setContentView(R.layout.accessibility_screen_size_setup_wizard);
        updateHeaderLayout();
        scrollToBottom();
        initFooterButton();
        if (savedInstanceState == null) {
            final PreferenceFragmentCompat fragment =
                    getFragmentType(getIntent()) == FragmentType.FONT_SIZE
                            ? new FontSizePreferenceFragmentForSetupWizard()
                            : new ScreenZoomPreferenceFragmentForSetupWizard();
            getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.content_frame, fragment)
                    .commit();
        }
    }

    @Override
    protected void onPause() {
        // For accessibility activities launched from setup wizard.
        if (getTransitionType(getIntent()) == TransitionType.TRANSITION_FADE) {
            overridePendingTransition(R.anim.sud_stay, android.R.anim.fade_out);
        }
        super.onPause();
    }

    @Override
    public int getMetricsCategory() {
        return getFragmentType(getIntent()) == FragmentType.FONT_SIZE
                ? SettingsEnums.SUW_ACCESSIBILITY_FONT_SIZE
                : SettingsEnums.SUW_ACCESSIBILITY_DISPLAY_SIZE;
    }

    @VisibleForTesting
    void updateHeaderLayout() {
        if (ThemeHelper.shouldApplyExtendedPartnerConfig(this) && isSuwSupportedTwoPanes()) {
            final GlifLayout layout = findViewById(R.id.setup_wizard_layout);
            final LinearLayout headerLayout = layout.findManagedViewById(R.id.sud_layout_header);
            if (headerLayout != null) {
                headerLayout.setPadding(0, layout.getPaddingTop(), 0,
                        layout.getPaddingBottom());
            }
        }
        ((TextView) findViewById(R.id.suc_layout_title)).setText(
                getFragmentType(getIntent()) == FragmentType.FONT_SIZE
                        ? R.string.title_font_size
                        : R.string.screen_zoom_title);
        ((TextView) findViewById(R.id.sud_layout_subtitle)).setText(
                getFragmentType(getIntent()) == FragmentType.FONT_SIZE
                        ? R.string.short_summary_font_size
                        : R.string.screen_zoom_short_summary);
    }

    private boolean isSuwSupportedTwoPanes() {
        return getResources().getBoolean(R.bool.config_suw_supported_two_panes);
    }

    private void initFooterButton() {
        final GlifLayout layout = findViewById(R.id.setup_wizard_layout);
        final FooterBarMixin mixin = layout.getMixin(FooterBarMixin.class);
        final View.OnClickListener nextButtonListener = v -> onBackPressed();
        final FooterButton primaryButton =
                new FooterButton.Builder(this)
                        .setText(R.string.done)
                        .setListener(nextButtonListener)
                        .setButtonType(FooterButton.ButtonType.NEXT)
                        .setTheme(R.style.SudGlifButton_Primary)
                        .build();
        mixin.setPrimaryButton(primaryButton);
    }

    /**
     * Scrolls to bottom while {@link ScrollView} layout changed.
     */
    private void scrollToBottom() {
        mLastScrollViewHeight = 0;
        final GlifLayout layout = findViewById(R.id.setup_wizard_layout);
        final ScrollView scrollView = layout.getScrollView();
        scrollView.getViewTreeObserver().addOnGlobalLayoutListener(() -> {
            final int scrollViewHeight = scrollView.getHeight();
            if (scrollViewHeight > 0 && scrollViewHeight != mLastScrollViewHeight) {
                mLastScrollViewHeight = scrollViewHeight;
                scrollView.post(() -> {
                    // Here is no need to show the scrolling animation. So disabled first and
                    // then enabled it after scrolling finished.
                    scrollView.setSmoothScrollingEnabled(false);
                    scrollView.fullScroll(View.FOCUS_DOWN);
                    scrollView.setSmoothScrollingEnabled(true);
                });
            }
        });
    }

    private int getTransitionType(Intent intent) {
        return intent.getIntExtra(EXTRA_PAGE_TRANSITION_TYPE, TransitionType.TRANSITION_NONE);
    }

    private int getFragmentType(Intent intent) {
        return intent.getIntExtra(VISION_FRAGMENT_NO, FragmentType.FONT_SIZE);
    }
}
