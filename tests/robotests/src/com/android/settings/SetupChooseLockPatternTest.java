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

package com.android.settings;

import static com.google.common.truth.Truth.assertThat;

import static org.robolectric.RuntimeEnvironment.application;

import android.app.Activity;
import android.content.ComponentName;
import android.content.pm.PackageManager;
import android.os.UserHandle;

import com.android.settings.password.ChooseLockPattern.ChooseLockPatternFragment;
import com.android.settings.password.ChooseLockPattern.IntentBuilder;
import com.android.settings.password.SetupChooseLockPattern;
import com.android.settings.testutils.SettingsRobolectricTestRunner;
import com.android.settings.testutils.shadow.SettingsShadowResources;
import com.android.settings.testutils.shadow.ShadowEventLogWriter;
import com.android.settings.testutils.shadow.ShadowUtils;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import org.robolectric.res.builder.RobolectricPackageManager.ComponentState;

@RunWith(SettingsRobolectricTestRunner.class)
@Config(
        manifest = TestConfig.MANIFEST_PATH,
        sdk = TestConfig.SDK_VERSION,
        shadows = {
                SettingsShadowResources.class,
                SettingsShadowResources.SettingsShadowTheme.class,
                ShadowEventLogWriter.class,
                ShadowUtils.class
        })
public class SetupChooseLockPatternTest {

    private SetupChooseLockPattern mActivity;

    @Before
    public void setUp() {
        RuntimeEnvironment.getRobolectricPackageManager().setComponentEnabledSetting(
                new ComponentName(application, SetupRedactionInterstitial.class),
                PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                PackageManager.DONT_KILL_APP);

        mActivity =  Robolectric.buildActivity(
                SetupChooseLockPattern.class,
                SetupChooseLockPattern.modifyIntentForSetup(
                        application,
                        new IntentBuilder(application)
                                .setUserId(UserHandle.myUserId())
                                .build()))
                .setup().get();
    }

    @Test
    public void chooseLockSaved_shouldEnableRedactionInterstitial() {
        findFragment(mActivity).onChosenLockSaveFinished(false, null);

        ComponentState redactionComponentState =
                RuntimeEnvironment.getRobolectricPackageManager().getComponentState(
                        new ComponentName(application, SetupRedactionInterstitial.class));
        assertThat(redactionComponentState.newState).named("Redaction component state")
                .isEqualTo(PackageManager.COMPONENT_ENABLED_STATE_ENABLED);
    }

    private ChooseLockPatternFragment findFragment(Activity activity) {
        return (ChooseLockPatternFragment)
                activity.getFragmentManager().findFragmentById(R.id.main_content);
    }
}
