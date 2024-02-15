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

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.widget.SeekBar;

import androidx.annotation.NonNull;
import androidx.preference.PreferenceScreen;

import com.android.settings.R;
import com.android.settings.core.BasePreferenceController;
import com.android.settings.widget.LabeledSeekBarPreference;
import com.android.settingslib.core.lifecycle.LifecycleObserver;
import com.android.settingslib.core.lifecycle.events.OnCreate;
import com.android.settingslib.core.lifecycle.events.OnDestroy;
import com.android.settingslib.core.lifecycle.events.OnSaveInstanceState;

import com.google.android.setupcompat.util.WizardManagerHelper;

import java.util.Optional;

/**
 * The controller of {@link LabeledSeekBarPreference} that listens to display size and font size
 * settings changes and updates preview size threshold smoothly.
 */
abstract class PreviewSizeSeekBarController extends BasePreferenceController implements
        TextReadingResetController.ResetStateListener, LifecycleObserver, OnCreate,
        OnDestroy, OnSaveInstanceState {
    private final PreviewSizeData<? extends Number> mSizeData;
    private static final String KEY_SAVED_QS_TOOLTIP_RESHOW = "qs_tooltip_reshow";
    private boolean mSeekByTouch;
    private Optional<ProgressInteractionListener> mInteractionListener = Optional.empty();
    private LabeledSeekBarPreference mSeekBarPreference;
    private int mLastProgress;
    private boolean mNeedsQSTooltipReshow = false;
    private AccessibilityQuickSettingsTooltipWindow mTooltipWindow;
    private final Handler mHandler;

    private String[] mStateLabels = null;

    private final SeekBar.OnSeekBarChangeListener mSeekBarChangeListener =
            new SeekBar.OnSeekBarChangeListener() {
                @Override
                public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                    setSeekbarStateDescription(progress);

                    if (mInteractionListener.isEmpty()) {
                        return;
                    }

                    final ProgressInteractionListener interactionListener =
                            mInteractionListener.get();
                    // Avoid timing issues to update the corresponding preview fail when clicking
                    // the increase/decrease button.
                    seekBar.post(interactionListener::notifyPreferenceChanged);

                    if (!mSeekByTouch) {
                        interactionListener.onProgressChanged();
                        onProgressFinalized();
                    }
                }

                @Override
                public void onStartTrackingTouch(SeekBar seekBar) {
                    mSeekByTouch = true;
                }

                @Override
                public void onStopTrackingTouch(SeekBar seekBar) {
                    mSeekByTouch = false;

                    mInteractionListener.ifPresent(ProgressInteractionListener::onEndTrackingTouch);
                    onProgressFinalized();
                }
            };

    PreviewSizeSeekBarController(Context context, String preferenceKey,
            @NonNull PreviewSizeData<? extends Number> sizeData) {
        super(context, preferenceKey);
        mSizeData = sizeData;
        mHandler = new Handler(context.getMainLooper());
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        // Restore the tooltip.
        if (savedInstanceState != null
                && savedInstanceState.containsKey(KEY_SAVED_QS_TOOLTIP_RESHOW)) {
            mNeedsQSTooltipReshow = savedInstanceState.getBoolean(KEY_SAVED_QS_TOOLTIP_RESHOW);
        }
    }

    @Override
    public void onDestroy() {
        // remove runnables in the queue.
        mHandler.removeCallbacksAndMessages(null);
        final boolean isTooltipWindowShowing = mTooltipWindow != null && mTooltipWindow.isShowing();
        if (isTooltipWindowShowing) {
            mTooltipWindow.dismiss();
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        final boolean isTooltipWindowShowing = mTooltipWindow != null && mTooltipWindow.isShowing();
        if (mNeedsQSTooltipReshow || isTooltipWindowShowing) {
            outState.putBoolean(KEY_SAVED_QS_TOOLTIP_RESHOW, /* value= */ true);
        }
    }

    void setInteractionListener(ProgressInteractionListener interactionListener) {
        mInteractionListener = Optional.ofNullable(interactionListener);
    }

    @Override
    public int getAvailabilityStatus() {
        return AVAILABLE;
    }

    @Override
    public void displayPreference(PreferenceScreen screen) {
        super.displayPreference(screen);

        final int dataSize = mSizeData.getValues().size();
        final int initialIndex = mSizeData.getInitialIndex();
        mLastProgress = initialIndex;
        mSeekBarPreference = screen.findPreference(getPreferenceKey());
        mSeekBarPreference.setMax(dataSize - 1);
        mSeekBarPreference.setProgress(initialIndex);
        mSeekBarPreference.setContinuousUpdates(true);
        mSeekBarPreference.setOnSeekBarChangeListener(mSeekBarChangeListener);
        if (mNeedsQSTooltipReshow) {
            mHandler.post(this::showQuickSettingsTooltipIfNeeded);
        }
        setSeekbarStateDescription(mSeekBarPreference.getProgress());
    }

    @Override
    public void resetState() {
        final int defaultProgress = mSizeData.getValues().indexOf(mSizeData.getDefaultValue());
        mSeekBarPreference.setProgress(defaultProgress);

        // Immediately take the effect of updating the progress to avoid waiting for receiving
        // the event to delay update.
        mInteractionListener.ifPresent(ProgressInteractionListener::onProgressChanged);
    }

    /**
     * Stores the String array we would like to use for describing the state of seekbar progress
     * and updates the state description with current progress.
     *
     * @param labels The state descriptions to be announced for each progress.
     */
    public void setProgressStateLabels(String[] labels) {
        mStateLabels = labels;
        if (mStateLabels == null) {
            return;
        }
        updateState(mSeekBarPreference);
    }

    /**
     * Sets the state of seekbar based on current progress. The progress of seekbar is
     * corresponding to the index of the string array. If the progress is larger than or equals
     * to the length of the array, the state description is set to an empty string.
     */
    private void setSeekbarStateDescription(int index) {
        if (mStateLabels == null) {
            return;
        }
        mSeekBarPreference.setSeekBarStateDescription(
                (index < mStateLabels.length)
                        ? mStateLabels[index] : "");
    }

    private void onProgressFinalized() {
        // Using progress in SeekBarPreference since the progresses in
        // SeekBarPreference and seekbar are not always the same.
        // See {@link androidx.preference.Preference#callChangeListener(Object)}
        int seekBarPreferenceProgress = mSeekBarPreference.getProgress();
        if (seekBarPreferenceProgress != mLastProgress) {
            showQuickSettingsTooltipIfNeeded();
            mLastProgress = seekBarPreferenceProgress;
        }
    }

    private void showQuickSettingsTooltipIfNeeded() {
        final ComponentName tileComponentName = getTileComponentName();
        if (tileComponentName == null) {
            // Returns if no tile service assigned.
            return;
        }

        if (Flags.removeQsTooltipInSuw()
                && mContext instanceof Activity
                && WizardManagerHelper.isAnySetupWizard(((Activity) mContext).getIntent())) {
            // Don't show QuickSettingsTooltip in Setup Wizard
            return;
        }

        if (!mNeedsQSTooltipReshow && AccessibilityQuickSettingUtils.hasValueInSharedPreferences(
                mContext, tileComponentName)) {
            // Returns if quick settings tooltip only show once.
            return;
        }

        // TODO (287728819): Move tooltip showing to SystemUI
        // Since the lifecycle of controller is independent of that of the preference, doing
        // null check on seekbar is a temporary solution for the case that seekbar view
        // is not ready when we would like to show the tooltip.  If the seekbar is not ready,
        // we give up showing the tooltip and also do not reshow it in the future.
        if (mSeekBarPreference.getSeekbar() != null) {
            mTooltipWindow = new AccessibilityQuickSettingsTooltipWindow(mContext);
            mTooltipWindow.setup(getTileTooltipContent(),
                    R.drawable.accessibility_auto_added_qs_tooltip_illustration);
            mTooltipWindow.showAtTopCenter(mSeekBarPreference.getSeekbar());
        }
        AccessibilityQuickSettingUtils.optInValueToSharedPreferences(mContext,
                tileComponentName);
        mNeedsQSTooltipReshow = false;
    }

    /** Returns the accessibility Quick Settings tile component name. */
    abstract ComponentName getTileComponentName();

    /** Returns accessibility Quick Settings tile tooltip content. */
    abstract CharSequence getTileTooltipContent();


    /**
     * Interface for callbacks when users interact with the seek bar.
     */
    interface ProgressInteractionListener {

        /**
         * Called when the progress is changed.
         */
        void notifyPreferenceChanged();

        /**
         * Called when the progress is changed without tracking touch.
         */
        void onProgressChanged();

        /**
         * Called when the seek bar is end tracking.
         */
        void onEndTrackingTouch();
    }
}
