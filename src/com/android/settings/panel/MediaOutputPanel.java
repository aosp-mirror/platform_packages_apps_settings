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
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.media.MediaMetadata;
import android.media.session.MediaController;
import android.media.session.MediaSessionManager;
import android.net.Uri;
import android.text.TextUtils;
import android.util.Log;

import androidx.core.graphics.drawable.IconCompat;

import com.android.settings.R;
import com.android.settings.Utils;

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

    private static final String TAG = "MediaOutputPanel";

    private final Context mContext;
    private final String mPackageName;

    private MediaSessionManager mMediaSessionManager;
    private MediaController mMediaController;

    public static MediaOutputPanel create(Context context, String packageName) {
        return new MediaOutputPanel(context, packageName);
    }

    private MediaOutputPanel(Context context, String packageName) {
        mContext = context.getApplicationContext();
        mPackageName = packageName;
        if (mPackageName != null) {
            mMediaSessionManager = mContext.getSystemService(MediaSessionManager.class);
            for (MediaController controller : mMediaSessionManager.getActiveSessions(null)) {
                if (TextUtils.equals(controller.getPackageName(), mPackageName)) {
                    mMediaController = controller;
                    break;
                }
            }
        }
        if (mMediaController == null) {
            Log.e(TAG, "Unable to find " + mPackageName + " media controller");
        }
    }

    @Override
    public CharSequence getTitle() {
        if (mMediaController != null) {
            final MediaMetadata metadata = mMediaController.getMetadata();
            if (metadata != null) {
                return metadata.getString(MediaMetadata.METADATA_KEY_ARTIST);
            }
        }
        return mContext.getText(R.string.media_volume_title);
    }

    @Override
    public CharSequence getSubTitle() {
        if (mMediaController != null) {
            final MediaMetadata metadata = mMediaController.getMetadata();
            if (metadata != null) {
                return metadata.getString(MediaMetadata.METADATA_KEY_ALBUM);
            }
        }
        return mContext.getText(R.string.media_output_panel_title);
    }

    @Override
    public IconCompat getIcon() {
        if (mMediaController == null) {
            return IconCompat.createWithResource(mContext, R.drawable.ic_media_stream).setTint(
                    Utils.getColorAccentDefaultColor(mContext));
        }
        final MediaMetadata metadata = mMediaController.getMetadata();
        if (metadata != null) {
            final Bitmap bitmap = metadata.getDescription().getIconBitmap();
            if (bitmap != null) {
                return IconCompat.createWithBitmap(bitmap);
            }
        }
        Log.d(TAG, "Media meta data does not contain icon information");
        return getPackageIcon();
    }

    private IconCompat getPackageIcon() {
        try {
            final Drawable drawable = mContext.getPackageManager().getApplicationIcon(mPackageName);
            if (drawable instanceof BitmapDrawable) {
                return IconCompat.createWithBitmap(((BitmapDrawable) drawable).getBitmap());
            }
            final Bitmap bitmap = Bitmap.createBitmap(drawable.getIntrinsicWidth(),
                    drawable.getIntrinsicHeight(), Bitmap.Config.ARGB_8888);
            final Canvas canvas = new Canvas(bitmap);
            drawable.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
            drawable.draw(canvas);

            return IconCompat.createWithBitmap(bitmap);
        } catch (PackageManager.NameNotFoundException e) {
            Log.e(TAG, "Package is not found. Unable to get package icon.");
        }
        return null;
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
