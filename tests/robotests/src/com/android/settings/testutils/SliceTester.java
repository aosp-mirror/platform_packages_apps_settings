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
 * limitations under the License
 */

package com.android.settings.testutils;

import static android.app.slice.Slice.HINT_TITLE;
import static android.app.slice.Slice.SUBTYPE_COLOR;
import static android.app.slice.SliceItem.FORMAT_IMAGE;
import static android.app.slice.SliceItem.FORMAT_INT;
import static android.app.slice.SliceItem.FORMAT_TEXT;

import static com.google.common.truth.Truth.assertThat;

import android.app.PendingIntent;
import android.content.Context;
import android.text.TextUtils;

import androidx.core.graphics.drawable.IconCompat;
import androidx.slice.Slice;
import androidx.slice.SliceItem;
import androidx.slice.SliceMetadata;
import androidx.slice.builders.ListBuilder;
import androidx.slice.core.SliceAction;
import androidx.slice.core.SliceQuery;
import androidx.slice.widget.EventInfo;

import com.android.settings.Utils;
import com.android.settings.slices.SettingsSliceProvider;
import com.android.settings.slices.SliceBuilderUtils;
import com.android.settings.slices.SliceData;

import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Testing utility class to verify the contents of the different Settings Slices.
 *
 * TODO (77712944) check Summary, range (metadata.getRange()), toggle icons.
 */
public class SliceTester {

    /**
     * Test the contents of an intent based slice, including:
     * - No toggles
     * - Correct intent
     * - Correct title
     * - Correct keywords
     * - TTL
     * - Color
     */
    public static void testSettingsIntentSlice(Context context, Slice slice, SliceData sliceData) {
        final SliceMetadata metadata = SliceMetadata.from(context, slice);

        final long sliceTTL = metadata.getExpiry();
        assertThat(sliceTTL).isEqualTo(ListBuilder.INFINITY);

        final SliceItem colorItem = SliceQuery.findSubtype(slice, FORMAT_INT, SUBTYPE_COLOR);
        final int color = colorItem.getInt();
        assertThat(color).isEqualTo(Utils.getColorAccentDefaultColor(context));

        final List<SliceAction> toggles = metadata.getToggles();
        assertThat(toggles).isEmpty();

        final PendingIntent primaryPendingIntent = metadata.getPrimaryAction().getAction();
        assertThat(primaryPendingIntent).isEqualTo(
                SliceBuilderUtils.getContentPendingIntent(context, sliceData));

        assertThat(metadata.getTitle()).isEqualTo(sliceData.getTitle());

        assertKeywords(metadata, sliceData);
    }

    /**
     * Test the contents of an toggle based slice, including:
     * - Contains one toggle
     * - Correct toggle intent
     * - Correct content intent
     * - Correct title
     * - Correct keywords
     * - TTL
     * - Color
     */
    public static void testSettingsToggleSlice(Context context, Slice slice, SliceData sliceData) {
        final SliceMetadata metadata = SliceMetadata.from(context, slice);

        final SliceItem colorItem = SliceQuery.findSubtype(slice, FORMAT_INT, SUBTYPE_COLOR);
        final int color = colorItem.getInt();
        assertThat(color).isEqualTo(Utils.getColorAccentDefaultColor(context));

        final List<SliceAction> toggles = metadata.getToggles();
        assertThat(toggles).hasSize(1);

        final long sliceTTL = metadata.getExpiry();
        assertThat(sliceTTL).isEqualTo(ListBuilder.INFINITY);

        final SliceAction mainToggleAction = toggles.get(0);

        assertThat(mainToggleAction.getIcon()).isNull();

        // Check intent in Toggle Action
        final PendingIntent togglePendingIntent = mainToggleAction.getAction();
        assertThat(togglePendingIntent).isEqualTo(SliceBuilderUtils.getActionIntent(context,
                SettingsSliceProvider.ACTION_TOGGLE_CHANGED, sliceData));

        // Check primary intent
        final PendingIntent primaryPendingIntent = metadata.getPrimaryAction().getAction();
        assertThat(primaryPendingIntent).isEqualTo(
                SliceBuilderUtils.getContentPendingIntent(context, sliceData));

        assertThat(metadata.getTitle()).isEqualTo(sliceData.getTitle());

        assertKeywords(metadata, sliceData);
    }

    /**
     * Test the contents of an slider based slice, including:
     * - No intent
     * - Correct title
     * - Correct keywords
     * - TTL
     * - Color
     */
    public static void testSettingsSliderSlice(Context context, Slice slice, SliceData sliceData) {
        final SliceMetadata metadata = SliceMetadata.from(context, slice);
        final SliceAction primaryAction = metadata.getPrimaryAction();

        final IconCompat icon = primaryAction.getIcon();
        if (icon == null) {
            final SliceItem colorItem = SliceQuery.findSubtype(slice, FORMAT_INT, SUBTYPE_COLOR);
            final int color = colorItem.getInt();
            assertThat(color).isEqualTo(Utils.getColorAccentDefaultColor(context));

        } else {
            final IconCompat expectedIcon = IconCompat.createWithResource(context,
                    sliceData.getIconResource());
            assertThat(expectedIcon.toString()).isEqualTo(icon.toString());
        }

        final long sliceTTL = metadata.getExpiry();
        assertThat(sliceTTL).isEqualTo(ListBuilder.INFINITY);

        final int headerType = metadata.getHeaderType();
        assertThat(headerType).isEqualTo(EventInfo.ROW_TYPE_SLIDER);

        // Check primary intent
        final PendingIntent primaryPendingIntent = primaryAction.getAction();
        assertThat(primaryPendingIntent).isEqualTo(
                SliceBuilderUtils.getContentPendingIntent(context, sliceData));

        assertThat(metadata.getTitle()).isEqualTo(sliceData.getTitle());

        assertKeywords(metadata, sliceData);
    }

    /**
     * Test the copyable slice, including:
     * - No intent
     * - Correct title
     * - Correct intent
     * - Correct keywords
     * - TTL
     * - Color
     */
    public static void testSettingsCopyableSlice(Context context, Slice slice,
            SliceData sliceData) {
        final SliceMetadata metadata = SliceMetadata.from(context, slice);

        final SliceItem colorItem = SliceQuery.findSubtype(slice, FORMAT_INT, SUBTYPE_COLOR);
        final int color = colorItem.getInt();
        assertThat(color).isEqualTo(Utils.getColorAccentDefaultColor(context));

        final SliceAction primaryAction = metadata.getPrimaryAction();

        final IconCompat expectedIcon = IconCompat.createWithResource(context,
                sliceData.getIconResource());
        assertThat(expectedIcon.toString()).isEqualTo(primaryAction.getIcon().toString());

        final long sliceTTL = metadata.getExpiry();
        assertThat(sliceTTL).isEqualTo(ListBuilder.INFINITY);

        // Check primary intent
        final PendingIntent primaryPendingIntent = primaryAction.getAction();
        assertThat(primaryPendingIntent).isEqualTo(
                SliceBuilderUtils.getContentPendingIntent(context, sliceData));

        assertThat(metadata.getTitle()).isEqualTo(sliceData.getTitle());

        assertKeywords(metadata, sliceData);
    }

    /**
     * Test the contents of an unavailable slice, including:
     * - No toggles
     * - Correct title
     * - Correct intent
     * - Correct keywords
     * - Color
     * - TTL
     */
    public static void testSettingsUnavailableSlice(Context context, Slice slice,
            SliceData sliceData) {
        final SliceMetadata metadata = SliceMetadata.from(context, slice);

        final long sliceTTL = metadata.getExpiry();
        assertThat(sliceTTL).isEqualTo(ListBuilder.INFINITY);

        final SliceItem colorItem = SliceQuery.findSubtype(slice, FORMAT_INT, SUBTYPE_COLOR);
        final int color = colorItem.getInt();
        assertThat(color).isEqualTo(Utils.getColorAccentDefaultColor(context));

        final List<SliceAction> toggles = metadata.getToggles();
        assertThat(toggles).isEmpty();

        final PendingIntent primaryPendingIntent = metadata.getPrimaryAction().getAction();
        assertThat(primaryPendingIntent).isEqualTo(SliceBuilderUtils.getContentPendingIntent(
                context, sliceData));

        assertThat(metadata.getTitle()).isEqualTo(sliceData.getTitle());

        assertKeywords(metadata, sliceData);
    }

    /**
     * Assert any slice item contains title.
     *
     * @param sliceItems All slice items of a Slice.
     * @param title Title for asserting.
     */
    public static void assertAnySliceItemContainsTitle(List<SliceItem> sliceItems, String title) {
        assertThat(hasText(sliceItems, title, HINT_TITLE)).isTrue();
    }

    /**
     * Assert any slice item contains subtitle.
     *
     * @param sliceItems All slice items of a Slice.
     * @param subtitle Subtitle for asserting.
     */
    public static void assertAnySliceItemContainsSubtitle(List<SliceItem> sliceItems,
            String subtitle) {
        // Subtitle has no hints
        assertThat(hasText(sliceItems, subtitle, null /* hints */)).isTrue();
    }

    /**
     * Assert no slice item contains subtitle.
     *
     * @param sliceItems All slice items of a Slice.
     * @param subtitle Subtitle for asserting.
     */
    public static void assertNoSliceItemContainsSubtitle(List<SliceItem> sliceItems,
            String subtitle) {
        // Subtitle has no hints
        assertThat(hasText(sliceItems, subtitle, null /* hints */)).isFalse();
    }

    private static boolean hasText(List<SliceItem> sliceItems, String text, String hints) {
        boolean hasText = false;
        for (SliceItem item : sliceItems) {
            List<SliceItem> textItems = SliceQuery.findAll(item, FORMAT_TEXT, hints,
                    null /* non-hints */);
            if (textItems == null) {
                continue;
            }

            for (SliceItem textItem : textItems) {
                if (TextUtils.equals(textItem.getText(), text)) {
                    hasText = true;
                    break;
                }
            }
        }
        return hasText;
    }

    /**
     * Assert any slice item contains icon.
     *
     * @param sliceItems All slice items of a Slice.
     * @param icon Icon for asserting.
     */
    public static void assertAnySliceItemContainsIcon(List<SliceItem> sliceItems, IconCompat icon) {
        boolean hasIcon = false;
        for (SliceItem item : sliceItems) {
            List<SliceItem> iconItems = SliceQuery.findAll(item, FORMAT_IMAGE,
                    (String) null /* hints */, null /* non-hints */);
            if (iconItems == null) {
                continue;
            }

            for (SliceItem iconItem : iconItems) {
                if (icon.toString().equals(iconItem.getIcon().toString())) {
                    hasIcon = true;
                    break;
                }
            }
        }
        assertThat(hasIcon).isTrue();
    }

    private static void assertKeywords(SliceMetadata metadata, SliceData data) {
        final List<String> keywords = metadata.getSliceKeywords();
        final Set<String> expectedKeywords = Arrays.stream(data.getKeywords().split(","))
                .map(s -> s = s.trim())
                .collect(Collectors.toSet());
        expectedKeywords.add(data.getTitle());
        expectedKeywords.add(data.getScreenTitle().toString());
        assertThat(keywords).containsExactlyElementsIn(expectedKeywords);
    }
}