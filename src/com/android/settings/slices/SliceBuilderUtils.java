/*
 * Copyright (C) 2017 The Android Open Source Project
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
 * limitations under the License
 */

package com.android.settings.slices;

import static com.android.settings.core.BasePreferenceController.DISABLED_DEPENDENT_SETTING;
import static com.android.settings.slices.SettingsSliceProvider.EXTRA_SLICE_KEY;

import android.annotation.ColorInt;
import android.app.PendingIntent;
import android.app.settings.SettingsEnums;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.SettingsSlicesContract;
import android.text.TextUtils;
import android.util.ArraySet;
import android.util.Log;
import android.util.Pair;

import androidx.annotation.VisibleForTesting;
import androidx.core.graphics.drawable.IconCompat;
import androidx.slice.Slice;
import androidx.slice.builders.ListBuilder;
import androidx.slice.builders.ListBuilder.InputRangeBuilder;
import androidx.slice.builders.ListBuilder.RowBuilder;
import androidx.slice.builders.SliceAction;

import com.android.settings.R;
import com.android.settings.SettingsActivity;
import com.android.settings.SubSettings;
import com.android.settings.Utils;
import com.android.settings.core.BasePreferenceController;
import com.android.settings.core.SliderPreferenceController;
import com.android.settings.core.SubSettingLauncher;
import com.android.settings.core.TogglePreferenceController;
import com.android.settings.overlay.FeatureFactory;
import com.android.settingslib.core.AbstractPreferenceController;

import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;


/**
 * Utility class to build Slices objects and Preference Controllers based on the Database managed
 * by {@link SlicesDatabaseHelper}
 */
public class SliceBuilderUtils {

    private static final String TAG = "SliceBuilder";

    /**
     * Build a Slice from {@link SliceData}.
     *
     * @return a {@link Slice} based on the data provided by {@param sliceData}.
     * Will build an {@link Intent} based Slice unless the Preference Controller name in
     * {@param sliceData} is an inline controller.
     */
    public static Slice buildSlice(Context context, SliceData sliceData) {
        Log.d(TAG, "Creating slice for: " + sliceData.getPreferenceController());
        final BasePreferenceController controller = getPreferenceController(context, sliceData);
        FeatureFactory.getFactory(context).getMetricsFeatureProvider()
                .action(SettingsEnums.PAGE_UNKNOWN,
                        SettingsEnums.ACTION_SETTINGS_SLICE_REQUESTED,
                        SettingsEnums.PAGE_UNKNOWN,
                        sliceData.getKey(),
                        0);

        if (!controller.isAvailable()) {
            // Cannot guarantee setting page is accessible, let the presenter handle error case.
            return null;
        }

        if (controller.getAvailabilityStatus() == DISABLED_DEPENDENT_SETTING) {
            return buildUnavailableSlice(context, sliceData);
        }

        if (controller.isCopyableSlice()) {
            return buildCopyableSlice(context, sliceData, controller);
        }

        switch (sliceData.getSliceType()) {
            case SliceData.SliceType.INTENT:
                return buildIntentSlice(context, sliceData, controller);
            case SliceData.SliceType.SWITCH:
                return buildToggleSlice(context, sliceData, controller);
            case SliceData.SliceType.SLIDER:
                return buildSliderSlice(context, sliceData, controller);
            default:
                throw new IllegalArgumentException(
                        "Slice type passed was invalid: " + sliceData.getSliceType());
        }
    }

    /**
     * Splits the Settings Slice Uri path into its two expected components:
     * - intent/action
     * - key
     * <p>
     * Examples of valid paths are:
     * - /intent/wifi
     * - /intent/bluetooth
     * - /action/wifi
     * - /action/accessibility/servicename
     *
     * @param uri of the Slice. Follows pattern outlined in {@link SettingsSliceProvider}.
     * @return Pair whose first element {@code true} if the path is prepended with "intent", and
     * second is a key.
     */
    public static Pair<Boolean, String> getPathData(Uri uri) {
        final String path = uri.getPath();
        final String[] split = path.split("/", 3);

        // Split should be: [{}, SLICE_TYPE, KEY].
        // Example: "/action/wifi" -> [{}, "action", "wifi"]
        //          "/action/longer/path" -> [{}, "action", "longer/path"]
        if (split.length != 3) {
            return null;
        }

        final boolean isIntent = TextUtils.equals(SettingsSlicesContract.PATH_SETTING_INTENT,
                split[1]);

        return new Pair<>(isIntent, split[2]);
    }

    /**
     * Looks at the controller classname in in {@link SliceData} from {@param sliceData}
     * and attempts to build an {@link AbstractPreferenceController}.
     */
    public static BasePreferenceController getPreferenceController(Context context,
            SliceData sliceData) {
        return getPreferenceController(context, sliceData.getPreferenceController(),
                sliceData.getKey());
    }

    /**
     * @return {@link PendingIntent} for a non-primary {@link SliceAction}.
     */
    public static PendingIntent getActionIntent(Context context, String action, SliceData data) {
        final Intent intent = new Intent(action)
                .setData(data.getUri())
                .setClass(context, SliceBroadcastReceiver.class)
                .putExtra(EXTRA_SLICE_KEY, data.getKey());
        return PendingIntent.getBroadcast(context, 0 /* requestCode */, intent,
                PendingIntent.FLAG_UPDATE_CURRENT);
    }

    /**
     * @return {@link PendingIntent} for the primary {@link SliceAction}.
     */
    public static PendingIntent getContentPendingIntent(Context context, SliceData sliceData) {
        final Intent intent = getContentIntent(context, sliceData);
        return PendingIntent.getActivity(context, 0 /* requestCode */, intent, 0 /* flags */);
    }

    /**
     * @return the summary text for a {@link Slice} built for {@param sliceData}.
     */
    public static CharSequence getSubtitleText(Context context,
            BasePreferenceController controller, SliceData sliceData) {

        // Priority 1 : User prefers showing the dynamic summary in slice view rather than static
        // summary. Note it doesn't require a valid summary - so we can force some slices to have
        // empty summaries (ex: volume).
        if (controller.useDynamicSliceSummary()) {
            return controller.getSummary();
        }

        // Priority 2: Show summary from slice data.
        CharSequence summaryText = sliceData.getSummary();
        if (isValidSummary(context, summaryText)) {
            return summaryText;
        }

        // Priority 3: Show screen title.
        summaryText = sliceData.getScreenTitle();
        if (isValidSummary(context, summaryText) && !TextUtils.equals(summaryText,
                sliceData.getTitle())) {
            return summaryText;
        }

        // Priority 4: Show empty text.
        return "";
    }

    public static Intent buildSearchResultPageIntent(Context context, String className, String key,
            String screenTitle, int sourceMetricsCategory) {
        final Bundle args = new Bundle();
        args.putString(SettingsActivity.EXTRA_FRAGMENT_ARG_KEY, key);
        final Intent searchDestination = new SubSettingLauncher(context)
                .setDestination(className)
                .setArguments(args)
                .setTitleText(screenTitle)
                .setSourceMetricsCategory(sourceMetricsCategory)
                .toIntent();
        searchDestination.putExtra(SettingsActivity.EXTRA_FRAGMENT_ARG_KEY, key)
                .setAction("com.android.settings.SEARCH_RESULT_TRAMPOLINE")
                .setComponent(null);
        searchDestination.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);

        return searchDestination;
    }

    public static Intent getContentIntent(Context context, SliceData sliceData) {
        final Uri contentUri = new Uri.Builder().appendPath(sliceData.getKey()).build();
        final String screenTitle = TextUtils.isEmpty(sliceData.getScreenTitle()) ? null
                : sliceData.getScreenTitle().toString();
        final Intent intent = buildSearchResultPageIntent(context,
                sliceData.getFragmentClassName(), sliceData.getKey(),
                screenTitle, 0 /* TODO */);
        intent.setClassName(context.getPackageName(), SubSettings.class.getName());
        intent.setData(contentUri);
        return intent;
    }

    private static Slice buildToggleSlice(Context context, SliceData sliceData,
            BasePreferenceController controller) {
        final PendingIntent contentIntent = getContentPendingIntent(context, sliceData);
        final IconCompat icon = getSafeIcon(context, sliceData);
        final CharSequence subtitleText = getSubtitleText(context, controller, sliceData);
        @ColorInt final int color = Utils.getColorAccentDefaultColor(context);
        final TogglePreferenceController toggleController =
                (TogglePreferenceController) controller;
        final SliceAction sliceAction = getToggleAction(context, sliceData,
                toggleController.isChecked());
        final Set<String> keywords = buildSliceKeywords(sliceData);
        final RowBuilder rowBuilder = new RowBuilder()
                .setTitle(sliceData.getTitle())
                .setPrimaryAction(
                        SliceAction.createDeeplink(contentIntent, icon,
                                ListBuilder.ICON_IMAGE, sliceData.getTitle()))
                .addEndItem(sliceAction);
        if (!Utils.isSettingsIntelligence(context)) {
            rowBuilder.setSubtitle(subtitleText);
        }

        return new ListBuilder(context, sliceData.getUri(), ListBuilder.INFINITY)
                .setAccentColor(color)
                .addRow(rowBuilder)
                .setKeywords(keywords)
                .build();
    }

    private static Slice buildIntentSlice(Context context, SliceData sliceData,
            BasePreferenceController controller) {
        final PendingIntent contentIntent = getContentPendingIntent(context, sliceData);
        final IconCompat icon = getSafeIcon(context, sliceData);
        final CharSequence subtitleText = getSubtitleText(context, controller, sliceData);
        @ColorInt final int color = Utils.getColorAccentDefaultColor(context);
        final Set<String> keywords = buildSliceKeywords(sliceData);
        final RowBuilder rowBuilder = new RowBuilder()
                .setTitle(sliceData.getTitle())
                .setPrimaryAction(
                        SliceAction.createDeeplink(contentIntent, icon,
                                ListBuilder.ICON_IMAGE,
                                sliceData.getTitle()));
        if (!Utils.isSettingsIntelligence(context)) {
            rowBuilder.setSubtitle(subtitleText);
        }

        return new ListBuilder(context, sliceData.getUri(), ListBuilder.INFINITY)
                .setAccentColor(color)
                .addRow(rowBuilder)
                .setKeywords(keywords)
                .build();
    }

    private static Slice buildSliderSlice(Context context, SliceData sliceData,
            BasePreferenceController controller) {
        final SliderPreferenceController sliderController = (SliderPreferenceController) controller;
        if (sliderController.getMax() <= sliderController.getMin()) {
            Log.e(TAG, "Invalid sliderController: " + sliderController.getPreferenceKey());
            return null;
        }
        final PendingIntent actionIntent = getSliderAction(context, sliceData);
        final PendingIntent contentIntent = getContentPendingIntent(context, sliceData);
        final IconCompat icon = getSafeIcon(context, sliceData);
        @ColorInt int color = Utils.getColorAccentDefaultColor(context);
        final CharSequence subtitleText = getSubtitleText(context, controller, sliceData);
        final SliceAction primaryAction = SliceAction.createDeeplink(contentIntent, icon,
                ListBuilder.ICON_IMAGE, sliceData.getTitle());
        final Set<String> keywords = buildSliceKeywords(sliceData);

        int cur = sliderController.getSliderPosition();
        if (cur < sliderController.getMin()) {
            cur = sliderController.getMin();
        }
        if (cur > sliderController.getMax()) {
            cur = sliderController.getMax();
        }
        final InputRangeBuilder inputRangeBuilder = new InputRangeBuilder()
                .setTitle(sliceData.getTitle())
                .setPrimaryAction(primaryAction)
                .setMax(sliderController.getMax())
                .setMin(sliderController.getMin())
                .setValue(cur)
                .setInputAction(actionIntent);
        if (sliceData.getIconResource() != 0) {
            inputRangeBuilder.setTitleItem(icon, ListBuilder.ICON_IMAGE);
            color = CustomSliceable.COLOR_NOT_TINTED;
        }
        if (!Utils.isSettingsIntelligence(context)) {
            inputRangeBuilder.setSubtitle(subtitleText);
        }

        return new ListBuilder(context, sliceData.getUri(), ListBuilder.INFINITY)
                .setAccentColor(color)
                .addInputRange(inputRangeBuilder)
                .setKeywords(keywords)
                .build();
    }

    private static Slice buildCopyableSlice(Context context, SliceData sliceData,
            BasePreferenceController controller) {
        final SliceAction copyableAction = getCopyableAction(context, sliceData);
        final PendingIntent contentIntent = getContentPendingIntent(context, sliceData);
        final IconCompat icon = getSafeIcon(context, sliceData);
        final SliceAction primaryAction = SliceAction.createDeeplink(contentIntent, icon,
                ListBuilder.ICON_IMAGE,
                sliceData.getTitle());
        final CharSequence subtitleText = getSubtitleText(context, controller, sliceData);
        @ColorInt final int color = Utils.getColorAccentDefaultColor(context);
        final Set<String> keywords = buildSliceKeywords(sliceData);
        final RowBuilder rowBuilder = new RowBuilder()
                .setTitle(sliceData.getTitle())
                .setPrimaryAction(primaryAction)
                .addEndItem(copyableAction);
        if (!Utils.isSettingsIntelligence(context)) {
            rowBuilder.setSubtitle(subtitleText);
        }

        return new ListBuilder(context, sliceData.getUri(), ListBuilder.INFINITY)
                .setAccentColor(color)
                .addRow(rowBuilder)
                .setKeywords(keywords)
                .build();
    }

    static BasePreferenceController getPreferenceController(Context context,
            String controllerClassName, String controllerKey) {
        try {
            return BasePreferenceController.createInstance(context, controllerClassName);
        } catch (IllegalStateException e) {
            // Do nothing
        }

        return BasePreferenceController.createInstance(context, controllerClassName, controllerKey);
    }

    private static SliceAction getToggleAction(Context context, SliceData sliceData,
            boolean isChecked) {
        PendingIntent actionIntent = getActionIntent(context,
                SettingsSliceProvider.ACTION_TOGGLE_CHANGED, sliceData);
        return SliceAction.createToggle(actionIntent, null, isChecked);
    }

    private static PendingIntent getSliderAction(Context context, SliceData sliceData) {
        return getActionIntent(context, SettingsSliceProvider.ACTION_SLIDER_CHANGED, sliceData);
    }

    private static SliceAction getCopyableAction(Context context, SliceData sliceData) {
        final PendingIntent intent = getActionIntent(context,
                SettingsSliceProvider.ACTION_COPY, sliceData);
        final IconCompat icon = IconCompat.createWithResource(context,
                R.drawable.ic_content_copy_grey600_24dp);
        return SliceAction.create(intent, icon, ListBuilder.ICON_IMAGE, sliceData.getTitle());
    }

    private static boolean isValidSummary(Context context, CharSequence summary) {
        if (summary == null || TextUtils.isEmpty(summary.toString().trim())) {
            return false;
        }

        final CharSequence placeHolder = context.getText(R.string.summary_placeholder);
        final CharSequence doublePlaceHolder =
                context.getText(R.string.summary_two_lines_placeholder);

        return !(TextUtils.equals(summary, placeHolder)
                || TextUtils.equals(summary, doublePlaceHolder));
    }

    private static Set<String> buildSliceKeywords(SliceData data) {
        final Set<String> keywords = new ArraySet<>();

        keywords.add(data.getTitle());

        if (!TextUtils.isEmpty(data.getScreenTitle())
                && !TextUtils.equals(data.getTitle(), data.getScreenTitle())) {
            keywords.add(data.getScreenTitle().toString());
        }

        final String keywordString = data.getKeywords();
        if (keywordString != null) {
            final String[] keywordArray = keywordString.split(",");
            final List<String> strippedKeywords = Arrays.stream(keywordArray)
                    .map(s -> s = s.trim())
                    .collect(Collectors.toList());
            keywords.addAll(strippedKeywords);
        }

        return keywords;
    }

    private static Slice buildUnavailableSlice(Context context, SliceData data) {
        final String title = data.getTitle();
        final Set<String> keywords = buildSliceKeywords(data);
        @ColorInt final int color = Utils.getColorAccentDefaultColor(context);

        final String customSubtitle = data.getUnavailableSliceSubtitle();
        final CharSequence subtitle = !TextUtils.isEmpty(customSubtitle) ? customSubtitle
                : context.getText(R.string.disabled_dependent_setting_summary);
        final IconCompat icon = getSafeIcon(context, data);
        final SliceAction primaryAction = SliceAction.createDeeplink(
                getContentPendingIntent(context, data),
                icon, ListBuilder.ICON_IMAGE, title);
        final RowBuilder rowBuilder = new RowBuilder()
                .setTitle(title)
                .setTitleItem(icon, ListBuilder.ICON_IMAGE)
                .setPrimaryAction(primaryAction);
        if (!Utils.isSettingsIntelligence(context)) {
            rowBuilder.setSubtitle(subtitle);
        }

        return new ListBuilder(context, data.getUri(), ListBuilder.INFINITY)
                .setAccentColor(color)
                .addRow(rowBuilder)
                .setKeywords(keywords)
                .build();
    }

    @VisibleForTesting
    static IconCompat getSafeIcon(Context context, SliceData data) {
        int iconResource = data.getIconResource();

        if (iconResource == 0) {
            iconResource = R.drawable.ic_settings_accent;
        }
        try {
            return IconCompat.createWithResource(context, iconResource);
        } catch (Exception e) {
            Log.w(TAG, "Falling back to settings icon because there is an error getting slice icon "
                    + data.getUri(), e);
            return IconCompat.createWithResource(context, R.drawable.ic_settings_accent);
        }
    }
}
