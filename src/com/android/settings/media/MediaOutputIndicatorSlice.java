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

package com.android.settings.media;

import static com.android.settings.slices.CustomSliceRegistry.MEDIA_OUTPUT_INDICATOR_SLICE_URI;

import android.annotation.ColorInt;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;

import androidx.annotation.VisibleForTesting;
import androidx.core.graphics.drawable.IconCompat;
import androidx.slice.Slice;
import androidx.slice.builders.ListBuilder;
import androidx.slice.builders.SliceAction;

import com.android.settings.R;
import com.android.settings.Utils;
import com.android.settings.slices.CustomSliceable;
import com.android.settings.slices.SliceBackgroundWorker;
import com.android.settingslib.media.MediaOutputSliceConstants;

public class MediaOutputIndicatorSlice implements CustomSliceable {

    private Context mContext;
    @VisibleForTesting
    MediaOutputIndicatorWorker mWorker;

    public MediaOutputIndicatorSlice(Context context) {
        mContext = context;
    }

    @Override
    public Slice getSlice() {
        if (!getWorker().isVisible()) {
            return null;
        }
        final IconCompat icon = IconCompat.createWithResource(mContext,
                com.android.internal.R.drawable.ic_settings_bluetooth);
        final CharSequence title = mContext.getText(R.string.media_output_title);
        final PendingIntent primaryActionIntent = PendingIntent.getActivity(mContext,
                0 /* requestCode */, getMediaOutputSliceIntent(), 0 /* flags */);
        final SliceAction primarySliceAction = SliceAction.createDeeplink(
                primaryActionIntent, icon, ListBuilder.ICON_IMAGE, title);
        @ColorInt final int color = Utils.getColorAccentDefaultColor(mContext);

        final ListBuilder listBuilder = new ListBuilder(mContext,
                MEDIA_OUTPUT_INDICATOR_SLICE_URI,
                ListBuilder.INFINITY)
                .setAccentColor(color)
                .addRow(new ListBuilder.RowBuilder()
                        .setTitle(title)
                        .setSubtitle(getWorker().findActiveDeviceName())
                        .setPrimaryAction(primarySliceAction));
        return listBuilder.build();
    }

    private MediaOutputIndicatorWorker getWorker() {
        if (mWorker == null) {
            mWorker = (MediaOutputIndicatorWorker) SliceBackgroundWorker.getInstance(getUri());
        }
        return mWorker;
    }

    private Intent getMediaOutputSliceIntent() {
        final Intent intent = new Intent()
                .setAction(MediaOutputSliceConstants.ACTION_MEDIA_OUTPUT)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        return intent;
    }

    @Override
    public Uri getUri() {
        return MEDIA_OUTPUT_INDICATOR_SLICE_URI;
    }

    @Override
    public Intent getIntent() {
        // This Slice reflects active media device information and launch MediaOutputSlice. It does
        // not contain its owned Slice data
        return null;
    }

    @Override
    public Class getBackgroundWorkerClass() {
        return MediaOutputIndicatorWorker.class;
    }
}
