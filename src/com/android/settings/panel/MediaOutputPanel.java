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

import static androidx.lifecycle.Lifecycle.Event.ON_START;
import static androidx.lifecycle.Lifecycle.Event.ON_STOP;

import static com.android.settings.media.MediaOutputSlice.MEDIA_PACKAGE_NAME;
import static com.android.settings.slices.CustomSliceRegistry.MEDIA_OUTPUT_SLICE_URI;

import android.app.settings.SettingsEnums;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.media.MediaMetadata;
import android.media.session.MediaController;
import android.media.session.MediaSessionManager;
import android.media.session.PlaybackState;
import android.net.Uri;
import android.text.TextUtils;
import android.util.Log;

import androidx.core.graphics.drawable.IconCompat;
import androidx.lifecycle.LifecycleObserver;
import androidx.lifecycle.OnLifecycleEvent;

import com.android.internal.annotations.VisibleForTesting;
import com.android.settings.R;
import com.android.settings.Utils;
import com.android.settingslib.media.InfoMediaDevice;
import com.android.settingslib.media.LocalMediaManager;
import com.android.settingslib.media.MediaDevice;
import com.android.settingslib.media.MediaOutputSliceConstants;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents the Media output Panel.
 *
 * <p>
 * Displays Media output item
 * </p>
 */
public class MediaOutputPanel implements PanelContent, LocalMediaManager.DeviceCallback,
        LifecycleObserver {

    private static final String TAG = "MediaOutputPanel";

    private final Context mContext;
    private final String mPackageName;

    @VisibleForTesting
    LocalMediaManager mLocalMediaManager;

    private PanelContentCallback mCallback;
    private boolean mIsCustomizedButtonUsed = true;
    private MediaSessionManager mMediaSessionManager;
    private MediaController mMediaController;

    public static MediaOutputPanel create(Context context, String packageName) {
        // Redirect to new media output dialog
        context.sendBroadcast(new Intent()
                .addFlags(Intent.FLAG_RECEIVER_FOREGROUND)
                .setPackage(MediaOutputSliceConstants.SYSTEMUI_PACKAGE_NAME)
                .setAction(MediaOutputSliceConstants.ACTION_LAUNCH_MEDIA_OUTPUT_DIALOG)
                .putExtra(MediaOutputSliceConstants.EXTRA_PACKAGE_NAME, packageName));
        return null;
    }

    private MediaOutputPanel(Context context, String packageName) {
        mContext = context.getApplicationContext();
        mPackageName = TextUtils.isEmpty(packageName) ? "" : packageName;
    }

    @Override
    public CharSequence getTitle() {
        if (mMediaController != null) {
            final MediaMetadata metadata = mMediaController.getMetadata();
            if (metadata != null) {
                return metadata.getDescription().getTitle();
            }
        }
        return mContext.getText(R.string.media_volume_title);
    }

    @Override
    public CharSequence getSubTitle() {
        if (mMediaController != null) {
            final MediaMetadata metadata = mMediaController.getMetadata();
            if (metadata != null) {
                return metadata.getDescription().getSubtitle();
            }
        }
        return mContext.getText(R.string.media_output_panel_title);
    }

    @Override
    public IconCompat getIcon() {
        if (mMediaController == null) {
            return null;
        }
        final MediaMetadata metadata = mMediaController.getMetadata();
        if (metadata != null) {
            final Bitmap bitmap = metadata.getDescription().getIconBitmap();
            if (bitmap != null) {
                final Bitmap roundBitmap = Utils.convertCornerRadiusBitmap(mContext, bitmap,
                        (float) mContext.getResources().getDimensionPixelSize(
                                R.dimen.output_switcher_panel_icon_corner_radius));

                return IconCompat.createWithBitmap(roundBitmap);
            }
        }
        Log.d(TAG, "Media meta data does not contain icon information");
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
    public boolean isCustomizedButtonUsed() {
        return mIsCustomizedButtonUsed;
    }

    @Override
    public CharSequence getCustomizedButtonTitle() {
        return mContext.getText(R.string.service_stop);
    }

    @Override
    public void onClickCustomizedButton() {
        mLocalMediaManager.releaseSession();
    }

    @Override
    public void registerCallback(PanelContentCallback callback) {
        mCallback = callback;
    }

    @Override
    public int getMetricsCategory() {
        return SettingsEnums.PANEL_MEDIA_OUTPUT;
    }

    @Override
    public void onSelectedDeviceStateChanged(MediaDevice device, int state) {
        dispatchCustomButtonStateChanged();
    }

    @Override
    public void onDeviceListUpdate(List<MediaDevice> devices) {
        dispatchCustomButtonStateChanged();
    }

    @Override
    public void onDeviceAttributesChanged() {
        dispatchCustomButtonStateChanged();
    }

    private void dispatchCustomButtonStateChanged() {
        hideCustomButtonIfNecessary();
        if (mCallback != null) {
            mCallback.onCustomizedButtonStateChanged();
        }
    }

    private void hideCustomButtonIfNecessary() {
        final MediaDevice device = mLocalMediaManager.getCurrentConnectedDevice();
        mIsCustomizedButtonUsed = device instanceof InfoMediaDevice;
    }

    @OnLifecycleEvent(ON_START)
    public void onStart() {
        if (!TextUtils.isEmpty(mPackageName)) {
            mMediaSessionManager = mContext.getSystemService(MediaSessionManager.class);
            for (MediaController controller : mMediaSessionManager.getActiveSessions(null)) {
                if (TextUtils.equals(controller.getPackageName(), mPackageName)) {
                    mMediaController = controller;
                    mMediaController.registerCallback(mCb);
                    mCallback.onHeaderChanged();
                    break;
                }
            }
        }
        if (mMediaController == null) {
            Log.d(TAG, "No media controller for " + mPackageName);
        }
        if (mLocalMediaManager == null) {
            mLocalMediaManager = new LocalMediaManager(mContext, mPackageName, null);
        }
        mLocalMediaManager.registerCallback(this);
        mLocalMediaManager.startScan();
    }

    @OnLifecycleEvent(ON_STOP)
    public void onStop() {
        if (mMediaController != null) {
            mMediaController.unregisterCallback(mCb);
        }
        mLocalMediaManager.unregisterCallback(this);
        mLocalMediaManager.stopScan();
    }

    @Override
    public int getViewType() {
        return PanelContent.VIEW_TYPE_SLIDER_LARGE_ICON;
    }

    private final MediaController.Callback mCb = new MediaController.Callback() {
        @Override
        public void onMetadataChanged(MediaMetadata metadata) {
            if (mCallback != null) {
                mCallback.onHeaderChanged();
            }
        }

        @Override
        public void onPlaybackStateChanged(PlaybackState state) {
            final int playState = state.getState();
            if (mCallback != null && (playState == PlaybackState.STATE_STOPPED
                    || playState == PlaybackState.STATE_PAUSED)) {
                mCallback.forceClose();
            }
        }
    };
}
