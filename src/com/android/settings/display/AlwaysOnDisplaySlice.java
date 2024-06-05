/*
 * Copyright (C) 2020 The Android Open Source Project
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

package com.android.settings.display;

import static android.provider.Settings.Secure.DOZE_ALWAYS_ON;
import static android.provider.Settings.Secure.DOZE_WAKE_DISPLAY_GESTURE;

import android.annotation.ColorInt;
import android.app.PendingIntent;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.hardware.display.AmbientDisplayConfiguration;
import android.net.Uri;
import android.os.UserHandle;
import android.provider.Settings;
import android.text.TextUtils;

import androidx.slice.Slice;
import androidx.slice.builders.ListBuilder;
import androidx.slice.builders.SliceAction;

import com.android.settings.R;
import com.android.settings.Utils;
import com.android.settings.slices.CustomSliceRegistry;
import com.android.settings.slices.CustomSliceable;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Custom {@link Slice} for Always on Display.
 * <p>
 *     We make a custom slice instead of using {@link AmbientDisplayAlwaysOnPreferenceController}
 *     because the controller will be unavailable if devices support aware sensor, and thus
 *     can not convert to slice.
 * </p>
 *
 */
public class AlwaysOnDisplaySlice implements CustomSliceable {
    private static final int MY_USER = UserHandle.myUserId();

    private final Context mContext;
    private final AmbientDisplayConfiguration mConfig;

    public AlwaysOnDisplaySlice(Context context) {
        mContext = context;
        mConfig = new AmbientDisplayConfiguration(mContext);
    }

    @Override
    public Slice getSlice() {
        if (!mConfig.alwaysOnAvailableForUser(MY_USER)) {
            return null;
        }

        final PendingIntent toggleAction = getBroadcastIntent(mContext);
        @ColorInt final int color = Utils.getColorAccentDefaultColor(mContext);
        final boolean isChecked = mConfig.alwaysOnEnabled(MY_USER);

        return new ListBuilder(mContext, CustomSliceRegistry.ALWAYS_ON_SLICE_URI,
                ListBuilder.INFINITY)
                .setAccentColor(color)
                .setKeywords(getKeywords())
                .addRow(new ListBuilder.RowBuilder()
                        .setTitle(mContext.getText(R.string.doze_always_on_title))
                        .setSubtitle(mContext.getText(R.string.doze_always_on_summary))
                        .setPrimaryAction(
                                SliceAction.createToggle(toggleAction, null /* actionTitle */,
                                        isChecked)))
                .build();
    }

    private Set<String> getKeywords() {
        final String keywords = mContext.getString(R.string.keywords_always_show_time_info);
        return Arrays.stream(TextUtils.split(keywords, ","))
                .map(String::trim)
                .collect(Collectors.toSet());
    }

    @Override
    public Uri getUri() {
        return CustomSliceRegistry.ALWAYS_ON_SLICE_URI;
    }

    @Override
    public void onNotifyChange(Intent intent) {
        final boolean isChecked = intent.getBooleanExtra(android.app.slice.Slice.EXTRA_TOGGLE_STATE,
                false);
        final ContentResolver resolver = mContext.getContentResolver();

        Settings.Secure.putInt(resolver, DOZE_ALWAYS_ON, isChecked ? 1 : 0);
        Settings.Secure.putInt(resolver, DOZE_WAKE_DISPLAY_GESTURE, 0);
    }

    @Override
    public Intent getIntent() {
        return null;
    }

    @Override
    public int getSliceHighlightMenuRes() {
        return R.string.menu_key_display;
    }
}
