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

import static org.robolectric.RuntimeEnvironment.application;

import android.content.ComponentName;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.UserHandle;
import android.view.View;
import android.widget.Button;

import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.FragmentActivity;

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

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.Shadows;
import org.robolectric.android.controller.ActivityController;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowPackageManager;
import org.robolectric.util.ReflectionHelpers;
import org.robolectric.util.ReflectionHelpers.ClassParameter;

import java.util.Arrays;

@RunWith(RobolectricTestRunner.class)
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
    public void optionsButton_whenPatternSelected_shouldBeVisible() {
        Button button = mActivity.findViewById(R.id.screen_lock_options);
        assertThat(button).isNotNull();
        assertThat(button.getVisibility()).isEqualTo(View.VISIBLE);

        LockPatternView lockPatternView = mActivity.findViewById(R.id.lockPattern);
        ReflectionHelpers.callInstanceMethod(lockPatternView, "notifyPatternDetected");

        enterPattern();
        assertThat(button.getVisibility()).isEqualTo(View.VISIBLE);
    }

    private void verifyScreenLockOptionsShown() {
        Button button = mActivity.findViewById(R.id.screen_lock_options);
        assertThat(button).isNotNull();
        assertThat(button.getVisibility()).isEqualTo(View.VISIBLE);

        button.performClick();
        AlertDialog chooserDialog = ShadowAlertDialogCompat.getLatestAlertDialog();
        assertThat(chooserDialog).isNotNull();
        int count = chooserDialog.getListView().getCount();
        assertThat(count).named("List items shown").isEqualTo(3);
    }

    @Config(qualifiers = "sw400dp")
    @Test
    public void sw400dp_shouldShowScreenLockOptions() {
        verifyScreenLockOptionsShown();
    }

    @Config(qualifiers = "sw400dp-land")
    @Test
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
    public void smallScreens_shouldHideScreenLockOptions() {
        verifyScreenLockOptionsHidden();
    }

    @Config(qualifiers = "sw300dp-land")
    @Test
    public void smallScreensLandscape_shouldHideScreenLockOptions() {
        verifyScreenLockOptionsHidden();
    }

    @Test
    public void skipButton_shouldBeVisible_duringNonFingerprintFlow() {
        PartnerCustomizationLayout layout = mActivity.findViewById(R.id.setup_wizard_layout);
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

        PartnerCustomizationLayout layout = mActivity.findViewById(R.id.setup_wizard_layout);
        final Button skipOrClearButton =
                layout.getMixin(FooterBarMixin.class).getSecondaryButtonView();
        assertThat(skipOrClearButton.getVisibility()).isEqualTo(View.VISIBLE);
        assertThat(skipOrClearButton.isEnabled()).isTrue();

        skipOrClearButton.performClick();
        assertThat(findFragment(mActivity).mChosenPattern).isNull();
    }

    @Test
    public void createActivity_enterPattern_clearButtonShouldBeShown() {
        ChooseLockPatternFragment fragment = findFragment(mActivity);

        PartnerCustomizationLayout layout = mActivity.findViewById(R.id.setup_wizard_layout);
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

    private Cell createCell(int row, int column) {
        return ReflectionHelpers.callConstructor(
                Cell.class,
                ClassParameter.from(int.class, row),
                ClassParameter.from(int.class, column));
    }
}
