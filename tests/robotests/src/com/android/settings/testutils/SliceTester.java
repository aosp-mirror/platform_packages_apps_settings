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
import static android.app.slice.SliceItem.FORMAT_TEXT;

import static com.android.settings.core.BasePreferenceController.DISABLED_DEPENDENT_SETTING;
import static com.android.settings.core.BasePreferenceController.DISABLED_FOR_USER;
import static com.android.settings.core.BasePreferenceController.DISABLED_UNSUPPORTED;
import static com.android.settings.core.BasePreferenceController.UNAVAILABLE_UNKNOWN;

import static com.google.common.truth.Truth.assertThat;

import android.app.PendingIntent;
import android.content.Context;

import java.util.List;

import androidx.slice.Slice;
import androidx.slice.SliceItem;
import androidx.slice.SliceMetadata;
import androidx.slice.core.SliceAction;
import androidx.slice.core.SliceQuery;

import android.support.v4.graphics.drawable.IconCompat;

import com.android.settings.slices.SettingsSliceProvider;
import com.android.settings.slices.SliceBuilderUtils;
import com.android.settings.slices.SliceData;

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
     */
    public static void testSettingsIntentSlice(Context context, Slice slice, SliceData sliceData) {
        final SliceMetadata metadata = SliceMetadata.from(context, slice);

        final List<SliceAction> toggles = metadata.getToggles();
        assertThat(toggles).isEmpty();

        final PendingIntent primaryPendingIntent = metadata.getPrimaryAction().getAction();
        assertThat(primaryPendingIntent).isEqualTo(
                SliceBuilderUtils.getContentIntent(context, sliceData));

        final List<SliceItem> sliceItems = slice.getItems();
        assertTitle(sliceItems, sliceData.getTitle());
    }

    /**
     * Test the contents of an toggle based slice, including:
     * - Contains one toggle
     * - Correct toggle intent
     * - Correct content intent
     * - Correct title
     */
    public static void testSettingsToggleSlice(Context context, Slice slice, SliceData sliceData) {
        final SliceMetadata metadata = SliceMetadata.from(context, slice);

        final List<SliceAction> toggles = metadata.getToggles();
        assertThat(toggles).hasSize(1);

        final SliceAction mainToggleAction = toggles.get(0);

        final IconCompat expectedToggleIcon = IconCompat.createWithResource(context,
                sliceData.getIconResource());
        assertThat(mainToggleAction.getIcon()).isNull();

        // Check intent in Toggle Action
        final PendingIntent togglePendingIntent = mainToggleAction.getAction();
        assertThat(togglePendingIntent).isEqualTo(SliceBuilderUtils.getActionIntent(context,
                SettingsSliceProvider.ACTION_TOGGLE_CHANGED, sliceData));

        // Check primary intent
        final PendingIntent primaryPendingIntent = metadata.getPrimaryAction().getAction();
        assertThat(primaryPendingIntent).isEqualTo(
                SliceBuilderUtils.getContentIntent(context, sliceData));

        final List<SliceItem> sliceItems = slice.getItems();
        assertTitle(sliceItems, sliceData.getTitle());
    }

    /**
     * Test the contents of an slider based slice, including:
     * - No intent
     * - Correct title
     */
    public static void testSettingsSliderSlice(Context context, Slice slice, SliceData sliceData) {
        final SliceMetadata metadata = SliceMetadata.from(context, slice);

        final IconCompat expectedToggleIcon = IconCompat.createWithResource(context,
                sliceData.getIconResource());

        // Check primary intent
        final SliceAction primaryAction = metadata.getPrimaryAction();
        assertThat(primaryAction).isNull();

        final List<SliceItem> sliceItems = slice.getItems();
        assertTitle(sliceItems, sliceData.getTitle());
    }

    /**
     * Test the contents of an unavailable slice, including:
     * - No toggles
     * - Correct title
     * - Correct intent
     */
    public static void testSettingsUnavailableSlice(Context context, Slice slice,
            SliceData sliceData) {
        final SliceMetadata metadata = SliceMetadata.from(context, slice);

        final List<SliceAction> toggles = metadata.getToggles();
        assertThat(toggles).isEmpty();

        final PendingIntent primaryPendingIntent = metadata.getPrimaryAction().getAction();
        final int availabilityStatus = SliceBuilderUtils.getPreferenceController(context,
                sliceData).getAvailabilityStatus();
        switch (availabilityStatus) {
            case DISABLED_UNSUPPORTED:
            case UNAVAILABLE_UNKNOWN:
                assertThat(primaryPendingIntent).isEqualTo(
                        SliceBuilderUtils.getSettingsIntent(context));
                break;
            case DISABLED_FOR_USER:
            case DISABLED_DEPENDENT_SETTING:
                assertThat(primaryPendingIntent).isEqualTo(
                        SliceBuilderUtils.getContentIntent(context, sliceData));
                break;
        }

        final List<SliceItem> sliceItems = slice.getItems();
        assertTitle(sliceItems, sliceData.getTitle());
    }

    private static void assertTitle(List<SliceItem> sliceItems, String title) {
        boolean hasTitle = false;
        for (SliceItem item : sliceItems) {
            List<SliceItem> titles = SliceQuery.findAll(item, FORMAT_TEXT, HINT_TITLE,
                    null /* non-hints */);
            if (titles != null & titles.size() == 1) {
                assertThat(titles.get(0).getText()).isEqualTo(title);
                hasTitle = true;
            }
        }
        assertThat(hasTitle).isTrue();
    }
}