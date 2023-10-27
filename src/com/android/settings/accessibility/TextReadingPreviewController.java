/*
 * Copyright (C) 2022 The Android Open Source Project
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

package com.android.settings.accessibility;

import android.content.Context;
import android.content.res.Configuration;
import android.content.res.TypedArray;
import android.os.SystemClock;
import android.util.Log;
import android.view.Choreographer;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.VisibleForTesting;
import androidx.preference.PreferenceScreen;

import com.android.settings.R;
import com.android.settings.accessibility.TextReadingPreferenceFragment.EntryPoint;
import com.android.settings.core.BasePreferenceController;
import com.android.settings.core.instrumentation.SettingsStatsLog;
import com.android.settings.display.PreviewPagerAdapter;
import com.android.settings.flags.Flags;
import com.android.settings.widget.LabeledSeekBarPreference;

import java.util.Objects;

/**
 * A {@link BasePreferenceController} for controlling the preview pager of the text and reading
 * options.
 */
class TextReadingPreviewController extends BasePreferenceController implements
        PreviewSizeSeekBarController.ProgressInteractionListener {
    private static final String TAG = "TextReadingPreviewCtrl";
    private static final int LAYER_INITIAL_INDEX = 0;
    private static final int FRAME_INITIAL_INDEX = 0;
    private static final int[] PREVIEW_SAMPLE_RES_IDS = new int[]{
            R.layout.accessibility_text_reading_preview_app_grid,
            R.layout.screen_zoom_preview_1,
            R.layout.accessibility_text_reading_preview_mail_content};
    private static final String PREVIEW_KEY = "preview";
    private static final String FONT_SIZE_KEY = "font_size";
    private static final String DISPLAY_SIZE_KEY = "display_size";
    private static final long MIN_COMMIT_INTERVAL_MS = 800;
    private static final long CHANGE_BY_SEEKBAR_DELAY_MS = 100;
    private static final long CHANGE_BY_BUTTON_DELAY_MS = 300;
    private final FontSizeData mFontSizeData;
    private final DisplaySizeData mDisplaySizeData;
    private int mLastFontProgress;
    private int mLastDisplayProgress;
    private long mLastCommitTime;
    private TextReadingPreviewPreference mPreviewPreference;
    private LabeledSeekBarPreference mFontSizePreference;
    private LabeledSeekBarPreference mDisplaySizePreference;

    @EntryPoint
    private int mEntryPoint;

    private final Choreographer.FrameCallback mCommit = f -> {
        tryCommitFontSizeConfig();
        tryCommitDisplaySizeConfig();

        mLastCommitTime = SystemClock.elapsedRealtime();
    };

    TextReadingPreviewController(Context context, String preferenceKey,
            @NonNull FontSizeData fontSizeData, @NonNull DisplaySizeData displaySizeData) {
        super(context, preferenceKey);
        mFontSizeData = fontSizeData;
        mDisplaySizeData = displaySizeData;
    }

    @Override
    public int getAvailabilityStatus() {
        return AVAILABLE;
    }

    @Override
    public void displayPreference(PreferenceScreen screen) {
        super.displayPreference(screen);

        mPreviewPreference = screen.findPreference(PREVIEW_KEY);

        mFontSizePreference = screen.findPreference(FONT_SIZE_KEY);
        mDisplaySizePreference = screen.findPreference(DISPLAY_SIZE_KEY);
        Objects.requireNonNull(mFontSizePreference,
                /* message= */ "Font size preference is null, the preview controller "
                        + "couldn't get the info");
        Objects.requireNonNull(mDisplaySizePreference,
                /* message= */ "Display size preference is null, the preview controller"
                        + " couldn't get the info");

        mLastFontProgress = mFontSizeData.getInitialIndex();
        mLastDisplayProgress = mDisplaySizeData.getInitialIndex();

        final Configuration origConfig = mContext.getResources().getConfiguration();
        final boolean isLayoutRtl =
                origConfig.getLayoutDirection() == View.LAYOUT_DIRECTION_RTL;
        final int[] previewSamples = getPreviewSampleLayouts(mContext);
        final PreviewPagerAdapter pagerAdapter = new PreviewPagerAdapter(mContext, isLayoutRtl,
                previewSamples, createConfig(origConfig));
        mPreviewPreference.setPreviewAdapter(pagerAdapter);
        mPreviewPreference.setCurrentItem(
                isLayoutRtl ? previewSamples.length - 1 : FRAME_INITIAL_INDEX);

        final int initialPagerIndex =
                mLastFontProgress * mDisplaySizeData.getValues().size() + mLastDisplayProgress;
        mPreviewPreference.setLastLayerIndex(initialPagerIndex);
        pagerAdapter.setPreviewLayer(initialPagerIndex, LAYER_INITIAL_INDEX,
                FRAME_INITIAL_INDEX, /* animate= */ false);
    }

    @Override
    public void notifyPreferenceChanged() {
        mPreviewPreference.notifyPreviewPagerChanged(getPagerIndex());
    }

    @Override
    public void onProgressChanged() {
        postCommitDelayed(CHANGE_BY_BUTTON_DELAY_MS);
    }

    @Override
    public void onEndTrackingTouch() {
        postCommitDelayed(CHANGE_BY_SEEKBAR_DELAY_MS);
    }

    void setCurrentItem(int index) {
        mPreviewPreference.setCurrentItem(index);
    }

    int getCurrentItem() {
        return mPreviewPreference.getCurrentItem();
    }

    /**
     * The entry point is used for logging.
     *
     * @param entryPoint from which settings page
     */
    void setEntryPoint(@EntryPoint int entryPoint) {
        mEntryPoint = entryPoint;
    }

    /**
     * Avoids the flicker when switching to the previous or next level.
     *
     * <p><br>[Flickering problem steps] commit()-> snapshot in framework(old screenshot) ->
     * app update the preview -> snapshot(old screen) fade out</p>
     *
     * <p><br>To prevent flickering problem, we make sure that we update the local preview
     * first and then we do the commit later. </p>
     *
     * <p><br><b>Note:</b> It doesn't matter that we use
     * Choreographer or main thread handler since the delay time is longer
     * than 1 frame. Use Choreographer to let developer understand it's a
     * window update.</p>
     *
     * @param commitDelayMs the interval time after a action.
     */
    void postCommitDelayed(long commitDelayMs) {
        if (SystemClock.elapsedRealtime() - mLastCommitTime < MIN_COMMIT_INTERVAL_MS) {
            commitDelayMs += MIN_COMMIT_INTERVAL_MS;
        }

        final Choreographer choreographer = Choreographer.getInstance();
        choreographer.removeFrameCallback(mCommit);
        choreographer.postFrameCallbackDelayed(mCommit, commitDelayMs);
    }

    @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
    static int[] getPreviewSampleLayouts(Context context) {
        if (!Flags.accessibilityCustomizeTextReadingPreview()) {
            return PREVIEW_SAMPLE_RES_IDS;
        }
        TypedArray previews = context.getResources().obtainTypedArray(
                R.array.config_text_reading_preview_samples);
        int previewCount = previews.length();
        int[] previewSamples = new int[previewCount];
        for (int i = 0; i < previewCount; i++) {
            previewSamples[i] = previews.getResourceId(i, R.layout.screen_zoom_preview_1);
        }
        previews.recycle();
        return previewSamples;
    }

    private int getPagerIndex() {
        final int displayDataSize = mDisplaySizeData.getValues().size();
        final int fontSizeProgress = mFontSizePreference.getProgress();
        final int displaySizeProgress = mDisplaySizePreference.getProgress();

        // To be consistent with the {@link PreviewPagerAdapter#setPreviewLayer(int, int, int,
        // boolean)} behavior, here also needs the same design. In addition, please also refer to
        // the {@link #createConfig(Configuration)}.
        return fontSizeProgress * displayDataSize + displaySizeProgress;
    }

    private void tryCommitFontSizeConfig() {
        final int fontProgress = mFontSizePreference.getProgress();
        if (fontProgress != mLastFontProgress) {
            mFontSizeData.commit(fontProgress);
            mLastFontProgress = fontProgress;

            if (Log.isLoggable(TAG, Log.DEBUG)) {
                Log.d(TAG, "Font size: " + fontProgress);
            }

            SettingsStatsLog.write(
                    SettingsStatsLog.ACCESSIBILITY_TEXT_READING_OPTIONS_CHANGED,
                    AccessibilityStatsLogUtils.convertToItemKeyName(mFontSizePreference.getKey()),
                    fontProgress,
                    AccessibilityStatsLogUtils.convertToEntryPoint(mEntryPoint));
        }
    }

    private void tryCommitDisplaySizeConfig() {
        final int displayProgress = mDisplaySizePreference.getProgress();
        if (displayProgress != mLastDisplayProgress) {
            mDisplaySizeData.commit(displayProgress);
            mLastDisplayProgress = displayProgress;

            if (Log.isLoggable(TAG, Log.DEBUG)) {
                Log.d(TAG, "Display size: " + displayProgress);
            }

            SettingsStatsLog.write(
                    SettingsStatsLog.ACCESSIBILITY_TEXT_READING_OPTIONS_CHANGED,
                    AccessibilityStatsLogUtils.convertToItemKeyName(
                            mDisplaySizePreference.getKey()),
                    displayProgress,
                    AccessibilityStatsLogUtils.convertToEntryPoint(mEntryPoint));
        }
    }

    private Configuration[] createConfig(Configuration origConfig) {
        final int fontDataSize = mFontSizeData.getValues().size();
        final int displayDataSize = mDisplaySizeData.getValues().size();
        final int totalNum = fontDataSize * displayDataSize;
        final Configuration[] configurations = new Configuration[totalNum];

        for (int i = 0; i < fontDataSize; ++i) {
            for (int j = 0; j < displayDataSize; ++j) {
                final Configuration config = new Configuration(origConfig);
                config.fontScale = mFontSizeData.getValues().get(i);
                config.densityDpi = mDisplaySizeData.getValues().get(j);

                configurations[i * displayDataSize + j] = config;
            }
        }

        return configurations;
    }
}
