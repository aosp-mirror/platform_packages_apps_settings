/*
 * Copyright (C) 2019 The Android Open Source Project
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

import static android.Manifest.permission.REQUEST_PASSWORD_COMPLEXITY;
import static android.app.admin.DevicePolicyManager.PASSWORD_COMPLEXITY_HIGH;

import static com.android.settings.password.ChooseLockSettingsHelper.EXTRA_KEY_REQUESTED_MIN_COMPLEXITY;

import static com.google.common.truth.Truth.assertThat;

import android.content.Intent;
import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import com.android.settings.password.SetupChooseLockGeneric.SetupChooseLockGenericFragment;
import com.android.settings.testutils.shadow.ShadowLockPatternUtils;
import com.android.settings.testutils.shadow.ShadowPasswordUtils;
import com.android.settings.testutils.shadow.ShadowUserManager;
import com.android.settings.testutils.shadow.ShadowUtils;

import com.google.android.setupdesign.GlifPreferenceLayout;

import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.Shadows;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowActivity;

import java.util.List;

@RunWith(RobolectricTestRunner.class)
@Config(shadows = {
        ShadowUserManager.class,
        ShadowUtils.class,
        ShadowLockPatternUtils.class,
})
public class SetupChooseLockGenericTest {

    @After
    public void tearDown() {
        ShadowPasswordUtils.reset();
    }

    @Test
    public void setupChooseLockGenericPasswordComplexityExtraWithoutPermission() {
        Intent intent = new Intent("com.android.settings.SETUP_LOCK_SCREEN");
        intent.putExtra(EXTRA_KEY_REQUESTED_MIN_COMPLEXITY, PASSWORD_COMPLEXITY_HIGH);
        SetupChooseLockGeneric activity =
                Robolectric.buildActivity(SetupChooseLockGeneric.class, intent).create().get();

        ShadowActivity shadowActivity = Shadows.shadowOf(activity);
        assertThat(shadowActivity.isFinishing()).isTrue();
    }

    @Test
    @Config(shadows = {ShadowPasswordUtils.class})
    public void setupChooseLockGenericPasswordComplexityExtraWithPermission() {
        ShadowPasswordUtils.addGrantedPermission(REQUEST_PASSWORD_COMPLEXITY);

        Intent intent = new Intent("com.android.settings.SETUP_LOCK_SCREEN");
        intent.putExtra(EXTRA_KEY_REQUESTED_MIN_COMPLEXITY, PASSWORD_COMPLEXITY_HIGH);
        SetupChooseLockGeneric activity =
                Robolectric.buildActivity(SetupChooseLockGeneric.class, intent).create().get();

        ShadowActivity shadowActivity = Shadows.shadowOf(activity);
        assertThat(shadowActivity.isFinishing()).isFalse();
    }

    @Test
    public void setupChooseLockGenericUsingDescriptionTextOfGlifLayout() {
        SetupChooseLockGenericFragment fragment = getFragmentOfSetupChooseLockGeneric(false);
        GlifPreferenceLayout view = getViewOfSetupChooseLockGenericFragment(fragment);
        assertThat(TextUtils.isEmpty(view.getDescriptionText())).isFalse();
        assertThat(view.getDescriptionText().toString()).isEqualTo(fragment.loadDescriptionText());
    }

    @Test
    public void setupChooseLockGenericUsingDescriptionTextOfGlifLayoutForBiometric() {
        SetupChooseLockGenericFragment fragment = getFragmentOfSetupChooseLockGeneric(true);
        GlifPreferenceLayout view = getViewOfSetupChooseLockGenericFragment(fragment);
        assertThat(TextUtils.isEmpty(view.getDescriptionText())).isFalse();
        assertThat(view.getDescriptionText().toString()).isEqualTo(fragment.loadDescriptionText());
    }

    private SetupChooseLockGenericFragment getFragmentOfSetupChooseLockGeneric(boolean biometric) {
        ShadowPasswordUtils.addGrantedPermission(REQUEST_PASSWORD_COMPLEXITY);
        Intent intent = new Intent("com.android.settings.SETUP_LOCK_SCREEN");
        intent.putExtra(EXTRA_KEY_REQUESTED_MIN_COMPLEXITY, PASSWORD_COMPLEXITY_HIGH);
        intent.putExtra(ChooseLockSettingsHelper.EXTRA_KEY_FOR_FINGERPRINT, biometric);
        SetupChooseLockGeneric activity =
                Robolectric.buildActivity(SetupChooseLockGeneric.class, intent).setup().get();

        List<Fragment> fragments = activity.getSupportFragmentManager().getFragments();
        assertThat(fragments).isNotNull();
        assertThat(fragments.size()).isEqualTo(1);
        assertThat(fragments.get(0)).isInstanceOf(SetupChooseLockGenericFragment.class);

        return (SetupChooseLockGenericFragment) fragments.get(0);
    }
    private GlifPreferenceLayout getViewOfSetupChooseLockGenericFragment(
            @NonNull SetupChooseLockGenericFragment fragment) {
        assertThat(fragment.getView()).isNotNull();
        assertThat(fragment.getView()).isInstanceOf(GlifPreferenceLayout.class);

        return (GlifPreferenceLayout) fragment.getView();
    }
}
