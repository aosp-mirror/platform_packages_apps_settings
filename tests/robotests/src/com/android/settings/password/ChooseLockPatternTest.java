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
import android.os.UserHandle;
import android.view.View;

import com.android.internal.widget.LockPatternUtils;
import com.android.internal.widget.LockscreenCredential;
import com.android.settings.R;
import com.android.settings.password.ChooseLockPattern.ChooseLockPatternFragment;
import com.android.settings.password.ChooseLockPattern.IntentBuilder;
import com.android.settings.testutils.shadow.ShadowUtils;
import com.android.settingslib.testutils.DrawableTestHelper;

import com.google.android.setupdesign.GlifLayout;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

@RunWith(RobolectricTestRunner.class)
@Config(shadows = ShadowUtils.class)
public class ChooseLockPatternTest {

    @Test
    public void activityCreationTest() {
        // Basic sanity test for activity created without crashing
        Robolectric.buildActivity(ChooseLockPattern.class, new IntentBuilder(application).build())
                .setup().get();
    }

    @Test
    public void intentBuilder_setPattern_shouldAddExtras() {
        Intent intent = new IntentBuilder(application)
                .setPattern(createPattern("1234"))
                .setUserId(123)
                .build();

        assertThat(intent
                .getBooleanExtra(ChooseLockSettingsHelper.EXTRA_KEY_HAS_CHALLENGE, true))
                .named("EXTRA_KEY_HAS_CHALLENGE")
                .isFalse();
        assertThat((LockscreenCredential) intent
                .getParcelableExtra(ChooseLockSettingsHelper.EXTRA_KEY_PASSWORD))
                .named("EXTRA_KEY_PASSWORD")
                .isEqualTo(createPattern("1234"));
        assertThat(intent.getIntExtra(Intent.EXTRA_USER_ID, 0))
                .named("EXTRA_USER_ID")
                .isEqualTo(123);
    }

    @Test
    public void intentBuilder_setChallenge_shouldAddExtras() {
        Intent intent = new IntentBuilder(application)
                .setChallenge(12345L)
                .setUserId(123)
                .build();

        assertThat(intent
                .getBooleanExtra(ChooseLockSettingsHelper.EXTRA_KEY_HAS_CHALLENGE, false))
                .named("EXTRA_KEY_HAS_CHALLENGE")
                .isTrue();
        assertThat(intent
                .getLongExtra(ChooseLockSettingsHelper.EXTRA_KEY_CHALLENGE, 0L))
                .named("EXTRA_KEY_CHALLENGE")
                .isEqualTo(12345L);
        assertThat(intent
                .getIntExtra(Intent.EXTRA_USER_ID, 0))
                .named("EXTRA_USER_ID")
                .isEqualTo(123);
    }

    @Test
    public void intentBuilder_setProfileToUnify_shouldAddExtras() {
        Intent intent = new IntentBuilder(application)
                .setProfileToUnify(23, LockscreenCredential.createNone())
                .build();

        assertThat(intent.getIntExtra(ChooseLockSettingsHelper.EXTRA_KEY_UNIFICATION_PROFILE_ID, 0))
                .named("EXTRA_KEY_UNIFICATION_PROFILE_ID")
                .isEqualTo(23);
        assertThat((LockscreenCredential) intent.getParcelableExtra(
                ChooseLockSettingsHelper.EXTRA_KEY_UNIFICATION_PROFILE_CREDENTIAL))
                .named("EXTRA_KEY_UNIFICATION_PROFILE_CREDENTIAL")
                .isNotNull();
    }

    @Config(qualifiers = "sw400dp")
    @Test
    public void fingerprintExtraSet_shouldDisplayFingerprintIcon() {
        ChooseLockPattern activity = createActivity(true);
        ChooseLockPatternFragment fragment = (ChooseLockPatternFragment)
                activity.getSupportFragmentManager().findFragmentById(R.id.main_content);
        DrawableTestHelper.assertDrawableResId(((GlifLayout) fragment.getView()).getIcon(),
                R.drawable.ic_fingerprint_header);
    }

    @Config(qualifiers = "sw300dp")
    @Test
    public void smallScreens_shouldHideIcon() {
        ChooseLockPattern activity = createActivity(true);
        ChooseLockPatternFragment fragment = (ChooseLockPatternFragment)
                activity.getSupportFragmentManager().findFragmentById(R.id.main_content);

        View iconView = fragment.getView().findViewById(R.id.sud_layout_icon);
        assertThat(iconView.getVisibility()).isEqualTo(View.GONE);
    }

    private ChooseLockPattern createActivity(boolean addFingerprintExtra) {
        return Robolectric.buildActivity(
                ChooseLockPattern.class,
                new IntentBuilder(application)
                        .setUserId(UserHandle.myUserId())
                        .setForFingerprint(addFingerprintExtra)
                        .build())
                .setup().get();
    }

    private LockscreenCredential createPattern(String patternString) {
        return LockscreenCredential.createPattern(LockPatternUtils.byteArrayToPattern(
                patternString.getBytes()));
    }
}
