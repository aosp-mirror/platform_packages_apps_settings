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
import static com.android.settingslib.media.flags.Flags.enableOutputSwitcherForSystemRouting;

import android.annotation.ColorInt;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.media.session.MediaController;
import android.net.Uri;
import android.util.Log;

import androidx.annotation.VisibleForTesting;
import androidx.core.graphics.drawable.IconCompat;
import androidx.slice.Slice;
import androidx.slice.builders.ListBuilder;
import androidx.slice.builders.SliceAction;

import com.android.settings.R;
import com.android.settings.Utils;
import com.android.settings.slices.CustomSliceable;
import com.android.settings.slices.SliceBackgroundWorker;
import com.android.settingslib.media.MediaOutputConstants;

public class MediaOutputIndicatorSlice implements CustomSliceable {

    private static final String TAG = "MediaOutputIndSlice";

    private Context mContext;
    private MediaOutputIndicatorWorker mWorker;

    public MediaOutputIndicatorSlice(Context context) {
        mContext = context;
    }

    @Override
    public Slice getSlice() {
        if (!isVisible()) {
            return new ListBuilder(mContext, getUri(), ListBuilder.INFINITY)
                    .setIsError(true)
                    .build();
        }
        final IconCompat icon = IconCompat.createWithResource(mContext,
                com.android.internal.R.drawable.ic_settings_bluetooth);
        final int stringRes = enableOutputSwitcherForSystemRouting()
                ? (getWorker().getActiveLocalMediaController() != null
                        ? R.string.media_output_label_title
                        : R.string.media_output_title_without_playing)
                : R.string.media_output_label_title;
        final CharSequence title = mContext.getString(stringRes,
                Utils.getApplicationLabel(mContext, getWorker().getPackageName()));
        final SliceAction primarySliceAction = SliceAction.create(
                getBroadcastIntent(mContext), icon, ListBuilder.ICON_IMAGE, title);

        @ColorInt final int color = Utils.getColorAccentDefaultColor(mContext);
        // To set an empty icon to indent the row
        final ListBuilder listBuilder = new ListBuilder(mContext, getUri(), ListBuilder.INFINITY)
                .setAccentColor(color)
                .addRow(new ListBuilder.RowBuilder()
                        .setTitle(title)
                        .setTitleItem(createEmptyIcon(), ListBuilder.ICON_IMAGE)
                        .setSubtitle(getWorker().getCurrentConnectedMediaDevice().getName())
                        .setPrimaryAction(primarySliceAction));
        return listBuilder.build();
    }

    private IconCompat createEmptyIcon() {
        final Bitmap bitmap = Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888);
        return IconCompat.createWithBitmap(bitmap);
    }

    @Override
    public Uri getUri() {
        return MEDIA_OUTPUT_INDICATOR_SLICE_URI;
    }

    @Override
    public Intent getIntent() {
        // This Slice reflects active media device information and launch MediaOutputDialog. It does
        // not contain its owned Slice data
        return null;
    }

    @Override
    public int getSliceHighlightMenuRes() {
        return R.string.menu_key_connected_devices;
    }

    @Override
    public Class getBackgroundWorkerClass() {
        return MediaOutputIndicatorWorker.class;
    }

    private MediaOutputIndicatorWorker getWorker() {
        if (mWorker == null) {
            mWorker = SliceBackgroundWorker.getInstance(getUri());
        }
        return mWorker;
    }

    @VisibleForTesting
    boolean isVisible() {
        // To decide Slice's visibility.
        // Return true if
        // 1. AudioMode is not in on-going call
        // 2. worker is not null
        // 3. Available devices are more than 0
        // 4. The local media session is active and the state is playing.
        //    - if !enableOutputSwitcherForSystemRouting(), (4) will be bypass.
        return getWorker() != null
                && !com.android.settingslib.Utils.isAudioModeOngoingCall(mContext)
                && getWorker().getMediaDevices().size() > 0
                && (enableOutputSwitcherForSystemRouting()
                        ? true : getWorker().getActiveLocalMediaController() != null);
    }

    @Override
    public void onNotifyChange(Intent intent) {
        final MediaController mediaController = getWorker().getActiveLocalMediaController();

        // Launch media output dialog
        if (enableOutputSwitcherForSystemRouting() && mediaController == null) {
            mContext.sendBroadcast(new Intent()
                    .setPackage(MediaOutputConstants.SYSTEMUI_PACKAGE_NAME)
                    .setAction(MediaOutputConstants.ACTION_LAUNCH_SYSTEM_MEDIA_OUTPUT_DIALOG));
        } else if (mediaController != null) {
            mContext.sendBroadcast(new Intent()
                    .setPackage(MediaOutputConstants.SYSTEMUI_PACKAGE_NAME)
                    .setAction(MediaOutputConstants.ACTION_LAUNCH_MEDIA_OUTPUT_DIALOG)
                    .putExtra(MediaOutputConstants.KEY_MEDIA_SESSION_TOKEN,
                            mediaController.getSessionToken())
                    .putExtra(MediaOutputConstants.EXTRA_PACKAGE_NAME,
                            mediaController.getPackageName()));
        } else {
            Log.d(TAG, "No active local media controller");
            return;
        }

        // Dismiss volume panel
        mContext.sendBroadcast(new Intent()
                .setPackage(MediaOutputConstants.SETTINGS_PACKAGE_NAME)
                .setAction(MediaOutputConstants.ACTION_CLOSE_PANEL));
    }
}
