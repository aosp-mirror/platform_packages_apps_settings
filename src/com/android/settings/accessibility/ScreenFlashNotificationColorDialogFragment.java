/*
 * Copyright (C) 2023 The Android Open Source Project
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

import static com.android.settings.accessibility.FlashNotificationsUtil.ACTION_FLASH_NOTIFICATION_START_PREVIEW;
import static com.android.settings.accessibility.FlashNotificationsUtil.ACTION_FLASH_NOTIFICATION_STOP_PREVIEW;
import static com.android.settings.accessibility.FlashNotificationsUtil.DEFAULT_SCREEN_FLASH_COLOR;
import static com.android.settings.accessibility.FlashNotificationsUtil.EXTRA_FLASH_NOTIFICATION_PREVIEW_COLOR;
import static com.android.settings.accessibility.FlashNotificationsUtil.EXTRA_FLASH_NOTIFICATION_PREVIEW_TYPE;
import static com.android.settings.accessibility.FlashNotificationsUtil.TYPE_LONG_PREVIEW;

import android.app.Dialog;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.os.UserHandle;
import android.view.View;

import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;

import com.android.settings.R;

import java.util.Timer;
import java.util.TimerTask;
import java.util.function.Consumer;


/**
 * DialogFragment for Screen flash notification color picker.
 */
public class ScreenFlashNotificationColorDialogFragment extends DialogFragment implements
        ColorSelectorLayout.OnCheckedChangeListener {

    private static final int PREVIEW_LONG_TIME_MS = 5000;
    private static final int BETWEEN_STOP_AND_START_DELAY_MS = 250;
    private static final int MARGIN_FOR_STOP_DELAY_MS = 100;

    @ColorInt
    private int mCurrentColor = Color.TRANSPARENT;
    private Consumer<Integer> mConsumer;

    private Timer mTimer = null;
    private Boolean mIsPreview = false;

    static ScreenFlashNotificationColorDialogFragment getInstance(int initialColor,
            Consumer<Integer> colorConsumer) {
        final ScreenFlashNotificationColorDialogFragment result =
                new ScreenFlashNotificationColorDialogFragment();
        result.mCurrentColor = initialColor;
        result.mConsumer = colorConsumer != null ? colorConsumer : i -> {
        };
        return result;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        final View dialogView = getLayoutInflater().inflate(R.layout.layout_color_selector_dialog,
                null);

        final ColorSelectorLayout colorSelectorLayout = dialogView.findViewById(
                R.id.color_selector_preference);
        if (colorSelectorLayout != null) {
            colorSelectorLayout.setOnCheckedChangeListener(this);
            colorSelectorLayout.setCheckedColor(mCurrentColor);
        }

        final AlertDialog createdDialog = new AlertDialog.Builder(getContext())
                .setView(dialogView)
                .setTitle(R.string.screen_flash_notification_color_title)
                .setNeutralButton(R.string.flash_notifications_preview, null)
                .setNegativeButton(R.string.color_selector_dialog_cancel, (dialog, which) -> {
                })
                .setPositiveButton(R.string.color_selector_dialog_done, (dialog, which) -> {
                    mCurrentColor = colorSelectorLayout.getCheckedColor(DEFAULT_SCREEN_FLASH_COLOR);
                    mConsumer.accept(mCurrentColor);
                })
                .create();
        createdDialog.setOnShowListener(
                dialogInterface -> createdDialog.getButton(AlertDialog.BUTTON_NEUTRAL)
                        .setOnClickListener(v -> showColor()));

        return createdDialog;
    }

    @Override
    public void onPause() {
        super.onPause();
        cancelPreviewTask();
    }

    @Override
    public void onCheckedChanged(ColorSelectorLayout group) {
        mCurrentColor = group.getCheckedColor(DEFAULT_SCREEN_FLASH_COLOR);
        if (mIsPreview) {
            showColor();
        }
    }

    private void showColor() {
        int startDelay = 0;

        synchronized (this) {
            if (mTimer != null) mTimer.cancel();

            mTimer = createTimer();
            if (mIsPreview) {
                mTimer.schedule(getStopTask(), 0);
                startDelay = BETWEEN_STOP_AND_START_DELAY_MS;
            }
            mTimer.schedule(getStartTask(), startDelay);
            mTimer.schedule(getStopTask(),
                    startDelay + PREVIEW_LONG_TIME_MS + MARGIN_FOR_STOP_DELAY_MS);
        }
    }

    private TimerTask getStartTask() {
        return new TimerTask() {
            @Override
            public void run() {
                synchronized (this) {
                    startPreviewLocked();
                }
            }
        };
    }

    private TimerTask getStopTask() {
        return new TimerTask() {
            @Override
            public void run() {
                synchronized (this) {
                    stopPreviewLocked();
                }
            }
        };
    }

    private void cancelPreviewTask() {
        synchronized (this) {
            if (mTimer != null) mTimer.cancel();
            stopPreviewLocked();
        }
    }

    private void startPreviewLocked() {
        if (getContext() == null) return;

        mIsPreview = true;
        Intent intent = new Intent(ACTION_FLASH_NOTIFICATION_START_PREVIEW);
        intent.putExtra(EXTRA_FLASH_NOTIFICATION_PREVIEW_TYPE, TYPE_LONG_PREVIEW);
        intent.putExtra(EXTRA_FLASH_NOTIFICATION_PREVIEW_COLOR, mCurrentColor);
        getContext().sendBroadcastAsUser(intent, UserHandle.SYSTEM);
    }

    private void stopPreviewLocked() {
        if (getContext() == null) return;

        Intent stopIntent = new Intent(ACTION_FLASH_NOTIFICATION_STOP_PREVIEW);
        getContext().sendBroadcastAsUser(stopIntent, UserHandle.SYSTEM);
        mIsPreview = false;
    }

    Timer createTimer() {
        return new Timer();
    }
}
