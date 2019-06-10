/*
 * Copyright (C) 2019 The Android Open Source Project
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

package com.android.settings.homepage.contextualcards.slices;

import static com.android.settings.core.BasePreferenceController.AVAILABLE_UNSEARCHABLE;
import static com.android.settings.display.AdaptiveSleepPreferenceController.PREF_NAME;
import static com.android.settings.display.AdaptiveSleepPreferenceController.isControllerAvailable;
import static com.android.settings.slices.CustomSliceRegistry.CONTEXTUAL_ADAPTIVE_SLEEP_URI;

import android.app.PendingIntent;
import android.app.settings.SettingsEnums;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;

import androidx.core.graphics.drawable.IconCompat;
import androidx.slice.Slice;
import androidx.slice.builders.ListBuilder;
import androidx.slice.builders.SliceAction;

import com.android.settings.R;
import com.android.settings.SubSettings;
import com.android.settings.display.AdaptiveSleepSettings;
import com.android.settings.slices.CustomSliceable;
import com.android.settings.slices.SliceBuilderUtils;

import com.google.common.annotations.VisibleForTesting;

import java.util.concurrent.TimeUnit;

public class ContextualAdaptiveSleepSlice implements CustomSliceable {
    private static final String TAG = "ContextualAdaptiveSleepSlice";
    private static final long DEFAULT_SETUP_TIME = 0;
    private Context mContext;

    @VisibleForTesting
    static final long DEFERRED_TIME_DAYS = TimeUnit.DAYS.toMillis(14);
    @VisibleForTesting
    static final String PREF_KEY_SETUP_TIME = "adaptive_sleep_setup_time";

    public static final String PREF_KEY_INTERACTED = "adaptive_sleep_interacted";
    public static final String PREF = "adaptive_sleep_slice";

    public ContextualAdaptiveSleepSlice(Context context) {
        mContext = context;
    }

    @Override
    public Slice getSlice() {
        final long setupTime = mContext.getSharedPreferences(PREF, Context.MODE_PRIVATE).getLong(
                PREF_KEY_SETUP_TIME, DEFAULT_SETUP_TIME);
        if (setupTime == DEFAULT_SETUP_TIME) {
            // Set the first setup time.
            mContext.getSharedPreferences(PREF, Context.MODE_PRIVATE)
                    .edit()
                    .putLong(PREF_KEY_SETUP_TIME, System.currentTimeMillis())
                    .apply();
            return null;
        }

        // Display the contextual card only if all the following 3 conditions hold:
        // 1. The Screen Attention is enabled in Settings.
        // 2. The device is not recently set up.
        // 3. Current user hasn't opened Screen Attention's settings page before.
        if (isSettingsAvailable() && !isUserInteracted() && !isRecentlySetup()) {
            final IconCompat icon = IconCompat.createWithResource(mContext,
                    R.drawable.ic_settings_adaptive_sleep);
            final CharSequence title = mContext.getText(R.string.adaptive_sleep_title);
            final CharSequence subtitle = mContext.getText(
                    R.string.adaptive_sleep_contextual_slice_summary);

            final SliceAction pAction = SliceAction.createDeeplink(getPrimaryAction(),
                    icon,
                    ListBuilder.ICON_IMAGE,
                    title);
            final ListBuilder listBuilder = new ListBuilder(mContext,
                    CONTEXTUAL_ADAPTIVE_SLEEP_URI,
                    ListBuilder.INFINITY)
                    .setAccentColor(COLOR_NOT_TINTED)
                    .addRow(new ListBuilder.RowBuilder()
                            .setTitleItem(icon, ListBuilder.ICON_IMAGE)
                            .setTitle(title)
                            .setSubtitle(subtitle)
                            .setPrimaryAction(pAction));
            return listBuilder.build();
        } else {
            return null;
        }
    }

    @Override
    public Uri getUri() {
        return CONTEXTUAL_ADAPTIVE_SLEEP_URI;
    }

    @Override
    public Intent getIntent() {
        final CharSequence screenTitle = mContext.getText(R.string.adaptive_sleep_title);
        final Uri contentUri = new Uri.Builder().appendPath(PREF_NAME).build();
        return SliceBuilderUtils.buildSearchResultPageIntent(mContext,
                AdaptiveSleepSettings.class.getName(), PREF_NAME, screenTitle.toString(),
                SettingsEnums.SLICE).setClassName(mContext.getPackageName(),
                SubSettings.class.getName()).setData(contentUri);
    }

    private PendingIntent getPrimaryAction() {
        final Intent intent = getIntent();
        return PendingIntent.getActivity(mContext, 0  /* requestCode */, intent, 0  /* flags */);
    }

    /**
     * @return {@code true} if the current user has opened the Screen Attention settings page
     * before, otherwise {@code false}.
     */
    private boolean isUserInteracted() {
        return mContext.getSharedPreferences(PREF, Context.MODE_PRIVATE).getBoolean(
                PREF_KEY_INTERACTED, false);
    }

    /**
     * The device is recently set up means its first settings-open time is within 2 weeks ago.
     *
     * @return {@code true} if the device is recently set up, otherwise {@code false}.
     */
    private boolean isRecentlySetup() {
        final long endTime = System.currentTimeMillis() - DEFERRED_TIME_DAYS;
        final long firstSetupTime = mContext.getSharedPreferences(PREF,
                Context.MODE_PRIVATE).getLong(PREF_KEY_SETUP_TIME, DEFAULT_SETUP_TIME);
        return firstSetupTime > endTime;
    }

    /**
     * Check whether the screen attention settings is enabled. Contextual card will only appear
     * when the screen attention settings is available.
     *
     * @return {@code true} if screen attention settings is enabled, otherwise {@code false}
     */
    @VisibleForTesting
    boolean isSettingsAvailable() {
        return isControllerAvailable(mContext) == AVAILABLE_UNSEARCHABLE;
    }
}