/*
 * Copyright (C) 2017 The Android Open Source Project
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
 * limitations under the License
 */

package com.android.settings.password;

import static com.google.common.truth.Truth.assertThat;
import static com.google.common.truth.Truth.assertWithMessage;

import static org.robolectric.RuntimeEnvironment.application;

import android.content.ComponentName;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.os.UserHandle;
import android.util.TypedValue;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.FragmentActivity;

import com.android.internal.widget.LockPatternUtils;
import com.android.internal.widget.LockPatternView;
import com.android.internal.widget.LockPatternView.Cell;
import com.android.internal.widget.LockPatternView.DisplayMode;
import com.android.settings.R;
import com.android.settings.SetupRedactionInterstitial;
import com.android.settings.password.ChooseLockPattern.ChooseLockPatternFragment;
import com.android.settings.password.ChooseLockPattern.IntentBuilder;
import com.android.settings.testutils.shadow.ShadowAlertDialogCompat;
import com.android.settings.testutils.shadow.ShadowLockPatternUtils;
import com.android.settings.testutils.shadow.ShadowUtils;

import com.google.android.setupcompat.PartnerCustomizationLayout;
import com.google.android.setupcompat.template.FooterBarMixin;
import com.google.android.setupcompat.template.FooterButton;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.Shadows;
import org.robolectric.android.controller.ActivityController;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.LooperMode;
import org.robolectric.shadows.ShadowPackageManager;
import org.robolectric.util.ReflectionHelpers;
import org.robolectric.util.ReflectionHelpers.ClassParameter;

import java.util.Arrays;

@RunWith(RobolectricTestRunner.class)
@LooperMode(LooperMode.Mode.LEGACY)
@Config(shadows = {ShadowUtils.class, ShadowAlertDialogCompat.class, ShadowLockPatternUtils.class})
public class SetupChooseLockPatternTest {

    private SetupChooseLockPattern mActivity;

    @Before
    public void setUp() {
        application.getPackageManager().setComponentEnabledSetting(
                new ComponentName(application, SetupRedactionInterstitial.class),
                PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                PackageManager.DONT_KILL_APP);

        final Intent intent =
                SetupChooseLockPattern.modifyIntentForSetup(
                        application,
                        new IntentBuilder(application)
                                .setUserId(UserHandle.myUserId())
                                .build());
        mActivity = ActivityController.of(new SetupChooseLockPattern(), intent).setup().get();
    }

    @Test
    public void chooseLockSaved_shouldEnableRedactionInterstitial() {
        findFragment(mActivity).onChosenLockSaveFinished(false, null);

        ShadowPackageManager spm = Shadows.shadowOf(application.getPackageManager());
        ComponentName cname = new ComponentName(application, SetupRedactionInterstitial.class);
        final int componentEnabled = spm.getComponentEnabledSettingFlags(cname)
                & PackageManager.COMPONENT_ENABLED_STATE_ENABLED;
        assertThat(componentEnabled).isEqualTo(PackageManager.COMPONENT_ENABLED_STATE_ENABLED);
    }

    @Test
    @Ignore("b/295325503")
    public void optionsButton_whenPatternSelected_shouldBeVisible() {
        final Button button = mActivity.findViewById(R.id.screen_lock_options);
        assertThat(button).isNotNull();
        assertThat(button.getVisibility()).isEqualTo(View.VISIBLE);

        final LockPatternView lockPatternView = mActivity.findViewById(R.id.lockPattern);
        ReflectionHelpers.callInstanceMethod(lockPatternView, "notifyPatternDetected");

        enterPattern();
        assertThat(button.getVisibility()).isEqualTo(View.VISIBLE);
    }

    private void verifyScreenLockOptionsShown() {
        final Button button = mActivity.findViewById(R.id.screen_lock_options);
        assertThat(button).isNotNull();
        assertThat(button.getVisibility()).isEqualTo(View.VISIBLE);

        button.performClick();
        final AlertDialog chooserDialog = ShadowAlertDialogCompat.getLatestAlertDialog();
        assertThat(chooserDialog).isNotNull();
        int count = chooserDialog.getListView().getCount();
        assertWithMessage("List items shown").that(count).isEqualTo(3);
    }

    @Config(qualifiers = "sw400dp")
    @Test
    @Ignore("b/295325503")
    public void sw400dp_shouldShowScreenLockOptions() {
        verifyScreenLockOptionsShown();
    }

    @Config(qualifiers = "sw400dp-land")
    @Test
    @Ignore("b/295325503")
    public void sw400dpLandscape_shouldShowScreenLockOptions() {
        verifyScreenLockOptionsShown();
    }

    private void verifyScreenLockOptionsHidden() {
        Button button = mActivity.findViewById(R.id.screen_lock_options);
        assertThat(button).isNotNull();
        assertThat(button.getVisibility()).isEqualTo(View.GONE);
    }

    @Config(qualifiers = "sw300dp")
    @Test
    @Ignore("b/295325503")
    public void smallScreens_shouldHideScreenLockOptions() {
        verifyScreenLockOptionsHidden();
    }

    @Config(qualifiers = "sw300dp-land")
    @Test
    @Ignore("b/295325503")
    public void smallScreensLandscape_shouldHideScreenLockOptions() {
        verifyScreenLockOptionsHidden();
    }

    @Test
    @Ignore("b/295325503")
    public void skipButton_shouldBeVisible_duringNonFingerprintFlow() {
        final PartnerCustomizationLayout layout = mActivity.findViewById(R.id.setup_wizard_layout);
        final Button skipOrClearButton =
                layout.getMixin(FooterBarMixin.class).getSecondaryButtonView();

        assertThat(skipOrClearButton).isNotNull();
        assertThat(skipOrClearButton.getVisibility()).isEqualTo(View.VISIBLE);

        skipOrClearButton.performClick();
        AlertDialog chooserDialog = ShadowAlertDialogCompat.getLatestAlertDialog();
        assertThat(chooserDialog).isNotNull();
    }

    @Test
    public void clearButton_shouldBeVisible_duringRetryStage() {
        enterPattern();

        final PartnerCustomizationLayout layout = mActivity.findViewById(R.id.setup_wizard_layout);
        final Button skipOrClearButton =
                layout.getMixin(FooterBarMixin.class).getSecondaryButtonView();

        assertThat(skipOrClearButton.getVisibility()).isEqualTo(View.VISIBLE);
        assertThat(skipOrClearButton.isEnabled()).isTrue();

        skipOrClearButton.performClick();

        assertThat(findFragment(mActivity).mChosenPattern).isNull();
    }

    @Test
    public void createActivity_enterPattern_clearButtonShouldBeShown() {
        final PartnerCustomizationLayout layout = mActivity.findViewById(R.id.setup_wizard_layout);
        final Button skipOrClearButton =
                layout.getMixin(FooterBarMixin.class).getSecondaryButtonView();

        assertThat(skipOrClearButton.isEnabled()).isTrue();
        assertThat(skipOrClearButton.getVisibility()).isEqualTo(View.VISIBLE);
        assertThat(skipOrClearButton.getText())
                .isEqualTo(application.getString(R.string.skip_label));

        enterPattern();

        assertThat(skipOrClearButton.isEnabled()).isTrue();
        assertThat(skipOrClearButton.getVisibility()).isEqualTo(View.VISIBLE);
        assertThat(skipOrClearButton.getText())
                .isEqualTo(application.getString(R.string.lockpattern_retry_button_text));
    }

    @Test
    public void createActivity_patternDescription_shouldBeShown() {
        final PartnerCustomizationLayout layout = mActivity.findViewById(R.id.setup_wizard_layout);
        final TextView patternDescription =
                layout.findViewById(R.id.sud_layout_subtitle);

        assertThat(patternDescription.getVisibility()).isEqualTo(View.VISIBLE);
        assertThat(patternDescription.getText()).isEqualTo(
                application.getString(R.string.lockpassword_choose_your_pattern_description));
    }

    @Test
    public void createActivity_patternTitle_shouldShowGenericText() {
        final CharSequence headerView = mActivity.getTitle();

        assertThat(headerView).isEqualTo(
                application.getString(R.string.lockpassword_choose_your_pattern_header));
    }

    @Test
    public void inIntroductionStage_theHeaderHeight_shouldSetMinLinesTwoToPreventFlicker() {
        final PartnerCustomizationLayout layout = mActivity.findViewById(R.id.setup_wizard_layout);
        final TextView headerView = layout.findViewById(R.id.sud_layout_subtitle);

        assertThat(headerView.getVisibility()).isEqualTo(View.VISIBLE);
        assertThat(headerView.getText().toString()).isEqualTo(
                application.getString(R.string.lockpassword_choose_your_pattern_description));
    }

    @Test
    public void createActivity_enterPattern_shouldGoToFirstChoiceValidStage() {
        final PartnerCustomizationLayout layout = mActivity.findViewById(R.id.setup_wizard_layout);
        final TextView headerView = layout.findViewById(R.id.sud_layout_subtitle);

        assertThat(headerView.getVisibility()).isEqualTo(View.VISIBLE);

        enterPattern();

        assertThat(headerView.getText().toString()).isEqualTo(
                application.getString(R.string.lockpattern_pattern_entered_header));
    }

    @Test
    public void createActivity_enterShortPattern_shouldGoToChoiceTooShortStage() {
        final PartnerCustomizationLayout layout = mActivity.findViewById(R.id.setup_wizard_layout);
        final TextView headerView = layout.findViewById(R.id.sud_layout_subtitle);

        enterShortPattern();

        assertThat(headerView.getVisibility()).isEqualTo(View.VISIBLE);
        assertThat(headerView.getText().toString()).isEqualTo(
                application.getResources().getString(
                        R.string.lockpattern_recording_incorrect_too_short,
                        LockPatternUtils.MIN_LOCK_PATTERN_SIZE));
    }

    @Test
    public void inChoiceTooShortStage_theHeaderColor_shouldTintOnErrorColor() {
        final PartnerCustomizationLayout layout = mActivity.findViewById(R.id.setup_wizard_layout);
        final TextView headerView = layout.findViewById(R.id.sud_layout_subtitle);
        final TypedValue typedValue = new TypedValue();
        final Resources.Theme theme = mActivity.getTheme();
        theme.resolveAttribute(androidx.appcompat.R.attr.colorError, typedValue, true);
        final int errorColor = typedValue.data;

        enterShortPattern();

        assertThat(headerView.getVisibility()).isEqualTo(View.VISIBLE);
        assertThat(headerView.getTextColors().getDefaultColor()).isEqualTo(errorColor);
    }

    @Test
    public void inFirstChoiceValidStage_nextButtonState_shouldEnabled() {
        final PartnerCustomizationLayout layout = mActivity.findViewById(R.id.setup_wizard_layout);
        final FooterBarMixin footerBarMixin = layout.getMixin(FooterBarMixin.class);
        final FooterButton nextButton = footerBarMixin.getPrimaryButton();

        assertThat(nextButton.getVisibility()).isEqualTo(View.VISIBLE);
        assertThat(nextButton.isEnabled()).isFalse();

        enterPattern();

        assertThat(nextButton.isEnabled()).isTrue();
    }

    @Test
    public void inFirstChoiceValidStage_clickNextButton_shouldGoToNeedToConfirmStage() {
        final PartnerCustomizationLayout layout = mActivity.findViewById(R.id.setup_wizard_layout);
        final TextView headerView = layout.findViewById(R.id.sud_layout_subtitle);
        final FooterBarMixin footerBarMixin = layout.getMixin(FooterBarMixin.class);
        final Button nextButton = footerBarMixin.getPrimaryButtonView();

        assertThat(headerView.getVisibility()).isEqualTo(View.VISIBLE);

        enterPattern();
        nextButton.performClick();

        assertThat(headerView.getText().toString()).isEqualTo(
                application.getString(R.string.lockpattern_need_to_confirm));
    }

    @Test
    public void inNeedToConfirmStage_enterWrongPattern_shouldGoToConfirmWrongStage() {
        final PartnerCustomizationLayout layout = mActivity.findViewById(R.id.setup_wizard_layout);
        final TextView headerView = layout.findViewById(R.id.sud_layout_subtitle);
        final FooterBarMixin footerBarMixin = layout.getMixin(FooterBarMixin.class);
        final Button nextButton = footerBarMixin.getPrimaryButtonView();
        // IntroductionStage
        assertThat(headerView.getVisibility()).isEqualTo(View.VISIBLE);

        enterPattern();
        nextButton.performClick();

        // NeedToConfirmStage
        assertThat(headerView.getText().toString()).isEqualTo(
                application.getString(R.string.lockpattern_need_to_confirm));

        enterShortPattern();

        // ConfirmWrongStage
        assertThat(headerView.getText().toString()).isEqualTo(
                application.getString(R.string.lockpattern_need_to_unlock_wrong));
        assertThat(nextButton.getText().toString()).isEqualTo(
                application.getString(R.string.lockpattern_confirm_button_text));
        assertThat(nextButton.isEnabled()).isFalse();
    }

    @Test
    public void inNeedToConfirmStage_enterCorrectPattern_shouldGoToChoiceConfirmedStage() {
        final PartnerCustomizationLayout layout = mActivity.findViewById(R.id.setup_wizard_layout);
        final TextView headerView = layout.findViewById(R.id.sud_layout_subtitle);
        final FooterBarMixin footerBarMixin = layout.getMixin(FooterBarMixin.class);
        final Button nextButton = footerBarMixin.getPrimaryButtonView();
        // IntroductionStage
        assertThat(headerView.getVisibility()).isEqualTo(View.VISIBLE);

        enterPattern();
        nextButton.performClick();

        // NeedToConfirmStage
        assertThat(headerView.getText().toString()).isEqualTo(
                application.getString(R.string.lockpattern_need_to_confirm));

        enterPattern();

        // ChoiceConfirmedStage
        assertThat(headerView.getText().toString()).isEqualTo(
                application.getString(R.string.lockpattern_pattern_confirmed_header));
        assertThat(nextButton.getText().toString()).isEqualTo(
                application.getString(R.string.lockpattern_confirm_button_text));
        assertThat(nextButton.isEnabled()).isTrue();
    }

    private ChooseLockPatternFragment findFragment(FragmentActivity activity) {
        return (ChooseLockPatternFragment)
                activity.getSupportFragmentManager().findFragmentById(R.id.main_content);
    }

    private void enterPattern() {
        LockPatternView lockPatternView = mActivity.findViewById(R.id.lockPattern);
        lockPatternView.setPattern(
                DisplayMode.Animate,
                Arrays.asList(
                        createCell(0, 0),
                        createCell(0, 1),
                        createCell(1, 1),
                        createCell(1, 0)));
        ReflectionHelpers.callInstanceMethod(lockPatternView, "notifyPatternDetected");
    }

    private void enterShortPattern() {
        LockPatternView lockPatternView = mActivity.findViewById(R.id.lockPattern);
        lockPatternView.setPattern(
                DisplayMode.Animate,
                Arrays.asList(
                        createCell(0, 0),
                        createCell(0, 1),
                        createCell(1, 1)));
        ReflectionHelpers.callInstanceMethod(lockPatternView, "notifyPatternDetected");
    }

    private Cell createCell(int row, int column) {
        return ReflectionHelpers.callConstructor(
                Cell.class,
                ClassParameter.from(int.class, row),
                ClassParameter.from(int.class, column));
    }
}
