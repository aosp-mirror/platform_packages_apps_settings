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
 * limitations under the License.
 */

package com.android.settings.panel;

import static com.android.settings.media.MediaOutputSlice.MEDIA_PACKAGE_NAME;
import static com.android.settings.slices.CustomSliceRegistry.MEDIA_OUTPUT_SLICE_URI;

import android.app.settings.SettingsEnums;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;

import com.android.settings.R;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents the Media output Panel.
 *
 * <p>
 * Displays Media output item
 * </p>
 */
public class MediaOutputPanel implements PanelContent {

    private final Context mContext;
    private final String mPackageName;

    public static MediaOutputPanel create(Context context, String packageName) {
        return new MediaOutputPanel(context, packageName);
    }

    private MediaOutputPanel(Context context, String packageName) {
        mContext = context.getApplicationContext();
        mPackageName = packageName;
    }

    @Override
    public CharSequence getTitle() {
        return mContext.getText(R.string.media_output_panel_title);
    }

    @Override
    public List<Uri> getSlices() {
        final List<Uri> uris = new ArrayList<>();
        MEDIA_OUTPUT_SLICE_URI =
                MEDIA_OUTPUT_SLICE_URI
                        .buildUpon()
                        .clearQuery()
                        .appendQueryParameter(MEDIA_PACKAGE_NAME, mPackageName)
                        .build();
        uris.add(MEDIA_OUTPUT_SLICE_URI);
        return uris;
    }

    @Override
    public Intent getSeeMoreIntent() {
        return null;
    }

    @Override
    public int getMetricsCategory() {
        return SettingsEnums.PANEL_MEDIA_OUTPUT;
    }
}
