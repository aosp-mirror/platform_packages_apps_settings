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

import android.app.settings.SettingsEnums;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.annotation.VisibleForTesting;
import androidx.preference.PreferenceFragmentCompat;

import com.android.settings.R;
import com.android.settings.core.InstrumentedActivity;
import com.android.settings.display.FontSizePreferenceFragmentForSetupWizard;
import com.android.settings.display.ScreenZoomPreferenceFragmentForSetupWizard;

import com.google.android.setupcompat.template.FooterBarMixin;
import com.google.android.setupcompat.template.FooterButton;
import com.google.android.setupdesign.GlifLayout;
import com.google.android.setupdesign.util.ThemeHelper;

/** Settings font/display size activity for SUW. */
public class AccessibilityScreenSizeForSetupWizardActivity extends InstrumentedActivity {
    private static final String TAG = "ScreenSizeForSetup";

    // A parameter decides which fragment ({@link FontSizePreferenceFragmentForSetupWizard} or
    // {@link ScreenZoomPreferenceFragmentForSetupWizard}) will be visioned.
    static final String VISION_FRAGMENT_NO = "vision_fragment_no";

    private int mFragmentNo;
    private int mFontSizeFragmentNo;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mFontSizeFragmentNo = getResources().getInteger(R.integer.suw_font_size_fragment_no);
        final int appliedTheme = ThemeHelper.trySetDynamicColor(this)
                ? R.style.SudDynamicColorThemeGlifV3_DayNight : R.style.SudThemeGlifV3_DayNight;
        setTheme(appliedTheme);
        setContentView(R.layout.accessibility_screen_size_setup_wizard);
        mFragmentNo = getIntent().getExtras().getInt(VISION_FRAGMENT_NO);
        Log.d(TAG, "onCreate: fragment no: " + mFragmentNo);
        generateHeader(mFragmentNo);
        scrollToBottom();
        initFooterButton();
        if (savedInstanceState == null) {
            final PreferenceFragmentCompat fragment =
                    (mFragmentNo == mFontSizeFragmentNo)
                            ? new FontSizePreferenceFragmentForSetupWizard()
                            : new ScreenZoomPreferenceFragmentForSetupWizard();
            getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.content_frame, fragment)
                    .commit();
        }
    }

    @Override
    public int getMetricsCategory() {
        return mFragmentNo == mFontSizeFragmentNo ? SettingsEnums.SUW_ACCESSIBILITY_FONT_SIZE
                : SettingsEnums.SUW_ACCESSIBILITY_DISPLAY_SIZE;
    }

    @VisibleForTesting
    void generateHeader(int fragmentNo) {
        ((TextView) findViewById(R.id.suc_layout_title)).setText(
                fragmentNo == mFontSizeFragmentNo ? R.string.title_font_size
                        : R.string.screen_zoom_title);
        ((TextView) findViewById(R.id.sud_layout_subtitle)).setText(
                fragmentNo == mFontSizeFragmentNo ? R.string.short_summary_font_size
                        : R.string.screen_zoom_short_summary);
    }

    @VisibleForTesting
    void initFooterButton() {
        final GlifLayout layout = findViewById(R.id.setup_wizard_layout);
        final FooterBarMixin mixin = layout.getMixin(FooterBarMixin.class);
        final View.OnClickListener nextButtonListener =
                v -> {
                    onBackPressed();
                };
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
        final GlifLayout layout = findViewById(R.id.setup_wizard_layout);
        final ScrollView scrollView = layout.getScrollView();
        scrollView.getViewTreeObserver().addOnGlobalLayoutListener(() -> {
            final int scrollViewHeight = scrollView.getHeight();
            if (scrollViewHeight > 0) {
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
}
