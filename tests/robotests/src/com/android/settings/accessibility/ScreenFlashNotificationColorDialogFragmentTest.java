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

import static android.content.DialogInterface.BUTTON_NEGATIVE;
import static android.content.DialogInterface.BUTTON_NEUTRAL;
import static android.content.DialogInterface.BUTTON_POSITIVE;

import static com.android.settings.accessibility.FlashNotificationsUtil.ACTION_FLASH_NOTIFICATION_START_PREVIEW;
import static com.android.settings.accessibility.FlashNotificationsUtil.ACTION_FLASH_NOTIFICATION_STOP_PREVIEW;
import static com.android.settings.accessibility.FlashNotificationsUtil.EXTRA_FLASH_NOTIFICATION_PREVIEW_COLOR;
import static com.android.settings.accessibility.FlashNotificationsUtil.EXTRA_FLASH_NOTIFICATION_PREVIEW_TYPE;
import static com.android.settings.accessibility.FlashNotificationsUtil.TYPE_LONG_PREVIEW;
import static com.android.settings.accessibility.FlashNotificationsUtil.TYPE_SHORT_PREVIEW;
import static com.android.settings.accessibility.ScreenFlashNotificationColor.AZURE;
import static com.android.settings.accessibility.ScreenFlashNotificationColor.BLUE;
import static com.android.settings.accessibility.ScreenFlashNotificationColor.CYAN;
import static com.android.settings.accessibility.ScreenFlashNotificationColor.ROSE;

import static com.google.common.truth.Truth.assertThat;

import static org.robolectric.Shadows.shadowOf;

import android.content.Intent;
import android.graphics.Color;

import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.FragmentActivity;

import com.android.settings.R;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.shadows.ShadowContextWrapper;

import java.util.ArrayList;
import java.util.List;

@RunWith(RobolectricTestRunner.class)
public class ScreenFlashNotificationColorDialogFragmentTest {

    private ShadowContextWrapper mShadowContextWrapper;
    private ScreenFlashNotificationColorDialogFragment mDialogFragment;
    private AlertDialog mAlertDialog;
    private ColorSelectorLayout mColorSelectorLayout;
    private int mCurrentColor;

    @Before
    public void setUp() {
        FragmentActivity fragmentActivity = Robolectric.setupActivity(FragmentActivity.class);
        mShadowContextWrapper = shadowOf(fragmentActivity);

        mCurrentColor = ROSE.mColorInt;
        mDialogFragment = ScreenFlashNotificationColorDialogFragment.getInstance(
                mCurrentColor, selectedColor -> mCurrentColor = selectedColor
        );
        mDialogFragment.show(fragmentActivity.getSupportFragmentManager(), "test");

        mAlertDialog = (AlertDialog) mDialogFragment.getDialog();
        if (mAlertDialog != null) {
            mColorSelectorLayout = mAlertDialog.findViewById(R.id.color_selector_preference);
        }
    }

    @Test
    public void test_assertShow() {
        assertThat(mAlertDialog.isShowing()).isTrue();
    }

    @Test
    public void clickNeutral_assertShow() {
        performClickOnDialog(BUTTON_NEUTRAL);
        assertThat(mAlertDialog.isShowing()).isTrue();
    }

    @Test
    public void clickNeutral_assertStartPreview() throws InterruptedException {
        performClickOnDialog(BUTTON_NEUTRAL);
        Thread.sleep(100);

        Intent captured = getLastCapturedIntent();
        assertThat(captured.getAction()).isEqualTo(ACTION_FLASH_NOTIFICATION_START_PREVIEW);
        assertThat(captured.getIntExtra(EXTRA_FLASH_NOTIFICATION_PREVIEW_TYPE, TYPE_SHORT_PREVIEW))
                .isEqualTo(TYPE_LONG_PREVIEW);
        assertThat(captured.getIntExtra(EXTRA_FLASH_NOTIFICATION_PREVIEW_COLOR, Color.TRANSPARENT))
                .isEqualTo(ROSE.mColorInt);
    }

    @Test
    public void clickNegative_assertNotShow() {
        performClickOnDialog(BUTTON_NEGATIVE);
        assertThat(mAlertDialog.isShowing()).isFalse();
    }

    @Test
    public void clickPositive_assertNotShow() {
        performClickOnDialog(BUTTON_POSITIVE);
        assertThat(mAlertDialog.isShowing()).isFalse();
    }

    @Test
    public void clickNeutralAndPause_assertStopPreview() throws InterruptedException {
        performClickOnDialog(BUTTON_NEUTRAL);
        Thread.sleep(100);
        mDialogFragment.onPause();
        Thread.sleep(100);

        assertThat(getLastCapturedIntent().getAction())
                .isEqualTo(ACTION_FLASH_NOTIFICATION_STOP_PREVIEW);
    }

    @Test
    public void clickNeutralAndClickNegative_assertStopPreview() throws InterruptedException {
        performClickOnDialog(BUTTON_NEUTRAL);
        Thread.sleep(100);
        performClickOnDialog(BUTTON_NEGATIVE);
        Thread.sleep(100);

        assertThat(getLastCapturedIntent().getAction())
                .isEqualTo(ACTION_FLASH_NOTIFICATION_STOP_PREVIEW);
    }

    @Test
    public void clickNeutralAndClickPositive_assertStopPreview() throws InterruptedException {
        performClickOnDialog(BUTTON_NEUTRAL);
        Thread.sleep(100);
        performClickOnDialog(BUTTON_POSITIVE);
        Thread.sleep(100);

        assertThat(getLastCapturedIntent().getAction())
                .isEqualTo(ACTION_FLASH_NOTIFICATION_STOP_PREVIEW);
    }

    @Test
    public void clickNeutralAndClickColor_assertStartPreview() throws InterruptedException {
        performClickOnDialog(BUTTON_NEUTRAL);
        Thread.sleep(100);
        checkColorButton(CYAN);
        Thread.sleep(500);

        Intent captured = getLastCapturedIntent();
        assertThat(captured.getAction()).isEqualTo(ACTION_FLASH_NOTIFICATION_START_PREVIEW);
        assertThat(captured.getIntExtra(EXTRA_FLASH_NOTIFICATION_PREVIEW_TYPE, TYPE_SHORT_PREVIEW))
                .isEqualTo(TYPE_LONG_PREVIEW);
        assertThat(captured.getIntExtra(EXTRA_FLASH_NOTIFICATION_PREVIEW_COLOR, Color.TRANSPARENT))
                .isEqualTo(CYAN.mColorInt);
    }

    @Test
    public void clickColorAndClickNegative_assertColor() {
        checkColorButton(AZURE);
        performClickOnDialog(BUTTON_NEGATIVE);

        assertThat(mCurrentColor).isEqualTo(ROSE.mColorInt);
    }

    @Test
    public void clickColorAndClickPositive_assertColor() {
        checkColorButton(BLUE);
        performClickOnDialog(BUTTON_POSITIVE);

        assertThat(mCurrentColor).isEqualTo(BLUE.mColorInt);
    }

    private void checkColorButton(ScreenFlashNotificationColor color) {
        mColorSelectorLayout.setCheckedColor(color.mColorInt);
    }

    private void performClickOnDialog(int whichButton) {
        mAlertDialog.getButton(whichButton).performClick();
    }

    private Intent getLastCapturedIntent() {
        final List<Intent> capturedIntents = new ArrayList<>(
                mShadowContextWrapper.getBroadcastIntents());
        final int size = capturedIntents.size();
        return capturedIntents.get(size - 1);
    }
}
