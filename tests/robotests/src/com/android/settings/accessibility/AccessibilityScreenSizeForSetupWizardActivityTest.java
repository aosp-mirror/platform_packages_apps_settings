/*
 * Copyright (C) 2021 The Android Open Source Project
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

import static com.android.settings.accessibility.AccessibilityScreenSizeForSetupWizardActivity.VISION_FRAGMENT_NO;
import static com.android.settings.core.SettingsBaseActivity.EXTRA_PAGE_TRANSITION_TYPE;

import static com.google.common.truth.Truth.assertThat;

import static org.robolectric.Shadows.shadowOf;

import android.content.Context;
import android.content.Intent;

import androidx.test.core.app.ApplicationProvider;

import com.android.settings.R;
import com.android.settings.accessibility.AccessibilityScreenSizeForSetupWizardActivity.FragmentType;
import com.android.settingslib.transition.SettingsTransitionHelper.TransitionType;

import com.google.android.setupcompat.template.FooterBarMixin;
import com.google.android.setupdesign.GlifLayout;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;

/** Tests for {@link AccessibilityScreenSizeForSetupWizardActivity} */
@RunWith(RobolectricTestRunner.class)
public class AccessibilityScreenSizeForSetupWizardActivityTest {

    private Context mContext = ApplicationProvider.getApplicationContext();

    private AccessibilityScreenSizeForSetupWizardActivity setupActivity(int fragmentType) {
        final Intent intent = new Intent();
        intent.putExtra(VISION_FRAGMENT_NO, fragmentType);
        return Robolectric.buildActivity(AccessibilityScreenSizeForSetupWizardActivity.class,
                intent).create().get();
    }

    private AccessibilityScreenSizeForSetupWizardActivity setupActivity(int fragmentType,
            int transitionType) {
        final Intent intent = new Intent();
        intent.putExtra(VISION_FRAGMENT_NO, fragmentType);
        intent.putExtra(EXTRA_PAGE_TRANSITION_TYPE, transitionType);
        return Robolectric.buildActivity(AccessibilityScreenSizeForSetupWizardActivity.class,
                intent).create().get();
    }

    @Test
    public void setupActivity_fontSizePage_returnFontSizeTitle() {
        final AccessibilityScreenSizeForSetupWizardActivity activity =
                setupActivity(FragmentType.FONT_SIZE, TransitionType.TRANSITION_FADE);

        final GlifLayout layout = activity.findViewById(R.id.setup_wizard_layout);
        assertThat(layout.getHeaderText()).isEqualTo(mContext.getText(R.string.title_font_size));
    }

    @Test
    public void setupActivity_generateDoneButton() {
        final AccessibilityScreenSizeForSetupWizardActivity activity =
                setupActivity(FragmentType.FONT_SIZE, TransitionType.TRANSITION_FADE);

        final GlifLayout layout = activity.findViewById(R.id.setup_wizard_layout);
        final FooterBarMixin mixin = layout.getMixin(FooterBarMixin.class);
        assertThat(mixin.getPrimaryButton().getText()).isEqualTo(mContext.getText(R.string.done));
    }

    @Test
    public void onPause_getPendingTransitionEnterAnimationResourceId_transitionFade_should() {
        final AccessibilityScreenSizeForSetupWizardActivity activity =
                setupActivity(FragmentType.FONT_SIZE, TransitionType.TRANSITION_FADE);

        activity.onPause();

        assertThat(shadowOf(activity).getPendingTransitionEnterAnimationResourceId())
                .isEqualTo(R.anim.sud_stay);
    }

    @Test
    public void onPause_getPendingTransitionExitAnimationResourceId_transitionFade_should() {
        final AccessibilityScreenSizeForSetupWizardActivity activity =
                setupActivity(FragmentType.FONT_SIZE, TransitionType.TRANSITION_FADE);

        activity.onPause();

        assertThat(shadowOf(activity).getPendingTransitionExitAnimationResourceId())
                .isEqualTo(android.R.anim.fade_out);
    }

    @Test
    public void onPause_getPendingTransitionEnterAnimationResourceId_transitionNone_should() {
        final AccessibilityScreenSizeForSetupWizardActivity activity =
                setupActivity(FragmentType.FONT_SIZE);

        activity.onPause();

        assertThat(shadowOf(activity).getPendingTransitionEnterAnimationResourceId())
                .isNotEqualTo(R.anim.sud_stay);
    }

    @Test
    public void onPause_getPendingTransitionExitAnimationResourceId_transitionNone_should() {
        final AccessibilityScreenSizeForSetupWizardActivity activity =
                setupActivity(FragmentType.FONT_SIZE);

        activity.onPause();

        assertThat(shadowOf(activity).getPendingTransitionExitAnimationResourceId())
                .isNotEqualTo(android.R.anim.fade_out);
    }

    @Test
    public void updateHeaderLayout_displaySizePage_returnDisplaySizeTitle() {
        final Intent intent = new Intent();
        intent.putExtra(VISION_FRAGMENT_NO, FragmentType.SCREEN_SIZE);
        intent.putExtra(EXTRA_PAGE_TRANSITION_TYPE, TransitionType.TRANSITION_FADE);
        final AccessibilityScreenSizeForSetupWizardActivity activity = Robolectric.buildActivity(
                AccessibilityScreenSizeForSetupWizardActivity.class, intent).get();
        activity.setContentView(R.layout.accessibility_screen_size_setup_wizard);
        activity.updateHeaderLayout();
        final GlifLayout layout = activity.findViewById(R.id.setup_wizard_layout);
        assertThat(layout.getHeaderText()).isEqualTo(mContext.getText(R.string.screen_zoom_title));
    }
}
