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

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import androidx.appcompat.app.AlertDialog;

import com.android.settings.R;
import com.android.settings.password.ChooseLockGeneric.ChooseLockGenericFragment;
import com.android.settings.password.ChooseLockPassword.IntentBuilder;
import com.android.settings.password.SetupChooseLockPassword.SetupChooseLockPasswordFragment;
import com.android.settings.testutils.SettingsRobolectricTestRunner;
import com.android.settings.testutils.shadow.SettingsShadowResources;
import com.android.settings.testutils.shadow.SettingsShadowResourcesImpl;
import com.android.settings.testutils.shadow.ShadowUtils;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Shadows;
import org.robolectric.android.controller.ActivityController;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.Implementation;
import org.robolectric.annotation.Implements;
import org.robolectric.shadows.ShadowActivity;
import org.robolectric.shadows.ShadowDialog;

import java.util.Collections;
import java.util.List;

@RunWith(SettingsRobolectricTestRunner.class)
@Config(shadows = {
        SettingsShadowResources.class,
        SettingsShadowResourcesImpl.class,
        SettingsShadowResources.SettingsShadowTheme.class,
        ShadowUtils.class
})
public class SetupChooseLockPasswordTest {

    @Before
    public void setUp() throws Exception {
        SettingsShadowResources.overrideResource(
                com.android.internal.R.string.config_headlineFontFamily, "");
    }

    @After
    public void tearDown() {
        SettingsShadowResources.reset();
    }

    @Test
    public void createActivity_shouldNotCrash() {
        // Basic sanity test for activity created without crashing
        final Intent intent =
                SetupChooseLockPassword.modifyIntentForSetup(
                        application,
                        new IntentBuilder(application).build());

        ActivityController.of(new SetupChooseLockPassword(), intent).setup().get();
    }

    @Test
    public void createActivity_withShowOptionsButtonExtra_shouldShowButton() {
        SetupChooseLockPassword activity = createSetupChooseLockPassword();
        Button optionsButton = activity.findViewById(R.id.screen_lock_options);
        assertThat(optionsButton).isNotNull();
        optionsButton.performClick();
        assertThat(ShadowDialog.getLatestDialog()).isNotNull();
    }

    @Test
    @Config(shadows = ShadowChooseLockGenericController.class)
    public void createActivity_withShowOptionsButtonExtra_buttonNotVisibleIfNoVisibleLockTypes() {
        SetupChooseLockPassword activity = createSetupChooseLockPassword();
        Button optionsButton = activity.findViewById(R.id.screen_lock_options);
        assertThat(optionsButton).isNotNull();
        assertThat(optionsButton.getVisibility()).isEqualTo(View.GONE);
    }

    @Test
    public void allSecurityOptions_shouldBeShown_When_OptionsButtonIsClicked() {
        SetupChooseLockPassword activity = createSetupChooseLockPassword();
        activity.findViewById(R.id.screen_lock_options).performClick();
        AlertDialog latestAlertDialog = (AlertDialog) ShadowDialog.getLatestDialog();
        int count = latestAlertDialog.getListView().getCount();
        assertThat(count).named("List items shown").isEqualTo(3);
    }

    @Test
    public void createActivity_clickDifferentOption_extrasShouldBePropagated() {
        Bundle bundle = new Bundle();
        bundle.putString("foo", "bar");

        Intent intent = new IntentBuilder(application).build();
        intent.putExtra(ChooseLockGenericFragment.EXTRA_CHOOSE_LOCK_GENERIC_EXTRAS, bundle);
        intent = SetupChooseLockPassword.modifyIntentForSetup(application, intent);
        intent.putExtra(ChooseLockGenericFragment.EXTRA_SHOW_OPTIONS_BUTTON, true);

        SetupChooseLockPassword activity =
                ActivityController.of(new SetupChooseLockPassword(), intent).setup().get();

        SetupChooseLockPasswordFragment fragment =
                (SetupChooseLockPasswordFragment) activity.getSupportFragmentManager()
                        .findFragmentById(R.id.main_content);
        fragment.onLockTypeSelected(ScreenLockType.PATTERN);

        ShadowActivity shadowActivity = Shadows.shadowOf(activity);
        final Intent nextStartedActivity = shadowActivity.getNextStartedActivity();
        assertThat(nextStartedActivity).isNotNull();
        assertThat(nextStartedActivity.getBooleanExtra(
                ChooseLockGenericFragment.EXTRA_SHOW_OPTIONS_BUTTON, false)).isTrue();
        assertThat(nextStartedActivity.getStringExtra("foo")).named("Foo extra")
                .isEqualTo("bar");
    }

    private SetupChooseLockPassword createSetupChooseLockPassword() {
        final Intent intent =
                SetupChooseLockPassword.modifyIntentForSetup(
                        application,
                        new IntentBuilder(application).build());
        intent.putExtra(ChooseLockGenericFragment.EXTRA_SHOW_OPTIONS_BUTTON, true);
        return ActivityController.of(new SetupChooseLockPassword(), intent).setup().get();
    }

    @Implements(ChooseLockGenericController.class)
    public static class ShadowChooseLockGenericController {
        @Implementation
        public List<ScreenLockType> getVisibleScreenLockTypes(int quality,
                boolean includeDisabled) {
            return Collections.emptyList();
        }
    }
}
