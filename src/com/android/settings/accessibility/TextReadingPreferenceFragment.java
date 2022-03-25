/*
 * Copyright (C) 2022 The Android Open Source Project
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

import static com.android.settings.accessibility.TextReadingResetController.ResetStateListener;

import android.app.Dialog;
import android.app.settings.SettingsEnums;
import android.content.Context;
import android.content.DialogInterface;

import androidx.appcompat.app.AlertDialog;

import com.android.settings.R;
import com.android.settings.accessibility.AccessibilityDialogUtils.DialogEnums;
import com.android.settings.dashboard.DashboardFragment;
import com.android.settings.search.BaseSearchIndexProvider;
import com.android.settingslib.core.AbstractPreferenceController;
import com.android.settingslib.search.SearchIndexable;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Accessibility settings for adjusting the system features which are related to the reading. For
 * example, bold text, high contrast text, display size, font size and so on.
 */
@SearchIndexable(forTarget = SearchIndexable.ALL & ~SearchIndexable.ARC)
public class TextReadingPreferenceFragment extends DashboardFragment {
    private static final String TAG = "TextReadingPreferenceFragment";
    private static final String FONT_SIZE_KEY = "font_size";
    private static final String DISPLAY_SIZE_KEY = "display_size";
    private static final String PREVIEW_KEY = "preview";
    private static final String RESET_KEY = "reset";
    private static final String BOLD_TEXT_KEY = "toggle_force_bold_text";
    private static final String HIGHT_TEXT_CONTRAST_KEY = "toggle_high_text_contrast_preference";

    @Override
    protected int getPreferenceScreenResId() {
        return R.xml.accessibility_text_reading_options;
    }

    @Override
    protected String getLogTag() {
        return TAG;
    }

    @Override
    public int getMetricsCategory() {
        return SettingsEnums.ACCESSIBILITY_TEXT_READING_OPTIONS;
    }

    @Override
    protected List<AbstractPreferenceController> createPreferenceControllers(Context context) {
        final List<AbstractPreferenceController> controllers = new ArrayList<>();
        final FontSizeData fontSizeData = new FontSizeData(context);
        final DisplaySizeData displaySizeData = new DisplaySizeData(context);

        final TextReadingPreviewController previewController = new TextReadingPreviewController(
                context, PREVIEW_KEY, fontSizeData, displaySizeData);
        controllers.add(previewController);

        final PreviewSizeSeekBarController fontSizeController = new PreviewSizeSeekBarController(
                context, FONT_SIZE_KEY, fontSizeData);
        fontSizeController.setInteractionListener(previewController);
        controllers.add(fontSizeController);

        final PreviewSizeSeekBarController displaySizeController = new PreviewSizeSeekBarController(
                context, DISPLAY_SIZE_KEY, displaySizeData);
        displaySizeController.setInteractionListener(previewController);
        controllers.add(displaySizeController);

        final FontWeightAdjustmentPreferenceController fontWeightController =
                new FontWeightAdjustmentPreferenceController(context, BOLD_TEXT_KEY);
        controllers.add(fontWeightController);

        final HighTextContrastPreferenceController highTextContrastController =
                new HighTextContrastPreferenceController(context, HIGHT_TEXT_CONTRAST_KEY);
        controllers.add(highTextContrastController);

        final TextReadingResetController resetController =
                new TextReadingResetController(context, RESET_KEY,
                        v -> showDialog(DialogEnums.DIALOG_RESET_SETTINGS));
        controllers.add(resetController);

        return controllers;
    }

    @Override
    public Dialog onCreateDialog(int dialogId) {
        if (dialogId == DialogEnums.DIALOG_RESET_SETTINGS) {
            return new AlertDialog.Builder(getPrefContext())
                    .setTitle(R.string.accessibility_text_reading_confirm_dialog_title)
                    .setMessage(R.string.accessibility_text_reading_confirm_dialog_message)
                    .setPositiveButton(
                            R.string.accessibility_text_reading_confirm_dialog_reset_button,
                            this::onPositiveButtonClicked)
                    .setNegativeButton(R.string.cancel, /* listener= */ null)
                    .create();
        }

        throw new IllegalArgumentException("Unsupported dialogId " + dialogId);
    }

    @Override
    public int getDialogMetricsCategory(int dialogId) {
        if (dialogId == DialogEnums.DIALOG_RESET_SETTINGS) {
            return SettingsEnums.DIALOG_RESET_SETTINGS;
        }

        return super.getDialogMetricsCategory(dialogId);
    }

    private void onPositiveButtonClicked(DialogInterface dialog, int which) {
        // To avoid showing the dialog again, probably the onDetach() of SettingsDialogFragment
        // was interrupted by unexpectedly recreating the activity.
        removeDialog(DialogEnums.DIALOG_RESET_SETTINGS);

        getResetStateListeners().forEach(ResetStateListener::resetState);
    }

    private List<ResetStateListener> getResetStateListeners() {
        final List<AbstractPreferenceController> controllers = new ArrayList<>();
        getPreferenceControllers().forEach(controllers::addAll);
        return controllers.stream().filter(c -> c instanceof ResetStateListener).map(
                c -> (ResetStateListener) c).collect(Collectors.toList());
    }

    public static final BaseSearchIndexProvider SEARCH_INDEX_DATA_PROVIDER =
            new BaseSearchIndexProvider(R.xml.accessibility_text_reading_options);
}
