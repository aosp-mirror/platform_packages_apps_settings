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
import androidx.fragment.app.testing.FragmentScenario;
import androidx.lifecycle.Lifecycle;

import com.android.settings.R;
import com.android.settings.testutils.FakeTimer;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.shadows.ShadowLooper;
import org.robolectric.util.ReflectionHelpers;

import java.util.List;
import java.util.Timer;
import java.util.function.Consumer;

@RunWith(RobolectricTestRunner.class)
public class ScreenFlashNotificationColorDialogFragmentTest {

    private FragmentScenario<TestScreenFlashNotificationColorDialogFragment> mFragmentScenario;
    private ScreenFlashNotificationColorDialogFragment mDialogFragment;
    private AlertDialog mAlertDialog;
    private ColorSelectorLayout mColorSelectorLayout;
    private int mCurrentColor;

    @Before
    public void setUp() {
        mCurrentColor = ROSE.mColorInt;
        mFragmentScenario = FragmentScenario.launch(
                TestScreenFlashNotificationColorDialogFragment.class,
                /* fragmentArgs= */ null,
                R.style.Theme_AlertDialog_SettingsLib,
                Lifecycle.State.INITIALIZED);
        setupFragment();
    }

    @After
    public void cleanUp() {
        mFragmentScenario.close();
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
    public void clickNeutral_assertStartPreview() {
        performClickOnDialog(BUTTON_NEUTRAL);
        getTimerFromFragment().runOneTask();

        assertStartPreview(ROSE.mColorInt);
    }

    @Test
    public void clickNeutral_flushAllScheduledTasks_assertStopPreview() {
        performClickOnDialog(BUTTON_NEUTRAL);
        getTimerFromFragment().runAllTasks();

        assertStopPreview();
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
    public void clickNeutralAndPause_assertStopPreview() {
        performClickOnDialog(BUTTON_NEUTRAL);
        getTimerFromFragment().runOneTask();
        // move the state from RESUMED to CREATED to make fragment's onPause() to be called
        mFragmentScenario.moveToState(Lifecycle.State.CREATED);

        assertStopPreview();
    }

    @Test
    public void clickNeutralAndClickNegative_assertStopPreview() {
        performClickOnDialog(BUTTON_NEUTRAL);
        getTimerFromFragment().runOneTask();
        performClickOnDialog(BUTTON_NEGATIVE);

        assertStopPreview();
    }

    @Test
    public void clickNeutralAndClickPositive_assertStopPreview() {
        performClickOnDialog(BUTTON_NEUTRAL);
        getTimerFromFragment().runOneTask();
        performClickOnDialog(BUTTON_POSITIVE);

        assertStopPreview();
    }

    @Test
    public void clickNeutralAndClickColor_assertStartPreview() {
        performClickOnDialog(BUTTON_NEUTRAL);
        getTimerFromFragment().runOneTask();
        checkColorButton(CYAN);
        // When changing the color while the preview is running, the fragment will schedule three
        // tasks: stop the current preview, start the new preview, stop the new preview
        int numOfPendingTasks = getTimerFromFragment().numOfPendingTasks();
        // Run all the pending tasks except the last one
        while (numOfPendingTasks > 1) {
            getTimerFromFragment().runOneTask();
            numOfPendingTasks--;
        }

        assertStartPreview(CYAN.mColorInt);
    }

    @Test
    public void clickColorAndClickNegative_assertColor() {
        checkColorButton(AZURE);
        performClickOnDialog(BUTTON_NEGATIVE);

        assertThat(getTimerFromFragment()).isNull();
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
        ShadowLooper.runUiThreadTasksIncludingDelayedTasks();
    }

    private Intent getLastCapturedIntent() {
        final List<Intent> capturedIntents =
                shadowOf(RuntimeEnvironment.getApplication()).getBroadcastIntents();
        final int size = capturedIntents.size();
        return capturedIntents.get(size - 1);
    }

    private void setupFragment() {
        mFragmentScenario.onFragment(fragment -> {
            ReflectionHelpers.setField(fragment, "mCurrentColor", mCurrentColor);
            ReflectionHelpers.setField(fragment, "mConsumer",
                    (Consumer<Integer>) selectedColor -> mCurrentColor = selectedColor);
        });
        mFragmentScenario.moveToState(Lifecycle.State.RESUMED);

        mFragmentScenario.onFragment(fragment -> {
            assertThat(fragment.getDialog()).isNotNull();
            assertThat(fragment.requireDialog().isShowing()).isTrue();
            assertThat(fragment.requireDialog()).isInstanceOf(AlertDialog.class);
            mAlertDialog = (AlertDialog) fragment.requireDialog();
            mDialogFragment = fragment;
            mColorSelectorLayout = mAlertDialog.findViewById(R.id.color_selector_preference);
        });
    }

    private FakeTimer getTimerFromFragment() {
        return (FakeTimer) ReflectionHelpers.getField(mDialogFragment, "mTimer");
    }

    private void assertStartPreview(int color) {
        Intent captured = getLastCapturedIntent();
        assertThat(captured.getAction()).isEqualTo(ACTION_FLASH_NOTIFICATION_START_PREVIEW);
        assertThat(captured.getIntExtra(EXTRA_FLASH_NOTIFICATION_PREVIEW_TYPE, TYPE_SHORT_PREVIEW))
                .isEqualTo(TYPE_LONG_PREVIEW);
        assertThat(captured.getIntExtra(EXTRA_FLASH_NOTIFICATION_PREVIEW_COLOR, Color.TRANSPARENT))
                .isEqualTo(color);
    }

    private void assertStopPreview() {
        assertThat(getTimerFromFragment().numOfPendingTasks()).isEqualTo(0);
        assertThat(getLastCapturedIntent().getAction())
                .isEqualTo(ACTION_FLASH_NOTIFICATION_STOP_PREVIEW);
    }

    /**
     * A {@link ScreenFlashNotificationColorDialogFragment} that uses a fake timer so that it won't
     * create unmanageable timer threads during test.
     */
    public static class TestScreenFlashNotificationColorDialogFragment extends
            ScreenFlashNotificationColorDialogFragment {

        @Override
        Timer createTimer() {
            return new FakeTimer();
        }
    }
}
