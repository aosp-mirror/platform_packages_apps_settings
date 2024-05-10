/*
 * Copyright (C) 2020 The Android Open Source Project
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

import static com.android.settings.accessibility.AccessibilityGestureNavigationTutorial.createAccessibilityTutorialDialog;
import static com.android.settings.accessibility.AccessibilityGestureNavigationTutorial.createAccessibilityTutorialDialogForSetupWizard;
import static com.android.settings.accessibility.AccessibilityGestureNavigationTutorial.createShortcutTutorialPages;
import static com.android.settings.accessibility.AccessibilityGestureNavigationTutorial.showGestureNavigationTutorialDialog;
import static com.android.settings.accessibility.AccessibilityUtil.UserShortcutType;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Mockito.verify;
import static org.robolectric.Shadows.shadowOf;

import android.app.Activity;
import android.app.settings.SettingsEnums;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.platform.test.annotations.RequiresFlagsEnabled;
import android.platform.test.flag.junit.CheckFlagsRule;
import android.platform.test.flag.junit.DeviceFlagsValueProvider;
import android.view.View;

import androidx.appcompat.app.AlertDialog;
import androidx.test.core.app.ApplicationProvider;

import com.android.server.accessibility.Flags;
import com.android.settings.SettingsActivity;
import com.android.settings.SubSettings;
import com.android.settingslib.core.instrumentation.MetricsFeatureProvider;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.LooperMode;

/** Tests for {@link AccessibilityGestureNavigationTutorial}. */
@RunWith(RobolectricTestRunner.class)
@LooperMode(LooperMode.Mode.LEGACY)
public final class AccessibilityGestureNavigationTutorialTest {

    @Rule
    public final MockitoRule mMockitoRule = MockitoJUnit.rule();
    @Rule
    public final CheckFlagsRule mCheckFlagsRule = DeviceFlagsValueProvider.createCheckFlagsRule();
    @Mock
    private DialogInterface.OnClickListener mOnClickListener;
    @Mock
    private DialogInterface.OnDismissListener mOnDismissListener;
    private final Context mContext = ApplicationProvider.getApplicationContext();
    private int mShortcutTypes;

    @Before
    public void setUp() {
        mContext.setTheme(androidx.appcompat.R.style.Theme_AppCompat);
        mShortcutTypes = /* initial */ 0;
    }

    @Test(expected = IllegalArgumentException.class)
    public void createTutorialPages_shortcutListIsEmpty_throwsException() {
        createAccessibilityTutorialDialog(mContext, mShortcutTypes);
    }

    @Test
    public void createTutorialPages_turnOnTripleTapShortcut_hasOnePage() {
        mShortcutTypes |= UserShortcutType.TRIPLETAP;

        final AlertDialog alertDialog =
                createAccessibilityTutorialDialog(mContext, mShortcutTypes);

        assertThat(createShortcutTutorialPages(mContext,
                mShortcutTypes)).hasSize(/* expectedSize= */ 1);
        assertThat(alertDialog).isNotNull();
    }

    @Test
    @RequiresFlagsEnabled(Flags.FLAG_ENABLE_MAGNIFICATION_MULTIPLE_FINGER_MULTIPLE_TAP_GESTURE)
    public void createTutorialPages_turnOnTwoFingerTripleTapShortcut_hasOnePage() {
        mShortcutTypes |= UserShortcutType.TWOFINGERTRIPLETAP;

        final AlertDialog alertDialog =
                createAccessibilityTutorialDialog(mContext, mShortcutTypes);

        assertThat(createShortcutTutorialPages(mContext,
                mShortcutTypes)).hasSize(/* expectedSize= */ 1);
        assertThat(alertDialog).isNotNull();
    }

    @Test
    public void createTutorialPages_turnOnSoftwareShortcut_hasOnePage() {
        mShortcutTypes |= UserShortcutType.SOFTWARE;

        final AlertDialog alertDialog =
                createAccessibilityTutorialDialog(mContext, mShortcutTypes);

        assertThat(createShortcutTutorialPages(mContext,
                mShortcutTypes)).hasSize(/* expectedSize= */ 1);
        assertThat(alertDialog).isNotNull();
    }

    @Test
    public void createTutorialPages_turnOnSoftwareAndHardwareShortcuts_hasTwoPages() {
        mShortcutTypes |= UserShortcutType.SOFTWARE;
        mShortcutTypes |= UserShortcutType.HARDWARE;

        final AlertDialog alertDialog =
                createAccessibilityTutorialDialog(mContext, mShortcutTypes);

        assertThat(createShortcutTutorialPages(mContext,
                mShortcutTypes)).hasSize(/* expectedSize= */ 2);
        assertThat(alertDialog).isNotNull();
    }

    @Test
    public void createTutorialPages_turnOnSoftwareShortcut_linkButtonVisible() {
        mShortcutTypes |= UserShortcutType.SOFTWARE;

        final AlertDialog alertDialog =
                createAccessibilityTutorialDialog(mContext, mShortcutTypes);
        alertDialog.show();

        assertThat(alertDialog.getButton(DialogInterface.BUTTON_NEGATIVE).getVisibility())
                .isEqualTo(View.VISIBLE);
    }

    @Test
    public void createTutorialPages_turnOnSoftwareAndHardwareShortcut_linkButtonVisible() {
        mShortcutTypes |= UserShortcutType.SOFTWARE;
        mShortcutTypes |= UserShortcutType.HARDWARE;

        final AlertDialog alertDialog =
                createAccessibilityTutorialDialog(mContext, mShortcutTypes);
        alertDialog.show();

        assertThat(alertDialog.getButton(DialogInterface.BUTTON_NEGATIVE).getVisibility())
                .isEqualTo(View.VISIBLE);
    }

    @Test
    public void createTutorialPages_turnOnHardwareShortcut_linkButtonGone() {
        mShortcutTypes |= UserShortcutType.HARDWARE;

        final AlertDialog alertDialog =
                createAccessibilityTutorialDialog(mContext, mShortcutTypes);
        alertDialog.show();

        assertThat(alertDialog.getButton(DialogInterface.BUTTON_NEGATIVE).getVisibility())
                .isEqualTo(View.GONE);
    }

    @Test
    public void createTutorialPages_turnOnSoftwareShortcut_showFromSuW_linkButtonGone() {
        mShortcutTypes |= UserShortcutType.SOFTWARE;

        final AlertDialog alertDialog =
                createAccessibilityTutorialDialogForSetupWizard(mContext, mShortcutTypes);
        alertDialog.show();

        assertThat(alertDialog.getButton(DialogInterface.BUTTON_NEGATIVE).getVisibility())
                .isEqualTo(View.GONE);
    }


    @Test
    public void performClickOnPositiveButton_turnOnSoftwareShortcut_dismiss() {
        mShortcutTypes |= UserShortcutType.SOFTWARE;
        final AlertDialog alertDialog =
                createAccessibilityTutorialDialog(mContext, mShortcutTypes);
        alertDialog.show();

        alertDialog.getButton(DialogInterface.BUTTON_POSITIVE).performClick();

        assertThat(alertDialog.isShowing()).isFalse();
    }

    @Test
    public void performClickOnPositiveButton_turnOnSoftwareShortcut_callOnClickListener() {
        mShortcutTypes |= UserShortcutType.SOFTWARE;
        final AlertDialog alertDialog =
                createAccessibilityTutorialDialog(mContext, mShortcutTypes, mOnClickListener);
        alertDialog.show();

        alertDialog.getButton(DialogInterface.BUTTON_POSITIVE).performClick();

        verify(mOnClickListener).onClick(alertDialog, DialogInterface.BUTTON_POSITIVE);
    }

    @Test
    public void performClickOnNegativeButton_turnOnSoftwareShortcut_directToSettingsPage() {
        mShortcutTypes |= UserShortcutType.SOFTWARE;
        Activity activity = Robolectric.buildActivity(Activity.class).create().get();
        final AlertDialog alertDialog = createAccessibilityTutorialDialog(activity, mShortcutTypes);
        alertDialog.show();

        alertDialog.getButton(DialogInterface.BUTTON_NEGATIVE).performClick();

        final Intent intent = shadowOf(activity).peekNextStartedActivity();
        assertThat(intent.getComponent().getClassName()).isEqualTo(SubSettings.class.getName());
        assertThat(intent.getStringExtra(SettingsActivity.EXTRA_SHOW_FRAGMENT))
                .isEqualTo(AccessibilityButtonFragment.class.getName());
        assertThat(intent.getIntExtra(MetricsFeatureProvider.EXTRA_SOURCE_METRICS_CATEGORY, -1))
                .isEqualTo(SettingsEnums.SWITCH_SHORTCUT_DIALOG_ACCESSIBILITY_BUTTON_SETTINGS);
    }

    @Test
    public void performClickOnPositiveButton_turnOnGestureShortcut_callOnDismissListener() {
        final AlertDialog alertDialog =
                showGestureNavigationTutorialDialog(mContext, mOnDismissListener);

        alertDialog.getButton(DialogInterface.BUTTON_POSITIVE).performClick();

        assertThat(alertDialog.isShowing()).isFalse();
        verify(mOnDismissListener).onDismiss(alertDialog);
    }
}
