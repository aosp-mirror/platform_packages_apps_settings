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
 * limitations under the License.
 */

package com.android.settings.development;

import static com.google.common.truth.Truth.assertThat;

import com.android.settings.TestConfig;
import com.android.settings.testutils.SettingsRobolectricTestRunner;
import com.android.settings.testutils.shadow.ShadowUtils;
import com.android.settings.widget.SwitchBar;
import com.android.settingslib.core.lifecycle.Lifecycle;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import org.robolectric.util.ReflectionHelpers;

import java.util.ArrayList;

@RunWith(SettingsRobolectricTestRunner.class)
@Config(manifest = TestConfig.MANIFEST_PATH, sdk = TestConfig.SDK_VERSION,
        shadows = {
                ShadowUtils.class
        })
public class DevelopmentSwitchBarControllerTest {

    @Mock
    private DevelopmentSettings mSettings;
    private Lifecycle mLifecycle;
    private SwitchBar mSwitchBar;
    private DevelopmentSwitchBarController mController;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mLifecycle = new Lifecycle();
        mSwitchBar = new SwitchBar(RuntimeEnvironment.application);
    }

    @After
    public void tearDown() {
        ShadowUtils.reset();
    }

    @Test
    public void runThroughLifecycle_isMonkeyRun_shouldNotRegisterListener() {
        ShadowUtils.setIsUserAMonkey(true);
        mController = new DevelopmentSwitchBarController(mSettings, mSwitchBar,
                true /* isAvailable */, mLifecycle);
        final ArrayList<SwitchBar.OnSwitchChangeListener> listeners =
                ReflectionHelpers.getField(mSwitchBar, "mSwitchChangeListeners");

        mLifecycle.onStart();
        assertThat(listeners).doesNotContain(mSettings);

        mLifecycle.onStop();
        assertThat(listeners).doesNotContain(mSettings);
    }

    @Test
    public void runThroughLifecycle_isNotMonkeyRun_shouldRegisterAndRemoveListener() {
        ShadowUtils.setIsUserAMonkey(false);
        mController = new DevelopmentSwitchBarController(mSettings, mSwitchBar,
                true /* isAvailable */, mLifecycle);
        final ArrayList<SwitchBar.OnSwitchChangeListener> listeners =
                ReflectionHelpers.getField(mSwitchBar, "mSwitchChangeListeners");

        mLifecycle.onStart();
        assertThat(listeners).contains(mSettings);

        mLifecycle.onStop();
        assertThat(listeners).doesNotContain(mSettings);
    }

    @Test
    public void buildController_unavailable_shouldDisableSwitchBar() {
        ShadowUtils.setIsUserAMonkey(false);
        mController = new DevelopmentSwitchBarController(mSettings, mSwitchBar,
                false /* isAvailable */, mLifecycle);

        assertThat(mSwitchBar.isEnabled()).isFalse();
    }
}
