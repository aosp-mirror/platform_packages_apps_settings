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

import static com.android.internal.accessibility.common.ShortcutConstants.UserShortcutType.HARDWARE;
import static com.android.internal.accessibility.common.ShortcutConstants.UserShortcutType.QUICK_SETTINGS;
import static com.android.internal.accessibility.common.ShortcutConstants.UserShortcutType.SOFTWARE;
import static com.android.internal.accessibility.common.ShortcutConstants.UserShortcutType.TRIPLETAP;
import static com.android.internal.accessibility.common.ShortcutConstants.UserShortcutType.TWOFINGER_DOUBLETAP;
import static com.android.settings.accessibility.AccessibilityShortcutsTutorial.createAccessibilityTutorialDialog;
import static com.android.settings.accessibility.AccessibilityShortcutsTutorial.createAccessibilityTutorialDialogForSetupWizard;
import static com.android.settings.accessibility.AccessibilityShortcutsTutorial.createShortcutTutorialPages;
import static com.android.settings.accessibility.AccessibilityShortcutsTutorial.showGestureNavigationTutorialDialog;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Mockito.verify;
import static org.robolectric.Shadows.shadowOf;

import android.app.Activity;
import android.app.settings.SettingsEnums;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.platform.test.annotations.EnableFlags;
import android.platform.test.flag.junit.SetFlagsRule;
import android.text.SpannableStringBuilder;
import android.util.ArrayMap;
import android.view.View;
import android.view.accessibility.AccessibilityManager;
import android.widget.Button;
import android.widget.TextSwitcher;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;
import androidx.test.core.app.ApplicationProvider;

import com.android.server.accessibility.Flags;
import com.android.settings.R;
import com.android.settings.SettingsActivity;
import com.android.settings.SubSettings;
import com.android.settings.testutils.AccessibilityTestUtils;
import com.android.settings.testutils.shadow.SettingsShadowResources;
import com.android.settingslib.core.instrumentation.MetricsFeatureProvider;
import com.android.settingslib.utils.StringUtil;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowAccessibilityManager;
import org.robolectric.shadows.ShadowLooper;

import java.util.Map;

/** Tests for {@link AccessibilityShortcutsTutorial}. */
@Config(shadows = SettingsShadowResources.class)
@RunWith(RobolectricTestRunner.class)
public final class AccessibilityShortcutsTutorialTest {
    private static final String FAKE_FEATURE_NAME = "Fake Feature Name";

    @Rule
    public final MockitoRule mMockitoRule = MockitoJUnit.rule();
    @Rule
    public final SetFlagsRule mSetFlagsRule = new SetFlagsRule();
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
        createAccessibilityTutorialDialog(mContext, mShortcutTypes, FAKE_FEATURE_NAME);
    }

    @Test
    public void createTutorialPages_turnOnTripleTapShortcut_hasOnePage() {
        mShortcutTypes |= TRIPLETAP;

        final AlertDialog alertDialog =
                createAccessibilityTutorialDialog(mContext, mShortcutTypes, FAKE_FEATURE_NAME);

        assertThat(
                createShortcutTutorialPages(
                        mContext, mShortcutTypes, FAKE_FEATURE_NAME, /* inSetupWizard= */ false)
        ).hasSize(/* expectedSize= */ 1);
        assertThat(alertDialog).isNotNull();
    }

    @Test
    @EnableFlags(Flags.FLAG_ENABLE_MAGNIFICATION_MULTIPLE_FINGER_MULTIPLE_TAP_GESTURE)
    public void createTutorialPages_turnOnTwoFingerTripleTapShortcut_hasOnePage() {
        mShortcutTypes |= TWOFINGER_DOUBLETAP;

        final AlertDialog alertDialog =
                createAccessibilityTutorialDialog(mContext, mShortcutTypes, FAKE_FEATURE_NAME);

        assertThat(
                createShortcutTutorialPages(
                        mContext, mShortcutTypes, FAKE_FEATURE_NAME, /* inSetupWizard= */ false)
        ).hasSize(/* expectedSize= */ 1);
        assertThat(alertDialog).isNotNull();
    }

    @Test
    @EnableFlags(android.view.accessibility.Flags.FLAG_A11Y_QS_SHORTCUT)
    public void createTutorialPages_turnOnQuickSettingShortcut_hasOnePage() {
        mShortcutTypes |= QUICK_SETTINGS;

        final AlertDialog alertDialog =
                createAccessibilityTutorialDialog(mContext, mShortcutTypes, FAKE_FEATURE_NAME);

        assertThat(
                createShortcutTutorialPages(
                        mContext, mShortcutTypes, FAKE_FEATURE_NAME, /* inSetupWizard= */ false)
        ).hasSize(/* expectedSize= */ 1);
        assertThat(alertDialog).isNotNull();
    }

    @Test
    public void createTutorialPages_turnOnSoftwareShortcut_hasOnePage() {
        mShortcutTypes |= SOFTWARE;

        final AlertDialog alertDialog =
                createAccessibilityTutorialDialog(mContext, mShortcutTypes, FAKE_FEATURE_NAME);

        assertThat(
                createShortcutTutorialPages(
                        mContext, mShortcutTypes, FAKE_FEATURE_NAME, /* inSetupWizard= */ false)
        ).hasSize(/* expectedSize= */ 1);
        assertThat(alertDialog).isNotNull();
    }

    @Test
    public void createTutorialPages_turnOnSoftwareAndHardwareShortcuts_hasTwoPages() {
        mShortcutTypes |= SOFTWARE;
        mShortcutTypes |= HARDWARE;

        final AlertDialog alertDialog =
                createAccessibilityTutorialDialog(mContext, mShortcutTypes, FAKE_FEATURE_NAME);

        assertThat(
                createShortcutTutorialPages(
                        mContext, mShortcutTypes, FAKE_FEATURE_NAME, /* inSetupWizard= */ false)
        ).hasSize(/* expectedSize= */ 2);
        assertThat(alertDialog).isNotNull();
    }

    @Test
    public void createTutorialPages_turnOnA11yGestureShortcut_linkButtonShownWithText() {
        mShortcutTypes |= SOFTWARE;
        AccessibilityTestUtils.setSoftwareShortcutMode(
                mContext, /* gestureNavEnabled= */ true, /* floatingButtonEnabled= */ false);

        final AlertDialog alertDialog =
                createAccessibilityTutorialDialog(mContext, mShortcutTypes, FAKE_FEATURE_NAME);
        alertDialog.show();
        ShadowLooper.idleMainLooper();

        Button btn = alertDialog.getButton(DialogInterface.BUTTON_NEGATIVE);
        assertThat(btn).isNotNull();
        assertThat(btn.getVisibility()).isEqualTo(View.VISIBLE);
        assertThat(btn.getText().toString()).isEqualTo(
                mContext.getString(
                        R.string.accessibility_tutorial_dialog_configure_software_shortcut_type));
    }

    @Test
    public void createTutorialPages_turnOnA11yNavButtonShortcut_linkButtonShownWithText() {
        mShortcutTypes |= SOFTWARE;
        AccessibilityTestUtils.setSoftwareShortcutMode(
                mContext, /* gestureNavEnabled= */ false, /* floatingButtonEnabled= */ false);

        final AlertDialog alertDialog =
                createAccessibilityTutorialDialog(mContext, mShortcutTypes, FAKE_FEATURE_NAME);
        alertDialog.show();
        ShadowLooper.idleMainLooper();

        Button btn = alertDialog.getButton(DialogInterface.BUTTON_NEGATIVE);
        assertThat(btn).isNotNull();
        assertThat(btn.getVisibility()).isEqualTo(View.VISIBLE);
        assertThat(btn.getText().toString()).isEqualTo(
                mContext.getString(
                        R.string.accessibility_tutorial_dialog_configure_software_shortcut_type));
    }

    @Test
    public void createTutorialPages_turnOnFloatingButtonShortcut_linkButtonShownWithText() {
        mShortcutTypes |= SOFTWARE;
        AccessibilityTestUtils.setSoftwareShortcutMode(
                mContext, /* gestureNavEnabled= */ false, /* floatingButtonEnabled= */ true);

        final AlertDialog alertDialog =
                createAccessibilityTutorialDialog(mContext, mShortcutTypes, FAKE_FEATURE_NAME);
        alertDialog.show();
        ShadowLooper.idleMainLooper();

        Button btn = alertDialog.getButton(DialogInterface.BUTTON_NEGATIVE);
        assertThat(btn).isNotNull();
        assertThat(btn.getVisibility()).isEqualTo(View.VISIBLE);
        assertThat(btn.getText().toString()).isEqualTo(
                mContext.getString(R.string.accessibility_tutorial_dialog_link_button));
    }

    @Test
    public void createTutorialPages_turnOnHardwareShortcut_linkButtonGone() {
        mShortcutTypes |= HARDWARE;

        final AlertDialog alertDialog =
                createAccessibilityTutorialDialog(mContext, mShortcutTypes, FAKE_FEATURE_NAME);
        alertDialog.show();
        ShadowLooper.idleMainLooper();

        assertThat(alertDialog.getButton(DialogInterface.BUTTON_NEGATIVE).getVisibility())
                .isEqualTo(View.GONE);
    }

    @Test
    public void createTutorialPages_turnOnSoftwareShortcut_showFromSuW_linkButtonGone() {
        mShortcutTypes |= SOFTWARE;

        final AlertDialog alertDialog =
                createAccessibilityTutorialDialogForSetupWizard(
                        mContext, mShortcutTypes, FAKE_FEATURE_NAME);
        alertDialog.show();
        ShadowLooper.idleMainLooper();

        assertThat(alertDialog.getButton(DialogInterface.BUTTON_NEGATIVE).getVisibility())
                .isEqualTo(View.GONE);
    }

    @Test
    @EnableFlags(android.view.accessibility.Flags.FLAG_A11Y_QS_SHORTCUT)
    public void createAccessibilityTutorialDialog_qsShortcut_inSuwTalkbackOn_verifyText() {
        mShortcutTypes |= QUICK_SETTINGS;
        setTouchExplorationEnabled(true);
        final String expectedTitle = mContext.getString(
                R.string.accessibility_tutorial_dialog_title_quick_setting);
        Map<String, Object> arguments = new ArrayMap<>();
        arguments.put("count", 2);
        arguments.put("featureName", FAKE_FEATURE_NAME);
        final CharSequence instruction = StringUtil.getIcuPluralsString(mContext,
                arguments,
                R.string.accessibility_tutorial_dialog_message_quick_setting);
        final SpannableStringBuilder expectedInstruction = new SpannableStringBuilder();
        expectedInstruction
                .append(mContext.getText(
                        R.string.accessibility_tutorial_dialog_shortcut_unavailable_in_suw))
                .append("\n\n");
        expectedInstruction.append(instruction);

        final AlertDialog alertDialog =
                createAccessibilityTutorialDialogForSetupWizard(
                        mContext, mShortcutTypes, FAKE_FEATURE_NAME);
        alertDialog.show();
        ShadowLooper.idleMainLooper();

        verifyTutorialTitleAndInstruction(
                alertDialog,
                expectedTitle,
                expectedInstruction.toString());
    }

    @Test
    @EnableFlags(android.view.accessibility.Flags.FLAG_A11Y_QS_SHORTCUT)
    public void createAccessibilityTutorialDialog_qsShortcut_notInSuwTalkbackOn_verifyText() {
        mShortcutTypes |= QUICK_SETTINGS;
        setTouchExplorationEnabled(true);
        final String expectedTitle = mContext.getString(
                R.string.accessibility_tutorial_dialog_title_quick_setting);
        Map<String, Object> arguments = new ArrayMap<>();
        arguments.put("count", 2);
        arguments.put("featureName", FAKE_FEATURE_NAME);
        final CharSequence expectedInstruction = StringUtil.getIcuPluralsString(mContext,
                arguments,
                R.string.accessibility_tutorial_dialog_message_quick_setting);

        final AlertDialog alertDialog =
                createAccessibilityTutorialDialog(
                        mContext, mShortcutTypes, FAKE_FEATURE_NAME);
        alertDialog.show();
        ShadowLooper.idleMainLooper();

        verifyTutorialTitleAndInstruction(
                alertDialog,
                expectedTitle,
                expectedInstruction.toString());
    }

    @Test
    @EnableFlags(android.view.accessibility.Flags.FLAG_A11Y_QS_SHORTCUT)
    public void createAccessibilityTutorialDialog_qsShortcut_inSuwTalkbackOff_verifyText() {
        mShortcutTypes |= QUICK_SETTINGS;
        setTouchExplorationEnabled(false);
        final String expectedTitle = mContext.getString(
                R.string.accessibility_tutorial_dialog_title_quick_setting);
        Map<String, Object> arguments = new ArrayMap<>();
        arguments.put("count", 1);
        arguments.put("featureName", FAKE_FEATURE_NAME);
        final CharSequence instruction = StringUtil.getIcuPluralsString(mContext,
                arguments,
                R.string.accessibility_tutorial_dialog_message_quick_setting);
        final SpannableStringBuilder expectedInstruction = new SpannableStringBuilder();
        expectedInstruction.append(mContext.getText(
                        R.string.accessibility_tutorial_dialog_shortcut_unavailable_in_suw))
                .append("\n\n");
        expectedInstruction.append(instruction);

        final AlertDialog alertDialog =
                createAccessibilityTutorialDialogForSetupWizard(
                        mContext, mShortcutTypes, FAKE_FEATURE_NAME);
        alertDialog.show();
        ShadowLooper.idleMainLooper();

        verifyTutorialTitleAndInstruction(
                alertDialog,
                expectedTitle,
                expectedInstruction.toString());
    }

    @Test
    @EnableFlags(android.view.accessibility.Flags.FLAG_A11Y_QS_SHORTCUT)
    public void createAccessibilityTutorialDialog_qsShortcut_notInSuwTalkbackOff_verifyText() {
        mShortcutTypes |= QUICK_SETTINGS;
        setTouchExplorationEnabled(false);
        final String expectedTitle = mContext.getString(
                R.string.accessibility_tutorial_dialog_title_quick_setting);
        Map<String, Object> arguments = new ArrayMap<>();
        arguments.put("count", 1);
        arguments.put("featureName", FAKE_FEATURE_NAME);
        final CharSequence expectedInstruction = StringUtil.getIcuPluralsString(mContext,
                arguments,
                R.string.accessibility_tutorial_dialog_message_quick_setting);

        final AlertDialog alertDialog =
                createAccessibilityTutorialDialog(
                        mContext, mShortcutTypes, FAKE_FEATURE_NAME);
        alertDialog.show();
        ShadowLooper.idleMainLooper();

        verifyTutorialTitleAndInstruction(
                alertDialog,
                expectedTitle,
                expectedInstruction.toString());
    }

    @Test
    public void createAccessibilityTutorialDialog_volumeKeysShortcut_verifyText() {
        mShortcutTypes |= HARDWARE;
        final String expectedTitle = mContext.getString(
                R.string.accessibility_tutorial_dialog_title_volume);
        final CharSequence expectedInstruction = mContext.getString(
                R.string.accessibility_tutorial_dialog_message_volume);

        final AlertDialog alertDialog =
                createAccessibilityTutorialDialog(
                        mContext, mShortcutTypes, FAKE_FEATURE_NAME);
        alertDialog.show();
        ShadowLooper.idleMainLooper();

        verifyTutorialTitleAndInstruction(
                alertDialog,
                expectedTitle,
                expectedInstruction.toString());
    }

    @Test
    public void createAccessibilityTutorialDialog_tripleTapShortcut_verifyText() {
        mShortcutTypes |= TRIPLETAP;
        final String expectedTitle = mContext.getString(
                R.string.accessibility_tutorial_dialog_title_triple);
        final CharSequence expectedInstruction = mContext.getString(
                R.string.accessibility_tutorial_dialog_tripletap_instruction, 3);

        final AlertDialog alertDialog =
                createAccessibilityTutorialDialog(
                        mContext, mShortcutTypes, FAKE_FEATURE_NAME);
        alertDialog.show();
        ShadowLooper.idleMainLooper();

        verifyTutorialTitleAndInstruction(
                alertDialog,
                expectedTitle,
                expectedInstruction.toString());
    }

    @Test
    @EnableFlags(Flags.FLAG_ENABLE_MAGNIFICATION_MULTIPLE_FINGER_MULTIPLE_TAP_GESTURE)
    public void createAccessibilityTutorialDialog_twoFingerDoubleTapShortcut_verifyText() {
        mShortcutTypes |= TWOFINGER_DOUBLETAP;
        final int numFingers = 2;
        final String expectedTitle = mContext.getString(
                R.string.accessibility_tutorial_dialog_title_two_finger_double, numFingers);
        final String expectedInstruction = mContext.getString(
                R.string.accessibility_tutorial_dialog_twofinger_doubletap_instruction, numFingers);

        final AlertDialog alertDialog =
                createAccessibilityTutorialDialog(
                        mContext, mShortcutTypes, FAKE_FEATURE_NAME);
        alertDialog.show();
        ShadowLooper.idleMainLooper();

        verifyTutorialTitleAndInstruction(
                alertDialog,
                expectedTitle,
                expectedInstruction);
    }

    @Test
    public void createAccessibilityTutorialDialog_floatingButtonShortcut_verifyText() {
        mShortcutTypes |= SOFTWARE;
        AccessibilityTestUtils.setSoftwareShortcutMode(
                mContext, /* gestureNavEnabled= */ false, /* floatingButtonEnabled= */ true);
        final String expectedTitle = mContext.getString(
                R.string.accessibility_tutorial_dialog_title_button);
        final String expectedInstruction = mContext.getString(
                R.string.accessibility_tutorial_dialog_message_floating_button);

        final AlertDialog alertDialog =
                createAccessibilityTutorialDialog(
                        mContext, mShortcutTypes, FAKE_FEATURE_NAME);
        alertDialog.show();
        ShadowLooper.idleMainLooper();

        verifyTutorialTitleAndInstruction(
                alertDialog,
                expectedTitle,
                expectedInstruction);
    }

    @Test
    public void createAccessibilityTutorialDialog_navA11yButtonShortcut_verifyText() {
        mShortcutTypes |= SOFTWARE;
        AccessibilityTestUtils.setSoftwareShortcutMode(
                mContext, /* gestureNavEnabled= */ false, /* floatingButtonEnabled= */ false);
        final String expectedTitle = mContext.getString(
                R.string.accessibility_tutorial_dialog_title_button);
        final String expectedInstruction = mContext.getString(
                R.string.accessibility_tutorial_dialog_message_button);

        final AlertDialog alertDialog =
                createAccessibilityTutorialDialog(
                        mContext, mShortcutTypes, FAKE_FEATURE_NAME);
        alertDialog.show();
        ShadowLooper.idleMainLooper();

        verifyTutorialTitleAndInstruction(
                alertDialog,
                expectedTitle,
                expectedInstruction);
    }

    @Test
    public void createAccessibilityTutorialDialog_gestureShortcut_talkbackOn_verifyText() {
        mShortcutTypes |= SOFTWARE;
        setTouchExplorationEnabled(true);
        AccessibilityTestUtils.setSoftwareShortcutMode(
                mContext, /* gestureNavEnabled= */ true, /* floatingButtonEnabled= */ false);
        final String expectedTitle = mContext.getString(
                R.string.accessibility_tutorial_dialog_title_gesture);
        final String expectedInstruction = StringUtil.getIcuPluralsString(
                mContext,
                /* count= */ 3,
                R.string.accessibility_tutorial_dialog_gesture_shortcut_instruction);

        final AlertDialog alertDialog =
                createAccessibilityTutorialDialog(
                        mContext, mShortcutTypes, FAKE_FEATURE_NAME);
        alertDialog.show();
        ShadowLooper.idleMainLooper();

        verifyTutorialTitleAndInstruction(
                alertDialog,
                expectedTitle,
                expectedInstruction);
    }

    @Test
    public void createAccessibilityTutorialDialog_gestureShortcut_talkbackOff_verifyText() {
        mShortcutTypes |= SOFTWARE;
        setTouchExplorationEnabled(false);
        AccessibilityTestUtils.setSoftwareShortcutMode(
                mContext, /* gestureNavEnabled= */ true, /* floatingButtonEnabled= */ false);
        final String expectedTitle = mContext.getString(
                R.string.accessibility_tutorial_dialog_title_gesture);
        final String expectedInstruction = StringUtil.getIcuPluralsString(
                mContext,
                /* count= */ 2,
                R.string.accessibility_tutorial_dialog_gesture_shortcut_instruction);

        final AlertDialog alertDialog =
                createAccessibilityTutorialDialog(
                        mContext, mShortcutTypes, FAKE_FEATURE_NAME);
        alertDialog.show();
        ShadowLooper.idleMainLooper();

        verifyTutorialTitleAndInstruction(
                alertDialog,
                expectedTitle,
                expectedInstruction);
    }

    @Test
    public void performClickOnPositiveButton_turnOnSoftwareShortcut_dismiss() {
        mShortcutTypes |= SOFTWARE;
        final AlertDialog alertDialog =
                createAccessibilityTutorialDialog(mContext, mShortcutTypes, FAKE_FEATURE_NAME);
        alertDialog.show();
        ShadowLooper.idleMainLooper();

        alertDialog.getButton(DialogInterface.BUTTON_POSITIVE).performClick();
        ShadowLooper.idleMainLooper();

        assertThat(alertDialog.isShowing()).isFalse();
    }

    @Test
    public void performClickOnPositiveButton_turnOnSoftwareShortcut_callOnClickListener() {
        mShortcutTypes |= SOFTWARE;
        final AlertDialog alertDialog =
                createAccessibilityTutorialDialog(
                        mContext, mShortcutTypes, mOnClickListener, FAKE_FEATURE_NAME);
        alertDialog.show();
        ShadowLooper.idleMainLooper();

        alertDialog.getButton(DialogInterface.BUTTON_POSITIVE).performClick();
        ShadowLooper.idleMainLooper();

        verify(mOnClickListener).onClick(alertDialog, DialogInterface.BUTTON_POSITIVE);
    }

    @Test
    public void performClickOnNegativeButton_turnOnSoftwareShortcut_directToSettingsPage() {
        mShortcutTypes |= SOFTWARE;
        Activity activity = Robolectric.buildActivity(Activity.class).create().get();
        final AlertDialog alertDialog =
                createAccessibilityTutorialDialog(activity, mShortcutTypes, FAKE_FEATURE_NAME);
        alertDialog.show();

        alertDialog.getButton(DialogInterface.BUTTON_NEGATIVE).performClick();
        ShadowLooper.idleMainLooper();

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
        ShadowLooper.idleMainLooper();

        assertThat(alertDialog.isShowing()).isFalse();
        verify(mOnDismissListener).onDismiss(alertDialog);
    }

    private void setTouchExplorationEnabled(boolean enable) {
        ShadowAccessibilityManager am = shadowOf(
                mContext.getSystemService(AccessibilityManager.class));
        am.setTouchExplorationEnabled(enable);
    }

    private void verifyTutorialTitleAndInstruction(AlertDialog alertDialog, String expectedTitle,
            String expectedInstruction) {
        TextSwitcher titleView = alertDialog.findViewById(R.id.title);
        assertThat(titleView).isNotNull();
        assertThat(((TextView) titleView.getCurrentView()).getText().toString()).isEqualTo(
                expectedTitle);
        TextSwitcher instructionView = alertDialog.findViewById(R.id.instruction);
        assertThat(instructionView).isNotNull();
        assertThat(((TextView) instructionView.getCurrentView()).getText().toString()).isEqualTo(
                expectedInstruction);
    }
}
